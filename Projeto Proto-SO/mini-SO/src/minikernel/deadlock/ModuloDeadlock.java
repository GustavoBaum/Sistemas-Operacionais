package minikernel.deadlock;

import java.util.List;
import java.util.Optional;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 4: Impasses (Algoritmo do Banqueiro e detecção de ciclos em grafo). */
public class ModuloDeadlock implements Modulo {

    @Override
    public String nome() {
        return "Impasses / Deadlocks (Banqueiro e detecção por grafo)";
    }

    @Override
    public void executar(Terminal t) {
        while (true) {
            int op = t.menu("MÓDULO 4 | Impasses (Deadlocks)", "Voltar ao menu principal",
                    "Algoritmo do Banqueiro (evitação: matriz Need e estado seguro)",
                    "Detecção de deadlock (grafo de alocação de recursos)");
            switch (op) {
                case 0 -> { return; }
                case 1 -> banqueiro();
                case 2 -> deteccao();
                default -> { }
            }
            t.pausar();
        }
    }

    // ------------------------------------------------------------ Banqueiro

    private void banqueiro() {
        System.out.println("""

                Algoritmo do Banqueiro (Dijkstra): antes de conceder um pedido, o sistema simula a
                alocação e só a confirma se o estado resultante for SEGURO (existe uma ordem em que
                todos os processos conseguem terminar). Need = Max - Alocado.
                Cenário clássico: 5 processos (P0..P4), recursos A=10, B=5, C=7 no total.
                """);
        var b = new Banqueiro(10, 5, 7);
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        int[][] aloc = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        for (int i = 0; i < max.length; i++) {
            b.registrar(i, max[i]);
            b.definirAlocacaoInicial(i, aloc[i]);
        }

        System.out.println("Estado inicial:");
        System.out.println(b.estadoComoTexto());
        mostrarSeguranca(b);

        pedido(b, 1, new int[]{1, 0, 2});
        pedido(b, 4, new int[]{3, 3, 0});
        pedido(b, 0, new int[]{0, 2, 0});
    }

    private void pedido(Banqueiro b, int pid, int[] req) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("P" + pid + " solicita [" + Banqueiro.texto(req) + "]");
        int[] need = b.necessidade(pid);
        int[] disp = b.disponivel();
        boolean ok1 = true, ok2 = true;
        for (int i = 0; i < req.length; i++) {
            ok1 &= req[i] <= need[i];
            ok2 &= req[i] <= disp[i];
        }
        System.out.printf("  1) Pedido <= Need [%s]?      %s%n", Banqueiro.texto(need), ok1 ? "sim" : "NÃO");
        if (ok1) System.out.printf("  2) Pedido <= Available [%s]? %s%n", Banqueiro.texto(disp), ok2 ? "sim" : "NÃO");

        var resultado = b.solicitarComMotivo(pid, req);
        switch (resultado) {
            case EXCEDE_MAXIMO -> System.out.println("  => NEGADO: o processo excede o máximo que declarou.");
            case SEM_RECURSOS -> System.out.println("  => NEGADO: recursos insuficientes agora; P" + pid + " deve esperar.");
            case ESTADO_INSEGURO -> {
                System.out.println("  3) A alocação provisória deixa o sistema em estado seguro?  NÃO");
                System.out.println("  => NEGADO: concedê-lo levaria a um estado INSEGURO (rollback; risco de deadlock).");
            }
            case CONCEDIDO -> {
                System.out.println("  3) A alocação provisória deixa o sistema em estado seguro?  sim");
                System.out.println("  => CONCEDIDO. Novo estado:");
                System.out.println(b.estadoComoTexto());
                mostrarSeguranca(b);
            }
        }
    }

    private void mostrarSeguranca(Banqueiro b) {
        Optional<List<Integer>> seq = b.sequenciaSegura();
        if (seq.isPresent()) {
            StringBuilder sb = new StringBuilder();
            for (int pid : seq.get()) sb.append(sb.length() == 0 ? "" : " -> ").append("P").append(pid);
            System.out.println("  Estado SEGURO. Sequência segura: " + sb);
        } else {
            System.out.println("  Estado INSEGURO.");
        }
    }

    // ------------------------------------------------------------ Detecção

    private void deteccao() {
        System.out.println("""

                Grafo de alocação de recursos (1 instância por recurso):
                  Processo -> Recurso : o processo ESPERA o recurso
                  Recurso  -> Processo : o recurso está ALOCADO ao processo
                Um ciclo no grafo (busca em profundidade) indica deadlock.
                """);

        System.out.println("=== Cenário A: dois processos, dois recursos ===");
        var a = new GrafoAlocacao();
        a.alocar("R1", "P1");
        a.alocar("R2", "P2");
        a.requisitar("P1", "R2");
        a.requisitar("P2", "R1");
        a.descrever().forEach(System.out::println);
        relatar(a);

        System.out.println("\nRecuperação: abortar P2 (vítima) libera R2, que passa a P1.");
        a.removerProcesso("P2");
        a.concederRequisicao("P1", "R2");
        a.descrever().forEach(System.out::println);
        relatar(a);

        System.out.println("\n=== Cenário B: três processos, espera em cadeia ===");
        var b = new GrafoAlocacao();
        b.alocar("R1", "P1");
        b.alocar("R2", "P2");
        b.alocar("R3", "P3");
        b.requisitar("P1", "R2");
        b.requisitar("P2", "R3");
        b.descrever().forEach(System.out::println);
        relatar(b);

        System.out.println("\nAgora P3 passa a pedir R1 (fecha o círculo):");
        b.requisitar("P3", "R1");
        b.descrever().forEach(System.out::println);
        relatar(b);
    }

    private void relatar(GrafoAlocacao g) {
        List<String> ciclo = g.encontrarCiclo();
        if (ciclo.isEmpty()) {
            System.out.println("  => Nenhum ciclo: SEM deadlock.");
            return;
        }
        System.out.println("  => CICLO encontrado: " + String.join(" -> ", ciclo));
        String presos = ciclo.stream().filter(n -> n.startsWith("P")).distinct()
                .reduce((x, y) -> x + ", " + y).orElse("");
        System.out.println("  => DEADLOCK entre os processos: " + presos);
    }
}
