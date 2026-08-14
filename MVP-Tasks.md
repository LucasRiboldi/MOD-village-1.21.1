# MVP-Tasks.md

# Village Colony — MVP Development Tasks

**Version:** 1.1.0

**Status:** Approved

**Last Update:** 2026-08-13 — estado anotado por tarefa; as divergências
que viviam numa nota de rodapé do Project-State passaram para o lado da
tarefa a que pertencem

---

# Objetivo

Definir a sequência de implementação do MVP.

O objetivo é transformar a arquitetura documentada em um mod Fabric funcional.

---

# Regra de Desenvolvimento

Cada tarefa deve:

* possuir uma responsabilidade única;
* gerar um resultado observável;
* não quebrar sistemas anteriores.

---

# Como ler o estado

Este documento é o **plano**. O estado detalhado — com o nome das classes
que cada tarefa produziu — está em `docs/technical/Project-State.md` §6, e
o histórico em `Development-Log.md`. Onde os dois discordarem, vale o
Project-State: ele é atualizado ao fim de cada sessão.

O que cada marca significa:

```text
FEITO EM JOGO      escrito, coberto por teste e visto rodando no jogo
                   real. É o único nível que este projeto trata como
                   prova — ver Project-State §11

FEITO              escrito e coberto por teste; a fronteira com o
                   Minecraft não foi exercitada, ou não existe nesta
                   tarefa

NÃO VISTO          escrito e coberto por teste de jogo, mas nunca
                   rodado no jogo real. É dívida, não conclusão

CANCELADA          deixou de fazer sentido; o motivo fica registrado
                   junto da tarefa

NÃO INICIADA       nada escrito
```

---

# Fase 0 — Preparação do Projeto

## TASK-001 — Criar Projeto Fabric

Estado: `FEITO EM JOGO`

Objetivo:

Criar o projeto base.

Implementar:

* Fabric Loader;
* Fabric API;
* Minecraft 1.21.1;
* Java configurado.

Resultado esperado:

```text
Minecraft inicia com o mod carregado.
```

---

## TASK-002 — Configurar Identidade do Mod

Estado: `FEITO EM JOGO`

Criar:

* mod id;
* nome;
* versão;
* arquivo fabric.mod.json.

Resultado:

```text
Fabric reconhece Village Colony.
```

---

## TASK-003 — Criar Estrutura de Pacotes

Estado: `FEITO`

Criar:

```text
core/

fabric/

data/
```

Resultado:

Código preparado para crescimento.

O layout definitivo não é mais este: quem manda é a **ADR-006**, e
`Class-Architecture.md` e `Initial-Setup-Checklist.md` §6 apontam para
ela em vez de repetir a estrutura.

---

# Fase 1 — Núcleo da Colônia

---

## TASK-004 — Criar Classe Principal do Mod

Estado: `FEITO EM JOGO`

Criar:

```text
VillageColonyMod
```

Responsável por:

* inicialização;
* registro de eventos.

Resultado:

Mod inicia corretamente.

---

## TASK-005 — Criar Modelo Colony

Estado: `FEITO`

Criar:

```text
Colony
```

Contendo:

* id;
* posição;
* estado.

Resultado:

A colônia pode existir em memória.

---

## TASK-006 — Criar Colony Service

Estado: `FEITO`

Criar:

```text
ColonyService
```

Responsável por:

* registrar colônias;
* buscar colônias existentes.

Resultado:

O jogo consegue administrar colônias.

Esta tarefa dizia **ColonyManager** até 2026-08-13. A **ADR-006 §5**
removeu *manager* como camada do projeto, e a classe que existe — e
sempre existiu — é `ColonyService`. O nome antigo ficou aqui por descuido,
não por decisão.

---

# Fase 2 — Persistência

---

## TASK-007 — Criar Colony Saved Data

Estado: `FEITO EM JOGO`

Criar:

```text
ColonySavedData
```

Salvar:

