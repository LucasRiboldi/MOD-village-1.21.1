# A próxima sessão de jogo — o que olhar, e em que ordem

**Atualização de 2026-09-24, tarde — jogar com o spark ligado.** O JAR em
`mods` agora é o do commit `78e7efc` (SHA-256
`FC4273A291CF275B6985931B1711A8A2E261533E16DF49A1B48464A994E07F48`):
revisão de naturalidade, rodada de qualidade, refatoração e a **vila foco**
(só ela planeja; procure `Focus village is now`, `Planner turns` e a queda
de `Colony cycle took`) — nada disso visto em jogo ainda, e o
**spark** 1.10.109 está instalado ao lado dele.

- **Primeiro, o perfil.** Seguir `docs/technical/Profiling-spark.md`:
  `/spark profiler start --only-ticks-over 50`, jogar 5 a 10 minutos perto
  da vila, `/spark profiler stop` e trazer o link. Motivo: o log de 24-09
  teve 196 ciclos acima de um tique.
- **Depois, o que só o jogo mostra:**
  - um filhote nascer e ganhar ofício (N1, `shared supper with`);
  - a placa do lote 5 blocos acima do telhado (N7);
  - a escada da fuga tampada (N10, `finished backfilling`);
  - a roça ou outra oficina de ofício logo depois da primeira casa (N9).
- **No fim:** `python scripts/analyze_village_log.py` conta tudo isso no
  `latest.log` e atualiza `docs/technical/Log-Stall-Statistics.md`.

Os cinco playtests da Task 14, abaixo, continuam pendentes.


**Atualização de 2026-09-24 — cinco playtests da entrega de confiabilidade
operacional (Tasks 3 a 13).** Esta sessão implementou dez tasks do plano
`docs/superpowers/plans/2026-09-23-operational-reliability.md` (scanner
com política/custo separados, migração de save idempotente, recuperação
pura de mina, traço de atividade persistido, armazém físico por snapshot,
observação de inventário sem planejar, endurance com seed fixa, auditoria
de exclusão de obra) — tudo verificado por `./gradlew.bat test` (992/992,
zero falha), `./gradlew.bat build` (sucesso) e
`./gradlew.bat runGametest --rerun-tasks` (**423/423 GAME TESTS COMPLETE**
na rodada final). Duas tasks (9 e 10) foram investigadas e **não**
implementadas por decisão — o comportamento que pediam já existia sob
outro desenho; ver `STATE.md` e `TODO.md` para a evidência de cada uma.

**⚠️ O JAR já foi publicado sem os cinco playtests confirmados — decisão
explícita do autor, não o caminho que o plano original recomenda.** O
SHA-256 `C1244064EE1DEC8A03C65844FAC37193A0ABCB64E983E2D982C154EE0B935691`
está agora em `build/libs/`, `downloads/` e
`%APPDATA%/.minecraft/mods/village-colony-0.3.0.jar` — as três cópias
comparadas idênticas com o cliente fechado, e
`release_manifest.py --dry-run` confirmou o manifesto. O plano original
(Task 14 de `docs/superpowers/plans/2026-09-23-operational-reliability.md`)
manda fechar cada playtest só com confirmação observada do autor
jogando, e nenhum dos cinco foi observado ainda nesta sessão. Os cinco
itens abaixo continuam em aberto e devem ser verificados na próxima
sessão de jogo — o código já está instalado, então qualquer defeito
real vai aparecer direto no `latest.log` dessa sessão.

**Os cinco playtests que ainda faltam confirmar, com o JAR já instalado:**

1. Destruir o arco/portal da mina, disparar a recuperação técnica,
   recarregar o mundo — o arco deve continuar ausente (não reconstruir).
2. Esgotar uma mina finita — só uma boca oposta válida deve abrir uma
   mina nova; carvão deve ser preferido quando elegível.
3. Encher os baús públicos reconhecidos — o produtor deve reportar
   `NO_CAPACITY` sem perder item nenhum, e retomar o trabalho físico
   quando a capacidade for liberada.
