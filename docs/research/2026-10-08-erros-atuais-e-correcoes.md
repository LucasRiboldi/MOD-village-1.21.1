# Erros atuais, causas e correções (2026-10-08)

Fontes: playtests de 07-10 e 08-10 (`docs/reports/Playtest-2026-10-08.md`), `time_ledger.py`,
`activity_report.py`, `action_report.py`, Spark `bpHFsttvlp` e `iX0dliasLV` (mod = 3,2% da thread),
os dois crash reports de 07-10 22:32 e 08-10 09:13, leitura do save e do código. As **decisões do
autor** de 08-10 estão em cada item; o que foi feito tem ✅ e o commit.

**Princípio pedido pelo autor:** a profissão está sempre fazendo uma atividade; se não dá, faz
outra, ou busca uma resolução alternativa para a primeira — nunca fica girando no mesmo alvo.

## Já corrigido (branch `claude/obra-lote-e-trava`)

| | Defeito | Correção |
|---|---|---|
| ✅ | Roça relida com trigo e camada da rua: trava de fundação recusava, obra "AVAILABLE with nobody" 37 min | `FarmBlueprint.asBuilt` |
| ✅ | Recusa da trava de reserva muda no log | `BuildSiteGate` + `refused: <motivo>` |
| ✅ | Obra abandonada sem base guardava o lote para sempre | reparo e retomada soltam o lote |
| ✅ | Funil: meta de ferro só via lampião | `WorkMaterials.iron` genérico, `RAW_IRON` em `constructionMaterials` |
| ✅ | Obra inteira parada por uma peça sem material | `SkipReason.WAITING_MATERIAL` |
| ✅ | Nome da profissão invisível no painel | escala da plaquinha Vanilla + mixin de cliente |
| ✅ | **Crash "Colony already has an open project"** (07-10 e 08-10) | `2132c67d` — ver C1 |
| ✅ | E1 mineiro preso a pedra sem posição de trabalho | `1b7c2601` |
| ✅ | E2, E3, E5, E6, E7, M1 | ver cada item |

## C1. Crash ao retomar obra do save — ✅ `2132c67d`

- **Os dois crashes são o mesmo:** `IllegalStateException: Colony already has an open project` em
  `ConstructionService.register` ← `ConstructionResume.resume`.
- **Causa:** o save trazia "1 projects to resume" e "1 paused sweeps". A passagem extra da busca de
  lote (`SweepCadence`, uma por segundo com jogador na vila) conferia só obra **aberta**, não a
  **pendente** do save; continuou a varredura e abriu o celeiro (09:11:51). No ciclo do planejador
  seguinte, a retomada registrou a obra salva por cima e o servidor caiu (09:13:45).
- **Correção:** a obra pendente ocupa a vaga única em `SweepCadence` e `SiteOpening`; a retomada
  com obra já aberta larga a salva com aviso; e uma exceção no ciclo de uma colônia perde o ciclo
  dela, nunca o mundo (`ColonyCycleRunner`, log ERROR com a pilha).
- **Prova:** `FoundationRepairGameTest.aSavedProjectNeverOpensOnTopOfAnOpenOne`. A primeira versão
  sobreviveu à mutação (obra salva sem base era largada antes do registro) — o cenário agora põe
  um bloco da planta de pé. Mutação: caiu com a mensagem do crash.

## 🔴 Os que travavam profissão

### E1. Mineiro preso a pedra sem posição de trabalho — ✅ `1b7c2601`
- **Decisão do autor:** reformular para *"alvo alcançável + posição de trabalho válida"* — um bloco
  só vira alvo se existir posição de trabalho válida e alcançável para o mineiro.
- **Causa medida no save:** a pedra -751,31,-942 estava exposta, mas uma caverna abriu embaixo da
  galeria; o único lugar de pé ao alcance do braço ficava em y34, quatro acima do mineiro (y30). O
  cursor da galeria pulava posições recusadas para a seguinte, ainda mais dentro da rocha.