* colônias;
* identificadores;
* posições.

Resultado:

Dados sobrevivem ao fechar mundo.

---

## TASK-008 — Testar Carregamento

Estado: `FEITO EM JOGO`

Validar:

```text
Criar mundo

↓

Encontrar vila

↓

Salvar

↓

Fechar

↓

Abrir

↓

Colony permanece
```

O passo "Encontrar vila" é a TASK-009, que vem depois desta. A tarefa
depende de outra posterior a ela e por isso foi executada fora de ordem.
Fica registrado como está: renumerar agora quebraria as referências
espalhadas pelo Development Log.

---

# Fase 3 — Detecção da Vila

---

## TASK-009 — Detectar Estruturas Vanilla

Estado: `FEITO EM JOGO`

Implementar:

* busca por vila;
* identificação do bioma.

MVP:

Somente:

```text
Plains Village
```

---

## TASK-010 — Criar Colônia Automaticamente

Estado: `FEITO EM JOGO`

Quando vila encontrada:

Criar:

```text
Colony
```

Resultado:

A vila passa a existir como entidade lógica.

---

# Fase 4 — Sistema de Trabalhadores

---

## TASK-011 — Criar Worker Model

Estado: `FEITO`

Criar:

```text
Worker
```

Dados:

* UUID;
* profissão;
* colônia.

---

## TASK-012 — Detectar Aldeões

Estado: `FEITO EM JOGO`

Implementar:

```text
VillagerScanner
```

Resultado:

Todos aldeões da vila são registrados.

---

## TASK-012b — Persistir Trabalhadores

Estado: `FEITO EM JOGO`

Estender:

```text
ColonySavedData
```

Decisão (2026-08-07): estender o save existente em vez de criar um
`WorkerSavedData`. Um segundo arquivo permitiria worker órfão apontando
para colônia que não foi gravada, sem transação que mantivesse os dois
em sincronia.

Gravar por trabalhador:

* villagerId;
* colonyId;
* profissão, quando houver.

Carregar com `Worker.restore`, que já aceita profissão ausente.

Motivo:

Profissão de colônia é decisão do mod, não existe no mundo Vanilla e
sumiria ao fechar o mundo. Sem isto, cada sessão redistribuiria funções
do zero e a TASK-014 não se sustenta.

Esta tarefa não constava do plano original. Recebeu letra em vez de
número para não renumerar as seguintes. Ver Project-State §13.

---

## TASK-013 — Criar Sistema de Profissões

Estado: `FEITO EM JOGO`

Implementar:

```text
ProfessionRegistry
```

Adicionar:

* Lumberjack;
* Manufacturer;
* Farmer;
* Builder.

---

## TASK-014 — Atribuição Inicial de Profissões

Estado: `FEITO EM JOGO`

Quando faltar profissão:

Fluxo:

```text
Novo aldeão

↓

Verificar vaga

↓

Receber função
```

Quantas vagas há por profissão é a **Regra 4** do autor, de 2026-08-13:
duas de cada. O enunciado mora em Project-State §18, não aqui — regra do
autor muda, e o plano não deve carregar número que envelhece.

---

# Fase 5 — Sistema de Armazenamento

---

## TASK-015 — Detectar Baús dos Trabalhadores

Estado: `FEITO EM JOGO`

Implementar:

Busca:

```text
Casa

↓

Cama

↓

Baú próximo
```

"Próximo" virou regra em 2026-08-08: linha livre entre a cama e o baú, no
mesmo nível. Parede desqualifica. Ver Project-State §10.

---

## TASK-016 — Registrar Storage

Estado: `FEITO EM JOGO`

Criar:

```text
StorageRegistry
```

Guardar:

* posição;
* trabalhador proprietário.

---

## TASK-017 — Ler Inventário dos Baús

Estado: `FEITO EM JOGO`

Implementar:

Contagem de:

* Oak Log;
* Oak Planks;
* Cobblestone.

