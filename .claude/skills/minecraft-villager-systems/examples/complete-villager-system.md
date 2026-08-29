# Exemplo — sistema completo, ponta a ponta

**Pedido:**

> "Quero um sistema de colônia: os aldeões se organizam sozinhos, ganham funções,
> coletam recursos e constroem."

Sistema grande. Este exemplo mostra as **três skills trabalhando juntas** e as
decisões que definem se ele vai durar.

---

## Fase 1 — Domínio, sem Minecraft

Descreva o sistema sem citar `BlockPos`. Se você não consegue, ainda não é um
sistema — é um punhado de chamadas.

```text
ATORES       colônia · trabalhador · recurso · construção
ESTADO       quem pertence a quê, quem faz o quê, o que existe
DECISÕES     atribuir função · escolher tarefa · reservar recurso
AÇÕES        coletar · transportar · construir
```

### Invariantes — escritas primeiro

```text
1. um trabalhador pertence a exatamente uma colônia
2. um trabalhador tem no máximo uma função
3. toda reserva tem exatamente um dono vivo
4. apenas o servidor modifica o estado
5. uma tarefa não executa sem recursos
```

**Documentadas, validadas e protegidas no código** — as três. Documentada e não
protegida é comentário.

## Fase 2 — Pesquisa (`minecraft-code-research`)

Sistema grande **exige** pesquisa fechada. Modo DEEP.

```text
[ ] como o Brain funciona
[ ] como POI e reivindicação funcionam
[ ] o que VillagerProfession contém (e o que não contém)
[ ] como o movimento é controlado
[ ] quais eventos avisam que um aldeão se foi
[ ] o que o Vanilla salva
```

Achados que **mudam a arquitetura**:

```text
[FATO] VillagerProfession é record e NÃO contém trades, tasks nem schedule
[FATO] setTaskList ACRESCENTA — nada precisa ser removido
[FATO] quem escolhe a Activity é a Schedule → Activity própria não ativa
[FATO] WALK_TARGET controla o caminho; navegação direta é sobrescrita
[FATO] zumbificação passa por MOB_CONVERSION, não por AFTER_DEATH
[FATO] getWorldChunk devolve null sem forçar carga
```

Cada um desses eliminou um desenho errado **antes** de virar código.

## Fase 3 — Escopo (esta skill)

### Colônia própria, ou vila Vanilla?

Não há objeto `Village` persistente no jogo moderno — a vila **emerge** de POIs
próximos, com limites fluidos.

> `[DECISÃO]` Conceito de colônia **próprio**, detectado a partir de camas e
> aldeões, persistido pelo mod. E **o nosso conceito não é o do Vanilla**: golem,
> raid e reprodução seguem as regras deles.

### Função: `VillagerProfession` ou papel do mod?

| | `VillagerProfession` | Papel próprio |
|---|---|---|
| Persistência | Vanilla salva | **nós salvamos** |
| POI/reivindicação | Vanilla gerencia | nós gerenciamos |
| Aparência | integrada | outro caminho |
| Compatibilidade | mods de aldeão reconhecem | invisível |
| Flexibilidade | limitada ao modelo | total |

> `[DECISÃO]` **Papel próprio**, porque a colônia decide quem faz o quê (limite
> por função), e o modelo Vanilla é "quem chega primeiro reivindica".
>
> **Consequência aceita e declarada:** nós persistimos o papel, e ele é invisível
> para outros mods de aldeão.

Registrar essa consequência é o que impede alguém de "consertar" depois.

### Escopo de cada estado

```text
individual   o que este aldeão faz agora        → memória
por POI      reivindicação de local              → Vanilla
por COLÔNIA  funções, reservas, obras, cursor    → PersistentState
por mundo    índice das colônias                 → PersistentState
```

## Fase 4 — Contrato

`templates/villager-system-contract.md`, com `NON RESPONSIBILITIES` preenchido:

```text
NÃO controla o ciclo do dia — a Schedule Vanilla faz isso
NÃO substitui a IA — acrescenta uma task
NÃO gerencia comércio
NÃO força carregamento de chunk
```

**É esse campo que impede a God Class.** Escrito no começo, resiste.

## Fase 5 — Plano incremental (`fabric-development`)

```text
1. modelo de domínio + testes de unidade      → build
2. detecção de colônia + persistência          → save/load
3. atribuição de função                        → runClient
4. a task base no Brain (uma só)               → gametest
5. um trabalho completo (lenhador)             → jogo
6. os demais trabalhos                         → build a cada um
7. construção
8. performance                                 → medir
```

