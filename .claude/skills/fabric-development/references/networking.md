# Networking

Packet é a única forma de informação atravessar a fronteira client/server. É
também uma superfície de ataque e um custo de banda — então a primeira pergunta é
sempre se ele é necessário.

## Antes de criar um packet

```text
É NECESSÁRIO?
Existe sincronização automática?     ← block state, DataTracker, componentes
O dado JÁ EXISTE no cliente?
O cliente PRECISA MESMO deste dado?  ← ou só a lógica do servidor precisa?
```

O que já sincroniza sozinho está em `client-server.md`. Packet para dado que o
cliente já tem é banda desperdiçada e uma segunda via que pode divergir.

## A API mudou em 1.20.5

Em **1.20.5+**, packets passaram a **payloads tipados**: um record implementando
`CustomPayload`, com codec, registrado por direção.

**Todo tutorial anterior está errado para você.** Confirme na sua versão:

```bash
J=$(find . -name "fabric-networking-api-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
unzip -p "$J" net/fabricmc/fabric/api/networking/v1/PayloadTypeRegistry.java
```

Ler os sources leva um minuto e evita escrever contra uma API que não existe mais.

## Contrato por packet

Preencha `templates/network-contract.md` para cada um:

```text
NOME              específico, não genérico
DIREÇÃO           C2S (cliente→servidor) ou S2C
GATILHO           o que faz ele ser enviado
PAYLOAD           os campos, tipados
VALIDAÇÃO         o que o servidor confere (obrigatório em C2S)
AUTORIDADE        quem decide o resultado
SIDE EFFECTS      o que muda
FREQUÊNCIA        quantas vezes por segundo, por jogador
IMPACTO DE VERSÃO o que quebra se o formato mudar
```

## Nomes explícitos

```text
✗  SyncPacket · UpdatePacket · GenericDataPacket · DataPayload
✓  ClaimJobSitePayload · ColonyStatePayload · OpenForgeScreenPayload
```

Packet genérico não pode ser validado (o servidor não sabe o que esperar), não
pode ser versionado, e vira o lugar onde tudo é empurrado. Contrato explícito é o
que permite validar.

## Validação — C2S é entrada hostil

```java
// no handler do servidor
if (!(context.player() instanceof ServerPlayerEntity player)) return;

if (!player.getWorld().isChunkLoaded(pos)) return;                    // existe?
if (player.getBlockPos().getSquaredDistance(pos) > MAX_DIST_SQ) return; // perto?
if (!player.getInventory().contains(ITEM_EXIGIDO)) return;            // tem?
if (!estadoPermite(pos)) return;                                      // pode?
```

Um cliente modificado envia qualquer valor, a qualquer momento, quantas vezes
quiser. Cada campo do payload é um valor que **você não controla**:

```text
[ ] posição: dentro do alcance? em chunk carregado?
[ ] índice: dentro dos limites do array/inventário?
[ ] quantidade: não-negativa? dentro do máximo?
[ ] id: existe no registro?
[ ] estado: a transição é legal a partir do atual?
[ ] frequência: dá para spammar? há cooldown?
```

O último é o mais esquecido: um packet válido enviado mil vezes por segundo é um
DoS contra o próprio servidor.

## Thread

**O handler chega numa thread de rede.** Mexer no mundo dali dá corrupção
intermitente — o pior tipo de bug, porque não reproduz.

```java
server.execute(() -> {
    // agora na thread do servidor
    mundo.setBlockState(pos, estado);
});
```

Faça a **validação barata** na thread de rede (formato, limites) e o **efeito** na
thread do jogo.

## Frequência

```text
✗  packet por tick, por entidade, por jogador
✓  packet quando o estado muda
✓  agregado: um packet com N mudanças em vez de N packets
```

Rede satura antes da CPU, e o sintoma **parece** lag de servidor — o que manda a
investigação para o lado errado.

## S2C: o estado inicial

```text
[ ] o jogador entra no mundo — recebe o estado atual?
[ ] o jogador reconecta — recebe de novo?
[ ] o chunk carrega — a block entity manda o estado?
```

Sincronizar **só em mudança** deixa o cliente errado desde a entrada até a
primeira alteração, que pode nunca vir. É um bug que não aparece para quem
testou entrando e mexendo logo.

## Versionamento

O payload é um contrato entre cliente e servidor de versões potencialmente
diferentes.

```text
[ ] mudar o formato quebra a compatibilidade entre versões do mod
[ ] cliente e servidor com versões diferentes: o que acontece?
[ ] há como recusar graciosamente em vez de crashar?
```

Declarar a dependência no `fabric.mod.json` faz o handshake recusar cedo — melhor
que um erro obscuro no meio da sessão.

## Checklist

```text
[ ] o packet é mesmo necessário
[ ] nome explícito, não genérico
[ ] payload tipado com codec (1.20.5+)
[ ] registrado na direção certa
[ ] TODO campo de C2S é validado
[ ] há limite de frequência onde faz sentido
[ ] efeito no mundo via server.execute
[ ] estado inicial e reconexão cobertos
[ ] testado em runServer com cliente conectando
[ ] contrato registrado em templates/network-contract.md
```