4. Reabrir um mundo com `BigHouseMOD` já migrada, duas vezes seguidas —
   nenhum baú ou estrutura deve duplicar.
5. Exercitar a alternância casa/infraestrutura e inspecionar, no save,
   os eventos do traço de atividade (`VC_ACTIVITY` no log, e — se houver
   ferramenta de leitura do save — os eventos `IDLE`/`WAITING`/
   `RECOVERED`/`ABANDONED`/`ERROR` da Task 7).

O JAR já está instalado — o próximo passo é abrir o Minecraft e observar
os cinco itens acima. Se algum falhar, o `latest.log` dessa sessão é a
evidência real para reabrir o item correspondente no `TODO.md`.

---

**Escrito em 2026-09-02, atualizado em 2026-09-20.** Este arquivo existe
porque o gargalo do projeto deixou de ser código: havia **dez consertos do
mineiro empilhados sem uma única sessão que os veja**, e nenhuma pergunta
importante em aberto pode ser respondida sem abrir o jogo.

**Atualização de 2026-09-21 — lotes e cancelamento de obras.** A
`BigHouseMOD` continua sendo criada automaticamente com a vila e permanece
fora do catálogo das profissões. O scanner e a fundação recusam qualquer
bloco até 25 blocos acima da pegada escolhida, e uma Tocha das Almas dentro de
uma obra profissional cancela a zona, o projeto e as tarefas sem cancelar a
`BigHouseMOD`. `./gradlew.bat test` e `./gradlew.bat build` passaram; a bateria
completa executou 394 GameTests, com 393 aprovados e apenas o residual já
conhecido de `FarmPlanGameTest`.

**JAR desta atualização:** 0.3.0, SHA-256
`1F3D285805DF24DE97D7C41042CCEC7C7224F928926102CF74929B833E9FB504` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`; as três cópias
foram comparadas após a atualização.

**JAR atual da entrega P1.4 (2026-09-23):** 0.3.0, SHA-256
`62FCECB70ACF7864DA852F707A1ADBA2197FEC8613A952A2E691DE7BE13BF1EF` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`; as tres copias
foram comparadas com o cliente fechado. `./gradlew.bat --no-daemon build` e
`./gradlew.bat runGametest --rerun-tasks` passaram, com 929 testes unitarios e
418/418 GameTests. Este e o JAR para repetir o playtest P1.3 e confirmar P1.4:
caracol unico, area comum antes dos quatro ramais, carvao prioritario, portal
quebrado sem reconstrucao, boca oposta no limite mineravel, porta da
`BigHouseMOD` no nivel da rua, baus somente em quartos Vanilla validos de uma
vila nova e linhas `VC_ACTIVITY` no `latest.log`.

A sessão de 09-03 aconteceu e **o mineiro trabalhou** — a primeira boa. Ela
não zerou a pilha: fechou o que dependia de vê-lo cavar, e abriu quatro
coisas novas, que estão no **item 3-a**.

A de **09-04** durou 43 minutos, não teve crash, e rendeu noventa e nove
`WARN` que renderam cinco ciclos de conserto. Nenhum deles foi visto rodar,
e um sexto entrou depois dela — estão no **item 3-b**, que é por onde esta
lista deve começar agora. A ordem abaixo continua valendo para o resto.

Ele não é diário — quem conta a história é o `TODO.md`. Este aqui é a lista
de conferência de uma sessão.

**Atualização de 2026-09-20 — correção da planta da BigHouseMOD.** Toda vila adotada
deve nascer com seis funções fundacionais: mineiro, lenhador, pedreiro, fundidor,
criador e construtor. Cada titular deve ter uma cama `HOME` e um baú próprio
dentro da `BigHouseMOD`. Agricultor e carpinteiro continuam disponíveis no
crescimento normal, mas não ocupam os dois conjuntos removidos da casa. A
planta do mod continua sendo uma cópia editada da big house Vanilla, sem
móveis e decorações; a estrutura Vanilla permanece sem alteração. Foram
removidos os conjuntos que bloqueavam a porta e o acesso à escada, além dos
blocos de gerador que apareciam como blocos pretos. No jogo, entre em uma vila
recém-detectada, confirme a BigHouseMOD, conte os seis aldeões, confira as
camas e os baús e observe se nenhuma estrutura existente foi substituída.
O playtest ainda não foi observado nesta sessão.

