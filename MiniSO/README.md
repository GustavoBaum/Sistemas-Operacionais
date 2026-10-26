# Mini-SO: Simulador Educacional de Núcleo de Sistema Operacional

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-Modular-green.svg)](#estrutura-do-projeto)

O **Mini-SO** é um simulador de núcleo operacional (*kernel*) desenvolvido em **Java 17+** focado no ensino e na demonstração prática dos conceitos fundamentais de Sistemas Operacionais. O projeto implementa, de forma isolada e integrada, componentes essenciais de concorrência, escalonamento de processos, memória virtual paginada, controle de impasses e sistemas de arquivos estruturados em FAT.

---

##  Destaques do Projeto

- **Arquitetura Modular Solida:** Cada subsistema é exposto através de interfaces claras, permitindo execuções totalmente independentes ou integradas.
- **Concorrência Real:** Algoritmos de sincronização utilizam threads nativas e estruturas concorrentes Java para demonstrar problemas clássicos sem *busy waiting*.
- **Métricas e Telemetria:** Cálculo em tempo real de Tempo Médio de Espera (TME), Tempo Médio de Turnaround (TMT), frequência de *Page Faults* e relatórios de execução.
- **Simulação Interativa e Integrada:** Interface de terminal para simulações passo a passo (por *ticks* de clock) ou demonstração completa do fluxo de vida dos processos.

---

##  Módulos do Sistema

O simulador é subdividido em 5 subsistemas especializados mais um modo de simulação integrada:

| Módulo | Escopo & Algoritmos | Destaques Técnicos |
| :--- | :--- | :--- |
| **1. Concorrência e Sincronização** | Produtor/Consumidor, Jantar dos Filósofos | Semáforos lógicos, Mútex, Prevenção de Impasses (Semáforo de Sala) vs. Abordagem Ingênua (4 Condições de Coffman). |
| **2. Escalonamento de CPU** | Round-Robin + *Aging* | Controle por quantum configurável, prevenção de inanição (*starvation*), geração de Diagrama de Gantt, TME e TMT. |
| **3. Memória Virtual e MMU** | Paginação e Substituição de Páginas | Tradução Virtual $\rightarrow$ Física via MMU, tratamento de *Page Faults*, comparação **FIFO** vs. **LRU**, demonstração da Anomalia de Belady. |
| **4. Impasses / Deadlocks** | Algoritmo do Banqueiro e RAG (*Resource Allocation Graph*) | Avaliação de estados seguros/inseguros via matriz *Need*, detecção de ciclos em grafos dirigidos e estratégias de recuperação. |
| **5. Sistema de Arquivos** | Sistema baseado em Tabela FAT | Mapeamento em disco simulado (16 blocos), mapa de bits de blocos livres, alocação encadeada, fragmentação e tratamento de limite. |
| **6. Simulação Integrada** | *Kernel Loop* (Proto-SO) | Integração de todos os módulos em um laço único de execução por interrupção de *clock ticks*. |

---

##  Estrutura do Projeto

A arquitetura do código-fonte é organizada dentro do pacote `minikernel`:

```text
src/minikernel/
├── app/        # Ponto de entrada (Main), ModuloIntegrado e cenários de execução
├── core/       # Tipos fundamentais: PCB (Processo), Estado, Instrucao, Eventos
├── deadlock/   # Algoritmo do Banqueiro e Grafo de Alocação de Recursos (RAG)
├── fs/         # Sistema de Arquivos (FAT16 simulado e Mapa de Bits)
├── kernel/     # Despachador central de chamadas de sistema (Syscall Dispatcher)
├── mem/        # Gerenciamento de Memória Virtual, MMU e Políticas (FIFO/LRU)
├── sched/      # Escalonador de CPU (Round-Robin com Aging)
├── sync/       # Primitivas de Sincronização e problemas clássicos de concorrência
└── ui/         # Interface em linha de comando (CLI) e contratos de módulo
```

---

##  Como Executar

### Pré-requisitos

- **JDK 17 ou superior** instalado (recomendado [Eclipse Temurin 21](https://adoptium.net)).
- Verifique a instalação com:
  ```bash
  javac -version
  ```

---

### Execução Rápida (Scripts Automatizados)

Navegue até o diretório raiz do projeto no terminal e execute o script correspondente ao seu sistema operacional:

- **Windows:**
  ```cmd
  .\rodar.bat
  ```
- **Linux / macOS:**
  ```bash
  chmod +x rodar.sh
  ./rodar.sh
  ```

---

### Compilação e Execução Manual via Terminal

Para compilar e rodar manualmente via CLI:

1. **Compilar os fontes:**
   ```bash
   javac -encoding UTF-8 -sourcepath src -d bin src/minikernel/app/Main.java
   ```

2. **Executar a aplicação:**
   ```bash
   java -cp bin minikernel.app.Main
   ```

> **Nota para usuários Windows:** Caso os caracteres acentuados não sejam exibidos corretamente no Prompt de Comando / PowerShell, altere o *codepage* para UTF-8 antes da execução:
> ```cmd
> chcp 65001
> ```

---

### Configuração no Visual Studio Code

1. Instale o pacote de extensões **Extension Pack for Java** (Microsoft).
2. Abra a pasta raiz do projeto (`File > Open Folder...`). Certifique-se de selecionar a pasta que contém o diretório `src/`.
3. Aguarde o serviço de linguagem Java inicializar (`Java: Ready`).
4. Abra o arquivo `src/minikernel/app/Main.java` e clique no botão **Run** acima do método `main`, ou pressione `F5`.

---

##  Extensibilidade

O projeto foi construído para facilitar a extensão acadêmica. Para adicionar um novo módulo ao simulador:

1. Crie uma nova classe implementando a interface `minikernel.ui.Modulo`.
2. Implemente os métodos `getTitulo()` e `executar(EntradaSaida io)`.
3. Registre a nova classe na lista de módulos em `minikernel.app.Main`.

---

## 📄 Licença

Este projeto está licenciado sob a Licença **MIT** — consulte o arquivo [LICENSE](LICENSE) para obter mais detalhes.
