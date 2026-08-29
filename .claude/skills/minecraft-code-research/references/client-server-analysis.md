# Análise client / server

Todo mod Minecraft é, na prática, dois programas. Ignorar isso produz a classe de
bug mais frustrante que existe: **funciona perfeitamente no singleplayer e quebra
no multiplayer.**

## Por que o singleplayer engana

O singleplayer roda um **servidor integrado no mesmo processo**. Cliente e
servidor compartilham memória, e o mesmo objeto Java acaba visível dos dois lados.
Código que lê estado do servidor a partir do cliente funciona — por acidente.

Em servidor dedicado são processos separados, em máquinas diferentes. Só existe o
que passou por packet. O bug estava lá o tempo todo; o singleplayer só o escondia.

**Testar em `runClient` não é testar.** `runServer` também, e idealmente cliente
conectando a servidor dedicado.

## Os três territórios

| | Common | Client | Server |
|---|---|---|---|
| Onde | `src/main` | `src/client` ou entrypoint `client` | entrypoint `server` |
| Executa | ambos | só cliente | só servidor dedicado |
| Pode tocar | lógica, registros, dados | render, tela, input, som | lógica de servidor dedicado |

Regra dura: **código comum nunca referencia classe de cliente.** Nem em import,
nem em campo, nem em assinatura. O carregamento da classe no servidor dedicado
lança `NoClassDefFoundError` — e o dev não vê porque o ambiente de dev
frequentemente tem tudo no classpath.

O ambiente que revela é `runServer`.

## Quem manda

```text
CLIENTE                          SERVIDOR
detecta input
   │
   └── envia intenção ──────────▶ VALIDA
                                    │  (permissão? distância? estado? recurso?)
                                    ▼
                                 EXECUTA
                                    │
                                 ALTERA ESTADO
                                    │
   exibe    ◀────────── SINCRONIZA ─┘
```

O cliente **pede**; o servidor **decide**. O cliente pode prever localmente para
a resposta parecer imediata, mas a verdade vem do servidor e sobrescreve a
previsão.

Nunca aceite:

```text
CLIENTE altera estado crítico → SERVIDOR aceita cegamente
```

Isso é bug e é vetor de trapaça. Todo packet vindo do cliente é **entrada não
confiável** e passa por validação: o jogador existe? está perto o bastante? tem o
item? o estado permite? Um cliente modificado envia qualquer coisa.

## Identificar o lado no código

```java
if (world.isClient()) return;              // sai no cliente
if (world instanceof ServerWorld server) { ... }   // e ganha o tipo certo
```

O segundo padrão é melhor: além de checar, dá acesso à API de servidor. Métodos
que mudam o mundo devem receber `ServerWorld` na assinatura — assim o compilador
impede a chamada errada, em vez de um `if` em runtime.

Ao analisar um projeto, `ServerWorld` nas assinaturas é **bom sinal de
arquitetura**: mostra que a fronteira foi pensada.

## Sincronização — o que já vem de graça

Antes de criar packet, verifique se o dado já viaja:

| Dado | Sincroniza sozinho? |
|---|---|
| Block state | sim |
| Tracked data de entidade (`DataTracker`) | sim |
| Componentes de item stack | sim (os sincronizáveis) |
| Block entity | **só se implementado** (`toUpdatePacket` / `toInitialChunkDataNbt`) |
| Estado do mundo (`PersistentState`) | **não** |
| Campo Java comum | **não** |

Packet para dado que o cliente já tem é tráfego desperdiçado — e uma fonte de
dessincronia quando as duas vias divergem.

## Networking

Em **1.20.5+** a API mudou para payloads tipados: um record implementando
`CustomPayload`, com codec, registrado em `PayloadTypeRegistry` para a direção
certa. Tutoriais anteriores estão errados. **Confirme na sua versão** lendo
`fabric-networking-api-v1` (`fabric-analysis.md`).

Para cada packet, documente:

```text
NOME · DIREÇÃO (C2S/S2C) · GATILHO · PAYLOAD · VALIDAÇÃO
AUTORIDADE · SIDE EFFECTS · FREQUÊNCIA · IMPACTO DE VERSÃO
```

Duas armadilhas:

1. **Thread.** O handler chega numa thread de rede. Mexer no mundo exige voltar
   para a thread do servidor (`server.execute(...)`). Mutação fora da thread do
   jogo dá corrupção intermitente — o pior tipo de bug.
2. **Frequência.** Packet por tick por entidade satura a rede antes de saturar a
   CPU, e o sintoma parece lag de servidor.

Evite packets genéricos (`SyncPacket`, `UpdatePacket`, `GenericDataPacket`):
contrato explícito é o que permite validar e versionar.

## O que perguntar

```text
Quem possui o estado verdadeiro?
O cliente pode modificá-lo? (deve ser não)
O servidor valida o quê, exatamente?
Existe packet? Em qual direção?
Quando sincroniza — a cada mudança, periodicamente, sob demanda?
O que o cliente vê antes da primeira sincronização?
O que acontece ao reconectar? O estado é reenviado?
E se o cliente não tiver o mod?
```

A penúltima é a mais esquecida: jogador entra no mundo e o cliente não sabe nada
até o primeiro sync. Se o visual depende de dado só-servidor, ele aparece errado
por um instante — ou para sempre, se o sync só dispara em mudança.

## Sinais ao analisar

**Bom:** `ServerWorld` nas assinaturas que mudam o mundo; entrypoint de cliente
separado; validação explícita no handler; payload tipado com codec.

**Ruim:** classe de render importada em código comum; handler que escreve no
mundo direto da thread de rede; packet sem validação; estado importante em campo
estático compartilhado; `world.isClient()` espalhado por toda parte em vez de uma
fronteira desenhada.
