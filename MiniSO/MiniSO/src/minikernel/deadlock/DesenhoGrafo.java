package minikernel.deadlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Desenha o grafo de alocação de recursos em ASCII (e o exporta para Graphviz/DOT).
 *
 * Notação:  (P1) = processo   [R1] = recurso
 *   ------>  aresta de ALOCAÇÃO   (recurso -> processo)
 *   ......>  aresta de ESPERA     (processo -> recurso)
 * Ciclos (deadlock) são desenhados como um anel; o restante, como cadeias.
 */
public final class DesenhoGrafo {
    private static final int GAP = 9;            // largura do espaço entre dois nós numa linha
    private static final int VERTICAL = 3;       // linhas de conector entre as duas fileiras do anel

    private DesenhoGrafo() {}

    public static List<String> legenda() {
        return List.of(
                "  Legenda:  (P1) processo    [R1] recurso",
                "            ------>   recurso ALOCADO ao processo   (R -> P)",
                "            ......>   processo ESPERANDO o recurso  (P -> R)");
    }

    /** Desenho completo: um anel para cada ciclo e cadeias para o resto do grafo. */
    public static List<String> desenhar(GrafoAlocacao g) {
        List<String> saida = new ArrayList<>();
        GrafoAlocacao resto = g.copia();
        List<List<String>> ciclos = resto.extrairCiclos(); // 'resto' fica sem as arestas dos ciclos

        int numero = 1;
        for (List<String> ciclo : ciclos) {
            saida.add("  CICLO " + numero++ + " (DEADLOCK): " + String.join(" -> ", ciclo));
            saida.add("");
            saida.addAll(anel(ciclo.subList(0, ciclo.size() - 1)));
            saida.add("");
        }

        List<List<String>> caminhos = caminhos(resto);
        if (!caminhos.isEmpty()) {
            saida.add(ciclos.isEmpty() ? "  Grafo SEM ciclos (sem deadlock):" : "  Demais arestas (fora dos ciclos):");
            saida.add("");
            for (List<String> c : caminhos) saida.add("    " + cadeia(c));
            saida.add("");
        }

        List<String> livres = g.nosSemArestas();
        if (!livres.isEmpty()) {
            saida.add("  Sem nenhuma aresta (ociosos/livres): " + String.join(", ", livres));
            saida.add("");
        }
        if (saida.isEmpty()) saida.add("  (grafo vazio)");
        return saida;
    }

    // ------------------------------------------------------------------ anel

    /** Anel para ciclos de tamanho par >= 4 (o caso de grafos processo/recurso); senão, uma linha fechada. */
    private static List<String> anel(List<String> nos) {
        int n = nos.size();
        if (n < 4 || n % 2 != 0) {
            List<String> fechado = new ArrayList<>(nos);
            fechado.add(nos.get(0));
            return List.of("    " + cadeia(fechado));
        }
        int k = n / 2;
        int w = nos.stream().mapToInt(x -> texto(x).length()).max().orElse(4);
        int passo = w + GAP;
        int largura = (k - 1) * passo + w;
        int altura = VERTICAL + 2;
        char[][] tela = new char[altura][largura];
        for (char[] linha : tela) Arrays.fill(linha, ' ');

        // fileira de cima, da esquerda para a direita: nos[0..k-1]
        for (int i = 0; i < k; i++) {
            escrever(tela, 0, i * passo, texto(nos.get(i)));
            if (i < k - 1) escrever(tela, 0, i * passo + w, setaHorizontal(nos.get(i), true));
        }
        // fileira de baixo, da esquerda para a direita: nos[n-1], nos[n-2], ..., nos[k]
        for (int j = 0; j < k; j++) {
            escrever(tela, altura - 1, j * passo, texto(nos.get(n - 1 - j)));
            if (j < k - 1) escrever(tela, altura - 1, j * passo + w, setaHorizontal(nos.get(n - 2 - j), false));
        }
        // conector da direita (desce): nos[k-1] -> nos[k]
        int xd = (k - 1) * passo + w / 2;
        char vd = verticalChar(nos.get(k - 1));
        for (int y = 1; y <= altura - 3; y++) tela[y][xd] = vd;
        tela[altura - 2][xd] = 'v';
        // conector da esquerda (sobe): nos[n-1] -> nos[0]
        int xe = w / 2;
        char ve = verticalChar(nos.get(n - 1));
        tela[1][xe] = '^';
        for (int y = 2; y <= altura - 2; y++) tela[y][xe] = ve;

        List<String> linhas = new ArrayList<>();
        for (char[] linha : tela) linhas.add("    " + new String(linha).stripTrailing());
        return linhas;
    }

