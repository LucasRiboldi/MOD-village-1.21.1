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
