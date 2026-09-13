# STATE — 2026-09-13

> Arquivo de estado vivo. Sobrescreve, não acumula.
> Se passar de 150 linhas, algo está errado — P0 não está fechando.

---

## P0 — bloqueadores

Um por vez, teste antes de seguir. Nada mais entra antes de fechar.

| Item | Descrição | Estado |
|---|---|---|
| P0.1 | O planejador não acha lote | ✅ entregue 09-11, **visto em jogo** |
| P0.1-b | O caminho de terra não sai de baú | ✅ entregue 09-11, **espera sessão** |
| P0.1-c | A recusa de lote diz por quê | ✅ entregue 09-11, **espera sessão** |
| P0.3 | Mineiro → armazenamento → fundidor | ✅ conserto entregue 09-11, **espera sessão** |
| P0.5 | Perda de item por inventário cheio (E3) | ✅ entregue 09-11, **espera sessão** |
| P0.6 | A enxurrada da areia calou | ✅ entregue 09-11, **espera sessão** |
| P0.7 | Pedra como solo de lote | ⬜ **decisão do autor pendente** |

**P0.7 — o número chegou e não era o esperado.** Das 6.583 recusas de lote, **4.578 (70%)** são `NOT_NATURAL_GROUND` — pedra não entra como solo natural, por decisão registrada (*"pedra à mostra é montanha"*). A vila do autor é rochosa. Mexer nisso toca a Regra 3 e a Regra 19 — é decisão do autor, não correção automática.

---

## Sessão de 2026-09-13

**Jar atualizado em 2026-09-13:** `CD29B5F8BC61871BCE74A910C57DB28E` — em `downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu no jogo:**

- Casa parada no meio.
- Mineiro invisível/parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a `STABLE` — reduz o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A carpintaria fabricava composteiras, mas usava a porta de consumo (`ColonySupply.take`) e retirava do baú a peça que acabou de produzir. Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`.

**Auditoria técnica de 09-13:** a casa média do log parou por uma exigência falsa de `minecraft:structure_void`; o leitor de blueprint agora ignora esse marcador. O teste novo falhou antes da correção e passou depois. Foram aprovados 798 unitários, 312 GameTests e 74 testes Python. A pequena fazenda antiga do log retomou com 45 blocos, mas não há identidade suficiente para afirmar que era a casa do relato. A casa média corrigida ainda aguarda validação em jogo.

**Reinvestigação após o playtest:** um GameTest reproduziu a colocação de
`minecraft:dirt` sem estoque: `BuilderWork` tratava toda a tag `DIRT` como
material moldado no local. A exceção foi limitada a farmland, água,
`dirt_path` e cultivos. Os 313 GameTests e `build` passaram; a nova regra
aguarda validação em jogo. O log anterior carregou o JAR antigo e não
registra bloco/posição da terra observada, então a causa está confirmada
no código, mas não pode ser atribuída com certeza àquela posição específica.

O mineiro fecha a frente sem espaço para ficar e reabre a mesma hélice no
fundo. E45 continua sem mudança de código até revisão/ADR sobre rota e
migração do save (`SHAPE_VERSION` 4).

**Playtest de 2026-09-13, após o JAR anterior:** nenhuma construção visível;
mineiros sem atividade percebida; baús de profissões misturando produção.
No log, a obra assentou só três peças e parou por falta de grama/pedregulho;
mineiros ficaram sem espaço para ficar em pé e produziram zero; lenhadores
e agricultores registraram colheitas, e fundidores pararam repetidamente
por falta de areia. Revisão dos depósitos confirmou saídas em baús de
colegas: agora a produção de mineiro, lenhador, fundidor, carpinteiro e
pedreiro vai ao baú pessoal; insumos continuam compartilhados. Se não há
espaço, transformações devolvem o insumo e drops de mineração/derrubada
permanecem no mundo. `build` e 313 GameTests passaram. **Aguardar validação
em jogo.** Próximo lote: causa da mina sem espaço para ficar em pé e
construção bloqueada por estoque/retomada.