**JAR anterior à correção final da BigHouseMOD:** 0.3.0,
SHA-256 `08927A5C6F04E54A94CEF33211A7880F856A2D449E2D617F2B1EDC1CA04FA911`;
foi substituído pelo artefato com seis conjuntos abaixo. A seleção continua alternando
`casa -> tipo não residencial A -> casa -> tipo não residencial B`, com o
mesmo scanner de zonas; a sequência completa no save continua como playtest.

**JAR atualizado após a correção final da BigHouseMOD:** 0.3.0, SHA-256
`1E4AB63F48B8591352481103B4CA0DBBB0B42AC0BEDE0330517F5FB3DD78C276` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`; as três cópias
foram comparadas após a atualização. `./gradlew.bat clean build` passou e a
segunda execução de `./gradlew.bat runGametest` passou com **384/384
GameTests**. A primeira execução repetiu uma falha intermitente já conhecida
do teste de coleta de terra fora do raio protegido.

**JAR publicado após o lote P0.9 em 2026-09-20:** 0.3.0, SHA-256
`C4848314C8E01CC1C04648B86472621FEAF85C4E12FF53828FBECA12EB82C210` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`; as três cópias
foram comparadas após a atualização. `./gradlew.bat build` e
`./gradlew.bat runGametest` passaram, com 387/387 GameTests. Falta conferir no
save a lista efetiva de estruturas, a rota da coleta de superfície e a entrada
seca da mina.

**JAR desta implementação em 2026-09-20:** 0.3.0, SHA-256
`0E0BAA033548135DB51BD2BFA5C8059AA823F9279FD2A28D498EE10F4F84AD6B` em
`build/libs/` e `downloads/`. A cópia em `%APPDATA%/.minecraft/mods/` ficou
pendente porque o cliente estava aberto pelo TLauncher durante a entrega.
`./gradlew.bat test compileGametestJava build` passou com 959 testes unitários;
`runGametest --rerun-tasks` executou 388 testes, com a regressão das dez árvores
verde e duas falhas residuais já registradas no `TODO.md`.

**JAR anterior em 2026-09-20:** 0.3.0, SHA-256
`2111E72B093FC007549946347C5948416061BC2774A10D1932F751BA46B8A7EA` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`. O cliente Minecraft
estava fechado durante a cópia. `build`, 954 testes unitários e
`runGametest` com 379/379 GameTests passaram. Este lote inclui a limitação da
perna real do mineiro ao próximo patamar escalável; a segunda obra e a entrega
de pedra ainda exigem playtest.

**JAR atual em 2026-09-15:** 0.3.0, SHA-256
`C5D0790F996082CE3B7D2AA55CED93936DF04063568A03B0B521F50245A0BA1A` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`. O cliente estava
fechado durante a copia. `build`, 828 unitarios e 327/327 GameTests passaram.
Ele inclui P0.7 e a liberacao imediata da claim quando um job mineiro encerra.
No jogo, confira que gravel e terracotta naturais aceitam lote, enquanto os
mesmos materiais dentro de `ROAD_AREA` continuam bloqueados; ao terminar ou
cancelar uma tarefa de mineracao, outro mineiro deve poder usar o ramal sem
esperar o proximo ciclo da colonia.

