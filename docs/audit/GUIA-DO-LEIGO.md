# O que este mod faz — guia para quem nunca viu o código

**Data:** 2026-10-06 · parte da [auditoria de recuperação](PROJECT-RECOVERY-AUDIT.md).

Este guia foi escrito a partir do **código**, não dos planos antigos. Onde
uma frase depende de um lugar do código, ele aparece entre parênteses, para
quem quiser conferir. Onde algo ainda não foi visto funcionando no jogo, o
texto diz.

---

## 1. A ideia em uma frase

> No Minecraft normal, a vila é um enfeite: os aldeões andam, dormem e trocam
> itens com você. Com este mod, **a vila vira uma pequena empresa que trabalha
> sozinha** — coleta, fabrica, constrói casas e cresce.

O lema do projeto resume a divisão de trabalho:

```text
A colônia decide.   →  a "cabeça" da vila escolhe o que falta e o que construir
As tarefas organizam. →  cada necessidade vira um pedido de trabalho
Os aldeões executam. →  cada aldeão pega um pedido que sabe fazer e faz
O Minecraft continua sendo Minecraft. → nada de blocos ou itens novos
```

## 2. Palavras que aparecem o tempo todo

| Palavra | O que quer dizer aqui |
|---|---|
| **Vila** | a vila que o Minecraft gera sozinho no mapa |
| **Colônia** | a vila depois que o mod a "adota" e começa a administrá-la |
| **Aldeão** | o morador da vila (o mesmo bicho do jogo normal) |
| **Profissão / ofício** | o emprego que o mod dá ao aldeão (minerador, lenhador…) |
| **Tarefa / pedido** | "busque 20 pedras", "construa esta casa" |
| **Ciclo** | a "reunião" que a colônia faz a cada 30 segundos para decidir |
| **Baú da vila** | os baús de onde a colônia tira e onde guarda as coisas |
| **Obra** | uma construção em andamento |
| **Lote** | o pedaço de chão escolhido para uma obra |
| **Caixa da vila** | o retângulo que diz até onde vai a vila (cresce com ela) |
| **BigHouseMOD** | a casa grande que o mod constrói quando adota uma vila |
| **Encalhado** | aldeão preso (cercado, enterrado, num buraco) |

## 3. O que acontece quando uma vila é encontrada

1. **O mod procura camas.** Onde houver pelo menos **3 camas e 2 aldeões**
   perto (até 64 blocos), é uma vila (`VillageDetector`).
2. **Adota a vila.** Ela vira uma colônia, com nome interno e uma "caixa"
   que marca até onde vai.
3. **Funda a colônia** (uma vez só):
   - coloca **um baú ao lado de cada cama**;
   - ergue a **BigHouseMOD** de uma vez, pronta;
   - nasce **um adulto para cada cama** da BigHouseMOD.
4. **Dá emprego.** Sete profissões são preenchidas logo: minerador, lenhador,
   carpinteiro, pedreiro, fundidor, pastor e construtor. O agricultor entra
   depois, conforme a vila cresce.

Daí em diante a colônia trabalha **enquanto o jogador estiver dentro da vila,
e até 5 minutos depois que ele sair**. Longe do jogador, ela pausa — isso
economiza processamento do servidor.

## 4. As profissões

| Profissão | Ferramenta | O que faz |
|---|---|---|
| **Minerador** | picareta | cava uma mina em escada, traz pedra, carvão e ferro; para de trazer um tipo quando já tem 256 |
| **Lenhador** | machado | corta árvores, replanta, cuida de um viveiro de mudas |
| **Agricultor** | enxada | planta e colhe; faz pão quando sobra trigo (mais de 32) |
| **Pastor** | tesoura | tosquia ovelhas e faz o rebanho crescer |
| **Fundidor** | pá | usa a fornalha (areia vira vidro, minério vira barra); busca areia e argila |
| **Carpinteiro** | — | transforma madeira em peças (tábuas, escadas, portas) |
| **Pedreiro** | — | transforma pedra em peças (degraus, lajes, tijolos) |
| **Construtor** | — | coloca os blocos da obra, um por um |