**Releitura após alterações do jogador (lote aplicado em 2026-09-13):**
interações que realmente mudam blocos e quebras invalidam os índices/cursor
de construção das colônias próximas; a varredura limitada relê estradas e
terreno no mundo. Abrir espaço sobre trecho já percorrido da mina reabre o
braço desde o primeiro ponto afetado e limpa seu bloqueio transitório.
`build` e 314 GameTests passaram, inclusive a reindexação após estrada nova.
Pendente confirmar em jogo que casa e mina retomam no mundo do autor. Isto
não cria novas galerias nem corrige geometria E45. Baús registrados e o da
boca da mina já são consultados ao vivo; baú arbitrário continua dependendo
de vínculo de armazenamento.

**ADR-011 — profissões e crescimento:** implementadas as sete funções
produtoras (Mineiro, Lenhador, Pedreiro, Fundidor, Carpinteiro, Agricultor,
Criador) e as vagas por população adulta: 1 de cada aos 15, 2 de cada aos
30, 3º Mineiro aos 31 e 3º Lenhador aos 32. Nitwits contam para a população,
mas não são contratados; bebês só contam quando adultos. `BUILDER` e
`SHEPHERD` permanecem compatíveis com saves, e Pastor legado conta na cota
de Criador. Qualquer produtor pode construir temporariamente sem trocar de
profissão. Build, testes unitários e 313 GameTests passaram; **aguarda
verificação em jogo**.

---

## P1 — em fila (depende de P0 verde)

| Item | Descrição | Decisão |
|---|---|---|
| E45 | No limite de profundidade, girar a galeria reabre a hélice/descida bloqueada | **revisão de geometria e ADR antes do código** |
| E44 | Recusa de alvos inalcançáveis | ✅ escada em `MineMarks`; **aguarda validação em jogo** |
| E43 | O descanso de 4 ciclos é anulado no ciclo seguinte | **autor** |
| E41 | Nada mede degradação ao longo de muitos ciclos | — |
| KF-001 | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard` | ✅ causa no teste corrigida; quota global é risco separado de vazão |

---

## Última verificação em jogo

| Data | O que foi visto |
|---|---|
| **2026-09-13** | casa parada, mineiro parado, lenhadores e fazendeiros trabalhando |
| **2026-09-12** | casa morreu esperando `smooth_stone_slab`; mineiro não desceu |
| **2026-09-11** | casa aberta (P0.1 visto); obra travou em `dirt_path` |

**Pendente de ver em jogo:**

- P0.1-b, P0.1-c, P0.3, P0.5, P0.6 — entregues em 09-11, **nunca vistos**.
- As quatro correções de 09-13 — **nunca vistas**.
- Arco da mina não volta depois de quebrado (`Mine.archRaised`).
- Lenhador corta todo o tronco sem deixar sobra (E39, fechado 09-12).

---

## Bateria

Última medição: **798 unitários**, **312 gametests** e **74 testes Python**, zero falhas (09-13).

---

## Decisões que esperam o autor

1. **P0.7 — aceitar pedra como solo de lote.** Toca a Regra 3 e a Regra 19. Três caminhos: aceitar pedra (zero custo, casa pode ficar esquisita em afloramento), terraplanar o lote (mexe no mundo, mais código), ampliar o raio de busca (casa nasce longe). **Recomendação registrada: aceitar pedra.**

2. **E43 — o descanso de 4 ciclos é anulado no ciclo seguinte.** A 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador quando a colônia não tem outro trabalho da profissão dele.

---

## Onde o projeto está

MVP previamente verificado em jogo; o playtest de 09-13 contradiz o estado da mina: o minerador repete uma frente bloqueada no limite. O falso material `structure_void` da casa média foi corrigido e ainda precisa de verificação visual. O log mostra uma obra antiga retomada, mas não prova que era a casa do relato. Lenhadores e fazendeiros foram vistos funcionando pelo autor.

**O gargalo não é código.** É verificação em jogo e decisões do autor. Cada item entregue acumula dívida de "não visto em jogo", e a fila cresce mais rápido do que drena.

**Próximo passo natural:** revisão do diagnóstico E45 e da decisão sobre geometria/save; depois, sessão de jogo para validar a casa, a mineração e as correções pendentes de 09-13. P0.7 e E43 seguem como decisões do autor; E44 aguarda validação em jogo.