O escopo cresceu: são as oito espécies de árvore, e o espaço livre é
medido junto com o conteúdo — é dele que a Regra 1 tira a meta da
colheita.

---

# Fase 6 — Sistema de Recursos

---

## TASK-018 — Criar Resource Registry

Estado: `FEITO`

Implementar:

```text
ResourceRegistry
```

Responsável por:

* quantidade;
* localização.

A classe chamou-se `ColonyResources`.

---

## TASK-019 — Criar Verificação de Déficit

Estado: `FEITO`

Exemplo:

```text
Precisa:

64 Oak Planks


Possui:

20


Déficit:

44
```

O número fixo do exemplo não existe mais. A **Regra 1** trocou a meta
constante pelo espaço livre dos baús, e a **Regra 5** definiu a da tábua
como metade da capacidade da colônia. As duas estão em Project-State §18.

---

## TASK-020 — Integrar Recursos com Simulação

Estado: `FEITO EM JOGO`

A Colônia deve saber:

* o que possui;
* o que falta.

---

# Fase 7 — Sistema de Tarefas

---

## TASK-021 — Criar Task Model

Estado: `FEITO`

Criar:

```text
Task
```

Estados:

* AVAILABLE;
* RESERVED;
* EXECUTING;
* COMPLETED.

---

## TASK-022 — Criar Task Manager

Estado: `FEITO`

Responsável por:

* criar;
* buscar;
* finalizar tarefas.

A classe é `TaskService`, pelo mesmo motivo da TASK-006.

---

## TASK-023 — Associar Tarefas a Profissões

Estado: `FEITO EM JOGO`

Exemplo:

```text
Build House

↓

Builder
```

---

# Fase 8 — Primeiro Trabalhador Funcional

---

## TASK-024 — Implementar Lumberjack

Estado: `FEITO`

Capacidade:

```text
COLLECT_WOOD
```

---

## TASK-025 — Criar Coleta de Madeira

Estado: `FEITO EM JOGO` — 2026-08-08

Fluxo:

```text
Receber tarefa

↓

Encontrar árvore

↓

Quebrar bloco permitido

↓

Coletar item
```

O que a fase entregou além do previsto: movimento pelo Brain do aldeão
(ADR-004 §11), nome sobre a cabeça, e as Regras 1 e 2 — colher até os
baús encherem, e no tempo de um jogador com machado de ferro.

O que ainda não foi visto em jogo: o lado do cliente — nome, rachadura no
bloco e braço balançando. Ver Project-State §8, P2.

---

## TASK-026 — Depositar em Pacotes de 32

Estado: `CANCELADA` — 2026-08-08

Regra prevista:

```text
32 Oak Logs

↓

Retornar casa

↓

Depositar baú
```

A madeira vai direto para o baú do trabalhador, por decisão do autor. A
viagem de volta a cada 32 troncos perdeu o motivo, e a tarefa foi
encerrada sem ser feita. Ver Project-State §10.

---

# Fase 9 — Fabricação

---

## TASK-027 — Implementar Manufacturer

Estado: `FEITO`

Capacidade:

```text
CRAFT_ITEMS
```

---

## TASK-028 — Integrar Recipe Manager

Estado: `FEITO`

Usar:

Receitas Vanilla.

A classe é `CraftingLookup`: a receita é perguntada ao próprio jogo, não
escrita no mod.

---

## TASK-029 — Produzir Oak Planks

Estado: `NÃO VISTO` — escrito em 2026-08-13

Fluxo:

```text
Oak Log

↓

Oak Planks

↓

Baú
```

Esta é a primeira vez que o mod **diminui** o que o jogador tem: até aqui
a colônia só somava. Tirar item de baú não existia no código, e entrou
aqui como `ChestWithdrawer`.

O que uma sessão em jogo precisa mostrar — Project-State §8, P1c:

```text
a linha manufacturers: aparecendo no log

tábua entrando no baú e tronco sumindo na mesma conta — quatro por um

a colônia parando sozinha ao atingir a metade (Regra 5)
```

