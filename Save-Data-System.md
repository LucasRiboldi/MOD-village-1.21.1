# Save-Data-System.md

# Village Colony — Save Data System

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir quais informações da colônia devem ser persistidas no mundo Minecraft.

O sistema garante que, após fechar e abrir o mundo:

* a vila continue funcionando;
* construções permaneçam registradas;
* trabalhadores mantenham suas funções;
* tarefas possam continuar.

---

# Princípio Fundamental

O mundo Minecraft é a fonte física da verdade.

O mod salva apenas informações adicionais necessárias para reconstruir o estado da colônia.

```text
Minecraft World

↓

Blocos
Entidades
Inventários

+

Colony Data

↓

Estado completo da simulação
```

---

# Sistema de Persistência

A persistência utilizará o sistema nativo de dados do mundo Minecraft.

O mod armazenará dados associados ao mundo.

---

# Dados da Colônia

Cada vila registrada possui uma entidade lógica:

```text
Colony Data

- colonyId
- position
- villageType
- creationTime
- status
```

---

## Estado da implementação

Gravado hoje por `data/save/ColonySavedData`:

```text
id          UUID

centerX/Y/Z int

state       ColonyState
```

Ainda não gravado, porque os campos não existem no modelo `Colony`:

```text
villageType

creationTime
```

---

## lifecycle não é persistido

`ColonyLifecycle` é **estado derivado**, não estado salvo.

Ele depende apenas de o chunk estar carregado. Ao abrir o mundo nada
está carregado, então toda colônia volta como:

```text
DORMANT
```

Persistir `ACTIVE` produziria uma colônia marcada como simulável cujo
chunk não existe em memória — mentira sobre o mundo.

O que precisa sobreviver é o `ColonyState`: o que a colônia estava
fazendo. Isso é gravado e restaurado.

Ver ADR-002.

---

## Estado desconhecido no save

Um `state` que não exista mais no enum não derruba o carregamento do
mundo. A colônia volta como `STABLE` e reavalia no próximo ciclo.

Perder a intenção é aceitável. Impedir o jogador de abrir o mundo, não.

---

# Colony ID

Cada colônia recebe um identificador único.

Exemplo:

```text
colony_0001
```

Esse identificador permite:

* diferenciar vilas próximas;
* associar trabalhadores;
* associar construções.

---

# Localização

A colônia registra:

```text
- centerX
- centerY
- centerZ
```

Essa posição identifica a vila principal.

---

# Tipo da Vila

No MVP:

Apenas:

```text
PLAINS
```

será suportado.

Futuro:

* Desert
* Taiga
* Savanna
* Snowy

---

# Estado da Colônia

Salvar:

```text
Stable

Production

Expansion
```

---

# Trabalhadores

Cada aldeão registrado possui:

```text
Worker Data

- villagerUUID
- profession
- colonyId
- storagePosition
- currentTask
```

---

# UUID do Aldeão

O aldeão é identificado pelo UUID Vanilla.

Nunca usar:

* posição;
* nome;
* aparência.

O UUID permanece único durante a vida da entidade.

---

# Profissão

Salvar apenas a profissão da colônia.

Exemplo:

```text
villagerUUID:

a82f-55cd


Colony Profession:

Lumberjack
```

---

# Armazenamento

O mod não salva o conteúdo do baú.

Salva apenas:

```text
Storage Data

- ownerUUID
- chestPosition
- status
```

O inventário real permanece no bloco Chest do Minecraft.

---

# Construções

Cada construção concluída possui registro:

```text
Building Data

- buildingId
- colonyId
- structureType
- position
- rotation
- status
```

---

# Por que não salvar blocos?

Não salvar:

```text
x,y,z → block
```

Motivos:

* grande consumo de memória;
* duplicação do mundo;
* dificuldade de manutenção.

A construção é a unidade persistente.

---

# Estados da Construção

Salvar:

```text
PLANNED

PREPARING

WAITING_RESOURCES

BUILDING

COMPLETED
```

---

# Construções Originais da Vila

Não precisam ser salvas.

