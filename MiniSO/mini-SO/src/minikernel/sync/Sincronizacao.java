package minikernel.sync;

import java.util.List;

/** Módulo 1: Sincronização, versão "lógica" (fila de PIDs) para integração. */
public interface Sincronizacao {
    boolean wait(int pid, String semaforo);  // true = prossegue; false = deve bloquear
    List<Integer> signal(String semaforo);   // pids a acordar
}