As ferramentas aparecem sozinhas quando o aldeão é contratado. Se houver uma
melhor no baú, ele troca. **Não existe sistema de níveis, experiência ou
habilidades** — um aldeão não "melhora" com o tempo.

## 5. Como um aldeão recebe e faz uma tarefa

```text
A cada 30 s, a colônia:
  1. olha os baús                     "temos 10 pedras"
  2. compara com a meta               "queremos encher o baú"
  3. cria pedidos para o que falta    "busque pedra"  (um por aldeão capaz)
  4. cada aldeão livre pega um pedido que sabe fazer
O aldeão então:
  5. anda até o lugar
  6. trabalha (cava, corta, colhe, fabrica, constrói)
  7. leva o resultado ao baú
  8. avisa que terminou, e fica livre de novo
```

A ordem de importância dos pedidos é: **sobrevivência** (comida) →
**material de obra** → **produção** → **construção**.

O aldeão trabalha de dia e para ao anoitecer. Chuva não para o trabalho.

## 6. Como os recursos circulam

```text
NATUREZA ──(coletores)──► BAÚS ──(artesãos)──► PEÇAS ──► BAÚS ──(construtor)──► OBRA
 árvore      lenhador              carpinteiro   tábua
 pedra       minerador             pedreiro      degrau
 areia       fundidor              fundidor      vidro
 trigo       agricultor
 lã          pastor
```

- **Meta:** a colônia quer os baús cheios. Há mínimos garantidos: 64 de
  pedra, 64 de comida, 16 de cada material fundido, 8 de comida por cama.
- **Madeira:** metade fica guardada em tora; a obra pode usar o resto.
- **Cadeia:** se a obra pede uma porta, a colônia vê que porta sai de tábua,
  que tábua sai de tora, e que tora vem do lenhador — e pede tudo na ordem.
- **Quais baús contam:** os de dentro da caixa da vila. Ficam de fora os baús
  que o jogador renomeou (são dele), os 7 baús da BigHouseMOD e o baú
  particular ao lado de cama Vanilla.

## 7. O que pode surgir "do nada"

O jogo não deixa o mod criar coisas sem custo à vontade. As exceções aceitas
pelo autor (ADR-028 e ADR-034) são:

- a ferramenta inicial do aldeão contratado;
- a BigHouseMOD e um adulto por cama dela, na fundação;
- pedregulho para o aldeão preso subir, quando o baú não tem;
- uma peça que **não existe no bioma** — aparece no baú na 3ª tentativa;
- itens que só caem de bichos (corante, linha, osso, pena…);
- blocos que não têm item (água no caldeirão, terra arada).

**Nunca:** lava é colocada sozinha. **Sempre** vêm do trabalho: tora, pedra,
terra, areia, lã e trigo.

## 8. Como uma construção é escolhida e feita

**O que construir** (`ConstructionPriority`):

1. Se há mais trabalhadores que camas → **casa**.
2. Se uma profissão não tem a sua oficina → **oficina** (fazenda para o
   agricultor, curral para o pastor, ferraria para o fundidor…).
3. Senão, alterna entre casa e outras construções.

As plantas vêm dos modelos próprios do mod quando existem; senão, do catálogo
do Minecraft para aquele bioma (deserto, savana…). A primeira casa é a menor;
depois, a maior que couber.

**Onde construir:** ao lado de uma rua, num chão plano, sem cortar nada que
seja da vila, do jogador ou da colônia. Se não há lugar, a rua cresce para
abrir lugar. A busca vai até metade da diagonal da vila + 12 blocos.

**Como acontece:**

```text
escolhe planta → acha lote → abre a obra (placa no canteiro)
→ reserva material → construtor coloca bloco por bloco
→ casa pronta vira parte permanente da colônia
```