Enquanto isso não acontecer, a Fase 9 não está fechada.

---

# Fase 10 — Construção

Estado da fase: `NÃO INICIADA`

`core/construction` existe com os pacotes criados e nenhuma classe
dentro.

Três decisões do autor precisam vir antes da primeira linha de código:

```text
onde a colônia constrói    a vila cresce para onde? e a que distância
                           da última casa?

o que ela constrói         a casa de planície Vanilla, ou um projeto
                           próprio?

quando ela para            uma vila que cresce para sempre vira outra
                           coisa. A Regra 1 respondeu isso para a
                           colheita com o espaço dos baús; a construção
                           não tem equivalente óbvio
```

E uma quarta, de confirmação: a **Regra 5** foi decidida por delegação, e
o teto da metade some assim que a obra existir. A TASK-032 é onde isso
deixa de ser hipótese — vale confirmar o enunciado com o autor antes
dela, não depois.

O que já está pronto para esta fase: `BlockProtection` como porta única
do "posso mexer aqui?", `ManufacturerWork` como molde do trabalho que
consome estoque, e `TaskType.BUILD`, que `ColonyCycle.typeFor` já
alimenta.

---

## TASK-030 — Criar Blueprint

Estado: `NÃO INICIADA`

Representar:

* blocos;
* posições;
* materiais.

---

## TASK-031 — Ler Estrutura Vanilla

Estado: `NÃO INICIADA`

MVP:

```text
Plains Small House
```

Depende da decisão "o que ela constrói".

---

## TASK-032 — Calcular Materiais

Estado: `NÃO INICIADA`

Gerar:

Lista necessária.

É aqui que a demanda da obra substitui o teto da Regra 5.

---

## TASK-033 — Criar Build Task

Estado: `NÃO INICIADA`

Enviar para Builder.

---

## TASK-034 — Implementar Builder

Estado: `NÃO INICIADA`

Capacidade:

```text
BUILD_STRUCTURE
```

---

## TASK-035 — Colocar Blocos

Estado: `NÃO INICIADA`

Fluxo:

```text
Selecionar bloco

↓

Verificar material

↓

Colocar

↓

Registrar
```

Toda quebra e toda colocação passam por `BlockProtection` — a Regra 3 é
o que separa a obra da vila do jogador.

O passo "Registrar" é a Fase 11, e acontece no mesmo instante: colocar o
bloco e dizer de quem ele é não são dois momentos.

---

# Fase 11 — Registro de Infraestrutura

Estado da fase: `NÃO INICIADA`

Esta fase não é opcional nem posterior: a decisão de 2026-08-12 — duas
vilas viram uma quando um bloco de uma encostar no bloco da outra —
**depende** de saber que bloco pertence a qual colônia. Sem a Fase 11, a
fusão não tem como ser perguntada, e o risco de duas vilas disputarem
trabalhador continua aberto (Project-State §11).

Por isso ela caminha junto da TASK-035, e não depois dela.

---

## TASK-036 — Criar Building Registry

Estado: `NÃO INICIADA`

Salvar:

* posição;
* tipo;
* colônia.

---

## TASK-037 — Marcar Blocos da Colônia

Estado: `NÃO INICIADA`

Todo bloco colocado recebe origem:

```text
Colony Infrastructure
```

---

# Fase 12 — Testes do MVP

Estado da fase: `PARCIALMENTE PAGA`

Quatro das cinco validações já são feitas pela bateria de testes de jogo,
que roda num servidor sem cliente. O que sobra é a TASK-042, e ela sobra
por um motivo estrutural: a bateria sobe **um** servidor, e não há como
fechar e reabrir o mundo dentro dela.

Isso não dispensa a sessão em jogo. O §11 do Project-State registra
defeitos sérios que passaram por baterias inteiras verdes — o gametest
prova que o código não contradiz a si mesmo, não que ele funciona.

