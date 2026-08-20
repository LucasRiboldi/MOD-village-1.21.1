# TODO

**Atualizado:** 2026-08-20, ao fim do ciclo de dezenove commits.

O enunciado de cada regra está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md) §18;
a lista longa, com a razão de cada item, em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). Onde os dois
discordarem, vale o Backlog.

**A distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas.

```text
431 testes unitários  ·  134 testes de jogo  ·  29 regras do autor
7 trabalhadores com código  ·  7 arquivos acima do teto de 500 linhas
```

---

## 🔒 As regras que mandam hoje

- **Regra 27 — imutável.** O construtor de cada bioma só levanta
  estruturas da pasta `minecraft-assets_structure`; o mod não cria casa.
  E ele **aguarda o bloco específico** de que precisa.
- **Regra 28 — provisória, e o autor a declarou assim.** Enquanto o
  projeto não estiver formalmente acabado: **uma casa por bioma**
  (`<estilo>_small_house_1`), e a obra **não espera** por porta, cama,
  lampião, baú, tronco descascado, tocha nem vidraça que não estejam num
  baú da vila.
  - **Onde ela sai:** `VillageStructures.ONLY_WHILE_TESTING` e
    `BuilderWork.isSkippableWhileTesting`. Dois lugares, e nada mais.

---

## ✅ O que foi feito neste ciclo

**Cadeia de produção — três profissões novas**

- **Mineiro** com mina de verdade (Regra 29): escada de dez degraus,
  sala 7×4 no nível −10, segundo lance virando, sala no −20, e galeria
  sem fim. Picareta de diamante.
- **Pastor**: tosquia e a ovelha continua viva. Fecha o laço
  casa → cama → aldeão → trabalhador.
- **Fundidor**: areia vira vidro, pela receita de fornalha do jogo.
- **Fabricante ampliado**: descasca tronco, monta tocha e vidraça.

**Construção**

- **Regra 27** — só o catálogo do jogo, e a espera pelo bloco exato.
- **Regra 28** — a barreira de teste.
- **Regra 25** — a maior planta que couber no lote.
- **Paleta por bioma** — parede, porta e as três matérias, por estilo.
- **`PatienceClock`** — obra parada sai da frente em vinte ciclos.
- **Regra 21 estendida** — a mobília vale para qualquer casa, não só
  para a cabana.
- **Peça destruída não volta**, e a conta vive no save.

**Corrigido, e as causas vieram de jogo**

- O alvo da obra era comparado com um id escrito no código.
- O cursor da busca de lote era guardado pela posição do centro, que
  troca de âncora a cada trinta segundos.
- O miolo oco da cabana era oferecido como lote.
- `TreeHarvester.isNaturalLeaf` derrubava o servidor com chunk
  descarregado.
- `ChestDepositor.deposit` era lido ao contrário pelo mineiro.

**Refatorado**

- `ConstructionPlanner` 703 → 414, em três arquivos.
- `LumberjackWork` 1232 → 455, em seis arquivos.
- `RingSweep`, `MineShaft`, `VillagePalette`, `PatienceClock` nasceram.

---

## 🧪 O que falta testar — nada disto foi visto em jogo

**Tudo o que este ciclo produziu está coberto por teste e nenhuma linha
dele rodou numa vila de verdade.** Em ordem do que mais precisa ser
visto:

| | O que | Como confirmar no log |
|---|---|---|
| **1** | **A mina** | `Miner ... opens a mine at ...`, depois a escada, a sala 7×4 e a galeria no chão do mundo |
| **2** | **A casa do catálogo subindo** | `planned minecraft:village/<bioma>/houses/<bioma>_small_house_1` |
| **3** | **A vila de deserto construindo** | Pela primeira vez na história do mod |
| **4** | **Pastor e fundidor** | `Shepherd ... sheared`, `Smelter ... made minecraft:glass` |
| **5** | **O fabricante descascando** | `Manufacturer ... stripped a oak_log into stripped_oak_log` |
| **6** | **A obra parada saindo da frente** | `gives up on ... never came in 20 cycles`, seguido de `planned` |
| **7** | **O cursor de busca por colônia** | A varredura concluindo, em vez de `still sweeping` eterno |
| **8** | **A mobília não voltando** | Nenhuma linha `furnished the house` repetida |

**Sem teste próprio, e a bateria só prova que nada quebrou:**

- O caminho novo do fabricante — descascar, montar tocha e vidraça.
- A retomada da mina depois de reiniciar o servidor.

**Coberto por teste, mas com limite conhecido:**

- **A arena da bateria tem bioma fixo.** A aceitação de uma vila de
  taiga, savana, nevada ou deserto nunca rodou.