**JAR atualizado após a emenda ADR-012 (2026-09-14):** 0.3.0, SHA-256
`EF0138BE7180FC47FB905C42EF7F64A8A31FE68CFCFCBCE4C07CAF72DB467231` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`. `build` e
324/324 GameTests passaram. Falta confirmação visual no mundo.

**Atualização de 2026-09-14 — ADR-016, Lote 1 parcial.** Este JAR registra
IDs e quantidades reais de itens em baús, inclusive materiais fora do catálogo
tipado. Isso ainda não cria metas, tarefas ou execução para profissões; confira
o progresso das construções sem assumir que a cobertura genérica já funciona.
JAR 0.3.0 em `downloads/` e `%APPDATA%/.minecraft/mods/`, cliente fechado na
cópia. SHA-256: `F41920D7DA1FBEADA94D4F886F5047F7A3242F850011DE7DFA2F4A9CAE28DB77`.

**Reanálise do playtest de 09-14:** o log carregou um JAR anterior à coleta de
terra. Após instalar o novo artefato, observe a casa além dos 380 blocos, se o
mineiro pega carvão e ferro até formar o piso de 64, e se árvores naturais são
cortadas por inteiro sem remover peças de estruturas Vanilla ou da colônia.
Construções manuais feitas de troncos sem marcas não são identificáveis com
certeza pelo estado atual do mundo.

**JAR atualizado para esta sessão:** 0.3.0, SHA-256
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`. `build` e
322/322 GameTests passaram. Falta confirmação visual no mundo.

**Atualização de 2026-09-13.** A sessão do autor mostrou casa parada no
meio, mineiro invisível/parado, lenhadores e fazendeiros funcionando. Já
entraram quatro correções para a próxima sessão: mina desce quando o poço
partilhado fica fechado antes da bifurcação; lenhador mira um ponto de pé
ao lado da árvore em vez do tronco; `SweepLog` deixou de acusar ciclos que
saíram antes de pedir lote; `ABANDONED` precisa de duas leituras positivas
seguidas para voltar a `STABLE`, reduzindo o E9. O jar copiado em
`downloads/` e em `%APPDATA%/.minecraft/mods` tem MD5
`CD29B5F8BC61871BCE74A910C57DB28E`.

O novo GameTest encontrou terra comum de blueprint sendo colocada sem
estoque; agora ela exige item no baú. Na obra real, observe se terra aparece
sem material disponível e se a construção continua quando o estoque existe.
Para a mina, `latest.log` repetiu `hit stone with nowhere to stand` antes de
fechar e reabrir a mesma descida. E45 segue sem correção até decisão/ADR;
registre se isso se repete com este jar, sem tratar a atualização como
correção da geometria.

**Atualização 2026-09-13 — depósitos por profissão:** baús pessoais recebem
a produção do próprio trabalhador; a retirada de insumos continua
compartilhada. Verifique se minério fica na boca da mina conforme a Regra
30, e lingotes/tábuas/blocos transformados ficam no baú do produtor. Nenhum
baú deve receber produção de outro ofício. Quando o baú pessoal estiver
cheio, confira o drop no chão e recolha-o; resíduos como gravetos e maçãs
continuam sem consumidor (E38). A casa e a mina permanecem bloqueadores
principais, sem correção neste lote.

**Atualização 2026-09-13 — releitura do mundo (ADR-012):** depois de
colocar/quebrar blocos perto da vila, dê tempo para o scanner limitado reler
a área e confira se a obra encontra os novos lotes/estradas. Se abrir espaço
sobre trecho já escavado da mina, observe se o mineiro retoma daquele braço.
Registre posição e mensagens do `latest.log` se continuar parado; esta
correção não cria ramais novos nem altera E45. Baús fora dos vínculos atuais
não entram no estoque automaticamente.

**Atualização 2026-09-13 — coleta do fundidor (ADR-014):** confira se ele
recebe pá de ferro com Toque Suave I, busca areia quando faltar vidro e
armazena os drops no próprio baú. Para `grass_block`, observe se só inicia
com uma construção que demande o material e se a coleta acontece além de 64
blocos, no setor mais afastado das estruturas da vila. A proteção também é
verificada a cada alvo; registre qualquer bloco de estrutura removido.

**JAR da sessão anterior (09-13):** atualizado após ADR-014/015. SHA-256
`E41063395BFC20A8D5D3182A9732B6FAF1E7265E96446E8909F32A623B1BE1D9`;
`downloads/village-colony-0.3.0.jar` e `%APPDATA%/.minecraft/mods/` têm
bytes idênticos. O cliente não estava aberto durante a cópia.

---

## Antes de abrir o jogo

