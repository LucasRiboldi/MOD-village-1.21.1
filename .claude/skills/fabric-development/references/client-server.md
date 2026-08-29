# Client / Server

Todo mod Fabric é dois programas. A maior parte dos bugs "impossíveis" nasce de
tratá-lo como um só.

## Por que o singleplayer engana

O singleplayer roda um **servidor integrado no mesmo processo**. Cliente e
servidor compartilham memória, e o mesmo objeto Java acaba visível dos dois
lados. Código que lê estado do servidor a partir do cliente funciona — por
acidente.

Em servidor dedicado são processos separados. Só existe o que passou por packet.
O bug estava lá o tempo todo.

**`runClient` não é teste.** `runServer` também é obrigatório, e idealmente
cliente conectando a servidor dedicado.

## Os três territórios

| | Common | Client | Server |
|---|---|---|---|
| Entrypoint | `main` | `client` | `server` |
| Roda em | ambos | só cliente | só servidor dedicado |
| Contém | lógica, registros, dados | render, tela, input, som, keybind | específico de dedicado |

**Regra dura: código comum nunca referencia classe de cliente.** Nem import, nem
campo, nem assinatura, nem tipo de retorno. No servidor dedicado, o carregamento
da classe lança `NoClassDefFoundError`.

O ambiente de dev esconde isso porque frequentemente tem as duas metades no
classpath. `runServer` revela.

```bash
grep -rn "net.minecraft.client" src/main/java | head
```

Qualquer resultado aqui é suspeito — deveria estar no sourceset/entrypoint de
cliente.

## Autoridade

```text
CLIENTE                             SERVIDOR
detecta input
   └── envia INTENÇÃO ─────────────▶ VALIDA
                                       (permissão? distância? estado? recurso?)
                                          ↓
                                       EXECUTA
                                          ↓
                                       ALTERA ESTADO
   exibe   ◀──────────── SINCRONIZA ──────┘
```

O cliente **pede**. O servidor **decide**.

Nunca aceite:

```text
CLIENTE altera estado crítico → SERVIDOR aceita cegamente
```

Isso é bug e é vetor de trapaça. **Todo packet vindo do cliente é entrada não
confiável**: um cliente modificado envia qualquer coisa, a qualquer momento, com
qualquer valor.

Valide sempre: o jogador existe? está perto o bastante? tem o item? o estado
permite? o valor está na faixa?

## Identificar o lado

```java
// ✓ checa E ganha o tipo certo
if (world instanceof ServerWorld serverWorld) {
    ...
}

// funciona, mas não dá acesso à API de servidor
if (world.isClient()) return;
```

Melhor ainda: **exija `ServerWorld` na assinatura** dos métodos que mudam o
mundo. O compilador passa a impedir a chamada errada, em vez de um `if` que
alguém esquece.

```java
public void colher(ServerWorld world, BlockPos pos) { ... }
```

Ao ler um projeto, `ServerWorld` nas assinaturas é bom sinal: mostra fronteira
pensada. `world.isClient()` espalhado por toda parte é o contrário.

## O que já sincroniza sozinho

Antes de criar packet, verifique se o dado já viaja:

| Dado | Sincroniza |
|---|---|
| Block state | **sim** |
| `DataTracker` de entidade | **sim** |
| Componentes de item stack | **sim** (os sincronizáveis) |
| BlockEntity | **só se implementado** (`toUpdatePacket` / `toInitialChunkDataNbt`) |
| `PersistentState` | **não** |
| campo Java comum | **não** |

Packet para dado que o cliente já tem é tráfego desperdiçado **e** uma fonte de
dessincronia quando as duas vias divergem.

## Estado inicial

A pergunta mais esquecida: **o que o cliente vê antes da primeira
sincronização?**

```text
[ ] o jogador entra no mundo — o cliente sabe o quê?
[ ] o jogador reconecta — o estado é reenviado?
[ ] o chunk carrega — a block entity manda estado inicial?
[ ] o sync dispara só em mudança? então o estado inicial nunca chega
```

Sincronizar apenas em mudança deixa o cliente errado desde o começo até a
primeira alteração — que pode nunca vir.

## Cliente sem o mod

```json
"environment": "*"        // ambos — o padrão
"environment": "client"   // só cliente
"environment": "server"   // só servidor
```

Se o mod é server-side puro, ele **não pode** exigir que o cliente conheça ids
próprios nem receber packets customizados. Se exige, declare a dependência — o
handshake vai recusar cliente sem o mod, e isso é melhor que um erro obscuro.

## Threads

O handler de packet chega numa **thread de rede**. Mexer no mundo dali dá
corrupção intermitente — o pior tipo de bug.

```java
server.execute(() -> {
    // agora sim, na thread do servidor
});
```

Ver `networking.md`.

## Sintomas e causas

| Sintoma | Causa provável |
|---|---|
| `NoClassDefFoundError` no servidor | classe de cliente em código comum |
| funciona em SP, quebra em MP | autoridade ou sincronização |
| o cliente vê estado errado até interagir | falta sync inicial |
| o cliente vê estado errado permanentemente | sync ausente ou só em mudança |
| corrupção intermitente | mutação fora da thread do servidor |
| conexão cai no handshake | registro condicional / ids divergentes |
| trapaça possível | validação ausente no servidor |

## Checklist

```text
[ ] nenhuma classe de cliente em código comum
[ ] render/tela/keybind no entrypoint client
[ ] o servidor valida TODO packet do cliente
[ ] métodos que mudam o mundo exigem ServerWorld
[ ] o estado inicial chega ao cliente
[ ] reconexão reenvia o estado
[ ] mutação de mundo na thread do servidor
[ ] environment coerente com o código
[ ] TESTADO em runServer
[ ] TESTADO com cliente conectando a servidor dedicado
```

Detalhamento em `checklists/client-server.md`.
