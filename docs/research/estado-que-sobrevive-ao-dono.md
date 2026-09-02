# Auditoria — "estado que sobrevive ao dono" nos sistemas de aldeão

**Data:** 2026-09-02 · **Modo:** auditoria, sem mudança de código ·
**Skill:** `minecraft-villager-systems`

O projeto nomeou esta classe de defeito em 2026-08-29, ao consertar duas
instâncias de uma vez: *ferramenta que sobrevive à profissão* e *destino que
sobrevive à tarefa*. A frase que ficou: **"uma ponta que só funciona se
disparar na hora certa não é invariante"**. Esta auditoria procurou uma
terceira instância.

## Inventário — todo registro em memória chaveado por dono

| Registro | Dono | Como é limpo | Veredito |
|---|---|---|---|
| `WorkTargets.TARGETS` | trabalhador | `forget()` e dispensa | ✅ |
| `MinerWork.JOBS` e as outras seis profissões | trabalhador | `forget()` e dispensa (as sete, nas duas listas) | ✅ |
| `WORKERS`, `STORAGES`, `TASKS` | trabalhador | `forget()` (morte e conversão) e dispensa | ✅ **parcial — ver o achado** |
| marca do baú (`ChestMarker`) | trabalhador | `unmarkChestOf` antes do `forget`, e na dispensa | ✅ |
| equipamento (`WorkerEquipment`) | profissão | **invariante** desde 08-29: *a mão combina com a profissão*, conferida a cada passagem | ✅ |
| `MineClaims.DIGGERS` / `STOOD_ASIDE` | colônia | **invariante**: `retainAll(working)` uma vez por ciclo | ✅ |
| `WaitingWork.WAITING_SINCE` | **projeto**, não trabalhador | `giveUp` e fim da espera | ✅ |

`[FATO]` Os dois caminhos de perda de dono cobertos por evento estão
completos e simétricos: `VillagerLifecycleHandler.onDeath` e `onConversion`
chamam os dois `unmarkChestOf` + `forget`, e `forget` solta tarefas, destino,
os sete trabalhos, o trabalhador e o armazém.

`[FATO]` A skill avisa que **zumbificação não é morte** — passa por
`MOB_CONVERSION`, não por `AFTER_DEATH`. O projeto já escuta os dois. Este
não é o buraco.

## O achado: o dono que some sem avisar

`[FATO]` Não existe nenhuma reconciliação de `WORKERS` contra os aldeões que
de fato existem — nenhum `retainAll`, `removeIf` ou varredura equivalente em
`WorkerService` ou `VillagerScanner`.

`[FATO]` As duas limpezas que existem são disparadas por **evento**
(`AFTER_DEATH`, `MOB_CONVERSION`) ou por **decisão da colônia** (dispensa).

`[INFERÊNCIA]` Um aldeão que desapareça sem passar por nenhum dos dois —
removido enquanto o chunk está descarregado, apagado por outro mod, por
comando, ou por região de mundo perdida — deixa para trás um **trabalhador
fantasma**: registrado, com profissão, segurando reserva de baú.

`[RISCO]` As consequências não são cosméticas, e o projeto já viu o sintoma
delas por outra causa: a vaga fica ocupada (há teto por profissão,
`MAX_PER_PROFESSION`) e o baú fica reservado. É a mesma forma da queixa
registrada na dispensa — *"a vila do autor tinha treze baús reivindicados e
quatro trabalhadores, e o fazendeiro não conseguia nenhum"*.

`[RISCO]` O fantasma **atravessa o save**: `ColonySavedData` grava os
trabalhadores, então ele volta a cada abertura do mundo, para sempre.

## Por que isto não tem conserto óbvio

`[FATO]` A regra que a própria skill enuncia: **ausência não é morte.**
Aldeão fora do raio ou em chunk descarregado não morreu — só não foi visto.
Podar `WORKERS` por "não achei a entidade" apagaria trabalhadores legítimos
toda vez que o jogador se afastasse da vila, e seria um defeito muito pior
que o fantasma.

`[VALIDAÇÃO NECESSÁRIA]` Uma reconciliação correta precisa de uma condição
mais forte que ausência. A forma que o `MineClaims` usa não serve aqui —
ele confere contra trabalhos abertos, que é informação do próprio mod;
aqui a pergunta é sobre o mundo. Candidatos, nenhum medido:

- podar só quando a entidade está ausente **e o chunk dela está carregado**,
  que é a única condição em que "não está lá" significa alguma coisa;
- exigir N observações consecutivas nessa condição antes de podar, para não
  depender de um único tique infeliz;
