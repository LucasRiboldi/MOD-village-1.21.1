# STATE — 2026-11-24

> Arquivo de estado vivo. **Sobrescreve, não acumula; teto de 150 linhas.** Consulte o [STATE de 13-11, candidatas 0.3.58–0.4.74](docs/archive/technical/Historico-2026-11.md) e os históricos de [outubro](docs/archive/technical/Historico-2026-10.md) e [setembro](docs/archive/technical/Historico-2026-09.md).
>
> **Retomada por outro agente:** comece por `HANDOFF.md`.

## Onde o projeto está
- **Candidata 0.4.133, instalada (Regras 151 e 160).** Resgate de telhado sem queda perigosa e trecho da rede de ruas só sobre chão de rua ou natural, mais o baú na borda do chunk (PR #106). JAR SHA-256 `FAC43A023956DF59C615BD3039DD9281113AAC6D31C2DBE14547682B1AAB3998`. PR #107 mesclado em `codex/piso-curral-grande` (`c76aadc5`); pré-release adiada até os testes instáveis (PenEscape, N05, N26, ChestPlacer na borda, que o PR #108 não estabilizou) ficarem verdes. Aguarda [playtest](docs/technical/Playtest-0.4.122-2026-11-23.md) perto de `338,86,1414`. Detalhes no [HANDOFF](HANDOFF.md).
- **Candidata local 0.4.131.** Obras novas ou retomadas do curral grande moldam o piso de circulação em grama no terreno natural do bioma. Água e canteiros seguem a planta. O item 9 aguarda o [playtest](HANDOFF.md#candidata-local-04131-2026-11-24).
- **Entrega preparada.** O código e os documentos estão na branch `codex/piso-curral-grande`. PR, CI, merge e `v0.4.131-alpha` ainda são etapas de integração; o roteiro está no topo de [`HANDOFF.md`](HANDOFF.md) e a lista viva no topo de [`TODO.md`](TODO.md).
- **Desde a 0.4.130.** Placas externas acompanham as camas atribuídas. Quadros de baús em casas pequenas preferem o centro. Baús residenciais com recibo de colocação podem migrar para uma parede livre. Os itens 7 e 8 aguardam o [playtest](HANDOFF.md#candidata-local-04130-2026-11-24).
- **Candidata local 0.4.129.** O pastor evita portas com animal na guia. Trabalhadores ficam fora de conversas entre aldeões no expediente. Falta confirmar o item 6 no [jogo do deserto](HANDOFF.md#candidata-local-04129-2026-11-24).
- **Grafo na 0.4.128.** A rua Vanilla de arenito entra no grafo desde a primeira leitura. BigHouseMOD e casa do mineiro pedem releitura após a fundação. O [handoff](HANDOFF.md) contém o roteiro do deserto. Os seis currais médios de grama e a prioridade do curral grande vieram da 0.4.127 e seguem em validação no [TODO](TODO.md).
- **Candidata local 0.4.126.** N10 prioriza a porta voltada ao centro com rua transitável no mesmo lote. N14 preserva as vagas de criação de baús quando uma tentativa falha. Os GameTests de moradia e baú passaram. O [roteiro da moradia](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md#n10-e-n14--roteiro-da-moradia) aguarda confirmação em jogo.
- **Sentinela da 0.4.126.** Unitários e propriedades passaram. Dos 1.251 GameTests, passaram 1.249 (`20261123T184500Z`). Falharam a tosquia N05 e a saída do curral entre varreduras, reproduzida também na 0.4.125.
- **Candidata local 0.4.125.** O aldeão tenta o pulo padrão antes do maior; colisão horizontal confirmada, encalhe e saída de curral liberam o pulo maior. Os 6 GameTests focados passaram. A candidata anterior de rebanho e telhado segue aguardando [playtest](docs/technical/Playtest-0.4.122-2026-11-23.md).
- **Altura das entradas (N12/N28).** O grafo preserva o piso válido da porta quando há uma rua acima na mesma coluna. Os 17 GameTests focados passaram. A mina e a casa do mineiro aguardam [confirmação em jogo](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md).
- **Jogo da 0.4.118:** [seis notas](docs/technical/Playtest-0.4.118-2026-11-22.md). A 0.4.119 trata a condução lateral de galinha e protege animais abrigados.
  A queda na contagem do curral grande e a atividade do lenhador na obra seguem em investigação. Os demais itens estão no [plano da 0.4.115](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md).
- **Verificação da 0.4.121:** 1.886 unitários e a suíte de propriedades passaram. O Sentinela `fast` (`20261123T014911Z`) encontrou três falhas nos 1.242 GameTests. Após atualizar a expectativa antiga de coleta, os cinco testes de abate e o teste de coleta do excedente passaram na rodada focada (20/21). A falha da guia também ocorre na 0.4.120. A falha no desnível lateral só apareceu na bateria completa.
- **Candidata 0.4.122 com N26.** A condução lateral passou localmente e nas duas rodadas do CI de `1a841653`. A primeira falhou na tosquia (N05). A segunda passou a tosquia e falhou na travessia entre redes após fusão de colônias. O [jogo com o JAR anterior](docs/technical/Playtest-0.4.122-2026-11-23.md) registrou três mortes de fazendeiros após resgate de telhado e a entrada do mineiro ainda solta (N28).
- **Qualidade:** nota 5,0/10 pela ISO/IEC 25010 (`docs/audit/2026-11-13-avaliacao-qualidade.md`), com a meta de uma
  1.0 pública. Seis dimensões bloqueiam a 1.0: regras de negócio, save, desempenho, compatibilidade, experiência do
  jogador e publicação.
- **Fase do cronograma:** F0, sinal limpo (documentos nos tetos, instáveis declarados, ferramentas de régua corrigidas).
  Depois vêm F1 (a vila trabalha) e o portão Beta 0.5.0.
- **Modo:** correção por playtest (`CLAUDE.md` §0.2.2).

## Candidatas que esperam jogo

Cada uma tem teste que falha sem ela; nenhuma foi vista em jogo.

| Versão | Regra | O que muda |
|---|---|---|
| 0.4.69 | 263 | o fundidor funde onde está, também com baú em casa da vila original |
| 0.4.70 | 264, 265 | o grafo nunca passa sobre a água (sem ponte); a linha planejada sai da construção erguida depois |
| 0.4.71 | 266 | uma casa do mineiro pronta basta, mesmo com a boca da mina mudada |
| 0.4.72 | 267 | a plantação grande sai em todo bioma com o fazendeiro parado |
| 0.4.73 | 268 | o bosque do lenhador acha o chão em qualquer altura |
| 0.4.74 | 269 | o prazo do pastor cresce com a distância; o animal largado fica amarrado na cerca |
| 0.4.75 | 270 | sem ofício do mod fica fora de toda análise de profissão (tempo, Regra 51, selos, relatórios) |
| 0.4.76 | 271 | a muda do bosque surge (também no deserto), em terra enraizada, declarada no `VC_SUPPLY` |
| 0.4.77 | 272 | o pastor colhe flor no ócio e faz corante; cor sem flor, falha ou demora fazem o corante surgir |
| 0.4.78 | 273 | a vila contrata no máximo um construtor (uma obra aberta por vez), também em mundo salvo |
| 0.4.79 | 274 | o construtor sem trabalho medido conta a obra aberta como trabalho na troca de cama. Sentinela `fast` da 0.4.79 verde de primeira: 1.117 GameTests; JAR instalado |
| 0.4.80 | 275–278 | do jogo da 0.4.75/0.4.79 (taiga): soleira de casa não é curral; alçapão em pé se pula e o caminho o evita; o lenhador só derruba encostado no tronco; água só sem caminho seco (custo 256). Sentinela `fast` PASS |
| 0.4.81 | 279, 280 | solto da fuga de encalhe só quem saiu do lugar; a árvore em cima da coluna do caminho sai de onde o calçador estiver (ramal da mina na mata). Sentinela `fast` PASS |
| 0.4.82 | 281, 282 | todo tronco posto pelo mod sai no log (`VC_LOG_PLACED`, com quem pôs); só a rede do grafo reserva chão contra lote (trilha fora da rede não). Sentinela `fast` PASS |
| 0.4.83 | 283 | o pastor só recolhe animal dentro da caixa da vila (sem a folga de 10 blocos); GameTests do pastor, da tosquia e da taiga sem depender dos vizinhos. 1.127/1.127 três vezes, Sentinela `fast` PASS |
| 0.4.84 | 284 | o animal atravessa o portão do curral amarrado a 2 blocos do destino (`PenTether`); GameTests do mineiro, da vaca solta, do canal de água e dos portões sem depender dos vizinhos. 10/10 baterias verdes, Sentinela `fast` PASS |
| 0.4.85 | 285, 286 | aldeão preso na água vai à margem e, se não sair, pisa no gelo sob o pulo; o mineiro novo dorme na casa junto da mina assim que ela sobe; diagnósticos `loose=` (grafo), `VC_SMOOTH` e `VC_LOT_EDGE` (terreno). Sentinela `fast` PASS; tosquia instável registrada (KF-015) |
| 0.4.86 | 287 | a tora deitada é chão do caminho (a borda da plantação grande fechava o miolo ao fazendeiro); a visão do canteiro vazio mira o lugar da planta. 1.137/1.137, Sentinela `fast` PASS |
| 0.4.88 | 289–294 | escolhas G1–G7 da ADR-056: plantação grande semeada, 2 fazendeiros e semente que surge; tronco inteiro e folha natural quebrada no caminho; grafo na tábua de rua e na frente de toda porta; sem torre de minério e sem ar protegido no alisamento; pátio de pedra na boca da mina; ociosos tiram bloco solto e toco. 1.156/1.156, Sentinela `fast` PASS |
| 0.4.113 | 346, 349–354 | relatório da sessão, travas de estoque, diagnóstico de fazendeiro e pastor, substituição da terracota, iluminação na mina e bosque |
| 0.4.112 | 348 | uma descida por arco: dois níveis sem braço, sem escada girada e sem cavar fora do desenho; pedra com solto em cima fora da escolha |
| 0.4.111 | 347 | o miolo da vila segue a forma das peças: sem chão quadrado, lago e morro entre peças afastadas ficam |
| 0.4.110 | 346 | `/vc salvar`: todas as análises da vila num arquivo só |
| 0.4.109 | 345 | conflito com o jogador: `VC_PLAYER_EDIT`, `VC_PLAYER_UNDO`, `VC_MOD_OVERRIDE` e a reafirmação `VC_PLAYER_INSIST`; `player_report.py` |
| 0.4.108 | 344 | telemetria: tarefa com número, linha do tempo de cada aldeão, balanço dos itens, falhas por item, dia do aldeão e relatório único (`game_report.py`) |
| 0.4.107 | 343 | medida das travas de cru: `VC_KEEP` diária e `stock_report.py` |
| 0.4.106 | 342 | registro de blocos: `VC_BLOCKS` diária e `/vc blocos` |
| 0.4.105 | 341 | o contratado perde o ofício e a roupa Vanilla na hora; conferência a cada ciclo; `VC_VANILLA_JOB` |
| 0.4.104 | 340 | reserva por classe de bloco (cru 256, fabricado 4, vila 12, especial 1), sem a tora guardada; bancada do mod fora da fabricação (ADR-062) |
| 0.4.103 | 339 | o fundidor solta o pedido sem cru em nenhum baú; a oficina barrada pela tora não recebe o mesmo pedido na hora; `VC_TIME_WAIT` |
| 0.4.102 | 338 | a BigHouseMOD nasce na altura das casas da vila (faixa de 8), com a busca partindo do meio delas e indo até 128 |
| 0.4.101 | 337 | na geração, a coluna da peça fixa da vila fica livre acima do piso: nada do morro solto em cima da casa que desce |
| 0.4.100 | 336 | a descida cavada: o mineiro com o alvo da descida no pé ou na rocha cava o degrau de 3 por 3 e desce por ele; piso de pedra no vão de caverna (ADR-061) |
| 0.4.99 | 335 | só a porta do térreo vira entrada do grafo; a porta solta sai no `VC_ROAD_GRAPH` com o chão lido em volta; `VC_KEEPER` no lugar do `VC_GROUND` do Zelador |
| 0.4.98 | 334 | o chão que o jogador cavou ou pôs fica: nivelamento e calçada não mexem na coluna dele (marca no save) |
| 0.4.97 | 332, 333 | ADR-060: o pasto do curral não é roça; sem tarefa de comida, o fazendeiro faz a ronda da roça e, sem nada a fazer, a vila pede a plantação grande |
| 0.4.96 | 330, 331 | ADR-059: o Zelador, dono do chão (sem bancada, nono adulto); só ele calça, devolve ao solo, nivela e tira neve, com `VC_KEEPER` diária; o grafo atravessa o feno e o Zelador o leva |
| 0.4.95 | 327–329 | jogo da 0.4.93 em planície, ADR-058: o curral é um lugar (porteira aberta não o desfaz, animal de dentro não é levado de novo); o pastor larga por 5 min o animal que não alcança; ronda diária com `VC_ROUND` e separação do misto 1× por dia |
| 0.4.94 | 326 | jogo da 0.4.93 em planície: a árvore tirada do caminho ou da obra sai com galho e tronco ligados (carvalho grande deixava tronco) |
| 0.4.93 | 315–325 | simulações de 3 dias da 0.4.92: fazendeiro semeia a terra comum; rua da vila gerada fica; `changes=` no grafo; três modos do grafo (ADR-057, `/vc grafo modo`); porta do jogador é entrada; só se calça aresta estável; terreno da vila inteira por fila (`VC_TERRAIN`); lenhador tira a folha do carvalho baixo; plantação grande arada e semeada inteira pelo molde. 1.197/1.197, Sentinela `fast` PASS |
| 0.4.92 | 313, 314 | jogo da 0.4.91: depois do degrau da porta (escada do encaixe do molde), a entrada e o patamar ficam no chão, com o solo no topo; a BigHouseMOD e a casa do mineiro nascem com caminho rente ao chão na porta. 1.187/1.187 duas vezes, Sentinela `fast` PASS |
| 0.4.91 | 305–312 | jogo da 0.4.90: a entrada da casa e um patamar de 2 em volta nascem no piso; só a aresta inteira de rosa a rosa fica fixa; telhado não é chão do grafo; a fundição para na quantidade pedida e a argila só surge para a obra; o pastor leva o animal ao curral sem prazo; o fazendeiro põe pó de osso em volta (surge sem composteira nem baú acima de 16); o pedreiro mói a areia da obra antes; o mineiro preso cava a subida rumo à borda. 1.184/1.184 duas vezes, Sentinela `fast` PASS |
| 0.4.90 | 299–304 | jogo da 0.4.89: o abate não leva o animal na corda e a corda só solta no portão; a porta sozinha se junta ao caminho e as partes soltas voltam ao rodízio (corrige a 295); a argila aparece na hora pedida; o nó da mina fica sob o arco; a frente da porta nasce no piso dela; casa da vila não fica sem solo. 1.170/1.170 duas vezes, Sentinela `fast` PASS |
| 0.4.89 | 295–298 | jogo da 0.4.88: a coluna recusada pelo calçador bloqueia a busca da ligação e a pedra dentro da colônia vira caminho; as peças da vila nascem no solo suavizado; o chão em volta da obra nova é suavizado numa janela de 12; a fundação só completa 8 adultos e a sobra fica sem ofício (Regra 134 retirada). 1.160/1.160 duas vezes, Sentinela `fast` PASS |
| 0.4.87 | 288 | na geração, a água dentro da vila vira chão com o solo seco do chunk, e a peça fixa sobre água ganha base até o piso (só em mundo novo). 1.139/1.139, Sentinela `fast` PASS |

Da 0.4.54 à 0.4.67 (Regras 243 a 261) também não houve confirmação escrita em jogo; a 0.4.68 trouxe as que o jogo de
12-11 viu funcionar: oito ofícios na fundação, casa do mineiro e mina com arco, patamar com tocha e curral achado.

## 🔴 P0 — a vila não trabalha sozinha (F1)

Nos jogos, a maior parte do expediente é espera, bloqueio ou ócio: 11% de trabalho na 0.4.43; na 0.4.68, fundidor
80% parado e fazendeiro 0%. **Aceite do Beta:** dois playtests seguidos, em dois biomas, com todo ofício abaixo de 40%
parado (espera + bloqueio + ócio + encalhe) e nenhum travado; a meta da 1.0 é a Regra 51 (≤ 15%) em três biomas.

## O que o próximo jogo precisa mostrar

Vila nova com a 0.4.100; os itens 1 a 6 vêm da 0.4.74 e seguem sem medida. Depois de jogar: `time_ledger.py`, `action_report.py`, `cost_ledger.py` e o Spark (§0.4).

| # | Item | Sinal |
|---|---|---|
| 1 | Fundidor (Regra 263) | `SMELTED` no diário dos três; `SMELT_MATERIAL` sai de `RESERVED` |
| 2 | Grafo (Regras 264 e 265) | `python scripts/save_read.py map`: nenhuma linha sobre `~`; nenhum `PAVED bridge` |
| 3 | Fazendeiro (Regra 267) | a plantação grande abre como obra; fazendeiro acima de 0% de trabalho |
| 4 | Bosque (Regra 268) | linhas `near the village at ... (grove N of M)`; lenhador andando menos de 67% |
| 5 | Pastor (Regra 269) | "into the pen"; nenhum animal largado solto |
| 6 | Poste de luz (Regra 261) | `VC_LAMP_POST` quando o construtor fica ocioso (na 0.4.68 nunca ficou) |
| 7 | Terreno (Regra 323) | `VC_TERRAIN`: `levelled` acima de 0 todo dia e `isolated` caindo |
| 8 | Lenhador (Regra 324) | `CLEARED_LEAVES` no diário; nenhum `released` do guarda de 300 tiques |
| 9 | Plantação grande (Regra 325) | a plantação entregue inteira semeada, com as quatro lavouras |

## Instáveis declarados

Medidos e não corrigidos; registro com evidência em `docs/behavioral-tests/known-failures.md`. Um vermelho só deles
na bateria não reprova a entrega, mas tem de ser dito; qualquer outro vermelho reprova.

| Teste | Taxa medida | Registro |
|---|---|---|
| `GroundPickupGameTest.workerCollectsWhileWalkingPast` | 23% das baterias inteiras (histórico local) | KF-011 |
| `PenAccessGameTest.animalsWalkInThroughEachOfTheFourGates` | 12% das baterias inteiras | KF-012 |
| `ShepherdPensGameTest.eachSpeciesGetsItsOwnPen` | 1 em 3 baterias de 13-11 (estourou 40 tiques) | KF-013 |
| `BiomeBlueprintsGameTest.theTaigaLargePenHasSpruceFencesAndTheOakOneStillStands` | 1 em 3 baterias de 13-11 | KF-014 |
| `LumberjackGameTest.theStallGuardReturnsTheTaskAndForgetsTheTree` | caiu no CI da `main` (11 a 13-11) | KF-004 |

## Aberto que não é código

- **Branches locais de outras linhas** (as sessões paralelas): cada uma fecha a própria; não apagar a de outra sessão.
- **Publicação:** declaração de uso de IA, procedência do ícone, das texturas e da `big_house_mod.nbt` (F5).

## Dívida conhecida

- Camada fabric num ciclo só, presa aos serviços estáticos de `VillageColonyMod` (259 arquivos); `fabric/work` com
  250 classes, 146 num ciclo. Não bloqueia a 1.0; está em "Depois da 1.0" na avaliação.
- NullAway só no `core`; `data/save` sem checagem de nulos é o primeiro a cobrir (F2).
- Teto de 500 linhas virou meta: 44 arquivos entre 450 e 500, 5 em exatamente 500.
- `MinerProbe`, `EnduranceReport` e `VillageInventoryObserver` estão em `src/main` só como sonda dos GameTests; mover
  para o conjunto de teste exige separar os unitários que os usam.
- `runGametest` não filtra por teste; use `scripts/gametest_focus.py` e restaure antes de commitar.

## Como avaliar e investigar

- **Avaliação de qualidade:** `docs/audit/2026-11-13-avaliacao-qualidade.md` e os anexos em
  `docs/audit/avaliacao-2026-11-13/`; refaça as notas a cada portão.
- **Sentinela:** `python scripts/test_lab.py fast`, depois `python scripts/sentinel_analysis.py`, com o Minecraft
  fechado; `python scripts/rule_coverage.py` quando mudar regra ou teste. Não rode `./gradlew.bat --stop` com outra
  sessão usando o Gradle.
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse; ler o save quando o log não
  basta (`scripts/save_read.py graph` e `map`).

| Ferramenta | Para quê |
|---|---|
| `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
| `scripts/spark_decode.py` | TPS, picos e métodos do mod num perfil do Spark |
| `scripts/save_read.py` | grafo de ruas e blocos do save, sem abrir o jogo |
| `scripts/analyze_village_log.py`, `scripts/verdict.py` | assinaturas no log; veredito por item pendente |
