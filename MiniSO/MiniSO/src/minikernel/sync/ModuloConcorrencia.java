package minikernel.sync;

import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 1: Concorrência e Sincronização (threads reais + semáforos). */
public class ModuloConcorrencia implements Modulo {

    @Override
    public String nome() {
        return "Concorrência e Sincronização (threads + semáforos)";
    }

    @Override
    public void executar(Terminal t) {
        while (true) {
            int op = t.menu("MÓDULO 1 | Concorrência e Sincronização", "Voltar ao menu principal",
                    "Produtor/Consumidor (buffer circular)",
                    "Jantar dos Filósofos (solução segura com semáforo contador)",
                    "Jantar dos Filósofos (versão ingênua: demonstra deadlock)",
                    "Barbeiro Dorminhoco (clientes, sala de espera e semáforos)");
            if (op == 0) return;
            try {
                switch (op) {
                    case 1 -> ProdutorConsumidor.simular();
                    case 2 -> JantarFilosofos.simularSeguro();
                    case 3 -> JantarFilosofos.simularComDeadlock();
                    case 4 -> {
                        int cadeiras = t.lerInt("Cadeiras na sala de espera", 3, 1);
                        int clientes = t.lerInt("Número de clientes", 10, 1);
                        BarbeiroDorminhoco.simular(cadeiras, clientes);
                    }
                    default -> { }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            t.pausar();
        }
    }
}
