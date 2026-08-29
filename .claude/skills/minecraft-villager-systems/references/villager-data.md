# Dados do aldeão e persistência

O que sobrevive, o que não sobrevive, e o que **não deveria** ser salvo.

## O que o Vanilla guarda

`[FATO]` MC 1.21.1 — `net.minecraft.village.VillagerData`, exposto via
`VillagerDataContainer`:

```text
VillagerType         bioma de origem (aparência)
VillagerProfession   a profissão
Level                nível de comércio
Experience           XP acumulado
```

E, junto com a entidade:

```text
inventário · gossips · trade offers · idade · saúde · posição
memórias COM CODEC
```

## O que o Vanilla NÃO guarda

```text
✗  memórias sem codec
✗  qualquer estado do SEU mod
```

**Nada do seu mod é salvo automaticamente.** Se o seu sistema atribui um papel,
uma colônia ou uma reserva ao aldeão, você precisa salvar — ou perde no restart.

## Memória com e sem codec

```java
// persiste
new MemoryModuleType<>(Optional.of(BlockPos.CODEC))

// só de sessão
new MemoryModuleType<>(Optional.empty())
```

Escolha conscientemente. Regra prática:

| Memória | Codec? | Por quê |
|---|---|---|
| local de trabalho reivindicado | sim | precisa sobreviver |
| alvo atual de uma ação | **não** | intenção do momento |
| cooldown em andamento | depende | reiniciar é aceitável? |
| identidade/vínculo | sim | não existe no mundo |

Ver `templates/memory-plan.md`.

## A decisão que define a robustez

```text
Existe no MUNDO?           → pergunte ao mundo, NÃO salve
Só existe na cabeça do mod? → salve
```

| Dado | Persistir | Por quê |
|---|---|---|
| profissão atribuída pelo mod | **sim** | não existe no Vanilla |
| id da colônia | **sim** | conceito do mod |
| posição do baú dele | **não** | existe no mundo, redescoberta |
| alvo atual de trabalho | **não** | intenção do momento |
| progresso de obra | **não**, em geral | legível olhando o que está de pé |

Persistir o que o mundo já sabe cria uma **segunda verdade que envelhece**: o
save diz que há um baú em X, o jogador quebrou, e o mod acredita numa coisa que
não existe. Redescobrir é mais barato que reconciliar.

> **Registre a decisão de NÃO persistir, com o motivo.** Sem isso, alguém
> "conserta" isso depois achando que foi esquecimento — e introduz o bug.

## Onde salvar o estado do seu mod

| Estado | Onde |
|---|---|
| por aldeão, pequeno | memória com codec, ou NBT da entidade |
| por colônia/vila | `PersistentState` do mundo |
| índice global | `PersistentState` |
| nunca | `static Map<UUID, ...>` |

### O anti-padrão número um

```java
// ✗ não persiste · não expira · não limpa na morte · vaza entre saves · briga com o Brain
private static final Map<UUID, BlockPos> ALVOS = new HashMap<>();
```

Memória registrada resolve os cinco problemas de uma vez.

Se um registro global for mesmo necessário (por exemplo, o índice das colônias),
ele precisa ser **limpo no `SERVER_STARTED` e no `SERVER_STOPPING`** — nos dois
lados, não em um só — e a decisão precisa estar documentada.

## Dados relacionados no mesmo arquivo

Se o trabalhador aponta para a colônia por id, **os dois no mesmo
`PersistentState`**. Arquivos separados podem ser gravados em momentos
diferentes; um restart no meio deixa trabalhador órfão apontando para colônia
inexistente.

**Não há transação entre arquivos.**

## Identidade

O UUID do aldeão é a chave natural. Duas armadilhas:

```text
[ ] cura de zumbi → UUID NOVO → o estado antigo não o encontra
[ ] aldeão em chunk descarregado → o UUID existe, a entidade não está carregada
```

A segunda importa muito: consultar por UUID e não achar **não significa que ele
morreu**.

## Migração

```text
[ ] há int de versão no NBT?
[ ] campos novos têm default?
[ ] save da versão anterior do mod abre?
```

Um `int` de versão custa nada e salva o futuro.

## Sincronização com o cliente

```text
profissão, nível, tipo   → o cliente vê (aparência, tela de comércio)
memórias                 → server-side
estado do seu mod        → só se o cliente precisar exibir
```

Se o seu mod mostra algo sobre o aldeão (nome, função, status), isso precisa
atravessar — ver `fabric-development/references/networking.md`.

## Checklist

```text
[ ] cada estado tem dono e mecanismo definidos
[ ] estado do mod é salvo (o Vanilla não salva por você)
[ ] memórias com codec onde precisa persistir
[ ] o que é redescobrível NÃO é persistido — e a decisão está escrita
[ ] nenhum static Map com estado de IA
[ ] dados relacionados no mesmo arquivo
[ ] registro global limpo no start E no stop
[ ] versão gravada no formato
[ ] cura de zumbi (identidade nova) tratada
[ ] TESTADO: fechar e reabrir o mundo
```
