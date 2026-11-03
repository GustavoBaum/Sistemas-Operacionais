# Documentação Técnica do Mini-SO

## 1. Arquitetura

O **Mini-SO** é um simulador de kernel desenvolvido em Java para demonstrar conceitos de sistemas operacionais. Ele modela concorrência, multiprogramação, gerência de memória virtual por paginação, controle de impasses (*deadlocks*), sincronização de processos e persistência de arquivos.

O projeto está dividido nos seguintes pacotes:

```
+-- minikernel
    +-- app          (Ponto de entrada, Inicialização e Gerenciador Principal da GUI)
    +-- core         (Abstrações fundamentais: PCB, Instruções, Estados e Contratos Observer)
    +-- deadlock     (Controle de recursos, Algoritmo do Banqueiro e Grafos de Alocação)
    +-- fs           (Sistema de Arquivos baseado em FAT, Blocos e Mapa de Bits)
    +-- kernel       (Núcleo central de despacho, Ciclo de Clock e Dispatcher de Syscalls)
    +-- mem          (Gerenciamento de Memória Virtual, Paginação, Swap e Políticas)
    +-- sched        (Subsistema de Escalonamento de CPU e Gerenciamento de Filas)
    +-- sync         (Primitivas de Sincronização: Semáforos e Monitores com Filas)
    +-- ui           (Painéis de visualização gráfica reativos via padrão Observer)

```

## 2. Módulos e classes

### 2.1 `minikernel.core` — Estruturas fundamentais

#### `EstadoProcesso.java` (Enum)
* Define o conjunto finito de estados nos quais um processo pode se encontrar ao longo do seu ciclo de vida no sistema operacional.
* **Constantes:**
    * `NOVO`: O processo foi instanciado no sistema, mas ainda não foi inserido na fila de prontos.
    * `PRONTO`: O processo reside na fila de prontos do escalonador (`Escalonador.filaProntos`), aguardando que a CPU lhe seja alocada.
    * `EXECUTANDO`: O processo está associado ao ponteiro de CPU (`Kernel.processador`), tendo suas instruções consumidas a cada ciclo de clock.
    * `BLOQUEADO`: O processo foi suspenso e inserido em uma fila de espera associada a um recurso indisponível, semáforo (`Semaforo.filaBloqueados`), E/S ou *Page Fault*.
    * `TERMINADO`: O processo concluiu a execução de todas as suas instruções ou foi abortado. Não concorre mais pela CPU e aguarda a coleta/liberação definitiva de seus recursos.

#### `TipoInstrucao.java` (Enum)
* Codifica o conjunto de instruções (*ISA - Instruction Set Architecture*) interpretado pela unidade de processamento simulada e pelo despachador de chamadas de sistema.
* **Constantes:**
    * `CPU`: Instrução de processamento local (não exige intervenção do kernel).
    * `MEM_READ`: Operação de leitura em endereço de memória virtual. Exige tradução e validação de página.
    * `MEM_WRITE`: Operação de escrita em endereço de memória virtual. Seta o bit de modificação (*dirty bit*).
    * `IO`: Operação de entrada e saída Genérica.
    * `SEM_WAIT`: Operação *P* (*down*) em um semáforo nomeado.
    * `SEM_SIGNAL`: Operação *V* (*up*) em um semáforo nomeado.
    * `FS_READ`: Solicitador de leitura em bloco de arquivo via sistema de arquivos.
    * `FS_WRITE`: Solicitador de escrita de bloco no sistema de arquivos.
    * `REQ_RECURSO`: Solicitação de alocação de recurso genérico supervisionado pelo Algoritmo do Banqueiro.
    * `REL_RECURSO`: Liberação de recurso previamente alocado.

#### `Instrucao.java`
* Modela uma instrução individual pertencente a um processo, especificando seu tipo, parâmetros operacionais e o tempo de processamento em ciclos de clock.
* **Atributos:**
  * `tipo` (`TipoInstrucao`): Tipo da operação a ser executada.
  * `parametro1` (`Object`): Primeiro parâmetro da instrução (pode representar um endereço virtual, nome de semáforo, id de arquivo ou tipo de recurso).
  * `parametro2` (`Object`): Segundo parâmetro opcional (ex.: valor a ser escrito, quantidade de recursos solicitados).
  * `ciclosRestantes` (`int`): Quantidade de ciclos do clock do kernel necessários para concluir a execução da instrução.
