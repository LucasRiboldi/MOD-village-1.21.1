# TODO

**Atualizado:** 2026-08-22, depois de quinze commits e três sessões de jogo.

Este arquivo é a **lista canônica**. Onde ele discordar do
[`Backlog.md`](docs/technical/Backlog.md) ou do
[`Project-State.md`](docs/technical/Project-State.md), vale este — os
dois pararam em 2026-08-15 e estão sendo alcançados aos poucos.

O enunciado das regras está em `Project-State.md §18`; a arquitetura de
destino, na
[`ADR-009`](docs/decisions/ADR-009-Autonomous-Village-Evolution.md).

**A distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas em toda
lista abaixo.

```text
464 testes unitários  ·  157 testes de jogo  ·  30 regras  ·  9 ADRs
8 arquivos de código acima de 500 linhas  ·  5 de teste
3 sessões de jogo em 2026-08-22  ·  7 commits desde a última
```

---

## ✅ Resolvido

### 2026-08-22 — as sessões de jogo e o que elas cobraram

| | O que | Prova |
|---|---|---|
| **Nível 1 — a mina tem onde nascer** | Era uma coluna só, sem alternativa e sem log. Agora são doze — quatro lados por três distâncias —, a boca é superfície e não miolo de morro, e o fracasso tem voz | teste de jogo, fase vermelha conferida |
| **O mineiro tem voz** | `MinerWork` não tinha linha por ciclo. O lenhador ganhou a dele em 08-12, o construtor em 08-18 | teste de jogo |
| **Regra 30 — a boca mobiliada** | Lanterna de um lado, baú do outro; minério menos carvão fica lá até lotar | 2 testes de jogo |
| **Grupo não é equivalência** | `ResourceSubstitution`: o padrão é não substituir, e a substituição se declara | 6 testes unitários |
| **A pedra é contada por família** | A casa de deserto pede 93 de arenito e a conta enxergava 5 | teste de jogo |
| **O construtor pisa onde cabe** | `footOf` mandava o aldeão para dentro da duna | **visto em jogo:** `WAITING_RESOURCES` |
| **A colônia procura aldeão onde viu as camas** | Com o centro parado pela Emenda 4, ela olhava o lugar antigo | **visto em jogo** |

### 2026-08-21 — o ciclo das nove decisões

| | O que |
|---|---|
| **ADR-003 Emenda 4** | O centro da colônia só anda pela sonda. **Visto em jogo:** 4 movimentos, todos convergindo, 23 varreduras do jogador recusadas |
| **Regra 21 morta** | A mobília não é mais reposta; a demanda de lã e ferro passou para a obra |
| **Regra 28 grita** | Cada peça riscada nomeia a cadeia que falhou, e a sessão termina com a soma |
| **A cabana e a paleta apagadas** | `ColonyHut` saiu; `VillagePalette` ficou com estilo, pedra e vidro |
| **ADR-007 e ADR-008** | Fusão e orientação, decididas por escrito. **Nenhuma implementada** |
| **O ícone** | Fundo recortado a partir da borda, e não por chave de cor |
| **O `NOTICE`** | O arquivo da Mojang declarado em vez de escondido |
| **E9 instrumentado** | A transição de estado diz de onde veio e o que a sonda viu |
| **A receita da cama** | O livro devolvia o tingimento, e a conta de lã dava zero |

<details>
<summary>Ciclos anteriores — 08-19 a 08-21</summary>

Regras 25, 26, 27, 28, 29 · mineiro, pastor, fundidor · paleta por bioma
· vila de deserto reconhecendo a própria rua · a rua que cresce · a mina
no save · a cadeia da areia, do carvão e do ferro · Regra 11 · obra
parada que sai da frente · casa do jogo girada para a rua · lote
conferido no volume · árvore grande deixando de ser recusada.

</details>

---

## 🔴 Erros abertos

| | Erro | Estado |
|---|---|---|
| **E19** | **`MinerWork` cruzou o teto de 500 linhas** — 465 → 511 em 08-22. `BuilderWork` foi de 729 a 838 | Regressão deste ciclo. A ADR-009 §6 diz que 500 é indicador e o corte é por responsabilidade |
| **E18** | **Ninguém funde pedra.** A colônia cava arenito e a casa pede o liso | Visto em jogo 7× numa sessão. **Único elo entre a obra de deserto e a casa de pé** |
| **E20** | **`theStallGuardReturnsTheTaskAndForgetsTheTree` instável** | 2 falhas em 12 rodadas, depois de melhorar de 3 em 10. **Sem diagnóstico** |
| **E21** | **`theStoneLeavesTheWorldAndReachesTheChest`** disse "a pedra não chegou ao baú" uma vez | Suspeita: custo de ler estrutura no tique. **Suspeita, não diagnóstico** |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte | Instrumentado em 08-21; falta a sessão que responde |
| **E4** | `path held: no` e o aldeão chega assim mesmo | Provável, nunca verificado |
| **E3** | Sobra de colheita é perda de item | Conhecido e aceito |
| **E5** | Colheita de outras espécies nunca vista em jogo | Só carvalho |

---

## 🟠 Pendências, por nível de progressão lógica

A ordem é de dependência: cada nível precisa do anterior de pé.

### Nível 0 — o que já roda em jogo

Detecção · identidade estável · aldeões e profissões · baús · lenhador ·
fabricante · construtor **chegando ao bloco** · centro parado pela sonda
· casa terminada uma vez, em 08-19, com baús que o jogador encheu.

### Nível 1 — a raiz do material *(corrigido, não visto)*

- **A mina abrir de verdade.** A correção está escrita e testada; nenhuma
  sessão a viu. É a primeira linha a procurar: `opens a mine at`.
