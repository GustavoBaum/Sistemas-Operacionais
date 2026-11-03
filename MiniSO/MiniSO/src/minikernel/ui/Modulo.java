package minikernel.ui;

/** Um módulo do Mini-SO que pode ser executado a partir do menu principal. */
public interface Modulo {
    /** Nome exibido no menu principal. */
    String nome();

    /** Executa a simulação do módulo (pode ter submenus próprios). */
    void executar(Terminal terminal);
}