* **Métodos:**1. `Instrucao(TipoInstrucao tipo, Object param1, Object param2, int ciclos)`:
     * **Parâmetros:** `tipo` (`TipoInstrucao`), `param1` (`Object`), `param2` (`Object`), `ciclos` (`int`).
     * **Lógica:** Atribui os parâmetros recebidos aos respetivos atributos da classe. Se `ciclos` for menor ou igual a zero, inicializa `ciclosRestantes` com o valor padrão `1`.
  2. `decrementarCiclo()`:
     * **Retorno:** `boolean` (indica se a instrução foi concluída após este decremento).
     * **Lógica:** Avalia se `ciclosRestantes > 0`. Em caso afirmativo, realiza a operação de decremento `ciclosRestantes--`. Retorna o resultado da expressão lógica `ciclosRestantes == 0`.
  3. `isConcluida()`:
     * **Retorno:** `boolean`.
     * **Lógica:** Avalia e retorna o resultado da verificação relacional `ciclosRestantes <= 0`.
  4. `getParametro1AsInt()` / `getParametro1AsString()`:
     * **Retorno:** `int` / `String`.
     * **Lógica:** Realiza o *type casting* explícito do atributo genérico `parametro1` para o tipo desejado. Lança `ClassCastException` tratada caso a conversão seja incompatível.

#### `Processo.java`
* Funciona como o *Process Control Block* (PCB). Concentra todo o estado, contexto de execução, prioridades, estruturas de memória e métricas de desempenho associadas a um processo.
* **Atributos:**
  * `pid` (`int`): Identificador único do processo no kernel.
  * `nome` (`String`): Nome descritivo do processo.
  * `estado` (`EstadoProcesso`): Estado atual do processo na FSM do sistema.
  * `prioridade` (`int`): Nível de prioridade para algoritmos de escalonamento com prioridade.
  * `pc` (`int`): *Program Counter*, apontando para o índice da instrução atual.
  * `instrucoes` (`List<Instrucao>`): Lista ordenada de instruções que compõem o programa do processo.
  * `tabelaPaginas` (`TabelaPaginas`): Tabela de mapeamento de memória virtual para o processo.
  * `recursosAlocados` (`Map<Integer, Integer>`): Mapeamento de `[ID_Recurso, Quantidade]` de recursos em posse do processo.
  * `tempoChegada` (`long`), `tempoEsperaTotal` (`long`), `tempoInicioExecucao` (`long`), `tempoFim` (`long`), `ultimoMomentoPronto` (`long`).
* **Métodos:**1. `Processo(int pid, String nome, List<Instrucao> instrucoes, int prioridade)`:
     * **Parâmetros:** `pid` (`int`), `nome` (`String`), `instrucoes` (`List<Instrucao>`), `prioridade` (`int`).
     * **Lógica:** Atribui os parâmetros aos atributos correspondentes. Inicializa `estado` como `EstadoProcesso.NOVO`, `pc` como `0`, `tabelaPaginas` como uma nova instância de `TabelaPaginas(pid)`, `recursosAlocados` como um `HashMap` vazio e zera todos os contadores de tempo e telemetria.
  2. `getInstrucaoAtual()`:
     * **Retorno:** `Instrucao` (ou `null`).
     * **Lógica:** Avalia se o atributo `pc` atende à condição `pc >= 0 && pc < instrucoes.size()`. Se verdadeiro, retorna `instrucoes.get(pc)`. Caso contrário, retorna `null`, sinalizando o término do código fonte do processo.
  3. `avancarPC()`:
     * **Retorno:** `void`.
     * **Lógica:** Aplica a operação de incremento `pc++`.
  4. `setEstado(EstadoProcesso novoEstado, long momentoAtual)`:
     * **Parâmetros:** `novoEstado` (`EstadoProcesso`), `momentoAtual` (`long`).
     * **Lógica:** 
       * Se o estado atual for `PRONTO` e o novo estado for `EXECUTANDO`, verifica se `tempoInicioExecucao == -1` (primeira execução). Se sim, define `tempoInicioExecucao = momentoAtual` para cálculo da latência/resposta.
       * Se o estado atual for `PRONTO` e for transicionado para qualquer outro estado, calcula o tempo gasto na fila de prontos: `tempoEsperaTotal += (momentoAtual - ultimoMomentoPronto)`.
       * Se o novo estado for `PRONTO`, atualiza `ultimoMomentoPronto = momentoAtual`.
       * Atualiza o atributo local `this.estado = novoEstado`.
  5. `getTurnaroundTime()`:
     * **Retorno:** `long`.
     * **Lógica:** Avalia se `tempoFim > 0`. Se verdadeiro, retorna o resultado da subtração `tempoFim - tempoChegada`. Caso o processo ainda não tenha terminado, retorna a diferença relativa em relação ao momento presente: `tempoAtual - tempoChegada`.

