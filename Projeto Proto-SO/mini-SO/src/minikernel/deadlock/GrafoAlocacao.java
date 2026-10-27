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
 */
public class GrafoAlocacao {
    private final Map<String, Set<String>> arestas = new LinkedHashMap<>();

    public void alocar(String recurso, String processo) {
        aresta(recurso, processo);
    }

    public void requisitar(String processo, String recurso) {
        aresta(processo, recurso);
    }

    /** O processo conseguiu o recurso: a aresta de espera vira aresta de alocação. */
    public void concederRequisicao(String processo, String recurso) {
        Set<String> destinos = arestas.get(processo);
        if (destinos != null) destinos.remove(recurso);
        alocar(recurso, processo);
    }

    /** Aborta o processo: remove o nó e todas as arestas (libera os recursos que ele detinha). */
    public void removerProcesso(String processo) {
        arestas.remove(processo);
        arestas.values().forEach(s -> s.remove(processo));
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
                boolean espera = origem.startsWith("P");
                linhas.add(String.format("  %s -> %s   (%s)", origem, destino,
                        espera ? origem + " ESPERA " + destino : destino + " está COM " + origem));
            }
        });
        return linhas;
    }

    private void aresta(String de, String para) {
        arestas.computeIfAbsent(de, k -> new LinkedHashSet<>()).add(para);
        arestas.computeIfAbsent(para, k -> new LinkedHashSet<>());
    }
}