**Persistência no passo 2, não no fim.** Descobrir no vigésimo arquivo que o
estado não cabe no formato custa a refatoração inteira.

## Fase 6 — Decisões que definem a robustez

### O que NÃO persistir

```text
profissão/papel atribuído  → SALVA (não existe no mundo)
id da colônia              → SALVA
cursor da varredura        → SALVA (senão a sessão recomeça)
fronteira da mina          → SALVA (senão ele recava o aberto)

posição do baú             → NÃO (existe no mundo, redescoberta)
alvo atual                 → NÃO (intenção do momento)
progresso da obra          → NÃO (legível olhando o que está de pé)
```

> Persistir o que o mundo já sabe cria uma segunda verdade que envelhece.
> **Registre a decisão de não persistir**, com o motivo.

### Estado global

```java
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    REGISTRO.clear();                     // limpar ANTES de carregar
    carregar(server);
});
ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    salvar(server);
    REGISTRO.clear();                     // e DEPOIS de salvar
});
```

Nos dois lados. O processo abre outro save sem reiniciar, e colônias vazam.

### Saída de trabalhador

```java
ServerLivingEntityEvents.AFTER_DEATH.register(...);
ServerLivingEntityEvents.MOB_CONVERSION.register(...);   // o caso mais comum
```

E a regra: **ausência na varredura não é morte.** Um aldeão fora do raio, ou
escondido durante um raid, não morreu.

Sem isso, o registro apaga a vila inteira durante uma incursão.

## Fase 7 — Um Mixin, uma vez

```java
@Inject(method = "initBrain", at = @At("TAIL"))
private void meumod$installTasks(Brain<VillagerEntity> brain, CallbackInfo info) {
    ColonyBrain.install(brain);
}
```

**Todas** as tasks do sistema entram por ele. Se aparecer um segundo Mixin em
`VillagerEntity`, o desenho errou.

E a degradação: exceção capturada → **um aldeão sem a task é um aldeão Vanilla**,
que é o estado de antes.

## Fase 8 — Performance

```text
[ ] varreduras incrementais com cursor persistido
[ ] trabalho escalonado entre aldeões
[ ] leitura de bloco sem forçar carga de chunk
[ ] pathfinding só quando o destino muda ou falha
[ ] limite de população
```

Testado com 1, 2, 10, 50, 100. O de **2** pega disputa; o de **50**, custo.

## Fase 9 — Regressão

`workflows/villager-regression.md`, e o item que importa:

```text
[ ] observar um aldeão por UM DIA INTEIRO:
    acorda · sino · trabalha · faz a coisa nova · socializa · volta · dorme
```

Um item faltando é regressão, mesmo que a feature funcione.

## Entregar

> **Sistema:** colônia autônoma com funções, coleta e construção.
>
> **Arquitetura:** núcleo de domínio independente do Minecraft (testável com
> JUnit) + camada de adaptação + **um** Mixin em `initBrain`.
>
> **Papel próprio, não `VillagerProfession`** — porque a colônia decide as
> funções. Consequência: nós persistimos, e é invisível para outros mods de
> aldeão. Declarado.
>
> **Não persistido deliberadamente:** posição de baú, alvo atual, progresso de
> obra — todos redescobríveis do mundo.
>
> **Verificado rodando:** `build`, `runGametest`, `runServer`, ciclo do dia
> completo, save/load, 50 aldeões.
>
> **Não verificado:** comportamento em vila gerada real além de uma sessão — o
> mundo do gametest é vazio.
>
> **Compatibilidade:** LOW. Um Mixin em `TAIL`, nada removido, degrada para
> Vanilla.

---

## O que este exemplo demonstra

1. **As três skills em sequência:** pesquisa fechou os fatos, o domínio escolheu
   as camadas, a implementação executou incrementalmente.
2. **Seis fatos verificados eliminaram seis desenhos errados** antes de virarem
   código.
3. **A escolha "papel próprio vs. profissão" foi feita com a tabela de custos** —
   e a consequência foi declarada, não descoberta.
4. **`NON RESPONSIBILITIES`** é o que impede a God Class num sistema deste tamanho.
5. **Persistência no passo 2.**
6. **Um Mixin, uma vez.** Dois seria sinal de que a arquitetura está lutando
   contra o Vanilla.
