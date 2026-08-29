# Workflow — construir um sistema

Modo **SYSTEM**. Para o que vai durar: um sistema de profissões, de construção,
de recursos, de automação. Decisões tomadas aqui ficam anos.

A diferença para `feature-development.md` não é tamanho — é que aqui **contratos
vêm antes de código**, e a implementação é incremental por definição.

---

## 1. Domínio

Antes de qualquer classe, descreva o sistema **em linguagem de domínio**, sem
Minecraft:

```text
ATORES       quem participa
ESTADO       o que o sistema sabe
DECISÕES     o que ele decide
AÇÕES        o que ele faz acontecer
INVARIANTES  o que nunca pode ser violado
```

Se você não consegue descrever o sistema sem citar `BlockPos`, ele ainda não é
um sistema — é um punhado de chamadas ao Minecraft.

### Invariantes

Escreva-as explicitamente. Elas são o contrato que o código precisa proteger:

```text
"Uma entidade não pode ter dois trabalhos simultâneos."
"Apenas o servidor modifica o estado."
"Uma tarefa não executa sem recursos."
"Todo baú reservado tem exatamente um dono vivo."
```

Invariante precisa ser **documentada, validada e protegida no código** — as três.
Documentada e não protegida é comentário; protegida e não documentada é
comportamento misterioso.

## 2. Pesquisa

Sistema grande **exige** pesquisa fechada. Acione `minecraft-code-research` em
modo DEEP para o que faltar:

```text
[ ] sistema Vanilla mapeado
[ ] ciclo de vida identificado
[ ] extension points levantados na ordem da escada
[ ] Fabric API verificada
[ ] outros mods consultados
[ ] riscos conhecidos
```

Implementar sistema sobre hipótese não validada é o caminho mais caro que existe:
a hipótese errada não aparece no primeiro arquivo, aparece no vigésimo.

## 3. Contratos

Preencha `templates/system-contract.md` para o sistema e para cada subsistema
relevante:

```text
PURPOSE               por que existe
RESPONSIBILITIES      o que faz
NON RESPONSIBILITIES  ← o que NÃO faz. Tão importante quanto.
INPUTS · OUTPUTS
STATE                 e quem é dono
LIFECYCLE
DEPENDENCIES
EVENTS
PERSISTENCE
NETWORKING
EXTENSION POINTS
INVARIANTS
FAILURE CASES
```

`NON RESPONSIBILITIES` é o campo que impede a God Class. Escrito no começo, ele
resiste; escrito depois, já não descreve o código.

## 4. Arquitetura

### Separação por domínio, não por tipo técnico

Conforme o sistema cresce, `entity/ item/ block/ util/` deixa de organizar
qualquer coisa — tudo fica em todo lugar. Prefira o domínio:

```text
mod/
├── registry/
├── villager/
│   ├── ai/  profession/  task/  data/
├── mining/
│   ├── logic/  data/  integration/
├── network/
├── client/
└── compatibility/
```

**Não aplique estrutura fixa cegamente.** Com poucos objetos, pasta por domínio é
cerimônia. A estrutura deve crescer com o projeto.

### O núcleo pode ser independente do Minecraft

Quando a regra de domínio é rica, vale separar:

```text
core/       regra pura — não conhece BlockPos, ServerWorld, ItemStack
fabric/     adapta: lê o mundo, escreve no mundo, converte tipos
adapter/    a fronteira, num lugar só
```

O ganho é concreto: o núcleo vira testável com JUnit, sem subir Minecraft. O
custo é a conversão de tipos. Vale quando a regra é complexa; não vale para um
bloco que solta partícula.

Ver `references/project-architecture.md`.

## 5. Plano de implementação

`templates/implementation-plan.md`. Quebre em passos que **compilam sozinhos**:

```text
PASSO 1  modelo de domínio + testes de unidade      → build
PASSO 2  registros                                   → build
PASSO 3  persistência (salvar/carregar vazio)        → build + save/load
PASSO 4  a lógica mínima ponta a ponta               → runClient
PASSO 5  integração com o Vanilla                    → gametest
PASSO 6  as demais capacidades, uma a uma            → build a cada uma
PASSO 7  client/networking, se necessário            → runServer
PASSO 8  performance                                 → medir
```

Cada passo tem um critério de "pronto" verificável. Passo sem critério não
termina — só cansa.

**Persistência cedo (passo 3), não no fim.** Descobrir no vigésimo arquivo que o
estado não cabe no formato escolhido custa a refatoração inteira.

## 6. Implementação incremental

```text
✗  escrever tudo → tentar compilar → 200 erros que se mascaram
✓  passo → build → passo → build → passo → rodar → validar
```

Regra prática: **se você não compila há mais de meia hora de trabalho, pare e
compile.**

## 7. Integração

Só agora, e só o necessário:

```text
REGISTRIES  → EVENTS → PERSISTENCE → NETWORK → RESOURCES → MIXINS
```

Mixin por último e mínimo (`mixin-workflow.md`). Em sistema grande a tentação de
Mixin é maior, e o custo de compatibilidade também.

## 8. Testes

```text
UNIT       o núcleo de domínio (rápido, sem Minecraft)
GAMETEST   a integração com o mundo
MANUAL     o que o gametest não alcança (worldgen, vila gerada, sessão longa)
```

```bash
./gradlew build
./gradlew runGametest
```

`references/testing.md`. Declare o que o gametest **não** cobre — o mundo de teste
é vazio, sem vila e sem estruturas.

## 9. Performance

Sistema grande com muitas entidades **precisa** de análise, não de impressão:

```text
Quantas entidades num mundo real?
Qual a frequência de cada operação?
Qual o raio de cada busca?
Há pathfinding? Com que frequência?
Há acesso a chunk? É seguro?
```

Teste mentalmente com 1, 10, 50, 100 e 500. `references/performance.md`.

## 10. Documentação

Sistema grande sem documentação é sistema que a próxima sessão vai reimplementar
por engano.

```text
[ ] contrato do sistema em docs/architecture/
[ ] ADRs das decisões que duram
[ ] invariantes registradas
[ ] o que ficou de fora, e por quê
[ ] research-status.md atualizado
```

## Fechamento

`checklists/architecture.md` e `checklists/feature.md`.

Um sistema entregue pela metade **com o estado declarado** é entrega. Entregue
pela metade em silêncio é dívida que alguém descobre da pior forma.
