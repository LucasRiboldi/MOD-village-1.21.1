# D9 — Segurança e robustez

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `java-static-analysis` (SpotBugs com esforço máximo, leitura das
entradas que vêm de fora) e `fabric-persistence-migration` (desserialização).
**Nota da dimensão:** 7/10.

## Superfícies de entrada

| Entrada | Quem controla | Validação | Evidência |
|---|---|---|---|
| `/vc log` | qualquer jogador | **nenhuma permissão** (nível 0); só leitura, só na Overworld | `fabric/command/VillageLogCommand.java:36-37,48` |
| Pedido de política (C2S `request`) | qualquer jogador | só devolve o snapshot | `fabric/network/ProfessionPolicyNetworking.java:26-27` |
| Mudança de política (C2S `update`, pelo Mod Menu) | jogador com o mod | **exige nível 2** (`:33`, constante `:13`); sem permissão, `WARN` e reenvio do snapshot | `:32-46` |
| Limites do payload | — | codec valida quantidade, duplicata e ordem; record limita `maximumWorkers` 0..512 e `searchRadius` −1 ou 16..128 | `ProfessionPolicyPayloadCodec.java:35-56`; `core/worker/model/ProfessionPolicy.java:19-27` |
| Save do mundo (NBT) | disco, outros mods, edição manual | parcial: ver AUD-02 | D5 |
| `Identifier` | — | não há `Identifier.of` sobre texto do disco; `ResourceId.parse` lança (AUD-02); `toBlock` lança com caractere inválido (AUD-02) | `MinecraftTypeAdapter.java:213-215,245` |
| Threads | — | nenhuma fora do servidor | D3 |

O README diz que mudar regra exige operador nível 2 (`README.md:167-168`), e o
código confirma.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-02 | P1 | Desserialização sem fronteira: uma entrada ruim apaga o estado inteiro | D5 | D5 | D5 | D5 |
| AUD-11 | P2 | A política de profissão, única escrita vinda do cliente, não tem teste | D7 | — | D7 | D7 |
| AUD-17 | P3 | `/vc log` aberto a todo jogador | `VillageLogCommand.java:36-37`, sem `.requires(...)` | Em servidor público, qualquer um lê o estado da colônia (posição da obra, alcance a pé, esperas) | Decisão do autor: `.requires(src -> src.hasPermissionLevel(2))` ou manter aberto e registrar o motivo numa ADR | Teste do comando com fonte de nível 0 |

## SpotBugs: o que foi descartado e por quê

| Tipo | Local | Por que não é defeito |
|---|---|---|
| `BC_IMPOSSIBLE_CAST` | `VillagerDataMixin.java:27` | `(VillagerEntity) (Object) this` é o idioma padrão do Mixin |
| `IM_BAD_CHECK_FOR_ODD` | `MineShaft.java:292` | `withinRow` vem de `index ≥ 0`, validado em `:283` |
| `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` | `ProfessionPolicyNetworking.java:39,43,50` | `getServer()` de um `ServerPlayerEntity` conectado |
| `NP_NULL_ON_SOME_PATH` | `client/modmenu/*Screen.java` | `client` da tela é nulo só antes de `init`; não lido em detalhe |
| `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` (49) | records e registros | exposição de coleção interna. O projeto usa `List.copyOf` nos records (`DetourPlanner.Detour`); não foi auditado caso a caso |

## Não verificado

- Os 49 `EI_EXPOSE_REP*` caso a caso.
- Comportamento do cliente com mod num servidor sem mod ao abrir o Mod Menu:
  `ClientProfessionPolicies.java:31,36` envia sem `ClientPlayNetworking.canSend`.
