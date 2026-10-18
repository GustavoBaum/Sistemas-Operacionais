package minikernel.app;

import java.util.List;
import minikernel.core.Instrucao;
import minikernel.core.Processo;
import minikernel.deadlock.Banqueiro;
import minikernel.fs.SistemaArquivosFat;
import minikernel.kernel.Kernel;
import minikernel.mem.MemoriaPaginada;
import minikernel.mem.Politica;
import minikernel.sched.EscalonadorRoundRobinAging;
import minikernel.sync.SemaforosLogicos;

/** Monta o cenário de demonstração e executa a simulação integrada. */
public final class Cenario {
    private static final int QUANTUM = 3;
    private static final int QUADROS = 4;
    private static final int BLOCOS_DISCO = 32;
    private static final int LIMITE_TICKS = 1000;

    private Cenario() {}

    /** Cria processos novos a cada simulação (o programa é consumido durante a execução). */
    static List<Processo> criarProcessos() {
        // Recursos: 2 tipos, total [3, 2]
        Processo p1 = new Processo(1, 2, 4, new int[]{2, 1}, List.of(
                new Instrucao.Cpu(2),
                new Instrucao.AcessoMemoria(0),
                new Instrucao.AcessoMemoria(300),
                new Instrucao.PedirRecursos(new int[]{2, 1}),
                new Instrucao.Cpu(3),
                new Instrucao.EscreverArquivo("relatorio.txt", 3),
                new Instrucao.AcessoMemoria(700),
                new Instrucao.LiberarRecursos(new int[]{2, 1}),
                new Instrucao.Cpu(1)));

        Processo p2 = new Processo(2, 1, 3, new int[]{2, 2}, List.of(
                new Instrucao.Cpu(1),
                new Instrucao.AcessoMemoria(0),
                new Instrucao.AcessoMemoria(256),
                new Instrucao.PedirRecursos(new int[]{2, 2}),
                new Instrucao.Cpu(2),
                new Instrucao.AcessoMemoria(0),
                new Instrucao.LiberarRecursos(new int[]{2, 2})));

        // Produtor: sinaliza que há dados
        Processo p3 = new Processo(3, 3, 2, new int[]{0, 0}, List.of(
                new Instrucao.Cpu(2),
                new Instrucao.AcessoMemoria(0),
                new Instrucao.Cpu(1),
                new Instrucao.Signal("dados"),
                new Instrucao.Cpu(1)));

        // Consumidor: espera os dados antes de prosseguir
        Processo p4 = new Processo(4, 3, 2, new int[]{0, 0}, List.of(
                new Instrucao.Wait("dados"),
                new Instrucao.AcessoMemoria(0),
                new Instrucao.AcessoMemoria(256),
                new Instrucao.LerArquivo("relatorio.txt"),
                new Instrucao.Cpu(1)));

        return List.of(p1, p2, p3, p4);
    }

    public static Resultado simular(Politica politica, boolean verbose) {
        var escalonador = new EscalonadorRoundRobinAging(QUANTUM);
        var memoria = new MemoriaPaginada(QUADROS, politica);
        var banqueiro = new Banqueiro(3, 2);
        var arquivos = new SistemaArquivosFat(BLOCOS_DISCO);
        var semaforos = new SemaforosLogicos();
        semaforos.criar("dados", 0);

        var kernel = new Kernel(escalonador, memoria, banqueiro, arquivos, semaforos);
        if (verbose) {
            kernel.observar(e -> System.out.printf("[t=%03d] %-9s %s%s%n",
                    e.tick(), e.origem(), e.pid() >= 0 ? "P" + e.pid() + ": " : "", e.descricao()));
        }
        criarProcessos().forEach(kernel::criarProcesso);

        while (!kernel.terminou() && kernel.relogio() < LIMITE_TICKS) kernel.executarTick();
        if (!kernel.terminou()) {
            System.out.println("AVISO: limite de ticks atingido (possível travamento)");
        }

        return new Resultado(politica, kernel.relogio(), kernel.tme(), kernel.tmt(),
                memoria.faltas(), memoria.acertos(), arquivos.blocosLivres());
    }

    public static int quantum() { return QUANTUM; }
    public static int quadros() { return QUADROS; }
    public static int blocosDisco() { return BLOCOS_DISCO; }
}
