package minikernel.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bloco de Controle de Processo (PCB): o "contrato" comum entre os módulos. */
public class Processo {
    public final int pid;
    public final int prioridadeBase;        // menor número = maior prioridade
    public final int numPaginas;            // usado pelo módulo de memória
    public final int[] demandaMaxima;       // usado pelo Banqueiro (matriz Max)
    public final Deque<Instrucao> programa; // "código" do processo

    public Estado estado = Estado.NOVO;     // quem muda: o Escalonador
    public int prioridadeEfetiva;           // o aging altera este campo
    public int quantumRestante;
    public int cpuRestante = -1;            // progresso da instrução Cpu atual

    // métricas
    public int instanteChegada, instanteFim, tempoEmEspera;

    public Processo(int pid, int prioridade, int numPaginas,
                    int[] demandaMaxima, List<Instrucao> programa) {
        this.pid = pid;
        this.prioridadeBase = prioridade;
        this.prioridadeEfetiva = prioridade;
        this.numPaginas = numPaginas;
        this.demandaMaxima = demandaMaxima;
        this.programa = new ArrayDeque<>(programa);
    }

    public int turnaround() {
        return instanteFim - instanteChegada;
    }
}
