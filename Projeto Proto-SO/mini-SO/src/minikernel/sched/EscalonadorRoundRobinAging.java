package minikernel.sched;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import minikernel.core.Estado;
import minikernel.core.MotivoBloqueio;
import minikernel.core.Processo;

/**
 * Round-Robin com quantum + Aging.
 *
 * Convivência dos dois: a fila é circular (quem esgota o quantum vai para o fim),
 * mas na hora de escolher, quem tem a melhor prioridade EFETIVA sai primeiro.
 * Enquanto espera, o processo "envelhece" (a prioridade efetiva melhora),
 * o que impede starvation de processos de baixa prioridade.
 */
public class EscalonadorRoundRobinAging implements Escalonador {
    public static final int LIMIAR_AGING_PADRAO = 4;

    private final int quantum;
    private final int limiarAging; // ticks esperando para ganhar 1 nível (0 = aging desligado)
    private final List<Processo> prontos = new ArrayList<>();
    private final Map<Processo, Integer> esperaConsecutiva = new HashMap<>();
    private final Map<Processo, MotivoBloqueio> motivos = new LinkedHashMap<>();
    private final Map<Processo, Integer> despertar = new HashMap<>();

    public EscalonadorRoundRobinAging(int quantum) {
        this(quantum, LIMIAR_AGING_PADRAO);
    }

    public EscalonadorRoundRobinAging(int quantum, int limiarAging) {
        this.quantum = quantum;
        this.limiarAging = limiarAging;
    }

    /** Fila de prontos (cópia), para exibição. */
    public List<Processo> prontos() {
        return List.copyOf(prontos);
    }

    /** Processos bloqueados (cópia), para exibição. */
    public List<Processo> bloqueados() {
        return List.copyOf(motivos.keySet());
    }

    @Override
    public void admitir(Processo p) {
        p.estado = Estado.PRONTO;
        prontos.add(p);
    }

    @Override
    public Optional<Processo> proximo() {
        if (prontos.isEmpty()) return Optional.empty();
        Processo melhor = prontos.get(0);
        for (Processo p : prontos) {
            if (p.prioridadeEfetiva < melhor.prioridadeEfetiva) melhor = p;
        }
        prontos.remove(melhor);
        melhor.prioridadeEfetiva = melhor.prioridadeBase; // zera o aging ao ganhar a CPU
        esperaConsecutiva.remove(melhor);
        melhor.estado = Estado.EXECUTANDO;
        melhor.quantumRestante = quantum;
        return Optional.of(melhor);
    }

    @Override
    public boolean cobrarTick(Processo p) {
        return --p.quantumRestante <= 0;
    }

    @Override
    public void aoFimDoQuantum(Processo p) {
        p.estado = Estado.PRONTO;
        prontos.add(p);
    }

    @Override
    public void bloquear(Processo p, MotivoBloqueio motivo, int ticks) {
        p.estado = Estado.BLOQUEADO;
        motivos.put(p, motivo);
        if (ticks > 0) despertar.put(p, ticks);
    }

    @Override
    public void desbloquear(Processo p) {
        if (motivos.remove(p) == null) return;
        despertar.remove(p);
        p.estado = Estado.PRONTO;
        prontos.add(p);
    }

    @Override
    public void desbloquearPorMotivo(MotivoBloqueio motivo) {
        for (Processo p : new ArrayList<>(motivos.keySet())) {
            if (motivos.get(p) == motivo) desbloquear(p);
        }
    }

    @Override
    public void terminar(Processo p) {
        p.estado = Estado.TERMINADO;
    }

    @Override
    public void tick() {
        // métrica de espera + aging
        for (Processo p : prontos) {
            p.tempoEmEspera++;
            int espera = esperaConsecutiva.merge(p, 1, Integer::sum);
            if (limiarAging > 0 && espera % limiarAging == 0 && p.prioridadeEfetiva > 0) p.prioridadeEfetiva--;
        }
        // bloqueios temporizados (page fault, disco)
        for (Processo p : new ArrayList<>(despertar.keySet())) {
            if (!despertar.containsKey(p)) continue;
            if (despertar.merge(p, -1, Integer::sum) <= 0) desbloquear(p);
        }
    }
}
