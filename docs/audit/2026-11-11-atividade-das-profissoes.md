# Atividade das profissões — jogos de 11-08, 11-09 e 11-10 (0.4.47)

Pedido do autor, 2026-11-11: listar o que cada profissão fez nos jogos anteriores e oferecer melhorias para deixá-las mais
ativas. Só diagnóstico; nada foi alterado. As opções esperam a escolha do autor (`CLAUDE.md` §0.2.2).

## Fontes

| Jogo | Arquivo | Versão | Duração |
|---|---|---|---|
| 11-08 | `villagecolony-actions-20261108-200548.jsonl` | 0.4.4x | 226 min de jogo |
| 11-09 | `2026-11-09-1.log.gz` | 0.4.45/46 | 8 janelas de 5 min |
| 11-10 | `2026-11-10-2.log.gz` + `villagecolony-actions-20261110-231030.jsonl` | 0.4.47 | 4 janelas de 5 min, 19,6 min de diário |

Nenhuma nota `VC_BIOME_NOTE` nos três. Vila principal da 0.4.47: colônia `9105dea9`, deserto, 36 aldeões, 32 construções.

Retrato do último dia (`VC_DAY`): mineiro 7, pedreiro 6, pastor 6, fundidor 5, carpinteiro 3, lenhador 3, fazendeiro 2,
construtor 2, sem ofício 2. Estoque: madeira 784, pedra 634, lã 127, carvão 66, areia 23, **comida 10**. Animais: 2 ovelhas,
1 vaca, 2 porcos, 3 galinhas. **Uma obra aberta** (`desert_small_house_1`, faltam 114 blocos). Mina com 1 nível.

## Tempo no expediente (`time_ledger.py`)

| Profissão | 11-09 trabalho / ocioso+bloqueado | 0.4.47 trabalho / espera / bloqueado / ocioso |
|---|---|---|
| Construtor | 12% / 46% | 4% / 4% / 0% / 46% |
| Carpinteiro | 7% / 56% | 6% / 6% / 0% / 68% |
| Fazendeiro | 1% / 10% | 0% / **47%** / 0% / 8% |
| Lenhador | 1% / **77%** | **13%** / 0% / 1% / **4%** — ok |
| Pedreiro | 13% / 40% | 5% / 4% / 1% / 62% |
| Mineiro | 1% / 27% | **0%** / 38% / 1% / 23% |
| Pastor | 0% / 82% | 0% / 0% / 0% / 69% |
| Fundidor | 1% / 88% | 2% / 23% / **59%** / 4% |

A Regra 232 funcionou para o lenhador: ocioso de 75% para 4%. Todas as outras passam do teto da Regra 51.

## Achados por profissão

### Mineiro — 0% trabalhando
- Log `miners:`: de 7, até 5 em "waiting for a branch — the shaft is one branch until the gallery starts". Só um cava o poço.
- Os que cavam vão atrás de **arenito na mina** ("digging Arenito ... out of reach, walking to nowhere"), num deserto com
  arenito sob a areia da superfície.
- 14× `NO_PATH`, 3× "Mine access failed — no dig-only way". No jogo de 11-08: 1 de 75 tarefas concluída.
- Contratação: a cota de 36 adultos dá 3 mineiros; há 7. Nada tira o excedente de um ofício.

### Fundidor — 59% bloqueado
- 5× "no raw for the furnace in any chest — the mason makes it" (161× em 11-08). Pede `smooth_sandstone`, que precisa de
  arenito; o pedreiro responde "nothing above what the open build needs is left to make sandstone" (areia 23).
- O fundidor solta a tarefa e a pega de novo: o mesmo aldeão soltou `smooth_sandstone` 4 vezes.
- `VC_SUPPLY WAITED_TOO_LONG`: `sandstone` ×16 e `smooth_sandstone` ×8 surgiram pela Regra 128. A cadeia do deserto não fecha.