    // ---------------------------------------------------------------- cadeias

    private static String cadeia(List<String> nos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nos.size(); i++) {
            sb.append(texto(nos.get(i)));
            if (i < nos.size() - 1) sb.append(setaHorizontal(nos.get(i), true));
        }
        return sb.toString();
    }

    /** Todos os caminhos maximais de um grafo SEM ciclos, partindo dos nós sem aresta de entrada. */
    private static List<List<String>> caminhos(GrafoAlocacao g) {
        Map<String, List<String>> adj = g.adjacencia();
        Map<String, Integer> entrada = new HashMap<>();
        adj.forEach((no, dests) -> {
            entrada.putIfAbsent(no, 0);
            dests.forEach(d -> entrada.merge(d, 1, Integer::sum));
        });
        List<List<String>> resultado = new ArrayList<>();
        for (String no : adj.keySet()) {
            if (entrada.get(no) == 0 && !adj.get(no).isEmpty()) {
                percorrer(no, adj, new ArrayList<>(), resultado);
            }
        }
        return resultado;
    }

    private static void percorrer(String no, Map<String, List<String>> adj, List<String> atual, List<List<String>> saida) {
        atual.add(no);
        List<String> proximos = adj.getOrDefault(no, List.of());
        if (proximos.isEmpty()) {
            saida.add(new ArrayList<>(atual));
        } else {
            for (String p : proximos) percorrer(p, adj, atual, saida);
        }
        atual.remove(atual.size() - 1);
    }

    // ------------------------------------------------------------ primitivas

    private static String texto(String no) {
        return GrafoAlocacao.ehProcesso(no) ? "(" + no + ")" : "[" + no + "]";
    }

    /** Aresta que SAI de 'origem': espera (pontilhada) se origem é processo, alocação (cheia) se é recurso. */
    private static String setaHorizontal(String origem, boolean paraDireita) {
        char c = GrafoAlocacao.ehProcesso(origem) ? '.' : '-';
        String miolo = String.valueOf(c).repeat(GAP - 3);
        return paraDireita ? " " + miolo + "> " : " <" + miolo + " ";
    }

    private static char verticalChar(String origem) {
        return GrafoAlocacao.ehProcesso(origem) ? ':' : '|';
    }

    private static void escrever(char[][] tela, int y, int x, String s) {
        for (int i = 0; i < s.length(); i++) tela[y][x + i] = s.charAt(i);
    }

    // -------------------------------------------------------------------- DOT

    /** Gera o grafo em formato Graphviz DOT. As arestas que fazem parte de ciclos saem em vermelho. */
    public static String paraDot(GrafoAlocacao g, String titulo) {
        Set<String> emCiclo = new HashSet<>();
        for (List<String> ciclo : g.copia().extrairCiclos()) {
            for (int i = 0; i < ciclo.size() - 1; i++) emCiclo.add(ciclo.get(i) + "->" + ciclo.get(i + 1));
        }
        Map<String, List<String>> adj = g.adjacencia();

        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  label=\"").append(titulo.replace("\"", "'")).append("\";\n");
        sb.append("  labelloc=t;\n  rankdir=LR;\n  node [fontname=\"Helvetica\"];\n");
        for (String no : adj.keySet()) {
            sb.append("  ").append(no).append(GrafoAlocacao.ehProcesso(no) ? " [shape=circle];\n" : " [shape=box];\n");
        }
        adj.forEach((origem, destinos) -> {
            for (String destino : destinos) {
                boolean espera = GrafoAlocacao.ehProcesso(origem);
                sb.append("  ").append(origem).append(" -> ").append(destino)
                        .append(" [label=\"").append(espera ? "espera" : "alocado").append("\"")
                        .append(espera ? ", style=dashed" : "")
                        .append(emCiclo.contains(origem + "->" + destino) ? ", color=red, penwidth=2.0" : "")
                        .append("];\n");
            }
        });
        return sb.append("}\n").toString();
    }
}
