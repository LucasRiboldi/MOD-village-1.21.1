# Interface local do Sentinela

## Objetivo

Permitir que o autor rode a varredura local por uma janela Windows, sem agente
de IA e sem duplicar os comandos ou a coleta de evidências.

## Decisão

`Sentinela/sentinela_gui.py` usa somente Tkinter da biblioteca padrão e chama
`scripts/test_lab.py` em processo separado. `Abrir-Sentinela.cmd` é o atalho
executável por duplo clique. A janela expõe somente perfil, repetição,
simulação, retomada, saída ao vivo e pasta de relatórios.

## Limites

- Não instala dependências e não inclui binário empacotado.
- Não agenda tarefas, não modifica CI e não altera o orquestrador.
- A validação desta entrega fica limitada a importação/sintaxe e simulação,
  pois o modo temporário de correção por playtest mantém as baterias suspensas.
