---
name: playtest-0385-deserto-iaxtye0lxm
description: "Playtest 21-10 da 0.3.85 no deserto (Spark IAXTyE0LXM) — TPS 20; 34/57 min sem obra porque NÃO HÁ lote (busca não está sem tempo); arenito, alcance do mineiro; estudo em docs/research/2026-10-21-deserto-ociosidade-alternativas.md"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-22T00:13:46.227Z
---

Versão confirmada no log: 0.3.85, 15:31–16:32 de 21-10, mundo da vila 153,-494. Relatório em
`docs/testing/relatorios/deserto-2026-10-21.md` (nota 6,1). Estudo de alternativas em
`docs/research/2026-10-21-deserto-ociosidade-alternativas.md`. Os dois estão sem commit.

**Causas medidas:**
- **P1, obra:** a busca de lote termina as voltas (472 passadas de ~63 colunas, 0 recomeços, todas
  respondidas pelo índice). "Budget ran out" é o estado normal da busca; **não existe lote**. Às 15:35 e às
  16:05 a vila anexou uma faixa ao sul (423/440 plana), e a rua da faixa cresceu 1 bloco e parou sem log
  (`keepGrowing` descarta as `PavingRefusals`). 115 mil recusas a −3…−8 (MAX_FILL=2).
- **P2, arenito:** `SupplyRoutes` aceita a rota direta "mina" do SANDSTONE antes da receita 4 areia → 1
  arenito, então ninguém fabrica arenito da areia. O G2 fez o arenito liso surgir 3×.
- **P3, mineiro:** `MiningTarget.judge` deu um ponto de trabalho a 4,12 da pedra (Δ 3,2,2 > REACH 4). Ele chega
  e não alcança, 2× no mesmo desvio. A boca da mina fica em y84, numa duna.
- **P4, pastor:** 39 procriações e 26 abates feitos em `tend`, fora de tarefa, contados como ócio no WorkTime.
  Falta comida porque o trigo fica no baú do fazendeiro e o VillageMeals come a beterraba.

**Erros meus de leitura, corrigidos na sessão:** "budget ran out" não é falta de tempo; o `n` do diário é a
cota da tarefa e não uma contagem (anunciei um laço do lenhador que não existia); a espera de queda do mineiro
é 1 tique.

**0.3.86 feita (o autor escolheu A1, C1, B1, D2):**
- Branch `fix/deserto-a1-c1-b1-d2`: e38a5e47 (C1), 91de43ca (A1), e002ba46 (B1, FurnaceFeed),
  25db9392 (D2), d7eb0c5f (versão). **Sem push nem PR.** JAR 4A56148A… instalado, não jogado.
- **KF-007 nova:** o MinerDirectPath cai na bateria com o arranjo de 830+ testes; não vem do código.
- **Achado:** `CraftsmanRequest.NOMINAL = COBBLESTONE` é o "cobblestone" do carpinteiro. O
  `FurnaceFeed.isOrder` exclui esse alvo de propósito.
- **0.3.86 jogada** (21-10, 28 min, Spark j7jTrVIaPQ, nota 3,7, análise em §10 do estudo):
  - A1: a rua da faixa para porque a duna sobe 2+ blocos na entrada.
  - B1: funciona (144 arenitos lisos fundidos).
  - C1: sem a assinatura, mas com 1 desvio só.
  - D2: não exercido (nenhuma ceia).
  - Novo: mineiros 71% encalhados num vaivém do ClimbOut (x=110↔113) que zera o BedTeleport.attempt
    (raio 1,5 < túnel). Hipótese: o arenito do DesertGround é recusado pela rua como peça original.
- **Próximo:** corrigir o vaivém do BedTeleport; decidir a rua na duna (A3 ou corte de rua).
- **Decisões abertas:** A5, F2/G1, D1.

Decodificador do Spark: a estatística por janela é o campo 7 do **topo**; o StackTraceNode tem classe=3,
método=4, times=8, refs=9. Ver [[spark-log-decodificar]].

Relacionado: [[deserto-jogado-0377]], [[folga-de-chegada-contra-alcance]], [[abandono-por-deriva-do-centro]],
[[regra-tres-buscas-material]].