#### `KernelObserver.java` (Interface)
* Contrato de projeto do padrão *Observer*. Permite que ouvintes externos (interfaces gráficas, registradores de log) sejam notificados assincronamente de eventos ocorridos no núcleo.
* **Assinaturas dos Métodos:**
  1. `onProcessoCriado(Processo p)`: Invocado imediatamente após a alocação do PCB pelo `GerenciadorProcessos`.
  2. `onMudancaEstado(Processo p, EstadoProcesso estadoAntigo, EstadoProcesso estadoNovo)`: Disparado a cada transição de estado na FSM de um processo.
  3. `onTrocaContexto(Processo processoSaindo, Processo processoEntrando)`: Acionado durante as trocas de contexto na CPU simulada.
  4. `onPageFault(int pid, int paginaVirtual)`: Notifica a ocorrência de uma falta de página.
  5. `onDeadlockDetectado(List<Processo> processosEmDeadlock)`: Notifica a identificação de um conjunto de processos bloqueados por impasse.

### 2.2 `minikernel.kernel` — Kernel

#### `Kernel.java`
* Componente central do simulador. Gerencia o laço do relógio global (*clock tick*), controla a execução do processador, gerencia as chamadas de sistema e unifica todos os subsistemas.
* **Atributos:**
  * `relogio` (`long`): Contador global de pulsos de clock do sistema.
  * `processador` (`Processo`): Referência ao processo atualmente em execução na CPU.
  * `escalonador` (`Escalonador`): Instância do componente responsável pelo escalonamento.
  * `memoriaVirtual` (`MemoriaVirtual`): Instância do gerenciador de memória virtual.
  * `gerenciadorFS` (`GerenciadorFS`): Instância do sistema de arquivos.
  * `gerenciadorProcessos` (`GerenciadorProcessos`): Controlador do ciclo de vida de processos.
  * `gerenciadorSincronizacao` (`GerenciadorSincronizacao`): Controlador de primitivas de sincronização.
  * `algoritmoBanqueiro` (`AlgoritmoBanqueiro`): Gestor de alocação segura de recursos.
  * `observadores` (`List<KernelObserver>`): Lista de ouvintes notificados via evento.
  * `emExecucao` (`boolean`): Sinalizador do estado de simulação (Ativo/Pausado).
* **Métodos:**1. `Kernel(Escalonador escalonador, MemoriaVirtual mem, GerenciadorFS fs, AlgoritmoBanqueiro banqueiro)`:
     * **Parâmetros:** Instâncias pré-configuradas dos subsistemas.
     * **Lógica:** Armazena as referências recebidas nos atributos locais. Instancia `gerenciadorProcessos` e `gerenciadorSincronizacao`. Inicializa `relogio = 0`, `processador = null`, `emExecucao = false` e `observadores` como uma `ArrayList` vazia.
  2. `registrarObserver(KernelObserver observer)`:
     * **Parâmetros:** `observer` (`KernelObserver`).
     * **Lógica:** Verifica se `observer != null` e se a lista `observadores` ainda não o contém. Se positivo, executa `observadores.add(observer)`.
  3. `executarCiclo()`:
     * **Retorno:** `void`.
     * **Lógica:**
       1. Incrementa o relógio global: `relogio++`.
       2. Executa a rotina de *aging* no escalonador: `escalonador.aplicarAging(relogio)`.
       3. Verifica a necessidade de escolha de processo para a CPU (`processador == null`):
          * Invoca `escalonador.proximoProcesso(null)`.
          * Se um novo processo `p` for retornado, define `processador = p`, ajusta o estado para `EXECUTANDO` e notifica os observadores via `notificarTrocaContexto(null, p)`.
       4. Caso exista um processo no `processador`:
          * Obtém a instrução corrente via `processador.getInstrucaoAtual()`.
          * **Condição A:** Se a instrução for `null` (fim de programa):
            * Altera estado de `processador` para `TERMINADO`.
            * Invoca `gerenciadorProcessos.destruirProcesso(processador.getPid())`.
            * Notifica observadores e limpa o ponteiro: `processador = null`.
          * **Condição B:** Se a instrução existir:
            * Se for uma instrução do tipo `CPU`:
              * Executa `boolean concluida = instrucao.decrementarCiclo()`.
              * Se `concluida == true`, avança o programa: `processador.avancarPC()`.
            * Se for uma instrução exigente de intervenção do Kernel (E/S, Memória, Semáforo, Recursos, Arquivos):
              * Invoca a chamada de sistema correspondente via `SystemCalls.executarSyscall(processador, instrucao, this)`.
       5. Avalia condições de preempção:
          * Testa se o escalonador sinaliza preempção por estouro de quantum via `escalonador.devePreempter(processador)`.
          * Em caso afirmativo, transiciona o `processador` para `PRONTO`, reinsere-o no `escalonador` via `escalonador.adicionarProcesso(processador)` e define `processador = null`.

