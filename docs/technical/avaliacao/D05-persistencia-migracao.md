# D5 — Persistência e migração

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skill:** `fabric-persistence-migration`, aplicada no inventário de `PersistentState`, na
versão de esquema, nas renomeações e no fuzzing leve
(`scripts/audit/probes/NbtFuzzProbeTest.java`, via `check-nbt-migration.sh`).
**Nota da dimensão:** 5/10.

## O mecanismo que torna estes achados graves

Desmontei o `PersistentStateManager` do Minecraft 1.21.1 (`javap -c` sobre o JAR
yarn em `.gradle/loom-cache`):

- `readFromFile` envolve a leitura num `catch (Exception)`, registra
  `Error loading saved data: {}` e **devolve `null`**;
- `getOrCreate` recebe esse `null`, **constrói um estado novo vazio** e o
  registra com `set(...)`.

Ou seja: **qualquer exceção** dentro de `ColonySavedData.readNbt` faz o mundo
abrir sem nenhuma colônia, obra ou trabalhador. No próximo salvamento, o
estado vazio é gravado por cima de `villagecolony_colonies.dat`. Não há crash
nem mensagem para o jogador; só uma linha `ERROR` no log.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-02 | **P1** | O leitor do save lança exceção em 6 de 10 formas de NBT malformado, o que aciona o mecanismo acima | `NbtFuzzProbeTest`: lançam `saveVersion=-1` (`SaveMigration.java:82-83`), `blueprint` de obra ou projeto com dois `:` e `blueprint` ausente ou em branco (`ResourceId.parse`, chamado sem proteção em `ColonySaveReader.java:93,117,161`). Passam: profissão de tipo trocado, profissão desconhecida (`MANUFACTURER`), lista com elemento string e `centerX` string. Também `MinecraftTypeAdapter.toBlock` (`fabric/adapter/MinecraftTypeAdapter.java:245`) lança `InvalidIdentifierException` com caractere inválido, apesar do javadoc prometer vazio | Uma entrada ruim, vinda de edição manual, de outro mod, de disco ou de bug futuro de escrita, apaga **todas** as colônias do mundo em silêncio | Ler cada entrada em `try/catch (RuntimeException)`: registrar, descartar a entrada e seguir. Para `saveVersion < 0`, tratar como 0. Promover a sonda a `src/test` | `NbtFuzzProbeTest` com 12/12 verdes |
| AUD-03 | **P1** | `saveVersion` nunca chega ao disco | `SaveMigration.migrate` grava a chave só na cópia em memória (`SaveMigration.java:67-75`); `ColonySavedData.writeNbt` (`:363`) não a escreve; nenhuma ocorrência fora de `SaveMigration` e dos testes | Todo carregamento é tratado como v0 e roda v0→v1 de novo. Hoje é inócuo, porque `BREEDER→SHEPHERD` é idempotente. Mas a primeira migração v1→v2 que não for idempotente vai rodar a cada abertura de mundo | `writeNbt` grava `saveVersion = SaveMigration.CURRENT` | `ColonySavedDataTest`: `writeNbt(...).getInt("saveVersion") == CURRENT` |

## O que está certo, com teste

| Pergunta | Resposta | Evidência |
|---|---|---|
| Migração `BREEDER`→`SHEPHERD` testada com save antigo? | **Sim**, pelo NBT gravado com o nome antigo | `ColonySavedDataTest.breederFromOldSaveBecomesShepherdAndWritesShepherd` (`:246-261`); `SaveMigrationTest.legacyBreederProfessionBecomesShepherdBeforeAnyReaderRuns` (`:49-59`) |
| `MANUFACTURER` existe? | **Não.** Só em comentário (`core/worker/model/ProfessionType.java:36,44`); lido do disco, cai em `NONE` | `unknownProfessionFallsBackToNone` e o caso da sonda |
| Versão futura preservada? | Sim | `SaveMigrationTest.futureVersionIsLeftUntouched`; caso de controle da sonda |
| Estados persistidos fora do `ColonySavedData` | `ProfessionPolicySavedData`, `WorkMarksSavedData`, `WorkMemorySavedData`; `MineSave` tem a própria versão (`SHAPE_VERSION = 8`, `MineSave.java:113,134`) | — |

## Resultado bruto da sonda

```text
NbtFuzzProbeTest: 12 testes, 6 falhas
  [ok   ] controle: save válido
  [ok   ] controle: versão futura (saveVersion=99)
  [FALHA] saveVersion negativo — IllegalStateException: no migration registered from saveVersion -1
  [FALHA] building com blueprint de dois ':' — IllegalArgumentException: Ambiguous resource id
  [FALHA] building sem blueprint — IllegalArgumentException: path must not be blank
  [FALHA] building com blueprint em branco — IllegalArgumentException: path must not be blank
  [FALHA] projeto com blueprint de dois ':' — IllegalArgumentException: Ambiguous resource id
  [FALHA] projeto sem blueprint — IllegalArgumentException: path must not be blank
  [ok   ] profession com tipo trocado (int) · profession desconhecida · lista workers com string · centerX string
```

## Não verificado

- Os leitores NBT manuais fora de `data/save` (`MineVein`, `MineClaims`,
  `ShepherdHerding`, `FarmOwners`, `SurfaceHits`, `VillageTrees`, `WorkMemory`) não
  passaram pela sonda.
- Um save real antigo (anterior a 09-24) não foi aberto: não há um no repositório.