**A armadilha que já custou uma sessão inteira.** Em 2026-08-29 a sessão
rodou com o jar do dia anterior: o Minecraft estava aberto segurando o
arquivo, a troca falhou em silêncio, e o `.jar.new` ficou ao lado sem ser
aplicado. A sessão mostrou defeitos — e **nenhum dos consertos daquele dia
estava rodando**.

**E um degrau antes dela, que mordeu em 2026-09-04.** O jar de
`downloads/` **não é gerado pelo Gradle** — nenhuma tarefa o copia. Ele é
passo de mão, e por isso envelhece em silêncio: naquele dia o arquivo para
o qual o README aponta estava uma correção atrás do código. Copiar um jar
velho com o Minecraft fechado direitinho não adianta nada.

Ao pedir **commit e push**, o autor também pede a atualização local do JAR
(Regra 33): esta sequência passa a ser obrigatória nessa entrega.

0. `./gradlew build`, e **copie `build/libs/village-colony-0.3.0.jar` para
   `downloads/`**. Se `git status` não acusar o jar como modificado, é
   porque ele já estava em dia — e não porque a build falhou.
1. **Feche o Minecraft** antes de copiar o jar.
2. Copie `downloads/village-colony-0.3.0.jar` para a pasta `mods`, apagando
   o jar antigo.
3. Compare o SHA-256 das três cópias (`build/libs/`, `downloads/` e
   `%APPDATA%/.minecraft/mods/`) e confirme no log que a build nova subiu.

**Duração.** A conta de 2026-08-26 estimou ~8,5 minutos só para a primeira
varredura. Sessões curtas não chegam a ver o resto. O piso desta lista é
**~20 minutos** — a tabela de tempo logo abaixo diz de onde sai esse número
e o que cai fora se a sessão for menor.

**Se chegar de noite:** `/time set day`. Trabalhador só trabalha com sol.

---

## Quanto tempo cada resposta custa

O relógio do mod é o **ciclo da colônia**: `VillageDetector.CYCLE_TICKS` =
600 ticks = **30 segundos**. Toda espera abaixo está contada em ciclos,
porque é assim que o log sai.

| | O que responde | Quando dá para responder |
|---|---|---|
| 1 | a varredura acaba num ciclo | **~8,5 min** até a primeira varredura fechar (estimativa de 08-26; teto de 1.024 colunas por passagem). Antes disso, `still sweeping` ainda não é veredito |
| 2 | o trabalhador fantasma | **1,5 min** de colônia ativa — `MISSES_BEFORE_NEWS = 3` ciclos. A linha sai **uma vez** e não repete |
| 3 | o mineiro entra e cava | só **depois** do item 1. Reserve **10 min** a partir daí: ele caminha, abre a boca e desce um degrau por ordem de cavar |
| 4 | ele para à noite | precisa **atravessar um anoitecer**. Sem tempo, force com `/time set night` e volte com `/time set day` |
| 5 | a picareta certa na mão | **imediato** — é olhar a mão dos dois |
| 6 | fundidor e cadeia da areia | **10 min**, que é o teto de paciência da obra (`PatienceClock.CYCLES = 20` ciclos) |
| 7 | casa inteira sem a barreira | uma obra do começo ao fim; a frase só sai em sessão que assentou peça |
| 8 | reabrir o mundo com a mina aberta | **~2 min** depois de fechar — e só vale se 1 e 3 já tiverem dado sinal |

**Sessão de 10 minutos** responde 1, 2 e 5, e nada mais. **20 minutos** é o
piso para o mineiro. **40 minutos** é o que os itens 4, 7 e 8 pedem.

---

## A ordem, e por que ela é essa

Cada item abaixo é a porta do seguinte. Não adianta procurar o mineiro se a
varredura comeu a sessão.

### 1. A varredura acaba num ciclo?

**É o item que decide se dá para ver qualquer outra coisa.**

| O que procurar | O que significa |
|---|---|
| `still sweeping` aparecendo **uma vez**, seguido de `planned ... at` | ✅ o índice de ruas de 08-27 resolveu. Fluidez em ordem |
| `still sweeping` a sessão **inteira** | ❌ é aqui que está todo o problema de fluidez, e o próximo trabalho é o *jeito de procurar* (Decisão 8), não o volume |

