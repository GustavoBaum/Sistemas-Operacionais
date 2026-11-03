package minikernel.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minikernel.core.Estado;
import minikernel.core.Evento;
import minikernel.core.Instrucao;
import minikernel.core.MotivoBloqueio;
import minikernel.core.Ouvinte;
import minikernel.core.Processo;
import minikernel.deadlock.GerenciadorRecursos;
import minikernel.fs.SistemaArquivos;
import minikernel.mem.GerenciadorMemoria;
import minikernel.sched.Escalonador;
import minikernel.sync.Sincronizacao;

/**
 * Kernel: único ponto que conhece todos os módulos.
 * A cada tick, pega o processo escolhido pelo escalonador, lê a próxima instrução
 * e a despacha ao módulo correspondente.
 */
public class Kernel {
    private final Escalonador escalonador;
    private final GerenciadorMemoria memoria;
    private final GerenciadorRecursos recursos;
    private final SistemaArquivos arquivos;
    private final Sincronizacao semaforos;

    private final List<Ouvinte> ouvintes = new ArrayList<>();
    private final Map<Integer, Processo> tabela = new HashMap<>();
    private Processo atual;
    private int relogio;

    public Kernel(Escalonador e, GerenciadorMemoria m, GerenciadorRecursos r,
                  SistemaArquivos a, Sincronizacao s) {
        this.escalonador = e;
        this.memoria = m;
        this.recursos = r;
        this.arquivos = a;
        this.semaforos = s;
    }

    public void observar(Ouvinte o) {
        ouvintes.add(o);
    }

    public int relogio() {
        return relogio;
    }

    private void emitir(String origem, Processo p, String msg) {
        Evento ev = new Evento(relogio, origem, p == null ? -1 : p.pid, msg);
        ouvintes.forEach(o -> o.aoOcorrer(ev));
    }

    public void criarProcesso(Processo p) {
        p.instanteChegada = relogio;
        tabela.put(p.pid, p);
        memoria.criarEspaco(p.pid, p.numPaginas);
        recursos.registrar(p.pid, p.demandaMaxima);
        escalonador.admitir(p);
        emitir("KERNEL", p, "processo criado");
    }

    public boolean terminou() {
        return tabela.values().stream().allMatch(p -> p.estado == Estado.TERMINADO);
    }

    /** Um "tick" de relógio: o coração da simulação integrada. */
    public void executarTick() {
        relogio++;
        escalonador.tick();

        if (relogio % 10 == 0) {
            List<Integer> presos = recursos.detectarDeadlock();
            if (!presos.isEmpty()) emitir("DEADLOCK", null, "ciclo entre PIDs " + presos);
        }

        if (atual == null) atual = escalonador.proximo().orElse(null);
        if (atual == null) {
            emitir("CPU", null, "ociosa (todos bloqueados)");
            return;
        }

        Processo p = atual;
        executarInstrucao(p);

        if (p.estado == Estado.TERMINADO || p.estado == Estado.BLOQUEADO) {
            atual = null;
        } else if (escalonador.cobrarTick(p)) {
            escalonador.aoFimDoQuantum(p);
            emitir("SCHED", p, "quantum esgotado, volta para a fila");
            atual = null;
        }
    }