- **Um teste instável:** `theStallGuardReturnsTheTaskAndForgetsTheTree`,
  cerca de 1 falha em 4 execuções, anterior a este ciclo.

---

## ⚠️ Conflitos — coisas que se contradizem hoje

Achados lendo o código ao fim do ciclo. Nenhum é urgente; todos são
dívida que cresce se ficar calada.

**1. Três regras sobre a mesma pergunta: a obra espera pela mobília?**

```text
Regra 21   não espera — a peça entra depois
Regra 27   espera sempre, sem exceção
Regra 28   não espera por sete blocos nomeados
```

Vale a 28, que é a mais nova, e ela é provisória. Quando sair, volta a
27 — e a 21 fica sem sentido, porque `HouseFurnishing` passa a nunca ter
o que repor. **Decidir se a 21 morre junto.**

**2. A `VillagePalette` ficou quase sem uso.**

`wall()` e `door()` só são lidos por `ColonyHut`, que a Regra 27
aposentou. O que a paleta ainda faz de útil é dizer o **estilo** (a pasta
do catálogo) e a **pedra** do bioma. Metade dela é código que ninguém
executa fora do caminho de save antigo.

**3. Um arquivo da Mojang num repositório público, e agora inútil.**

`src/main/resources/data/villagecolony/structure/houses/small_house.nbt`
é cópia byte a byte de `plains_small_house_1.nbt`. Desde a Regra 27 a
produção não o lê mais — só dois testes de jogo. O `Project-State`
afirmava em 08-19 que nenhum byte da Mojang tinha entrado no
repositório, e isso deixou de ser verdade naquele mesmo dia.
**Decisão jurídica, não técnica, e é do autor.**

**4. A Regra 25 está inerte enquanto a 28 valer.**

"A maior planta que couber" precisa de mais de uma planta. Com uma casa
por bioma, ela não escolhe nada. Volta a valer quando a barreira sair.

**5. `RingSweep` ficou sem quem o chame.**

Nasceu para o mineiro de superfície, que a mina substituiu no mesmo dia.
Ou os dois scanners migram para ele, ou ele sai.

**6. A cabana do mod continua no código.**

É deliberado — save antigo tem cabana pela metade, e apagá-la faria a
colônia construir por cima dela. Mas é uma estrutura que a Regra 27
proíbe criar, morando no código que a proíbe. **Vale um prazo:** quando
nenhum save conhecido tiver cabana, ela sai.

---

## 🗂️ Próximas atividades, por importância

**1 — Rodar em jogo.** Dezenove commits, quatro regras novas e três
profissões sem uma única sessão de verdade. Nada mais deveria ser
construído antes disto.

**2 — Gravar a mina no save.** Hoje ela é estado em memória num sistema
que grava todo o resto. Ao voltar, o mineiro reabre a boca e revarre os
índices já abertos. Custa pouco hoje e cresce com a profundidade.

**3 — Areia, para o vidro fechar a cadeia.** O fundidor funde a areia
que houver no baú, e ninguém a colhe. É o único elo da cadeia de
materiais que ainda depende do jogador.

**4 — Minério na mina.** O mineiro já desce vinte blocos e não reconhece
carvão nem ferro. Com eles, tocha e lampião saem da lista de dispensáveis
e a casa fica completa.

**5 — Regra 15, a estrada crescendo com a vila.** A colônia só constrói
em beira de rua que já existe. Quando ela acabar, a vila para — e a
Regra 25 só adiou isso.

**6 — Regra 11, uma de cada profissão por vila.** Ficou maior com a
cadeia: são sete profissões e catorze vagas por colônia. Nada garante o
piso, e a dispensa pode tirar o último de uma profissão.

**7 — Quebrar os sete arquivos acima de 500 linhas.**

```text
963  VillageDetectionHandler      639  ManufacturerWork
766  BuilderWork                  598  BuildSiteScanner
724  TreeHarvester                580  MinerWork
528  ColonySavedData
```

**8 — Decidir o movimento do centro da colônia.** Troca de âncora e
volta a cada 30 segundos, entre 49 camas e 7. Visto em 08-18, 08-19 e
08-20. É a ADR-003, e **espera decisão do autor**.

**9 — Regra 16**, distância mínima e máxima entre construções.

**10 — O `ItemRequest`.** O trabalhador pedir o que lhe falta em vez de
travar. Toca `Task`, que é o centro, e destrava a areia, a metade do
fabricante e a cadeia de receitas.

---

## 🟢 Depois disso

- **O fazendeiro.** Tem nome, enxada e baú desde a Fase 4, e nenhum
  trabalho. É a última profissão do modelo sem código.
- **O buraco que o mineiro deixa** na superfície e a mina aberta — se
  preenche, se fecha a boca, ou se aceita.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
- **Defesa.** Nada no modelo ainda.
