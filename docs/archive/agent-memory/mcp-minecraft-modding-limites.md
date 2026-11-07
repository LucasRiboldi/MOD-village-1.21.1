---
name: mcp-minecraft-modding-limites
description: "MCP minecraft-modding medido em 30-10 — validação de mixin dá falso positivo em override, fonte Yarn não sai; find-mapping funciona"
metadata:
  node_type: memory
  type: reference
  modified: 2026-10-31T01:59:39.370Z
---

Medido em 30-10 sobre o projeto (Regra 166 do CLAUDE.md §3):
- `validate-project` com `task: mixin` precisa de `version: "1.21.1"` explícito (não aceita `preferProjectVersion`). Deu 3 "inválidos" em 8: `VillagerEntity#setVillagerData`, `VillagerEntity#loot` e `LandPathNodeMaker#getSuccessors`. Os três existem no jar Yarn (javap); são falsos positivos de override/remap.
- `get-class-source` com mapping yarn falha ("only decompile path"), seja pela versão, seja pelo workspace. O confiável é javap no `.gradle/loom-cache/minecraftMaven/.../minecraft-merged-...yarn...jar` ou `genSources`.
- `find-mapping` intermediary → yarn funciona e é exato (class_421 = FatalErrorScreen), útil para stack trace do log.

**How to apply:** erro do MCP só vale depois de conferido com javap. Ver [[jars-nao-instalados-deslocamento]] para o mesmo cuidado com a evidência.
