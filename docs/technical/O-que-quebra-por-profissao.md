# O que quebra em cada teste das profissões

> **Lista de leitura, 2026-09-17.** Extraída das **mensagens de falha** dos
> 338 GameTests — a frase que cada teste imprime quando fica vermelho é a
> descrição do que quebrou.
>
> **Os temas iguais estão mesclados.** Onde a mesma coisa quebra em várias
> profissões, ela aparece **uma vez**, com a lista de quem compartilha.
>
> Método: 326 testes com mensagem legível, 831 asserções, agrupadas por
> tema. Regenerável — ver §5.

---

## 1. Como ler

Cada tema abaixo é **um jeito de a vila quebrar**, não um arquivo de teste.
O número entre parênteses é quantos testes daquela profissão guardam aquele
tema — quanto maior, mais vezes aquilo já deu errado de verdade.

**Ordem: do mais compartilhado para o mais específico.** Os primeiros temas
são os que valem ler antes de mexer em qualquer profissão.

---

## 2. Temas compartilhados — leia estes primeiro

### 2.1 A tarefa não abre, abre repetida, ou vai ao ofício errado
**9 de 9 profissões · 20 testes**

> Fabricante/Pedreiro (6) · Mineiro (5) · Coleta de superfície (3) ·
> Construtor (1) · Fazendeiro (1) · Lenhador (1) · Fundidor (1) ·
> Ferramenta (1) · Ciclo de vida (1)

**É o único tema que toca todas as profissões.** A tarefa é o contrato entre
a colônia e o trabalhador: sem ela o aldeão fica ocioso com material no baú,
e com ela duplicada dois aldeões disputam a mesma peça.

⚠️ **Foi exatamente aqui que o P1.1 quebrou em jogo** (2026-09-17): a obra
esperava `cobblestone_stairs`, havia pedregulho e pedreiro, e a tarefa
**nunca abria** porque a peça não era um `ResourceType`.

**Antes de mexer em qualquer profissão, pergunte:** quem abre a tarefa dela,
e o que acontece se a peça pedida não estiver declarada?

### 2.2 A peça não chega ao baú nem ao mundo
**7 profissões · 25 testes**

> Mineiro (10) · Construtor (6) · Ferramenta (3) · Lenhador (2) ·
> Coleta de superfície (2) · Pastor (1) · Fundidor (1)

O trabalho aparenta acontecer e **nada muda no mundo**. As duas pontas:
o bloco não sai (mineiro, lenhador) ou não é assentado (construtor), e o
produto não chega ao baú.

A variante mais perigosa tem nome nos testes do fundidor: **"matéria do
nada"** — o vidro aparece e a areia continua inteira. Duplicar item é pior
que não produzir.

### 2.3 O trabalhador não para, ou para cedo demais
**7 profissões · 17 testes**

> Mineiro (7) · Construtor (3) · Lenhador (2) · Ferramenta (2) ·
> Fabricante (1) · Fazendeiro (1) · Ciclo de vida (1)

Os dois lados do mesmo defeito. Não parar: o pedido foi atendido e o aldeão
continua cavando. Parar cedo: **"deixou a veia pela metade"**, do teste do
mineiro — que é o que você pediu para corrigir no P1.3.

### 2.4 A contagem ou a meta está errada
**5 profissões · 22 testes**

> Mineiro (9) · Lenhador (7) · Fabricante (4) · Construtor (1) · Pastor (1)

A colônia acredita num número que o mundo não confirma. É o defeito mais
silencioso da lista: nada trava, e a vila decide errado — pede material que
já tem, ou dá meta por cumprida com o baú vazio.

### 2.5 O lote foi recusado ou aceito errado
**5 profissões · 17 testes**

> Mineiro (6) · Lenhador (4) · Construtor (3) · Fazendeiro (3) ·
> Ferramenta (1)

Não é só do construtor: o mineiro escolhe boca de mina, o lenhador escolhe
árvore, o fazendeiro escolhe canteiro. **Todos fazem a mesma pergunta —
"este lugar serve?"** — e todos já erraram.

⚠️ **É o P1.0**, medido em jogo: `0 survived every check` em 960.672
colunas, com 54,8% recusadas pela reserva de estrada.

### 2.6 Regra 3: mexeu no que é do jogador ou da vila
**4 profissões · 23 testes**

