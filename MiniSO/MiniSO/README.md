# Mini-SO: simulador de núcleo de sistema operacional

Aplicação de terminal com um **menu de módulos**. Cada opção executa a simulação de um
subsistema do núcleo de um SO, de forma independente:

    1 - Concorrência e Sincronização   (threads reais + semáforos)
    2 - Escalonamento de Processos     (Round-Robin + Aging, TME e TMT)
    3 - Memória Virtual e MMU          (page fault, FIFO e LRU)
    4 - Impasses / Deadlocks           (Banqueiro e detecção por grafo)
    5 - Sistema de Arquivos            (mapa de bits + Tabela FAT)
    6 - Simulação integrada            (protótipo do futuro proto-SO)

## Requisitos

- **JDK 17 ou superior** (precisa ser JDK, não apenas JRE). Confira com `javac -version`.
  Download: https://adoptium.net (Temurin 21 recomendado).

## Jeito mais simples de rodar (sem depender de IDE)

Abra um terminal **dentro da pasta do projeto** (a que contém `src` e `rodar.bat`) e execute:

- Windows: `rodar` (ou duplo clique em `rodar.bat`)
- Linux/macOS: `./rodar.sh`

Ou manualmente, em qualquer sistema:

    javac -encoding UTF-8 -sourcepath src -d bin src/minikernel/app/Main.java
    java -cp bin minikernel.app.Main

(No Windows, se os acentos saírem errados, rode antes `chcp 65001`.)

## Rodar no VS Code

1. Instale o **Extension Pack for Java** (Microsoft). O VS Code vai sugerir isso ao abrir o projeto.
2. `File > Open Folder...` e escolha a pasta que **contém `src` e `.vscode`** (cuidado com pastas aninhadas).
3. Aguarde o carregamento do projeto Java (a barra de status mostra "Java: Ready").
4. Abra `src/minikernel/app/Main.java` e clique em **Run** acima do `main`,
   ou use `Run and Debug` (Ctrl+Shift+D) > **Mini-SO (terminal)**.
5. Digite o número da opção no terminal integrado e pressione ENTER.

Se o botão Run não aparecer: Ctrl+Shift+P > `Java: Clean Java Language Server Workspace`.
Se o VS Code usar um JDK antigo: Ctrl+Shift+P > `Java: Configure Java Runtime`.

## O que cada módulo mostra

| Opção | Conteúdo |
|---|---|
| 1 | Produtor/Consumidor com buffer circular; Jantar dos Filósofos seguro (semáforo "sala") e ingênuo (deadlock com as 4 condições de Coffman). Sem busy waiting. |
| 2 | Filas Pronto/Executando/Bloqueado tick a tick, quantum e aging configuráveis, Gantt, TME e TMT. |
| 3 | Tradução endereço virtual -> físico com tabela de páginas; FIFO x LRU; anomalia de Belady. |
| 4 | Banqueiro com matriz Need, passos de verificação e sequência segura; detecção de ciclo em grafo e recuperação. |
| 5 | Disco de 16 blocos com mapa de bits e FAT; criação, remoção, fragmentação e disco cheio. |

## Estrutura

    src/minikernel/
      ui/        Terminal (entrada/saída) e a interface Modulo
      sync/      Módulo 1: ModuloConcorrencia, ProdutorConsumidor, JantarFilosofos, semáforos lógicos
      sched/     Módulo 2: ModuloEscalonamento, Escalonador + RoundRobinAging
      mem/       Módulo 3: ModuloMemoria, GerenciadorMemoria + MemoriaPaginada (FIFO/LRU)
      deadlock/  Módulo 4: ModuloDeadlock, Banqueiro, GrafoAlocacao
      fs/        Módulo 5: ModuloArquivos, SistemaArquivosFat
      core/      Tipos compartilhados: Processo (PCB), Estado, Instrucao, Evento
      kernel/    Kernel: despacha instruções de processos aos módulos (usado pela opção 6)
      app/       Main (menu), ModuloIntegrado e Cenario (demonstração da integração)

## Como o projeto evolui para um "proto-SO"

Cada módulo já expõe uma interface (`Escalonador`, `GerenciadorMemoria`, `GerenciadorRecursos`,
`SistemaArquivos`, `Sincronizacao`) e o `Kernel` conecta todas em um laço de ticks. A opção 6 é
um primeiro protótipo dessa integração. Para adicionar um novo módulo ao menu, crie uma classe
que implemente `Modulo` e registre-a na lista de `Main`.
