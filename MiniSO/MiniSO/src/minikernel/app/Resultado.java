package minikernel.app;

import minikernel.mem.Politica;

/** Métricas de uma simulação. */
public record Resultado(Politica politica, int ticks, double tme, double tmt,
                        int faltas, int acertos, int blocosLivres) {}
