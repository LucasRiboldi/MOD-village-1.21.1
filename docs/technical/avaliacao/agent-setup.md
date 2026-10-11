# Setup do agente — skills da auditoria técnica de 2026-10-10

> Gate de saída da FASE 1. Branch: `claude/auditoria-tecnica-2026-10-10`, criada
> a partir da `main` em `b290ed10` (igual a `origin/main`).

## Skills e onde cada uma foi aplicada

| # | Skill | Caminho | Origem | Estado | Dimensões |
|---|---|---|---|---|---|
| 1 | `minecraft-modding` | `.claude/skills/minecraft-modding/` | Jahrome907/minecraft-agent-skills | já instalada, igual ao upstream | D2, D7, D8, D11 |
| 2 | `minecraft-testing` | `.claude/skills/minecraft-testing/` | Jahrome907/minecraft-agent-skills | já instalada, igual ao upstream | D2, D3, D7, D11 |
| 3 | `minecraft-ci-release` | `.claude/skills/minecraft-ci-release/` | Jahrome907/minecraft-agent-skills | já instalada, igual ao upstream | D8 |
| 4 | `modpack-modsmith` | `.claude/skills/modpack-modsmith/` | rdl7999-hub/**minecraft-modpack-modsmith** @ `55ce5b8` (MIT) | **instalada agora** | D6, D8, D12 |
| 5 | `minecraft-mod-dev` | `~/.claude/skills/minecraft-mod-dev/` (global) | chouzz/minecraft-mod-dev @ `48946f2` | já instalada; difere do upstream só por CRLF | D6, D12 |
| 6 | `java-architecture-review` | `.claude/skills/java-architecture-review/` | escrita nesta sessão | **nova** | D1, D7 |
| 7 | `java-static-analysis` | `.claude/skills/java-static-analysis/` | escrita nesta sessão | **nova** | D1, D7, D9 |
| 8 | `minecraft-performance-profiling` | `.claude/skills/minecraft-performance-profiling/` | escrita nesta sessão | **nova** | D3, D4 |
| 9 | `fabric-persistence-migration` | `.claude/skills/fabric-persistence-migration/` | escrita nesta sessão | **nova** | D5, D9, D12 |
| 10 | `git-workflow-audit` | `.claude/skills/git-workflow-audit/` | escrita nesta sessão | **nova** | D8, D10 |

As skills 6 a 10 não têm repositório público indicado no pedido. Foram escritas
para este projeto: apontam para as ferramentas que ele já usa (ArchUnit, PMD,
NullAway, PIT, JaCoCo, `cost_ledger.py`) em vez de pedir ferramentas novas.

## Como a origem foi conferida

- **Jahrome907 (1–3).** Clone raso do upstream em `dd57c5a` (2026-09-12). O
  `SKILL.md` instalado e o do upstream têm o mesmo SHA-256 (`ddc52fc5…`,
  `ee5ba4ff…`, `602ef1d6…`). O checkout do clone falhou no Windows (nome de
  arquivo); a comparação foi feita por `git show HEAD:<caminho>`.
- **chouzz (5).** `diff` com `\r` removido: vazio.
- **modsmith (4).** O nome no pedido (`rdl7999-hub/modpack-modsmith`) não
  existe; o repositório é `rdl7999-hub/minecraft-modpack-modsmith`. `SKILL.md`,
  `probar.py` e as funções de `radiografia.py` que ele importa foram lidos
  antes de rodar. Níveis 1–2 só leem arquivos. Rede só em
  `radiografia.py --integrar`, que esta auditoria não usa. O nível 3 grava
  `run/eula.txt` e roda `runServer`. A licença MIT foi copiada junto.

## Divergências com `skills-lock.json`

| Skill | Lock | Real |
|---|---|---|
| `minecraft-ci-release`, `minecraft-modding`, `minecraft-testing` | `computedHash` = `4f155028…`, `4966194c…`, `dff4009e…` | SHA-256 do `SKILL.md` = `ddc52fc5…`, `ee5ba4ff…`, `602ef1d6…`. O conteúdo é o do upstream; o `computedHash` do lock usa outro algoritmo (provavelmente o hash da pasta inteira pelo CLI que o gerou). Não é desvio de conteúdo. |
| `modpack-modsmith` | ausente | instalada; **não** foi acrescentada ao lock, porque o algoritmo do `computedHash` não é conhecido e escrever um hash inventado seria pior do que a ausência |
| skills 6–10 | ausentes | locais, sem upstream: não cabem num lock de origem GitHub |
| outras 10 skills da Jahrome em `.claude/skills/` (`minecraft-datapack`, `-server-admin`…) | ausentes | instaladas sem registro no lock |

## Verificação

```bash
# os 5 skills novos e o modsmith foram reconhecidos pelo harness nesta sessão
ls .claude/skills
# gate do modsmith, níveis 1-2, contra o .minecraft de teste
python -I .claude/skills/modpack-modsmith/tools/probar.py downloads/village-colony-0.3.23.jar --pack "$APPDATA/.minecraft"
```

`claude mcp list` não se aplica: nenhuma destas skills é servidor MCP.

### Resultado do gate (rodado em 2026-10-10)

`probar.py` sobre `downloads/village-colony-0.3.23.jar` contra
`%APPDATA%/.minecraft` (Fabric Loader 0.19.5, MC 1.21.1, 15 mods):
**14 OK, 0 aviso, 0 falha, saída 0.** Saída bruta em
`build/audit/probar-0.3.23.txt` (fora do Git).
