# Pastor aproveita o rebanho — design (2026-10-14)

## Objetivo

Estender a **manutenção do ofício do pastor** (direção aceita no ciclo anterior: profissões
contínuas por manutenção, sem tocar a obra — Regra 59). Quando ocioso, além da lã e da criação, o
pastor aproveita os outros animais:

1. **Couro e carne a partir do 5º adulto da espécie** — com mais de 4 adultos de uma espécie, abate
   o excedente (do 5º em diante), **gradualmente, 1 por ciclo**, deixando sempre ≥ 4 adultos vivos
   para repovoar.
2. **Ovos de galinha** — recolhe do chão o ovo que a galinha já larga sozinha.
3. **Cozinha** a carne crua (o próprio pastor), **sem carvão/combustível**.
4. **Reserva fixa (16)** de cada item no baú: só processa/cozinha o que excede a cota.

Decisão do autor, 2026-10-14. É **comportamento novo**, não profissão nova (confirmado com a skill
`minecraft-villager-systems`): o pastor já existe; isto é ação por ciclo na manutenção, sem
POI/memória/sensor novos.

## Comportamento Vanilla confirmado (fonte, não presumido)

- **`PassiveEntity.isBaby()`** / `getBreedingAge()` (Yarn 1.21.1): adulto = `!isBaby()`.
- **`LivingEntity.damage(DamageSource, float)`** (2 args) e `kill()` existem.
- **`ChickenEntity`**: larga `Items.EGG` no chão sozinha a cada 6000–12000 ticks (`eggLayTime`,
  `tickMovement` → `dropItem(Items.EGG)`). O ovo **já vira item no mundo** — o pastor só recolhe.
- **Drops Vanilla por espécie** (loot tables): vaca → leather (1–2) + beef (1–3); porco → porkchop
  (1–3); ovelha → mutton (1–2); galinha → chicken (1) + feather (0–2). Valores médios usados no
  depósito.
