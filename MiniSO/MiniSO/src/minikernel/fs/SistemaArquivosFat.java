package minikernel.fs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema de arquivos simulado:
 *   - blocos livres controlados por MAPA DE BITS;
 *   - endereçamento dos arquivos por TABELA FAT (alocação encadeada);
 *   - diretório plano (nome -> primeiro bloco).
 */
public class SistemaArquivosFat implements SistemaArquivos {
    private static final int LIVRE = -1, FIM = -2;

    private final int total;
    private final int[] fat;
    private final BitSet ocupados = new BitSet();
    private final Map<String, Integer> diretorio = new LinkedHashMap<>();

    public SistemaArquivosFat(int totalBlocos) {
        this.total = totalBlocos;
        this.fat = new int[totalBlocos];
        Arrays.fill(fat, LIVRE);
    }

    @Override
    public int escrever(int pid, String nome, int blocos) {
        if (blocos <= 0) throw new IllegalArgumentException("Um arquivo precisa de ao menos 1 bloco");
        remover(nome); // sobrescrita
        List<Integer> livres = new ArrayList<>();
        for (int b = ocupados.nextClearBit(0); b < total && livres.size() < blocos; b = ocupados.nextClearBit(b + 1)) {
            livres.add(b);
        }
        if (livres.size() < blocos) {
            throw new IllegalStateException("Disco cheio: '" + nome + "' precisa de " + blocos
                    + " bloco(s) e há apenas " + blocosLivres() + " livre(s)");
        }
        for (int k = 0; k < blocos; k++) {
            int b = livres.get(k);
            fat[b] = (k == blocos - 1) ? FIM : livres.get(k + 1);
            ocupados.set(b);
        }
        diretorio.put(nome, livres.get(0));
        return blocos; // 1 tick por bloco
    }

    @Override
    public int ler(int pid, String nome) {
        return cadeia(nome).size(); // percorrer a cadeia custa 1 tick por bloco
    }

    /** Remove o arquivo e devolve seus blocos ao mapa de bits. */
    public boolean remover(String nome) {
        Integer b = diretorio.remove(nome);
        if (b == null) return false;
        int cur = b;
        while (cur != FIM) {
            int prox = fat[cur];
            fat[cur] = LIVRE;
            ocupados.clear(cur);
            cur = prox;
        }
        return true;
    }

    public boolean existe(String nome) {
        return diretorio.containsKey(nome);
    }

    /** Cadeia de blocos do arquivo, seguindo a FAT a partir do primeiro bloco. */
    public List<Integer> cadeia(String nome) {
        List<Integer> blocos = new ArrayList<>();
        Integer b = diretorio.get(nome);
        if (b == null) return blocos;
        for (int cur = b; cur != FIM; cur = fat[cur]) blocos.add(cur);
        return blocos;
    }

    public int blocosLivres() {
        return total - ocupados.cardinality();
    }

    public int totalBlocos() {
        return total;
    }

    // ---------- exibição ----------

    public String mapaDeBits() {
        StringBuilder sb = new StringBuilder("  Mapa de bits (1 = ocupado): ");
        for (int i = 0; i < total; i++) {
            if (i > 0 && i % 8 == 0) sb.append(' ');
            sb.append(ocupados.get(i) ? '1' : '0');
        }
        sb.append("   [").append(blocosLivres()).append("/").append(total).append(" livres]");
        return sb.toString();
    }

    public String tabelaFat() {
        StringBuilder sb = new StringBuilder();
        for (int ini = 0; ini < total; ini += 16) {
            int fim = Math.min(ini + 16, total);
            sb.append("  Bloco: ");
            for (int i = ini; i < fim; i++) sb.append(String.format("%4d", i));
            sb.append("\n  FAT  : ");
            for (int i = ini; i < fim; i++) {
                String v = fat[i] == LIVRE ? "." : fat[i] == FIM ? "FIM" : String.valueOf(fat[i]);
                sb.append(String.format("%4s", v));
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    public List<String> listarDiretorio() {
        List<String> linhas = new ArrayList<>();
        for (String nome : diretorio.keySet()) {
            List<Integer> c = cadeia(nome);
            StringBuilder cadeia = new StringBuilder();
            for (int i = 0; i < c.size(); i++) {
                if (i > 0) cadeia.append(" -> ");
                cadeia.append(c.get(i));
            }
            linhas.add(String.format("  %-12s início=%-3d %d bloco(s)   cadeia: %s", nome, c.get(0), c.size(), cadeia));
        }
        return linhas;
    }
}
