package minikernel.sched;

import java.util.Optional;
import minikernel.core.MotivoBloqueio;
import minikernel.core.Processo;

/** Módulo 2: Escalonamento. Dono das transições de estado dos processos. */
public interface Escalonador {
    void admitir(Processo p);                         // NOVO -> PRONTO
    Optional<Processo> proximo();                     // PRONTO -> EXECUTANDO
    boolean cobrarTick(Processo p);                   // consome quantum; true = deve ser preemptado
    void aoFimDoQuantum(Processo p);                  // EXECUTANDO -> PRONTO
    void bloquear(Processo p, MotivoBloqueio motivo, int ticks); // ticks = 0: até ser acordado
    void desbloquear(Processo p);                     // BLOQUEADO -> PRONTO
    void desbloquearPorMotivo(MotivoBloqueio motivo); // acorda todos para reavaliar (ex.: RECURSO)
    void terminar(Processo p);                        // -> TERMINADO
    void tick();                                      // aging, contagem de espera, bloqueios temporizados
}
