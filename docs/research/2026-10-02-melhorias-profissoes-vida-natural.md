# Melhorias das profissões: vida natural e jogo fluido — 2026-10-02

> Pedido do autor: *"pesquise melhorias para todas as profissões do mod,
> visando deixar a jogabilidade mais natural e fluida, simulando uma vida dos
> aldeões com evolução natural da vila; pesquise outros projetos e ideias,
> avalie e ofereça uma lista de melhorias e seus impactos, quais recomenda e
> quais contrariam regras atuais"*.
>
> É só estudo: nada foi implementado. Complementa
> `2026-10-01-novas-atividades.md` (ações no tempo ocioso A1–A7, ofícios
> estendidos E1–E4, profissões novas N1–N3), sem repetir o que está lá.
>
> **Esforço:** P (horas), M (um dia), G (vários dias, pede ADR).
> **Impacto** é medido no que o autor vê em jogo: menos espera, menos
> aldeão parado, vila que parece viva.

---

## 1. De onde vêm as ideias

| Projeto | O que tem que serve aqui |
|---|---|
| **MineColonies** | Saciedade (0–20) gasta no trabalho; felicidade (1–10) recalculada toda noite por comida, moradia e segurança; o nível do trabalhador sobe com a prática; restaurante e cozinheiro; entregador |
| **Millénaire** | A vila cresce sozinha e melhora as casas; casais têm filhos, que viram adultos; cada cultura com seus prédios |
| **MCA Reborn** | Cada aldeão com nome, personalidade, humor e genética; filhos que crescem e fazem tarefas |
| **Liberty's Villagers** | Desvia de perigo; corrige a rota contra cerca e muro; fazendeiro escolhe a cultura; pescador pesca e cozinha; ferreiro cura o golem; o aldeão não procura posto de trabalho à noite |
| **AI Minecraft Villager** | Rotina diária (dormir, conviver, trabalhar); defesa contra monstros |
| **Banished** | Envelhecimento; escola; a comida só vale depois de chegar ao depósito; distância até o depósito custa produtividade |
| **RimWorld** | Prioridade de trabalho de 1 a 4; agenda (sono, trabalho, lazer); necessidades que, abaixo de um limite, passam à frente do trabalho |

---

## 2. O que os playtests já mediram

Toda melhoria abaixo responde a algo medido, ou é dita como "só aparência".

| Sinal | Onde | Número |
|---|---|---|
| **Mineiro volta sempre ao mesmo alvo inalcançável** | 02-10, 01:36–02:23 | 7 encalhes em `-241, 10, 379`, a cada ~2 min |
| **Fundidor varre o raio inteiro sem achar areia** | 02-10, 00:43 | 38 varreduras em 20 min; 0,55% do servidor |
| **Lenhador re-examina tora de casa da vila** | 02-10 | 124 recusas; 0,7% do servidor |
| **Tarefa só a cada 30 s** | Fluidez F1–F14 (30-09) | o trabalhador fica parado entre um ciclo e outro |
| **Obra esperando material que existe na cadeia** | todos os playtests | `waiting for glass_pane / wall_torch` |
| **Ofício invisível** | observação | fundir e fabricar acontecem no baú, sem fornalha nem bancada |

---

## 3. A lista

Legenda da última coluna:
- ✅ não toca regra;
- ⚠️ pede emenda pequena ou decisão;
- ⛔ contraria regra ativa.

### 3.1 Fluidez: menos espera e menos tentativa perdida

| # | Melhoria | Profissões | Impacto | Esforço | Regra |
|---|---|---|---|---|---|
| **F-1** | **Alvo inalcançável tem espera própria e crescente.** O mineiro deixa de voltar à pedra do outro lado da caverna; o fundidor espera 5 → 10 → 20 min depois de varrer sem areia; o lenhador descarta tora de casa da vila antes de medir a copa | mineiro, fundidor, lenhador | **alto:** acaba com os 7 encalhes, as 38 varreduras e as 124 recusas medidas | M | ✅ Regra 23 ("analisado se analisa de novo") continua valendo, porque a espera expira |
| **F-2** | **Encadear a próxima tarefa ao concluir.** Quem termina pega a próxima do mesmo ofício na hora, sem esperar o ciclo de 30 s | todas | **alto:** acaba com o "parado olhando o baú" | M | ✅ |
| **F-3** | **Rota da mina salva no save.** O rastro do `MineReturn` (02-10) e a escada da mina gravados, para o retorno valer também depois de carregar o mundo | mineiro | médio: o retorno hoje só vale depois de ele descer na sessão | P–M | ✅ |
| **F-4** | **Fila de material da obra visível.** `/vc log` diz "a obra espera vidro; falta areia no raio" em vez de só "esperando" | construtor, fundidor | médio: o jogador entende o porquê | P | ✅ |
| **F-5** | **Descer com a mochila, subir uma vez.** O mineiro enche o inventário (64) antes de voltar ao baú, em vez de viagens curtas | mineiro | médio: menos ida e volta na escada | M | ✅ conferir a Regra 30 (recolhe tudo) |

