package minikernel.sync;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Semáforos contadores "lógicos": o bloqueio real é feito pelo escalonador. */
public class SemaforosLogicos implements Sincronizacao {
    private final Map<String, Integer> contadores = new HashMap<>();
    private final Map<String, Deque<Integer>> filas = new HashMap<>();

    public void criar(String nome, int valorInicial) {
        contadores.put(nome, valorInicial);
    }

    @Override
    public boolean wait(int pid, String nome) {
        int c = contadores.getOrDefault(nome, 0);
        if (c > 0) {
            contadores.put(nome, c - 1);
            return true;
        }
        filas.computeIfAbsent(nome, k -> new ArrayDeque<>()).add(pid);
        return false;
    }

    @Override
    public List<Integer> signal(String nome) {
        Deque<Integer> fila = filas.get(nome);
        if (fila != null && !fila.isEmpty()) return List.of(fila.poll()); // passa o "token" direto
        contadores.merge(nome, 1, Integer::sum);
        return List.of();
    }
}
