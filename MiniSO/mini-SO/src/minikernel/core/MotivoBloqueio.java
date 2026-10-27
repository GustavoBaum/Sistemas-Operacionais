package minikernel.core;

/** Por que um processo está bloqueado. */
public enum MotivoBloqueio {
    PAGE_FAULT, RECURSO, SEMAFORO, DISCO
}