#### `GerenciadorProcessos.java`
* Administra a criação, o registro global e a destruição de PCBs no kernel.
* **Atributos:**
  * `tabelaProcessos` (`Map<Integer, Processo>`): Tabela hash relacionando PIDs com suas instâncias de `Processo`.
  * `contadorPid` (`AtomicInteger`): Gerador thread-safe de PIDs sequenciais.
* **Métodos:**1. `criarProcesso(String nome, List<Instrucao> instrucoes, int prioridade)`:
     * **Parâmetros:** `nome` (`String`), `instrucoes` (`List<Instrucao>`), `prioridade` (`int`).
     * **Retorno:** `Processo`.
     * **Lógica:** Obtém um identificador único executando `int pid = contadorPid.incrementAndGet()`. Instancia `Processo p = new Processo(pid, nome, instrucoes, prioridade)`. Insere o registro na tabela executando `tabelaProcessos.put(pid, p)`. Retorna a referência `p`.
  2. `destruirProcesso(int pid, Kernel kernel)`:
     * **Parâmetros:** `pid` (`int`), `kernel` (`Kernel`).
     * **Retorno:** `boolean`.
     * **Lógica:** Consulta a existência do registro na tabela `Processo p = tabelaProcessos.get(pid)`. Se `p == null`, retorna `false`. Caso contrário:
       * Percorre e remove o mapeamento de molduras alocadas na memória virtual: `kernel.getMemoriaVirtual().liberarMemoriaProcesso(pid)`.
       * Libera todos os recursos alocados ao processo no `AlgoritmoBanqueiro`.
       * Remove o processo da tabela global: `tabelaProcessos.remove(pid)`.
       * Define o estado `p.setEstado(EstadoProcesso.TERMINADO, kernel.getRelogio())` e retorna `true`.

#### `SystemCalls.java`
* Ponto de entrada unificado (*dispatcher*) para interrupções de software solicitadas pelos processos.
* **Métodos:**1. `executarSyscall(Processo p, Instrucao inst, Kernel kernel)`:
     * **Parâmetros:** `p` (`Processo`), `inst` (`Instrucao`), `kernel` (`Kernel`).
     * **Retorno:** `void`.
     * **Lógica (Estrutura Condicional `switch(inst.getTipo())`):**
       * `case SEM_WAIT`:
         * Extrai o nome do semáforo: `String semNome = inst.getParametro1AsString()`.
         * Invoca `kernel.getGerenciadorSincronizacao().wait(semNome, p, kernel)`.
         * Se o processo for bloqueado dentro do semáforo, desassocia o processo do processador: `kernel.desalocarCPU()`.
         * Avança o program counter: `p.avancarPC()`.
       * `case SEM_SIGNAL`:
         * Extrai `String semNome = inst.getParametro1AsString()`.
         * Invoca `kernel.getGerenciadorSincronizacao().signal(semNome, kernel)`.
         * Avança o program counter: `p.avancarPC()`.
       * `case MEM_READ` / `case MEM_WRITE`:
         * Extrai o endereço virtual: `int endVirt = inst.getParametro1AsInt()`.
         * Solicita a tradução ao subsistema de memória: `int endFisico = kernel.getMemoriaVirtual().traduzirEndereco(p, endVirt, inst.getTipo() == TipoInstrucao.MEM_WRITE)`.
         * Se `endFisico == -1` (ocorrência de *Page Fault*):
           * Altera estado de `p` para `BLOQUEADO`.
           * Aciona tratamento: `kernel.getMemoriaVirtual().tratarPageFault(p, endVirt, kernel)`.
           * Remove `p` da CPU (não avança `pc` para reexecutar a instrução após o tratamento).
         * Caso contrário (`endFisico >= 0`), a instrução conclui com sucesso: avança `p.avancarPC()`.
       * `case REQ_RECURSO`:
         * Extrai o tipo do recurso e a quantidade solicitada dos parâmetros.
         * Invoca `kernel.getAlgoritmoBanqueiro().requisitarRecurso(p, idRecurso, qtd)`.
         * Se a requisição for negada por segurança, bloqueia `p`. Caso aprovada, aloca o recurso e avança `p.avancarPC()`.
       * `case FS_READ` / `case FS_WRITE`:
         * Encaminha os parâmetros de arquivo e bloco ao `kernel.getGerenciadorFS()`. Avança `p.avancarPC()`.

### 2.3 `minikernel.sched` — Escalonamento

#### `TipoAlgoritmo.java` (Enum)
* Enumeração das políticas de escalonamento suportadas.
* **Constantes:** `FCFS`, `SJF`, `SRTF`, `ROUND_ROBIN`, `PRIORIDADE_PREEMPTIVO`, `PRIORIDADE_NAO_PREEMPTIVO`.

