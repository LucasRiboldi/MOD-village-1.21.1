# TODO

**Atualizado:** 2026-10-08. Só o que está **aberto**. O histórico até hoje está em
`docs/archive/technical/TODO-ate-2026-10-07.md` (consultar por `grep`, nunca inteiro).
Estado vivo: `STATE.md`. Decisões: `docs/decisions/` (a última é a ADR-039).

Prioridade: 🔴 bloqueia · 🟠 importante · 🟡 melhoria · 🟢 futuro.

## 🔴 Playtest da 0.3.13 (`docs/reports/Playtest-2026-10-08-tarde.md`)

- [ ] `action_report.py`: SMELTER sem roda (pegas ≈ feitas); sem minério, "no smelter work".
- [ ] Mineiro sem `pauses — … chest … is full` enquanto o baú tem espaço; conclusão acima de 0%.
- [ ] Mineiro sem ficar parado no próprio lugar "walking to" a posição em que está.
- [ ] `CRAFTED` com o item no diário.

## 🟠 Vistos no playtest de 08-10 tarde e não corrigidos

- [ ] Lenhador 49% "blocked": árvores a 58–134 blocos; a navegação para e recomeça. Viveiro perto
      da vila ou teto de distância.
- [ ] `/vc log` mostra "smelter surface: ainda procurando (clay_ball)" depois de a argila ser estocada.
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

- [ ] M2 `VC_REACH` (BFS do sino, degrau ≤ 1) **antes** de criar o nivelador.
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