### 3.2 Ofício visível: o aldeão trabalha onde se vê

| # | Melhoria | Profissões | Impacto | Esforço | Regra |
|---|---|---|---|---|---|
| **V-1** | **O carpinteiro e o pedreiro fabricam de pé na bancada** (crafting table, stonecutter) da própria casa, com o gesto de braço | carpinteiro, pedreiro | **alto em naturalidade**, nulo em produção | M | ✅ Regra 44 bloqueia o ofício Vanilla, não o posto |
| **V-2** | **O fundidor usa uma fornalha de verdade.** Põe o cru e o combustível e tira o fundido no tempo do jogo | fundidor | alto: a fumaça acesa é a vila trabalhando | M | ⚠️ a fundição fica mais lenta; a meta de estoque (Regra 40) precisa conviver com o tempo real |
| **V-3** | **O construtor carrega o bloco na mão** e assenta de baixo para cima, camada por camada | construtor | médio | P | ✅ |
| **V-4** | **O fazendeiro leva a colheita ao composteiro e ao baú da roça**, não ao baú pessoal (com o A4) | fazendeiro | médio | M | ✅ |

### 3.3 Rotina de vida

| # | Melhoria | Profissões | Impacto | Esforço | Regra |
|---|---|---|---|---|---|
| **R-1** | **Encontro no sino ao fim do expediente.** O `VillageMeals` já marca a hora; ali o aldeão vai ao sino, conversa (o gesto Vanilla) e come | todos | **alto em vida da vila**; a refeição já existe | P | ✅ é a hora à toa (Regra 18: "a última hora de luz é dele") |
| **R-2** | **O pastor recolhe o rebanho no curral ao entardecer** e fecha a porteira (reusa `FencedIn`) | pastor | médio, e natural: o curral passa a ter função | M | ✅ |
| **R-3** | **Felicidade da vila** (camas livres, comida no baú, ninguém encalhado), calculada uma vez por noite, só como **ritmo de procriação**, nunca como freio de trabalho | vila | médio: crescimento natural, mais rápido em vila bem cuidada | M | ✅ não toca o trabalho; ⚠️ é decisão de desenho |
| **R-4** | **Aprendiz.** A criança acompanha um adulto da profissão que a vila vai precisar e, adulta, recebe esse ofício primeiro | vila | médio em vida; nenhum em produção | M | ✅ o `ProfessionAssigner` (Regras 11 e 38) continua decidindo |
| **R-5** | **Golem patrulha as ruas** entre o sino e as pontas de rua, em vez de vagar | golem | baixo; só aparência | P | ✅ |
| **R-6** | **Pausa ao meio-dia** (almoço no sino) | todos | médio em vida; **perde produção** | P | ⛔ **Regra 18** ("enquanto houver sol, trabalha") |
| **R-7** | **Abrigo na chuva e na tempestade** para o trabalho de fora | lenhador, fazendeiro, pastor, construtor | médio em vida; perde produção | M | ⛔ Regra 18; ⚠️ emenda: "chuva forte conta como noite para quem trabalha fora" |

### 3.4 Evolução natural da vila

| # | Melhoria | Impacto | Esforço | Regra |
|---|---|---|---|---|
| **E-1** | **A casa do ofício surge quando o ofício existe:** armeiro, bibliotecário, oficina, do catálogo do jogo, depois da moradia | **alto:** a vila ganha forma de vila | M | ⛔ **Regra 28** (provisória: só a casa pequena). É a regra que trava a evolução; a Regra 25 (maior planta) acorda junto |
| **E-2** | **Sino e praça quando a vila passa de N adultos** (`meeting_point` do catálogo) | médio | P | ✅ Regra 27, porque é do catálogo |
| **E-3** | **Iluminar e manter as ruas** (A2/A3 do estudo de 01-10) | médio | M | ✅ |
| **E-4** | **Melhorar a casa velha** trocando-a por uma maior (Millénaire) | alto em evolução | G | ⛔ **Regra 3** (nunca destruir construção da colônia nem da vila) |

