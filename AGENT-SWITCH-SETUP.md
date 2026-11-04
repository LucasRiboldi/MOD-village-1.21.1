# Configuração de alternância entre agentes

Verificação em 2026-11-04, no worktree e branch indicados no bloco final.
DeltaTorch 0.1.6 foi instalado como skill de projeto nos diretórios
`.claude/skills/delta-torch/` e `.codex/skills/delta-torch/`, a partir da tag
`v0.1.6` do repositório `MethosPi/delta-torch`. O CLI é executado via pnpm,
sem instalação global. `sync-claude-md` 1.0.1 é executado via npx.

## Evidências e alterações

- `npx --version` e `pnpm --version`: disponíveis; pnpm 11.25.0.
- `npx --yes sync-claude-md@1.0.1 check --all`: antes da configuração,
  indicou ausência de sincronização; após `sync --all --stage`, retornou 0.
- `CLAUDE.md` recebeu somente `@AGENTS.md` e linha em branco antes do conteúdo
  original, que foi comparado com o backup e preservado.
- `pnpm dlx delta-torch@0.1.6 init`, `list`, `save` e `resume latest`
  executaram. O checkpoint #1 de teste continha as seções solicitadas; o arquivo
  foi removido e o registro local foi esvaziado. `list` confirmou zero entradas.
- `.handoff/` está inicializado e ignorado pelo Git. A regra consta de
  `.gitignore`; checkpoints podem conter nomes de arquivos e contexto privado.
- O hook versionado `.githooks/pre-commit` executa
  `npx --yes sync-claude-md@1.0.1 sync --stage`. A execução direta retornou 0.
  Um despachante local no diretório de hooks Git compartilhado o chama apenas
  neste worktree. Ele não existia anteriormente. Outros worktrees não são
  afetados. O despachante não é versionado e precisará ser instalado em outra
  máquina ou checkout sem substituir hooks existentes.
- `AGENTS.md`, `HANDOFF.md` e `README.md` documentam o procedimento. A inclusão
  em `AGENTS.md` também é lida pelo Claude através de `@AGENTS.md`.
- Backup dos arquivos anteriores: `C:\Users\lucas\.codex\agent-switch-backup-20261104`.
- O projeto usa versões explícitas. A execução via `pnpm dlx`/`npx` pode exigir
  rede ou cache disponível. A ferramenta de sincronização insere referências;
  ela não detecta contradições semânticas entre regras.

## Uso e instalação do hook em outro checkout

1. Ao surgir indicador de contexto baixo, execute
   `pnpm dlx delta-torch@0.1.6 save --reason context-limit` e forneça contexto
   sem segredos. Sem medidor visível, não é possível medir os 10% restantes.
2. No outro agente, abra o mesmo worktree e execute
   `pnpm dlx delta-torch@0.1.6 resume latest`; siga **Next Steps**.
3. Para outro checkout, inspecione primeiro `git rev-parse --git-path hooks/pre-commit`.
   Instale um despachante que chame `.githooks/pre-commit` somente para esse
   checkout, preservando qualquer hook existente. Hooks Git podem ser
   compartilhados por vários worktrees. Não substitua um hook existente.

## STATUS DA CONFIGURAÇÃO — ALTERNÂNCIA ENTRE AGENTES

```text
Data: 2026-11-04 10:18
Worktree de referência: C:\Users\lucas\.codex\worktrees\sentinela-analytics\MOD-village-1.21.1
Branch ativa: codex/mae-structure-access

FERRAMENTAS CONFIGURADAS
- DeltaTorch (skill + CLI): INSTALADO
  - Skill Claude Code: INSTALADA (arquivos presentes; descoberta pendente de confirmação na sessão)
  - Skill Codex: INSTALADA (arquivos presentes; descoberta pendente de confirmação em nova sessão)
  - Estado .handoff/ inicializado: SIM
  - Diretório .handoff/ versionado: NÃO (local, ignorado)
- sync-claude-md: CONFIGURADO
  - Hook de pré-commit ativo: SIM (despachante local restrito a este worktree)
  - dry-run limpo (check --all): SIM

VERIFICAÇÃO DO FLUXO DE CHECKPOINT
- pnpm dlx delta-torch list executa sem erro: SIM (versão fixada em 0.1.6)
- Checkpoint de teste criado com sucesso (save --reason test-checkpoint): SIM
- Checkpoint de teste retomado com sucesso (resume latest): SIM
- Checkpoint de teste removido após verificação: SIM (arquivo e entrada do registro)
- Formato contém Goal, Work Completed, Current State, Files Changed, Commands Run, Problems/Risks, Next Steps e Resume Prompt: SIM

VERIFICAÇÃO DA SINCRONIZAÇÃO DE REGRAS
- CLAUDE.md contém referência @AGENTS.md: SIM
- Conteúdo original do CLAUDE.md preservado: SIM
- sync-claude-md check --all sem drift: SIM (verifica referências, não conflitos semânticos)

PENDÊNCIAS
- Bloqueantes: nenhuma identificada.
- Não bloqueantes: [PENDENTE_HUMANO] confirmar descoberta das skills em sessões novas; instalar despachante em outros checkouts se necessário.

AÇÕES HUMANAS RECOMENDADAS
1. No worktree, execute `claude` e, na sessão Claude Code, `/skills`; confirme DeltaTorch.
2. No mesmo worktree, execute `codex` e confira as skills disponíveis na sessão; confirme DeltaTorch.
3. Na primeira troca, agente A: `pnpm dlx delta-torch@0.1.6 save --reason context-limit`.
   Agente B: `pnpm dlx delta-torch@0.1.6 resume latest`.
4. .handoff/ é local e não deve ser commitado; compartilhamento exige decisão posterior.

VEREDITO FINAL
- STATUS_GERAL: AJUSTES_NECESSÁRIOS
- PRONTO_PARA_USO: SIM (CLI e hook verificados; descoberta das skills depende de sessões novas)
- PRÓXIMO_PASSO_HUMANO: Abrir Claude Code e Codex neste worktree e confirmar a descoberta da skill DeltaTorch.
```
