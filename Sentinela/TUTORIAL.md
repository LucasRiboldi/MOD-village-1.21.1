# Tutorial — Sentinela Local

O Sentinela Local roda verificações do Village Colony na própria máquina. Ele
não usa agente de IA, não inicia ao abrir o projeto e não altera o CI.

## Antes de executar

- Feche Minecraft/TLauncher antes de atualizar o JAR ou medir o mod.
- Use Java 21 e Python 3.9+; a janela também requer Tkinter.
- Escolha o perfil mais barato que responde à dúvida. Não use `full` para uma
  alteração documental.

## Abrir a janela Windows

Abra `Sentinela/Abrir-Sentinela.cmd` por duplo clique. Se o Windows não abrir
a janela, rode no PowerShell, a partir da raiz do repositório:

```powershell
py -3 Sentinela/sentinela_gui.py
```

Na janela:

1. Escolha o perfil.
2. Deixe selecionadas somente as suites necessárias.
3. Defina repetições. Use poucas primeiro; aumente após uma execução estável.
4. Defina o limite em minutos. `0` significa sem limite.
5. Marque `Simular comandos` para conferir a seleção sem iniciar Gradle.
6. Clique `Executar`.

Quando o limite chega, a suite já iniciada termina. A próxima não inicia, o
relatório recebe `INTERRUPTED` e pode ser retomado depois.

## Perfis e suites

| Perfil | Suites disponíveis | Uso indicado |
| --- | --- | --- |
| `quick` | `unit`, `property` | lógica rápida e propriedades jqwik |
| `fast` | `quick` + `gametest` | alteração que toca mundo ou Fabric |
| `full` | `fast` + `gametest-audit`, `mutation` | validação manual completa |

Suites indisponíveis no perfil ficam desativadas na janela. Na linha de comando,
`--only` só aceita suites do perfil escolhido.

## Linha de comando

Execute na raiz do repositório:

```powershell
python scripts/test_lab.py --list
python scripts/test_lab.py quick --dry-run
python scripts/test_lab.py fast --only unit,property --repeat 100
python scripts/test_lab.py full --budget-minutes 120
python scripts/test_lab.py --resume 20261102T120000Z
```

`--dry-run` lista os comandos e não inicia Gradle. `--repeat` repete cada suite.
`--budget-minutes` encerra com segurança antes da próxima suite. `--resume`
retoma apenas suites que ainda não foram registradas na execução interrompida.

## Segurança MAE — Fases 1 e 2

Os testes MAE integram as suites existentes: `WorkAreaTest`, `TerrainPlannerTest`
e `MaeWorkStoreTest` em `unit`; `MaeSafetyGameTest`, `MaeTerrainGameTest`,
`MaeRoadEvidenceGameTest`, `MaeRoadGateGameTest`, `MaeContractGameTest` e
`MaeMaterialGameTest` em `gametest`. Não há suite, perfil ou execução
automática adicional. A janela inclui esses casos quando executa as respectivas
suites. Para guardar os relatórios completos no fluxo normal:

```powershell
python scripts/test_lab.py fast --only unit,gametest
```

Para diagnóstico restrito, sem executar todas as profissões e construções:

```powershell
.\gradlew.bat test --tests com.villagecolony.core.construction.model.WorkAreaTest --tests com.villagecolony.core.construction.model.TerrainPlannerTest --tests com.villagecolony.data.save.MaeWorkStoreTest
.\gradlew.bat runGametest "-PgametestOnly=MaeSafetyGameTest,MaeTerrainGameTest,MaeRoadEvidenceGameTest,MaeRoadGateGameTest,MaeContractGameTest,MaeMaterialGameTest"
```

Os comandos Gradle diretos geram os relatórios normais de `build/`; não criam
uma execução do painel Sentinela. Verificam prova de geração, recusa em terreno
legado/desconhecido, estruturas, água, árvores, BlockEntities, limites de lote,
proteção surgida após aprovação, orçamento, pausa, progresso salvo e intenção
sem confirmação. A Fase 2 cobre flores controladas, corte/aterro, materiais,
exclusão de seções, prova encadeada de ruas, recarga e percurso de aldeão real
numa superfície local com degrau. As fixtures são sintéticas, fora de saves do
jogador. Não aprovam qualidade visual de smoothing, navegação da vila inteira
ou desempenho sob carga real. As fases não ativam obras MAE automaticamente.

A fixture `MaeRoadEvidenceGameTest` emite `VC_MAE_METRICS` no log Gradle após
concluir o percurso: leituras incrementais, escritas, máximo por fatia e maior
duração observada em nanossegundos. Leituras internas de hash/proteção não entram
no contador incremental. Uma operação síncrona pode exceder dois milissegundos;
o registro não demonstra SLA nem desempenho de uma vila completa. Os testes
de arquitetura existentes também devem passar após mudanças entre camadas.

## Analisar execuções já concluídas

Depois de rodar testes, clique `Analisar relatórios` na janela ou execute:

```powershell
py -3 scripts/sentinel_analysis.py
```

A frase `analisar Sentinela Local` usa esse comando. Ele nunca inicia Gradle:
lê `reports/test-runs/` e cria `reports/sentinel-analysis/<UTC-id>/` com
`analysis.json`, `analysis.md` e `dashboard.html`. A análise agrupa falhas de
compilação, teste e infraestrutura; marca `RUNNING` como evidência incompleta;
e aponta ausência de XML como lacuna de evidência, sem inventar casos de teste.

Cada suite nova também grava `metrics.json` com amostras locais de CPU, RAM e
memória Java. GPU depende de `nvidia-smi`; se não existir, fica explicitamente
indisponível e a suite mantém seu status Gradle. As métricas comparam pressão da
máquina, não provam desempenho do cliente ou correção visual no jogo.

## Ler o resultado

Cada execução cria `reports/test-runs/<UTC-id>/`:

- `summary.md`: resumo direto.
- `summary.json`: dados para análise local.
- `dashboard.html`: painel visual; abra no navegador.
- `suites/*.stdout.log` e `*.stderr.log`: saída de cada suite.
- `artifacts/`: XMLs e relatórios copiados na hora da execução.

`PASS` significa que todas as suites executadas retornaram sucesso. `FAIL` ou
`ERROR` exige abrir os logs. `UNSTABLE` indica que a mesma suite falhou e passou
em repetições diferentes. `INTERRUPTED` não é resultado verde: retome a execução
ou faça uma nova rodada.

O painel agrupa falhas pela primeira assinatura XML, lista até 20 casos lentos
e mostra até 50 execuções locais. XML ausente não altera o resultado do Gradle;
o código de saída da suite continua sendo a fonte de verdade.

## Regra de manutenção

Sempre que uma alteração mudar perfis, suites, argumentos, relatórios, a janela
ou o comportamento de retomada do Sentinela, atualize este tutorial no mesmo
commit. Atualize também o resumo do Sentinela em `README.md` se o comando ou a
capacidade visível ao autor mudar.

O modo temporário do projeto ainda pode proibir baterias reais. Nesse caso, use
somente `--dry-run` até o autor liberar os testes.
