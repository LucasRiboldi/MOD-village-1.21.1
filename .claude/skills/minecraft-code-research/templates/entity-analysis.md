# Análise de entidade — <EntityType>

> Guarde em `docs/research/vanilla/` (entidade Vanilla) ou
> `docs/research/systems/` (entidade do projeto).

**Entidade:** `net.minecraft.entity.…` · **Id:** `minecraft:<path>`
**Minecraft:** <versão> · **Mappings:** <…> · **Data:** AAAA-MM-DD

## Pergunta

<O que esta análise responde.>

## Identidade

| | |
|---|---|
| Classe | |
| Superclasse | |
| Interfaces | |
| Categoria de spawn | |

## Ciclo de vida

| Etapa | Onde acontece | Observação |
|---|---|---|
| spawn / load | | quais caminhos? natural, ovo, conversão, comando |
| initialize | | atributos, dados, IA |
| tick (server) | | |
| tick (client) | | interpolação, animação |
| ai tick | | Goal ou Brain |
| interaction | | |
| damage | | |
| death | | |
| save / despawn | | |

> **Morte não é a única saída.** Despawn, descarga de chunk e **conversão** também
> removem. Em Fabric 1.21.1, zumbificação passa por `MOB_CONVERSION`, não por
> `AFTER_DEATH`.

## Atributos

| Atributo | Valor | Onde registrado |
|---|---|---|

## Dados

| Dado | Mecanismo | Sincroniza | Persiste |
|---|---|---|---|
| | DataTracker / NBT / campo | sim/não | sim/não |

> Estado que precisa aparecer no cliente **e** sobreviver ao save precisa dos
> dois — são dois códigos, não um.

## IA

**Sistema:** Goal / Brain / ambos
**Verificado com:** `javap -cp "$MC_JAR" <fqn> | grep -i "brain\|goal"`

<Se Brain, detalhe em `ai-system-analysis.md`.>

## Navegação

<Como se move. Em mobs de Brain, quem manda é a memória `WALK_TARGET`, não
`getNavigation().startMovingTo`.>

## Inventário

<Tem? Onde vive? Persiste? — não se aplica.>

## Persistência

| Estado | Escrito em | Lido em | Sobrevive ao restart |
|---|---|---|---|

## Networking

<O que o cliente precisa saber, e como recebe.>

## Registro

| Registro | Entrada | Onde | Obrigatório para |
|---|---|---|---|
| `ENTITY_TYPE` | | | existir |
| atributos | | | não crashar no spawn |
| renderer (cliente) | | | aparecer |

## Estados de falha

| Situação | Tratado hoje? | Consequência |
|---|---|---|
| alvo sumiu | | |
| caminho não encontrado | | |
| recurso indisponível | | |
| chunk descarregado | | |
| entidade morreu no meio | | |
| jogador interrompeu | | |
| servidor reiniciou | | |

> Comportamento sem saída de falha vira entidade travada tentando o impossível.
> Cada ação precisa de **timeout, condição de desistência e o que fazer depois**.

## Performance

| | |
|---|---|
| Quantas existem num mundo real | |
| Custo por tick | |
| Busca de bloco/entidade | raio, frequência |
| Pathfinding | frequência |

## Extension points

| Ponto | Viável | Degrau |
|---|---|---|

## Evidência

| Afirmação | Etiqueta | Fonte |
|---|---|---|

## Conclusão
