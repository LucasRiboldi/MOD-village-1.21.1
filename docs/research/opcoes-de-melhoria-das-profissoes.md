# Base de opções de melhoria — o que trava as profissões

> **Pesquisa de 2026-09-17**, skill `minecraft-code-research`, modo FEATURE.
> Ambiente conferido no `gradle.properties` de hoje: MC **1.21.1**, Yarn
> **1.21.1+build.3**, Loader **0.19.3**, Fabric API **0.116.15+1.21.1**.
>
> **É uma base de opções, não um plano.** Cada entrada traz o degrau da
> escada de extensão, a evidência e o custo. A escolha é do autor.

---

## 0. ⚠️ O que esta pesquisa NÃO tem

O pedido citava *"pesquisei como outros mods resolvem estes problemas"*.

**Não consegui ler o código do MineColonies, Ancient Warfare ou Millénaire.**
Nenhum está neste disco, e a busca web não estava disponível na sessão.
Descrever o que eles fazem seria inventar — e a skill é explícita:
`[FATO]` exige arquivo, classe e método.

**O que fiz no lugar**, e que é verificável: apliquei a hierarquia da skill —
**Vanilla → Fabric API → o código do mod** — sobre os pontos de travamento
medidos nos playtests. Os degraus 1 e 2 responderam mais do que eu esperava.

Se você tiver os `.jar` desses mods, aponte a pasta e eu leio: a skill tem
roteiro próprio (`references/mod-analysis.md`) e a comparação vira uma matriz.

---

## 1. Água que trava o mineiro (P1.2)

### 1.1 O que já existe — e funciona

⚠️ **O que o pedido descreveu já foi implementado em 2026-09-03.**

```java
// MinerWork.java:726
if (MineFlooding.seal(world, job.target) > 0) {
    MineDigging.flooded(job.task.colonyId(), villager.getUuid(), job.target);
}
```

`[FATO]` `MineFlooding.seal` tapa **as seis faces** que vertem, respeitando a
Regra 3, e `MineDigging.flooded` vira o ramal. É exatamente *"fechar o bloco
que gerou a água e trocar de caminho"*.

### 1.2 A lacuna, medida

| Frase no log de 2026-09-17 | Vezes |
|---|---|
| `The mine sealed N face(s)` — o conserto agindo | **5** |
| `The branch turns away from the water` | **5** |
| `… gave up … which is flooded` — o aldeão travado | **30** |

**O conserto cobre 1 caso em 6.** E a razão está na linha inteira:

```text
Miner … gave up the stone at 2455,20,-2999 — it has not moved a block in
300 ticks … the miner is at 2453,31,-3002, 11,5 blocks away … the place to
stand is 2455,21,-2999, which is flooded
```

`[FATO]` **É o lugar de ficar de pé que está alagado, e o aldeão está a 11
blocos dali, andando.** A picareta nunca chega; `seal` só roda **depois** de
o bloco ser quebrado. O caso morre antes de o conserto existir.

### 1.3 Opções

| # | Opção | Degrau | Custo | Efeito esperado |
|---|---|---|---|---|
| **A1** | **Recusar o alvo cujo `place to stand` está alagado, na escolha** | 1 (Vanilla) | baixo | Ataca os 30 direto. O mineiro nunca anda 300 tiques para um lugar onde não pode ficar |
| **A2** | Tapar a fonte **ao escolher**, e não ao quebrar | 1 | médio | Recupera o alvo em vez de descartá-lo; mas edita o mundo longe do aldeão |
| **A3** | Penalidade de pathfinding no aldeão | 1 | baixo | Ver §1.4 — trata o caminho, não o destino |

