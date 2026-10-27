package minikernel.mem;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** MMU com tabela de quadros global e substituição FIFO ou LRU. */
public class MemoriaPaginada implements GerenciadorMemoria {
    public static final int TAMANHO_PAGINA = 256;
    public static final int CUSTO_FALTA = 2;

    private record Pagina(int pid, int numero) {}

    private final int quadros;
    // accessOrder = true -> LRU (get() move para o fim); false -> FIFO (ordem de inserção)
    private final LinkedHashMap<Pagina, Integer> residentes;
    private final Deque<Integer> quadrosLivres = new ArrayDeque<>();
    private final Map<Integer, Integer> numPaginas = new HashMap<>();
    private int faltas, acertos;

    public MemoriaPaginada(int quadros, Politica politica) {
        this.quadros = quadros;
        this.residentes = new LinkedHashMap<>(16, 0.75f, politica == Politica.LRU);
        for (int q = 0; q < quadros; q++) quadrosLivres.add(q);
    }

    @Override
    public void criarEspaco(int pid, int paginas) {
        numPaginas.put(pid, paginas);
    }

    @Override
    public ResultadoAcesso acessar(int pid, int enderecoVirtual) {
        int numero = enderecoVirtual / TAMANHO_PAGINA;
        if (numero >= numPaginas.getOrDefault(pid, 0)) {
            throw new IllegalArgumentException("Segmentation fault: P" + pid + " endereço " + enderecoVirtual);
        }
        Pagina pg = new Pagina(pid, numero);
        Integer quadro = residentes.get(pg);
        if (quadro != null) {
            acertos++;
            return new ResultadoAcesso(false, quadro, 0, null);
        }

        faltas++;
        int destino;
        Integer expulsa = null;
        if (!quadrosLivres.isEmpty()) {
            destino = quadrosLivres.poll();
        } else {
            var it = residentes.entrySet().iterator();
            var vitima = it.next(); // primeiro = mais antigo (FIFO) ou menos recente (LRU)
            destino = vitima.getValue();
            expulsa = vitima.getKey().numero();
            it.remove();
        }
        residentes.put(pg, destino);
        return new ResultadoAcesso(true, destino, CUSTO_FALTA, expulsa);
    }

    @Override
    public void liberar(int pid) {
        var it = residentes.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getKey().pid() == pid) {
                quadrosLivres.add(e.getValue());
                it.remove();
            }
        }
        numPaginas.remove(pid);
    }

    /** Conteúdo de cada quadro físico ("-" = livre), para exibição. */
    public String[] ocupacaoQuadros() {
        String[] q = new String[quadros];
        Arrays.fill(q, "-");
        residentes.forEach((pg, quadro) -> q[quadro] = String.valueOf(pg.numero()));
        return q;
    }

    /** Tabela de páginas residentes de um processo: página -> quadro. */
    public Map<Integer, Integer> tabelaDePaginas(int pid) {
        Map<Integer, Integer> tabela = new TreeMap<>();
        residentes.forEach((pg, quadro) -> {
            if (pg.pid() == pid) tabela.put(pg.numero(), quadro);
        });
        return tabela;
    }

    public int faltas() { return faltas; }

    public int acertos() { return acertos; }
}