**Se falta material:** a obra espera até **10 minutos**. Se nada chega, é
abandonada e pode ser retomada depois.

**O jogador pode cancelar** uma obra pondo uma **tocha de alma** no canteiro.

## 9. Como a vila cresce

- **População:** depois da fundação, os aldeões se reproduzem como no jogo
  normal. A colônia dá comida extra no fim do dia **se houver cama sobrando**.
  O limite é o número de camas — por isso construir casas faz a vila crescer.
- **Empregos:** as vagas abrem com a população: até 15 adultos, uma vaga por
  adulto (até 8); depois, um lote novo a cada 15 adultos. A profissão de que a
  obra precisa é contratada primeiro.
- **Território:** a caixa da vila aumenta a cada casa e rua nova.
- **Floresta:** o lenhador planta árvores conforme a população cresce.

## 10. Quando algo dá errado

| Situação | O que o mod faz |
|---|---|
| Aldeão parado muito tempo no mesmo lugar (15 s no expediente) | larga a tarefa e descansa 4 ciclos (2 min) |
| Desiste da mesma coisa 3 vezes em 6 min | perde a profissão e recebe outra depois |
| Preso num buraco ou enterrado | cava a saída, sobe por escada, pilar ou túnel |
| Preso num curral | abre o portão (e fecha atrás) ou pula a cerca |
| Preso no fundo da mina | volta pelo caminho por onde desceu |
| Obra sem material | espera 10 min, depois é abandonada |
| Sem lote | a rua cresce para abrir espaço |

## 11. Quando o jogo é salvo e carregado

- Os **blocos** (casas, minas, baús) são salvos pelo Minecraft normalmente.
- O **"cérebro" da colônia** (quem é de qual vila, que profissão, quais obras
  e minas existem) é gravado **quando o servidor fecha normalmente**.
- Ao carregar, o mod relê os blocos do mundo e retoma as obras de onde
  pararam.
- **Atenção (problema P0-02 da auditoria):** se o jogo **travar ou for
  fechado à força**, o "cérebro" volta para o último fechamento normal,
  enquanto os blocos ficam do último salvamento automático. Isso ainda não foi
  visto acontecer, mas o código permite.
- Tarefas em andamento e descansos não são salvos: recomeçam do zero, o que é
  de propósito.

## 12. O que o jogador faz — e o que não precisa fazer

**Pode fazer:**
- configurar profissões no **Mod Menu** (ligar/desligar, máximo de
  trabalhadores, ordem de contratação, raio de busca);
- cancelar uma obra com uma **tocha de alma**;
- ver o diagnóstico da vila com **`/vc log`**;
- renomear um baú para que a colônia não mexa nele.

**Não precisa:** dar ordens, levar material, escolher onde construir,
contratar ou alimentar ninguém.

## 13. Regras que nunca deveriam ser quebradas

1. O aldeão **não decide** estratégia; só a colônia decide.
2. **Nunca destruir** o que o jogador construiu, o que a colônia construiu, nem
   os blocos construídos da vila original (o chão natural pode ser usado).
3. **Lava nunca** é colocada automaticamente.
4. A vila só **cresce para fora**; a original não é substituída.
5. Receitas são **sempre as do Minecraft**.
6. O núcleo do mod **não depende** do Minecraft (por isso é testável).
7. **Nunca reescrever** o código do Minecraft por inteiro (`@Overwrite`).
8. Toda casa terminada é **permanente**.

## 14. O que ainda não está provado

Mais de 20 correções foram testadas por programa mas **ainda não foram vistas
no jogo** (lista no `STATE.md`). E desde 03-10 o trabalho seguiu em duas
linhas separadas que estão sendo juntadas agora (P0-01). Até um playtest
depois disso, a descrição acima é **o que o código manda fazer**, não
necessariamente o que já foi visto acontecer.