### Fazendeiro — 47% esperando
- "nothing ripe and no empty plot within N blocks of the village" e "the budget ran out before an answer" (6× cada).
- A roça está toda plantada e crescendo, e nenhuma roça nova abre. Comida no estoque: 10. Isso trava também o pastor (procriar
  pede trigo) e os nascimentos.

### Pastor — 69% ocioso
- 6 pastores para 2 ovelhas, 1 vaca, 2 porcos e 3 galinhas. "no shepherd work: no task open", "no sheep with wool within the
  radius" (22× em 11-08). Lã 127 guardada.
- "dropped the herd in phase LEAD" 5× e "in phase FETCH" 3×: trazer bicho para o curral falha no caminho.

### Carpinteiro e pedreiro — 68% e 62% ociosos
- Terminam rápido (mediana de 11 s) e ficam sem pedido: "nothing this workshop can make now" 13×.
- A colônia só tem **uma obra aberta por vez** (`ConstructionService.openOf`), e 32 construções. Com madeira 784 e pedra 634,
  o que falta é pedido, não material. O pedreiro aplainou bastante (`LEVELLED` 12,4/min).

### Construtor — 46% ocioso
- 2 construtores numa obra de 114 blocos que espera arenito e arenito liso (os `WAITED_TOO_LONG` acima).

## Opções

Cada uma é pequena e independente, salvo onde diz. Recomendação marcada.

**Transversal**
- **T1 (recomendada): o excedente troca de ofício.** Uma vez por dia, quem está num ofício acima da cota (`HiringQuota.targetCount`)
  e passou do teto da Regra 51 no dia vai para o ofício abaixo da cota que mais falta, comida primeiro. Hoje: 4 mineiros, 3
  pastores e 2 fundidores sobram; fazendeiro e construtor faltam. Risco: troca em vaivém; mitigar com 1 troca por dia por colônia.
- **T2: duas obras abertas a partir de 30 adultos.** Dá pedido a carpinteiro, pedreiro e construtor. Muda a regra de uma obra
  por vez; exige ADR e mexe em reserva de material e lote. Maior.

**Mineiro**
- **M1 (recomendada): no deserto, arenito e areia são de superfície.** O mineiro tira arenito da borda da vila, como o lenhador
  tira árvore, em vez de descer atrás dele. Destrava também o fundidor e a obra.
- **M2: a galeria abre mais cedo.** No primeiro nível, um segundo ramal abre logo, para dois mineiros cavarem juntos.

**Fundidor**
- **S1 (recomendada): espera sem pegar de novo.** Com o pedido do pedreiro aberto, o fundidor não pega a mesma fundição; no
  lugar, faz carvão vegetal com a madeira de sobra (784), que é o próprio ofício e alimenta a fornalha.
- **S2: ele mesmo busca a areia.** Com o pedreiro sem sobra para fazer arenito, o fundidor coleta areia na superfície.

**Fazendeiro**
- **F1 (recomendada): roça nova quando espera.** Sem nada maduro e sem canteiro livre, com comida abaixo de 64, abre um
  canteiro novo a até 32 blocos, perto de água (item pendente desde a 0.4.47).
- **F2: composteira.** Enquanto espera, transforma semente sobrando em farinha de osso e acelera a roça.

**Pastor**
- **P1 (recomendada): pastor por rebanho.** Vaga de pastor só com 8 animais por pastor; o resto entra na troca da T1.
- **P2: procriar com a sobra.** Com comida acima de 64, procria até o curral ter 8 de cada espécie.

**Carpinteiro e pedreiro**
- **C1 (recomendada): adiantar a próxima obra.** Sem pedido, fazem as peças da próxima planta da fila e guardam no baú.
- **C2: T2.**

## Registro

- Blocos que surgiram (Regra 128): `sandstone` ×16, `smooth_sandstone` ×8 — buscar fluxo de fabricação (M1/S1/S2).
- Custo (`cost_ledger.py`, 4+ colônias): ciclo médio de 34,9 ms, p95 57,4 ms; `detect` 13,8 ms é a fase maior.
