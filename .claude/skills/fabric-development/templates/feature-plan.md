# Plano de feature — <nome>

> Preencha antes de escrever código. **Corte as seções que não se aplicam**, com
> o motivo — seção apagada com "— não se aplica: <por quê>" é informação; seção
> vazia mantida é ruído.

**Data:** AAAA-MM-DD · **Modo:** SMALL / FEATURE / SYSTEM / SURGERY
**Minecraft:** <versão> · **Mappings:** <…> · **Fabric API:** <…>

## Objetivo

<Uma frase. O que o jogador vai poder fazer. Se não cabe numa frase, o pedido
ainda não está entendido.>

## Experiência do jogador

<O que ele vê, faz e sente. Antes de qualquer decisão técnica.>

## Escopo

**Entra:**

**Não entra (non goals):**

> `Non goals` é o campo que impede a feature de crescer sozinha durante a
> implementação.

## Sistemas do Minecraft envolvidos

## Dependências Vanilla

## Dependências Fabric

| Necessidade | API / evento | Verificado em |
|---|---|---|

## Pesquisa existente

| Documento | O que estabeleceu |
|---|---|
| `docs/research/…` | |

**Hipóteses não validadas que este plano assume:**

- `[HIPÓTESE]` <…> → validar antes, ou o plano é adivinhação

---

## Arquitetura

**Qual é a menor arquitetura correta?**

<Resposta em uma ou duas frases, antes das seções abaixo.>

### Data

<O que a feature lê.>

### State

<O que ela guarda.>

### Ownership

<**Quem é dono de cada estado?** servidor · entidade · bloco · mundo · item>

### Lifecycle

<Quando nasce, quando morre. Em que ponto do ciclo de vida do mod isso entra.>

### Client

<O que o jogador vê. — não se aplica: <por quê>>

### Server

<O que é autoritativo.>

### Persistence

| Estado | Mecanismo | Sobrevive a |
|---|---|---|
| | campo / DataTracker / NBT / PersistentState / Component | tick / chunk / save / restart |

### Networking

| Packet | Direção | Payload | Validação |
|---|---|---|---|

<— não se aplica: o dado já sincroniza por <mecanismo>>

### Precisa de Mixin?

<Se sim, qual degrau da escada foi tentado antes e por que não bastou.
→ `workflows/mixin-workflow.md`>

---

## Passos de implementação

> Cada passo compila sozinho e tem critério de pronto verificável.

| # | Passo | Critério de pronto |
|---|---|---|
| 1 | | `./gradlew build` |
| 2 | | |
| 3 | | |

## Resources necessários

```text
[ ] lang
[ ] modelo / blockstate / textura
[ ] loot table
[ ] recipe
[ ] tags
[ ] item group
[ ] renderer (cliente)
```

## Riscos

| Risco | Tipo | Severidade | Mitigação |
|---|---|---|---|
| | técnico / compat. / performance / versão | | |

## Performance

<Frequência × população. Números, não adjetivos.>

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH — <justificativa em uma linha>

## Testes

| Nível | Aplica | Como |
|---|---|---|
| compile | | |
| game start | | |
| feature | | |
| save/load | | |
| multiplayer | | |
| regressão | | |
| performance | | |

## Definition of Done

```text
[ ] código implementado
[ ] compila
[ ] o jogo inicia (runClient)
[ ] o servidor dedicado inicia (runServer)
[ ] a feature funciona
[ ] edge cases testados
[ ] client/server validado
[ ] persistência validada (fechar e reabrir)
[ ] performance aceitável
[ ] resources completos
[ ] logs limpos
[ ] compatibilidade avaliada
[ ] documentação atualizada
```

> Marque só o que se aplica — mas marque com **verificação**, não com intenção.
