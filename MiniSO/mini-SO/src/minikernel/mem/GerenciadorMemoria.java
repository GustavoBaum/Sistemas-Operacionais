package minikernel.mem;

/** Módulo 3: Memória virtual / MMU. */
public interface GerenciadorMemoria {
    void criarEspaco(int pid, int numPaginas);
    ResultadoAcesso acessar(int pid, int enderecoVirtual);
    void liberar(int pid);

    /**
     * @param paginaSubstituida número da página expulsa para dar lugar à nova (null se não houve substituição)
     */
    record ResultadoAcesso(boolean pageFault, int quadro, int custoTicks, Integer paginaSubstituida) {}
}
