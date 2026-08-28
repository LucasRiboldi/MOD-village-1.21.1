# Village-Economy.md

# Village Colony — A economia da vila

**A árvore tecnológica da colônia**, de acampamento a cidade autônoma:
quais profissões existem, o que cada uma consome e produz, e o que
destrava a fase seguinte.

Escrito em 2026-08-28, a partir da proposta do autor. Este arquivo é o
**plano**; o estado de verdade vive no [`README.md`](../../README.md) e a
lista de pendências no [`TODO.md`](../../TODO.md).

---

# 1. O enquadramento: perfil, e não árvore por bioma

A proposta original descreve a vila de **planície** — carvalho,
pedregulho, terracota branca, cascalho, trigo, lã. A
[ADR-009](../decisions/ADR-009-Autonomous-Village-Evolution.md) já
decidiu como isso entra no mod, e a decisão vale aqui inteira:

> **Um motor universal alimentado por perfis** — não uma árvore de
> evolução escrita à mão por bioma. Bioma novo entra como perfil, e não
> como dezenas de exceções.

Então este documento separa duas coisas que a proposta juntava:

| | |
|---|---|
| **O motor** | as profissões, a cadeia produtiva, as fases de crescimento e as condições de desbloqueio. **Igual para toda vila.** |
| **O perfil** | quais materiais aquele bioma oferece e com o que ele constrói. Planície usa carvalho e terracota branca; deserto usa arenito liso; taiga usa abeto. **Muda por bioma, lido do catálogo do jogo.** |

A regra de ouro da ADR-009 continua sendo o filtro de tudo o que segue:

> Isto funciona para **uma** vila, ou para **qualquer** vila com um
> contexto diferente?

Uma lista de materiais escrita no código reprova nessa pergunta. A mesma
lista **medida do catálogo de estruturas do bioma** passa — é o caminho
que a Regra 27 e o `VillageRoad` já tomaram, e o que o `OreVein` passou a
fazer com `c:ores` em 08-27.

---

# 2. Onde o projeto está hoje

**Sete profissões existem e funcionam.** Todas buscam recurso e guardam
no próprio baú.

| Profissão de hoje | Equivale, na proposta, a |
|---|---|
| 🪓 Lenhador | Lenhador |
| ⛏️ Mineiro | Minerador |
| 🌾 Fazendeiro | Fazendeiro (só a colheita; não ara nem planta) |
| 🐑 Pastor | Pastor |
| 🔥 Fundidor | Vidraceiro + a metade de fundição do Armeiro |
| 🪚 Fabricante | Carpinteiro (tábua, tocha, vidraça, tronco descascado) |
| 🏠 Construtor | Construtor |

**Duas delas são justamente as que o Vanilla não tem** e que a proposta
chama de indispensáveis: o **Minerador** e o **Construtor**. As duas
estão escritas e rodando.

O que **não** existe ainda: as 13 profissões vanilla como agentes
econômicos, e as de logística — Transportador e Armazenista.

---

# 3. As famílias de material — o perfil de planície

Levantado das estruturas vanilla de planície. **É um perfil**, e serve de
molde para os outros biomas.

| Família | O que a vila usa | Quem produz hoje |
|---|---|---|
| **Madeira** | tronco, tronco descascado, tábua, escada, laje, porta, alçapão, cerca, portão, placa, botão, pressão, baú, barril, mesa de trabalho, composteira | Lenhador ✅ · Fabricante ✅ (só tábua, tocha, vidraça, descascado) |
| **Pedra** | pedregulho, escada, laje, muro, pedra, fornalha, cortador, rebolo, alto-forno | Mineiro ✅ · **falta o Pedreiro** |
| **Terracota e argila** | argila, bola de argila, terracota, terracota branca, tijolo | **ninguém** — argila não é coletada |
| **Cascalho e caminhos** | cascalho, caminho de terra, terra | Mineiro ⚠️ (cascalho cai da galeria, sem uso) |
| **Agricultura** | terra arada, água, trigo, cenoura, batata, beterraba, sementes, feno, composto, farinha de osso | Fazendeiro ⚠️ (só colhe o que já existe) |
| **Pecuária** | feno, cercas, água, ovelha, vaca, porco, galinha, couro, lã, carne | Pastor ✅ (só lã) · **falta o Pecuarista** |
| **Lã e tecidos** | lã branca e colorida, carpetes | Pastor ✅ |
| **Vidro** | vidro, vidraça, vidro colorido | Fundidor ✅ + Fabricante ✅ |
| **Iluminação** | tocha, carvão, graveto | Mineiro ✅ + Fabricante ✅ |
| **Infraestrutura** | cama, sino, baú, barril, balde, caldeirão, fornalha, alto-forno, defumador, bigorna, rebolo, cortador, suporte de poções, tear, mesa de flechas, mesa de cartografia, atril, estante, composteira | **quase ninguém** — são as estações das profissões vanilla |

