# Reanálise das profissões: o que fazem, quanto demoram, onde param — 2026-10-02

> Pedido do autor: *"reanalise todas as profissões, mostre tudo que elas fazem
> e o tempo médio de cada atividade; depois verifique as possibilidades de
> deixar as atividades mais ágeis em velocidade normal, e as alternativas para
> ninguém ficar parado sem trabalhar, por erro ou trava, e liste isso"*.
>
> **Fonte:** quatro sessões, 130 minutos de log — 01-10 23:12, 02-10 00:00,
> 00:43 e 01:36. Medido com `scripts/activity_report.py` (novo, repetível):
> eventos concluídos e linhas de situação de 30 s.
>
> **Limite.** Estes logs são anteriores à Regra 50. A linha de situação só
> aparece para quem tem tarefa, então o tempo **ocioso** não está nas
> proporções abaixo. A partir do JAR `924f7c1e`, a linha `VC_TIME` mede todos,
> todo segundo (`scripts/time_ledger.py`). "Velocidade normal" quer dizer o
> mesmo relógio e a mesma Regra 2: o ganho vem do fluxo, não de acelerar a
> picareta.

---

## 1. Resumo

| Profissão | O que mais fez | Onde o tempo foi | Veredito |
|---|---|---|---|
| Lenhador (2) | 41 árvores, 6,4 toras cada | **66% procurando árvore**, 33% andando | busca lenta |
| Mineiro (até 3) | **0 pedra cavada**, 31 desistências | **79% andando** para pedra a 90 blocos; 3% cavando | travado no caminho |
| Construtor (1) | 3 casas; 39,5 blocos/min assentando | **50% esperando material**, 17% assentando | sem material |
| Carpinteiro | 144 fabricações, quase todas de 0–1 peça | **72% andando até o baú** | abre e fecha à toa |
| Pedreiro | — | 2 amostras em 130 min | sem tarefa |
| Fazendeiro | 75 colheitas (2,7 cada), 3 semeaduras | rajadas; 23 varreduras sem fim | ok |
| Pastor | **0 tosquia** | nenhuma tarefa de lã | ocioso por desenho |
| Fundidor | **0 fundição** | 20 paradas sem cru, 126 varreduras de areia vazias | sem cadeia |
| Todos | 34 encalhes em 18 trabalhadores | 1,0 min preso em média (máx 10,9) | resolvido pela fuga |

---

## 2. Profissão por profissão

### 2.1 Lenhador

**O que faz:**
- procura árvore num raio que cresce com as camas;
- anda até ela e corta o tronco e as folhas no tempo do machado (Regra 2);
- replanta a muda (Regra 7);
- cuida do viveiro de terra enraizada com rebento (Regra 36) e do bosque
  fundacional;
- guarda tudo no baú.

**Tempos medidos:**

| Atividade | Tempo |
|---|---|
| Ciclo por árvore (por lenhador) | média **2,3 min**, mediana 0,9 |
| Busca até sair andando | média **2,4 min**, mediana 2,0, máx 8,1 |
| Caminho até a árvore | **32 blocos** em média |
| Produção | 6,4 toras por árvore |

**Por que demora:**
- A busca roda com orçamento por tique e reexamina tora de casa da vila.
  Foram 124 recusas por sessão; o F-1 de 02-10 pôs essas toras no castigo
  mais longo.
- As árvores boas ficam cada vez mais longe.

### 2.2 Mineiro

**O que faz:**
- abre a mina em escada, com duas salas e galeria sem fim (Regra 29);
- escolhe a pedra e o minério por valor;
- ilumina a mina;
- recolhe tudo e guarda (Regra 30);
- quando preciso, cava ou põe bloco para desviar (ADR-025), sai do buraco
  (`ClimbOut`) e volta pela mina (`MineReturn`, 02-10).

**Tempos medidos:**

| Atividade | Tempo |
|---|---|
| Distância até a pedra pedida | média **90 blocos** |
| Ciclo entre desistências | média **4,7 min** |
| Proporção cavando | **3%** das amostras |
| Pedras cavadas e registradas | **nenhuma** nas quatro sessões |

**Por que para:**
- A descida da mina trava numa caverna com lava (7 encalhes em
  `-241, 10, 379`). O F-1 (`MineDescent`) agora pula as pedras abaixo desse
  ponto.