> Mudou em 2026-09-02: colônia **abandonada** deixou de planejar obra, então
> ela não gasta mais varredura. Se você tem vilas abandonadas por perto, a
> sessão deve estar mais folgada que as anteriores.

### 2. O trabalhador fantasma existe?

Instrumentação nova de 2026-09-02, e ela existe **só para ser lida nesta
sessão**.

| O que procurar | O que significa |
|---|---|
| `Worker ... has been missing for 3 cycles while the colony was active` | o fantasma é real. Vale consertar, e aí a decisão de poda volta à mesa |
| a linha **não aparece** | nenhuma evidência de fantasma. Não prova ausência — prova que não é frequente, que já é o suficiente para não consertar agora |

### 3. O mineiro entra na mina e cava?

Dez consertos dependem desta resposta.

| O que procurar | O que significa |
|---|---|
| `digging ... at` com o aldeão em **y≈44** | ✅ ele está dentro da galeria. É o que sete sessões não conseguiram mostrar |
| `0/0 ticks` com ele parado | ❌ o **E32** não fechou — o `mine()` nunca roda |
| `the miner is at ... 66 ...` (y=66) | ❌ ele voltou para a **superfície acima da mina**. É o sintoma exato que o conserto do E32 atacou |
| `could not reach` repetido no mesmo lugar | possível **E34**: ele foi mandado para um vão do outro lado de uma parede |
| `hit stone with nowhere to stand` e a mesma descida reaparece | assinatura observada em 09-13, **E45 aberto**; o jar atualizado ainda não troca a geometria da mina |

### 3-a. O que entrou em 2026-09-03, e como conferir cada um

Quatro coisas entraram **depois** que esta lista foi escrita, e três delas
são pedido do autor na sessão anterior. Duas falam no log; duas só se veem
com os olhos.

| O que conferir | O que procurar |
|---|---|
| **o detector de imobilidade** | `Miner ... gave up the stone at ... — it has not moved a block in N ticks of work time`. É o conserto dos dois minutos: agora sai em **15 s**. A linha vizinha, `it walked for N ticks of work time without arriving`, continua existindo e quer dizer **outra coisa** — ele andou e não chegou |
| **`still` ao lado de `stall`** | `..., stall N/2400, still N/300` no relatório do mineiro. É o instrumento que decide o A\*: `still` perto de zero com `stall` subindo = ele **anda** e não chega, e o gargalo é navegação. Os dois subindo juntos = congelado. **E os dois perto de zero com ele parado é um terceiro estado**, medido em 09-04: quer dizer que a passagem não chega ao ramo que conta — ou não é expediente, ou o trabalho nem começou. Não confunda com "está tudo bem" |
| **água e lava tapadas** | `The mine sealed N face(s) at ... — the pick opened a flow`. Sai só quando a picareta abre fluxo de verdade; sessão sem nascente não diz nada, e isso não é defeito |
| **minério raro primeiro** | **olho, não log** — `OreVein` não escreve nada. Num veio com dois tipos, qual some primeiro |
| **galeria com bolsões** | **olho, não log** — `MineShaft` não escreve nada. Um vão de 3×2×2 a cada 8 colunas, de lado alternado |

> **O detector deixou de ser do mineiro.** Ele está nas sete profissões
> desde 09-03, e o pastor ganhou no mesmo commit o gate de expediente que
> lhe faltava. Nenhuma das outras seis foi vista com ele em jogo — se
> aparecer `gave up ... it has not moved` para lenhador, construtor,
> fabricante, fazendeiro ou pastor, é a primeira vez.

**E a pergunta que só um save antigo responde.** A mina de antes do bolsão
tem o cursor apontando para a forma antiga de galeria. O `findTheFrontier`
foi escrito para recuar sozinho — **ver se recua é a primeira coisa a
olhar** no item 3. Se não recuar, o mineiro cava contra a parede de um
plano que não existe mais.

### 3-b. O que entrou em 2026-09-04, e é por onde começar