#### `Escalonador.java`
* Mantém as filas de processos prontos e aplica a lógica de seleção de CPU, preempção por tempo e envelhecimento (*aging*).
* **Atributos:**
  * `filaProntos` (`List<Processo>`): Lista gerenciada dos processos aptos a executar.
  * `algoritmoAtivo` (`TipoAlgoritmo`): Política de ordenação utilizada.
  * `quantum` (`int`): Duração máxima contínua de uso da CPU no Round-Robin.
  * `contadorQuantum` (`int`): Contador de ciclos consumidos pelo processo ativo.
  * `fatorAging` (`int`): Intervalo de tempo para aumento de prioridades estagnadas.
* **Métodos:**1. `Escalonador(TipoAlgoritmo algoritmo, int quantum)`:
     * **Lógica:** Salva os parâmetros. Instancia `filaProntos` como uma `LinkedList` thread-safe, e inicializa `contadorQuantum = 0`.
  2. `adicionarProcesso(Processo p)`:
     * **Parâmetros:** `p` (`Processo`).
     * **Lógica:** Altera o estado do processo `p.setEstado(EstadoProcesso.PRONTO)`. Insere `p` em `filaProntos`. Invoca a ordenação interna da fila: `ordenarFila()`.
  3. `ordenarFila()`:
     * **Lógica:** Aplica ordenação com base no `algoritmoAtivo`:
       * Se `SJF`: Ordena por menor número de ciclos contidos na instrução corrente ou tamanho total.
       * Se `SRTF`: Ordena por menor quantidade total de ciclos restantes de instrução (`instrucoes.size() - pc`).
       * Se `PRIORIDADE_PREEMPTIVO` ou `PRIORIDADE_NAO_PREEMPTIVO`: Ordena comparando `p1.getPrioridade()` com `p2.getPrioridade()`.
  4. `proximoProcesso(Processo processoAtual)`:
     * **Parâmetros:** `processoAtual` (`Processo`).
     * **Retorno:** `Processo` (ou `null`).
     * **Lógica:**
       * Se `filaProntos.isEmpty()`, retorna `processoAtual`.
       * Se `processoAtual == null`, remove e retorna o primeiro elemento: `filaProntos.remove(0)`.
       * Se o algoritmo for preemptivo (`SRTF` ou `PRIORIDADE_PREEMPTIVO`):
         * Compara o topo da `filaProntos` com `processoAtual`. Se o processo da fila tiver maior prioridade/menor tempo restante, realiza a troca preempção.
       * Caso contrário, se `processoAtual` estiver em execução e não houver preempção, retorna `processoAtual`.
  5. `aplicarAging(long relogioAtual)`:
     * **Parâmetros:** `relogioAtual` (`long`).
     * **Lógica:** Percorre `filaProntos` utilizando um laço `for`. Para cada processo `p`, calcula o tempo acumulado na fila de espera `(relogioAtual - p.getUltimoMomentoPronto())`. Se a espera for superior ao limite do `fatorAging`, incrementa a prioridade `p.setPrioridade(p.getPrioridade() + 1)` e reordena a fila.

### 2.4 `minikernel.mem` — Memória virtual

#### `AlgoritmoSubstituicao.java` (Enum)
* Enumeração das políticas de substituição de páginas (`FIFO`, `LRU`).

#### `EntradaTabelaPaginas.java`
* Encapsula o mapeamento e os bits de controle de uma única página no espaço virtual.
* **Atributos:** `numeroPagina` (`int`), `numeroMoldura` (`int`), `presente` (`boolean`), `modificado` (`boolean`), `ultimoAcesso` (`long`), `momentoCarregamento` (`long`).
* **Métodos:**1. `EntradaTabelaPaginas(int numeroPagina)`:
     * **Lógica:** Inicializa `this.numeroPagina = numeroPagina`, `numeroMoldura = -1`, `presente = false`, `modificado = false`, `ultimoAcesso = 0` e `momentoCarregamento = 0`.
  2. `mapearMoldura(int moldura, long relogio)`:
     * **Lógica:** Define `this.numeroMoldura = moldura`, `presente = true`, `momentoCarregamento = relogio` e `ultimoAcesso = relogio`.

#### `TabelaPaginas.java`
* Tabela de mapeamento de páginas de um processo individual.
* **Atributos:** `pidProcesso` (`int`), `mapeamentos` (`Map<Integer, EntradaTabelaPaginas>`).
* **Métodos:**1. `getEntradaOuCriar(int numeroPagina)`:
     * **Parâmetros:** `numeroPagina` (`int`).
     * **Retorno:** `EntradaTabelaPaginas`.
     * **Lógica:** Consulta `mapeamentos.get(numeroPagina)`. Se retornar `null`, instancia `EntradaTabelaPaginas nova = new EntradaTabelaPaginas(numeroPagina)`, armazena com `mapeamentos.put(numeroPagina, nova)` e a retorna.