- A boca em terreno impossível ainda **desiste** em vez de a vila fazer
  outra coisa — isso é Nível 4.

### Nível 2 — material processado

- **E18 — quem funde pedra.** Pelo sistema **genérico**, sem exceção de
  deserto: "a colônia sabe produzir X a partir de Y".
- O fundidor tem duas linhas escritas à mão (`GLASS`, `IRON_INGOT`).

### Nível 3 — a obra termina sem o jogador

- Casa nunca terminou com material que a colônia mesma fez.
- **E21** — instrumentar antes de corrigir.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
- **A Regra 28 filtra o catálogo para `*_small_house_1`** — a mesma
  barreira que torna o teste possível impede escolher outro objetivo.
- **O catálogo do jogo já tem as alternativas**, e isso está confirmado:
  `farm`, `large_farm`, `animal_pen`, `armorer`, `mason`, `tannery`,
  `tool_smith`, `library`, `medium_house`, `big_house`. O propósito sai
  do **nome** — nenhum `.nbt` novo, nenhum byte da Mojang.
- `plains_small_farm_1` = terra, terra arada, tronco, água, trigo. **A
  vila de planície poderia construir isso hoje.**

### Nível 5 — o motor da ADR-009

`VillageProfile` · inventário de território · escassez e distância ·
orçamento de recursos · detecção de dependência circular · reserva
mínima de sobrevivência · objetivos graduais. **Nada disso existe.**

### Nível 6 — o que nem modelo tem

Comida · água · o fazendeiro (tem enxada e baú desde a Fase 4 e nunca
teve código) · população por capacidade · defesa · especialização ·
comércio entre vilas.

### Fora dos níveis — dívida que não bloqueia

- **8 arquivos de código acima de 500 linhas**, e 5 de teste:

```text
982  VillageDetectionHandler      621  BuildSiteScanner
838  BuilderWork                  550  ConstructionPlanner
724  TreeHarvester                511  MinerWork
639  ManufacturerWork             502  ColonySavedData

1571 LumberjackGameTest           754  MinerGameTest
975  BuilderGameTest              670  BuildSiteGameTest
                                  504  ProfessionAssignerTest
```

- **ADR-008** (orientação) e **ADR-007** (fusão), decididas e por escrever.
- **Regra 16** — distância mínima e máxima entre construções.
- **O ícone** — 1,95 MB num jar de 2,29 MB.
- **Cenário de teste por bioma.** A planície escondeu **duas vezes** que
  o deserto estava quebrado.
- **O `Development-Log`** parou em 08-15. Quarenta e seis commits e três
  sessões de jogo não estão nele.

---

## ⚠️ Incompatibilidades — o que se contradiz hoje

| | O que |
|---|---|
| 🔴 | **`ResourceSubstitution` é binária; a ADR-009 §3.10 pede quatro níveis** (`PREFERRED / ACCEPTABLE / ALTERNATIVE / FORBIDDEN`). A versão binária conserta o defeito e **não** implementa a política |
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **`ChestWithdrawer.takeGroup` ainda usa grupo como equivalência.** Hoje é inócuo — só o fundidor o chama, com `SAND` e `IRON`, que têm um membro só. É o resto do buraco |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |
| 🟡 | **ADR-009 §17 (população por capacidade) vs o vanilla**, que controla o *breeding*. O mod não tem como segurar população |
| ✅ | **ADR-009 §14 vs Regra 27 — resolvida.** O propósito da estrutura sai do nome, e o catálogo do jogo já os tem. Nenhuma contradição |

---

## 👤 Decisões que faltam, na ordem em que travam

| | Decisão | Trava |
|---|---|---|
| 1 | **Mina sem lugar:** a vila tenta outro raio, aceita boca ruim, ou declara `BLOCKED` e faz outra coisa? | Nível 1 → 4 |
| 2 | **Substituição:** fica binária ou vira os quatro níveis da ADR? `cut_sandstone` serve no lugar de `smooth_sandstone`? | Nível 2 |
| 3 | **Regra 28:** sai quando o planejador souber desistir, ou antes? | Nível 4 |
| 4 | **Regra 25:** morre ou volta? Hoje é lógica morta | limpeza |
| 5 | **Água e comida:** o mod planta e coloca água, ou fazenda fica fora do escopo? | Nível 6 e a alternativa de planície |
| 6 | **População:** o mod controla, ou aceita o *breeding* do vanilla? | ADR-009 §17 |
| 7 | **Fusão de colônias:** qual UUID e o teto de profissão já estão decididos na ADR-007 — falta só escrever | Nível 6 |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto:

| | O que | A linha que prova |
|---|---|---|
| **1** | **A mina abrindo** | `Miner ... opens a mine at ...` — ou a linha nova dizendo por que não |
| **2** | **O mineiro cavando** | `Miner ... took` de um arenito, e a linha do ciclo dizendo o que ele faz |
| **3** | **A boca mobiliada** | `Mine mouth at ... furnished — miner chest at ..., lantern at ...` |
| **4** | **A casa de deserto subindo** | `blocks left` caindo de 113 |
| **5** | **A barreira de teste** | `TEST BARRIER covered for nothing` é a notícia boa |
| **6** | **A casa esperando a cama**, e o pastor tosquiando por causa disso | `WAITING_RESOURCES` por `white_bed` |
| **7** | **Pastor, fundidor, fabricante descascando** | `sheared`, `made minecraft:glass`, `stripped a oak_log` |
| **8** | **A cadeia da areia inteira** | meta de `SAND` → praia → vidro → vidraça |
| **9** | **O E9** | `E9 — colony ... changed state N times`. Silêncio fecha o erro |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície —
taiga, savana, nevada e deserto nunca rodaram. E fechar e reabrir o mundo
de verdade nunca foi feito (dívida E4 do Backlog).
