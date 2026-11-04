# Handoff — Village Colony, MAE Fase 3

## Alternância entre agentes

Os checkpoints DeltaTorch ficam locais em `.handoff/`, fora do Git. Antes de
atingir 10% de contexto restante, quando esse indicador estiver disponível,
registre o trabalho com `pnpm dlx delta-torch@0.1.6 save --reason context-limit`.
Não inclua segredos. No outro agente, execute
`pnpm dlx delta-torch@0.1.6 resume latest` e siga a seção **Next Steps**.
O hook local de pré-commit usa `sync-claude-md@1.0.1` para manter referências
`@AGENTS.md` em arquivos `CLAUDE.md`; ele não compara o sentido das regras.
Consulte `AGENT-SWITCH-SETUP.md` para instalação e limites do hook.

Registro de 2026-11-04. `[V]` indica evidência conferida no Git ou na
documentação do repositório; `[?]` indica informação ainda não comprovada.
Reconfira o estado antes de trabalhar: este arquivo é uma fotografia.

## Objetivo e estado do Git

- [V] Continuar a Fase 3 do MAE: acesso físico e construção supervisionada
  de estruturas em áreas elegíveis de vilas novas.
- [V] Branch ativa: `codex/mae-structure-access`. Checkpoint:
  `28408957e401c0d64c11dc3183c864e27bf576d2` —
  `chore(mae): registrar checkpoint parcial da fase 3`.
- [V] A branch remota de trabalho e `backup/mae-fase3-2026-11-04` apontavam
  para esse commit na conferência. A base é `9dc8b4f2`, merge da PR #51.
- [V] Antes de criar este arquivo, não havia diff rastreado; `.serena/` era
  um diretório não rastreado, preservado fora do commit. Nenhum stash foi listado.
  Este `HANDOFF.md` foi criado depois do checkpoint de código.
- [V] O checkpoint é parcial; não foi integrado à `main`. A existência de
  outras branches não prova que todas foram integradas ou estão concluídas.
- [V] Leia `STATE.md` (primeiras 60 linhas), `CLAUDE.md`,
  `docs/decisions/ADR-046-mae-structure-access.md` e o plano
  `docs/superpowers/plans/2026-11-04-mae-structure-access.md`.

## Concluído no checkpoint

- [V] Metadados de acesso, leitura dos templates, limites e rotações,
  validação local de circulação e leitura estrita dos estados.
- [V] Variante supervisionada do curral grande com quatro portões; os
  acessos pertencem ao mesmo cercado, com capacidade total 20.
- [V] A opção B do autor mantém o curral automático anterior, de um portão.
  Os quatro portões são exclusivos de `readForMae` por enquanto.
- [V] A codificação NBT do pórtico foi corrigida, preservando geometria e
  164 peças. Correções também cobrem limites, overflow de orçamento e uma
  dependência circular identificada pelo teste de arquitetura.
- [V] `STATE.md` e `docs/technical/MAE-Fase-3.md` registram 36 testes
  unitários/arquitetura e 67 GameTests focados aprovados anteriormente.
  As execuções ocorreram antes da remoção posterior de variáveis sem uso em
  uma fixture; não foram repetidas após essa limpeza. Não trate isso como
  prova de execução sobre o hash final. Nenhum teste foi executado na pausa.

## Decisões e armadilhas

- [V] Consulte a ADR-046 e a seção 7 de
  `docs/technical/MAE-Fase-3-Matriz.md`: leitura de modelo não prova
  conteúdo funcional, materiais, circulação de cada modelo ou habilitação.
- [V] `PaletteStates.planned` interpreta estados estritos sem criar uma
  dependência de `integration` para `work`; não generalize essa política
  para a colocação legada sem analisar compatibilidade.
- [V] A coleta legada do pastor usa teleporte. Testes existentes de cercado
  não demonstram a passagem física de animais pelos portões.
- [V] `PersistentState.markDirty` agenda salvamento; não torna atômicos
  inventário, blocos e diário. Uma retomada ambígua deve bloquear e informar
  a causa. Round-trip de serialização sozinho não prova segurança após crash.
- [V] Não copiar NBT arbitrário, entidades, UUIDs ou inventários dos
  templates. Respeitar proteção, prova da região, orçamento e materiais reais.
- [V] Não certificar ou migrar saves antigos; não ativar orquestração
  automática antes de carga real e playtest da Fase 2.
- [V] A regra temporária do autor impede profissões de executar atividades
  de outras profissões. Não antecipar o grafo da Fase 4 neste recorte.
- [V] O servidor Fabric de conhecimento reportou `project_root_unresolved`;
  a consulta atendia apenas ao store pessoal. O destino de escrita de equipe
  não estava configurado na última verificação; não presumir conhecimento
  compartilhado disponível.
- [?] Outras bibliotecas ou abordagens experimentadas e rejeitadas não foram
  comprovadas nesta transferência. Consulte os documentos antes de inferi-las.

## Verificações e comandos

