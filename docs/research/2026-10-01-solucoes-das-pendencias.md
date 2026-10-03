# Soluções para as pendências de 2026-10-01

> Estudo pedido pelo autor depois da sessão de 01-10 (E52 `PenEscape`, E47
> `ClimbOut`, Regra 45, ADR-003 Emenda 7). Para cada pendência: o que se sabe,
> as opções, o esforço, e o que decide entre elas. **Nada aqui foi
> implementado** — é o cardápio para a próxima decisão.
>
> Esforço: **P** (horas, 1–2 arquivos), **M** (um dia, vários arquivos, testes
> novos), **G** (vários dias, ADR ou emenda).

---

## 0. Duas descobertas que mudam a ordem

1. **O planejador roda uma vez por ciclo de colônia (600 tiques = 30 s)**, e
   cada vez a varredura de lote olha ~1.024 colunas (`BuildSiteScanner.MAX_COLUMNS`,
   `ColonyCycleRunner` → `PlannerTurns`). A lentidão da vila grande na Emenda 7
   não é o custo da varredura: é a **cadência**. Isso abre uma solução barata
   para o item 7.
2. **Das duas decisões abertas do E51, uma já foi tomada** — "execução só na
   vila do jogador" virou a Emenda 6. Sobra só "limpar o save" (item 6).

---

## A. Validação em jogo (🔴)

Os itens 1–5 não têm solução a escolher: têm linha de log a conferir. O que
há para decidir é **como** conferir sem depender de memória.

| # | O quê | Linhas que provam |
|---|---|---|
| 1 | Curral (E52) | `is fenced in at` → `is out of the pen`; `N time this session` |
| 2 | Escalada (E47) | `climbed to … by PILLAR/STAIRS/TUNNEL`, `switches from`, `boxed in` |
| 3 | Baús (Regra 45) | `is outside the village of colony … out of reach` |
| 4 | Crescimento (Emenda 7) | `planned … the radius is N` com N > 64; tempo em `looking for a lot` |
| 5 | Revisão 30-09 | `baked`, `fed two sheep`, `leaves the vanilla trade`, `lava is never placed`, `drop ingredients` |

**Opções**

- **A. Roteiro manual** — o autor joga e o assistente lê o `latest.log`.
  Esforço zero; é o que se fez até aqui. Risco: a sessão é curta (a de 09:16
  durou 7 min) e o que não aconteceu não aparece.
- **B. Script de conferência** (`scripts/playtest_check.py`) — lê o log (e os
  `.log.gz` do dia), conta cada assinatura da tabela, e aponta as que ficaram
  em zero. Esforço **P**. O projeto já tem `scripts/world_survey.py` no mesmo
  espírito, e a memória registra a armadilha da assinatura que "casa no meio"
  (`0 survived` dentro de `30 survived`): o script usa regex ancorada.
- **C. Mundo de teste dedicado** — um save novo com um curral, um poço fundo e
  uma vila medida, para provocar cada caso de propósito. Esforço **M**.
  Prova o que o save do autor talvez nunca exercite (o pilar, o `boxed in`).

**Recomendação:** B agora, C se algum item ficar em zero depois de duas
sessões.

### 6. Limpar o save (E51)

O save tem 7 BigHouseMOD (6 a mais) e os adultos criados com elas, na colônia
`44cd9e5a`. O mod não desfaz isso sozinho.

- **A. Comando `/vc`** que remove do registro as BigHouseMOD excedentes e
  dispensa os adultos criados com elas. Esforço **M**; fica para sempre como
  ferramenta, mas mexe em registro vivo.
- **B. Editar o `.dat` do mod fora do jogo** (nbtlib, o mesmo do
  `world_survey.py`), com backup. Esforço **P**; não toca o código do mod.
  Os blocos das casas ficam no mundo — o jogador os quebra se quiser.
- **C. Mundo novo para os playtests** e o save antigo só como referência.
  Esforço zero; perde o histórico da vila do autor.

**Recomendação:** C para validar (combina com a opção C da seção A), B se o
autor quiser continuar jogando no save antigo.