#### `MemoriaVirtual.java`
* Controla a alocação da RAM física (dividida em molduras/frames) e o arquivo de troca (*swap*).
* **Atributos:**
  * `tamanhoPagina` (`int`): Tamanho fixo da página/moldura em bytes.
  * `totalMolduras` (`int`): Quantidade total de molduras na RAM.
  * `moldurasLivres` (`BitSet`): Mapa de bits indicando ocupação de molduras na RAM.
  * `filaFIFO` (`Queue<EntradaTabelaPaginas>`): Fila de páginas para a política FIFO.
  * `algoritmoSubstituicao` (`AlgoritmoSubstituicao`): Política ativa.
* **Métodos:**1. `traduzirEndereco(Processo p, int endVirtual, boolean eEscrita)`:
     * **Parâmetros:** `p` (`Processo`), `endVirtual` (`int`), `eEscrita` (`boolean`).
     * **Retorno:** `int` (Endereço Físico ou `-1` em Page Fault).
     * **Lógica:**
       1. Calcula o número da página virtual: `int numPagina = endVirtual / tamanhoPagina`.
       2. Calcula o deslocamento (*offset*): `int offset = endVirtual % tamanhoPagina`.
       3. Obtém a entrada da tabela do processo: `EntradaTabelaPaginas entrada = p.getTabelaPaginas().getEntradaOuCriar(numPagina)`.
       4. Condicional de Validação: Se `entrada.isPresente() == false`, lança falta de página e retorna `-1`.
       5. Se presente:
          * Atualiza o timestamp de acesso: `entrada.setUltimoAcesso(System.currentTimeMillis())`.
          * Se `eEscrita == true`, marca o bit de modificação: `entrada.setModificado(true)`.
          * Calcula o endereço físico traduzido: `return (entrada.getNumeroMoldura() * tamanhoPagina) + offset`.
  2. `tratarPageFault(Processo p, int endVirtual, Kernel kernel)`:
     * **Parâmetros:** `p` (`Processo`), `endVirtual` (`int`), `kernel` (`Kernel`).
     * **Lógica:**
       1. Notifica os observadores registrados: `kernel.notificarPageFault(p.getPid(), numPagina)`.
       2. Busca uma moldura livre no `BitSet`: `int molduraLivre = moldurasLivres.nextClearBit(0)`.
       3. **Alocação direta:** Se `molduraLivre < totalMolduras`:
          * Marca a moldura como ocupada: `moldurasLivres.set(molduraLivre)`.
          * Mapeia na entrada da tabela: `entrada.mapearMoldura(molduraLivre, kernel.getRelogio())`.
          * Registra a entrada na `filaFIFO`.
          * Transiciona o processo de volta para `PRONTO`.
       4. **Substituição quando a RAM está cheia (`molduraLivre >= totalMolduras`):**
          * Invoca `substituirPagina(kernel)` para selecionar a página vítima segundo a política (`FIFO` ou `LRU`).
          * Salva a página vítima em *swap* caso `vitima.isModificado() == true`.
          * Desaloca a moldura da vítima, transfere a posse para a nova página e ajusta os mapeamentos das tabelas envolvidas.

### 2.5 `minikernel.sync` — Sincronização

#### `Semaforo.java`
* Primitiva de sincronização de Dijkstra baseada em um contador inteiro e uma fila de processos bloqueados.
* **Atributos:** `nome` (`String`), `valor` (`int`), `filaBloqueados` (`Queue<Processo>`).
* **Métodos:**1. `wait(Processo p, Kernel kernel)`:
     * **Parâmetros:** `p` (`Processo`), `kernel` (`Kernel`).
     * **Lógica:** Decrementa o contador interno: `valor--`. Se `valor < 0`:
       * Altera o estado do processo `p.setEstado(EstadoProcesso.BLOQUEADO, kernel.getRelogio())`.
       * Insere o processo na fila: `filaBloqueados.add(p)`.
  2. `signal(Kernel kernel)`:
     * **Parâmetros:** `kernel` (`Kernel`).
     * **Lógica:** Incrementa o contador: `valor++`. Se `valor <= 0` e `filaBloqueados` não estiver vazia:
       * Remove o processo no topo da fila: `Processo liberado = filaBloqueados.poll()`.
       * Altera o estado para `PRONTO`: `liberado.setEstado(EstadoProcesso.PRONTO, kernel.getRelogio())`.
       * Inserção na fila de prontos do escalonador: `kernel.getEscalonador().adicionarProcesso(liberado)`.

