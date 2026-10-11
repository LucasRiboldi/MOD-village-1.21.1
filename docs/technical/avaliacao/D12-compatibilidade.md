# D12 — Compatibilidade e regressão cruzada

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `modpack-modsmith` (níveis 2–4) e `minecraft-mod-dev` (integração entre
mods e dependência opcional).
**Nota da dimensão:** 8/10.

## Matriz executada

| Combinação | Resultado | Evidência |
|---|---|---|
| Fabric API 0.116.15 (mínima declarada = a do build) + Loader 0.19.3 | **690/690** × 3 | `build/audit/raw/gametest-{1,2,3}.log` |
| Fabric API **0.116.17** (a mais nova para 1.21.1) + Loader **0.19.5** | **690/690** | `build/audit/raw/gametest-fabricapi-0.116.17.log`, via `-Pfabric_version` e `-Ploader_version`, sem editar o `gradle.properties` |
| JAR 0.3.23 contra o `.minecraft` de teste (91 mods carregados: Sodium, Iris, Litematica, Xaero, Mod Menu 11.0.5, Spark…) | níveis 1–2 sem falha; a sessão 0.3.23 de 21:24 carregou | `probar-0.3.23.txt`; `latest.log` |
| Save de versão anterior | migração `BREEDER` testada; save malformado derruba tudo (AUD-02) | D5 |

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-12 | P2 | Convivência com outro mod de aldeão não é declarada nem detectada | `fabric.mod.json` sem `suggests`, `recommends`, `breaks` ou `conflicts`; Mod Menu é opcional (ADR-030) e não aparece em `suggests`; ver D6 | Com Sapience ou mod similar instalado, a colônia pode não trabalhar e nada avisa | `"suggests": {"modmenu": "*"}`; `breaks`/`conflicts` para mods de IA de aldeão sabidamente incompatíveis; o diagnóstico de `Brain` de D6 | Nível 4 do modsmith com o mod concorrente; GameTest de D6 |

## Não verificado

- Colisão real com Sapience ou outro mod de aldeão: não instalado; seria o nível 4 do modsmith.
- Servidor dedicado com cliente Vanilla conectado: a lógica foi lida (D11), não executada.
