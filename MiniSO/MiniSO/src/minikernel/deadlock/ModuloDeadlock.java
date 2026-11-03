package minikernel.deadlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 4: Impasses (Algoritmo do Banqueiro, detecção de ciclos e desenho dos grafos). */
public class ModuloDeadlock implements Modulo {

    private record CenarioGrafo(String arquivo, String titulo, GrafoAlocacao grafo) {}

    @Override
    public String nome() {
        return "Impasses / Deadlocks (Banqueiro, detecção e grafos)";
    }

    @Override
    public void executar(Terminal t) {
        while (true) {
            int op = t.menu("MÓDULO 4 | Impasses (Deadlocks)", "Voltar ao menu principal",
                    "Algoritmo do Banqueiro (evitação: matriz Need e estado seguro)",
                    "Detecção de deadlock (grafo de alocação de recursos)",
                    "Exibir os grafos gerados (desenho em ASCII + exportação .dot)");
            switch (op) {
                case 0 -> { return; }
                case 1 -> banqueiro();
                case 2 -> deteccao();
                case 3 -> exibirGrafos(t);
                default -> { }
            }
            t.pausar();
        }
    }

    // ------------------------------------------------------------ cenários de grafo

    /** A: P1 tem R1 e quer R2; P2 tem R2 e quer R1 -> deadlock. */
    private static GrafoAlocacao cenarioA() {
        var g = new GrafoAlocacao();
        g.alocar("R1", "P1");
        g.alocar("R2", "P2");
        g.requisitar("P1", "R2");
        g.requisitar("P2", "R1");
        return g;
    }

    /** B (antes): espera em cadeia P1 -> P2 -> P3, sem ciclo. */
    private static GrafoAlocacao cenarioBCadeia() {
        var g = new GrafoAlocacao();
        g.alocar("R1", "P1");
        g.alocar("R2", "P2");
        g.alocar("R3", "P3");
        g.requisitar("P1", "R2");
        g.requisitar("P2", "R3");
        return g;
    }

    /** B (depois): P3 passa a pedir R1 e fecha o círculo. */
    private static GrafoAlocacao cenarioBCiclo() {
        var g = cenarioBCadeia();
        g.requisitar("P3", "R1");
        return g;
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
                (Para ver os desenhos dos grafos, use a opção 3 deste módulo.)
                """);

        System.out.println("=== Cenário A: dois processos, dois recursos ===");
        var a = cenarioA();
        a.descrever().forEach(System.out::println);
        relatar(a);

        System.out.println("\nRecuperação: abortar P2 (vítima) libera R2, que passa a P1.");
        a.removerProcesso("P2");
        a.concederRequisicao("P1", "R2");
        a.descrever().forEach(System.out::println);
        relatar(a);

        System.out.println("\n=== Cenário B: três processos, espera em cadeia ===");
        var b = cenarioBCadeia();
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
        String presos = ciclo.stream().filter(GrafoAlocacao::ehProcesso).distinct()
                .reduce((x, y) -> x + ", " + y).orElse("");
        System.out.println("  => DEADLOCK entre os processos: " + presos);
    }

    // ------------------------------------------------------------ Exibição dos grafos

    private void exibirGrafos(Terminal t) {
        var recuperado = cenarioA();
        recuperado.removerProcesso("P2");
        recuperado.concederRequisicao("P1", "R2");

        List<CenarioGrafo> cenarios = List.of(
                new CenarioGrafo("A1_deadlock", "Cenário A: P1 e P2 em deadlock", cenarioA()),
                new CenarioGrafo("A2_recuperado", "Cenário A após abortar P2: P1 recebe R2", recuperado),
                new CenarioGrafo("B1_cadeia", "Cenário B: espera em cadeia, sem ciclo", cenarioBCadeia()),
                new CenarioGrafo("B2_deadlock", "Cenário B: P3 pede R1 e fecha o ciclo (3 processos)", cenarioBCiclo()));

        System.out.println("\nGrafos de alocação de recursos gerados pelas simulações de deadlock:\n");
        DesenhoGrafo.legenda().forEach(System.out::println);

        int n = 1;
        for (CenarioGrafo c : cenarios) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println(" Grafo " + n++ + ": " + c.titulo());
            System.out.println("=".repeat(70));
            DesenhoGrafo.desenhar(c.grafo()).forEach(System.out::println);
            System.out.println(c.grafo().encontrarCiclo().isEmpty()
                    ? "  Resultado: SEM deadlock."
                    : "  Resultado: DEADLOCK detectado (ciclo no grafo).");
        }

        String resp = t.lerLinha("\nExportar os 4 grafos como arquivos .dot (Graphviz) na pasta 'grafos'? [s/N]: ");
        if (resp != null && resp.equalsIgnoreCase("s")) exportar(cenarios);
    }

    private void exportar(List<CenarioGrafo> cenarios) {
        Path pasta = Path.of("grafos");
        try {
            Files.createDirectories(pasta);
            for (CenarioGrafo c : cenarios) {
                Path arquivo = pasta.resolve(c.arquivo() + ".dot");
                Files.writeString(arquivo, DesenhoGrafo.paraDot(c.grafo(), c.titulo()));
                System.out.println("  gravado: " + arquivo.toAbsolutePath());
            }
            System.out.println("Para gerar imagens (requer Graphviz): dot -Tpng grafos/A1_deadlock.dot -o A1.png");
        } catch (IOException e) {
            System.out.println("Não foi possível gravar os arquivos: " + e.getMessage());
        }
    }
}