#### `GerenciadorSincronizacao.java`
* Mantém o dicionário global de semáforos ativos identificados por nome.
* **Atributos:** `semaforos` (`Map<String, Semaforo>`).
* **Métodos:**1. `getOuCriarSemaforo(String nome, int valorInicial)`:
     * **Lógica:** Verifica se `semaforos.containsKey(nome)`. Se não existir, instancia `Semaforo s = new Semaforo(nome, valorInicial)` e salva no mapa. Retorna a referência.

### 2.6 `minikernel.fs` — Sistema de arquivos

#### `BlocoDisco.java`
* Modela o bloco físico individual de dados no disco simulado.
* **Atributos:** `idBloco` (`int`), `dados` (`byte[]`), `ocupado` (`boolean`).

#### `EntradaDiretorio.java`
* Estrutura de entrada do diretório raiz, contendo os metadados do arquivo.
* **Atributos:** `nomeArquivo` (`String`), `blocoInicial` (`int`), `tamanhoBytes` (`int`).

#### `GerenciadorFS.java`
* Implementa um sistema de arquivos baseado na Tabela de Alocação de Arquivos (FAT - *File Allocation Table*) e mapa de bits de blocos livres.
* **Atributos:**
  * `tabelaFAT` (`int[]`): Vetor representando a tabela FAT (contém ponteiro para o próximo bloco ou `-1` para EOF).
  * `mapaBits` (`BitSet`): BitSet sinalizador de blocos livres no disco.
  * `diretorioRaiz` (`Map<String, EntradaDiretorio>`): Catálogo de arquivos do sistema.
  * `blocos` (`BlocoDisco[]`): Vetor de blocos simulados de armazenamento.
* **Métodos:**1. `escreverArquivo(String nome, byte[] conteudo)`:
     * **Parâmetros:** `nome` (`String`), `conteudo` (`byte[]`).
     * **Lógica:**
       1. Calcula quantos blocos são necessários: `int numBlocos = ceil(conteudo.length / tamanhoBloco)`.
       2. Percorre `mapaBits` procurando `numBlocos` livres. Se não houver capacidade suficiente, lança `DiskFullException`.
       3. Para cada segmento de dados:
          * Ocupa o bloco no `mapaBits`.
          * Escreve os bytes no `BlocoDisco`.
          * Atualiza o índice na `tabelaFAT` apontando para o próximo bloco alocado (ou `-1` para o último).
       4. Registra a `EntradaDiretorio` no `diretorioRaiz`.

### 2.7 `minikernel.deadlock` — Controle de impasses

#### `AlgoritmoBanqueiro.java`
* Implementação do Algoritmo do Banqueiro para verificação de estados seguros na alocação de múltiplos recursos.
* **Atributos:**
  * `disponivel` (`int[]`): Vetor de instâncias disponíveis por tipo de recurso.
  * `maximo` (`int[][]`): Matriz de exigência máxima `[Processo, Recurso]`.
  * `alocacao` (`int[][]`): Matriz de recursos correntemente alocados `[Processo, Recurso]`.
  * `necessidade` (`int[][]`): Matriz calculada (`maximo - alocacao`).
* **Métodos:**1. `requisitarRecurso(Processo p, int idRecurso, int quantidade)`:
     * **Parâmetros:** `p` (`Processo`), `idRecurso` (`int`), `quantidade` (`int`).
     * **Retorno:** `boolean` (Aprovado / Negado).
     * **Lógica:**
       1. Condicional de Validação: Se `quantidade > necessidade[p.getPid()][idRecurso]`, interrompe por erro (excedeu o máximo declarado).
       2. Condicional de Disponibilidade: Se `quantidade > disponivel[idRecurso]`, nega a alocação (recurso insuficiente).
       3. **Simulação:**
          * Decrementa provisoriamente: `disponivel[idRecurso] -= quantidade`.
          * Incrementa provisoriamente: `alocacao[p.getPid()][idRecurso] += quantidade`.
          * Decrementa necessidade: `necessidade[p.getPid()][idRecurso] -= quantidade`.
       4. Executa o **Algoritmo de Estado Seguro (`isEstadoSeguro()`)**:
          * Cria cópias locais de trabalho `work = disponivel.clone()` e vetor booleano `finish[numProcessos] = false`.
          * Em laço `while`, procura por um processo `i` que satisfaça `finish[i] == false` e `necessidade[i] <= work`.
          * Se encontrar, simula a conclusão de `i`: adiciona seus recursos liberados ao `work` (`work += alocacao[i]`), seta `finish[i] = true` e reinicia a busca no laço.
          * Se todos os processos finalizarem (`finish` contiver apenas `true`), o estado é **seguro**. Efetiva as alterações e retorna `true`.
          * Caso contrário, o estado é **inseguro**: desfaz as alterações simuladas e retorna `false` (bloqueando o processo).

