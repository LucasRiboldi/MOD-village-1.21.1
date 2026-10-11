---
name: git-workflow-audit
description: Auditoria de histórico e fluxo Git — commits gigantes, branches órfãs ou à frente da main, arquivos que não deviam estar versionados (JAR, build, segredos), e drift entre documentação e o estado real do código. Use ao encerrar ciclo, ao auditar as dimensões de release e documentação, ou antes de apagar branch. Não apaga nem reescreve histórico.
---

# git-workflow-audit

## Propósito

Mostrar se o histórico e os documentos contam a verdade: o que está na `main`,
o que está esquecido em branch, e onde o README/STATE/TODO divergem da saída
real dos comandos.

## Quando usar

- Dimensões **D8 (build/release)** e **D10 (documentação e drift)**.
- Antes de "relatorio commit" ou de abrir PR.
- Quando dois agentes (Claude, Codex) trabalharam em paralelo (ADR-035 §2).

## Mapeamento para a auditoria

| Dimensão | O que este skill entrega |
|---|---|
| D8 Build/release | `.gitignore` cobre JAR/build/site? tags vs versão do Gradle |
| D10 Drift | README/STATE/TODO vs saída do Gradle; ADR vs código |

## Checklist

1. **Estado.** `git status`, branch, `git log main..origin/main` e o inverso.
2. **Branches.** Para cada branch local e remota: commits à frente da `main`
   (`git rev-list --count main..<b>`), data do último commit, se tem worktree.
   À frente e parada há mais de 3 dias = órfã a decidir (não apagar).
3. **Commits grandes.** `git log --since=30.days --numstat`, somar linhas por
   commit, listar os > 500 linhas e separar código de documentação.
4. **Arquivos versionados indevidos.** `git ls-files | grep -E '\.jar$|/build/|\.env|\.log$'`;
   `.gitignore` cobre `downloads/`, `build/`, `run/`?
5. **Drift numérico.** Toda contagem no README/STATE/TODO (testes, GameTests,
   PIT, versão) contra a saída do comando nesta sessão. Número sem fonte é achado.
6. **Teto dos documentos vivos.** `STATE.md` declara teto de 150 linhas;
   medir. `CLAUDE.md` pede arquivos < 500 linhas.
7. **ADR vs código.** Para as ADRs citadas no `STATE.md`, um `grep` pelo
   símbolo que a ADR governa basta para ver se o código ainda a segue.

## Comandos

```bash
git for-each-ref --format='%(refname:short) %(committerdate:short)' refs/heads refs/remotes
for b in $(git for-each-ref --format='%(refname:short)' refs/heads); do echo "$b $(git rev-list --count main..$b)"; done
git log --since=30.days --pretty=format:'@%h %s' --numstat
git ls-files | grep -E '\.jar$'
```

## Anti-padrões

- Apagar branch "órfã" durante auditoria — decisão é do autor.
- Corrigir número do README copiando de outro documento em vez do comando.
- `git mv` de documento histórico sem atualizar quem aponta para ele.