---

## B. Importantes (🟠)

### 7. Crescimento lento em vila grande (Emenda 7)

Uma volta inteira da varredura: ~8,5 min (raio 64), ~26 min (caixa 144),
~1 h 40 (caixa 300) — a 1 passagem por ciclo de 30 s. A rua só cresce depois
de uma volta sem lote.

- **A. Cadência: passagens mais frequentes, com o mesmo orçamento.** Rodar
  uma passagem por segundo (ou por tique, sob o `SweepDeadline`) só para a
  colônia em foco, em vez de uma por ciclo. 52 passagens viram ~1 min.
  Esforço **P–M**; não muda o que a busca acha, só quando. Risco: o custo por
  segundo sobe — medir no Spark (o F1 já mostrou o planejador a 28 ms).
- **B. Crescer pelas ruas** (o "plano B" já discutido): varredura fixa em 64
  para o miolo; índice de ruas e ponta de rua seguem a caixa. Custo cresce
  com o comprimento da rua. Esforço **M**. Muda o jeito de crescer: casa nova
  só ao longo de rua.
- **C. Faixa da borda**: a varredura olha o miolo de 64 e uma faixa em volta
  da borda da caixa. Custo cresce com o perímetro. Esforço **M–G**.
- **D. Índice incremental de lotes livres**, atualizado por evento de bloco
  (como o `VillageChests` faz com baús). Responde na hora. Esforço **G**;
  é o desenho "certo" de longo prazo e o mais arriscado.

**Recomendação:** A primeiro — preserva a decisão do autor (raio pela caixa)
e ataca a causa medida (cadência). B se o Spark mostrar custo alto.

### 8. Prevenção da entrada no curral

O E52 mostrou aldeões entrando "de cima do feno encostado na cerca". Um mob
só pula para um bloco adjacente mais alto, e o fardo (1 bloco) deixa a cerca
(1,5) a meio bloco — o salto passa. A pesquisa confirma o padrão: aldeões
atravessam cerca por laje, escada ou bloco encostado. Hoje eles **saem** em
segundos (`PenEscape`); a pergunta é se vale impedir a entrada.

**Falta evidência:** não se sabe **para onde** o aldeão ia quando entrou
(alvo de ofício? passeio Vanilla?). Sem isso, qualquer prevenção é chute.

- **A. Instrumentar primeiro** — no primeiro `is fenced in`, registrar o
  destino que ele tinha (`WorkTargets`, `WALK_TARGET`) e de onde veio. Esforço
  **P**. Decide entre as opções abaixo.
- **B. Memória de curral**: as células de um curral já detectado (o `reach`
  do `FencedIn`) viram zona proibida para destino de ofício — alvo dentro de
  curral é pulado. Esforço **P–M**. Resolve se a causa for alvo de ofício.
- **C. Penalidade no pathfinding**: `MobEntity.setPathfindingPenalty` só
  pesa tipos de nó (`FENCE`, `DANGER_*`); o salto do feno para dentro é uma
  queda, não um nó de cerca, e não seria pego. Precisaria de mixin no
  `LandPathNodeMaker` só para aldeões da colônia. Esforço **G**; mixin em
  código Vanilla é o que a ADR-004 mais restringe.
- **D. Aceitar**: com a saída em segundos e a fuga marcando "N time this
  session", o custo do curral virou pequeno. Esforço zero.

**Recomendação:** A + D agora; B se o log mostrar alvo de ofício dentro do
curral; C só se A mostrar passeio Vanilla e o custo for alto.

### 9. Limites da escalada (`ClimbOut`)

**9a. Sem material para o pilar** — poço sem bloco natural e sem pedregulho
no baú. O teste mostrou 9 de 10 níveis.

- **A. A colônia cria o bloco** quando não há outro: a ADR-028 retirou a
  regra "a colônia não cria recurso" em 30-09, então pôr um bloco de terra do
  nada é coerente com a decisão vigente. Esforço **P**.
