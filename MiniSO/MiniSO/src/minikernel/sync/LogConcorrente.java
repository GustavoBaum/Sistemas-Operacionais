package minikernel.sync;

/** Log com marca de tempo (ms desde a criação), seguro para uso por várias threads. */
final class LogConcorrente {
    private final long inicio = System.nanoTime();

    void log(String quem, String mensagem) {
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.printf("[%5d ms] %-12s %s%n", ms, quem, mensagem);
    }
}