O Minecraft já gerou essas estruturas.

O mod apenas registra:

```text
Original Village Registry
```

quando necessário para proteção.

---

# Tarefas

Tarefas ativas podem ser salvas.

Modelo:

```text
Task Data

- taskId
- type
- colonyId
- assignedWorker
- status
```

---

# Tarefas que devem sobreviver

Exemplo:

```text
Construção de casa interrompida

↓

Fechar mundo

↓

Abrir mundo

↓

Continuar construção
```

---

# Tarefas temporárias

Não precisam ser salvas:

* procura de caminho;
* posição atual;
* animação;
* movimento.

Essas informações são reconstruídas.

---

# Recursos

O mod não salva quantidade de itens.

Exemplo:

Não salvar:

```text
Oak Log = 120
```

O valor será calculado:

```text
Baús reais

↓

Inventários reais

↓

Resource Registry
```

---

# Cache de Recursos

O registro de recursos é temporário.

Pode existir durante execução:

```text
Runtime Resource Cache
```

Mas não é persistente.

---

# Índice de Ruas

A exceção ao princípio, e ela é deliberada — 2026-08-27.

O índice de ruas **é** uma leitura do mundo, e o mundo é a fonte da
verdade. Ainda assim vai para o disco, gravado por
`data/save/RoadIndexSave` junto da colônia:

```text
Colony Roads

- colonyId
- from       o centro de onde o raio foi varrido
- columns    as colunas calçadas, x e z empacotados
```

## Por que esta leitura se grava

Porque refazê-la custa **16.641 colunas** — o quadrado de raio 64 —, e o
orçamento é de mil por passagem, uma passagem por ciclo. Dezessete
ciclos, oito minutos e meio, antes de a colônia saber onde procurar
lote. Sendo de sessão, toda entrada no mundo pagava a conta inteira, e a
sessão curta acabava dentro dela.

## Por que isto não contradiz o princípio

Porque o índice não é acreditado.

```text
o índice diz    onde vale a pena perguntar

o mundo diz     se ainda é rua
```

Cada coluna é reperguntada ao mundo quando visitada. O centro vai junto
para que um índice medido de outro lugar seja descartado em vez de
mentir. O que se grava é o **caminho até a pergunta**, e não a resposta.

## O que a fronteira recusa ao ler

```text
índice vazio            afirma "varri tudo e não há rua"

entrada sem dono        mapa de vila nenhuma

colônia desconhecida    mesma regra das casas e das minas

maior que o orçamento   não caberia numa passagem
```

Recusado, a colônia varre o quadrado — o comportamento de antes desta
versão.

---

# Recuperação após Carregamento

Ao abrir o mundo:

Fluxo:

```text
Load Colony Data

↓

Encontrar aldeões

↓

Encontrar construções

↓

Encontrar baús

↓

Reconstruir recursos

↓

Retomar tarefas
```

---

# Caso um Aldeão Morra

Ao detectar:

```text
Worker UUID inexistente
```

A colônia remove:

* trabalhador;
* armazenamento associado;
* tarefa ativa.

Depois:

```text
Criar necessidade de profissão
```

---

# Caso uma Construção Seja Perdida

Se uma construção registrada não existir mais:

A colônia marca:

```text
Building Invalid
```

No MVP:

Não reconstruir automaticamente.

---

# Compatibilidade com Multiplayer

O sistema deve funcionar:

* mundo singleplayer;
* servidor dedicado.

Os dados pertencem ao mundo, não ao jogador.

---

# Segurança dos Dados

O sistema deve evitar:

* salvar referências inválidas;
* depender de entidades carregadas;
* duplicar inventários.

---

# Fora do MVP

Não implementar:

* sincronização externa;
* backup automático;
* migração entre mundos;
* banco de dados externo;
* nuvem.

---

# Objetivo Final

Garantir que a colônia tenha memória.

O mundo guarda a realidade.

A colônia guarda conhecimento.

Ao retornar ao mundo:

```text
A vila continua viva.

Os trabalhadores continuam suas funções.

As construções permanecem.

A expansão continua.
```
