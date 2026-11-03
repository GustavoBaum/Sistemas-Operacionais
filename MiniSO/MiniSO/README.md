# Mini-SO: simulador de núcleo de sistema operacional

Aplicação de terminal com um **menu de 5 módulos** (mais um protótipo de integração).
Cada opção executa a simulação de um tipo de gerenciamento do SO, de forma independente.

```
 1 - Concorrência e Sincronização   threads reais + semáforos (Produtor/Consumidor, Filósofos, Barbeiro Dorminhoco)
 2 - Escalonamento de Processos     Round-Robin + Aging, filas de estado, TME e TMT
 3 - Memória Virtual e MMU          tradução de endereços, page fault, FIFO x LRU, anomalia de Belady
 4 - Impasses / Deadlocks           Algoritmo do Banqueiro, detecção por grafo e desenho dos grafos (ASCII / .dot)
 5 - Sistema de Arquivos            mapa de bits + Tabela FAT, fragmentação
 6 - Simulação integrada            protótipo do futuro proto-SO (módulos trabalhando juntos)
 0 - Sair
```

## Requisitos

- **JDK 21 ou superior** (usa `record`, `sealed interface` e `switch` com pattern matching)
- VS Code com a extensão **Extension Pack for Java** (ou IntelliJ IDEA)

## Executar no VS Code

1. `File > Open Folder...` e escolha a pasta `MiniSO`.
2. Aguarde a extensão Java carregar o projeto.
3. Abra `src/minikernel/app/Main.java` e clique em **Run** (acima do `main`),
   ou use `Run and Debug` (Ctrl+Shift+D) > **Mini-SO (menu interativo)**.
4. Digite as opções **no terminal integrado** que se abrir (o programa lê do teclado).

## Executar pelo terminal

Linux / macOS:

    javac -d bin $(find src -name "*.java")
    java -cp bin minikernel.app.Main

Windows (PowerShell):

    javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
    java -cp bin minikernel.app.Main

Se os acentos aparecerem errados, adicione `-Dstdout.encoding=UTF-8` ao comando `java`.

## Estrutura

    src/minikernel/
      ui/        Terminal (entrada/saída) e a interface Modulo
      core/      tipos compartilhados: Processo (PCB), Estado, Instrucao, Evento
      sync/      Módulo 1: ModuloConcorrencia, ProdutorConsumidor, JantarFilosofos, BarbeiroDorminhoco
      sched/     Módulo 2: ModuloEscalonamento + EscalonadorRoundRobinAging
      mem/       Módulo 3: ModuloMemoria + MemoriaPaginada (FIFO/LRU)
      deadlock/  Módulo 4: ModuloDeadlock + Banqueiro + GrafoAlocacao + DesenhoGrafo
      fs/        Módulo 5: ModuloArquivos + SistemaArquivosFat
      kernel/    Kernel: despacha instruções dos processos aos módulos (base da integração)
      app/       Main (menu), Cenario/ModuloIntegrado (protótipo da opção 6)

## Exibir os grafos (Módulo 4, opção 3)

Desenha no terminal os grafos de alocação de recursos das simulações de deadlock
(processos em `( )`, recursos em `[ ]`; alocação em linha cheia, espera pontilhada;
ciclos desenhados como anel). Ao final, oferece exportar os grafos para a pasta `grafos/`
em formato Graphviz. Para gerar imagens (requer Graphviz instalado):

    dot -Tpng grafos/B2_deadlock.dot -o B2_deadlock.png

## Como adicionar ou alterar um módulo

Cada módulo implementa a interface `Modulo` (`nome()` e `executar(Terminal)`).
Para criar um novo, implemente essa interface e inclua-o na lista em `Main`.

## Caminho para o proto-SO integrado

Os módulos 2 a 5 já seguem interfaces (`Escalonador`, `GerenciadorMemoria`, `GerenciadorRecursos`,
`SistemaArquivos`, `Sincronizacao`) e o `Kernel` já sabe despachar instruções de um processo
(CPU, E/S, acesso à memória, pedido de recursos, arquivos, semáforos) a cada módulo.
A opção 6 é uma demonstração disso. Evoluir o protótipo significa ampliar o `Kernel`
e os cenários, sem mexer nas simulações dos módulos isoladas.
