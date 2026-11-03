package minikernel.sched;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import minikernel.core.Estado;
import minikernel.core.Instrucao;
import minikernel.core.MotivoBloqueio;
import minikernel.core.Processo;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 2: Escalonamento de processos (Round-Robin com quantum + Aging). */
public class ModuloEscalonamento implements Modulo {

    private record Spec(int pid, int chegada, int prioridade, List<Instrucao> programa) {}

    private static final int LIMITE_TICKS = 500;

    @Override
    public String nome() {
        return "Escalonamento de Processos (Round-Robin + Aging)";
    }

    @Override
    public void executar(Terminal t) {
        t.titulo("MÓDULO 2 | Escalonamento de Processos");
        System.out.println("""

                Round-Robin com quantum, filas de estado (Pronto / Executando / Bloqueado) e Aging:
                quem espera muito na fila de prontos ganha prioridade, o que evita starvation.
                Prioridade: número MENOR = mais prioritário. Na fila de prontos, o número entre
                parênteses é a prioridade efetiva; '^' indica que o processo já envelheceu.
                """);
        int quantum = t.lerInt("Quantum (ticks)", 3, 1);
        int aging = t.lerInt("Aging: ganha 1 nível a cada N ticks de espera (0 = desligado)", 4, 0);
        simular(quantum, aging);
        t.pausar();
    }

    private static List<Spec> cenario() {
        return List.of(
                new Spec(1, 0, 3, List.of(new Instrucao.Cpu(6))),
                new Spec(2, 1, 1, List.of(new Instrucao.Cpu(4), new Instrucao.Es(3), new Instrucao.Cpu(2))),
                new Spec(3, 2, 5, List.of(new Instrucao.Cpu(5))),
                new Spec(4, 3, 2, List.of(new Instrucao.Cpu(3), new Instrucao.Es(2), new Instrucao.Cpu(2))));
    }

    private void simular(int quantum, int aging) {
        Map<Integer, Processo> processos = new LinkedHashMap<>();
        Map<Integer, Integer> chegada = new HashMap<>();
        Map<Integer, Integer> burstTotal = new HashMap<>();

        System.out.println("Processos de entrada:");
        System.out.println("  Proc  Chegada  Prioridade  Programa");
        for (Spec s : cenario()) {
            Processo p = new Processo(s.pid(), s.prioridade(), 0, new int[0], s.programa());
            processos.put(s.pid(), p);
            chegada.put(s.pid(), s.chegada());
            burstTotal.put(s.pid(), s.programa().stream()
                    .filter(i -> i instanceof Instrucao.Cpu)
                    .mapToInt(i -> ((Instrucao.Cpu) i).ciclos()).sum());
            System.out.printf("  P%-4d %-8d %-11d %s%n", s.pid(), s.chegada(), s.prioridade(), descrever(s.programa()));
        }
        System.out.printf("%nQuantum = %d | Aging = %s%n%n", quantum, aging == 0 ? "desligado" : "a cada " + aging + " tick(s)");

        var esc = new EscalonadorRoundRobinAging(quantum, aging);
        List<String> gantt = new ArrayList<>();
        Processo atual = null;
        int tick = 0;

        System.out.printf(" %4s | %-14s | %-26s | %-12s | %s%n", "Tick", "CPU", "Prontos", "Bloqueados", "Eventos");
        System.out.println("-".repeat(100));

        while (!todosTerminaram(processos) && tick < LIMITE_TICKS) {
            List<String> eventos = new ArrayList<>();

            for (Processo p : processos.values()) {
                if (chegada.get(p.pid) == tick) {
                    p.instanteChegada = tick;
                    esc.admitir(p);
                    eventos.add("P" + p.pid + " chegou");
                }
            }
            if (atual == null) atual = esc.proximo().orElse(null);

            String cpu = atual == null ? "ociosa" : "P" + atual.pid + " (resta " + restante(atual) + ")";
            String prontos = formatarProntos(esc.prontos());
            String bloqueados = esc.bloqueados().isEmpty() ? "-" : esc.bloqueados().stream()
                    .map(p -> "P" + p.pid).collect(Collectors.joining(" "));

            // espera + aging + despertar de bloqueios: observamos o que mudou
            Map<Integer, Integer> priAntes = new HashMap<>();
            esc.prontos().forEach(p -> priAntes.put(p.pid, p.prioridadeEfetiva));
            List<Processo> bloqueadosAntes = esc.bloqueados();
            esc.tick();
            for (Processo p : esc.prontos()) {
                Integer antes = priAntes.get(p.pid);
                if (antes != null && p.prioridadeEfetiva < antes) {
                    eventos.add("P" + p.pid + " envelheceu (pri " + antes + "->" + p.prioridadeEfetiva + ")");
                }
            }
            for (Processo p : bloqueadosAntes) {
                if (p.estado == Estado.PRONTO) eventos.add("P" + p.pid + " voltou da E/S");
            }

            if (atual == null) {
                gantt.add("--");
            } else {
                gantt.add("P" + atual.pid);
                atual = executarUmTick(atual, esc, tick, eventos);
            }

            System.out.printf(" %4d | %-14s | %-26s | %-12s | %s%n",
                    tick, cpu, prontos, bloqueados, String.join("; ", eventos));
            tick++;
        }
        if (!todosTerminaram(processos)) System.out.println("AVISO: limite de ticks atingido.");

        imprimirResumo(processos, chegada, burstTotal, gantt);
    }