**Seis consertos, nenhum visto rodar.** A ordem é essa porque é a ordem em
que um falhando esconde o outro.

| | O que conferir | O que procurar |
|---|---|---|
| 1 | **o lenhador entrega madeira** | `filled the chest — N logs collected`. `N` zerado ciclo após ciclo = o transbordo não pegou. E `Colony ... had no room mid-harvest` só deve sair com a colônia **inteira** cheia; se sair cedo, o assoreamento (E38) chegou antes |
| 2 | **a obra anda sem a barreira** | `TEST BARRIER covered for N of M pieces` no encerramento. Em 09-04 foram **47 de 169** — 28% da obra era falsa. Se cair, a cadeia da madeira passou a entregar |
| 3 | **os mineiros se revezam na escada** | `waiting for the shaft` **pode** aparecer; o que não pode é ficar. Mesmo par por 20 minutos = o conserto não pegou |
| 4 | **o mineiro sobe para buscar areia** | `digging Areia at ... y=62` com ele lá embaixo. A perna agora olha para o destino, e ele deve **sair pela boca** em vez de varrer a galeria |
| 5 | **a colônia que cala diz por quê** | `no cycle work: the chest count came in partial`. Se aparecer, a colônia estava parada e agora se sabe o motivo. E o **estoque sai a cada ciclo em que muda** — é a série que faltava para responder "a colônia tinha material?" sem adivinhar |
| 6 | **o guarda de imobilidade morde** | `has not moved a block in N ticks of work time`, e `still` **subindo** no relatório em vez de cravado em zero. Um congelado agora volta em 15 s. Se `still` continuar em zero com trabalhador visivelmente parado, o **E36 não era tudo** — ver o terceiro estado no item 3-a |

> **O item 6 é o que esta sessão pode fechar sozinha.** O E36 saiu com teste
> e fase vermelha conferida, mas o teste que deveria confirmá-lo em bateria
> — o E37 — **continua instável, e o E36 não o curou**. Um trabalhador
> congelado em jogo responde numa linha o que dois ciclos de teste não
> responderam.

**A pergunta do save antigo continua aberta** e é a mesma do item 3-a: a
mina de antes do bolsão tem o cursor na forma antiga de galeria, e o
`findTheFrontier` deve recuar sozinho.

### 4. Ele para à noite?

| O que procurar | O que significa |
|---|---|
| contador de `stall` **congelado** enquanto o relatório diz `off hours` | ✅ certo |
| o contador subindo com `off hours` | ❌ ele está sendo punido por não trabalhar de noite |

### 5. A picareta certa na mão

Cosmético e de velocidade ao mesmo tempo: o mineiro deve estar segurando
**picareta de diamante**, e o pastor **não** deve estar segurando picareta.

### 6. O fundidor assa, e a cadeia da areia fecha

| O que procurar |
|---|
| `Smelter ... made ...` |
| a meta de `SAND` saindo de `looking for sand, 0 of 6` e chegando a vidro e vidraça |

### 7. A casa inteira sem a barreira de teste

`TEST BARRIER covered for nothing this session` — e desde 08-28 essa frase
**só sai numa sessão que assentou peça**, então ela já não pode mentir como
mentia no E31.

### 8. Fechar e reabrir o mundo com a mina aberta

O registro da mina sobrevive (visto em 08-26). O que nunca foi visto é a
**galeria retomada** depois de reabrir — e agora, com o cursor conferido
contra o mundo, é a hora de olhar.

---

## O que anotar, mesmo que pareça pequeno

- **a hora de início e fim** da sessão, e se o jar novo subiu;
- **qualquer linha que você não entenda** — o log deste mod é escrito para
  ser lido, e linha confusa é defeito de instrumento;
- **o que você viu com os olhos** e o log não disse. Foi assim que o E34
  nasceu: o autor cavou até a galeria e viu que não havia nada lá.

### Playtest de 2026-09-12, jar 0.3.0

- **Casa parada no meio:** confirmado no log. A obra ficou com **1 bloco
  faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A
  carpintaria fabricava composteiras, mas usava a porta de consumo
  (`ColonySupply.take`) e retirava do baú a peça que acabou de produzir.
  Corrigido em 2026-09-13: `CraftingWork` passa a chamar
  `ColonySupply.stock`, que fabrica e deixa a peça no estoque para o
  construtor assentar.