- Mas não abre outro caminho: sem pedra alcançável, o mineiro fica sem
  trabalho.

### 2.3 Construtor

**O que faz:**
- abre a obra e prepara o lote (base na rua, base da planta, degrau ou escada
  de madeira);
- assenta bloco a bloco;
- mobilia;
- repara.

**Tempos medidos:**

| Atividade | Tempo |
|---|---|
| Ritmo enquanto assenta | **39,5 blocos/min** (≈ uma casa pequena em 2–5 min de trabalho) |
| Tempo da obra | **50% esperando material**, 16% andando sem chegar ao ponto |
| Paradas sem material | 23, a cada 7,2 min |

**Material mais esperado:** `glass_pane` (16), `oak_log` (11), `wall_torch`
(10), `oak_planks` (5).

**Por que para:**
- A cadeia do vidro não tem começo: não há areia no raio (ver fundidor).
- A tora está reservada pela regra da metade (ver carpinteiro).
- A tocha depende de carvão, e o carvão depende do mineiro.

### 2.4 Carpinteiro e pedreiro

**O que fazem:** fabricam, na bancada ou no cortador, as peças de madeira
(carpinteiro) e de pedra (pedreiro) que a obra e a meta pedem.

**Tempos medidos:**
- 144 fabricações, quase todas de 0 ou 1 peça.
- Na última sessão, **14 de 22 tarefas fecharam com 0 peças**, com o motivo
  *"half of the colony's wood stays in logs"*.
- 72% das amostras andando até o baú.

**Por que para:**
- A meta da colônia (Regra 5) pede tábua.
- O executor recusa porque metade da madeira tem de ficar em tora (a reserva
  de 28-09).
- A tarefa abre, ele anda até o baú, a tarefa fecha, e no ciclo seguinte tudo
  se repete. É o mesmo defeito das emendas da Regra 27: *"a conta e o
  consumidor precisam concordar"*.
- O pedreiro quase não recebe tarefa.

### 2.5 Fazendeiro

**O que faz:**
- colhe e replanta;
- semeia;
- assa pão (Regra 39);
- tira terra (`COLLECT_SOIL`);
- dá a refeição do fim do dia (procriação).

**Tempos medidos:**
- 75 colheitas, 2,7 itens cada.
- Ciclo médio de 1,4 min, mas em **rajadas**: mediana de 6 s entre uma
  colheita e outra.
- 23 varreduras do campo não terminaram no prazo do tique. A colheita
  acontece mesmo assim.

**Veredito:** é o que mais trabalha. A espera dele é a lavoura crescer.

### 2.6 Pastor

**O que faz:** tosquia, faz o rebanho procriar (Regra 43) e alimenta.

**Medido:** **nenhuma tosquia em 130 min.**

**Por que:** a lã só é pedida quando falta. A linha e os drops aparecem
sozinhos (Regra 41), e as camas não estão faltando. O pastor fica **ocioso
por desenho**.

### 2.7 Fundidor

**O que faz:** funde minério e vidro, e coleta areia e terra de superfície.

**Medido:**
- **nenhuma fundição**;
- 20 paradas por falta de cru;
- 126 varreduras de areia vazias.

O F-1 de 02-10 pôs a areia em espera crescente.

**Por que:** a cadeia dele depende do mineiro (que não cava) e da areia (que
não existe no raio).

### 2.8 Encalhe (todas)

34 encalhes, em 18 trabalhadores. Ficaram presos **1,0 min** em média, com
máximo de 10,9. A fuga funciona. O caso dos mineiros repetindo o encalhe foi
coberto pelo `MineDescent` e pelo `MineReturn`.

---

## 3. Mais ágil em velocidade normal

Sem mexer na Regra 2 (ferramenta e tempo de quebra), só no fluxo.

