# Ferramentas do Codex para o Village Colony

Registro reproduzível da configuração de agentes e ferramentas feita em 2026-10-31.
Este documento separa o que pertence ao repositório do que é instalação global da
máquina do desenvolvedor.

## Estado versionado

- `skills-lock.json` registra a skill `minecraft-modding` de
  `Jahrome907/minecraft-agent-skills`.
- `.agents/skills/minecraft-modding/SKILL.md` é a cópia local usada pelo Codex.
- `.mcp.json` mantém somente o MCP portátil do projeto:
  `@adhisang/minecraft-modding-mcp@8.0.0`.
- `AGENTS.md`, `CLAUDE.md` e os arquivos gerados pelo Fabric mantêm as regras e
  os hooks compartilhados entre os agentes.

O caminho absoluto do servidor Fabric não é colocado em `.mcp.json`: ele depende
do diretório global do Node em cada máquina. O Fabric continua sendo instalado
globalmente conforme descrito abaixo.

## Instalações globais

```powershell
npx --yes skill-freeze status
codex plugin marketplace add vagkaratzas/token-saviour
codex plugin add token-saviour@token-saviour
npm install -g jvmsrc
codex mcp add minecraft-modding -- npx -y @adhisang/minecraft-modding-mcp
codex mcp add jvmsrc -- npx -y jvmsrc mcp
npm install -g @fenglimg/fabric-cli
fabric install
```

Versões verificadas nesta configuração: Codex CLI `0.166.0-alpha.5.1`, Node
`v26.12.1`, Java `21.0.12 LTS`, token-saviour `1.0.0` e Fabric CLI `2.6.0`.

## Higiene de contexto

O `skill-freeze` deixou 11 skills hot e 98 cold, estimando cerca de 495
tokens/sessão contra 4.905 antes da instalação — economia aproximada de 90%.
As skills Minecraft essenciais permanecem disponíveis sob demanda; skills não
essenciais ficam congeladas.

## MCPs esperados

Após a instalação, `codex mcp list` deve mostrar como habilitados:

- `minecraft-modding` — fonte, mappings, versões, Mixins e Access Wideners;
- `jvmsrc` — inspeção de classes Java de dependências;
- `fabric` — hooks e conhecimento compartilhado do Fabric CLI.

O MCP `fabric` é registrado pelo `fabric install` e pode aparecer com um
caminho global específico da máquina. Não copie esse caminho para o repositório.

## Token-saviour

O plugin global `token-saviour@token-saviour` é invocado no Codex com
`@token-saviour`. Ele recomenda ferramentas econômicas por camada, mas não é
uma dependência do build do mod.

## Fabric store

O store pessoal está montado e o projeto exige esse store, mas ele é
`local-only` e não pode ser usado como destino de escrita compartilhado. Isso é
uma configuração da máquina, não um bloqueio do build. Para uma equipe, deve-se
associar um store compartilhado antes de executar `fabric store switch-write`.

## Decisão explícita

Stonecutter **não está instalado**. O projeto continua sendo Fabric 1.21.1;
suporte futuro a 26.3 será tratado como uma migração Gradle separada, com
decisão sobre matriz de versões e organização das fontes. Não há plugin
Stonecutter nem subprojetos versionados neste commit.

## Verificação realizada

- `./gradlew.bat build` passou antes da configuração final, com 15 tarefas
  acionáveis; os avisos de PMD existentes não impediram o build.
- `codex plugin list` confirmou `token-saviour` instalado e habilitado.
- `codex mcp list` confirmou `minecraft-modding`, `jvmsrc` e `fabric` habilitados.
- `npx --yes skill-freeze status` confirmou o congelamento e a economia acima.

Depois de instalar ou atualizar plugins globais, reinicie a sessão do Codex para
que as skills e os servidores recém-registrados sejam carregados.
