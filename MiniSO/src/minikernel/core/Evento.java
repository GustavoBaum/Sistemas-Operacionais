package minikernel.core;

/** Evento para log/visualização. pid = -1 quando não há processo associado. */
public record Evento(int tick, String origem, int pid, String descricao) {}
