# TODO

**Atualizado:** 2026-10-10. Só o que está **aberto**. O histórico até hoje está em
`docs/archive/technical/TODO-ate-2026-10-07.md` (consultar por `grep`, nunca inteiro).
Estado vivo: `STATE.md`. Decisões: `docs/decisions/` (a última é a ADR-039).

Prioridade: 🔴 bloqueia · 🟠 importante · 🟡 melhoria · 🟢 futuro.

## 🔴 Auditoria técnica 10-10 (fila AUD, P0→🔴 P1→🟠 P2→🟡 P3→🟢)

Detalhe, evidência e teste de aceite: `docs/technical/avaliacao/ACTION-QUEUE.md`.
Ordem de execução: `docs/technical/Roteiro-Correcoes-2026-10-10.md`.
Scripts: `scripts/audit/check-*.sh`.

- [ ] 🔴 **AUD-01** Regra 51 reprovada nas duas sessões de 10-10 (0.3.23: SMELTER 91%, BUILDER 92%, MINER 17%;
      fundidor 311/311 soltas). Raiz: mineiro sem boca de mina (`no mine mouth`, 50×). Roteiro: `docs/technical/Roteiro-Correcoes-2026-10-10.md`.
- [ ] 🟠 **AUD-02** Save malformado apaga todas as colônias (o Vanilla cria estado vazio e o grava):
      `try/catch` por entrada em `ColonySaveReader`; promover `scripts/audit/probes/NbtFuzzProbeTest`.