| # | Melhoria | Profissão | Ganho esperado | Esforço | Regras |
|---|---|---|---|---|---|
| **A-1** | **Meta e executor concordarem na tábua.** A meta não pede tábua quando a reserva da metade não deixa converter, ou a reserva entra na conta da meta | carpinteiro | acaba com as 14 de 22 tarefas de 0 peça e com os 72% andando ao baú | **P** | ✅ é a lição das emendas da 27 |
| **A-2** | **Índice de árvores.** A varredura guarda todas as árvores boas que viu; a próxima escolha sai da lista, e a busca só roda com a lista vazia | lenhador | busca de **2,4 min → segundos** | M | ✅ Regra 23 (a lista vence com prazo) |
| **A-3** | **Lista de material ao abrir a obra.** Pedir a planta inteira de uma vez (já existe "a próxima peça", 28-09), para a cadeia começar antes | construtor e cadeias | reduz os 50% esperando | M | ✅ |
| **A-4** | **Nova boca de mina quando a descida trava**, perto do baú e longe da caverna, em vez de só pular as pedras de baixo | mineiro | tira o mineiro dos 79% andando e das 0 pedras | M–G | ✅ Regra 29 (mina em escada) |
| **A-5** | **Mochila do mineiro:** encher o inventário antes de subir | mineiro | menos subidas de 80 níveis | M | ✅ conferir a Regra 30 |
| **A-6** | **Ponto de apoio mais perto da peça** (o construtor fica 16% "andando sem chegar") | construtor | +10–15% de tempo assentando | P–M | ✅ |
| **A-7** | **Viveiro ao lado do baú do lenhador** | lenhador | caminho de 32 → ~10 blocos | P | ✅ Regra 36 |
| **A-8** | **Varredura do campo pela roça conhecida**, sem varrer o raio | fazendeiro | acaba com as 23 varreduras sem fim | P–M | ✅ |

---

## 4. Ninguém parado sem trabalhar

| # | Alternativa | Para quem | O que resolve | Esforço | Regras |
|---|---|---|---|---|---|
| **B-1** | **Ajudante no tempo ocioso.** Sem tarefa, o aldeão recolhe o item caído que falta à obra (Regra 48) e leva material do baú para perto do construtor | pastor, pedreiro, fundidor, mineiro sem pedra | ocioso vira apoio da obra | M | ✅ é ação, não profissão (Regra 11 intacta) |
| **B-2** | **Cadeia sem começo vira aviso, e outro caminho.** Sem areia no raio: o vidro espera e o `/vc log` diz *"sem areia: a obra espera vidro"* (F-4); a areia é buscada fora do raio (mais longe) | fundidor, construtor | o vidro deixa de ser espera muda | M | ✅; trocar o vidro por outro bloco ⛔ Regra 27 |
| **B-3** | **Pastor recolhe o rebanho no curral** ao entardecer e procria quando a lã baixar (R-2) | pastor | ofício com rotina, não ocioso | M | ✅ |
| **B-4** | **Fundidor sem cru funde outra coisa:** pedregulho em pedra lisa para a obra, e tora sobrando em carvão vegetal (E4) | fundidor | o carvão sai da madeira, e a tocha deixa de depender só do mineiro | P–M | ✅ ADR-028 já aceita produzir o que falta |
| **B-5** | **Toda espera longa vira motivo no `/vc log`**, com o tempo (F-4) | todos | o autor vê quem espera o quê | P | ✅ |
| **B-6** | **O time_ledger como portão:** profissão acima de 40% sem trabalhar abre item de correção (Regra 50) | todos | parada não passa despercebida | — | ✅ já em vigor |
| **B-7** | **Destravas automáticas que já existem:** inalcançável (F-1), descida (`MineDescent`), curral (`PenEscape`), buraco (`ClimbOut`, `MineReturn`), tarefa encadeada (F-2) | todos | — | feito | — |

---

## 5. Recomendação

| Ordem | Item | Por quê |
|---|---|---|
| 1 | **A-1** meta e executor da tábua | o maior desperdício medido, e o menor conserto |
| 2 | **B-1** ajudante no tempo ocioso (com a Regra 48) | põe para trabalhar o pastor, o pedreiro e o fundidor parados |
| 3 | **A-2** índice de árvores | dois terços do tempo do lenhador |
| 4 | **B-4** fundidor funde o que há (carvão vegetal, pedra lisa) | tira a tocha da dependência do mineiro |
| 5 | **A-4** nova boca de mina | o mineiro voltar a cavar |
| 6 | **A-3** material da obra pedido inteiro | os 50% de espera do construtor |

**Depois de cada item:** um playtest com `python scripts/time_ledger.py`. A
proporção de "sem trabalhar" da profissão tem de cair (Regra 50).
