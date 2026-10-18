package minikernel.sync;

import minikernel.ui.Modulo;
import minikernel.ui.Terminal;

/** Módulo 1: Concorrência e Sincronização (threads reais + semáforos). */
public class ModuloConcorrencia implements Modulo {

    @Override
    public String nome() {
        return "Concorrencia e Sincronizacao (threads + semaforos)";
    }

    @Override
    public void executar(Terminal t) {
        while (true) {
            int op = t.menu("MODULO 1 | Concorrencia e Sincronizacao", "Voltar ao menu principal",
                    "Produtor/Consumidor (buffer circular)",
                    "Jantar dos Filosofos (solução segura com semaforo contador)",
                    "Jantar dos Filosofos (versão ingênua: demonstra deadlock)");
            if (op == 0) return;
            try {
                switch (op) {
                    case 1 -> ProdutorConsumidor.simular();
                    case 2 -> JantarFilosofos.simularSeguro();
                    case 3 -> JantarFilosofos.simularComDeadlock();
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
