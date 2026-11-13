# 10 — Comparação com o Village Colony

A matriz do §11 do briefing. Nada aqui é inventado: cada coluna aponta o
arquivo de onde saiu.

---

## 1. A matriz

| Sistema | Workers | Village Colony | Melhor abordagem |
|---|---|---|---|
| **Worker Entity** | `AbstractWorkerEntity extends recruits.AbstractChunkLoaderEntity`. Nove subclasses. Inventário próprio, moral, dono, pagamento | aldeão **Vanilla**, sem subclasse. `core.worker.model.Worker` é dado puro; `VillagerEntityMixin` só injeta no `initBrain` | **VC.** ADR-001: estender o Vanilla em vez de substituir. Compatibilidade e zero entidade nova. O preço — sem item na mão renderizado — você já registrou |
| **AI** | `goalSelector` do Vanilla, 19 Goals, `enum State` por Goal (até 17 estados) | `Brain`/`MultiTickTask` (`GoToWorkTargetTask`) + lógica em `fabric/work/*` sobre um `Task` do Core | **VC.** A decisão sobre o trabalho mora em Java puro e é testada; só o movimento passa pelo Brain |
| **Tasks** | **não existe.** Cada trabalhador acha o próprio trabalho | `Task` com `AVAILABLE→RESERVED→EXECUTING→COMPLETED`, `release`, `cancel`, transição inválida lança | **VC, com folga.** Ver `04` |
| **Pathfinding** | pathfinder A* próprio: heurística com peso em Y, orçamento adaptativo, custo por queda/água/beco, chegada exata | navegação Vanilla do aldeão via memória `WALK_TARGET` | **Depende.** Workers é tecnicamente superior; VC é o certo para aldeão Vanilla (ADR-004). Ver `05 §7` |
| **Inventory** | inventário do trabalhador (slots 6+), `NeededItem`, depósito ao passar de 128 | o baú do trabalhador **é** o inventário. `ChestDepositor`/`ChestWithdrawer`/`ChestInventoryReader` | **VC** para o modelo de vila. **Workers** tem o `NeededItem`, que falta a você |
| **Jobs** | classe por profissão + Goal por profissão. Profissão nova = 5 classes + 8 arquivos do núcleo editados | `ProfessionType` + `ProfessionRegistry` + `Capability` + `ToolType`, tudo dado. `ProfessionAssigner` preenche vaga | **VC, com folga.** Ver `07 §5` |
| **Persistence** | NBT de entidade, 3 campos. Sem `SavedData`. Estado de trabalho não persiste | `ColonySavedData` (`PersistentState`): colônias, trabalhadores, baús, obras, construções. Testado | **VC** |
| **Work Areas** | `Entity` invisível desenhada pelo jogador, 11 tipos, com GUI e sincronização automática | o mod **descobre** o trabalho: `VillageScanner`, `TreeScanner`, `ChestScanner`, `BuildSiteScanner` | **Depende do jogo que se quer.** Workers = o jogador manda; VC = a vila se organiza. São produtos diferentes |
| **GUI** | 17 telas, 4 widgets, ~4.500 linhas. Preview 3D da estrutura | nenhuma. Nome sobre a cabeça (`WorkerNameplate`) e log | **Workers**, hoje. Mas é o eixo do produto dele, e não do seu — no seu, GUI é P3 |
| **Networking** | 27 mensagens, uma por operação de GUI. `SynchedEntityData` para os campos de área | nenhuma mensagem custom. Nada a sincronizar | **VC**, por não precisar. Quando precisar, o padrão "uma mensagem por operação" do Workers é sadio |

---

## 2. Onde vocês convergiram sozinhos

Os pontos em que dois autores, sem se conhecer, chegaram à mesma regra.
São os mais valiosos da análise: cada um deles é uma decisão sua que
acabou de ganhar uma segunda testemunha.

### 2.1 Tronco sem copa viva não é árvore

* **VC:** decisão de 2026-08-12, `Project-State.md §10`.
* **Workers:** `LumberArea.hasNaturalLeavesConnected` (linha 232).

Ambos porque a casa de madeira do jogador foi derrubada. O Workers testa
duas propriedades (`PERSISTENT` e `DISTANCE`); vale conferir se o seu
testa as duas.

### 2.2 O mundo é a verdade sobre o progresso da obra

* **VC:** "quem sabe o que já está de pé é o mundo"
  (`Project-State.md §9`).
* **Workers:** `BuildArea.scanBreakArea()` + `statesMatch()` a cada
  retomada.

Mesma regra. Você escreveu o porquê; ele não.

### 2.3 Dois relógios para o trabalho

* **VC:** `LumberjackWork.run` no ciclo longo, `LumberjackWork.tick` no
  tick.
* **Workers:** `WOOD_CUTTING` tratado antes do gate `% 10`
  (`LumberjackWorkGoal:81-88`).

A quebra de bloco precisa de todo tick; a decisão, não.

