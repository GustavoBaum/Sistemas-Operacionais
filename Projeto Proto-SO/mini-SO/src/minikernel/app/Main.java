package minikernel.app;

import java.util.List;
import minikernel.deadlock.ModuloDeadlock;
import minikernel.fs.ModuloArquivos;
import minikernel.mem.ModuloMemoria;
import minikernel.sched.ModuloEscalonamento;
import minikernel.sync.ModuloConcorrencia;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Ponto de entrada do Mini-SO: terminal com um menu de módulos, cada um rodando sua simulação. */
public class Main {
    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        List<Modulo> modulos = List.of(
                new ModuloConcorrencia(),
                new ModuloEscalonamento(),
                new ModuloMemoria(),
                new ModuloDeadlock(),
                new ModuloArquivos(),
                new ModuloIntegrado());
        String[] nomes = modulos.stream().map(Modulo::nome).toArray(String[]::new);

        while (true) {
            int op = terminal.menu("MINI-SO | Simulador de Núcleo de Sistema Operacional", "Sair", nomes);
            if (op == 0) break;
            modulos.get(op - 1).executar(terminal);
        }
        System.out.println("\nEncerrando o Mini-SO. Até logo!");
    }
}
