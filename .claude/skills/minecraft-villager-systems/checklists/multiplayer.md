# Checklist — multiplayer

> Toda a IA de aldeão é **server-side**. O que quebra em multiplayer costuma ser
> registro, sincronização de exibição, ou premissa de singleplayer.

## O básico

```bash
./gradlew runServer
```

```text
[ ] o servidor dedicado SOBE
[ ] o cliente conecta
[ ] nenhum NoClassDefFoundError no log do servidor
```

> Funcionar em `runClient` não é evidência de nada: no singleplayer os dois lados
> compartilham memória.

## Código

```text
[ ] nenhuma classe de cliente em código comum
[ ] a lógica de IA roda só no servidor
[ ] métodos que mudam o mundo exigem ServerWorld
```

```bash
grep -rn "net.minecraft.client" src/main/java | head
```

Todo resultado é suspeito.

## Registro — o que derruba a conexão

```text
[ ] POI registrado incondicionalmente
[ ] profissão registrada incondicionalmente
[ ] memórias e sensores registrados incondicionalmente
[ ] ordem determinística
[ ] nada depende de config para EXISTIR
```

> Registro condicional produz ids diferentes entre cliente e servidor, e a
> conexão cai no handshake. É o bug de multiplayer mais comum em mod de aldeão.

## Exibição

```text
[ ] profissão e nível aparecem corretos no cliente
[ ] a textura de roupa aparece
[ ] a tela de comércio mostra as ofertas certas
[ ] se o mod exibe algo próprio (nome, status), isso ATRAVESSA
```

## Estado inicial

```text
[ ] o jogador entra no mundo e vê o estado CORRETO, sem interagir
[ ] reconectar mantém correto
[ ] o chunk carregando traz o estado
```

> Sincronizar só em mudança deixa o cliente errado desde a entrada até a primeira
> alteração — que pode nunca vir.

## Autoridade

```text
[ ] o servidor valida toda ação vinda do cliente
[ ] o comércio é validado no servidor (itens, estoque, oferta existe)
[ ] o cliente não decide nada sobre o aldeão
```

## Vários jogadores

```text
[ ] a reputação é por jogador (UUID), não global
[ ] dois jogadores veem preços coerentes com a reputação de cada um
[ ] o herói da vila afeta só quem tem o efeito
```

## Comportamento

```text
[ ] os aldeões se comportam igual em dedicado
[ ] o ciclo do dia funciona
[ ] as profissões são atribuídas
[ ] o trabalho acontece
```

## Persistência em servidor

```text
[ ] reiniciar o servidor mantém o estado
[ ] o estado do mod carrega no SERVER_STARTED
[ ] e é salvo no SERVER_STOPPING
```

## Environment

```text
[ ] "environment" no fabric.mod.json bate com o que o código assume
[ ] se o cliente precisa do mod, a dependência está declarada
```

## Sintomas

```text
conexão cai no handshake      → registro condicional / ids divergentes
NoClassDefFoundError          → classe de cliente em código comum
o cliente vê estado errado    → falta sync inicial
funciona em SP, quebra em MP  → autoridade ou premissa de memória compartilhada
comércio dá item de graça     → validação ausente no servidor
```