### 2.4 Quebrar bloco leva o tempo que leva

* **VC:** Regra 2 de 2026-08-08 — tempo de um jogador com machado de
  ferro (`BlockBreakTime`).
* **Workers:** `mineBlock` com `breakingTime = destroySpeed * 30` e
  `destroyBlockProgress` (`AbstractWorkerEntity:385-403`).

Os dois calculam a partir da dureza do bloco e mostram progresso. O
Workers usa a ferramenta real na mão; você fixou no machado de ferro **de
propósito**, e registrou por quê.

### 2.5 Instrumentar antes de suspeitar

* **VC:** `Project-State.md §11`, com o E14 como preço pago.
* **Workers:** a linha comentada de `setState` em todos os Goals, e os 11
  `ERROR_*` nomeados.

---

## 3. Onde vocês divergiram, e por quê

| Questão | Workers | Village Colony | O que separa |
|---|---|---|---|
| Quem é dono do trabalhador | o jogador | ninguém | ADR-001: a vila é do mundo |
| Quem escolhe onde trabalhar | o jogador, plantando áreas | o mod, varrendo | PROJECT_CONSTITUTION §4 |
| Simula sem jogador | sim, force-load | não, `DORMANT` | ADR-002 |
| Depósito | volta ao armazém a cada 128 | direto no baú próprio | decisão de 08-08 (TASK-026 cancelada) |
| Blocos de duas partes | segunda passada, com par | duas metades soltas (E8) | **aqui o Workers está à frente** |
| Estrutura a construir | jogador escaneia e salva `.nbt` | lê estrutura Vanilla do jogo | ADR-001 |
| Teste | zero | 366 + 80 | — |

---

## 4. Os cinco pontos em que o Workers está à frente

Honestidade exige listá-los:

1. **Blocos de duas partes** resolvidos (segunda passada + par
   localizado). Seu E8 está aberto e a TASK-046 supõe que exige decisão
   de arquitetura — **pode não exigir**. Ver `12 §3.5`.
2. **Pedido de item declarado** (`NeededItem`). Você não tem como um
   trabalhador dizer "falta-me X".
3. **Pathfinding**, em capacidade absoluta. Não aplicável a você, mas é
   fato.
4. **Motivos de falha nomeados em toda parte**, não só na construção.
5. **GUI**. O jogador vê e configura tudo; no seu, tudo passa pelo log.

---

## 5. Os pontos em que o Village Colony está à frente

1. **Núcleo testável sem Minecraft.** 366 testes de unidade, e um
   `DependencyRuleTest` que faz cumprir a ADR-006 §6 lendo o
   `import`. O Workers não tem uma unidade que se possa exercitar.
2. **Sistema de tarefas de verdade**, com estados, transições validadas e
   dono único.
3. **Persistência de domínio** (`ColonySavedData`), testada.
4. **Extensibilidade de profissão** por dado, não por classe.
5. **Zero dependência de terceiros.** O Workers não roda sem o Recruits.
6. **Decisões registradas.** Seis ADRs, `Development-Log.md`, e um §17 que
   registra erro sem inventar causa. O Workers tem seis comentários bons e
   um `//SOMETHING WRONG I CAN FEEL IT`.
7. **Gametest.** A camada de fronteira — onde o §11 diz que moraram todos
   os defeitos sérios — tem 80 testes. No Workers, nenhum.

---

## 6. Incompatibilidades arquiteturais duras

Registradas para que nenhuma tentativa futura de "trazer uma classe" seja
feita por engano.

```text
1. Loader          Forge ≠ Fabric.
                   @Mod/@SubscribeEvent/DeferredRegister/SimpleChannel
                   não existem no Fabric.

2. Mappings        official ≠ yarn. Level/World, getCenter/toCenterPos,
                   BlockPathTypes/PathNodeType — nomes diferentes para
                   as mesmas classes.

3. Versão do jogo  1.20.1 ≠ 1.21.1. Componentes de item mudaram,
                   ItemStack.isSameItemSameTags não existe mais.

4. Java            17 ≠ 21.

5. Base de entidade O Workers herda de recruits.AbstractChunkLoaderEntity.
                   Sem o Recruits, AbstractWorkerEntity não compila.

6. IA              goalSelector (PathfinderMob) ≠ Brain (VillagerEntity).
                   Nenhum Goal do Workers roda num aldeão Vanilla.

7. Tipos no domínio Workers usa BlockPos/BlockState/Level em toda parte.
                   Sua ADR-005 proíbe no Core, e o DependencyRuleTest
                   reprova o build.

8. Simulação       force-load por trabalhador ≠ ACTIVE/DORMANT da ADR-002.

9. Propriedade     dono jogador + times ≠ colônia sem dono.
```

**Conclusão operacional: nenhuma classe do Workers pode ser aproveitada
como código.** Isso é independente da licença — mesmo que fosse MIT, não
compilaria e não caberia. A licença apenas torna a questão irrelevante.
