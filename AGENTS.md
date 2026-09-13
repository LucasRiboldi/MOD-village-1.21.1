# AGENTS.md — Village Colony (MOD-village-1.21.1)

Instruções para agentes Codex neste repositório.

O arquivo canônico de regras é `CLAUDE.md` na raiz. Em divergência entre
este e o `CLAUDE.md`, **o `CLAUDE.md` vence** — exceto onde este arquivo
for mais específico sobre comportamento do Codex.

---

## 0. Antes de qualquer coisa — a ordem de leitura

O erro mais comum neste projeto é **ler demais**. A documentação é
excelente e longa, e o histórico passa de 4.600 linhas que **não**
precisam ser lidas para trabalhar.

**Ordem canônica, para qualquer tarefa:**

| # | Documento | Por quê |
|---|---|---|
| 1º | `STATE.md` | estado vivo — P0 aberto, o que espera verificação. **Leia só o topo, primeiras 60 linhas.** |
| 2º | `CLAUDE.md` §1 e §2 | as regras e o workflow |
| 3º | `docs/PATTERNS.md` | sintoma → causa → onde olhar. Consulte **antes** de investigar. |
| 4º | ADR ou doc do subsistema que você vai tocar | a decisão que governa |

**Depois disso, procure por trecho.** Nunca leia inteiro:

| Documento | Regra |
|---|---|
| `Development-Log.md` | histórico. Só `grep` por data ou símbolo. **Nunca inteiro.** |
| `docs/technical/Project-State.md` | histórico desde 2026-08-26. Use `docs/RULES.md`. |
| `Backlog.md` | histórico desde 2026-08-15. Use `TODO.md`. |
| `TODO.md` | vivo, mas ~700 linhas. Leia o topo (primeiras 100) e `grep` o resto. |
| `docs/technical/Plano-de-Correcao.md` | vivo. Leia a régua (§1) e o item atual. |
| `docs/technical/Development-Log.md` | histórico puro. Só `grep`. |

**Regra dura:** se você não sabe o que procura, você está lendo o
documento errado. Vá para `STATE.md` primeiro.

**O estado em uma linha:** MVP completo e verificado em jogo, 8
profissões funcionando. **O gargalo não é mais código — é verificação em
jogo e decisões do autor.** Detalhe em `STATE.md`.

---

## 1. Não comece criando classes

Não suponha arquitetura. Não simplifique decisões existentes. Não escreva
código antes de responder por escrito:
Qual problema está sendo resolvido?

Qual sistema é responsável?

Quais arquivos serão alterados?

Existe decisão arquitetural envolvida?


Se a resposta 4 for "sim" e não houver ADR que a cubra, **pare e pergunte**.
`PROJECT_CONSTITUTION.md §Amendment Policy` exige ADR registrada.

---

## 2. O que é este projeto

- Mod **Fabric** para **Minecraft 1.21.1**, Java 21, Gradle wrapper.
- Transforma vilas Vanilla em colônias autônomas: aldeões trabalham,
  produzem e constroem sozinhos, sem menu e sem o jogador.
- **Versões fixas** em `gradle.properties`. Nunca `latest`, `+` ou
  SNAPSHOT em dependência. Ver `docs/technical/Fabric-Version.md`.

### Regras invioláveis do código

| Regra | Onde | Teste que a guarda |
|---|---|---|
| **Core não conhece Minecraft.** `core/` nunca importa `net.minecraft.*`, `net.fabricmc.*`, `fabric.*` nem `data.*`. | ADR-005, ADR-006 §6 | `DependencyRuleTest` |
| **Um domínio do core não importa outro.** Exceção: `core/coordination`. | ADR-006 §6 (emenda 2026-08-08) | `DependencyRuleTest` |
| **Nada é inventado.** Recurso nunca aparece sem origem física no mundo. | `PROJECT_CONSTITUTION.md §9`, ADR-001 §6 | vários |
| **O mundo é a fonte da verdade.** Não persistir o que o Minecraft já guarda. | `Save-Data-System.md` | vários |
| **Nunca `@Overwrite` em mixin.** Só `@Inject` / `@ModifyVariable`. | ADR-004 §4 | — |
| **Arquivo até 500 linhas.** Indicador, não lei — mas justifique por escrito se passar. | `Plano-de-Correcao.md` | — |

