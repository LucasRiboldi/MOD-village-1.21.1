# Checklist — client / server

> Rode antes de dizer que funciona em multiplayer. **Funcionar no singleplayer não
> é evidência de nada** — ali os dois lados compartilham memória e o bug fica
> invisível.

## Separação de código

```text
[ ] nenhuma classe de cliente em código comum
[ ] render, tela, keybind e som no entrypoint client
[ ] nenhum import de net.minecraft.client fora do lado cliente
```

```bash
grep -rn "net.minecraft.client" src/main/java | head
```

> Todo resultado aqui é suspeito. No servidor dedicado, o carregamento da classe
> lança `NoClassDefFoundError` no boot — e o ambiente de dev esconde isso.

## Autoridade

```text
[ ] para cada estado, sei quem tem a verdade
[ ] o cliente PEDE; o servidor DECIDE
[ ] nenhum estado crítico é alterado pelo cliente
[ ] o servidor não aceita nada cegamente
```

## Validação de entrada

Todo packet C2S é **entrada hostil**:

```text
[ ] o jogador existe e é ServerPlayerEntity
[ ] posição dentro do alcance
[ ] posição em chunk carregado
[ ] índice dentro dos limites
[ ] quantidade não-negativa e dentro do máximo
[ ] id existe no registro
[ ] a transição de estado é legal a partir do atual
[ ] há permissão
[ ] há limite de frequência (packet válido em spam é DoS)
```

## Fronteira no código

```text
[ ] métodos que mudam o mundo exigem ServerWorld na assinatura
[ ] a fronteira foi desenhada, não improvisada com world.isClient() espalhado
```

> Exigir `ServerWorld` transforma um erro de runtime em erro de compilação. É a
> diferença mais barata que existe.

## Sincronização

```text
[ ] verifiquei o que JÁ sincroniza sozinho antes de criar packet
    (block state · DataTracker · componentes de item)
[ ] não criei packet para dado que o cliente já tem
[ ] block entity implementa toUpdatePacket, se o cliente precisa ver
```

## Estado inicial

> O item mais esquecido.

```text
[ ] o jogador entra no mundo — o cliente recebe o estado atual?
[ ] o jogador reconecta — recebe de novo?
[ ] o chunk carrega — a block entity manda estado inicial?
[ ] o sync não dispara SÓ em mudança
```

> Sincronizar só em mudança deixa o cliente errado desde a entrada até a primeira
> alteração — que pode nunca vir.

## Threads

```text
[ ] validação barata na thread de rede
[ ] efeito no mundo via server.execute(...)
[ ] nenhuma mutação de mundo direto da thread de rede
```

> Mutação fora da thread do servidor dá corrupção intermitente — o pior tipo de
> bug, porque não reproduz.

## Cliente sem o mod

```text
[ ] environment no fabric.mod.json bate com o que o código assume
[ ] se o mod exige o cliente, a dependência está declarada
[ ] se é server-side puro, não depende de ids próprios no cliente
```

## Verificação executada

```bash
./gradlew runClient
./gradlew runServer
```

```text
[ ] o cliente inicia
[ ] o SERVIDOR DEDICADO inicia          ← não é opcional
[ ] cliente conecta ao servidor dedicado
[ ] a feature funciona conectado
[ ] o estado aparece correto logo ao entrar
[ ] desconectar e reconectar mantém o estado correto
[ ] nenhum NoClassDefFoundError no log do servidor
```

## Sintomas a conferir

```text
[ ] NoClassDefFoundError no servidor  → classe de cliente em código comum
[ ] funciona em SP, quebra em MP      → autoridade ou sincronização
[ ] cliente vê estado errado até interagir  → falta sync inicial
[ ] cliente vê estado errado sempre   → sync ausente
[ ] corrupção intermitente            → mutação fora da thread do servidor
[ ] conexão cai no handshake          → registro condicional / ids divergentes
```
