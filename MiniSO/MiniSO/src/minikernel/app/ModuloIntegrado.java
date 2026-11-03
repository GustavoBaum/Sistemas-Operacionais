package minikernel.app;

import java.util.List;
import minikernel.mem.Politica;
import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Protótipo da futura integração: processos que usam CPU, memória, recursos, disco e semáforos juntos. */
public class ModuloIntegrado implements Modulo {

    @Override
    public String nome() {
        return "Simulação integrada (protótipo do futuro proto-SO)";
    }

    @Override
    public void executar(Terminal t) {
        t.titulo("PROTÓTIPO | Simulação integrada");
        System.out.printf("Quantum=%d | %d quadros de memória | recursos totais [3,2] | disco de %d blocos%n%n",
                Cenario.quantum(), Cenario.quadros(), Cenario.blocosDisco());

        Resultado lru = Cenario.simular(Politica.LRU, true);
        Resultado fifo = Cenario.simular(Politica.FIFO, false);

        System.out.println("\n=== COMPARAÇÃO DAS POLÍTICAS DE SUBSTITUIÇÃO ===");
        System.out.printf("%-8s %6s %8s %8s %8s %8s%n", "Política", "Ticks", "TME", "TMT", "Faltas", "Acertos");
        for (Resultado r : List.of(lru, fifo)) {
            System.out.printf("%-8s %6d %8.2f %8.2f %8d %8d%n",
                    r.politica(), r.ticks(), r.tme(), r.tmt(), r.faltas(), r.acertos());
        }
        t.pausar();
    }
}
