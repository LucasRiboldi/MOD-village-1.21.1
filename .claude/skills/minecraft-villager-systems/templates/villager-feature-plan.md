# Plano de feature de aldeão — <nome>

> Preencha antes de escrever código. **Corte as seções que não se aplicam**, com
> o motivo — "— não se aplica: <por quê>" é informação; seção vazia é ruído.

**Data:** AAAA-MM-DD
**Minecraft:** <versão> · **Mappings:** <…> · **Fabric API:** <…>

## Objetivo

<Uma frase, em linguagem de jogo. O que o jogador vai ver acontecer.>

## Experiência do jogador

<O que ele observa. Antes de qualquer decisão técnica.>

## A pergunta de escopo

> **Isto é identidade nova, ou capacidade nova?**

```text
[ ] identidade  → profissão + POI + task        (7 peças)
[ ] capacidade  → task + memória                (2 peças)
```

<Justifique. Criar profissão quando bastava comportamento é o erro mais caro do
domínio — e só aparece depois de tudo escrito.>

## Escopo

**Entra:**

**Não entra (non goals):**

## Camada afetada

```text
[ ] SENSOR      ele precisa notar algo
[ ] MEMORY      ele precisa lembrar de algo
[ ] ACTIVITY    modo novo            ← leia workflows/add-activity.md antes
[ ] TASK        ele precisa fazer algo
[ ] POI         local de trabalho
[ ] PROFESSION  identidade nova
[ ] SCHEDULE    horário
[ ] TRADES      comércio (sistema SEPARADO da profissão)
```

<Marque o mínimo. Cada peça marcada é custo.>

## Escopo de estado

```text
[ ] individual (memória)
[ ] por POI
[ ] por VILA           ← estado compartilhado mora aqui
[ ] por mundo
```

## Pesquisa existente

| Documento | O que estabeleceu |
|---|---|

**Lacunas que bloqueiam o desenho:**

- `[HIPÓTESE]` <…> → acionar `minecraft-code-research` antes

---

## Arquitetura

### Memórias

| Memória | Vanilla ou nova | Tipo | Codec | Quem escreve | Quem lê | Validade |
|---|---|---|---|---|---|---|

> `JOB_SITE`, `HOME` e `MEETING_POINT` são **`GlobalPos`**, não `BlockPos`.

### Sensores

| Sensor | Observa | Frequência | Área | Memória que escreve |
|---|---|---|---|---|

> Sensor **percebe**; task **age**. Nunca o contrário.

### Activity

<Qual Activity hospeda a task. `CORE` com condições próprias é quase sempre a
resposta — ver `workflows/add-activity.md`.>

### Tasks

| Task | Activity | Prioridade | Memórias exigidas (gate) | Início | Fim |
|---|---|---|---|---|---|

### POI / local de trabalho

<— não se aplica.>

### Profissão

<— não se aplica.>

### Schedule

<Uso a Vanilla? Se alterei, o que deslocou?>

### Pathfinding

<Como o aldeão chega. `WALK_TARGET` mantido, alcance, velocidade, desistência.>

### Estado e persistência

| Estado | Onde mora | Persiste | Por quê |
|---|---|---|---|

**O que deliberadamente NÃO persiste, e por quê:**

<Existe no mundo → pergunte ao mundo. Registre a decisão, senão alguém "conserta"
depois.>

### Client / Server

<Toda IA é server-side. O cliente precisa exibir algo? — não se aplica.>

---

## Casos de falha

| Situação | Comportamento |
|---|---|
| alvo sumiu | |
| caminho não encontrado | |
| recurso indisponível | |
| POI ocupado | |
| chunk descarregado | **caso normal** — pular |
| inventário/baú cheio | |
| anoiteceu | deixar a Schedule assumir |
| inimigo perto | deixar PANIC assumir |
| durante raid | ceder |
| aldeão morreu ou foi convertido | liberar reservas |
| servidor reiniciou | |

```text
[ ] TIMEOUT definido
[ ] limite de tentativas
[ ] o que fazer ao desistir
```

## Vários aldeões

```text
[ ] há disputa por recurso?
[ ] há reserva? com dono e validade?
[ ] a reserva é liberada na morte e na conversão?
[ ] a vaga reabre?
```

## Performance

| | |
|---|---|
| Nº de aldeões esperado | |
| Frequência de sensor | |
| Frequência de busca | |
| Raio | |
| Pathfinding | |
| Escalonado entre aldeões? | |

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH

```text
[ ] quantos Mixins? (mais de um em VillagerEntity é alerta)
[ ] alguma task Vanilla removida?
[ ] a Schedule foi alterada?
```

## Testes

```text
[ ] a feature funciona
[ ] o ciclo do dia Vanilla continua (acorda, sino, trabalha, socializa, dorme)
[ ] cede em PANIC e RAID
[ ] fechar e reabrir o mundo
[ ] servidor dedicado
[ ] 1, 2, 10, 50 aldeões
```

## Definition of Done

```text
[ ] pesquisa concluída          [ ] comportamento Vanilla compreendido
[ ] arquitetura definida        [ ] camada mínima escolhida
[ ] compila                     [ ] gametest passa
[ ] runClient e runServer       [ ] persistência validada
[ ] performance avaliada        [ ] casos de falha testados
[ ] regressão passou            [ ] documentação atualizada
```

> Marque só o que se aplica — mas com **verificação**, não intenção.
