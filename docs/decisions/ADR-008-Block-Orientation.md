# ADR-008-Block-Orientation.md

# Architecture Decision Record 008

# Village Colony — Orientação de blocos

**Status:** Accepted
**Date:** 2026-08-21
**Accepted:** 2026-08-21
**Decision Type:** Architecture / Core
**Implements:** TASK-046, E8, B4 do Backlog
**Amends:** ADR-005 (Core Type Isolation)

---

# 1. Context

A ADR-005 isolou o Core: ele fala de `ResourceId`, `ColonyPos` e
`ResourceType`, e não conhece nenhum tipo do Minecraft. É o que permite
469 testes unitários rodarem sem subir servidor.

O preço apareceu na construção. Um `BlueprintBlock` diz **que bloco** e
**onde**, e não diz **para que lado**. Escada, porta, cama, tocha de
parede e tronco têm lado, e o mod os punha no estado padrão:

```text
E8, aberto desde 2026-08-13

  a escada sobe para o norte, sempre
  a cama fica com a cabeceira ao norte, sempre
  o tronco fica em pé, sempre
```

**Duas correções parciais já entraram**, e as duas são geométricas:
a Regra 17 vira a porta para a rua, e `BuilderWork.facing` deduz o lado
da parede. Elas funcionam porque porta e parede têm uma geometria que a
casa revela. Escada, cama e tronco não têm — a informação existe **só no
arquivo**, e o leitor a joga fora.

O autor decidiu em 2026-08-15: **o Core aprende a falar de orientação.**
O que faltava era decidir **quanto**.

---

# 2. As três formas consideradas

```text
(a)  enum próprio de quatro direções, traduzido na fronteira

(b)  par (propriedade, valor) em texto, genérico

(c)  mapa paralelo fora do BlueprintBlock, na camada fabric
```

**(b) foi recusada** porque carrega strings do Minecraft para dentro do
Core — `"facing"`, `"axis"`, `"half"` — e é literalmente o que a ADR-005
proíbe. Cobriria tudo ao custo de deixar de ser Core.

**(c) foi recusada** porque cria uma segunda verdade: a planta em um
lugar, a orientação em outro, e nada obrigando as duas a andarem juntas.
O `furnished` que acabou de ser removido do save era exatamente essa
forma de erro.

---

# 3. Decision

**(a).** O Core ganha `Side` — que já existe — como parte de
`BlueprintBlock`:

```text
BlueprintBlock(offset, block, furniture, facing)

facing: Optional<Side>   NORTH SOUTH EAST WEST

vazio = "o arquivo não disse", e o jogo decide
```

**Quatro direções, e não seis.** Cima e baixo não aparecem em nada que a
casa de vila contenha: escada, porta, cama e tocha de parede são todas
horizontais. O dia em que uma peça pedir vertical, é uma ADR nova e não
um `Side` remendado.

**A tradução mora na fronteira**, nos dois sentidos:

```text
StructureBlueprintReader   Direction do arquivo  →  Side
BuilderWork.facing         Side  →  BlockState com a propriedade certa
```

O Core continua sem saber que existe `net.minecraft.util.math.Direction`.

## 3.1 O que fica de fora, e é assumido

```text
axis do tronco        stripped_oak_log deitado no eixo X
half da escada        escada de teto, virada para baixo
shape da escada       canto interno e externo
open/powered          porta já aberta na planta
```

Estes **continuam saindo no padrão**. São estado de bloco que não é
direção, e (a) foi escolhida sabendo disso: cobre o que a casa de vila
usa, e não cobre o resto.

**Como isso vai aparecer:** um tronco decorativo deitado sobe em pé, e
uma escada de canto sobe reta. É defeito visível e local — a casa fica
torta num detalhe, e não desabada.

## 3.2 A geometria vence o arquivo, e é de propósito

Onde a Regra 17 já decide — a porta, e a face da parede —, **a
geometria continua mandando**. O arquivo diz para onde a porta olhava na
vila que a Mojang gerou; a Regra 17 diz para onde ela deve olhar nesta
casa, que está numa rua diferente.

`facing` só é lido onde não há geometria que responda.

---

# 4. Consequences

**Ganha:** o E8 fecha para as peças que importam. A cama deixa de
apontar sempre para o mesmo lado, e a escada sobe para onde o arquivo
manda.

**Custa:** `BlueprintBlock` ganha um componente, e todo teste que o
constrói posicionalmente muda junto. O construtor de conveniência sem
`facing` continua existindo, e é ele que a maior parte dos testes usa.

**Emenda à ADR-005, e o precedente que ela abre:** o Core pode ganhar um
tipo próprio para um conceito do jogo **quando o conceito for fechado e
pequeno** — quatro direções são fechadas e pequenas. Não pode ganhar um
tipo que seja a forma do jogo com outro nome; a diferença entre `Side` e
um par `(propriedade, valor)` é exatamente essa.
