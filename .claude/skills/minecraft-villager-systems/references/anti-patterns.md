# Anti-padrões de sistemas de aldeão

Cada um: o padrão, **por que atrai**, como falha, e o que fazer.

---

## Villager = Entity + funções

**Atrai:** é como quase toda outra entidade funciona.

**Falha:** o aldeão é Brain + memórias + sensores + activities + POI + profissão +
schedule + vila. Tratá-lo como entidade simples faz você reimplementar peças que
já existem — e brigar com elas.

**No lugar:** o modelo de `references/villager-architecture.md`. Descubra **qual
camada** mexer.

---

## Static Villager State

**Atrai:** `static Map<UUID, BlockPos>` é a coisa mais rápida de escrever.

**Falha:** cinco problemas de uma vez — não persiste, não expira, não é limpo na
morte, vaza entre saves, e briga com o Brain (que não sabe que ele existe).

**No lugar:** memória registrada com codec. Resolve os cinco.

> **É o anti-padrão número um deste domínio.**

---

## Memory Abuse

**Atrai:** memória é o lugar "certo", então tudo vai para lá.

**Falha:** memória é conhecimento **com validade**, não banco de dados. Estado
permanente da colônia numa memória de aldeão morre com ele.

**No lugar:** memória para o que o aldeão sabe agora; `PersistentState` para o
que a colônia sabe sempre.

---

## Sensor Doing Actions

**Atrai:** o sensor já está lá, já achou a coisa — por que não agir?

**Falha:** sensores rodam com frequência própria e não são o lugar de efeito.
Ação no sensor acontece fora do controle da Activity, ignora o gate de memórias e
roda quando não deveria.

**No lugar:** sensor escreve memória; task age. A separação é rígida.

---

## Behavior Doing Perception

**Atrai:** a task precisa saber onde está o alvo, então ela procura.

**Falha:** varredura dentro da task roda toda vez que a task roda, sem a
frequência controlada do sensor. Multiplicado pela vila, é lag.

**No lugar:** sensor com frequência própria, ou índice por evento.

---

## Profession Doing Everything

**Atrai:** "é a profissão dele, então a lógica é da profissão".

**Falha:** `[FATO]` o record `VillagerProfession` não contém tasks, trades nem
schedule. Empurrar comportamento para lá significa criar uma classe paralela que
faz tudo — a God Class do domínio.

**No lugar:** profissão = identidade + POI. Comportamento = task. Comércio =
trades.

---

## Job Site Without POI

**Atrai:** criar o bloco e esperar que o aldeão o reconheça.

**Falha:** sem `PointOfInterestType` registrado o aldeão ignora o bloco. E
registrando **só um block state**, ele perde o local quando o bloco muda de
estado.

**No lugar:** `references/poi-and-job-sites.md`. Todos os block states.

---

## Profissão quando bastava comportamento

**Atrai:** "é um tipo novo de aldeão".

**Falha:** profissão traz POI, reivindicação, schedule, trades, níveis, XP, som e
resources. Um aldeão que foge de um inimigo específico não precisa de nada disso
— e você descobre isso **depois** de escrever tudo.

**No lugar:** pergunte antes: *isto é identidade nova, ou capacidade nova?* Ver
`examples/guard-villager-decision.md`.

---

## Activity nova sem Schedule

**Atrai:** parece o lugar limpo para o modo novo.

**Falha:** `[FATO]` quem escolhe a Activity é a `Schedule`. Uma Activity que ela
não conhece **nunca é escolhida** — a task nunca roda e o bug parece
inexplicável.

**No lugar:** task em `CORE` com as condições dentro. Ver
`workflows/add-activity.md`.

---

## Navegação direta

**Atrai:** `getNavigation().startMovingTo` é a API óbvia.

**Falha:** o cérebro reescreve o destino no mesmo tick. O aldeão anda dois blocos
e volta.

**No lugar:** memória `WALK_TARGET`, **mantida** enquanto o destino valer.

---

## Horário paralelo

**Atrai:** `if (world.getTimeOfDay() ...)` é direto.

**Falha:** a Schedule Vanilla não sabe da sua opinião. Ela manda dormir, sua task
manda trabalhar, e o aldeão oscila.

**No lugar:** consulte a Activity/Schedule vigente.

---

## Full Vanilla Replacement

**Atrai:** controle total, e mais simples de raciocinar.

**Falha:** o aldeão deixa de dormir, comer, socializar e fugir. Incompatível com
todo mod de aldeão. Congela a versão.

**No lugar:** acrescentar. `setTaskList` adiciona sem remover.

---

## Blind Mixin

**Atrai:** alguém já resolveu, o código está ali.

**Falha:** o Mixin foi escrito para outra versão, com outro mapping, e depende do
estado que *aquele* mod garante. Injeta no lugar certo com premissa errada — o
modo de falha mais difícil de diagnosticar.

**No lugar:** um Mixin em `initBrain`, verificado com `javap`, que delega. Copiar
a **ideia** é ótimo; copiar o código sem análise, não.

---

## Tick Everything

**Atrai:** garante que aconteça.

**Falha:** aldeão é caro por unidade. 20×/s × 50 aldeões × busca = TPS no chão.

**No lugar:** reativo > periódico > por tick, **escalonado** entre aldeões.

---

## Pathfinding Spam

**Atrai:** manter o caminho atualizado.

**Falha:** o cálculo mais caro do jogo, repetido por tick, por aldeão.

**No lugar:** recalcule quando o destino muda ou o caminho falha.

---

## Infinite Retry / No Failure State

**Atrai:** "ele vai conseguir na próxima".

**Falha:** o alvo pode ter sumido para sempre. O aldeão fica preso, gastando CPU,
parecendo quebrado.

**No lugar:** timeout, limite de tentativas, e o que fazer ao desistir. `[FATO]` o
Vanilla tem `CANT_REACH_WALK_TARGET_SINCE` — **o próprio jogo assume que retry
infinito é bug.**

---

## Ausência tratada como morte

**Atrai:** "não vi, logo não existe".

**Falha:** aldeão fora do raio ou em chunk descarregado não morreu. O registro
apaga trabalhadores vivos toda vez que o jogador se afasta — e a vila inteira
durante um raid, quando todos se escondem.

**No lugar:** só `AFTER_DEATH` e `MOB_CONVERSION` servem como prova.

---

## Esquecer a conversão

**Atrai:** morte parece cobrir "o aldeão se foi".

**Falha:** zumbificação é o caso **mais comum**, e não dispara `AFTER_DEATH`. A
correção parcial passa em todos os testes e falha em jogo.

**No lugar:** os dois eventos, sempre.

---

## Global Villager Manager

**Atrai:** um lugar que sabe de tudo.

**Falha:** vira o ponto de acoplamento de tudo, não é testável, e normalmente
guarda estado em `static`.

**No lugar:** estado por vila em `PersistentState`, estado individual em memória.

---

## Client Authority

**Atrai:** funciona no singleplayer.

**Falha:** no SP os dois lados compartilham memória. Em servidor dedicado vira
dessincronia ou trapaça.

**No lugar:** cliente pede, servidor valida e executa.

---

## Testar com dois aldeões

**Atrai:** dois já mostram o comportamento.

**Falha:** dois não mostram disputa por recurso, custo agregado, nem reserva
órfã. Todos os problemas de escala aparecem depois.

**No lugar:** 1, 2, 10, 50, 100. O teste de **2** pega conflito; o de **50** pega
custo.
