# Sentinela Local — aplicativo

Abra `Abrir-Sentinela.cmd` para configurar e iniciar uma varredura local sem
usar um agente de IA. A janela chama somente o orquestrador oficial
`../scripts/test_lab.py`; resultados, logs e artefatos continuam em
`../reports/test-runs/<UTC-id>/`.

Requisitos: Python 3.9+ com Tkinter e o Java 21 já exigido pelo projeto.
`Abrir relatórios` abre a pasta dos resultados. Use `Simular comandos` para
conferir o que será executado sem iniciar Gradle.

Para verificar a instalação sem abrir a janela:

```powershell
py -3 Sentinela/sentinela_gui.py --check
```

O aplicativo não agenda testes, não altera CI e não substitui o playtest do
jogo.

Leia [TUTORIAL.md](TUTORIAL.md) para perfis, suites, orçamento, retomada e
interpretação dos relatórios.

## Regra de manutenção

Qualquer alteração nos perfis, suites, argumentos, relatórios, janela ou
retomada do Sentinela deve atualizar `TUTORIAL.md` no mesmo commit. Se mudar um
comando ou uma capacidade visível ao autor, atualize também a seção Sentinela do
`README.md` da raiz.
