package minikernel.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Jantar dos Filósofos: versão segura (semáforo "sala") e versão ingênua (que entra em impasse). */
final class JantarFilosofos {
    private static final int N = 5;
    private static final int REFEICOES = 3;
    private static final String[] NOMES = {"Sócrates", "Platão", "Aristóteles", "Descartes", "Kant"};

    private JantarFilosofos() {}

    /** Solução com semáforo contador: no máximo N-1 filósofos disputam os garfos ao mesmo tempo. */
    static void simularSeguro() throws InterruptedException {
        System.out.println("""

                %d filósofos, %d garfos (um semáforo binário por garfo), %d refeições cada.
                Solução: um semáforo contador "sala" com N-1 = %d permissões. Com no máximo %d filósofos
                disputando os garfos, sempre sobra um garfo para alguém comer: a espera circular é impossível.
                """.formatted(N, N, REFEICOES, N - 1, N - 1));

        LogConcorrente log = new LogConcorrente();
        Semaphore[] garfos = novosGarfos();
        Semaphore sala = new Semaphore(N - 1);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            final int id = i;
            threads.add(new Thread(() -> {
                String nome = NOMES[id];
                int esq = id, dir = (id + 1) % N;
                try {
                    for (int r = 1; r <= REFEICOES; r++) {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                        log.log(nome, "com fome (refeição " + r + ")");
                        sala.acquire();
                        garfos[esq].acquire();
                        garfos[dir].acquire();
                        log.log(nome, "pegou garfos " + esq + " e " + dir + " -> COMENDO");
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 250));
                        garfos[dir].release();
                        garfos[esq].release();
                        sala.release();
                        log.log(nome, "largou os garfos e voltou a pensar");
                    }
                    log.log(nome, "terminou todas as refeições");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, NOMES[id]));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();
        System.out.println("\nTodos comeram " + REFEICOES + " vezes, sem deadlock e sem starvation.");
    }

    /** Solução ingênua: todos pegam o garfo esquerdo e depois tentam o direito -> impasse garantido. */
    static void simularComDeadlock() throws InterruptedException {
        System.out.println("""

                Versão INGÊNUA: cada filósofo pega o garfo da esquerda e depois tenta o da direita.
                Um latch garante que todos já seguram o garfo esquerdo antes da segunda tentativa.
                Para a demonstração não travar, a segunda tentativa usa timeout de 1,5 s (tryAcquire).
                """);

        LogConcorrente log = new LogConcorrente();
        Semaphore[] garfos = novosGarfos();
        CountDownLatch todosComEsquerdo = new CountDownLatch(N);
        CountDownLatch todosDesistiram = new CountDownLatch(N);
        AtomicInteger impasses = new AtomicInteger();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            final int id = i;
            threads.add(new Thread(() -> {
                String nome = NOMES[id];
                int esq = id, dir = (id + 1) % N;
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150));
                    garfos[esq].acquire();
                    log.log(nome, "pegou o garfo esquerdo (" + esq + ")");
                    todosComEsquerdo.countDown();
                    todosComEsquerdo.await();

                    log.log(nome, "tenta o garfo direito (" + dir + "), segurando o " + esq + "...");
                    if (garfos[dir].tryAcquire(1500, TimeUnit.MILLISECONDS)) {
                        log.log(nome, "conseguiu o garfo direito -> COMENDO");
                        garfos[dir].release();
                    } else {
                        impasses.incrementAndGet();
                        log.log(nome, "IMPASSE: garfo " + dir + " nunca ficou livre (timeout)");
                    }
                    todosDesistiram.countDown();
                    todosDesistiram.await(5, TimeUnit.SECONDS); // só solta o esquerdo quando todos desistirem
                    garfos[esq].release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, NOMES[id]));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        System.out.println("\n" + impasses.get() + " de " + N + " filósofos ficaram presos: DEADLOCK.");
        System.out.println("As quatro condições de Coffman estiveram presentes:");
        System.out.println("  1. Exclusão mútua   : cada garfo serve a um filósofo por vez");
        System.out.println("  2. Posse e espera   : seguram o esquerdo enquanto esperam o direito");
        System.out.println("  3. Não-preempção    : ninguém pode tomar um garfo à força");
        System.out.println("  4. Espera circular  : S -> P -> A -> D -> K -> S (cada um espera o vizinho)");
    }

    private static Semaphore[] novosGarfos() {
        Semaphore[] garfos = new Semaphore[N];
        for (int i = 0; i < N; i++) garfos[i] = new Semaphore(1);
        return garfos;
    }
}
