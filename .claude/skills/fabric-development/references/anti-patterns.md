# Anti-padrões de implementação

Cada um traz o padrão, **por que ele atrai** (ninguém escolhe um anti-padrão de
propósito), como falha e o que fazer no lugar.

Os de *pesquisa* estão em `minecraft-code-research/references/anti-patterns.md`.

---

## Mixin First

**Atrai:** funciona na primeira tentativa e resolve exatamente o pedido. É a
ferramenta mais poderosa disponível.

**Falha:** `@Redirect` e `@Overwrite` são exclusivos — o segundo mod não roda. O
bug chega como "não funciona com o X", e você não reproduz.

**No lugar:** escada de extensão. Se o Mixin continuar necessário, escreva por
quê. Ver `workflows/mixin-workflow.md`.

---

## Tick Everything

**Atrai:** tick é o lugar mais fácil de fazer algo acontecer sempre.

**Falha:** 20×/s × N entidades. Um scan por aldeão por tick derruba o TPS com 20
aldeões — e o autor testou com dois.

**No lugar:** reativo > sob demanda > periódico > por tick. E **escalone** entre
entidades: picos são o que o jogador sente. Ver `performance.md`.

---

## Chunk Load Cascade

**Atrai:** `world.getBlockState(pos)` é a API óbvia e funciona nos testes, onde
tudo está carregado.

**Falha:** carrega o chunk que faltar. Do tick, é gerar terreno no laço; de dentro
de um evento de carga de chunk, **trava a thread**.

**No lugar:**

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

---

## Static World State

**Atrai:** acesso global sem passar referência por dez camadas.

**Falha:** o processo abre outro save sem reiniciar e o estado vaza. Nada é salvo,
então some no restart. Invisível em dev (o dev reinicia entre testes), certo em
produção.

**No lugar:** `PersistentState` para mundo, NBT para entidade/bloco. Se o acesso
global for mesmo necessário, limpe-o **no start e no stop** e documente a decisão.

---

## Serialization Assumption

**Atrai:** em muitos frameworks, campo é salvo automaticamente.

**Falha:** aqui não é. O campo some no restart, e o sintoma aparece muito depois
de o código ter sido escrito.

**No lugar:** par escrita/leitura explícito, e `markDirty()`. Ver `persistence.md`.

---

## Persistir o que o mundo já sabe

**Atrai:** parece mais confiável ter os dados "seus".

**Falha:** cria uma segunda verdade que envelhece. O save diz que há um baú em X;
o jogador quebrou o baú; o mod acredita numa coisa que não existe.

**No lugar:** existe no mundo → pergunte ao mundo. Só existe na cabeça do mod →
salve. E **registre a decisão de não persistir**, senão alguém "conserta" depois.

---

## Client Authority

**Atrai:** funciona perfeitamente no singleplayer.

**Falha:** no singleplayer os dois lados compartilham memória. Em servidor
dedicado vira dessincronia ou vetor de trapaça.

**No lugar:** cliente pede, servidor valida e executa, servidor sincroniza. Ver
`client-server.md`.

---

## Client Class Leakage

**Atrai:** compila e roda no ambiente de dev.

**Falha:** `NoClassDefFoundError` no servidor dedicado, no boot. Só quem hospeda
descobre.

**No lugar:** entrypoint `client` separado. `./gradlew runServer`.

---

## Packet Everything

**Atrai:** garantir que o cliente saiba.

**Falha:** banda desperdiçada e uma segunda via que pode divergir. Rede satura
antes da CPU e o sintoma **parece** lag de servidor.

**No lugar:** verifique o que já sincroniza sozinho — block state, `DataTracker`,
componentes. Ver `networking.md`.

---

## Registry Chaos

**Atrai:** "registro perto de onde uso".

**Falha:** ninguém consegue responder "o que este mod registra?" sem ler o projeto
inteiro. E registro condicional ou de ordem instável quebra multiplayer e saves.

**No lugar:** registro no entrypoint, incondicional, determinístico, agrupado por
categoria quando houver volume. Ver `registration.md`.

---

## Resource Blindness

**Atrai:** o código está pronto, então a feature está pronta.

**Falha:** cubo preto e rosa chamado `block.meumod.forja` que não dropa nada.

