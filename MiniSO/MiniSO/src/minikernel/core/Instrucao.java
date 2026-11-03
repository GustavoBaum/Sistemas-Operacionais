package minikernel.core;

/** "Chamadas de sistema" que um processo simulado pode executar. */
public sealed interface Instrucao {
    record Cpu(int ciclos) implements Instrucao {}
    /** Operação de E/S genérica: o processo fica bloqueado por 'ticks' ticks. */
    record Es(int ticks) implements Instrucao {}
    record AcessoMemoria(int enderecoVirtual) implements Instrucao {}
    record PedirRecursos(int[] quantidades) implements Instrucao {}
    record LiberarRecursos(int[] quantidades) implements Instrucao {}
    record LerArquivo(String nome) implements Instrucao {}
    record EscreverArquivo(String nome, int blocos) implements Instrucao {}
    record Wait(String semaforo) implements Instrucao {}
    record Signal(String semaforo) implements Instrucao {}
}
