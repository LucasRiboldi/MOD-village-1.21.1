# D6 — Mixins e AccessWideners

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `modpack-modsmith` (níveis 1 e 2 do `probar.py`, contra o JAR 0.3.23 e o
`.minecraft` real) e `minecraft-mod-dev` (convivência com outros mods de aldeão).
Verificador próprio: `scripts/audit/check-mixin-safety.sh`.
**Nota da dimensão:** 9/10.

## Inventário

| Mixin | Alvo | Injeção | Handler |
|---|---|---|---|
| `fabric/mixin/VillagerEntityMixin` | `VillagerEntity.initBrain` | `@Inject` em `TAIL`, sem cancelar | `villagecolony$installColonyTask` |
| `fabric/mixin/VillagerDataMixin` | `VillagerEntity.setVillagerData` | `@ModifyVariable` em `HEAD`, `argsOnly` | `villagecolony$noVanillaTradeForColonyWorkers` |
| `client/mixin/WorkerNameplateMixin` (só cliente) | `LivingEntityRenderer.hasLabel` | `@ModifyReturnValue` (MixinExtras) | `villagecolony$nameInsideTheOverlay` |

- **Nenhum** `@Overwrite`, `@Redirect` ou `@ModifyConstant`. A recomendação do Fabric
  (MixinExtras no lugar dos que não encadeiam) já é seguida.
- Todos os handlers têm o prefixo `villagecolony$`. Não há campo novo em mixin, então `@Unique` não se aplica.
- `injectors.defaultRequire = 1` nas duas configs: injeção que não acha o alvo
  derruba o carregamento em vez de falhar em silêncio.
- Não há AccessWidener (`fabric.mod.json` sem `accessWidener`).
- **Refmap:** o JAR não tem refmap, e não precisa. As strings de alvo já estão em
  intermediary no bytecode (`method_19174`, `method_7195`, `method_4055`,
  `class_1646`): o Loom remapeia direto.
- `probar.py` níveis 1 e 2: **14 OK, 0 aviso, 0 falha** (`build/audit/probar-0.3.23.txt`).
- O verificador foi testado com uma mutação plantada (`@Redirect` e handler sem
  prefixo em `VillagerEntityMixin`): reprovou com saída 1. A mutação foi revertida
  e `git diff -- src/` ficou vazio.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-12 | P2 | Ninguém percebe se outro mod substituir o `Brain` do aldeão depois do `initBrain` | `VillagerEntityMixin.java:29` acrescenta a tarefa no fim; não há verificação posterior de que ela continua no `Brain`; ADR-004 §7 registra o conflito como conhecido | Com um mod de IA de aldeão (ex.: Sapience) a colônia pode simplesmente não trabalhar, sem erro | Diagnóstico: a cada ciclo, se um trabalhador registrado não tem a tarefa da colônia, registrar `WARN` uma vez por aldeão. E declarar os conflitos conhecidos em `fabric.mod.json` (`breaks` ou `conflicts`) | GameTest que limpa o `Brain` depois do spawn e espera o `WARN`; nível 4 do modsmith com o mod concorrente |
| AUD-23 | P3 | O javadoc diz "O único mixin do mod" | `VillagerEntityMixin.java:14` (há 3 mixins) | Comentário que mente (ADR-035 §6) | Corrigir o javadoc | — |

## Não verificado

- Nível 4 do modsmith (o pack completo com o mod, lendo o log): manual. O
  `.minecraft` de teste tem 15 mods, sem nenhum que mexa em aldeão.