    /** Executa 1 tick de CPU do processo e devolve o processo que continua na CPU (ou null). */
    private Processo executarUmTick(Processo p, EscalonadorRoundRobinAging esc, int tick, List<String> eventos) {
        if (!(p.programa.peek() instanceof Instrucao.Cpu cpu)) {
            throw new IllegalStateException("Esperava um burst de CPU em P" + p.pid);
        }
        if (p.cpuRestante < 0) p.cpuRestante = cpu.ciclos();
        p.cpuRestante--;

        if (p.cpuRestante == 0) { // burst terminou
            p.cpuRestante = -1;
            p.programa.poll();
            if (p.programa.isEmpty()) {
                p.instanteFim = tick + 1;
                esc.terminar(p);
                eventos.add("P" + p.pid + " TERMINOU");
                return null;
            }
            if (p.programa.peek() instanceof Instrucao.Es es) {
                p.programa.poll();
                esc.bloquear(p, MotivoBloqueio.DISCO, es.ticks());
                eventos.add("P" + p.pid + " pede E/S (" + es.ticks() + " ticks)");
                return null;
            }
        }
        if (esc.cobrarTick(p)) {
            esc.aoFimDoQuantum(p);
            eventos.add("P" + p.pid + " quantum esgotado");
            return null;
        }
        return p;
    }

    private void imprimirResumo(Map<Integer, Processo> processos, Map<Integer, Integer> chegada,
                                Map<Integer, Integer> burst, List<String> gantt) {
        System.out.println("\nGantt (CPU por intervalo de ticks):");
        StringBuilder sb = new StringBuilder("  ");
        int ini = 0;
        for (int i = 1; i <= gantt.size(); i++) {
            if (i == gantt.size() || !gantt.get(i).equals(gantt.get(ini))) {
                sb.append("| ").append(gantt.get(ini)).append(" (").append(ini).append("-").append(i).append(") ");
                ini = i;
            }
        }
        System.out.println(sb.append("|"));

        System.out.println("\nMétricas por processo:");
        System.out.println("  Proc  Chegada  CPU  Término  Espera  Turnaround");
        double somaEspera = 0, somaTurn = 0;
        for (Processo p : processos.values()) {
            int turn = p.instanteFim - chegada.get(p.pid);
            somaEspera += p.tempoEmEspera;
            somaTurn += turn;
            System.out.printf("  P%-4d %-8d %-4d %-8d %-7d %d%n", p.pid, chegada.get(p.pid), burst.get(p.pid),
                    p.instanteFim, p.tempoEmEspera, turn);
        }
        int n = processos.size();
        System.out.printf("%n  TME (tempo médio de espera na fila de prontos): %.2f%n", somaEspera / n);
        System.out.printf("  TMT (tempo médio de turnaround)               : %.2f%n", somaTurn / n);
        System.out.println("  (Turnaround = término - chegada, incluindo o tempo bloqueado em E/S.)");
    }

    private static boolean todosTerminaram(Map<Integer, Processo> processos) {
        return processos.values().stream().allMatch(p -> p.estado == Estado.TERMINADO);
    }

    private static String formatarProntos(List<Processo> prontos) {
        if (prontos.isEmpty()) return "-";
        return prontos.stream()
                .map(p -> "P" + p.pid + "(" + p.prioridadeEfetiva + (p.prioridadeEfetiva < p.prioridadeBase ? "^" : "") + ")")
                .collect(Collectors.joining(" "));
    }

    private static int restante(Processo p) {
        int total = 0;
        boolean primeiro = true;
        for (Instrucao i : p.programa) {
            if (i instanceof Instrucao.Cpu c) {
                total += (primeiro && p.cpuRestante >= 0) ? p.cpuRestante : c.ciclos();
            }
            primeiro = false;
        }
        return total;
    }

    private static String descrever(List<Instrucao> programa) {
        return programa.stream()
                .map(i -> i instanceof Instrucao.Cpu c ? "CPU " + c.ciclos()
                        : i instanceof Instrucao.Es e ? "E/S " + e.ticks() : "?")
                .collect(Collectors.joining(" > "));
    }
}