- **Mineiros não vistos trabalhando:** o log mostra produção real no começo
  da sessão (`38 hauled`), seguida de muitos ciclos em `looking for stone`.
  Próximo diagnóstico: separar mina realmente sem alvo de limbo de ramal
  (`no miner branch work: every open branch is taken or finished`) depois de
  recusa/cedência da frente.
- **Lenhadores e fazendeiros vistos trabalhando:** confirmado pelo relato e
  pelo log. Ainda há recusas de alvo fora de alcance, mas havia produção.
- **Outras profissões pouco visíveis:** carpinteiros trabalharam no log; para
  fundidor, pedreiro e pastor, próxima sessão deve conferir se havia tarefa
  aberta, trabalhador com profissão e material de entrada.

### Validação pendente de 2026-09-13, jar 0.3.0

- O MD5 esperado em `downloads/` e `%APPDATA%/.minecraft/mods` é
  `CD29B5F8BC61871BCE74A910C57DB28E`.
- **Construtor:** conferir que terra comum não é colocada sem estoque; anotar
  o bloco/posição e o estado da obra. A linha do log anterior não identificou
  qual terra foi vista.
- **Mineiro:** nenhum ajuste de geometria foi incluído. Anotar se `hit stone
  with nowhere to stand` reaparece e se a mesma hélice reinicia; isso decide o
  próximo diagnóstico, mas E45 precisa de aprovação/ADR antes do código.

---

## O que já está pronto e esperando esta sessão

Consertos escritos, com teste e fase vermelha conferida, que **nenhuma
sessão viu rodar**.

> **Atualizado em 2026-09-04.** A sessão de 09-03 toca as quatro primeiras
> linhas desta tabela, mas o log não foi conferido item a item, então elas
> ficam onde estão: *provável, não provado*. As linhas do meio entraram
> depois dela; **as seis últimas entraram depois da sessão de 09-04**, e
> são as do item 3-b.

| | O que espera prova |
|---|---|
| **E33-a** | a folga de chegada virou zero — ele para no lugar exato |
| **E35** | a descida passou a ser dada pela ordem de cavar, um degrau por vez |
| **E32** | a perna deixou de mandar o mineiro para bloco onde não se fica de pé |
| **E34** | a perna para na parede em vez de saltar para o bolsão atrás dela |
| — | reserva de mina: um mineiro por escada, o segundo espera dizendo que espera |
| — | galeria acesa com tocha atrás da frente de escavação |
| — | ferramenta que combina com a profissão, conferida a cada passagem |
| — | destino solto quando a tarefa termina |
| — | colônia abandonada não paga mais a varredura |
| — | contagem do trabalhador fantasma |
| — | vedação de água e lava, com a galeria virando na hora |
| — | minério mais raro escolhido entre as seis faces |
| — | galeria com bolsão de 3×2×2 a cada 8 colunas |
| — | detector de imobilidade nas **sete** profissões — visto só no mineiro |
| — | pastor com gate de expediente: ele para de contar a noite |
| — | **09-04** · lenhador transborda para a colônia em vez de destruir tronco |
| — | **09-04** · recusa no portão da escada não gasta mais a busca do tique |
| — | **09-04** · a perna do mineiro olha para o destino, e sabe voltar |
| — | **09-04** · o ciclo que pula por varredura parcial diz que pulou |
| — | **09-04** · o estoque da colônia vai ao log a cada ciclo em que muda |
| — | **E36** · o guarda de imobilidade deixou de ser zerado por alvo novo |

---

## Decisões que esperam o resultado desta sessão

- **Trabalhador fantasma:** consertar ou aceitar, conforme o item 2 acima.
- **A varredura:** atacar o jeito de procurar, ou deixar quieto, conforme o
  item 1.
- **A Regra 28** (uma casa por bioma) só cai depois de o planejador saber
  desistir de um objetivo — ver Decisão 3 no `TODO.md`.