    private void executarInstrucao(Processo p) {
        Instrucao i = p.programa.peek();
        if (i == null) {
            finalizar(p);
            return;
        }
        emitir("CPU", p, descrever(i));

        if (i instanceof Instrucao.Cpu c) {
            if (p.cpuRestante < 0) p.cpuRestante = c.ciclos();
            if (--p.cpuRestante == 0) {
                p.cpuRestante = -1;
                p.programa.poll();
            }
        } else if (i instanceof Instrucao.Es e) {
            p.programa.poll();
            emitir("E/S", p, "operação de E/S por " + e.ticks() + " tick(s)");
            if (e.ticks() > 0) escalonador.bloquear(p, MotivoBloqueio.DISCO, e.ticks());
        } else if (i instanceof Instrucao.AcessoMemoria a) {
            var r = memoria.acessar(p.pid, a.enderecoVirtual());
            if (r.pageFault()) {
                // a instrução NÃO é consumida: será reexecutada (e dará hit) ao acordar
                emitir("MMU", p, "PAGE FAULT -> quadro " + r.quadro()
                        + ", bloqueado por " + r.custoTicks() + " tick(s)");
                escalonador.bloquear(p, MotivoBloqueio.PAGE_FAULT, r.custoTicks());
            } else {
                p.programa.poll();
            }
        } else if (i instanceof Instrucao.PedirRecursos r) {
            if (recursos.solicitar(p.pid, r.quantidades())) {
                p.programa.poll();
                emitir("BANQUEIRO", p, "recursos concedidos " + Arrays.toString(r.quantidades()));
            } else {
                emitir("BANQUEIRO", p, "pedido NEGADO (indisponível ou estado inseguro), bloqueado");
                escalonador.bloquear(p, MotivoBloqueio.RECURSO, 0);
            }
        } else if (i instanceof Instrucao.LiberarRecursos r) {
            recursos.liberar(p.pid, r.quantidades());
            p.programa.poll();
            emitir("BANQUEIRO", p, "recursos liberados " + Arrays.toString(r.quantidades()));
            escalonador.desbloquearPorMotivo(MotivoBloqueio.RECURSO);
        } else if (i instanceof Instrucao.LerArquivo l) {
            int custo = arquivos.ler(p.pid, l.nome());
            p.programa.poll();
            emitir("FS", p, "leu '" + l.nome() + "' (" + custo + " bloco(s))");
            if (custo > 0) escalonador.bloquear(p, MotivoBloqueio.DISCO, custo);
        } else if (i instanceof Instrucao.EscreverArquivo w) {
            int custo = arquivos.escrever(p.pid, w.nome(), w.blocos());
            p.programa.poll();
            emitir("FS", p, "gravou '" + w.nome() + "' (" + custo + " bloco(s))");
            if (custo > 0) escalonador.bloquear(p, MotivoBloqueio.DISCO, custo);
        } else if (i instanceof Instrucao.Wait w) {
            boolean passou = semaforos.wait(p.pid, w.semaforo());
            p.programa.poll(); // ao ser acordado por signal, o wait já está satisfeito
            if (!passou) {
                emitir("SEM", p, "wait(" + w.semaforo() + ") bloqueou");
                escalonador.bloquear(p, MotivoBloqueio.SEMAFORO, 0);
            }
        } else if (i instanceof Instrucao.Signal s) {
            List<Integer> acordar = semaforos.signal(s.semaforo());
            for (int pid : acordar) {
                emitir("SEM", p, "signal(" + s.semaforo() + ") acordou P" + pid);
                escalonador.desbloquear(tabela.get(pid));
            }
            p.programa.poll();
        }


        if (p.estado == Estado.EXECUTANDO && p.programa.isEmpty()) finalizar(p);
    }

    private void finalizar(Processo p) {
        p.instanteFim = relogio;
        memoria.liberar(p.pid);
        recursos.liberarTodos(p.pid);
        escalonador.terminar(p);
        escalonador.desbloquearPorMotivo(MotivoBloqueio.RECURSO);
        emitir("KERNEL", p, "processo TERMINOU");
    }

    private static String descrever(Instrucao i) {
        if (i instanceof Instrucao.Cpu c) return "computa (" + c.ciclos() + " ciclo(s) no total)";
        if (i instanceof Instrucao.Es e) return "E/S (" + e.ticks() + " tick(s))";
        if (i instanceof Instrucao.AcessoMemoria a) return "acessa endereço virtual " + a.enderecoVirtual();
        if (i instanceof Instrucao.PedirRecursos r) return "pede recursos " + Arrays.toString(r.quantidades());
        if (i instanceof Instrucao.LiberarRecursos r) return "libera recursos " + Arrays.toString(r.quantidades());
        if (i instanceof Instrucao.LerArquivo l) return "lê arquivo '" + l.nome() + "'";
        if (i instanceof Instrucao.EscreverArquivo w) return "grava arquivo '" + w.nome() + "'";
        if (i instanceof Instrucao.Wait w) return "wait(" + w.semaforo() + ")";
        if (i instanceof Instrucao.Signal s) return "signal(" + s.semaforo() + ")";
        throw new IllegalStateException("Instrução desconhecida: " + i);
    }

    // ---- métricas finais (Módulo 2) ----
    public double tme() {
        return tabela.values().stream().mapToInt(p -> p.tempoEmEspera).average().orElse(0);
    }

    public double tmt() {
        return tabela.values().stream().mapToInt(Processo::turnaround).average().orElse(0);
    }
}