- **B. Buscar em qualquer baú da colônia** (`ColonyChests.withdraw`), não só
  no dele. Esforço **P** de código, mas é dependência nova de `work` para
  `integration` — cresce o ciclo congelado do ArchUnit (ver a memória
  `climb-out-nunca-desiste`).
- **C. Cavar mais fundo na parede** — atravessar um bloco de parede não
  natural não é permitido; só serve onde há natural a 2 blocos. Esforço **P**,
  cobre pouco.

**Recomendação:** A.

**9b. Fechado por blocos protegidos** (construção da colônia, vila original,
bloco do jogador): ele tenta para sempre e avisa a cada minuto.

- **A. Quebrar só bloco da colônia e deixar o reparo reconstruir.** O
  `BuildingRepairPlanner` já reabre lacuna de construção da colônia. Vila
  original e bloco do jogador continuam intocáveis (Regra 3). Esforço **M**.
- **B. Teletransporte de último recurso** (para a borda da vila, depois de N
  minutos). É o que o MineColonies faz — e o próprio rastreador dele mostra
  construtores presos quando o teletransporte falha. **O autor vetou
  teletransporte no E47**; precisa de decisão explícita. Esforço **P**.
- **C. Avisar o jogador** (mensagem no chat ou na barra de ação com as
  coordenadas). Esforço **P**; não resolve, mas tira o caso do log.

**Recomendação:** A + C; B só com aval do autor.

### 10. Desempenho a medir (Spark)

Itens: a varredura de baús agora cobre a caixa inteira (Regra 45);
`ChestWithdrawer.countIn` por drop do mineiro; F13 (`minersRoom`/`roomOf`
releem baús a cada ciclo); e o lenhador (`TreeScanner.findNearestLog`), que
foi **73% do custo do mod** no Spark de 30-09 21h.

- **Baús da caixa** — **A.** Índice de baús por evento: o `VillageChests` já
  escuta `BLOCK_ENTITY_LOAD/UNLOAD` só para invalidar o cache; o mesmo evento
  pode manter um conjunto de posições, e a varredura de chunks some. Esforço
  **P–M**. **B.** Manter a varredura (só chunks carregados, cache de 1 s) e
  medir antes.
- **F13** — cache do `ChestSurvey` por colônia com validade (o
  `ColonyChestSurvey` já faz levantamento em rodadas de 8). Esforço **P**.
- **Lenhador** — **A.** índice de toras por chunk, atualizado por evento de
  bloco; **B.** orçamento por tique e cache de árvore achada (`TreeMarks`
  existe); **C.** buscar só dentro da área de trabalho (caixa + margem).
  Esforço **M**.

