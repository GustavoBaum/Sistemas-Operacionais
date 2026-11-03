package minikernel.deadlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo de alocação de recursos (um recurso com uma única instância cada).
 *   Processo -> Recurso : o processo está ESPERANDO o recurso (requisição)
 *   Recurso  -> Processo : o recurso está ALOCADO ao processo
 * Um ciclo no grafo significa deadlock.
 * Convenção de nomes: processos começam com "P" e recursos com "R".
 */
public class GrafoAlocacao {
    private final Map<String, Set<String>> arestas = new LinkedHashMap<>();

    public static boolean ehProcesso(String no) {
        return no.startsWith("P");
    }

    public void alocar(String recurso, String processo) {
        aresta(recurso, processo);
    }

    public void requisitar(String processo, String recurso) {
        aresta(processo, recurso);
    }

    /** O processo conseguiu o recurso: a aresta de espera vira aresta de alocação. */
    public void concederRequisicao(String processo, String recurso) {
        removerAresta(processo, recurso);
        alocar(recurso, processo);
    }

    public void removerAresta(String de, String para) {
        Set<String> destinos = arestas.get(de);
        if (destinos != null) destinos.remove(para);
    }

    /** Aborta o processo: remove o nó e todas as arestas (libera os recursos que ele detinha). */
    public void removerProcesso(String processo) {
        arestas.remove(processo);
        arestas.values().forEach(s -> s.remove(processo));
    }

    public GrafoAlocacao copia() {
        GrafoAlocacao c = new GrafoAlocacao();
        arestas.forEach((no, destinos) -> c.arestas.put(no, new LinkedHashSet<>(destinos)));
        return c;
    }

    /** Cópia da lista de adjacência (nó -> sucessores), na ordem de inserção. */
    public Map<String, List<String>> adjacencia() {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        arestas.forEach((no, destinos) -> copia.put(no, new ArrayList<>(destinos)));
        return copia;
    }

    /** Nós que não têm nenhuma aresta (nem entrando, nem saindo). */
    public List<String> nosSemArestas() {
        Set<String> alvo = new HashSet<>();
        arestas.values().forEach(alvo::addAll);
        List<String> livres = new ArrayList<>();
        arestas.forEach((no, destinos) -> {
            if (destinos.isEmpty() && !alvo.contains(no)) livres.add(no);
        });
        return livres;
    }

    /** Devolve um ciclo (ex.: [R1, P1, R2, P2, R1]) ou lista vazia se não houver. */
    public List<String> encontrarCiclo() {
        Set<String> visitados = new HashSet<>();
        for (String no : new ArrayList<>(arestas.keySet())) {
            List<String> ciclo = dfs(no, visitados, new ArrayList<>());
            if (ciclo != null) return ciclo;
        }
        return List.of();
    }

    /**
     * DESTRUTIVO: encontra ciclos um a um e remove as arestas de cada um deste grafo.
     * Ao final o grafo fica sem ciclos. Use copia() antes se precisar do original.
     */
    public List<List<String>> extrairCiclos() {
        List<List<String>> ciclos = new ArrayList<>();
        List<String> ciclo;
        while (!(ciclo = encontrarCiclo()).isEmpty()) {
            for (int i = 0; i < ciclo.size() - 1; i++) removerAresta(ciclo.get(i), ciclo.get(i + 1));
            ciclos.add(ciclo);
        }
        return ciclos;
    }

    private List<String> dfs(String no, Set<String> visitados, List<String> caminho) {
        int idx = caminho.indexOf(no);
        if (idx >= 0) { // voltamos a um nó do caminho atual: achamos o ciclo
            List<String> ciclo = new ArrayList<>(caminho.subList(idx, caminho.size()));
            ciclo.add(no);
            return ciclo;
        }
        if (!visitados.add(no)) return null; // já explorado sem ciclo
        caminho.add(no);
        for (String prox : new ArrayList<>(arestas.getOrDefault(no, Set.of()))) {
            List<String> ciclo = dfs(prox, visitados, caminho);
            if (ciclo != null) return ciclo;
        }
        caminho.remove(caminho.size() - 1);
        return null;
    }

    /** Lista as arestas em texto, uma por linha. */
    public List<String> descrever() {
        List<String> linhas = new ArrayList<>();
        arestas.forEach((origem, destinos) -> {
            for (String destino : destinos) {
                linhas.add(String.format("  %s -> %s   (%s)", origem, destino,
                        ehProcesso(origem) ? origem + " ESPERA " + destino : destino + " está COM " + origem));
            }
        });
        return linhas;
    }

    private void aresta(String de, String para) {
        arestas.computeIfAbsent(de, k -> new LinkedHashSet<>()).add(para);
        arestas.computeIfAbsent(para, k -> new LinkedHashSet<>());
    }
}