> Mineiro (11) · Ferramenta (6) · Lenhador (5) · Fundidor (1)

**O tema mais denso do mineiro depois do posicionamento.** A colônia não
pode comer a própria vila nem desfazer o que o jogador fez — e a assinatura
recorrente é o que os testes chamam de *"repor o que o jogador desfez"*: o
mod reconstrói para sempre o que o dono do mundo derrubou.

### 2.7 Material ou receita errada
**4 profissões · 21 testes**

> Construtor (8) · Fabricante/Pedreiro (8) · Mineiro (4) · Ferramenta (1)

A peça certa, feita do jeito errado — ou a substituição que não devia
valer. Inclui **"uma porta devia custar uma porta"** (o construtor gastando
duas metades) e a família de madeira trocada por espécie.

### 2.8 O guarda de travamento conta errado
**4 profissões · 10 testes**

> Mineiro (6) · Lenhador (2) · Construtor (1) · Pastor (1)

Os contadores `stall`, `still` e `adrift`. ⚠️ **Foi o E45**, e a lição está
no código: *guarda que o caminho de falha zera não é guarda* — o contador
morava no braço, que era solto e reocupado a cada passagem, e por isso nunca
chegava ao limite.

### 2.9 O alvo está fora de alcance ou não cabe
**4 profissões · 7 testes**

> Mineiro (4) · Construtor (1) · Fazendeiro (1) · Lenhador (1)

O aldeão anda até o alvo e não chega, ou não há onde ficar de pé. ⚠️ É o que
o autor viu em jogo com a água: `the place to stand is …, which is flooded`
— **P1.2, aberto**.

---

## 3. Temas de uma ou duas profissões

### 3.1 A peça foi posta no lugar ou na forma errada
**3 profissões · 16 testes** — Mineiro (13) · Construtor (2) · Fabricante (1)

Quase todo do mineiro: degrau na altura errada, mobília ocupando a descida,
veio que desce sem abrir por onde voltar.

### 3.2 O log não diz o que devia
**3 profissões · 10 testes** — Mineiro (7) · Fazendeiro (2) · Superfície (1)

Testes que guardam o **diagnóstico**, não o comportamento. Existem porque o
projeto já perdeu sessões inteiras com log mudo — *"mineiro com trabalho
aberto e nenhuma linha — é o defeito de 08-22 de volta"*.

---

## 4. O que cada profissão guarda de próprio

| Profissão | Testes | O que só ela quebra |
|---|---|---|
| **Mineiro** | 79 | Arco e lanterna da boca; veio que desce; escada do poço; água que inunda o lugar de pé |
| **Lenhador** | 43 | Tronco sem cortar até o fim; folha recolhida junto; replantio da muda |
| **Construtor** | 25 | Porta como peça inteira; canteiro assentado sem baú; pé de coluna enterrada |
| **Fabricante/Pedreiro** | 18 | Divisão das duas oficinas; receita em profundidade; descascar tronco |
| **Fazendeiro** | 11 | Semente que sai do baú; verde não é maduro; terra solta perto d'água |
| **Fundidor** | 3 | Areia→vidro e cru→lingote sem "matéria do nada" |
| **Coleta de superfície** | 3 | Setor fora do raio protegido da vila |
| **Pastor** | 2 | Tosquiar sem matar a ovelha |

⚠️ **Pastor e Fundidor têm 2 e 3 testes.** A cobertura seguiu os defeitos
encontrados — o mineiro acumulou 79 porque quebrou muitas vezes. **As
profissões calmas são as menos protegidas, não as mais sólidas.**

---

## 5. Como regenerar esta lista

Os scripts de extração estão no scratchpad da sessão (não versionados). O
método, para refazer:

1. Ler `src/gametest/java/**/*GameTest.java`, remover comentários.
2. Para cada `public void <nome>(TestContext`, capturar as strings dentro
   de `context.assertTrue/assertFalse`.
3. Juntar as mensagens **por teste** antes de classificar — elas vêm
   fragmentadas por concatenação, e classificar fragmento solto inventa
   temas que não existem.
4. Agrupar por tema e contar profissões distintas.

⚠️ **O que esta lista não é.** Ela diz o que os testes **afirmam**, e não o
que está quebrado hoje — a bateria passa em 5 rodadas de 5. É um mapa do que
já quebrou e está trancado contra regressão.
