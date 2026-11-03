package minikernel.deadlock;

import java.util.List;

/** Módulo 4: Deadlocks (Algoritmo do Banqueiro + detecção). */
public interface GerenciadorRecursos {
    void registrar(int pid, int[] demandaMaxima);
    boolean solicitar(int pid, int[] quantidades); // true = concedido (estado seguro)
    void liberar(int pid, int[] quantidades);
    void liberarTodos(int pid);
    List<Integer> detectarDeadlock();              // pids envolvidos em deadlock
}
