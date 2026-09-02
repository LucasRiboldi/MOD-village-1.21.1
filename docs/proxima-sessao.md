# A próxima sessão de jogo — o que olhar, e em que ordem

**Escrito em 2026-09-02.** Este arquivo existe porque o gargalo do projeto
deixou de ser código: há **dez consertos do mineiro empilhados sem uma única
sessão que os veja**, e nenhuma pergunta importante em aberto pode ser
respondida sem abrir o jogo.

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
varredura. Sessões curtas não chegam a ver o resto. Se der, deixe rodar
**mais de dez minutos**.

**Se chegar de noite:** `/time set day`. Trabalhador só trabalha com sol.

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
sessão viu rodar**:

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

---

## Decisões que esperam o resultado desta sessão

- **Trabalhador fantasma:** consertar ou aceitar, conforme o item 2 acima.
- **A varredura:** atacar o jeito de procurar, ou deixar quieto, conforme o
  item 1.
- **A Regra 28** (uma casa por bioma) só cai depois de o planejador saber
  desistir de um objetivo — ver Decisão 3 no `TODO.md`.
