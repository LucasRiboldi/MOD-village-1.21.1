# Handoff — Village Colony

Ponto de retomada para outro agente (Codex) a partir do GitHub. Fotografia de 2026-11-07, feita pelo Claude Code: reconfira com `git log` e `gh pr list` antes de trabalhar. O handoff anterior, da Fase 3 do MAE, está no histórico do Git.

## Onde o projeto está

- **Versão:** 0.4.43, publicada como pré-lançamento [`v0.4.43-alpha`](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases/tag/v0.4.43-alpha). Branch de trabalho: `main`.
- **Modo:** correção por playtest (`CLAUDE.md` §0.2.2): log primeiro, opções ao autor, ele escolhe, teste que falha, correção, Sentinela, commit, JAR.
- **Última tarefa:** o autor jogou a 0.4.43 e pediu para anotar a análise, para avaliar e corrigir depois. Nada dela foi corrigido.

## O próximo passo

**Não comece corrigindo.** O autor ainda vai escolher. Leia [`docs/audit/2026-11-07-jogo-0443.md`](docs/audit/2026-11-07-jogo-0443.md) e pergunte qual destas opções ele quer:

1. Branco e socorro, recomendada: socorro só depois de todos os tipos de obra falharem; quina em cima da rua perde o branco (defeito de `RoadGraph.hasLineNear`); `plains_accessory_1` vira enfeite.
2. Investigar as 230 colunas de rua cobertas por grama, só diagnóstico.
3. Diagnosticar falta de madeira e nenhum nascimento em 63 minutos.

Em aberto com o autor: a obra do socorro foi parada 17 s depois de pedida. Foi ele? O que viu no lugar?

## Ordem de leitura

1. Este arquivo.
2. `AGENTS.md`, inteiro no topo; `CLAUDE.md` §0 a §2.1. `CLAUDE.md` vence em conflito.
3. `STATE.md`, só as primeiras 60 linhas.
4. `docs/audit/2026-11-07-jogo-0443.md`, e o topo do `TODO.md` mais as seções "Jogo da 0.4.43" e "MAE: o que falta".
5. `docs/RULES.md`: procure a regra por número. As mais recentes são 222 (socorro) e 223 (branco só onde se entra).

## Regras do autor que não estão nos outros documentos

Elas viviam só na memória local do Claude Code. O que está em `CLAUDE.md` e `AGENTS.md` continua valendo.

- **Idioma:** sempre português do Brasil. Ao perguntar ou oferecer opções, frases completas, de modo leigo. Ele quer opções para decidir: 2 ou 3, com efeito e risco, e a recomendada primeiro.
- **"commit push":** commite, envie e responda só `commit push realizado`. Se algo falhar, não diga a frase: diga o que houve.
- **"relatorio commit":** ciclo completo: estado atual, revisão, testes de verdade, README, `TODO.md` por prioridade, higiene, commit, push e relatório `✅ CICLO ENCERRADO`. Separe "tem teste" de "foi verificado rodando de verdade".
- **Merge na `main`:** autorizado sem perguntar quando é o próximo passo, com CI verde e sem conflito (`gh pr merge N --merge`). **Release e tag exigem pedido.** Nunca `--force`.
- **Sistema velho:** autor em 2026-11-06: "ignora a antiga, esquece o que é velho e passa sempre a usar o sistema mais atual".
- **Não marque como feito o que não rodou:** diga o que foi verificado, o que não foi e o que depende do jogo. Correção é candidata até o próximo playtest.
- **Imagens do autor valem mais que a leitura do texto dele:** o desenho da 0.4.43 derrubou uma recomendação minha de juntar encontros próximos.
- **Limpeza:** ao fim de cada etapa, `./gradlew.bat --stop` e mate o que ficou ocioso.
- **CI:** não altere os workflows.

## Ambiente

- **Máquina do autor:** Windows 11, projeto em `C:\Users\lucas\Desktop\MOD-village-1.21.1`.
- **JDK 21:** o `PATH` não basta. Em Git Bash: `export JAVA_HOME=/c/Users/lucas/.jdks/jdk-21.0.12+8`.
- **Compilar:** `./gradlew.bat compileJava compileTestJava compileGametestJava -q`. ErrorProne roda com `-Werror`, então aviso reprova.
- **Sentinela:** `python scripts/test_lab.py fast`, depois `python scripts/sentinel_analysis.py`, com o Minecraft fechado. Mudou regra ou teste: `python scripts/rule_coverage.py`. Mudou `docs/RULES.md`: `python scripts/rules_to_readme.py`.
- **Instalar para o autor jogar:** `./gradlew.bat remapJar`, copie `build/libs/village-colony-<versão>.jar` para `downloads/` e para `%APPDATA%\.minecraft\mods` (TLauncher), renomeie o anterior para `.jar.disabled` e compare o SHA-256 das três cópias.
- **Publicar:** com `docs/releases/v<versão>.md` na `main`, `gh workflow run Release --ref main`. O workflow compila outro JAR, com outro SHA-256.

