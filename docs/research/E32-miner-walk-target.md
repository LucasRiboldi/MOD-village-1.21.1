# E32 — por que o mineiro não entra na própria escada

**Modo:** FORENSIC · **Data:** 2026-09-02 · **Objetivo:** investigar bug

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mappings | Yarn 1.21.1+build.3 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.15+1.21.1 |
| Conferido em | 2026-09-02, via `gradle.properties` |

## A pergunta

O E32 diz: *"o mineiro não entra na própria escada quando começa do lado
errado. Vizinho pisável existe, o `approachTo` aponta para ele, e o aldeão
continua estacionado a 4 blocos com `0/0 ticks`"*. O TODO registra a
suspeita — *"a navegação recusa descer no buraco de um bloco de largura"* —
e diz, corretamente, que é **suspeita, não diagnóstico**.

## A suspeita registrada estava errada

`[FATO]` A navegação **não** recusa buraco de um bloco. O que ela faz com um
alvo que não é chão está em `MobNavigation.findPathTo(BlockPos, int)`
(fonte Vanilla 1.21.1, gerado por `./gradlew genSources`):

```java
if (worldChunk.getBlockState(target).isAir()) {
    BlockPos blockPos = target.down();
    while (blockPos.getY() > this.world.getBottomY() && worldChunk.getBlockState(blockPos).isAir()) {
        blockPos = blockPos.down();
    }
    if (blockPos.getY() > this.world.getBottomY()) {
        return super.findPathTo(blockPos.up(), distance);   // alvo no AR: desce até o chão
    }
    ...
}
if (!worldChunk.getBlockState(target).isSolid()) {
    return super.findPathTo(target, distance);
}
BlockPos blockPos = target.up();
while (blockPos.getY() < this.world.getTopY() && worldChunk.getBlockState(blockPos).isSolid()) {
    blockPos = blockPos.up();                                // alvo SÓLIDO: sobe até sair da rocha
}
return super.findPathTo(blockPos, distance);
```

Duas regras, e elas são opostas:

| Alvo entregue à navegação | O que o Vanilla faz |
|---|---|
| **ar** | **abaixa** até o primeiro chão daquela coluna |
| **sólido** | **sobe** até o primeiro bloco não-sólido acima |

## A causa

`[FATO]` A ordem de cavar não é uma lista de lugares onde se fica de pé.
`MineShaft.stair()` abre três camadas por degrau
(`STAIR_HEADROOM = 3`) e a galeria abre duas (`HEADROOM = 2`):

```java
private static ColonyPos stair(ColonyPos top, Side towards, int i) {
    int step  = i / STAIR_HEADROOM + 1;
    int layer = i % STAIR_HEADROOM;          // 0 = pés, 1 e 2 = ar acima
    return new ColonyPos(..., top.y() - step + 1 + layer, ...);
}
```

`[FATO]` `Mine.cut` conta posição **entregue**, não bloco **cavado** —
`nextPosition()` é `shaft.positionAt(cut++)`, e `holdPosition()` só desanda
em algumas desistências.

`[FATO]` `MinerReach.stepAlongTheShaft` varre `i ∈ [0, cut)` e devolve
`at(shaft.positionAt(i))` **cru** como destino de caminhada.

`[FATO]` `MinerReach.legTowards(BlockPos, BlockPos, Optional<Mine>)` **não
recebe `ServerWorld`**. Por construção ele não pode perguntar ao mundo se o
destino é pisável.

`[FATO]` `MinerWork.approachTo` pergunta — `BuilderApproach.standable(world,
at)`, `MinerWork.java:435`. É a regra unificada de 2026-08-28: *"havia duas
definições de 'cabe um aldeão aqui'… uma conta só, e é a do construtor"*.

`[FATO]` Nada a jusante conserta: `GoToWorkTargetTask` só embrulha o alvo num
`WalkTarget`, sem validar.

`[INFERÊNCIA]` Quando o destino passa de `LEG = 8`, o `job.approach`
— que **é** garantidamente pisável — é substituído por um bloco cru da ordem.
Se esse bloco já foi entregue e **ainda não foi cavado**, ele é sólido, e o
Vanilla sobe o alvo até sair da rocha: dentro de uma mina, isso é a
**superfície acima dela**. O mineiro caminha para cima do próprio poço e fica
lá — sem `mine()` rodando, que é o `0/0 ticks` do relatório.

`[FATO]` É o sintoma que o TODO já tinha registrado em 2026-08-28, com número:
*"the miner is at 734, 66, 878, 20,5 blocks away… Y 66 é a superfície. Ele
estava vinte e um blocos em linha reta acima da galeria"*.

## O que isto **não** é

`[FATO]` As camadas de cabeça (2 de cada 3 posições da escada) **não** causam
o defeito: o Vanilla abaixa alvo no ar até o chão da coluna. Era a primeira
hipótese desta investigação e a leitura do fonte a derrubou.

## Decisão

`[DECISÃO]` Degrau **6 da escada de extensão (composição)** — nada de Mixin,
nada de tocar em Vanilla. O defeito é do mod: ele entrega à navegação um
destino que a sua própria regra classificaria como não-pisável.

O conserto é fazer a perna obedecer à mesma conta que o `approachTo` já
obedece: `legTowards` passa a receber o mundo e a filtrar candidatos por
`BuilderApproach.standable`, caindo para a boca da mina quando nenhum
candidato passa.

`[RISCO]` Ler o mundo dentro do laço custa: `stepAlongTheShaft` já varre até
`STEPS_SCANNED = 2000` posições. A filtragem deve rodar só sobre os candidatos
dentro da perna, não sobre a varredura inteira.

## Validação necessária

`[VALIDAÇÃO NECESSÁRIA]` Teste de jogo com a mina do save cujo `cut` está
adiantado em relação ao que está aberto — a forma exata do defeito — afirmando
que o destino entregue é pisável.

`[VALIDAÇÃO NECESSÁRIA]` Sessão de jogo: o E32 nunca foi visto depois de
nenhum conserto, e a bateria deixou de exercitá-lo quando a boca foi fixada.
