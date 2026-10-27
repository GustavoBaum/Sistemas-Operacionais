package minikernel.core;

/** Quem quiser acompanhar os eventos do kernel (log, interface gráfica, testes). */
public interface Ouvinte {
    void aoOcorrer(Evento e);
}
