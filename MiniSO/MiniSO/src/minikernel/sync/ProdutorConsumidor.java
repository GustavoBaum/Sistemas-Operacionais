package minikernel.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Problema do Produtor/Consumidor com buffer circular limitado.
 * Três semáforos, nenhuma espera ocupada:
 *   vazias (conta posições livres), cheias (conta itens disponíveis) e mutex (exclusão mútua).
 */
final class ProdutorConsumidor {
    private static final int TAMANHO = 4;
    private static final int PRODUTORES = 2;
    private static final int CONSUMIDORES = 2;
    private static final int ITENS_POR_PRODUTOR = 5;

    private final LogConcorrente log = new LogConcorrente();
    private final int[] buffer = new int[TAMANHO]; // 0 = posição vazia
    private final Semaphore vazias = new Semaphore(TAMANHO);
    private final Semaphore cheias = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1);
    private int entrada, saida;

    static void simular() throws InterruptedException {
        System.out.println("""

                Buffer circular de %d posições | %d produtores x %d itens | %d consumidores
                Semáforos: vazias(%d) = posições livres, cheias(0) = itens prontos, mutex(1) = exclusão mútua.
                Se o buffer está cheio, o produtor BLOQUEIA em wait(vazias); se está vazio, o consumidor
                BLOQUEIA em wait(cheias). Nenhuma thread fica em laço checando o buffer (sem busy waiting).
                """.formatted(TAMANHO, PRODUTORES, ITENS_POR_PRODUTOR, CONSUMIDORES, TAMANHO));
        new ProdutorConsumidor().executar();
    }

    private void executar() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= PRODUTORES; i++) {
            final int id = i;
            threads.add(new Thread(() -> produtor(id), "Produtor-" + id));
        }
        int porConsumidor = PRODUTORES * ITENS_POR_PRODUTOR / CONSUMIDORES;
        for (int i = 1; i <= CONSUMIDORES; i++) {
            final int id = i;
            threads.add(new Thread(() -> consumidor(id, porConsumidor), "Consumidor-" + id));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();
        System.out.println("\nTodas as threads terminaram: " + (PRODUTORES * ITENS_POR_PRODUTOR)
                + " itens produzidos e consumidos, buffer final " + estado() + ".");
    }

    private void produtor(int id) {
        String nome = "Produtor-" + id;
        try {
            for (int k = 1; k <= ITENS_POR_PRODUTOR; k++) {
                int item = id * 100 + k;
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150)); // "produzindo"
                if (vazias.availablePermits() == 0) log.log(nome, "buffer CHEIO -> bloqueia em wait(vazias)");
                vazias.acquire();
                mutex.acquire();
                int pos = entrada;
                buffer[pos] = item;
                entrada = (entrada + 1) % TAMANHO;
                log.log(nome, "colocou " + item + " na posição " + pos + "   buffer " + estado());
                mutex.release();
                cheias.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void consumidor(int id, int quantidade) {
        String nome = "Consumidor-" + id;
        try {
            for (int k = 1; k <= quantidade; k++) {
                if (cheias.availablePermits() == 0) log.log(nome, "buffer VAZIO -> bloqueia em wait(cheias)");
                cheias.acquire();
                mutex.acquire();
                int pos = saida;
                int item = buffer[pos];
                buffer[pos] = 0;
                saida = (saida + 1) % TAMANHO;
                log.log(nome, "retirou  " + item + " da posição " + pos + "   buffer " + estado());
                mutex.release();
                vazias.release();
                Thread.sleep(ThreadLocalRandom.current().nextInt(150, 300)); // "consumindo"
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Deve ser chamado com o mutex em mãos (ou quando não há mais threads ativas). */
    private String estado() {
        StringBuilder sb = new StringBuilder("[");
        Arrays.stream(buffer).forEach(v -> sb.append(String.format("%4s", v == 0 ? "." : v)));
        return sb.append(" ]").toString();
    }
}