---

## 3. Build, testes, verificação

```bash
# build + unitários — rápido (segundos)
./gradlew build

# só unitários
./gradlew test

# testes de jogo — servidor Fabric sem cliente — LENTO (minutos)
./gradlew runGametest

JAVA_HOME já vem do ~/.codex/config.toml. Se rodar num shell sem
ele, exporte explicitamente antes: o PATH da máquina tem Java 8 e o Loom
recusa sem Java 21.

O jar em downloads/ não é gerado pelo Gradle. É passo de mão; ver
docs/proxima-sessao.md §Antes de abrir o jogo antes de mexer nele.
Nomes de teste

Os testes de jogo usam nomes em camelCase minúsculo
(aVillageBecomesAColony, fellingReplantsASapling). Mantenha.
Antes de dizer que um conserto está pronto

    O teste que reproduz o defeito existe?

    Ele foi rodado contra a regra desligada e falhou? — PATTERNS.md §Um teste novo passa, mas não prova nada.

    ./gradlew build verde? ./gradlew runGametest verde?

    Você tem uma linha de log ou um arquivo que prova o comportamento,
    não uma dedução?

A régua do Plano-de-Correcao.md §1 vale sempre:

    Ter teste verde ≠ estar pronto. Todo conserto precisa ser visto em jogo.

4. Como trabalhar

    Antes de editar: rode ./gradlew build. Verde? Siga. Vermelho?
    Pare e relate.

    Uma tarefa por commit. Formato:
    P0.1: <o que mudou> (<teste que prova>). Não agrupe tarefas.

    Teste novo precisa de fase vermelha conferida. Desligue a regra,
    rode o teste, veja falhar com a mensagem certa.

    Ao terminar, atualize o que a documentação manda:

        STATE.md se o estado mudou.

        TODO.md se um item fechou ou abriu.

        Development-Log.md com a entrada da sessão.

        ADR nova, se houver decisão de arquitetura.

5. Onde você vai errar neste projeto

Estes são os modos de falha que este repositório já pagou caro para
aprender. Releia antes de começar.
Ler demais

O Development-Log.md tem mais de 4.600 linhas. O Project-State.md tem
1.400. Se você abrir inteiro, gasta 30k tokens para responder uma pergunta
sobre uma classe. Use grep.
bash

# exemplo — procurar uma decisão sem ler o log
rg -n "E44|stair|TreeMarks" docs/technical/Development-Log.md | head -20

Inventar causa de defeito

Todo o docs/PATTERNS.md existe porque alguém adivinhou em vez de
instrumentar. Se você não sabe, escreva SUSPEITA, não diagnóstico.
Sugerir refatoração fora do escopo

O projeto tem 10 ADRs aceitas. Se você acha que precisa de uma nova,
veja docs/decisions/ primeiro — provavelmente já foi decidido.
Traduzir nomes

O código é em inglês (LumberjackWork, ColonyState). A
documentação é em português. Mantenha os dois. Não traduza
identificador de código, não anglicize prosa de doc.
Mexer em Plano-de-Correcao.md ou TODO.md sem ler o topo

Eles dizem a ordem — e a ordem é decisão do autor, não sugestão.
Rodar o gametest inteiro quando só precisa dos unitários

runGametest leva minutos. test leva segundos. Se você tocou só em
core/, use test.
Copiar arquivo inteiro na resposta

Este projeto revisa por diff. Não entregue arquivo completo a cada
alteração — mostre o trecho que mudou.
6. Fluxos típicos
Implementar uma tarefa do plano
bash

codex --profile village-max "implemente P0.1-b do
  docs/technical/Plano-de-Correcao.md. Leia só o item, o teste que o
  cobre, e o arquivo que ele aponta. Rode o teste contra a regra
  desligada antes de dizer que está pronto."

Analisar antes de decidir
bash

codex --profile village-audit "compare as 10 ADRs em docs/decisions/
  com o código em src/main/java. Liste divergências com arquivo:linha
  e severidade. Não edite nada."

Explorar o estado sem risco
bash

codex --profile village-quick "leia STATE.md (topo, primeiras 60
  linhas) e me diga o P0 aberto e o que espera verificação em jogo.
  Não abra outro arquivo."

Investigar um defeito
bash

codex --profile village-max "o TODO.md lista o E44. Leia o item, o
  docs/PATTERNS.md §Alvo ruim é servido para sempre, e o TreeMarks.
  Proponha o conserto — não edite ainda."

7. Onde está o quê
Preciso de…	Arquivo
estado atual	STATE.md
regra do autor	docs/RULES.md
sintoma de defeito	docs/PATTERNS.md
decisão arquitetural	docs/decisions/ADR-*.md
o que fazer agora	TODO.md + docs/technical/Plano-de-Correcao.md
o que olhar em jogo	docs/proxima-sessao.md
princípios permanentes	PROJECT_CONSTITUTION.md
ponto de entrada canônico	CLAUDE.md
histórico	docs/technical/Development-Log.md (só grep)
8. Regra final

Cada linha de código, cada sugestão, cada conserto deve responder:

    "Isto aproxima o Village Colony de uma vila Vanilla realmente viva
    — sem substituir o Minecraft e sem inventar o que o mundo já guarda?"

Se não aproximar, provavelmente não pertence.


---

## 3. `src/main/java/AGENTS.md` (opcional, mas recomendado)

Escopo reduzido quando o Codex estiver mexendo em código Java. Reduz a chance de ele trazer contexto de docs para dentro de código.

```markdown
# AGENTS.md — código Java

