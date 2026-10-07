# ADR-038 — Rotinas, trajetos e ócio das profissões

**Status:** Accepted
**Date:** 2026-10-07
**Decision Type:** Gameplay
**Origem:** resposta do autor ao plano `docs/technical/Plano-Atividades-2026-10-07.md` (P1–P7).

## Decisões

| # | Decisão |
|---|---|
| P1 | Contratar pela demanda: ofício parado cede vaga a quem produz o que a obra espera |
| P2a | Sem tarefa, cada ofício tem rotina própria: fundidor junta areia e argila; carpinteiro adianta peças da obra seguinte; construtor calça o caminho até a obra |
| P2b | **Pastor:** caça animais até a borda + 10, põe corda (corda infinita), traz um de cada vez e amarra na cerca mais perto da cama dele |
| P2c | **Pastor:** alimenta todo animal amarrado ou dentro de cercado da vila, com a comida de qualquer baú da vila |
| P3a | **Lenhador:** guarda na memória o lugar de toda árvore e muda dentro da vila; não anda até muda, vai direto à árvore disponível |
| P3b | **Mina:** boca nova até 5 blocos para dentro do limite da vila, preferindo morro e longe da água |
| P3c | **Pedreiro e carpinteiro:** trabalham diante da bancada do ofício, se houver na vila; senão, em volta do sino. Usam material de todos os baús e guardam o produto direto no baú da profissão, sem carregar |
| P5a | **Baú cheio:** qualquer baú da colônia passa 10 compartimentos para um baú da vila sem profissão |
| P5b | **Adiantamento** (peça que a obra não pediu): no máximo 5 de cada vez, depois troca de peça — ciclo sem fim de um pouco de tudo |
| P6 | Mina mais útil e persistente: ramais rumo ao minério de que a vila precisa; degrau em vez de encerrar por falta de lugar de pé; galerias que sobrevivem ao save |
| P7 | Varredura de superfície: começa pelas colunas onde já achou recurso; "nada no raio" espera 5 min também para terra, grama e argila |

## Estado

| # | Commit | Verificação |
|---|---|---|
| P5a, P5b | `93d99530` | Alívio: qualquer baú da colônia cheio passa 10 compartimentos para outro baú sem profissão. Adiantamento: `AdvanceStock` (core) — 5 de uma peça, depois a seguinte, em ciclo; peça parada 10 ciclos cede a vez; substitui o piso de 16 por peça do pedreiro; o lote do alívio de pedra cai de 64 para 5. 1355 unitários (`AdvanceStockTest`); 632/632 ×2 |
| P3b | `aa8c8bef` | `MineEdge.inside`: com caixa medida, toda boca nova (primeira mina, relocação, fundo esgotado) nasce de 1 a 5 blocos para dentro da borda; vence água longe (×4) + morro acima do centro até 12 (×3). Sem caixa, a escolha de antes. O GameTest não separa morro de água (os dois apontam para o mesmo lado). 633 ×2 (uma com o `craft_family`, KF-003) |
| P3a | `553657f0` | `VillageTrees`: por colônia, base de cada árvore (achada pela busca ou nascida da floresta) e cada muda (viveiro e replantio); muda que cresce vira árvore, tronco derrubado sai; teto de 512 por colônia; em memória (o save não guarda; a busca reconstrói). O lenhador vai à árvore conhecida mais perto **dele**; sem nenhuma, varre como antes. 634/634 ×2 |
| P3c | `8228ad76` | `CraftStation`: carpinteiro vai à bancada de trabalho mais perto da casa (até 16 blocos do baú); pedreiro ao cortador de pedra da vila (POI do jogo, até 64); sem bancada, o sino (POI de reunião); sem sino, o baú. Escolha guardada 5 min. Material e produto já eram virtuais (todos os baús → baú da profissão). O caminho do sino não tem teste. 635/635 ×2 |
| P2b, P2c | `200bc8fd` | `ShepherdFlock`: ovelha, vaca, porco e galinha guardados (amarrados em nó de cerca, ou cercados com cerca perto) ganham um par por espécie a cada 5 min, com a comida que aceitam de qualquer baú (trigo, cenoura, batata, beterraba, sementes); teto 12 adultos por espécie. **Mudou:** ovelha solta não procria mais. `ShepherdHerding`: sem tosquia, o pastor busca o animal solto mais perto até a borda + 10, põe corda sem gastar item, volta e amarra no nó da cerca mais perto da cama (até 16 blocos); até 8 por cerca; 2 min de prazo; desistência solta a corda sem largar item. O GameTest prova o mecanismo passo a passo, **sem a caminhada** (o pastor é posto no lugar). 636/636 ×2 |
| P1 | `aaf0989f` | `IdleYield` (core): ofício com mais de 60% de ócio em 2 janelas seguidas do `WorkTime` (10 min) e com 2 ou mais pessoas cede uma (sem tarefa): `giveUpProfession` marca o ofício como recusado para ela, e a contratação por demanda (`ProfessionDemand`, que já existia) a leva aonde a obra espera. Ofício de uma pessoa nunca cede. A coleta do pastor conta como trabalho. A regra tem teste unitário; o gancho no `WorkTime` (janela de 5 min) **não tem GameTest**. 1358 unitários; 636/636 ×2 |
| P2a (parte) | `e805218d` | Fila própria de adiantamento por ofício: o fundidor adianta vidro e pedra lisa, 5 de cada vez, em ciclo (busca areia pelo caminho que já existia). **Não feito:** o carpinteiro — as peças de madeira da obra (escada, laje de carvalho) não são recurso acompanhado, e adiantar tora descascada gasta a tora que a obra espera; o construtor calçar o caminho até a obra (traçado no terreno) ficou pendente. 1359 unitários; 636/636 ×2 |
| P6 (parte) | `18150ddc` | Pedra sem lugar de pé: o mineiro cava a pedra vizinha que alcança (pés, depois cabeça) e volta a ela, em vez de contar recusa até o ramal acabar (`MineVein.roomBeside`). **Pendente:** ramais rumo ao minério de que a vila precisa, e galerias/veio no save. 637/637 ×2 |
| P7 | `3ac580d8` | `SurfaceHits`: a coleta de superfície começa pelas colunas onde já achou o recurso e pelas 8 em volta de cada uma (até 32 por colônia e recurso, em memória); coluna sem nada é esquecida. O "nada no raio espera" já valia para terra, grama, argila e areia (`EmptySweeps`, 1/2/4 min por decisão de 03-10); os 5 min do plano **não** foram aplicados — fica para o autor (conflito com a de 03-10). 1361 unitários; 637/637 ×2 |
| versão | este commit | 0.3.3; JAR `103DE3F9…3734` em `build/libs`, `downloads/` e `mods`; 1361 unitários; 95 Python; PIT ok; 637/637. Pendências em `docs/technical/Decisoes-Pendentes-2026-10-07.md` |
