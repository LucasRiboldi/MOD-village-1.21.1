# Hooks do Claude Code para este projeto

Criados em 2026-09-24. Dois scripts transformam regras do `CLAUDE.md` em
garantia, e não só em pedido:

| Script | Evento | O que faz |
|---|---|---|
| `commit_gate.py` | `PreToolUse` (Bash/PowerShell) | Em `git commit` com mudança em `src/` ou nos arquivos de build, roda `gradlew build`; se falhar, **bloqueia** o commit e mostra o erro. Commit só de documentação passa direto. |
| `file_size_guard.py` | `PostToolUse` (Edit/Write) | Avisa quando um `.java` ou `.py` passa de 500 linhas. Não bloqueia. |

Os dois foram testados à mão:
- o aviso dispara no `BuildSiteScanner.java` (2134 linhas);
- a trava devolveu saída 2 com um erro de sintaxe na árvore.

## Como ligar

O `.claude/settings.json` é ignorado pelo git, e o agente não pode editá-lo
sozinho: é configuração que controla o próprio agente, então quem liga é
você. Acrescente ao `.claude/settings.json` do projeto, mantendo os hooks
do graphify que já estão lá:

```json
{
  "env": {
    "JAVA_HOME": "C:\\Program Files\\Java\\jdk-21.0.12"
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash|PowerShell",
        "hooks": [
          {
            "type": "command",
            "command": "python \"$CLAUDE_PROJECT_DIR/scripts/hooks/commit_gate.py\"",
            "timeout": 600
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "python \"$CLAUDE_PROJECT_DIR/scripts/hooks/file_size_guard.py\""
          }
        ]
      }
    ]
  }
}
```

O `env.JAVA_HOME` resolve a armadilha da memória `village-colony-build-env`:
o Java do PATH é o 8, e o Loom exige o 21.