### 2.8 `minikernel.app` e `minikernel.ui` — Aplicação e interface

#### `SimuladorGUI.java` (`minikernel.app`)
* Ponto de entrada executável do aplicativo Java Swing (`JFrame`). Controla os menus, a barra de ferramentas e os botões de simulação (Play, Pause, Step).
* **Atributos:** `kernel` (`Kernel`), `timerSimulacao` (`javax.swing.Timer`), componentes de interface visual.
* **Métodos:**1. `main(String[] args)`:
     * **Lógica:** Configura o *Look and Feel* do sistema e instancia a janela `SimuladorGUI` na thread do Event Dispatch Thread (`SwingUtilities.invokeLater`).
  2. `iniciarSimulacao()`:
     * **Lógica:** Instancia e inicia um `javax.swing.Timer` com o *delay* configurado. A cada pulso do *timer*, invoca `kernel.executarCiclo()`, desencadeando a atualização encadeada dos painéis ouvintes.

#### `PainelProcessos.java`, `PainelMemoria.java`, `PainelFS.java`, `PainelDeadlock.java` (`minikernel.ui`)
* Componentes visuais gráficos que implementam a interface `KernelObserver`.
* **Métodos:**1. `onMudancaEstado(...)` / `onPageFault(...)` / `onTrocaContexto(...)`:
     * **Lógica:** Quando estes métodos de notificação são chamados pelo `Kernel`, os painéis coletam as estruturas atualizadas e disparam `repaint()`. Isso faz com que a interface Swing invoque `paintComponent(Graphics g)`, que redesenha as tabelas, gráficos de Gantt, barras de ocupação de RAM e o grafo de recursos.

## 3. Chamadas de sistema

| Tipo de Instrução | Subsistema Destino | Ação Executada no Kernel | Transição de Estado do Processo |
| :--- | :--- | :--- | :--- |
| `CPU` | Processador | Executa 1 ciclo da instrução localmente no processador. | Mantém `EXECUTANDO`. |
| `SEM_WAIT` | `GerenciadorSincronizacao` | Decrementa contador do semáforo. Se `valor < 0`, insere na fila. | Transiciona para `BLOQUEADO` (se `valor < 0`). |
| `SEM_SIGNAL` | `GerenciadorSincronizacao` | Incrementa contador. Se `valor <= 0`, libera o primeiro da fila. | O processo liberado passa para `PRONTO`. |
| `MEM_READ` | `MemoriaVirtual` | Traduz endereço virtual para físico. Se ausente, gera *Page Fault*. | Se Page Fault: `BLOQUEADO`. Senão: `EXECUTANDO`. |
| `MEM_WRITE` | `MemoriaVirtual` | Traduz endereço e ativa bit de modificação (*dirty bit*). | Se Page Fault: `BLOQUEADO`. Senão: `EXECUTANDO`. |
| `REQ_RECURSO` | `AlgoritmoBanqueiro` | Valida requisição via Algoritmo do Banqueiro (Estado Seguro). | Se inseguro: `BLOQUEADO`. Se seguro: `EXECUTANDO`. |
| `REL_RECURSO` | `AlgoritmoBanqueiro` | Libera recursos alocados e reavalia solicitações pendentes. | Mantém `EXECUTANDO`. |
| `FS_READ` | `GerenciadorFS` | Lê blocos do arquivo via tabela FAT. | Transiciona para `BLOQUEADO` durante a E/S. |
| `FS_WRITE` | `GerenciadorFS` | Aloca blocos no mapa de bits, atualiza a FAT e grava dados. | Transiciona para `BLOQUEADO` durante a E/S. |

## 4. Ciclo de vida do processo

```
                     +-------------------+
                     |       NOVO        |
                     +-------------------+
                               |
                               | (Criação do Processo / Alocação do PCB)
                               v
                     +-------------------+
      +------------> |      PRONTO       | <-----------+
      |              +-------------------+             |
      |                        |                       |
      | (Desbloqueio /         | (Escalonamento pela   | (Preempção por Quantum /
      |  E/S Concluída /       |  CPU / Context        |  Algoritmo de Prioridade)
      |  Recurso Liberado)     |  Switch)              |
      |                        v                       |
+--------------+     +-------------------+             |
|  BLOQUEADO   | <-- |    EXECUTANDO     | ------------+
+--------------+     +-------------------+
  (Wait Semáforo /             |
   Page Fault /                | (Término do Programa /
   Recurso Inseguro)           |  Erro de Execução)
                               v
                     +-------------------+
                     |    TERMINADO      |
                     +-------------------+
```