**O que esta tabela mostra de mais útil:** os buracos não são de
material, são de **profissão**. Argila, cascalho e as estações de
trabalho não têm dono.

---

# 4. As profissões, e o que falta em cada uma

## 4.1 As que existem

| | Consome | Produz | Falta |
|---|---|---|---|
| 🪓 **Lenhador** | — (coleta) | tronco | escolher espécie por necessidade; graveto, folha, maçã |
| ⛏️ **Mineiro** | — (coleta) | pedra, carvão, ferro cru, e todo minério de `c:ores` | **cavar em jogo** (E33); cascalho, argila e areia como alvo; teto de inventário |
| 🌾 **Fazendeiro** | — (colheita) | trigo, cenoura, batata, beterraba | **arar e plantar**; feno, composto, farinha de osso |
| 🐑 **Pastor** | — (tosquia) | lã | criar rebanho; lã colorida; carpete |
| 🔥 **Fundidor** | areia, ferro cru, arenito | vidro, lingote, arenito liso | combustível próprio; a cadeia da areia nunca começou em jogo |
| 🪚 **Fabricante** | tronco, ferro, carvão | tábua, tocha, vidraça, descascado | porta, cama, janela, baú, barril, cerca, escada, laje |
| 🏠 **Construtor** | tudo | casa | desistir de obra travada; escolher entre as 1.180 estruturas |

## 4.2 As 13 vanilla, como agentes econômicos

O Vanilla dá a estação de trabalho; o mod daria o **trabalho**.

| Profissão | Estação | Papel na colônia | Depende de |
|---|---|---|---|
| Fazendeiro | Composteira | ✅ existe | — |
| Pastor | Tear | ✅ existe | Pecuarista |
| Pescador | Barril | peixe, comida aquática | água próxima |
| Açougueiro | Defumador | carne cozida | Pecuarista |
| Coureiro | Caldeirão | couro processado, armadura de couro | Pecuarista |
| Flecheiro | Mesa de flechas | flecha, arco, besta | Lenhador, Pecuarista (pena), Mineiro (pederneira) |
| Pedreiro | Cortador de pedra | escada, laje, muro, tijolo, terracota | Mineiro |
| Ferreiro de ferramentas | Mesa de ferraria | picareta, machado, pá, enxada | Mineiro, Fundidor |
| Ferreiro de armas | Rebolo | espada, machado de guerra | Mineiro, Fundidor |
| Armeiro | Alto-forno | capacete, peitoral, calça, bota, escudo | Mineiro, Coureiro |
| Bibliotecário | Atril | livro, papel, **tecnologia da vila** | Fazendeiro (cana), Coureiro |
| Cartógrafo | Mesa de cartografia | mapa, rota, **área de expansão** | Bibliotecário |
| Clérigo | Suporte de poções | poção, recurso raro | Mineiro (redstone, lápis) |

**A lacuna que a proposta identifica corretamente:** o Armeiro usa ferro
e o Vanilla não tem quem o extraia. O mod já resolveu isso — o Mineiro
existe.

## 4.3 As que o mod precisa acrescentar