**No lugar:** `JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE`, os quatro elos.
Ver `datagen-and-resources.md`.

---

## Version Assumption

**Atrai:** o código do tutorial existe e aparentemente resolve.

**Falha:** três modos — não compila (barato); compila mas a semântica mudou
(caro); compila, roda e corrompe save (péssimo).

**No lugar:** `javap` antes de escrever. Ver `migration.md`.

---

## Silent Failure

**Atrai:** `catch (Exception ignored)` faz o crash sumir.

**Falha:** o sintoma aparece longe da causa, sem rastro. Quando alguém
investigar, não vai haver nada no log.

**No lugar:** degrade **e** logue. Ver `error-handling.md`.

---

## Log Spam

**Atrai:** mais informação parece melhor.

**Falha:** enche o disco, esconde o que importa e **muda o timing**, mascarando
bugs de concorrência.

**No lugar:** log de estado, não de passagem; nunca em laço quente sem limite;
"avisar uma vez" para condições recorrentes.

---

## God Manager

**Atrai:** cresce um método de cada vez, e cada passo parece razoável.

**Falha:** vira o ponto de acoplamento de tudo. Nada é testável isoladamente.

**No lugar:** responsabilidade por sistema, estado com dono. Escreva as
`NON RESPONSIBILITIES` no contrato — é o campo que resiste.

---

## Premature Framework

**Atrai:** "vai crescer, melhor já deixar genérico".

**Falha:** paga hoje, quase nunca usado. Interface com uma implementação e
`Factory` para três objetos são indireção sem retorno.

**No lugar:** abstração quando houver repetição, variação, extensibilidade real,
testabilidade ou separação de domínio — **já presentes**. Ver
`project-architecture.md`.

> God Class e Premature Framework são erros simétricos. Escolha pelo tamanho real
> do problema, não pelo que ele pode vir a ser.

---

## Feature Coupling

**Atrai:** o dado está ali, é só ler.

**Falha:** impossível mexer numa feature sem quebrar a outra. É como um mod fica
intocável.

**No lugar:** contrato explícito entre sistemas, ou estado com dono claro.

---

## Massive Refactor

**Atrai:** "já que estou aqui".

**Falha:** cinquenta arquivos alterados, um erro em qualquer um, diff ilegível,
revisão impossível.

**No lugar:** passos pequenos com build entre eles. Nunca misture refactor +
feature + migração. Ver `workflows/refactor.md`.

---

## Infinite Retry

**Atrai:** "ele vai conseguir na próxima".

**Falha:** o alvo pode ter sumido para sempre. A entidade fica presa gastando o
cálculo mais caro do jogo, sem progresso, parecendo quebrada.

**No lugar:** timeout, condição de desistência, e o que fazer depois de desistir.

---

## AI Every Tick

**Atrai:** "responsivo".

**Falha:** custo × população, e **oscilação** — a entidade troca de alvo entre
ticks e nunca chega a lugar nenhum.

**No lugar:** memória com validade, sensor com intervalo, cooldown. O Brain já é
feito disso; use o mecanismo em vez de contorná-lo. Ver `ai-development.md`.

---

## Pathfinding Spam

**Atrai:** garantir que o caminho está atualizado.

**Falha:** dos cálculos mais caros do jogo, repetido por tick, por entidade.

**No lugar:** recalcule só quando o destino muda ou o caminho falha. Em mobs de
Brain, escreva `WALK_TARGET` e deixe as tasks Vanilla trabalharem.

---

## Copy Tutorial Architecture

**Atrai:** o tutorial funciona e é de alguém que sabe.

**Falha:** tutorial otimiza para ensinar um conceito, não para o seu projeto. A
estrutura que cabe num exemplo de três classes não cabe em cinquenta — e a versão
quase nunca é a sua.

**No lugar:** entenda a ideia, avalie o contexto, escreva o seu.

---

## Declarar pronto sem executar

**Atrai:** compilou, a lógica está certa, deve funcionar.

**Falha:** metade dos problemas desta lista só aparece rodando — e vários só em
`runServer`.

**No lugar:** separe no relato **verificado rodando**, **tem teste escrito** e
**não verificado**. Nunca diga que passou sem ter executado.