---

## TASK-038 — Teste Vila Inicial

Estado: `FEITO` — coberto por gametest e visto em jogo

Validar:

* vila encontrada;
* colônia criada.

---

## TASK-039 — Teste Trabalhadores

Estado: `FEITO` — coberto por gametest e visto em jogo

Validar:

* aldeões registrados;
* profissões atribuídas.

---

## TASK-040 — Teste Recursos

Estado: `FEITO` — coberto por gametest e visto em jogo

Validar:

* coleta;
* armazenamento;
* leitura.

---

## TASK-041 — Teste Construção

Estado: `NÃO INICIADA` — depende das Fases 10 e 11

Validar:

```text
Recursos

↓

Projeto

↓

Builder

↓

Casa nova
```

---

## TASK-042 — Teste Persistência

Estado: `NÃO INICIADA` — e não é automatizável na bateria atual

Validar:

* salvar;
* fechar;
* abrir;
* continuar.

Precisa ser feito à mão, no jogo real. As partes já verificadas — colônia
e trabalhadores sobrevivendo ao fechar o mundo — foram vistas nas
TASK-008 e TASK-012b; o que falta é o ciclo completo do MVP atravessando
uma reabertura.

---

# Fora do plano original

Tarefas que não constavam da lista e foram feitas porque o caminho pediu.
Ficam aqui para o plano não parecer maior do que o trabalho.

```text
marca do baú                 ChestMarker — um quadro com o ícone da
                             profissão, pregado no baú adotado.
                             FEITO EM JOGO

tirar item do baú            ChestWithdrawer — o mod nunca tinha feito
                             isso antes da Fase 9. FEITO

as oito espécies             TreeSpecies e ResourceGroup.PLANKS; o MVP
                             falava só em carvalho. FEITO, e só o
                             carvalho foi visto em jogo (E5)

movimento pelo Brain         ADR-004 §11. FEITO EM JOGO

liberar vaga na morte        VillagerLifecycleHandler — morte e
                             zumbificação devolvem vaga, baú e tarefa.
                             FEITO EM JOGO
```

---

# O que não é tarefa desta lista

Três itens aceitos em documento, fora de qualquer fase do MVP, feitos em
2026-08-13. O texto integral está em Project-State §8 e §9.

```text
ColonyState.ABANDONED        FEITO — a detecção passou a dizer o que
                             recusou, e a sonda da própria colônia
                             decide. ADR-003 §6

aviso de colônias            FEITO — dois centros a menos de 32 blocos
sobrepostas                  rendem um aviso por par por sessão. O
                             aviso não funde nada. ADR-003 §5

ferramenta inicial           FEITO — o trabalhador recebe a ferramenta
                             da profissão, e a devolve ao perder a
                             função. Não muda a velocidade de trabalho,
                             que a Regra 2 fixou em ferro
```

Nenhum foi visto em jogo: os três esperam a mesma sessão do TASK-029.

---

# Critério Final do MVP

O MVP está concluído quando:

```text
Uma vila Vanilla existe.                  FEITO EM JOGO

↓

A Colônia é criada.                       FEITO EM JOGO

↓

Aldeões recebem funções.                  FEITO EM JOGO

↓

Recursos são coletados.                   FEITO EM JOGO

↓

Recursos são armazenados.                 FEITO EM JOGO

↓

Materiais são produzidos.                 NÃO VISTO EM JOGO

↓

Uma nova casa é construída.               NÃO INICIADA

↓

A nova estrutura pertence à vila.         NÃO INICIADA
```

Cinco dos oito passos estão provados em jogo. O sexto está escrito e
esperando uma sessão. Os dois últimos são as Fases 10 e 11, e é por isso
que elas andam juntas.

---

# Próximas Expansões Após MVP

Não fazem parte desta versão:

* mineração;
* ferreiro;
* pedreiro;
* logística;
* transporte;
* defesa;
* distritos;
* múltiplas vilas;
* economia.