**Recomendação: A1.** O mod já sabe detectar (`MinerReport` escreve *"which
is flooded"*), e a informação é jogada fora — ela vira texto de log em vez de
recusa. É o menor conserto com o maior efeito medido.

### 1.4 O que o Vanilla oferece de graça

`[FATO]` Conferido por `javap` no JAR de 1.21.1:

```java
// MobEntity — API pública, sem Mixin
public float getPathfindingPenalty(PathNodeType);
public void  setPathfindingPenalty(PathNodeType, float);

// PathNodeType — o Vanilla já distingue:
WATER, WATER_BORDER, LAVA, DANGER_OTHER, DAMAGE_OTHER, BLOCKED

// LandPathNodeMaker — público e estático:
public static PathNodeType getLandNodeType(MobEntity, BlockPos);
```

`[FATO]` **O mod não usa nenhum dos três** — `grep` por `setPathfindingPenalty`
e `PathNodeType` em `src/main` não retorna nada.

`[INFERÊNCIA]` `getLandNodeType(mob, pos)` permite perguntar **ao próprio
Vanilla** se uma posição serve, antes de mandar o aldeão andar — com a mesma
régua que a navegação vai usar. Hoje o mod tem predicados próprios
(`nowhereToStand`, `MineRock.isOpenSpace`), e duas réguas que discordam já
causaram defeito neste projeto: é a assinatura do E46.

`[VALIDAÇÃO NECESSÁRIA]` Confirmar em gametest que
`getLandNodeType` devolve `WATER` no cenário do log antes de trocar
qualquer predicado.

---

## 2. Tarefa que não abre (o tema das 9 profissões)

`[FATO]` **Corrigido nesta sessão** (commit `4cee104`): a obra abre a tarefa
pela peça que espera, e quem decide se dá para fabricar é
`ColonySupply.canProvide` → `CraftingLookup`, que lê as receitas reais do
jogo.

### 2.1 O que o Vanilla oferece e o mod já usa

`[FATO]` `CraftingLookup` consulta `RecipeType.SMELTING` e
`RecipeType.CRAFTING` — degrau 1 da escada, sem lista escrita à mão.

### 2.2 Opção aberta

| # | Opção | Degrau | Custo | Efeito |
|---|---|---|---|---|
| **B1** | Ler também `RecipeType.STONECUTTING` | 1 | baixo | O pedreiro tem cortador de pedra (`ChestMarker` dá `Items.STONECUTTER` ao MASON), e o cortador faz escada e laje **1 para 1**, contra 6→4 da bancada |

`[FATO]` `CraftingLookup` **não lê** `STONECUTTING` hoje — só `SMELTING` e
`CRAFTING`.

`[INFERÊNCIA]` Isso é desperdício de material, não travamento: a bancada
resolve, só que mais caro. **Não é urgente.**

---

## 3. Seguir o veio inteiro (pedido do autor, P1.3)

⚠️ **Já está implementado.** `[FATO]` `MineArm.followVein` e
`veinExhausted` existem, e `MineDigging` os usa em **seis** pontos — o veio é
perseguido até esgotar, com teste que afirma: *"só veio um carvão — o mineiro
voltou ao túnel e deixou a veia pela metade"*.

`[INFERÊNCIA]` No playtest de 09-17 o mineiro tirou 60 blocos e passou a
maior parte do tempo emparedado ou parado em água. **Talvez não falte
comportamento, e sim oportunidade.**

`[VALIDAÇÃO NECESSÁRIA]` Contar, num playtest com o P1.2 corrigido, quantos
veios foram encontrados e quantos foram seguidos até o fim. **Sem esse
número, mexer aqui é mexer no que já funciona.**

---

## 4. Explorar mais a camada da mina (pedido do autor, P1.3)

`[FATO]` As constantes que governam, todas em `MineShaft`:

| Constante | Valor | O que decide |
|---|---|---|
| `ARM` | 16 | Até onde cada ramal vai |
| `RINGS` | `ARM / RUN` | Quantos anéis a espiral abre |
| `HELIX_FLIGHTS` | 4 | Voltas da escada |
| `CARVED` | 120 | Posições até o poço abrir |
| `DEEPEST` | −59 | Fundo da mina |

| # | Opção | Custo | Preço |
|---|---|---|---|
| **C1** | Aumentar `ARM` | baixo (uma constante) | Nível demora mais a fechar; mina desce mais devagar |
| **C2** | Adensar a galeria (mais bolsões) | médio | Mais pedra por nível, mais tiques por nível |
| **C3** | Deixar como está | zero | — |

⚠️ **É decisão de design, não defeito.** E o projeto tem uma regra escrita
para isto: *"o log decide a constante"* — o número novo sai do vale na
distribuição do playtest, não de chute. **Recomendo medir antes:** quantos
níveis a mina fecha por hora hoje, e quanta pedra rende cada um.

---

## 5. Prioridade sugerida

| Ordem | Item | Por quê |
|---|---|---|
| **1** | **A1** — recusar alvo com `place to stand` alagado | 30 travamentos medidos; o mod já detecta e joga a informação fora |
| **2** | Confirmar em jogo o P1.1 e o P1.0 | Duas correções sem playtest; medir antes de empilhar |
| **3** | §3 — contar veios seguidos vs. encontrados | Antes de mexer no que já funciona |
| **4** | **B1** — `STONECUTTING` | Economia de material, não travamento |
| **5** | §4 — constantes da mina | Decisão de design, e o log decide o número |

---

## 6. Etiquetas de risco

- `[RISCO]` Trocar `nowhereToStand` por `getLandNodeType` muda a régua de
  escolha de alvo, e **régua de escolha e régua de execução que discordam já
  produziram o E46**. Trocar as duas juntas, ou nenhuma.
- `[RISCO]` `MineFlooding.seal` edita o mundo. Fazê-lo na **escolha** (A2)
  significa editar longe do aldeão, o que é mais intrusivo que hoje.
- `[VERSÃO]` Todos os `[FATO]` deste documento valem para **1.21.1** e foram
  conferidos por `javap` no JAR desta versão.
