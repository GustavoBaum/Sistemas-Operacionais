package minikernel.deadlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Algoritmo do Banqueiro (Dijkstra) + detecção por redução da matriz de alocação. */
public class Banqueiro implements GerenciadorRecursos {

    public enum ResultadoPedido { CONCEDIDO, EXCEDE_MAXIMO, SEM_RECURSOS, ESTADO_INSEGURO }

    private final int[] disponivel;
    private final Map<Integer, int[]> max = new HashMap<>();
    private final Map<Integer, int[]> alocado = new HashMap<>();
    private final Map<Integer, int[]> pendente = new HashMap<>();

    public Banqueiro(int... totalPorTipo) {
        this.disponivel = totalPorTipo.clone();
    }

    @Override
    public void registrar(int pid, int[] demandaMaxima) {
        max.put(pid, demandaMaxima.clone());
        alocado.put(pid, new int[disponivel.length]);
    }

    /** Define a alocação inicial de um processo (retira os recursos do Available). */
    public void definirAlocacaoInicial(int pid, int[] alocacao) {
        int[] a = alocado.get(pid);
        for (int i = 0; i < a.length; i++) disponivel[i] += a[i]; // devolve o que havia
        for (int i = 0; i < a.length; i++) {
            if (alocacao[i] > max.get(pid)[i] || alocacao[i] > disponivel[i]) {
                throw new IllegalArgumentException("Alocação inicial inválida para P" + pid);
            }
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = alocacao[i];
            disponivel[i] -= alocacao[i];
        }
    }

    /** Matriz Need de um processo: Max - Alocado. */
    public int[] necessidade(int pid) {
        int[] need = new int[disponivel.length];
        for (int i = 0; i < need.length; i++) need[i] = max.get(pid)[i] - alocado.get(pid)[i];
        return need;
    }

    public int[] disponivel() {
        return disponivel.clone();
    }

    @Override
    public boolean solicitar(int pid, int[] req) {
        ResultadoPedido r = solicitarComMotivo(pid, req);
        if (r == ResultadoPedido.EXCEDE_MAXIMO) {
            throw new IllegalArgumentException("P" + pid + " excede o máximo declarado");
        }
        return r == ResultadoPedido.CONCEDIDO;
    }

    /** Executa os 3 passos do Banqueiro e informa qual deles barrou o pedido (se algum). */
    public ResultadoPedido solicitarComMotivo(int pid, int[] req) {
        int[] a = alocado.get(pid);
        if (!cabe(req, necessidade(pid))) return ResultadoPedido.EXCEDE_MAXIMO;   // passo 1: Request <= Need
        if (!cabe(req, disponivel)) {                                              // passo 2: Request <= Available
            pendente.put(pid, req.clone());
            return ResultadoPedido.SEM_RECURSOS;
        }
        // passo 3: alocação provisória + teste de segurança
        for (int i = 0; i < req.length; i++) { disponivel[i] -= req[i]; a[i] += req[i]; }
        if (!estadoSeguro()) {
            for (int i = 0; i < req.length; i++) { disponivel[i] += req[i]; a[i] -= req[i]; } // rollback
            pendente.put(pid, req.clone());
            return ResultadoPedido.ESTADO_INSEGURO;
        }
        pendente.remove(pid);
        return ResultadoPedido.CONCEDIDO;
    }

    public boolean estadoSeguro() {
        return sequenciaSegura().isPresent();
    }

    /** Algoritmo de segurança: devolve uma sequência segura de PIDs, se existir. */
    public Optional<List<Integer>> sequenciaSegura() {
        int[] work = disponivel.clone();
        Set<Integer> restantes = new TreeSet<>(max.keySet());
        List<Integer> sequencia = new ArrayList<>();
        boolean progrediu = true;
        while (progrediu && !restantes.isEmpty()) {
            progrediu = false;
            for (var it = restantes.iterator(); it.hasNext(); ) {
                int pid = it.next();
                if (cabe(necessidade(pid), work)) {
                    for (int i = 0; i < work.length; i++) work[i] += alocado.get(pid)[i];
                    sequencia.add(pid);
                    it.remove();
                    progrediu = true;
                }
            }
        }
        return restantes.isEmpty() ? Optional.of(sequencia) : Optional.empty();
    }

    private static boolean cabe(int[] necessidade, int[] work) {
        for (int i = 0; i < work.length; i++) {
            if (necessidade[i] > work[i]) return false;
        }
        return true;
    }

    @Override
    public void liberar(int pid, int[] qtd) {
        int[] a = alocado.get(pid);
        for (int i = 0; i < qtd.length; i++) {
            int x = Math.min(qtd[i], a[i]);
            a[i] -= x;
            disponivel[i] += x;
        }
    }

    @Override
    public void liberarTodos(int pid) {
        int[] a = alocado.remove(pid);
        if (a != null) for (int i = 0; i < a.length; i++) disponivel[i] += a[i];
        max.remove(pid);
        pendente.remove(pid);
    }

    /**
     * Detecção: um processo "pode terminar" se não tem pedido pendente ou se o pedido cabe em work.
     * Com o Banqueiro ativo o estado é sempre seguro, então o resultado esperado é lista vazia;
     * o método existe para demonstrar a detecção e para uso com uma política menos restritiva.
     */
    @Override
    public List<Integer> detectarDeadlock() {
        int[] work = disponivel.clone();
        Set<Integer> restantes = new TreeSet<>(alocado.keySet());
        boolean progrediu = true;
        while (progrediu && !restantes.isEmpty()) {
            progrediu = false;
            for (var it = restantes.iterator(); it.hasNext(); ) {
                int pid = it.next();
                int[] pend = pendente.get(pid);
                if (pend == null || cabe(pend, work)) {
                    for (int i = 0; i < work.length; i++) work[i] += alocado.get(pid)[i];
                    it.remove();
                    progrediu = true;
                }
            }
        }
        return new ArrayList<>(restantes);
    }

    /** Tabela Alocado / Max / Need + Available, para exibição. */
    public String estadoComoTexto() {
        StringBuilder sb = new StringBuilder();
        StringBuilder nomes = new StringBuilder();
        for (int i = 0; i < disponivel.length; i++) nomes.append((char) ('A' + i)).append(' ');
        String cab = nomes.toString().trim();
        sb.append(String.format("  %-5s| %-10s| %-10s| %-10s%n", "Proc", "Alocado", "Max", "Need"));
        sb.append(String.format("  %-5s| %-10s| %-10s| %-10s%n", "", cab, cab, cab));
        sb.append("  ").append("-".repeat(40)).append('\n');
        for (int pid : new TreeSet<>(max.keySet())) {
            sb.append(String.format("  P%-4d| %-10s| %-10s| %-10s%n", pid,
                    texto(alocado.get(pid)), texto(max.get(pid)), texto(necessidade(pid))));
        }
        sb.append("  Available: ").append(texto(disponivel)).append("   (recursos ").append(cab).append(")");
        return sb.toString();
    }

    public static String texto(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(v[i]);
        }
        return sb.toString();
    }
}
