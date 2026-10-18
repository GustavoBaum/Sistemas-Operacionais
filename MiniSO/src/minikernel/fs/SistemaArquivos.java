package minikernel.fs;

/** Módulo 5: Sistema de arquivos. */
public interface SistemaArquivos {
    int ler(int pid, String nome);                 // retorna custo em ticks de disco
    int escrever(int pid, String nome, int blocos);
}