- **Correção:** `MiningTarget` — veredito com a posição de trabalho ou o motivo (`NO_STANDING_POSITION`,
  `TOO_HIGH`, `NO_PATH`). Geometria e busca a pé local, determinística e com teto; **não** o
  `findPathTo` (a lição do `TreeMarks`: a navegação recusou árvore boa). Teto estourado = "não sei",
  e "não sei" nunca recusa pedra. Conferido ao escolher e uma vez ao chegar a 12 blocos;
  `MinerHands.passOver` larga a pedra **sem** contar desistência, com marca temporária
  (`MineMarks`, já esquece sozinha) e linha `TARGET_REJECTED` no diário.
- **Prova:** `MiningTargetGameTest` (pilar com lugar de pé 2 acima → `TOO_HIGH`; controle com chão).
- **Próximo passo (resolução alternativa, não feito):** quando a recusa é por **chão que falta**, pôr
  um bloco de pedregulho para criar o lugar de pé — o que o mineiro do MineColonies faz com caverna,
  água e lava ("they might need lots of cobblestone to fill in caves"). O mod já põe bloco no desvio
  (`MinerDetours`); fica para depois do playtest medir quantas recusas são desse tipo.

### E2. Segundo mineiro sem alvo, relatório errado — ✅
- **Decisão do autor:** manter, com **reserva explícita de ramal**.
- **O que já existia:** `MineClaims` já reserva ramal com dono (um mineiro por ramal); o defeito era
  o relatório. Enquanto a escada é um ramal só (`Mine.branchesOpenNow() == 1`), a linha dizia
  "1 of 4 taken", como se houvesse três livres.