**Recomendação:** medir antes de mexer (regra da casa: "o log decide a
constante"). Se o lenhador continuar no topo, ele vem antes de tudo.

### 11. Decisões do E51

Resolvida pela Emenda 6, exceto "limpar o save" — ver item 6.

### 12. PRs aguardando aval

PR #8 (`claude/sync-local-github-80cc8a` → `codex/bighousemod`) e PR #3
(→ `main`). Esta branch (`claude/analise-atualizacoes-projeto-9b8886`) foi
enviada sem PR.

- **A.** Abrir PR desta branch para a mesma base do #8 e mesclar em cadeia
  (#8 → bighousemod → main).
- **B.** Mesclar tudo direto em `main` com squash por PR.
- **C.** Esperar o playtest e só então abrir PR, com o resultado no texto.

**Recomendação:** C — os quatro commits de 01-10 não foram vistos em jogo, e
o PR com a prova do log é o que o autor costuma pedir.

---

## C. Melhorias (🟡)

| # | Pendência | Opções | Recomendação |
|---|---|---|---|
| F7 | Busca do fazendeiro não fecha (26 `SWEEP_INCOMPLETE`) | A. mesma cadência do item 7A; B. cache de canteiros achados; C. raio pela caixa em vez de 32 do centro | A, que sai junto com o 7 |
| F8 | Tingimento tenta as 16 cores de vidraça | A. só as cores que a planta pede; B. cache negativo por cor com validade | A (P) |
| F9 | Carpinteiro para em lotes de 2–9 peças pela reserva de tora | A. reserva proporcional à demanda da obra; B. liberar a reserva quando a obra espera justamente esta peça | B (P–M) |
| F14 | `WORK_STALLED` em pastor, lenhador e mineiro | A. conferir no log se o `ClimbOut`/`PenEscape` já cobrem; B. tratar por ofício o que sobrar | A primeiro |
| — | Vanilla volta a reivindicar a estação a cada ciclo (ADR-029) | A. medir; B. limpar a memória de posto da colônia a cada ciclo; C. tirar as tasks Vanilla de posto do Brain — **a ADR-004 §7 proíbe remover task Vanilla** | A, depois B |
| — | Combustível da fornalha e custo do viveiro fora da economia (ADR-028) | A. combustível do baú da colônia, como o MineColonies (que pede combustível e deixa o jogador escolher qual); B. o fundidor faz carvão vegetal das toras; C. manter de graça, com o motivo escrito | Decisão do autor; A+B é o mais coerente com a cadeia de produção |
| — | Regra 45: baú fora de casa dentro da vila | A. manter (todo baú da caixa); B. voltar a "só dentro de casa" também na caixa; C. fora de casa só perto de construção ou rua da colônia | Decisão do autor; C é o meio-termo |

---

## D. Futuro (🟢)

As opções de evolução da vila (núcleo e área de trabalho, contorno por
células, quadras e zonas, níveis, distritos) estão na conversa de 01-10 e na
ADR-003 §10; o primeiro passo natural é "núcleo e área de trabalho", que dá
nome ao "fora de alcance" de cada ofício (Regra 45 já fez a parte dos baús).

---

## E. Ordem sugerida

| Ordem | Item | Esforço | Por quê primeiro |
|---|---|---|---|
| 1 | B do §A (script de conferência) | P | toda sessão de jogo passa a render evidência |
| 2 | 8A (instrumentar entrada no curral) | P | decide a prevenção sem chute |
| 3 | 7A (cadência da varredura) + F7 | P–M | ataca a causa medida da lentidão |
| 4 | 9a-A, 9b-A + 9b-C (escalada) | P–M | fecha os dois casos em que ele ainda fica |
| 5 | 10 (Spark) | — | decide se o lenhador e os baús precisam de índice |
| 6 | F8, F9 | P | ganhos locais, baixo risco |
| 7 | Decisões do autor: combustível/viveiro, Regra 45 fora de casa, teletransporte | — | não têm resposta técnica |

---

## Fontes

- MineColonies: [construtor preso sem teletransporte (#10003)](https://github.com/ldtteam/minecolonies/issues/10003), [aldeão preso no andaime (#5225)](https://github.com/ldtteam/minecolonies/issues/5225), [combustível do cozinheiro (#3295)](https://github.com/ldtteam/minecolonies/issues/3295), [fundição pedindo combustível (#5837)](https://github.com/ldtteam/minecolonies/issues/5837), [O cozinheiro e o sistema de pedidos](https://steemit.com/utopian-io/@raycoms/minecolonies-and-the-cook)
- Aldeões e cercas: [Villagers, Sheep and Fences](https://www.minecraftforum.net/forums/minecraft-java-edition/survival-mode/280278-villagers-sheep-and-fences-a-menace), [Can villagers jump?](https://www.minecraftforum.net/forums/minecraft-java-edition/survival-mode/2786935-can-villagers-jump), [Can villagers open gates](https://sportskeeda.com/minecraft/can-villagers-open-gates-minecraft)
- Pathfinding: [`MobEntity.setPathfindingPenalty` (yarn)](https://maven.fabricmc.net/docs/yarn-1.16.4+build.1/net/minecraft/entity/mob/MobEntity.html), [usos de `PathNodeType`](https://maven.fabricmc.net/docs/yarn-21w15a+build.2/net/minecraft/entity/ai/pathing/class-use/PathNodeType.html)
- Posto de trabalho Vanilla: [Villager professions (Minecraft Wiki)](https://minecraft.wiki/w/Professions)
