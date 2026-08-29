# Exemplo — sincronizar dado do servidor para o cliente

**Pedido:**

> "O jogador precisa ver quantos trabalhadores a colônia tem, numa tela. Cria um
> packet."

O pedido já vem com a solução embutida. **A primeira coisa a fazer é verificar se
a solução é necessária** — packet é a resposta certa com menos frequência do que
parece.

---

## 1. É necessário?

```text
[ ] existe sincronização automática?   block state · DataTracker · componentes
[ ] o dado JÁ existe no cliente?
[ ] o cliente PRECISA MESMO deste dado?
```

| Dado | Já sincroniza? |
|---|---|
| contagem de trabalhadores | **não** — vive num `PersistentState` |

`PersistentState` não sincroniza. Então sim, precisa de packet.

> Se o dado fosse "a forja está acesa", a resposta seria **não** — block state
> resolve de graça. Vale gastar os trinta segundos da verificação.

## 2. O contrato

`templates/network-contract.md`:

```text
NOME        ColonyStatusPayload           ← não "SyncPacket"
DIREÇÃO     S2C
GATILHO     jogador abre a tela · mudança na contagem · jogador entra no mundo
PAYLOAD     colonyId (UUID) · trabalhadores (int) · vagas (int)
AUTORIDADE  servidor
FREQUÊNCIA  por mudança, não por tick
```

**Nome explícito importa.** `SyncPacket` não pode ser validado (o servidor não
sabe o que esperar), não pode ser versionado, e vira o lugar onde tudo é
empurrado.

## 3. A API mudou em 1.20.5

```bash
J=$(find . -name "fabric-networking-api-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
unzip -p "$J" net/fabricmc/fabric/api/networking/v1/PayloadTypeRegistry.java
```

Em **1.20.5+**, packets são **payloads tipados**: um record implementando
`CustomPayload`, com codec, registrado por direção.

**Todo tutorial anterior está errado.** Um minuto lendo os sources evita escrever
contra uma API que não existe mais.

## 4. A direção certa e o registro

Registro de payload é como registro de conteúdo: **no entrypoint, incondicional,
determinístico**, e registrado na direção correta (S2C e C2S são registros
separados).

Payload registrado só de um lado = a conexão cai ou o packet é ignorado.

## 5. O estado inicial — o item mais esquecido

```text
[ ] o jogador entra no mundo — recebe o estado atual?
[ ] reconecta — recebe de novo?
[ ] o sync dispara SÓ em mudança?
```

Se o sync dispara só em mudança, o cliente fica errado **desde a entrada até a
primeira alteração** — que pode nunca vir. O jogador abre a tela e vê zero.

> `[DECISÃO]` Enviar em três momentos: ao entrar no mundo, ao abrir a tela, e a
> cada mudança na contagem.

## 6. Se fosse C2S — validação

Este caso é S2C, mas se o jogador pudesse **pedir** algo (por exemplo, "demitir um
trabalhador"), a regra muda completamente:

```java
// no handler do servidor
if (!(context.player() instanceof ServerPlayerEntity player)) return;

if (!colonias.existe(colonyId)) return;                            // existe?
if (!player.getWorld().isChunkLoaded(centro)) return;              // carregado?
if (player.getBlockPos().getSquaredDistance(centro) > MAX_SQ) return; // perto?
if (!podeGerenciar(player, colonyId)) return;                      // permissão?
```

**Todo packet do cliente é entrada hostil.** Um cliente modificado envia qualquer
`colonyId`, de qualquer distância, quantas vezes quiser.

O item mais esquecido dessa lista é o **último**: um packet válido enviado mil
vezes por segundo é um DoS contra o próprio servidor. Cooldown por jogador
resolve.

## 7. Thread

```java
// ✗ mutação de mundo direto da thread de rede → corrupção intermitente
handler.receive((payload, context) -> {
    mundo.setBlockState(...);
});

// ✓
handler.receive((payload, context) -> {
    context.server().execute(() -> {
        mundo.setBlockState(...);
    });
});
```

Validação barata (formato, limites) na thread de rede; **efeito** na thread do
jogo. Corrupção por mutação fora da thread é o pior tipo de bug: não reproduz.

## 8. Frequência

```text
✗  packet por tick × colônias × jogadores
✓  packet quando a contagem muda
```

Rede satura antes da CPU, e o sintoma **parece lag de servidor** — o que manda a
investigação para o lado errado por horas.

## Validar

```bash
./gradlew build
./gradlew runServer
# em outro terminal
./gradlew runClient    # conectar ao servidor dedicado
```

```text
[ ] a tela mostra o valor correto AO ABRIR
[ ] o valor atualiza quando a contagem muda
[ ] entrar no mundo já mostra o valor certo, sem interagir
[ ] desconectar e reconectar mantém correto
[ ] o servidor não crasha com payload inválido
[ ] nenhum packet por tick no log de rede
```

O terceiro item é o que pega a falha de estado inicial — e é invisível para quem
testa entrando e mexendo logo.

---

## O que este exemplo demonstra

1. **O pedido trazia a solução; a primeira tarefa foi validá-la.** Aqui o packet
   era necessário; muitas vezes não é.
2. **A API de networking mudou em 1.20.5** — verificar os sources é obrigatório,
   não opcional.
3. **Estado inicial é um caso separado de "quando muda"**, e o mais esquecido.
4. **C2S é entrada hostil**, incluindo a frequência.
5. **Thread de rede não mexe no mundo.**
6. **Testar em servidor dedicado** é o único jeito de saber que funciona.
