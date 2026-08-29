# Análise de entidades

Uma entidade no Minecraft é duas coisas ao mesmo tempo: **um objeto autoritativo
no servidor** e **uma representação no cliente**. Confundir as duas é a origem da
maior parte dos bugs que só aparecem em multiplayer.

## O mapa

```text
Entity
├── Data           o que ela é (tracked data, componentes, NBT)
├── Attributes     saúde, velocidade, alcance
├── AI             Goal ou Brain (ver ai-brain-analysis.md)
├── Navigation     como se move
├── Targeting      o que persegue
├── Damage         como recebe/causa
├── Interaction    o que acontece ao clicar nela
├── Inventory      o que carrega (nem toda entidade tem)
├── Persistence    o que sobrevive ao save
├── Networking     o que o cliente vê
└── Lifecycle      spawn → tick → death → despawn
```

## Ciclo de vida

```text
spawn / load
    ↓
initialize      atributos, dados, IA
    ↓
tick            servidor: lógica · cliente: interpolação e animação
    ↓
ai tick         Goal ou Brain, só no servidor
    ↓
interaction     jogador clica
    ↓
damage
    ↓
death           drops, XP, eventos
    ↓
save / despawn
```

Duas distinções que precisam ficar firmes:

**Morte não é a única saída.** Uma entidade pode ser removida por despawn, por
descarga de chunk, ou **convertida** em outra. `[FATO]` em Fabric 1.21.1: aldeão
mordido por zumbi passa por `ServerLivingEntityEvents.MOB_CONVERSION`, **não** por
`AFTER_DEATH`. Um mod que só escuta morte perde o caso mais comum.

**Ausência não é morte.** Uma entidade fora do raio, ou num chunk descarregado,
não está morta — apenas não foi vista. Registro que decide "sumiu, então morreu"
a partir de varredura vai apagar entidades vivas. Só o evento serve como prova.

## Como as entidades são registradas

```text
EntityType<T> → Registry → Identifier → atributos → (cliente) renderer
```

Verifique na sua versão antes de escrever — a API de builder mudou entre versões.
Ver `registry-analysis.md`.

Um `EntityType` sem atributos registrados crasha no spawn; sem renderer
registrado no lado cliente, some ou crasha ao aparecer. São três registros
distintos e os três precisam existir.

## Dados da entidade — três mecanismos

| Mecanismo | Para que | Sincroniza? | Persiste? |
|---|---|---|---|
| **Tracked data** (`DataTracker`) | estado que o cliente precisa ver | sim, automático | não, por si só |
| **NBT** (`writeCustomDataToNbt` / `readCustomDataFromNbt`) | estado que sobrevive ao save | não | sim |
| **Campo Java comum** | estado só do tick atual | não | não |

O erro clássico é escolher um e esperar as três propriedades. Estado que precisa
aparecer no cliente **e** sobreviver ao save precisa de tracked data **e** de NBT
— são dois códigos, não um.

Em 1.20.5+ a serialização ganhou `RegistryWrapper.WrapperLookup` nas assinaturas.
Confirme com `javap` antes de escrever.

## O que investigar

Use `templates/entity-analysis.md`. As perguntas que mais rendem:

```text
Quem cria esta entidade, e por quais caminhos? (spawn natural, ovo, conversão, comando)
O que é inicializado onde? (construtor vs. initialize vs. primeiro tick)
Qual estado é autoritativo no servidor?
O que o cliente precisa saber, e como recebe?
O que sobrevive ao restart? Onde está o código que grava?
Quais eventos externos podem removê-la?
O que acontece se o chunk descarregar no meio de uma ação?
```

## Interação e autoridade

```text
Cliente detecta clique → envia intenção → SERVIDOR valida → executa →
altera estado → sincroniza → cliente exibe
```

O cliente **nunca** decide o resultado. Ele pode prever para a animação parecer
imediata, mas a verdade vem do servidor. Ver `client-server-analysis.md`.

## Estado de falha

Todo comportamento de entidade precisa prever:

```text
ALVO SUMIU · CAMINHO NÃO ENCONTRADO · RECURSO INDISPONÍVEL
CHUNK DESCARREGADO · ENTIDADE MORREU · JOGADOR INTERROMPEU
MUNDO MUDOU · SERVIDOR REINICIOU
```

**O mundo não é estático.** O jogador quebra o bloco que a entidade ia usar, o
chunk descarrega no meio do caminho, outra entidade pega o alvo primeiro. Um
comportamento sem saída de falha vira entidade travada tentando o impossível para
sempre — consumindo CPU e parecendo quebrada.

Para cada ação, defina: **timeout, condição de desistência, e o que fazer depois**.

## Performance

Entidades são numerosas. Antes de pôr qualquer coisa no tick:

```text
Quantas entidades desse tipo existem num mundo real? (não no seu teste com duas)
O custo é por entidade ou compartilhado?
Há busca de bloco ou de entidade? Com qual raio?
Há pathfinding? Com que frequência?
Isso pode ser reativo em vez de por tick?
```

Ver `performance-analysis.md`.

## Sinais ao analisar entidade de outro mod

**Bom sinal:** lógica fora da classe da entidade; estado com dono claro;
comportamento com timeout; `ServerWorld` explicitamente exigido nos métodos que
mudam o mundo.

**Mau sinal:** classe de entidade com mais de mil linhas; campo `static` guardando
estado por entidade; `world.getBlockState` em posição arbitrária dentro do tick;
nenhum tratamento para alvo ausente.
