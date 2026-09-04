# A próxima sessão de jogo — o que olhar, e em que ordem

**Escrito em 2026-09-02, atualizado em 2026-09-04.** Este arquivo existe
porque o gargalo do projeto deixou de ser código: havia **dez consertos do
mineiro empilhados sem uma única sessão que os veja**, e nenhuma pergunta
importante em aberto pode ser respondida sem abrir o jogo.

A sessão de 09-03 aconteceu e **o mineiro trabalhou** — a primeira boa. Ela
não zerou a pilha: fechou o que dependia de vê-lo cavar, e abriu quatro
coisas novas, que estão no **item 3-a**.

A de **09-04** durou 43 minutos, não teve crash, e rendeu noventa e nove
`WARN` que renderam cinco ciclos de conserto. Nenhum deles foi visto rodar,
e um sexto entrou depois dela — estão no **item 3-b**, que é por onde esta
lista deve começar agora. A ordem abaixo continua valendo para o resto.

Ele não é diário — quem conta a história é o `TODO.md`. Este aqui é a lista
de conferência de uma sessão.

---

## Antes de abrir o jogo

**A armadilha que já custou uma sessão inteira.** Em 2026-08-29 a sessão
rodou com o jar do dia anterior: o Minecraft estava aberto segurando o
arquivo, a troca falhou em silêncio, e o `.jar.new` ficou ao lado sem ser
aplicado. A sessão mostrou defeitos — e **nenhum dos consertos daquele dia
estava rodando**.

1. **Feche o Minecraft** antes de copiar o jar.
2. Copie `downloads/village-colony-0.3.0.jar` para a pasta `mods`, apagando
   o jar antigo.
3. Confirme no log que a build nova subiu, e não a anterior.

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