| Profissão | Por quê | Prioridade |
|---|---|---|
| **Pecuarista** | Couro, carne, ovo, leite. Hoje nenhuma delas entra na vila | ★★★★ |
| **Pedreiro** | Fecha `Mineiro → pedra → Pedreiro → Construtor → casa`. Hoje o Construtor consome pedregulho cru e não tem escada, laje nem muro | ★★★★★ |
| **Transportador** | Hoje cada trabalhador guarda no **próprio** baú, e o Construtor tira de qualquer um. Com mais profissões isso não escala | ★★★★ |
| **Armazenista** | Estoque central, prioridade e criação automática de tarefa por escassez | ★★★★ |
| **Vidraceiro** | Hoje é metade do Fundidor. Separar só quando houver vidro colorido | ★★ |
| **Guarda** | A defesa, que o modelo prevê e ninguém escreveu | ★★★ |
| **Explorador / Agrimensor** | Define área de expansão. Depende do Cartógrafo | ★★ |

---

# 5. A cadeia produtiva

```text
                        NATUREZA
                            │
        ┌───────────────┬───┴────────┬───────────────┐
        ↓               ↓            ↓               ↓
     MADEIRA         MINÉRIO      ALIMENTO        ANIMAL
     Lenhador        Mineiro      Fazendeiro     Pecuarista
        │               │            │               │
        ↓         ┌─────┼─────┐      ↓         ┌─────┴─────┐
   Carpinteiro   pedra ferro carvão  pão    Açougueiro  Coureiro
   (Fabricante)   │      │     │      │         │          │
        │      Pedreiro  └──┬──┘      │       carne      couro
        │         │         ↓         │         │          │
        │         │      Ferreiros    │         │       Armeiro
        │         │      Armeiro      │         │          │
        └─────────┴─────────┬─────────┴─────────┴──────────┘
                            ↓
                       CONSTRUTOR
                            ↓
                          CASA
                            ↓
                          CAMA          ← lã, do Pastor
                            ↓
                      NOVO ALDEÃO
                            ↓
                     NOVA PROFISSÃO
                            ↓
                     MAIOR PRODUÇÃO
                            ↓
                      NOVA EXPANSÃO
```

**O laço fechado é o ponto.** O mod já tem o trecho
`madeira → tábua → casa → cama → aldeão`, e é ele que faz a vila crescer
sozinha. Tudo o mais é largura.

---

# 6. Recurso → profissão

| Recurso | Principal | Secundária | Uso | Hoje |
|---|---|---|---|---|
| Tronco | Lenhador | Carpinteiro | casas | ✅ |
| Pedra / pedregulho | Mineiro | Pedreiro | fundação, parede | ✅ / ❌ |
| Cascalho | Mineiro | Construtor | caminho | ⚠️ cai sem uso |
| Argila | Mineiro | Pedreiro | terracota | ❌ |
| Ferro | Mineiro | Armeiro, Ferreiros | ferramenta, armadura | ✅ / ❌ |
| Carvão | Mineiro | Fundidor, Ferreiros | combustível, tocha | ✅ |
| Diamante | Mineiro | Ferreiros | equipamento | ⚠️ coletado, sem uso |
| Trigo, cenoura, batata, beterraba | Fazendeiro | Açougueiro | alimento | ✅ colheita |
| Feno | Fazendeiro | Pecuarista | animal | ❌ |
| Lã | Pastor | Fabricante | **cama** | ✅ |
| Couro, carne | Pecuarista | Coureiro, Açougueiro | equipamento, alimento | ❌ |
| Peixe | Pescador | — | alimento | ❌ |
| Cana-de-açúcar | Fazendeiro | Bibliotecário | papel | ❌ |
| Pena | Pecuarista | Flecheiro | flecha | ❌ |
| Areia | Mineiro | Fundidor | vidro | ✅ coletada, elo nunca fechou em jogo |
| Vidro | Fundidor | Construtor | janela | ✅ |
| Redstone, lápis | Mineiro | Clérigo | tecnologia | ⚠️ coletados, sem uso |

---

# 7. As fases da vila

Cada fase é um **estado do motor**, medido da vila — não uma etiqueta
posta à mão. As condições abaixo são a proposta de régua.

## Fase 1 — Acampamento