- **`SmelterWork`** já documenta: a conversão do mod é **sem combustível** ("transforma o que está
  no baú"); `CraftingLookup.smelted(world, raw)` dá o cozido (beef→cooked_beef). O pastor reusa esse
  caminho — por isso **nenhuma profissão precisa de carvão** para cozinhar (já é verdade hoje; carvão
  no mod é recurso/minério do mineiro, não combustível).

## Comportamento atual reusado

- `ShepherdFlock`: lista `SPECIES`, conta adultos por espécie (`!isBaby()`), teto de rebanho, cria
  pares (`pairOf`/`tend`). O abate é o **espelho** da criação — mesma classe, mesma lista, mesma
  contagem.
- `ShepherdWork.run`: chama `ShepherdFlock.tend` quando o pastor está ocioso (sem tarefa de lã).
- `GroundPickup`: recolhe item do chão — reusado para o ovo.
- `ChestDepositor.deposit` / `ColonyChests.countIn` / `ColonyChests.nearestFirst`: baús.
- `CraftStation`-style POI: achar a fornalha/fogueira perto (se o cozimento exigir estação).
- `ActionJournal`: registrar as ações novas.

## Arquitetura

### Abate gradual (`FlockHarvest`, novo, ou método em `ShepherdFlock`)
- Entra no `ShepherdFlock.tend`, uma ação por ciclo (como `pairOf`): para **uma** espécie com > 4
  adultos, abate **um** animal adulto (o excedente), se a reserva de item ainda não está cheia.
- **Reserva viva:** `ADULTS_KEPT = 4` por espécie — nunca abate se isso deixaria < 4 adultos.
- Abate: `animal.damage(world.getDamageSources().mobAttack(pastor), lethal)` (ou `kill()`), e
  **deposita o drop esperado por espécie direto no baú** (padrão do `shear`, não depende da loot
  table caindo): vaca → 1 leather + 2 beef; porco → 2 porkchop; ovelha → 2 mutton; galinha → 1
  chicken (+ a lã da ovelha e pena seguem o que já cai). `ActionJournal.action(..., "BUTCHERED", ...)`.
- A ovelha abatida não conflita com a tosquia (lã) nem com a criação para lã (`breedForWool`): o
  abate só mira o excedente acima de 4 adultos, e a tosquia continua preferida enquanto há lã.

### Ovos (`GroundPickup`, reuso)
- Na área do rebanho, o pastor recolhe `Items.EGG` do chão (o Vanilla já o largou). Sem mexer na
  galinha. Mesma cota de reserva.

### Cozinhar (reuso de `CraftingLookup.smelted`, sem carvão)
- Para a carne crua acima da reserva, o pastor leva à fornalha/fogueira da vila (POI, raio como o
  `CraftStation`) e assa via `CraftingLookup.smelted` (beef→cooked_beef, etc.) — **sem combustível**,
  como o fundidor. Guarda o cozido no baú. (Se não houver estação perto, deposita o cru e tenta no
  próximo ciclo — não fica parado.)
- Ovo em receita de comida (ex.: bolo) **só se** houver todos os ingredientes; caso contrário, o ovo
  fica guardado. Não força.

### Reserva (cota por recurso, como `SmelterAdvance.KEEP`)
- `KEEP = 16` por item (leather, beef/cooked_beef, porkchop, mutton, chicken, egg). O abate e o
  cozimento só agem sobre o excedente acima de 16 daquele item no baú; a cota fica sempre guardada.
- Dois limites distintos e independentes: **ADULTS_KEPT=4** protege o rebanho vivo; **KEEP=16**
  protege o estoque de itens.

## Fluxo de dados (por ciclo, pastor ocioso)

1. `ShepherdWork.run` sem tarefa de lã → `ShepherdFlock.tend`.
2. `tend` faz: criar par (já existe) **e** `FlockHarvest.step` — se alguma espécie > 4 adultos e o
   item não está na cota, abate 1 e deposita o drop.
3. Carne crua acima de 16 → cozinha na estação (sem carvão) → deposita o cozido.
4. Ovo no chão → `GroundPickup` → baú (até a cota).
Nenhum passo deixa o pastor parado: se não há excedente/estação, cai no próximo item da manutenção.

## Erros e bordas

- Nunca < 4 adultos (reserva viva); abate 1 por ciclo evita dizimar e espalha o custo.
- Sem fornalha/fogueira perto: deposita o cru, não trava.
- Baú cheio: respeita o leftover do `ChestDepositor` (como o resto do mod).
- Multiplayer/save: sem estado de IA novo em `static Map` — a ação é por ciclo, derivada do mundo
  (contagem de adultos) e do baú (cota); nada a persistir além do que já existe.

## Testes

- **Unidade** (lógica pura de `FlockHarvest`): "> 4 adultos e item abaixo da cota → abate 1";
  "exatamente 4 adultos → não abate"; "item na cota (≥16) → não abate"; a reserva viva nunca cai
  de 4.
- **GameTest**: 6 vacas adultas + baú vazio → após o ciclo, 5 vivas e leather+beef no baú; 4 vacas →
  nenhuma abatida; ovo no chão na área → recolhido; carne crua acima da cota com fornalha perto →
  vira cozida.
- **Mutação**: remover a reserva viva de 4 (ou o teto da cota) derruba o teste correspondente.
- Determinismo: spawnar os animais com idade adulta explícita; afirmar sobre contagem viva e itens
  no baú, não sobre timing.

## Arquivos

**Novo:** `fabric/work/FlockHarvest.java` (abate + depósito do drop), testes
(`FlockHarvestTest` unidade, `FlockHarvestGameTest`).
**Editados:** `ShepherdFlock.java` (chamar `FlockHarvest` no `tend`; reusar `SPECIES`/contagem),
`ShepherdWork`/cozimento (levar carne à estação, reuso de `CraftingLookup.smelted`), lang se
precisar de novo texto. Sem mexer na obra (Regra 59). Arquivos sob 500 linhas (A3 da varredura).

## Versão

Sai como 0.3.56, branch curta `claude/pastor-rebanho` a partir da main. Commit por marco; JAR +
playtest de dia.