### 3.5 Contrariam regras atuais (não recomendo sem decisão do autor)

| Ideia | Fonte | Regra |
|---|---|---|
| Prática acelera o trabalho; nível do trabalhador | MineColonies | ⛔ **Regra 2** (tempo de jogador com ferro) e **Regra 37** (ferramenta de ferro) |
| Fome que para o trabalho ou dá lentidão | MineColonies | ⛔ choca com a fluidez; hoje a comida só decide a procriação (decisão N1) |
| Envelhecer e morrer de velhice | Banished, MCA | ⛔ decisão de 24-09: "a fundação não repõe quem morre" — a vila encolheria e a Regra 11 ficaria descoberta |
| Prédio fora do catálogo do jogo (taverna, escola, restaurante) | MineColonies, Millénaire | ⛔ **Regra 27** (imutável) |
| Comércio com o jogador para crescer | Millénaire | ⛔ pilar do projeto ("o jogador acha a vila e vai embora") e **Regra 44** |
| Profissão nova (guarda, pescador, cozinheiro) | MineColonies, Liberty's | ⛔ **Regra 11** e **Regra 35** (7 titulares da BigHouseMOD), com ADR; como ação de ofício já existente (E2 pesca, de 01-10), não contraria |
| Voltar a recolher drop de verdade em vez do automático | naturalidade | ⛔ **ADR-028 / Regra 41** (decisão de 30-09) |

---

## 4. Recomendação

**Ordem sugerida.** Primeiro o que destrava fluidez medida; depois o que se vê.

| Ordem | Item | Por quê |
|---|---|---|
| 1 | **F-1** espera do alvo inalcançável | três defeitos medidos de uma vez; ~1,2% do servidor |
| 2 | **F-2** encadear a tarefa | o maior "parado" que sobra (Fluidez F1) |
| 3 | **R-1** encontro no sino ao entardecer | vida da vila quase de graça (`VillageMeals` já existe) |
| 4 | **V-1** bancada e cortador visíveis | o ofício passa a ser visto |
| 5 | **R-2** pastor recolhe o rebanho | dá função ao curral, que hoje só prende |
| 6 | **F-3** rota da mina no save | completa o retorno pela mina de 02-10 |
| 7 | **E-1** casa do ofício | **pede tirar a Regra 28** — é a maior mudança de "vila que evolui" |
| 8 | **R-3** felicidade como ritmo de procriação | crescimento natural, sem frear o trabalho |

**Decisões que só o autor pode tomar:**
- Regra 28 (sair da barreira de teste);
- Regra 18 (pausa e chuva);
- R-3 (felicidade);
- V-2 (fornalha real e mais lenta).

Cada item nasce como os de 01-10: regra no `RULES.md` com chave para
desligar, GameTest que falha sem ele, e a linha de log que o prova em jogo.

---

## Fontes

- MineColonies: [felicidade](https://www.wiki.minecolonies.com/source/systems/happiness.html), [comida](https://minecolonies.com/wiki/needs/food/), [saciedade](https://steemit.com/utopian-io/@raycoms/minecolonies-and-new-saturation-system), [pesquisa](https://minecolonies.com/wiki/systems/research/)
- Millénaire: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/millenaire), [porte 1.21.1](https://github.com/Leviaria/Millenaire), [wiki](https://minecraft-mods.fandom.com/wiki/Millenaire)
- MCA Reborn: [Modrinth](https://modrinth.com/mod/minecraft-comes-alive-reborn), [MCA Social Expansion](https://www.curseforge.com/minecraft/mc-mods/mca-social-expansion)
- Liberty's Villagers: [Modrinth](https://modrinth.com/mod/libertyvillagers)
- AI Minecraft Villager: [player.games](https://www.player.games/en-US/creator-hub/minecraft/community/aiminecraftvillager)
- Banished: [envelhecimento](https://steamcommunity.com/app/242920/discussions/0/45350244926731941/), [guia](https://steamcommunity.com/sharedfiles/filedetails/?id=399003504)
- RimWorld: [trabalho](https://rimworldwiki.com/wiki/Work)
