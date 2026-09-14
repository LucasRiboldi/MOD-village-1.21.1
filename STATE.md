# STATE — 2026-09-14

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

## Sessão de 2026-09-14

**Distribuição após a emenda ADR-012:** o JAR 0.3.0 foi reconstruído e
copiado para `downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256 nas três
cópias (`build/libs/`, distribuição e launcher):
`EF0138BE7180FC47FB905C42EF7F64A8A31FE68CFCFCBCE4C07CAF72DB467231`.
`build` e 324/324 GameTests passaram; falta a validação visual das obras e da
mineração no mundo do jogador.

## Sessão de 2026-09-13

**Distribuição desta sessão:** JAR 0.3.0 copiado de `build/libs/` para
`downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256:
`E41063395BFC20A8D5D3182A9732B6FAF1E7265E96446E8909F32A623B1BE1D9`.
`build` e 316/316 GameTests passaram; validação visual continua pendente.

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3` — em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

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

## Sessão de 2026-09-14 — obra esperando terra

O log mais recente mostra que o planejador **abriu** `plains_butcher_shop_2`
com 382 blocos às 00:50:15. A obra assentou dois blocos e então ficou em
`WAITING_RESOURCES`, com 380 restantes, aguardando `minecraft:dirt` até
01:01:15. Os 18 baús lidos tinham `GRASS_BLOCK=33`, mas nenhum `DIRT`; o
fundidor não tinha rota para coletar terra. Portanto, a ausência visual de
construções neste teste decorre de uma obra ativa bloqueada por insumo, não
de falha de seleção do planejador. Um aviso posterior de alcance do construtor
é secundário e precisa de nova medição depois que a terra chegar.

`DIRT` agora é recurso `SURFACE_GATHERED`, tem conversão do item Vanilla e
entra na mesma coleta externa e protegida por setor usada para `grass_block`.
GameTests cobrem catálogo, atribuição ao coletor, coleta real de terra fora do
raio protegido e depósito no baú pessoal. `build` e 320/320 GameTests passaram.
**Pendente:** instalar o JAR atualizado e validar em jogo se o fundidor coleta
terra, se a obra retoma além dos 380 blocos e se o aviso de alcance reaparece.

**Reanálise do playtest de 09-14:** o JAR do launcher tinha SHA diferente do
`build/libs/`; o log veio do artefato anterior à correção de terra. Nele,
construções abriram mas pararam esperando `dirt`; três mineiros estavam aptos,
mas sem tarefa, porque havia 3 carvões e não existiam metas de carvão/ferro
sem obra. Lenhadores também ignoravam `BlockProtection` durante a derrubada.
Correções atuais: piso de 64 carvão + 64 minério bruto (obra soma ao piso) e
proteção de árvores no plano e em cada quebra. Construções Vanilla e da colônia
são cobertas; troncos manuais sem marca não têm autoria recuperável pelo jogo.
`build` passou, 322/322 GameTests passaram, e o JAR foi copiado para
`downloads/` e o launcher. SHA-256 nas três cópias:
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.
**Pendente apenas validação em jogo** da construção, mineração e preservação
estrutural; troncos manuais sem marca seguem como limite conhecido.

**ADR-016 / Lote 1 em andamento (09-14):** `ResourceTally`,
`ColonyResources` e a leitura de baús agora preservam contagens por `ResourceId`
para itens fora de `ResourceType`, mantendo a visão tipada existente. `build`
verde e 317/317 GameTests; ainda não há metas/tarefas nem executor genérico para
esses IDs, portanto a cobertura de materiais continua pendente. Validação em
mundo de desenvolvimento ainda não realizada. JAR 0.3.0 distribuído em
`downloads/` e `%APPDATA%/.minecraft/mods/`, com cliente fechado; SHA-256 nas
três cópias: `F41920D7DA1FBEADA94D4F886F5047F7A3242F850011DE7DFA2F4A9CAE28DB77`.

O mineiro fechava a frente sem espaço para ficar e reabria a mesma hélice no
fundo. E45 foi resolvido em 2026-09-13 pela ADR-013: no limite, a rota gira
sem mover a boca; saves v4 reiniciam os cursores e mantêm o arco. `build`
passou com 808 testes unitários e `runGametest` com 314/314; confirmação
visual ainda pendente.

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

Última medição: **810 unitários**, **314 GameTests** e **74 testes Python**;
zero falhas nos dois primeiros nesta sessão. Python não foi reexecutado.

**Diagnóstico do playtest/log de 09-13:** uma tarefa de areia foi criada para
vidro, mas `MinerWork` gravava no Job a pedra da paleta da vila. O log mostra
`0 of 3` apesar de areia transportada; corrigido para o Job acompanhar o
recurso da tarefa. Antes disso, ele completou 64/64 pedregulhos; depois a
busca de areia não encontrou bloco num raio de 48. `MinerWork.tick` remove
Jobs concluídos sem uma etapa explícita de volta à boca da mina; retorno
segue aberto. A obra esperou `minecraft:grass_block` com 382 blocos restantes
e desistiu mantendo o lote ocupado; nenhuma profissão fornece esse recurso
no catálogo atual. Agricultores não acharam plantio maduro/lote vazio em 32
blocos. Carpinteiro, fundidor, pedreiro e pastor reportaram sem tarefa aberta;
lenhadores foram vistos cortando e aumentando a produção. `build`: 810
unitários; GameTests: 314/314.

---

## Decisões que esperam o autor

1. **P0.7 — aceitar pedra como solo de lote.** Toca a Regra 3 e a Regra 19. Três caminhos: aceitar pedra (zero custo, casa pode ficar esquisita em afloramento), terraplanar o lote (mexe no mundo, mais código), ampliar o raio de busca (casa nasce longe). **Recomendação registrada: aceitar pedra.**

2. **E43 — o descanso de 4 ciclos é anulado no ciclo seguinte.** A 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador quando a colônia não tem outro trabalho da profissão dele.

---

## Onde o projeto está

MVP previamente verificado em jogo; o playtest de 09-13 contradiz o estado da mina: o minerador repete uma frente bloqueada no limite. O falso material `structure_void` da casa média foi corrigido e ainda precisa de verificação visual. O log mostra uma obra antiga retomada, mas não prova que era a casa do relato. Lenhadores e fazendeiros foram vistos funcionando pelo autor.

**O gargalo recorrente é verificação em jogo e decisões do autor**, embora
o playtest ainda revele defeitos de código, como a fome de buscas da mina
corrigida nesta sessão. Cada item entregue acumula dívida de "não visto em
jogo", e a fila cresce mais rápido do que drena.

**Próximo passo natural:** sessão de jogo para validar o portal da mina em
vila nova, a rota E45, a casa e as correções pendentes de 09-13. P0.7 e E43
seguem como decisões do autor; E44 aguarda validação em jogo.

**Nova vila sem portal da mina (09-13):** o log mostra mineiros das vilas
novas repetindo `looking for stone, 0 of 64`, sem linha de abertura. A causa
no código era a cota global de uma busca por tique presa ao primeiro
trabalho sem alvo. O rodízio foi corrigido e passou em `build` (809
unitários) e 314 GameTests; portal visível e início da escavação **aguardam
validação em jogo**.

**Coleta superficial do fundidor (09-13):** pá de ferro com Toque Suave I;
areia para a cadeia do vidro e `grass_block` apenas quando uma obra aberta
precisa dele. A grama é buscada estritamente além de 64 blocos, no setor
cardinal mais distante das peças de estruturas de vila conhecidas em chunks
carregados; nenhum chunk é forçado e o raio de busca de trabalho continua 48.
`build` e 315/315 GameTests passaram. **Aguardam validação em jogo** a coleta,
o baú pessoal do fundidor e a preservação visual das estruturas. JAR de
`downloads/` e launcher não atualizado nesta tarefa.

**Distribuição híbrida e pedra lisa (09-13):** faltas da obra aberta recebem
`CONSTRUCTION_MATERIAL` antes das tarefas de estoque; trabalhadores restantes
mantêm `PRODUCTION`. O pedreiro reconhece `smooth_stone_slab`, e a demanda
deriva a quantidade de `smooth_stone` da receita Vanilla. `build` e 316/316
GameTests passaram; aguarda validação visual. ADR-015 registra a escolha C,
sem teto numérico arbitrário de reserva. **Ainda não implementado:** tarefas,
contagem e estoque de blocos arbitrários, nem o fallback genérico para
fundidor/criador; o catálogo de recursos atual é enum fechado. Ver TODO.

**Plano de continuidade e construção (09-14):** plano por lotes em
`docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md`.
Lote 1 aplicado: ADR-012 reconcilia a coluna editada pelo jogador, preserva
a varredura parcial e reinicia apenas o cursor de consulta de ruas. `build` e
324/324 GameTests passaram; **sem confirmação em jogo**. Próximo lote para
revisão: continuidade do mineiro; depois diversidade de estruturas entre
construtores, estratégias distintas para avaliar lotes e revalidação após
falhas repetidas.