- [V] `CLAUDE.md` descreve a confirmação de modelo por etapa, o fluxo de
  branch/PR e as verificações de desenvolvimento. O autor liberou unitários e
  GameTests focados para o recorte já trabalhado; confirme o escopo antes de
  executar novas baterias. A pausa solicitada não autorizou testes.
- [V] Os comandos efetivamente registrados para a execução anterior estão em
  `docs/technical/MAE-Fase-3.md`, seção «Execuções finais focadas». Consulte
  também `docs/PATTERNS.md` e as regras atuais antes de novo commit.
- [V] O JAR desta fase não foi gerado ou instalado; o checkpoint não é release.
  Preserve `.serena/`, saves do jogador, `downloads/` e a pasta de mods ao
  retomar, salvo autorização correspondente.

## Próximos passos

1. [V] Conferir `git status`, `git fetch` e a branch de trabalho; preservar
   este handoff e quaisquer alterações locais existentes.
2. [V] Detalhar o esquema `structure_v3` na ADR-046 antes de implementá-lo:
   região, projeto, efeitos, intenção, confirmação, consumo e cursor.
   O plano descreve os casos que devem bloquear e a compatibilidade de versões.
3. [V] Reutilizar `MaeWorldWriter`, `MaeWorkStore` e o fluxo existente de
   materiais; projetar recuperação diante de falha entre escrita e consumo.
4. [V] Implementar e validar os casos focados aprovados, inclusive partes
   secundárias, proteção, falta de materiais e retomada ambígua.
5. [V] Comprovar conteúdo funcional e circulação por modelo; testar passagem,
   fechamento e ausência de fuga com pastor e animais controlados.
6. [V] Manter ativação automática bloqueada até carga real e playtest da
   Fase 2. Depois, seguir o plano de integração e entrega do projeto.

## Perguntas abertas

- [?] Qual desenho concreto de persistência e consumo comprovará a
  recuperação sem perda ou duplicação após interrupção? A ADR exige bloqueio
  quando o resultado é ambíguo; os campos ainda precisam ser detalhados.
- [?] Quando o autor liberará carga real, playtest e entrega da Fase 3?

## Configuração do Claude Code

- [V] Claude Code local: `2.1.263`; inferior à versão 2.1.281 recomendada na
  solicitação de transferência. Não foi comprovado suporte ao modo combinado.
  A configuração pessoal não contém opção `agents-md` persistida. Existem
  instruções adicionais em `C:/Users/lucas/CLAUDE.md` e
  `C:/Users/lucas/.claude/CLAUDE.md`.
- [?] No Claude aberto neste worktree, consulte `/config` → Project
  instructions → `claude-md-and-agents-md`; reinicie e use `/context` para
  verificar `CLAUDE.md` e `AGENTS.md`. Se a opção não existir, atualize ou
  escolha outro mecanismo de carregamento após aprovação. Não presuma que
  ambos são lidos. O `CLAUDE.md` do projeto continua canônico.
- [V] Há 42 skills de projeto com frontmatter válido; 24 foram copiadas da
  instalação pessoal e ainda estavam não rastreadas na auditoria. Seis nomes
  também existem no perfil Claude pessoal; seus `SKILL.md` são idênticos.
  `/skills` em modo não interativo não estava disponível; confira em sessão.
- [V] `.mcp.json` declara `minecraft-modding` em `8.0.0`. `claude mcp list`
  excedeu 20 segundos, exibiu somente resultados parciais e não comprovou o
  servidor de projeto. Na primeira sessão, use `/mcp` e aprove somente os
  servidores de projeto necessários, caso haja pedido de aprovação.
- [V] `.claude/settings.json` de projeto não existe. Configurações e hooks
  pessoais existem; use `/permissions` e `/hooks` para avaliar o estado real.
  Não foram criadas restrições ou hooks para simular os perfis do Codex.
- [V] `claude doctor` não comprovou autenticação Claude.ai e reportou uma
  instalação npm global adicional. `claude -p /doctor` retornou `Not logged
  in`; faça `/login` e execute `/doctor` na sessão interativa.
- [V] `.fabric/AGENTS.md` não existe neste worktree, mas foi encontrado em
  `C:/Users/lucas/.codex/worktrees/4a81/MOD-village-1.21.1/.fabric/AGENTS.md`.
  Não copie arquivo gerado sem conferir a instalação deste projeto. O MCP
  Fabric atual atende apenas o store pessoal e aponta raiz não resolvida.
- [V] Backup de configurações anterior à cópia das skills:
  `C:/Users/lucas/.codex/worktrees/sentinela-analytics/tools-config-backup-20261104-100330`.
- [?] Decisões do autor: qual versão de `minecraft-modding` usar? `jvmsrc`,
  `serena` e Fabric devem ser instalados por pessoa ou compartilhados? Quais
  permissões e hooks devem valer? Como orientação mínima, avaliar bloqueio de
  comandos destrutivos e confirmação de `git push`; não estão implementados.
- [V] `RULE-RECONCILIATION.md`, `TOOLS-RECONCILIATION.md` e as skills copiadas
  estavam não rastreados na auditoria. `FINAL-VERIFICATION.md` registra o
  diagnóstico final. Arquivos não rastreados não acompanham um novo clone.