Leia antes de editar qualquer arquivo neste diretório.

## Arquitetura

A fonte única do layout de pacotes é `docs/decisions/ADR-006-Package-Layout.md`.
Não repita a estrutura aqui; se você não tem certeza de onde uma classe
deve morar, leia a ADR.

Resumo operacional:

- `core/` — lógica pura. Nunca importa `net.minecraft.*`, `net.fabricmc.*`,
  `fabric.*`, `data.*`. Nunca importa outro domínio de `core/` (exceto
  `core/coordination`, que pode importar todos).
- `fabric/` — adapta o Minecraft ao Core. É a **única** camada que conhece
  `ServerWorld`, `VillagerEntity`, `BlockEntity`, `Inventory`.
- `data/save/` — serialização. Conhece NBT; não conhece regra de negócio.

Se você precisa violar qualquer uma destas três linhas, **pare e
pergunte** — não é decisão de agente.

## Regras de estilo

- **Uma responsabilidade por classe.** Se você está escrevendo `e também`,
  é hora de outra classe.
- **Arquivo até 500 linhas.** Se passar, justifique em comentário ou no
  commit. Refatore por responsabilidade, não por tamanho.
- **Javadoc em português.** Código e identificadores em inglês.
- **`Optional` em vez de `null`** para retorno que pode não existir.
  Ausência de valor é `Optional.empty()`, não valor sentinela de enum.
- **Nada de estado global novo.** Se precisar de um mapa estático, veja se
  já existe um equivalente antes de criar.
- **Não persista o que o mundo já guarda.** Antes de adicionar campo ao
  `ColonySavedData`, pergunte: *o Minecraft já responde isto?* Se sim, não
  grave.

## Mixin

Antes de escrever mixin, releia `docs/decisions/ADR-004-Mixin-Policy.md`.