- [ ] 🟠 **AUD-03** Gravar `saveVersion` no `ColonySavedData.writeNbt` (hoje a v0→v1 roda a cada carga).
- [ ] 🟠 **AUD-05** Integrar a `codex/fluxo-grafo-waypoints` (decidido, ADR-041 #2): 14 commits, ADR-040,
      despacho físico e retenção do fundidor fora da `main`; a PR #19 foi fechada sem trazê-los.
- [ ] 🟠 **AUD-06** Asserções em `WorkDemandTest` para `constructionMaterials` (10 mutantes sobreviventes; PIT com folga de 13).
- [ ] 🟡 **AUD-07** Régua de avaliação: C14 cego às seções 🔴, C01 conta uma linha a mais, quebra em cp1252 (régua v2).
- [ ] 🟡 **AUD-08** `STATE.md` com 663 linhas (teto 150): arquivar seções fechadas de 02-10 a 08-10.
- [ ] 🟡 **AUD-09** Ciclo raiz ↔ `fabric.*` por `VillageColonyMod` como localizador global.
- [ ] 🟡 **AUD-10** 181 estáticos mutáveis; config estática não-final (`MineLease`, `TreeChoice`, `MinerDetours`, `FarmerWork`).
- [ ] 🟡 **AUD-11** Testes de `ProfessionPolicyNetworking`/codec/SavedData; cobertura de `fabric.network` e `fabric.command`.
- [ ] 🟡 **AUD-12** Avisar quando outro mod tirar a tarefa da colônia do `Brain`; `suggests: modmenu` e `breaks` no `fabric.mod.json`.
- [ ] 🟡 **AUD-04** Ciclo p95 67 ms na 0.3.22, 27,7 ms na 0.3.23: medir de novo (≥ 20 amostras) antes de otimizar.
- [ ] 🟡 **AUD-25** Diário de ações sem o motivo de `TASK_RELEASED` (grava só o estado).
- [ ] 🟡 **AUD-13** Alternância de obras: o teste passa; atualizar `docs/behavioral-tests/README.md` e provar em jogo.
- [ ] 🟢 **AUD-14** CHANGELOG sem entradas de 0.3.1 a 0.3.23.
- [ ] 🟢 **AUD-15** 848 imports não usados (`RemoveUnusedImports` no Error Prone).
- [ ] 🟢 **AUD-16** Cache de `CraftingLookup.listAllOfType`, se o Spark mostrar.
- [x] 🟢 **AUD-17** `/vc log` continua aberto (ADR-041 #4).
- [ ] 🟢 **AUD-18** Tag por JAR publicado (0.3.4–0.3.23 sem tag).
- [ ] 🟡 **AUD-19** `/vc log` com dados amplos (ADR-041 #4): janela do `WorkTime` por ofício e a marca da regra do ócio.
- [ ] 🟢 **AUD-26** **Escolha do autor:** número da regra do ócio (colide com a Regra 51 do `docs/RULES.md`).
- [ ] 🟢 **AUD-20** Dividir as 15 classes em 488–500 linhas antes da próxima mudança nelas.
- [ ] 🟢 **AUD-21** `check-tests-green.sh --runs 8` antes de declarar a intermitência curada.
- [ ] 🟢 **AUD-22** Semente em `BigHouseFoundation.java:79` (`Random.create()`).
- [ ] 🟢 **AUD-23** Javadoc "o único mixin do mod" em `VillagerEntityMixin.java:14`.
- [ ] 🟢 **AUD-24** **Escolha do autor:** apagar as 7 branches já contidas na `main`.

## 🟡 Playtest do patch local 10-10 (obra/mina/baú)

- [x] Executar a bateria completa de GameTests da 0.3.23 (pulada no fechamento por instrução do autor) —
      **690/690** em 3 rodadas `--rerun-tasks`, mais 1 com Fabric API 0.116.17 e Loader 0.19.5 (auditoria 10-10),
      e 690/690 no CI de `b290ed10`.
- [ ] Obra em `WAITING_RESOURCES` não libera o lote antes de retentar blocos correspondentes,
      rotas de profissão e baú próximo do construtor.
- [ ] Depois de quatro tentativas sem entrega real, o bloco faltante aparece em baú livre perto da obra
      e o construtor retoma com esse item.
- [ ] Mineiro em mina aberta usa a boca/galeria mais próxima sem roubar a busca do dono do ramal.
- [ ] Recurso novo não recolhido antes entra na rota de busca quando aparece como peça adiada da obra.

## 🟡 Playtest da 0.3.21 (Regra 52: roça por fazendeiro)

- [ ] Com dois fazendeiros, a vila mantém ou constrói duas roças; com uma roça só, abre outra antes
      da próxima oficina não prioritária.
- [ ] Roça Vanilla já existente conta como roça da colônia e recebe dono.
- [ ] Cada fazendeiro prioriza a própria roça: colhe trigo maduro ou semeia canteiro vazio antes de
      varrer a vila inteira.
- [ ] Fechar e abrir o mundo preserva a posse da roça (`WorkMemory`).

## 🔴 Playtest da 0.3.23 / 0.3.20 (celeiro, pastor, funil)

- [ ] Próxima carga no JAR 0.3.23 deve descartar pendência salva de `colony/barn_majest` e não abrir nova obra desse modelo.
- [ ] `clay_ball` aparece no baú do SMELTER em lote mínimo de receita (4), mesmo com vários fundidores.
- [ ] `time_ledger.py`: SMELTER sem passar de 15% ocioso nas janelas da Regra 51.
- [ ] Pastor tosquia no curral: `sheared` frequente e menos `found no sheep with wool`; SHEPHERD fora da Regra 51.
- [ ] `stocked minecraft:hopper` depois das faltas, e a obra termina.

## 🟠 Abertos no playtest de 08-10 noite

- [ ] Mina que acha ferro: a escada não cruza minério (0 de 21 em 37 min; 977 minérios na área). Decisão de desenho.
- [ ] Mineiro cc99a53b volta à pedra −751 31 −942 (lugar de pé fora da galeria) — item 2 do relatório da manhã.
- [ ] Lenhador que não sai do lugar marca a árvore como inalcançável (três lenhadores, uma árvore, 34 min).

## 🔴 Playtest da 0.3.19 (Regra 51)

- [ ] `time_ledger.py` sem `REGRA 51` no lenhador (pernas + bosque de 6) e no construtor (calçamento).
- [x] `Builder … paved the path at …` com a obra esperando; `PAVED` no `action_report` (64, playtest 08-10 noite).
- [ ] **Escolha do autor:** A1, A2, A3, A6, A9, A10, B1–B6 (`docs/research/2026-10-08-tempo-ocioso.md`).

## 🔴 Playtest da 0.3.17

- [ ] Mineiro chega ao nível da pedra (y 38) e `MINED` > 0; ferro chega ao fundidor e ao lampião.
- [x] Lampião/corrente no baú da obra depois das faltas (08-10 noite, 18:21–18:23).
- [ ] `Shepherd … leaves the build after N blocks` com N ≥ 10.
- [ ] `GroundPickupGameTest` cai só nas rodadas de mutação — investigar.

## 🔴 Playtest da 0.3.16

- [ ] Fardo de feno no baú do construtor depois de três faltas (`stocked minecraft:hay_block`).
- [ ] `planted … near the village at … (grove k of 3)` três vezes, fora da vila e longe das ruas.
- [ ] `Shepherd … lends a hand at the build` e `leaves the build — the flock needs shearing`;
      `time_ledger` com o pastor menos ocioso; a obra pondo mais blocos por minuto.

## 🔴 Playtest da 0.3.15

- [ ] Mineiro desce até o nível da pedra (linhas `miners:` com y caindo 68 → 58 → 48 → 38) e MINED > 0.

## 🔴 Playtest da 0.3.14

- [ ] Mineiro na superfície: `walking to` a boca de superfície (-775, 68, -922 no "Novo mundo"),
      depois as entradas dos níveis; linha `the surface entrance is remembered at …` uma vez.
- [ ] Lenhador: as árvores cortadas começam pelo centro da vila; `time_ledger` com `blocked` abaixo de 49%.
- [ ] `VC_REACH` no log e `[ALCANCE]` no `/vc log`; conferir se os lugares "fora" são mesmo inalcançáveis.
- [ ] `/vc log` sem "Obra: já há uma aberta" e sem argila "ainda procurando" depois de estocada.

## 🔴 Playtest da 0.3.13 (`docs/reports/Playtest-2026-10-08-tarde.md`)

- [ ] `action_report.py`: SMELTER sem roda (pegas ≈ feitas); sem minério, "no smelter work".
- [ ] Mineiro sem `pauses — … chest … is full` enquanto o baú tem espaço; conclusão acima de 0%.
- [ ] Mineiro sem ficar parado no próprio lugar "walking to" a posição em que está.
- [ ] `CRAFTED` com o item no diário.

## 🟠 Vistos no playtest de 08-10 tarde e não corrigidos

- [ ] Bateria: a detecção de vila funda colônia com aldeões de outro GameTest (tarefa aberta).

## 🔴 Playtest da 0.3.12 (crash da retomada e E1–E7 — `docs/research/2026-10-08-erros-atuais-e-correcoes.md`)

- [ ] Carregar o save "Novo mundo" com obra pendente e jogador na vila: sem crash; se aparecer
      `drops the saved … another project is already open`, uma vez só.
- [ ] Mineiro: linhas `passes over the stone … (TOO_HIGH|NO_PATH|NO_STANDING_POSITION)` em vez de
      400 tiques andando; `action_report.py` mostra `TARGET_REJECTED` por motivo; conclusão do
      mineiro acima dos 24% da bateria.
- [ ] Relatório do segundo mineiro: `branches 0 reserved by …, 1-3 not open yet` enquanto a escada abre.
- [ ] Fazendeiro sem `still sweeping — the budget ran out` com a roça plantada; `wait` abaixo de 60%.
- [ ] `/vc log` com obra esperando: linha `[CADEIA] … — falta: …` nomeando o elo certo.
- [ ] Sem `could not make iron_ingot — needs iron_block` nem `wheat — needs hay_block`.
- [ ] Pastor: `no wool and the flock is full` com 12 ovelhas; par de ovelhas posto com menos.
- [ ] Em jogo: obra com funil faz o mineiro pedir `raw_iron`; overlay com ícones lisos e painel da
      obra sobre a placa (pendentes da 0.3.9–0.3.11).

## 🟠 Abertos depois da 0.3.12

- [ ] **E1 alternativo:** quando a recusa é por chão que falta (caverna sob a galeria), pôr
      pedregulho para criar o lugar de pé (MineColonies tapa caverna/água/lava). Decidir pelo número
      de `TARGET_REJECTED` do playtest.
- [ ] **Mina com entrada que não liga à vila** (08-10: boca em y38 numa caverna, mineiro em y65 sem
      caminho) — medir com o M2.
- [ ] **ADR do ciclo comum de tarefa** (Candidate → Eligibility → Executability → Assignment →
      Execution → Outcome → Recovery), sugerido no arquivo do autor — decisão do autor.

## 🟢 Esperam, com a informação no doc de erros (decisões do autor, 08-10)

- [x] M2 fase 1 (`ReachMap`, `VC_REACH`) — 0.3.14; as fases 2–4 do nivelador esperam o número dele.
- [ ] M3 não criar profissão só por ociosidade; investigar carregador antes.
- [ ] M4 pico de 420 ms: correlacionar, não otimizar.
- [ ] M5 cadeia e motivos no overlay, depois do E5.

## 🔴 Playtest da obra destravada (08-10)

Playtest de 07-10 noite: obra "AVAILABLE with nobody" 37 min, construtor 100% ocioso. Corrigido na
branch `claude/obra-lote-e-trava` (ver `STATE.md`).

- [ ] A roça retomada do save é reservada e sobe (o log não pede mais `wheat` ao carpinteiro).
- [ ] Nenhuma linha `builders: … AVAILABLE with nobody` por mais de um ciclo sem `refused:` ao lado;
      se aparecer `refused:`, o motivo dela decide o próximo passo.
- [ ] No save "Novo mundo": `frees the lot of the abandoned plains_small_house_2` ou
      `drops the saved …` aparece uma vez e a vila planeja obra nova.
- [ ] `time_ledger.py`: BUILDER com `work` > 0.

## 🔴 Playtest da 0.3.5

Nada das ADR-037, 038 e 039 foi visto em jogo. Rodar depois: `python scripts/time_ledger.py`
e `python scripts/cost_ledger.py` no `latest.log`, e Spark.

- [ ] `walk` aparece no `time_ledger` (A3: o braço do aldeão agora desce).
- [ ] Carpinteiro com escada/laje/cerca **da madeira do bioma** no baú dele; pedreiro com as peças
      das casas da vila; fundidor com vidro e a pedra lisa que as casas usam.
- [ ] Peça feita entra no **baú da profissão** de quem fez, não no do vizinho.
- [ ] Mineiro encalhado **continua mineiro**; ramal novo abre para o minério que falta.
- [ ] Caminho calçado até obra afastada da rua.
- [ ] Fechar e abrir o mundo: lenhador vai direto à árvore conhecida; mineiro volta ao ramal e ao veio;
      animal na corda continua sendo levado à cerca.
- [ ] Pastor trazendo animal solto até a cerca perto da cama; ovelha solta também procria.

## 🟠 Erros abertos

- [ ] **KF-002** (aldeão de coleta some no tique 1): não reapareceu em ~27 baterias de 07-10.
      Correção recomendada: montar o cenário dentro da própria arena, com o raio protegido encurtado
      por gancho de teste (como `MineDigging.shortenSurfaceRadiusTo`), em vez de 65–97 blocos fora.
- [ ] **KF-003**: causa achada e corrigida (fusão de colônias no tique de teste). Fecha com
      `python scripts/gametest_battery.py 30` limpo; até agora 7 + 6 limpas.
- [ ] **`lumber_stall_guard`** caiu 1 vez em ~6 baterias de 07-10 (tarde), sem relação com a mudança
      da hora. Medir com o script antes de investigar.

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que
passa com a regra desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da
borda (ela já fez um teste passar sem medir nada, o D2).

Plano de 07-10 aplicado: D2, B1, B3, B4, B5, B6 e o arquivo do save provados por mutação (ADR-039).
Falta só fechar e abrir um mundo de verdade (playtest).

## 🟡 Bioma (regra do autor: toda profissão se adequa ao bioma)

- [x] Carpinteiro, pedreiro e fundidor: peças das casas do bioma (`BiomePieces`).
- [x] Lenhador: espécies do bioma (floresta da vila, ADR-037); mineiro e coleta: o que o chão dá.
- [ ] Fazendeiro e pastor: hoje iguais em todo bioma — levantar o que a vila Vanilla de cada bioma cultiva e cria.

## 🟡 Estrutura

- [ ] Seis arquivos de produção em 497–500 linhas (`SmelterWork`, `Mine`, `ClimbOut`,
      `SurfaceGatheringWork`, `Worker`, `BuilderWork`): dividir antes da próxima mudança neles.
- [ ] Branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`): já toda na
      `main`; remover o worktree.

## Decisões do autor que faltam

- [ ] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes?
- [ ] Dois construtores na mesma obra.
- [ ] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33.
- [ ] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24).
- [ ] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
- Peça sem rota: depois de 4 tentativas aparece no baú de quem precisa (`BiomeConstructionSupply`,
  já em vigor, testado em `LocateFallbackGameTest`).
