package minikernel.mem;

import java.util.Map;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 3: Memória virtual e MMU (tradução de endereços, page fault, FIFO e LRU). */
public class ModuloMemoria implements Modulo {

    @Override
    public String nome() {
        return "Memória Virtual e MMU (Page Fault, FIFO e LRU)";
    }

    @Override
    public void executar(Terminal t) {
        while (true) {
            int op = t.menu("MODULO 3 | Memoria Virtual e MMU", "Voltar ao menu principal",
                    "Tradução de endereços virtual -> físico (MMU + tabela de páginas)",
                    "Comparar FIFO x LRU (string de referência clássica)",
                    "Anomalia de Belady (FIFO com mais quadros pode piorar)");
            switch (op) {
                case 0 -> { return; }
                case 1 -> traducao(t);
                case 2 -> comparar(t);
                case 3 -> belady();
                default -> { }
            }
            t.pausar();
        }
    }

    // ---------------------------------------------------------------- opção 1

    private void traducao(Terminal t) {
        t.subtitulo("Tradução de endereços (MMU)");
        int quadros = t.lerInt("Número de quadros de memória física", 3, 1);
        var mem = new MemoriaPaginada(quadros, Politica.LRU);
        mem.criarEspaco(1, 4);
        int[] enderecos = {0x0010, 0x0105, 0x0210, 0x0012, 0x0301, 0x0108, 0x0020};

        System.out.println("\nPágina de " + MemoriaPaginada.TAMANHO_PAGINA + " bytes | processo com 4 páginas (1 KB virtual) | "
                + quadros + " quadro(s) físico(s) | substituição LRU");
        System.out.println("Endereço virtual = (número da página, deslocamento). O MMU consulta a tabela de páginas;");
        System.out.println("se a página não está na memória, ocorre PAGE FAULT e o SO a carrega em um quadro.\n");
        System.out.printf("  %-10s | %-4s | %-7s | %-58s | %s%n", "End. virt.", "Pág", "Desloc.", "Resultado", "End. físico");
        System.out.println("  " + "-".repeat(102));

        for (int end : enderecos) {
            int pag = end / MemoriaPaginada.TAMANHO_PAGINA;
            int off = end % MemoriaPaginada.TAMANHO_PAGINA;
            var r = mem.acessar(1, end);
            String resultado = r.pageFault()
                    ? "PAGE FAULT -> pág " + pag + " carregada no quadro " + r.quadro()
                      + (r.paginaSubstituida() != null ? " (expulsa pág " + r.paginaSubstituida() + ")" : "")
                    : "HIT (pág " + pag + " no quadro " + r.quadro() + ")";
            System.out.printf("  0x%04X     | %-4d | %-7d | %-58s | 0x%04X%n", end, pag, off, resultado,
                    r.quadro() * MemoriaPaginada.TAMANHO_PAGINA + off);
        }

        System.out.println("\nTabela de páginas final do processo:");
        Map<Integer, Integer> tabela = mem.tabelaDePaginas(1);
        for (int pag = 0; pag < 4; pag++) {
            System.out.printf("  pág %d -> %s%n", pag, tabela.containsKey(pag) ? "quadro " + tabela.get(pag) : "(não residente)");
        }
        System.out.printf("%nFaltas de página: %d | Acertos: %d | Taxa de faltas: %.0f%%%n",
                mem.faltas(), mem.acertos(), 100.0 * mem.faltas() / (mem.faltas() + mem.acertos()));
    }

    // ---------------------------------------------------------------- opção 2

    private void comparar(Terminal t) {
        t.subtitulo("FIFO x LRU");
        int quadros = t.lerInt("Número de quadros", 3, 1);
        int[] refs = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
        System.out.println("\nString de referência (páginas acessadas em ordem):");
        System.out.println("  " + java.util.Arrays.toString(refs));

        System.out.println("\n>>> FIFO: expulsa a página que está há mais tempo na memória");
        int fifo = executarReferencias(refs, quadros, Politica.FIFO, 8, true);
        System.out.println("\n>>> LRU: expulsa a página usada há mais tempo (menos recentemente)");
        int lru = executarReferencias(refs, quadros, Politica.LRU, 8, true);

        System.out.println("\nResumo com " + quadros + " quadro(s) e " + refs.length + " acessos:");
        System.out.printf("  %-6s | %-6s | %s%n", "Política", "Faltas", "Taxa de faltas");
        System.out.printf("  %-8s | %-6d | %.0f%%%n", "FIFO", fifo, 100.0 * fifo / refs.length);
        System.out.printf("  %-8s | %-6d | %.0f%%%n", "LRU", lru, 100.0 * lru / refs.length);
    }

    // ---------------------------------------------------------------- opção 3

    private void belady() {
        int[] refs = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        System.out.println("\nAnomalia de Belady: com FIFO, aumentar o número de quadros pode AUMENTAR as faltas.");
        System.out.println("String de referência: " + java.util.Arrays.toString(refs));

        System.out.println("\n>>> FIFO com 3 quadros");
        int f3 = executarReferencias(refs, 3, Politica.FIFO, 6, true);
        System.out.println("\n>>> FIFO com 4 quadros");
        int f4 = executarReferencias(refs, 4, Politica.FIFO, 6, true);
        int l3 = executarReferencias(refs, 3, Politica.LRU, 6, false);
        int l4 = executarReferencias(refs, 4, Politica.LRU, 6, false);

        System.out.println("\nResumo (número de faltas de página):");
        System.out.printf("  %-8s | %-6s | %s%n", "Quadros", "FIFO", "LRU");
        System.out.printf("  %-8d | %-6d | %d%n", 3, f3, l3);
        System.out.printf("  %-8d | %-6d | %d%n", 4, f4, l4);
        System.out.println(f4 > f3
                ? "\nCom FIFO, mais memória (4 quadros) causou MAIS faltas: anomalia de Belady.\n"
                  + "O LRU não sofre disso: mais quadros nunca pioram o resultado (é um algoritmo de pilha)."
                : "\nNeste cenário a anomalia não se manifestou.");
    }

    // ---------------------------------------------------------------- comum

    /** Simula uma string de referência e retorna o número de faltas. */
    private int executarReferencias(int[] refs, int quadros, Politica politica, int paginas, boolean detalhar) {
        var mem = new MemoriaPaginada(quadros, politica);
        mem.criarEspaco(1, paginas);
        if (detalhar) {
            System.out.printf("  %-4s | %-" + (quadros * 3 + 3) + "s | %s%n", "Ref", "Quadros", "Resultado");
            System.out.println("  " + "-".repeat(30 + quadros * 3));
        }
        for (int pagina : refs) {
            var r = mem.acessar(1, pagina * MemoriaPaginada.TAMANHO_PAGINA);
            if (detalhar) {
                StringBuilder q = new StringBuilder("[");
                for (String c : mem.ocupacaoQuadros()) q.append(String.format("%2s ", c));
                q.append("]");
                String res = r.pageFault()
                        ? "FALTA" + (r.paginaSubstituida() != null ? " (expulsa pág " + r.paginaSubstituida() + ")" : "")
                        : "hit";
                System.out.printf("  %-4d | %-" + (quadros * 3 + 3) + "s | %s%n", pagina, q, res);
            }
        }
        if (detalhar) System.out.printf("  => %d falta(s) em %d acessos%n", mem.faltas(), refs.length);
        return mem.faltas();
    }
}
