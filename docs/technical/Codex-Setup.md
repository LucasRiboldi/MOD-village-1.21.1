# Personalização do Codex para Village Colony

Guia de configuração enxuta para trabalhar neste mod Fabric 1.21.1 sem perder os portões de qualidade do projeto.

## Onde cada regra mora

- `AGENTS.md`: fluxo do Codex e invariantes que devem valer em toda tarefa do repositório.
- `CLAUDE.md`: ponto de entrada canônico do projeto; em divergência, ele prevalece.
- `skills-lock.json`: skills externas instaláveis e reproduzíveis. O lock deste projeto mantém `minecraft-modding`, `minecraft-testing` e `minecraft-ci-release`.
- `~/.codex/config.toml`: preferências pessoais e perfis. Não versionar dados pessoais de configuração no repositório.
- `.claude/skills/`: skills versionadas do projeto. Use apenas as relevantes para a tarefa; não carregue a coleção inteira por padrão.

## Perfis atuais

A configuração pessoal já contém quatro perfis adequados. Na CLI, selecione com `codex --profile NOME`:

| Perfil | Configuração | Uso recomendado |
|---|---|---|
| `village-quick` | GPT-5.5, raciocínio baixo, somente leitura | Consultar `STATE.md`, localizar pendências e responder perguntas pontuais. |
| `village-audit` | GPT-5.5, raciocínio alto, somente leitura | Diagnóstico, revisão de código e coleta de evidências sem editar. |
| `village-dev` | GPT-5.6 Terra, raciocínio médio, escrita no workspace | Correções e tarefas rotineiras com testes direcionados. É o melhor padrão para implementação. |
| `village-max` | GPT-5.5, raciocínio alto, escrita no workspace | Defeitos difíceis, mudanças que cruzam subsistemas e análise com várias hipóteses. |

O padrão pessoal atual é GPT-5.5, raciocínio alto, verbosidade baixa, `workspace-write` e aprovação `on-request`. Mantenha a verbosidade baixa; aumente o raciocínio só quando a complexidade justificar. O perfil `village-quick` é somente leitura, mesmo com aprovação automática.

## Fluxo recomendado

1. Para pergunta simples, use `village-quick`. Se o problema e o escopo já estão claros, vá direto a `village-dev`.
2. Use `village-audit` apenas quando ainda for necessário confirmar causa, escopo ou risco; evite uma passagem de auditoria que repita contexto já conhecido.
3. Reserve `village-max` para problemas difíceis ou que cruzem subsistemas.
4. Peça teste direcionado primeiro. Para código Fabric, execute também `runGametest`; resultado verde ainda precisa de validação em jogo quando o comportamento for visual ou depender do mundo.
5. Termine com resumo do diff, comandos executados e o que permanece aguardando playtest ou decisão do autor.

Prefira prompts que nomeiem o item, os arquivos/sintomas conhecidos, a evidência esperada e se a tarefa autoriza editar. Evite colar históricos extensos: indique o documento e o trecho a consultar.

## Skills sob demanda

- Alterações de gameplay Fabric: `fabric-development` e `minecraft-modding`.
- Brain, profissões, POI ou agenda de aldeões: `minecraft-villager-systems`.
- API ou comportamento Vanilla incerto: `minecraft-code-research` antes de codificar.
- Testes de mod: `minecraft-testing`.
- CI/CD, artefatos e release: `minecraft-ci-release`; mantenha a **categoria 1.9** nas auditorias de CI/CD.

Uma skill deve cobrir um fluxo repetido e específico. Não copie todas as instruções da skill para o prompt nem para `AGENTS.md`; a descrição da skill deve acioná-la quando o assunto combinar.

## Personalização segura

- Edite `~/.codex/config.toml` para preferências pessoais; consulte **Settings > Configuration > Open config.toml** no Codex desktop.
- Só crie `.codex/config.toml` no projeto se houver uma política compartilhada que deva valer para todos os colaboradores.
- Mantenha permissões restritas por perfil: auditoria em leitura; escrita apenas nos perfis de implementação; confirmação para operações externas ou destrutivas.
- Não transforme este guia em uma segunda cópia de `CLAUDE.md`. Atualize `AGENTS.md` quando uma regra curta e duradoura corrigir um erro recorrente; mantenha detalhes de um fluxo em skill ou documento específico.

## Referências oficiais

- [AGENTS.md no Codex](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Configuração do Codex](https://learn.chatgpt.com/docs/config-file/config-basic)
- [Criar skills](https://learn.chatgpt.com/docs/build-skills)
