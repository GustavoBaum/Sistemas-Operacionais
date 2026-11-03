package minikernel.ui;

import java.util.Scanner;

/** Pequena camada de entrada/saída de texto usada por todos os módulos. */
public class Terminal {
    private final Scanner in;

    public Terminal() {
        this(new Scanner(System.in));
    }

    public Terminal(Scanner in) {
        this.in = in;
    }

    public void titulo(String texto) {
        String barra = "=".repeat(Math.max(texto.length() + 6, 60));
        System.out.println();
        System.out.println(barra);
        System.out.println("   " + texto);
        System.out.println(barra);
    }

    public void subtitulo(String texto) {
        System.out.println("\n--- " + texto + " ---");
    }

    /** Lê uma linha. Retorna null se a entrada acabou (Ctrl+D / fim do arquivo). */
    public String lerLinha(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!in.hasNextLine()) return null;
        return in.nextLine().trim();
    }

    /** Lê um inteiro >= min; Enter vazio devolve o valor padrão. */
    public int lerInt(String prompt, int padrao, int min) {
        while (true) {
            String s = lerLinha(prompt + " [padrão " + padrao + "]: ");
            if (s == null || s.isEmpty()) return padrao;
            try {
                int n = Integer.parseInt(s);
                if (n >= min) return n;
            } catch (NumberFormatException ignored) {
                // cai na mensagem abaixo
            }
            System.out.println("Valor inválido (use um inteiro >= " + min + ").");
        }
    }

    /**
     * Mostra um menu numerado de 1..n mais a opção 0.
     * Retorna 0 também quando a entrada acaba, o que encerra os laços de menu.
     */
    public int menu(String titulo, String rotuloZero, String... opcoes) {
        while (true) {
            titulo(titulo);
            for (int i = 0; i < opcoes.length; i++) {
                System.out.printf("  %d - %s%n", i + 1, opcoes[i]);
            }
            System.out.printf("  0 - %s%n", rotuloZero);
            String s = lerLinha("\nEscolha uma opção: ");
            if (s == null) return 0;
            try {
                int n = Integer.parseInt(s);
                if (n >= 0 && n <= opcoes.length) return n;
            } catch (NumberFormatException ignored) {
                // cai na mensagem abaixo
            }
            System.out.println("Opção inválida.");
        }
    }

    public void pausar() {
        lerLinha("\n[ENTER para continuar] ");
    }
}
