package minikernel.fs;

import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 5: Sistema de arquivos (mapa de bits + Tabela FAT). */
public class ModuloArquivos implements Modulo {

    @Override
    public String nome() {
        return "Sistema de Arquivos (mapa de bits + Tabela FAT)";
    }

    @Override
    public void executar(Terminal t) {
        t.titulo("MÓDULO 5 | Sistema de Arquivos");
        System.out.println("""

                Disco simulado de 16 blocos.
                  - Blocos livres: controlados por um MAPA DE BITS (1 = ocupado).
                  - Endereçamento: TABELA FAT. Cada entrada aponta para o próximo bloco do arquivo
                    (FIM = último bloco). O diretório guarda o primeiro bloco de cada arquivo.
                """);
        var fs = new SistemaArquivosFat(16);

        passo("1. Disco vazio", fs);

        System.out.println("\n>> Criando A.txt (3 blocos), B.txt (2 blocos) e C.txt (4 blocos)");
        fs.escrever(1, "A.txt", 3);
        fs.escrever(1, "B.txt", 2);
        fs.escrever(1, "C.txt", 4);
        passo("2. Três arquivos gravados em sequência", fs);

        System.out.println("\n>> Removendo B.txt (abre um buraco de 2 blocos no meio do disco)");
        fs.remover("B.txt");
        passo("3. Depois de remover B.txt", fs);

        System.out.println("\n>> Criando D.txt (5 blocos): não cabe no buraco, então usa o buraco + blocos do final");
        fs.escrever(1, "D.txt", 5);
        passo("4. D.txt ficou FRAGMENTADO (blocos não contíguos), mas a FAT o encadeia", fs);

        System.out.println("\n>> Lendo D.txt: o SO segue a cadeia da FAT, 1 acesso a disco por bloco");
        System.out.println("   cadeia: " + fs.cadeia("D.txt") + "  -> custo = " + fs.ler(1, "D.txt") + " acesso(s)");

        System.out.println("\n>> Tentando criar E.txt (6 blocos) com apenas " + fs.blocosLivres() + " bloco(s) livre(s)");
        try {
            fs.escrever(1, "E.txt", 6);
        } catch (IllegalStateException e) {
            System.out.println("   ERRO tratado: " + e.getMessage());
        }

        System.out.println("\n>> Removendo A.txt e C.txt e tentando de novo");
        fs.remover("A.txt");
        fs.remover("C.txt");
        fs.escrever(1, "E.txt", 6);
        passo("5. Estado final", fs);

        t.pausar();
    }

    private void passo(String titulo, SistemaArquivosFat fs) {
        System.out.println("\n[" + titulo + "]");
        System.out.println(fs.mapaDeBits());
        System.out.println(fs.tabelaFat());
        var dir = fs.listarDiretorio();
        System.out.println("  Diretório:" + (dir.isEmpty() ? " (vazio)" : ""));
        dir.forEach(System.out::println);
    }
}