### Só existe na máquina do autor, fora do GitHub

`%APPDATA%\.minecraft\logs\latest.log` (sobrescrito a cada jogo), o diário `logs\villagecolony-actions-*.jsonl`, o save `saves\Novo mundo`, `reports/`, `graphify-out/`, `downloads/` e os checkpoints `.handoff/`. Sem acesso a eles, peça o log ao autor: os relatórios e o `scripts/save_read.py` dependem deles.

O hook do Claude Code que manda rodar `graphify` antes de ler arquivos não se aplica se `graphify-out/` não existir.

## Ferramentas

| Script | Para quê |
|---|---|
| `scripts/time_ledger.py <log>` | tempo por ofício (Regras 50 e 51) |
| `scripts/action_report.py` | diário e motivos de recusa; toda análise de Spark o mostra ao autor |
| `scripts/cost_ledger.py <log>` | custo por fase do ciclo |
| `scripts/spark_decode.py` | resumo de um Spark baixado |
| `scripts/save_read.py` | grafo e mapa do save, sem abrir o jogo |
| `scripts/gametest_focus.py` | roda só algumas classes de GameTest; `runGametest` não aceita filtro |
| `scripts/gametest_summary.py` | total e falhas do relatório de GameTest |

Triagem no log: `VC_BIOME_NOTE` primeiro, depois `VC_MAE_SITE`, `VC_ROAD_GRAPH`, `VC_ROAD_COVERED`, `VC_SUPPLY`, `VC_SUPPLY_ERROR`, `VC_PENS`, `VC_DAY`, `VC_STATE`.

**Prova com sabotagem:** teste verde de primeira não prova nada. Depois de implementar, troque um comportamento de cada vez e confira que algum teste falha. Na 0.4.43 isso expôs um teste que passava de qualquer jeito.

## Verificado e não verificado na 0.4.43

- **Verificado:** Sentinela com 1060 GameTests e 1768 unitários, CI da PR 60 verde, 11 sabotagens pegas. No jogo: 0 quadrados de nós e 0 brancos soltos.
- **Não verificado:** PIT local (o core mudou), a troca de `DoorFronts.of` por `outside` em `BuildingGaps`, e os efeitos em jogo das Regras 217 a 221.
- **Testes instáveis:** `animalsWalkInThroughEachOfTheFourGates` e `workerCollectsWhileWalkingPast`, sem causa achada. Veja o `TODO.md`. Repita a bateria antes de culpar a mudança.

## O que falta no MAE

Lista viva em `TODO.md`, seção "MAE: o que falta depois da 0.4.26". Resumo:

- **Depende de jogo:** `/vc mae obra <modelo>` do `queued` ao `done`, retomada depois de reiniciar, obra `fora`, socorro até o fim (só disparou, e a obra foi parada), carga real.
- **Depende de código:** testes de `WaitingWork`, `MaeSites.move` e da volta que zera a conta em `LotSeeking`; motivo de recusa de `/vc mae obra`; travada de `MaeSites.start`; prova por seção durante a obra; PIT; plano mestre desatualizado.
- **Depende do autor:** regra da água para a obra pedida pelo jogador, ruínas de saves antigos e o bairro novo, hoje fora do recorte (ADR-049).

## Git

- **PR aberta antiga:** [#45](https://github.com/LucasRiboldi/MOD-village-1.21.1/pull/45), `codex/local-test-sentinel`, sem atividade desde 2026-11-02. Pergunte ao autor antes de mexer.
- **Só locais e fora da `main`:** `docs/regra-166-ferramentas` e `fix/miner-direct-path-gametest`, 1 commit cada, de outubro. Não foram enviados: são velhos e não fazem falta ao ponto atual.
- **Muitas branches `origin/claude/*` e `origin/codex/*`:** históricas. A existência de uma branch não prova que está integrada nem concluída.
- **Atribuição:** o rodapé `Co-Authored-By: Claude` dos commits antigos é do Claude Code. Use o seu.

## Memória das sessões anteriores

[`docs/archive/agent-memory/`](docs/archive/agent-memory/MEMORY.md) traz a cópia da memória do Claude Code: uma lição por arquivo, com o índice em `MEMORY.md`. É histórico: procure por tema (`rg`), não leia inteiro, e confira no código antes de confiar, porque cada nota reflete o dia em que foi escrita. Vale ler quando for mexer no assunto: gametest, mina, pastor, grafo, MAE.

As armadilhas que mais custaram:

- Classe de GameTest fora de `src/gametest/resources/fabric.mod.json` some da bateria em silêncio.
- Relatório de GameTest velho engana: apague `build/gametest-report.xml` antes de rodar.
- GameTests do mesmo lote interferem entre si: use `batchId` próprio e coordenada longe.
- `findPathTo` logo depois de `spawnEntity` dá `null`: espere pousar.
- Heredoc grande no shell quebra: escreva o script em arquivo.
- Rodar `gradlew.bat` de dentro de um `subprocess` do Python com `shell=True` no Windows falha: use Git Bash.
