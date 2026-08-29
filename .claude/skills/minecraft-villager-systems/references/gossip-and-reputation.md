# Gossip e reputação

O sistema social do aldeão. É o que faz uma vila "lembrar" do jogador.

## Três coisas diferentes

Separe conceitualmente, porque misturá-las produz sistema paralelo desnecessário:

| | O que é | Escopo |
|---|---|---|
| **Gossip** | fofoca que se propaga entre aldeões | por aldeão, sobre um alvo |
| **Reputação do jogador** | soma dos gossips que afeta preço e comportamento | derivada |
| **Estado da vila** | população, raid, herói | por vila |

`[FATO]` MC 1.21.1: `net.minecraft.village.VillagerGossips` e
`net.minecraft.village.VillageGossipType`.

## O modelo

```text
Villager A  →  gossip (tipo, valor)  →  Villager B
      ↑                                      │
      └──── propaga no encontro (MEET) ──────┘
                    │
                    ▼
            reputação do jogador
                    │
                    ▼
        preço · comportamento do golem · raid
```

Os tipos e valores mudaram entre versões — **verifique na sua**:

```bash
javap -cp "$MC_JAR" net.minecraft.village.VillageGossipType
javap -cp "$MC_JAR" net.minecraft.village.VillagerGossips
```

## Propriedades de um gossip

```text
SOURCE      quem tem a opinião
TARGET      sobre quem (normalmente um jogador)
TYPE        que tipo de opinião
VALUE       intensidade
DECAY       enfraquece com o tempo
TRANSFER    propaga a outros aldeões, com perda
TRIGGER     o que o gera
```

Duas propriedades importam para desenho:

**Decay.** Gossip enfraquece. Um sistema que depende de gossip permanente está
lutando contra o mecanismo.

**Transfer com perda.** A propagação não é cópia: o valor diminui a cada
transmissão. Isso limita naturalmente o alcance — e é o motivo de a vila inteira
não reagir instantaneamente.

## Antes de mexer

```text
[ ] o Vanilla já tem um tipo que serve?
[ ] preciso mesmo de um tipo novo, ou de um gatilho novo para um existente?
[ ] a propagação e o decay atuais servem?
[ ] isto é gossip mesmo, ou é estado da MINHA vila/colônia?
```

A última é a que mais economiza trabalho. "A colônia sabe que este bloco está
reservado" **não é gossip** — é estado do seu sistema. Gossip é opinião social
sobre alguém.

## Não crie sistema paralelo

```text
✗  sistema próprio de reputação por jogador, ignorando o gossip
✓  usar o gossip Vanilla, ou declarar por que ele não serve
```

Sistema paralelo tem três custos: duplica o que existe, não se integra a preço e
golem, e é invisível para outros mods que leem reputação.

Se ele for mesmo necessário — porque a sua reputação é entre **aldeões e a
colônia**, não entre aldeão e jogador — então é outro conceito e merece outro
nome. Não o chame de reputação.

## Efeitos da reputação

```text
preço das trocas
comportamento do golem de ferro
início de raid
resposta ao herói da vila
```

Se o seu mod muda preço **e** gossip, os dois se influenciam. Mudanças em um
aparecem no outro, e isso costuma surpreender.

## Persistência

Gossips são salvos com o aldeão. Um aldeão morto leva as opiniões dele; um aldeão
novo começa neutro.

```text
[ ] cura de zumbi → identidade nova → gossips novos
[ ] o gossip do meu mod persiste? (se for tipo próprio)
```

## Multiplayer

Gossip é sobre jogadores específicos, por UUID.

```text
[ ] a reputação é por jogador, não global
[ ] jogador que sai e volta mantém a reputação
[ ] o cliente vê o preço correto (afetado por reputação)
```

## Checklist

```text
[ ] verifiquei os tipos de gossip da minha versão
[ ] não criei sistema paralelo sem justificativa
[ ] o que eu chamo de "reputação" é mesmo reputação, não estado do meu sistema
[ ] decay e transfer considerados
[ ] o efeito no preço foi avaliado
[ ] persistência verificada
[ ] testado com dois jogadores diferentes
```
