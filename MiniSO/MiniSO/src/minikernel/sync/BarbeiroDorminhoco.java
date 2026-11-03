package minikernel.sync;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Problema do Barbeiro Dorminhoco (Dijkstra).
 *
 * Uma barbearia com 1 barbeiro, 1 cadeira de corte e N cadeiras de espera.
 *   - Sem clientes, o barbeiro DORME (bloqueado em wait(clientes), sem busy waiting).
 *   - Cliente que chega e acha a sala cheia vai embora.
 *   - Cliente que chega e acha vaga senta; se o barbeiro dorme, ele o acorda.
 *
 * Semáforos:
 *   clientes : conta clientes sentados esperando (o barbeiro dorme nele)
 *   mutex    : exclusão mútua sobre a sala de espera
 *   chamado / corteFeito : um par por cliente, para o encontro (rendez-vous) com o barbeiro
 */
final class BarbeiroDorminhoco {

    private static final class Cliente {
        final int id;
        final Semaphore chamado = new Semaphore(0);    // barbeiro -> cliente: "pode sentar na cadeira"
        final Semaphore corteFeito = new Semaphore(0); // barbeiro -> cliente: "terminei"

        Cliente(int id) {
            this.id = id;
        }

        String nome() {
            return "Cliente-" + id;
        }
    }

    private final int cadeiras;
    private final int totalClientes;
    private final LogConcorrente log = new LogConcorrente();
    private final Semaphore clientes = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1);
    private final Deque<Cliente> sala = new ArrayDeque<>(); // protegida pelo mutex
    private final AtomicInteger atendidos = new AtomicInteger();
    private final AtomicInteger desistentes = new AtomicInteger();
    private volatile boolean encerrar;

    private BarbeiroDorminhoco(int cadeiras, int totalClientes) {
        this.cadeiras = cadeiras;
        this.totalClientes = totalClientes;
    }

    static void simular(int cadeiras, int totalClientes) throws InterruptedException {
        System.out.println("""

                Barbearia com 1 barbeiro e %d cadeira(s) de espera | %d cliente(s) chegando em intervalos aleatórios.
                Sem clientes, o barbeiro DORME bloqueado em wait(clientes). O cliente que chega acorda o
                barbeiro (signal) se houver vaga na sala; se a sala está cheia, ele vai embora.
                Nenhuma thread fica em laço verificando a sala (sem busy waiting).
                """.formatted(cadeiras, totalClientes));
        new BarbeiroDorminhoco(cadeiras, totalClientes).executar();
    }

    private void executar() throws InterruptedException {
        Thread barbeiro = new Thread(this::barbeiro, "Barbeiro");
        barbeiro.start();

        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= totalClientes; i++) {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300)); // intervalo entre chegadas
            Cliente c = new Cliente(i);
            Thread t = new Thread(() -> cliente(c), c.nome());
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();

        encerrar = true;
        clientes.release(); // acorda o barbeiro (se dormindo) para ele encerrar o expediente
        barbeiro.join();

        System.out.printf("%nResumo: %d cliente(s) atendido(s), %d desistiram (sala cheia), %d no total.%n",
                atendidos.get(), desistentes.get(), totalClientes);
    }

    private void barbeiro() {
        try {
            while (true) {
                boolean dormindo = clientes.availablePermits() == 0;
                if (dormindo) log.log("Barbeiro", "sem clientes -> DORME em wait(clientes)");
                clientes.acquire();
                if (encerrar) {
                    log.log("Barbeiro", "fim do expediente, vai para casa");
                    return;
                }
                Cliente c;
                mutex.acquire();
                c = sala.poll();
                log.log("Barbeiro", (dormindo ? "ACORDOU! " : "") + "chama " + c.nome() + "   sala " + salaTexto());
                mutex.release();

                c.chamado.release();
                Thread.sleep(ThreadLocalRandom.current().nextInt(300, 500)); // cortando
                log.log("Barbeiro", "terminou o corte de " + c.nome());
                atendidos.incrementAndGet();
                c.corteFeito.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cliente(Cliente c) {
        String nome = c.nome();
        try {
            mutex.acquire();
            if (sala.size() < cadeiras) {
                sala.add(c);
                log.log(nome, "chegou e sentou na sala de espera (" + sala.size() + "/" + cadeiras
                        + ")   sala " + salaTexto());
                mutex.release();

                clientes.release();        // signal: há cliente (acorda o barbeiro se ele dorme)
                c.chamado.acquire();       // espera ser chamado
                log.log(nome, "senta na cadeira do barbeiro");
                c.corteFeito.acquire();    // espera o corte terminar
                log.log(nome, "saiu com o cabelo cortado");
            } else {
                desistentes.incrementAndGet();
                log.log(nome, "chegou, sala CHEIA (" + cadeiras + "/" + cadeiras + ") -> foi embora sem cortar");
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Estado da sala de espera. Deve ser chamado com o mutex em mãos. */
    private String salaTexto() {
        StringBuilder sb = new StringBuilder("[");
        int ocupadas = 0;
        for (Cliente c : sala) {
            sb.append(String.format("%3d", c.id));
            ocupadas++;
        }
        for (int i = ocupadas; i < cadeiras; i++) sb.append("  .");
        return sb.append(" ]").toString();
    }
}