- Superfície mínima e declarada. **Adicionar um mixin novo exige ADR.**
- Nunca `@Overwrite`. Só `@Inject` / `@ModifyVariable`.
- Nunca `ci.cancel()` em método de IA do aldeão.
- Mixin **não contém lógica**. Ele delega para `fabric/brain` ou
  `core/coordination`.
- Falha isolada: se a colônia não existe, não faça nada. Nunca lance
  exceção dentro de método Vanilla.

## Antes de commitar

```bash
./gradlew build          # unitários
./gradlew runGametest    # só se tocou em fabric/

Se um teste novo não falha contra a regra desligada, ele não está
afirmando nada. Ver docs/PATTERNS.md §Um teste novo passa, mas não prova nada.




---

## 4. `docs/AGENTS.md` (opcional)

O `docs/` é onde o Codex mais erra: ele lê demais. Um arquivo curto aqui evita isso.

```markdown
# AGENTS.md — documentação

## Regra de leitura

**Nunca leia um arquivo deste diretório inteiro.** Todos os grandes
(`Development-Log.md`, `Project-State.md`, `Backlog.md`,
`Plano-de-Correcao.md`) ultrapassam 500 linhas e a maior parte é
histórico que não muda decisão nenhuma.

Use:

```bash
rg -n "palavra" docs/                          # busca global
rg -n "palavra" docs/technical/Development-Log.md   # busca em um
sed -n '1,80p' STATE.md                        # topo de um vivo

Hierarquia de verdade

Onde dois documentos discordarem, vale o mais específico desta ordem:

    STATE.md — estado vivo (sobrescreve, não acumula)

    TODO.md — lista canônica de pendências

    docs/RULES.md — regras do autor

    docs/decisions/ADR-*.md — decisões de arquitetura

    docs/technical/Plano-de-Correcao.md — roteiro atual

    docs/PATTERNS.md — assinaturas de defeito

    Todo o resto

Backlog.md, Project-State.md e Development-Log.md são histórico
— registram o que aconteceu, não o que vale hoje.
Ao editar

    Não reescreva entrada antiga de histórico. Correção entra como
    entrada nova, com data; a antiga fica com o apontamento.

    Uma regra do autor não é renumerada quando emendada. A emenda é
    sufixada (27-e1).

    Toda regra nova do autor vai para docs/RULES.md e para
    Project-State.md §18 no mesmo commit.

    ADR nova exige número, status e data. Ver as existentes para o
    formato.

text


---

## Como o Codex carrega isto

~/.codex/AGENTS.md ← global, toda sessão
↓
<raiz-do-projeto>/AGENTS.md ← carregado ao abrir o projeto
↓
<raiz-do-projeto>/src/main/java/AGENTS.md ← carregado ao editar em src/main
↓
<raiz-do-projeto>/docs/AGENTS.md ← carregado ao editar em docs
text


O Codex **concatena** os arquivos que encontra do diretório-raiz até o diretório do arquivo sendo editado. Por isso o `docs/AGENTS.md` não precisa repetir a ordem de leitura — ele herda do raiz.

---

## Antes de aplicar

Três coisas a conferir:

1. **`CLAUDE.md` vs `AGENTS.md` — qual vence?** Eu deixei `CLAUDE.md` como canônico, com `AGENTS.md` referenciando-o. Se você preferir o contrário, inverte a hierarquia no topo de ambos, **nos dois**, para não deixar ambiguidade.

2. **`AGENTS.md` atual do seu projeto** — ele já existe e é bom. O que estou entregando é um **superconjunto** dele, mais específico para Codex. Vale substituir, não duplicar.

3. **`~/.codex/AGENTS.md`** — se você já tem um, mescle os hábitos gerais em vez de sobrescrever.

Feito isso, o par **`village-audit` (read-only) + `village-max` (workspace-write)**, com `AGENTS.md` em hierarquia, é o que mais vai te economizar tempo: você separa mecanicamente **analisar** de **implementar**, que é a disciplina que o próprio `CLAUDE.md §0.3` pede.