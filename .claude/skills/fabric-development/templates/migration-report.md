# Relatório de migração — <versão antiga> → <versão nova>

> Preencha durante a migração, não no fim. Guarde em `docs/`.

**Data:** AAAA-MM-DD · **Branch:** <…>

## Versões

| | Antes | Depois |
|---|---|---|
| Minecraft | | |
| Yarn / Mappings | | |
| Fabric Loader | | |
| Fabric API | | |
| Loom | | |
| Gradle | | |
| Java | | |

## Estado de partida

```text
[ ] o build passava ANTES de começar
[ ] trabalho numa branch
```

## Quebras por camada

### Build

| Erro | Causa | Correção |
|---|---|---|

### API Fabric

| Classe/método | Mudança | Correção |
|---|---|---|

### Mappings

| Nome antigo | Nome novo | Verificado com |
|---|---|---|
| | | `javap` / `mappings.tiny` |

### Mixins

| Alvo | Status | Ação |
|---|---|---|
| | ok / alvo sumiu / descriptor mudou / ponto moveu | |

```text
[ ] nenhum aviso de mixin no log de boot
[ ] defaultRequire: 1 ativo
```

### Dados

| Formato | Mudança | Migração |
|---|---|---|
| NBT | | |
| caminhos de datapack | | |
| tags | | |
| Data Components | | |

### Runtime

> **A camada que o compilador não pega.** Nome igual, assinatura igual, semântica
> nova.

| Comportamento | O que mudou | Como descobri |
|---|---|---|

---

## Mudanças de semântica

> Separe do resto: são as que mais custam e as menos visíveis.

| Método | Nome/assinatura mudou? | Comportamento mudou? | Impacto |
|---|---|---|---|
| | não | **sim** | |

## Saves

```text
[ ] mundo criado na versão ANTERIOR abre na nova
[ ] o estado do mod voltou
[ ] nada foi apagado silenciosamente
```

<Se houve perda de dados, ela foi **decidida e declarada**? Onde?>

## Verificação

| Verificação | Resultado |
|---|---|
| `./gradlew clean build` | |
| `./gradlew runClient` | |
| `./gradlew runServer` | |
| `./gradlew runGametest` | |
| save antigo abre | |
| multiplayer | |

## Regressão comportamental

> A parte que quase todo mundo pula, e a razão de o build passar não bastar.

| Feature | Exercitada em jogo | Resultado |
|---|---|---|

```text
[ ] os comportamentos Vanilla que o mod preserva continuam
[ ] performance comparável
```

## Documentação atualizada

```text
[ ] gradle.properties
[ ] documento de ambiente/versões do projeto (no MESMO commit)
[ ] README (versões suportadas)
[ ] fabric.mod.json (faixas de depends)
```

## Riscos remanescentes

| Risco | Severidade | Mitigação |
|---|---|---|

## O que NÃO foi verificado

> Migração "concluída" que só compilou é migração não concluída. Declarar é mais
> barato que descobrir depois.

<…>

## Pendências

| Item | Prioridade |
|---|---|
