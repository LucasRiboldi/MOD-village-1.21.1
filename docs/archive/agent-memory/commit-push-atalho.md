---
name: commit-push-atalho
description: "Quando o usuário pedir \"commit push\", commitar e enviar ao GitHub e responder apenas \"commit push realizado\""
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-08-16T01:55:23.492Z
---

Quando o usuário disser **"commit push"**, fazer o commit e o push para
o GitHub e responder **apenas com a frase**:

```text
commit push realizado
```

Sem resumo, sem lista de arquivos, sem explicação do que mudou, sem
relatório. A frase e nada mais.

**Por quê:** pedido do usuário em 2026-08-15. Ele já acompanha o
trabalho enquanto ele acontece, e o relato depois do push vira ruído
repetido. Quando ele quer o relato, ele pede — e aí é outro comando, o
[[relatorio-commit-ritual]].

**Como aplicar:**

- A mensagem de commit continua sendo escrita com cuidado, no padrão do
  projeto. O que encurta é a **resposta**, não o commit.
- Vale o que o projeto já manda: sem `Co-Authored-By` a menos que
  `.claude/settings.json` tenha `attribution.commit`, e sem `--force`.

**A única exceção é falha.** Se o build quebrar, o push for recusado,
houver divergência com o remoto ou aparecer segredo rastreado, **não
responder a frase** — dizer o que houve. A frase afirma que o push
aconteceu, e afirmá-la sem ter acontecido seria mentir sobre o estado do
repositório, que é o oposto do que o usuário quer dela.
