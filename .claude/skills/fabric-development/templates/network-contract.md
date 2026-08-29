# Contrato de packet — <NomeDoPayload>

> Um por packet. Preencha antes de implementar. Packet sem contrato não pode ser
> validado nem versionado.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Antes de tudo: é necessário?

```text
[ ] existe sincronização automática?   block state · DataTracker · componentes
[ ] o dado JÁ existe no cliente?
[ ] o cliente PRECISA MESMO deste dado, ou só a lógica do servidor precisa?
```

<Se algum for "sim", o packet provavelmente não deveria existir.>

## Identificação

| | |
|---|---|
| Nome | <específico — nada de `SyncPacket`/`UpdatePacket`> |
| Identifier | `meumod:<path>` |
| Direção | **C2S** (cliente→servidor) / **S2C** |

## Gatilho

<O que faz este packet ser enviado.>

## Payload

| Campo | Tipo | Faixa válida |
|---|---|---|

<Codec: **1.20.5+** usa `CustomPayload` tipado registrado em `PayloadTypeRegistry`.
Confirme na sua versão — tutoriais anteriores estão errados.>

## Validação — obrigatória em C2S

> Todo packet do cliente é **entrada hostil**. Um cliente modificado envia
> qualquer valor, a qualquer momento, quantas vezes quiser.

```text
[ ] o jogador existe e é ServerPlayerEntity
[ ] posição está dentro do alcance?
[ ] posição está em chunk carregado?
[ ] índice está dentro dos limites?
[ ] quantidade é não-negativa e dentro do máximo?
[ ] o id existe no registro?
[ ] a transição de estado é legal a partir do atual?
[ ] há permissão?
[ ] há limite de frequência? (packet válido em spam é DoS)
```

<Descreva a validação concreta:>

## Autoridade

<Quem decide o resultado. Em C2S, sempre o servidor.>

## Side effects

<O que muda no mundo/estado quando este packet é processado.>

## Thread

```text
[ ] validação barata na thread de rede
[ ] efeito no mundo via server.execute(...)
```

> Mutação fora da thread do servidor dá corrupção intermitente — o pior tipo de
> bug, porque não reproduz.

## Frequência

| | |
|---|---|
| Quantas vezes por segundo | |
| Por quem | jogador / entidade / bloco |
| Multiplicado por | |

<Rede satura antes da CPU, e o sintoma **parece** lag de servidor.>

## Estado inicial (S2C)

```text
[ ] o jogador entra no mundo — recebe o estado atual?
[ ] reconecta — recebe de novo?
[ ] o chunk carrega — recebe?
```

> Sincronizar só em mudança deixa o cliente errado desde a entrada até a primeira
> alteração — que pode nunca vir.

## Versionamento

```text
[ ] mudar o formato quebra compatibilidade entre versões do mod
[ ] cliente e servidor com versões diferentes: o que acontece?
[ ] a dependência está declarada no fabric.mod.json?
```

## Teste

```text
[ ] runServer com cliente conectando
[ ] payload inválido é recusado sem crash
[ ] valor fora da faixa é recusado
[ ] spam não derruba o servidor
[ ] o estado inicial chega
```