```text
aldeões       1 a 5
camas         as que a vila gerada trouxe
profissões    Lenhador, Mineiro, Fazendeiro
objetivo      sobreviver: madeira, pedra e comida em estoque
destrava      estoque mínimo de madeira e pedra para uma casa
```

**Estado: ✅ é onde o mod roda hoje.**

## Fase 2 — Vila

```text
aldeões       6 a 15
constrói      casas, e a rua que as liga
profissões    + Pastor, Fabricante, Construtor, Fundidor
objetivo      excedente: produzir mais do que consome
destrava      primeira casa terminada só com material da própria vila
```

**Estado: 🔨 quase.** A casa já sobe do começo ao fim; falta ela subir
**sem a barreira de teste** — 19 peças vieram dela na última verificação.

## Fase 3 — Vila desenvolvida

```text
aldeões       16 a 30
constrói      fazenda, estábulo, armazém, oficinas
profissões    + Pedreiro, Pecuarista, Açougueiro, Coureiro, Carpinteiro
objetivo      especialização: cada material tem dono
destrava      Armazenista e estoque central
```

**Estado: ⬜ não começado.** O primeiro passo é o **Pedreiro**, que fecha
a cadeia da pedra.

## Fase 4 — Cidade

```text
aldeões       31 a 60
constrói      quartel, biblioteca, mercado, área industrial
profissões    + Ferreiros, Armeiro, Flecheiro, Bibliotecário, Guarda
objetivo      autonomia: ferramenta e defesa produzidas dentro
destrava      árvore tecnológica pelo Bibliotecário
```

## Fase 5 — Cidade autônoma

```text
aldeões       60+
produz        ferramenta, arma, armadura, poção, encantamento
profissões    + Cartógrafo, Clérigo, Explorador, Agrimensor
objetivo      expansão territorial
```

**A tecnologia pelo Bibliotecário** é a ideia mais forte da proposta:
transformar experiência em capacidade da vila.

```text
nível 1   casas melhores
nível 2   ferramentas avançadas
nível 3   oficinas
nível 4   estruturas especializadas
nível 5   tecnologia avançada
```

---

# 8. A ordem que eu recomendo

Ancorada no que existe, e não no que é mais bonito de escrever.

| | O quê | Por quê agora |
|---|---|---|
| **1** | **Fechar o mineiro (E33)** | Sete sessões sem cavar um bloco. **Toda a cadeia da pedra e do metal depende dele**, e nenhuma profissão nova compensa isso |
| **2** | **Casa sem barreira de teste** | É o que fecha a Fase 2 de verdade. Enquanto 19 peças vierem da barreira, a vila não é autônoma |
| **3** | **Pedreiro** | Fecha `Mineiro → pedra → Pedreiro → Construtor`. Hoje o Construtor pede escada e laje e ninguém as faz |
| **4** | **Armazenista e estoque central** | Antes de dobrar o número de profissões. Com 15 trabalhadores, "cada um no seu baú" para de escalar |
| **5** | **Pecuarista** | Destrava couro, carne e pena de uma vez — três cadeias por uma profissão |
| **6** | **Transportador** | Só depois do estoque central; sem ele não há o que transportar |
| **7** | **As 13 vanilla, uma a uma** | Na ordem em que a Fase 3 e a 4 pedem |
| **8** | **Árvore tecnológica pelo Bibliotecário** | Depende de tudo acima, e é o que transforma o mod em simulador |

---

# 9. O que este plano não decide

Fica dito para não parecer decidido:

- **Como o Armazenista e o Transportador convivem com o baú por
  trabalhador.** Hoje a posse do baú é uma regra forte do mod
  (`Storage-System.md`), e um estoque central a contradiz. É uma ADR
  própria.
- **Se as 13 vanilla mantêm o comércio.** A proposta as transforma em
  agentes econômicos; o comércio com o jogador é outra coisa, e as duas
  podem coexistir ou não.
- **O número de aldeões por fase.** Os intervalos acima são proposta, não
  medida. A régua de verdade sai da primeira vila que chegar lá.
- **Se o Carpinteiro se separa do Fabricante.** Hoje são a mesma
  profissão, e dividir por dividir só acrescenta um baú.