- não podar nunca, e em vez disso **documentar** o fantasma como custo
  aceito — o que também é resposta, desde que escrita.

`[DECISÃO]` **Nenhuma mudança de código nesta auditoria.** Escolher entre as
três é decisão do autor: a primeira e a segunda arriscam apagar trabalhador
vivo, e a terceira aceita um vazamento permanente. Auditoria que decide
sozinha o que fazer com risco desse tamanho passa do seu papel.

## O que a auditoria não cobriu

- Registros chaveados por **colônia** (`BuildSiteScanner`, `RoadExtension`,
  `RingSweep`, `SweepLog`, `ColonyStateLog`): a mesma pergunta vale para
  colônia abandonada, e não foi feita aqui.
- Memórias do **Brain do Vanilla** que o mod escreve (`WALK_TARGET` via
  `WorkTargets`): o mod as escreve, mas quem as expira é o Vanilla. Não foi
  verificado se alguma memória escrita pelo mod sobrevive à tarefa que a
  criou de um jeito que o Vanilla não limpe.

---

## Parte 2 — os registros por colônia, 2026-09-02

A metade que a auditoria acima deixou explicitamente de fora.

### O dono não morre, e isso muda a pergunta

`[FATO]` `ColonyService.remove(UUID)` existe e **nunca é chamado em
produção**. Só há `clear()`, no início e no fim do servidor. Colônia não é
removida: ela é marcada `ABANDONED`, e o `ColonyStateLog` registra que a
marca **também é desfeita** quando a sonda volta a achar vila.

`[INFERÊNCIA]` Não há estado órfão por colônia no sentido da Parte 1 — o dono
não some dentro da sessão. Os registros (`BuildSiteScanner.SWEEPS/ROADS/
BUILDING`, `RingSweep.NEXT_RING`, `RoadExtension.ENDS/JUST_PAVED/GROWING`,
`SweepLog.TALLIES`, `ColonyStateLog.FLIPS`) são limpos no `clearAll` do fim
de servidor, e no meio da sessão pertencem a colônias que continuam
existindo. Crescimento limitado pelo número de vilas detectadas.

### O que apareceu no lugar, e é maior

`[FATO]` `runColonyCycles` filtra por `colony.isActive()`, que é
**lifecycle** — chunk carregado —, e **não** por estado. Colônia `ABANDONED`
com chunks carregados roda o ciclo inteiro.

`[FATO]` Dentro do ciclo, `ConstructionPlanner.plan(overworld, colony)` roda
sem depender de a colônia ter trabalhador, e é ele que dispara a varredura de
lote.

`[FATO]` `BuildSiteScanner.MAX_COLUMNS = 1024`, e o teto é **por passagem**.

`[INFERÊNCIA]` Cada colônia abandonada com chunks carregados custa até 1.024
colunas de leitura por ciclo, e não compra nada: sem vila, não há o que
construir. O custo escala com o número de colônias abandonadas perto do
jogador.

`[RISCO]` Isto encosta no item que o `TODO.md` chama de decisivo — *"a
varredura acabando num ciclo… é o que decide se dá para ver qualquer outra
coisa"*. A decisão 8 já mediu o aperto: raio 64 são 16.641 colunas, e a
primeira varredura leva ~8,5 min de sessão.

### Por que não consertei

`[FATO]` Pular colônia `ABANDONED` no `runColonyCycles` **seria seguro
quanto à ressurreição**: quem desmarca é `judgeAbandonment`, dentro de
`detectFromColonyCenters`, que é outra passagem e não depende do ciclo de
trabalho.

`[RISCO]` Mas não é só a varredura que mora no ciclo. Uma colônia abandonada
pode ter trabalhadores registrados — aldeões continuam lá, a vila é que
deixou de ser reconhecida —, e pular o ciclo **congelaria esses
trabalhadores**: ninguém decide tarefa, ninguém deposita, ninguém dispensa.
Isso pode ser o certo (vila que acabou não deveria seguir construindo) ou o
errado (a marca de abandono oscila, e o E9 é exatamente sobre isso).

`[DECISÃO]` **Nenhuma mudança.** A escolha entre *pular o ciclo inteiro*,
*pular só o planejamento de obra* e *deixar como está* é do autor, e a
diferença entre elas é comportamento de jogo, não limpeza de estado. Uma
auditoria que muda o laço principal do mod sem sessão de jogo para conferir
passa do seu papel — e nesta mesma sessão um conserto meu, feito com
confiança, abriu uma regressão (o E34).