- **Correção:** `MineBranch` (núcleo) dá o estado de cada ramal — `NOT_OPEN_YET`, `OPEN`,
  `RESERVED`, `EXHAUSTED` —, `MineClaims.branches` o diz numa frase ("branches 0 reserved by
  cc99a53b, 1-3 not open yet") e `MinerReport` conta só os ramais abertos agora.
- **Prova:** `MineBranchTest` (unidade).
- **Visto no log e não corrigido:** o segundo mineiro, na superfície em y65 bem em cima da mina,
  não achou caminho até a boca em y38 (numa caverna) — "found no detour within 16 blocks". É o caso
  "mina existe, mas a entrada não conecta à vila", que o M2 (`VC_REACH`) deve medir.

### E3. Varredura do fazendeiro sem fim — ✅
- **Decisão do autor:** cursor incremental + cache de parcelas.
- **Causa:** a memória de roça (`CropPatch.KNOWN`, A-8) só pulava a varredura quando achava algo
  **maduro**; com a roça toda plantada e nada maduro — o caso comum — o raio 32 inteiro (~4.225
  colunas, 5 passagens) era revarrido toda vez: "still sweeping — the budget ran out".
- **Correção:** a roça conhecida responde sozinha, com cursor de 512 células por passagem: maduro
  encerra na hora; no fim da volta vale o canteiro vazio, ou "nada" com a volta completa (e o
  descanso do `FieldRest`). O raio só é varrido quando não há roça conhecida ou quando a expansão
  periódica vence (2 min), para achar roça nova — "cache + cursor + expansão ocasional".
- **Prova:** `CropPatchKnownFieldGameTest` (raio 16, roça plantada: a segunda pergunta responde
  sem recomeçar a varredura; controle: o trigo maduro é achado na roça conhecida).

## 🟠 Os que enganavam o diagnóstico

### E5. Chat `[OBRA] … em resolução: uma profissão consegue no bioma; falta entregar` — ✅
- **Decisão do autor:** implementar.
- **Correção:** `ProductionChain` desce a cadeia da peça (fornalha para o que é fundido, bancada
  para o que é fabricado, nunca a forma guardada) e diz cada elo: no baú, tarefa aberta, sem tarefa,
  bancada, ninguém obtém. O `/vc log` ganha a linha `[CADEIA] hopper (bancada) ← iron_ingot
  (fundidor: sem tarefa) ← raw_iron (mineiro: tarefa aberta) — falta: iron_ingot`. A frase antiga
  virou "há rota no bioma e ainda não chegou ao baú — a cadeia diz qual elo falta".
- **Prova:** `ProductionChainGameTest`.
- **Para o futuro (sugestão do 1.txt, não feita):** estados por ligação da cadeia (`REQUESTED`,
  `ASSIGNED`, `IN_PROGRESS`, `BLOCKED`, `NO_WORKER`, `NO_PATH`, `NO_STOCK`) e a cadeia no painel
  da obra (M5).

### E6. `could not make iron_ingot — needs iron_block` — ✅
- **Decisão do autor:** manter o `sameFamily`.
- **Causa:** o fabricante descia a receita do funil, faltava lingote, e tentava o lingote pela
  receita "bloco de ferro → 9 lingotes" — que pede fazer o bloco, que pede 9 lingotes. Mesmo ruído
  com "wheat — needs hay_block". O `sameFamily` (última palavra do nome) não pega lingote/bloco.
- **Correção:** a mesma regra, generalizada: `CraftingLookup.isStorageForm` — compactar e
  descompactar armazenamento não é rota de produção (o ingrediente sai na bancada só do item).
  Vale para **fazer** o que falta (`ColonySupply.makeWhatIsMissing`) e para o motivo
  (`CraftReasons`, que agora diz "it comes from the furnace — the smelter owns it"); o que já está
  guardado no baú continua podendo ser desfeito.
- **Prova:** `StorageFormRecipeGameTest` (lingote/bloco, trigo/fardo; controle: funil e tábua).

### E7. Pastor sem ovelha tosquiável — ✅
- **Decisão do autor:** só fazer sob condição de **déficit real** do rebanho.
- **Correção:** quando a busca por ovelha com lã volta vazia (`EmptyFlock`), `ShepherdFlock.breedForWool`
  põe um par de ovelhas para procriar **na hora**, fora do relógio de 6.000 tiques — mas só abaixo
  do teto de 12 adultas. Com o rebanho cheio, nada se cria: a lã volta pelo pasto, e a terceira
  busca vazia ainda estoca a lã (`LocateFallback`), como antes.
- **Prova:** `ShepherdWoolDeficitGameTest` (2 tosquiadas → procriam; 12 tosquiadas → não).
- **Visto no 1.txt e não feito:** "ovelha existe mas não está acessível → resolver alcance" — depende
  do M2.

## 🟡 Melhorias

### M1. Diário de ações (`VC_ACTION`, JSONL) — peça central do diagnóstico ✅
- **Decisão do autor:** tornar peça central do diagnóstico.
- Já implementado em 0.3.11. Nesta rodada: recusa de alvo com motivo (`TARGET_REJECTED`) e o
  `action_report.py` passou a listar **os motivos de recusa e desistência por profissão** (números
  dentro da frase não separam o motivo). Regra do autor: toda análise de Spark mostra também o
  relatório do `action_report.py`.

### Para esperar — com a informação para o futuro

| # | Decisão do autor | O que fica registrado |
|---|---|---|
| M2 | **`VC_REACH` antes de criar o nivelador** | BFS do sino com degrau ≤ 1 (plano em `2026-10-08-nivelamento-do-solo-da-vila.md`, fase 1). É **diagnóstico global**; a navegação Vanilla continua resolvendo a rota individual. Deve responder: roça isolada, mina sem entrada ligada à vila (o E2 de 08-10: boca em y38 numa caverna, mineiro em y65), baú que ninguém alcança, obra que o construtor não alcança, estação de trabalho fora da rede. |
| M3 | **Não criar profissão só por ociosidade** | 9 desempregados não provam falta de profissão: pode ser falta de demanda, demanda bloqueada, capacidade maior que o consumo, ou espera do ciclo. Só criar o nivelador com dados de demanda recorrente de manutenção do terreno. Alternativa a investigar antes: um carregador (logística entre baús), que ataca vários gargalos de uma vez. |
| M4 | **Investigar, não otimizar o mod ainda** | O pico de 420 ms do Spark não é do mod (3,2% da thread). Correlacionar com autosave, geração de chunk, GC e outro mod; só mexer se o ciclo do mod coincidir com o pico (`cost_ledger.py`). |
| M5 | **Depois do E5** | A cadeia no painel da obra e o motivo do mineiro no painel dele ("procurando alvo: 31 candidatos, 12 NO_PATH, 8 TOO_HIGH"). O overlay não pode reconstruir o que o sistema ainda não representa. |

## Avaliação do arquivo do autor (1.txt)

Útil, e entrou nas decisões acima. O que ele acrescenta de verdade:

- **E1 por elegibilidade física, e não "não escolher tooHigh"** — virou o `MiningTarget`, com
  motivos nomeados. *Ajuste nosso:* a sugestão de perguntar à navegação ("mineiro consegue chegar
  nela?") foi trocada por geometria + busca local, porque o projeto já mediu a navegação errando
  (`TreeMarks`).
- **Marca temporária, não lista negra** — já era assim (`MineMarks` e `TreeMarks` esquecem sozinhas,
  com prazo que cresce); a assinatura do mundo (`worldSignature`) não foi feita: o prazo já cobre o
  terreno que muda.
- **Reserva de ramal com dono** — já existia em `MineClaims`; faltava o estado visível
  (`MineBranch`).
- **E3 cache + cursor + expansão** — feito como descrito.
- **E5 cadeia com estado por elo** — feito na forma de leitura (`ProductionChain`); estados de
  ligação ficam para o M5.
- **E7 condicionado ao rebanho** — feito; "resolver alcance" depende do M2.
- **Princípio Existência ≠ Elegibilidade ≠ Executabilidade** e o contrato
  *Candidate → Eligibility → Executability → Assignment → Execution → Outcome → Recovery* — bom
  candidato a ADR (as oito profissões pelo mesmo ciclo; o `TODO.md` já registra o ciclo de tarefa
  comum aos ofícios). **Não escrito ainda:** é decisão de arquitetura do autor.

## Pesquisa em outros projetos

- [MineColonies — mineiro](https://minecolonies.com/wiki/workers/miner/) e
  [cabana do mineiro](https://minecolonies.com/wiki/buildings/miner): poço vertical com plataforma
  a cada 3 blocos, galerias em "nós" a partir do poço; **tapa caverna, água, lava e areia com
  pedregulho** e contorna o líquido — a resolução alternativa do E1.
- [MineColonies — fazendeiro](https://minecolonies.com/wiki/workers/farmer) e
  [cabana](https://minecolonies.com/wiki/buildings/farmer/): o campo é um bloco posto pelo jogador
  (a roça é **conhecida**, nunca procurada) e há uma ação por campo por dia — o mesmo princípio do
  E3: trabalhar sobre a roça conhecida, não varrer o raio.
- [MineColonies — pastor](https://minecolonies.com/wiki/workers/shepherd) e
  [cabana](https://minecolonies.com/wiki/buildings/shepherd): rebanho com teto pelo nível da cabana,
  procriar e tosquiar são chaves separadas — o teto de 12 do E7.
- [MineColonies (GitHub)](https://github.com/Minecolonies/minecolonies): código-fonte das IAs, para
  quando o E1 alternativo (pôr chão) for implementado.

## Ordem sugerida a partir daqui

Playtest da 0.3.12 com `time_ledger.py`, `action_report.py` (motivos) e `/vc log` (cadeia) →
E1 alternativo (pôr chão onde falta), se os `TARGET_REJECTED` forem muitos → M2 (só medir) →
ADR do ciclo comum de tarefa, se o autor quiser.
