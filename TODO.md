# TODO

**Atualizado:** 2026-09-15, depois da reavaliacao controlada da falha de CI e da entrega P0.7.

> **Este arquivo é a lista viva.** Só o que está aberto agora.
> O histórico — sessões por data, ciclos fechados, erros resolvidos — está
> em [`TODO-archive.md`](TODO-archive.md).
>
> Onde este arquivo e o `Backlog.md` discordarem, **vale este**.
> Onde este arquivo e o `Plano-de-Correcao.md` discordarem sobre *o que já
> foi feito*, vale este. Sobre *o que fazer e em que ordem*, vale o plano.

---

## Plano de trabalho por lotes — 2026-09-14

Plano solicitado para manter mineiros em atividade, variar as estruturas
tentadas pelos construtores, ampliar a avaliação de espaços e revalidar após
falhas repetidas: [plano completo](docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md).

**Lote 1 concluído em código:** emenda da ADR-012 e reconciliação local de
alterações do jogador no scanner; 324/324 GameTests e `build` passaram. Ainda
aguarda validação em jogo. **Lote 2 começou com uma correção pontual
aprovada:** job fechado agora libera imediatamente a claim do ramal do
mineiro. A continuidade/recuperação restante segue sujeita a revisão do
autor; lotes seguintes cobrem variedade dos construtores, estratégias de
avaliação de espaço e revalidação escalonada.
“Continuar trabalhando” respeitará expediente, recursos, perigo e chunks
carregados, sem criar recursos ou tarefas fisicamente impossíveis.

**AUD-001 continua aberto:** a asserção Linux histórica foi recuperada e a
contraprova confirmou que o teste acusa a ausência real de coleta quando a
regra de terra é desligada. A suíte local fresca passa 324/324; porém a perda
do aldeao entre `spawnEntity` e o tick 1 ainda nao tem causa deterministica,
e a execucao Linux da revisao ainda nao existe. Nao fechar o Lote 0 nem
aumentar timeout por este achado; P0.7 e uma decisao independente ja entregue.

---

## ⏭️ Por onde começar

**P0.7 entregue em codigo e no JAR, espera playtest.** A politica aceita qualquer piso
solido disponivel, sem taxonomia geologica e sem terraplanagem. Os unicos
materiais de estrada sao `dirt_path`, `gravel` e `terracotta`, e so bloqueiam
o lote dentro de `ROAD_AREA`. A decisao e a verificacao de 327/327 GameTests
estao na ADR-017. O JAR 0.3.0 foi distribuido em 2026-09-15 com SHA-256
`7B2C820AA298FF72DF1D0BC00B0BC0AD66A5359417B7F5C9950B66FA14725F44`.

**E a varredura não era a culpada — o instrumento do projeto disse isso por
escrito.** O `SweepLog` gravou no encerramento:
`47 planner runs, 0 passes over 0 columns, 0 complete rounds`, com
`46 of 47 planner runs gave up before reaching the sweep` e o aviso
*"the sweep is not why: most cycles gave up before reaching it. Look at
what the planner refused, not at the sweep."* **Zero passagens em 47
tentativas.** A hipótese de orçamento de varredura foi descartada por
medição, não por leitura.

✅ **Resolvido em 2026-09-12:** o defeito latente do `CropPatch.survey` foi
confirmado com GameTest e corrigido. O canteiro vazio achado numa fatia
pausada agora é lembrado por colônia enquanto o `RingSweep` não fecha a
volta; a lembrança é validada contra o mundo antes de ser reaproveitada e
limpa junto com a varredura do fazendeiro.

✅ **Auditoria 1.9 registrada em 2026-09-12:** nenhum CRÍTICO novo foi
confirmado. O ALTO era de release: não havia workflow de CI/CD, e o
manifesto publicado aceitava qualquer Fabric API com `"*"`. Entrou
`.github/workflows/ci.yml` com actions pinados por SHA, Java 21,
unitários, Python, `build`, `runGametest`, artefato do jar e relatórios
em falha. O `fabric.mod.json` agora exige a Fabric API da matriz por
expansão do Gradle, e `ModMetadataTest` impede o curinga de voltar.
Registro completo em
[`docs/technical/Auditoria-2026-09-12.md`](docs/technical/Auditoria-2026-09-12.md).

---

## Playtest de 2026-09-14 — casa sem avanço visível

O log registra seleção e abertura de `plains_butcher_shop_2` às 00:50:15,
com 382 blocos. Dois foram assentados; a obra ficou com 380 em
`WAITING_RESOURCES` esperando `minecraft:dirt` por mais de seis minutos. Os
18 baús tinham 33 `grass_block` e nenhum `dirt`. A causa imediata era uma
lacuna na cadeia de produção: o catálogo reconhecia areia e grama, mas não
terra, então nenhum trabalho de superfície do fundidor atendia essa demanda.

**Corrigido e testado:** `DIRT` entrou no catálogo de recursos e no conversor
Vanilla; o fundidor busca terra exposta fora do raio protegido, no setor
cardinal definido para coleta externa, sem carregar chunks. GameTests cobrem
tipo/tarefa e o ciclo físico de quebrar e guardar terra. `build` passou e
`runGametest` passou com 320/320. **Ainda requer JAR atualizado e confirmação
em jogo:** coleta de terra, retomada dos 380 blocos e eventual bloqueio de
alcance observado depois da espera por material. Não foi alterada a escolha
da construção: o log prova que ela já foi aberta.

---

## 🎮 Sessão de 2026-09-13

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3`, em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu:**

- Casa parada no meio.
- Mineiro invisível / parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a
   `STABLE`, reduzindo o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco
faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A
carpintaria fabricava composteiras, mas usava a porta de consumo
(`ColonySupply.take`) e retirava do baú a peça que acabou de produzir.
Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`,
que fabrica e deixa a peça no estoque para o construtor assentar.

**Reinvestigação após o playtest:** novo GameTest reproduziu `minecraft:dirt`
sendo colocado sem estoque porque `BlockTags.DIRT` era tratado como terra
moldada no local. A exceção agora só cobre farmland, água, `dirt_path` e
cultivos. `runGametest` (313) e `build` passaram. **Pendente:** confirmar
em jogo com o JAR atualizado se a obra deixa de assentar terra sem material
e continua construindo; o log anterior veio de um JAR antigo e não registra
o bloco/posição exata da terra vista.

## Reanálise do playtest de 2026-09-14

O log usado pelo launcher veio de JAR anterior à correção de terra. As casas
foram selecionadas, mas aguardaram `dirt`; o mineiro estava sem tarefa, não
preso na mina: havia apenas 3 carvões e nenhuma reserva de carvão/ferro definida.
Lenhadores cortavam sem consultar a proteção de estruturas e podiam confundir
troncos de casas com árvore de copa compartilhada.

**Implementado neste ciclo:** metas de 64 carvões e 64 minérios de ferro brutos,
somadas à demanda da obra; proteção da árvore no planejamento e revalidação de
cada bloco antes de quebrar. Testes de jogo cobrem estrutura da colônia
registrada antes/depois do plano. Troncos manuais sem marca ou folha persistente
continuam indistinguíveis de árvores; proteção adicional exige decisão sobre
marcação/persistência.

**Verificado e distribuído:** `build` passou, 322/322 GameTests passaram, e
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/` têm SHA-256
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.

**Próximo passo: validar em jogo.** A casa deve avançar além de 380 blocos; o
mineiro deve abrir tarefas e manter os pisos minerais; o lenhador deve cortar
árvores naturais por inteiro e preservar estruturas Vanilla e da colônia.
Fazenda e demais profissões não têm falha comprovada neste log.

**Playtest seguinte, 2026-09-13:** nenhuma construção visível e baús de
profissões recebendo produção cruzada. O log associado mostra somente três
peças assentadas; construtor encerrou por falta de `grass_block` e
`cobblestone`; mineiros repetiram falta de espaço para ficar em pé e
produziram zero; lenhadores e agricultores colheram; fundidores pararam por
falta de areia. A produção agora vai ao baú pessoal do profissional, com
insumos compartilhados. Sem espaço, transformações devolvem o insumo e
drops ficam no mundo. `build` e 313 GameTests verdes; **aguarda validação em
jogo**. A investigação da construção e da mineração segue aberta.

**Diagnóstico do log de 09-13:** o mineiro completou 64/64 pedregulhos e
ficou sem tarefa; depois recebeu areia para vidro, mas `MinerWork.Job` usava
a pedra da paleta e mantinha o progresso em `0 of 3`. Corrigido e coberto
por teste; `build`: 810 unitários, `runGametest`: 314/314. Depois da correção,
a tarefa ainda depende de encontrar areia: o log diz repetidamente que não
há areia num raio de 48 blocos. **Aberto:** implementar/validar o retorno
explícito do mineiro à boca quando a tarefa termina; `MinerWork.tick` hoje
descarta o Job encerrado sem criar uma rota de volta.

A obra `plains_butcher_shop_2` esperou `grass_block` (382 blocos restantes),
desistiu e manteve lote/obra parcial. Nenhuma profissão do catálogo produz
`grass_block`. **Aberto:** impedir seleção/reserva de blueprint sem cadeia de
materiais possível e definir recuperação da obra parcial, respeitando a
propriedade do lote. Fazenda: sem cultivo maduro ou lote vazio em 32 blocos.
Lenhadores cortaram e produziram durante a mesma sessão, embora às vezes
tenham ficado sem árvore próxima. Carpinteiros, fundidores, pedreiros e
pastor registraram "no task open"; o carpinteiro também ficou no baú em
`0/20` ticks fora do expediente. Isso aponta falta de demanda/insumos no
recorte observado, não falha de execução confirmada dessas profissões.

**Alterações do jogador, aplicadas em 2026-09-13 (ADR-012):** quebras e
interações que mudam blocos agora invalidam a busca local de lotes; trechos
abertos pelo jogador reabrem braços já percorridos da mina. A busca continua
limitada por tick e sem forçar chunks. `build` e 314 GameTests verdes. **Na
próxima sessão, verificar retomada da casa e reabertura da mina.** ADR-013
resolveu E45 (geometria/ramais), sem cadastrar baús arbitrários: o estoque
é vivo nos baús já vinculados ao trabalhador e na boca da mina.

**E45 resolvido em 2026-09-13 (ADR-013):** no fundo da mina, quando todos os
braços terminam, a hélice muda de orientação sem mover a boca. A galeria
continua usando `turned()` para seus quatro braços. A geometria agora é
`SHAPE_VERSION` 5; saves v4 zeram os cursores e preservam entrada, rumo e
arco. Testes focados reproduziram as duas falhas antes da correção; `build`
passou com 808 unitários e `runGametest` com 314/314. Falta confirmar no
mundo do autor se o mineiro percorre e trabalha na nova hélice.

---

## 🔴 P0 — bloqueadores

Um por vez, teste antes de seguir.

| item | o quê | estado |
|---|---|---|
| **P0.1** | O planejador não acha lote | ✅ entregue 09-11 · **visto em jogo** |
| **P0.1-b** | O caminho de terra não sai de baú | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.1-c** | A recusa de lote diz por quê | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.3** | Mineiro → armazenamento → fundidor | ✅ conserto entregue 09-11 · ⬜ **espera sessão** |
| **P0.5** | Perda de item por inventário cheio (E3) | ✅ corrigido e testado · ⬜ **espera sessão**; depósito pessoal revisto em 09-13 |
| **P0.6** | A enxurrada da areia calou | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.7** | Elegibilidade simplificada de lotes | ✅ entregue 09-15 · ⬜ **espera playtest** |

---

## 🔴 Erros abertos

| | erro | estado |
|---|---|---|
| **E44** | A escada de recusas já existe em `MineMarks` e é consultada pela mineração e pela fronteira da galeria; há unitários e GameTests. O playtest ainda observou o mineiro parado, então a integração completa segue **sem validação em jogo**. Não reabrir a decisão original sem reproduzir um defeito residual. | ⬜ validar em jogo |
| **E43** | O descanso de quatro ciclos é anulado no ciclo seguinte. O `giveUp` do mineiro marca `worker.rest(COLLECT_STONE)`, e a 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador sempre que a colônia não tem outro trabalho da profissão dele. **Decisão de projeto, e é do autor** | 🟠 aberto |
| **E41** | Nada mede degradação ao longo de muitos ciclos. O teste mais longo do projeto tem centenas de tiques. **Maior lacuna de cobertura depois do E37-b.** | 🟠 aberto |
| **E42** | Nenhum teste de impasse entre profissões. Os dois casos reais — a roça que travava toda a construção, e o fabricante que nunca descascava — foram achados **em jogo**, não pela bateria. **A tentativa de 09-09 à noite foi retirada pelo gauntlet-verifier** — os três casos escritos eram a invariante de vários trabalhadores, com outro nome. **O trabalho de verdade é outro gametest:** lote de roça fora do alcance do fazendeiro, planta de casa disponível, duas passagens do planejador, e a segunda tem de abrir projeto de CASA | 🔴 aberto |
| **E38** | O baú pessoal pode assorear com vara, maçã e muda sem consumidor; a colheita não transborda para outra profissão e itens sem espaço viram drops no mundo. Definir tratamento sustentável dos resíduos sem misturar depósitos | ⚙️ aberto |
| **KF-001** | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard`. **Fechado em 09-09:** a afirmação lia o estado da tarefa depois que o ciclo podia reservá-la novamente; o teste passou a registrar o instante da devolução e força a fase do ciclo. O orçamento global de uma busca/tique continua sendo um risco separado de vazão, não a causa provada da falha. | ✅ teste corrigido; medir vazão se houver evidência |
| **E21** | `theStoneLeavesTheWorldAndReachesTheChest` disse "a pedra não chegou ao baú" uma vez. Suspeita: custo de ler estrutura no tique. **Suspeita, não diagnóstico** | 🟡 aberto |
| **E4** | `path held: no` e o aldeão chega assim mesmo. Provável, nunca verificado | 🟡 aberto |
| **E3** | Sobra de colheita é perda de item. **Metade fechada em 09-04** — o lenhador deixou de destruir; **o mineiro continua sem teto de inventário** | ⚙️ metade fechada |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte. **Mitigado em 09-13** — precisa de duas leituras positivas seguidas | ⚙️ mitigado |

---

## 🟠 Pendências abertas

| | o quê |
|---|---|
| **Mina de vila nova sem portal visível** | Confirmado no escalonador: uma única busca global por tique ficava presa no primeiro trabalho sem alvo, impedindo os mineiros seguintes de iniciar a busca. O rodízio foi corrigido sem aumentar a cota; `build` e 314 GameTests verdes. **Aguardar validação em jogo**: portal aparece e o mineiro inicia a escavação. |
| **Terra comum em blueprint** | Corrigido localmente: agora exige estoque. 313 GameTests e build verdes; **aguarda validação em jogo**. |
| **P0.7 — elegibilidade simplificada de lotes** | ✅ entregue 09-15; piso solido e elegivel, estrada exige `ROAD_AREA`; 327/327 GameTests e JAR atualizado. Espera playtest. |
| **A casa ainda sobe com a barreira de teste** | Última medição: 47 de 169 peças em 09-04. É o item que fecha a Fase 2 de verdade |
| **O lenhador rejeita as paredes da própria vila** | 118 rejeições sobre 28 posições. O castigo escalona (6.000→48.000) e funciona, mas ele redescobre a mesma parede seis vezes. **Nenhuma recusa veio com o número 24** |
| **O fundidor não tem o que fundir** | `nothing in the colony chests to smelt`, 34× na sessão de 09-04. Deve seguir o E44 |
| **Cobertura de qualquer material de construção** | ADR-015: fallback para fundidor e criador e estoque limitado pela capacidade física. Em 09-14, contagem e localização por `ResourceId` entraram em `ResourceTally`/`ColonyResources` e no leitor de baús (317 GameTests verdes). Metas, tarefas e execução profissional para IDs arbitrários continuam pendentes; não declarar cobertura funcional ainda. ADR-016, Lote 1. |
| **Mineiro longe do corredor não tem resgate** | Sem posição da ordem a uma perna dele, não há passo a dar e a boca continua sendo a resposta. Decisão de projeto: caminhar em linha reta, ou devolver a tarefa |
| **A arena de gametest não hospeda a galeria** | Ela assenta no fundo do mundo. Toda a bateria de mineração exercita **só o poço**. A divergência dos ramais é provada por unitário, não de ponta a ponta |
| **Os 49 `assign()` que criam trabalhador de mãos vazias** | Passam hoje por folga no `tickLimit`, não por estarem certos |
| **A proteção não consulta o registro de construções em todos os caminhos** | `BlockProtection` passou a consultar (`isColonyBuilt`), mas o caminho que decide o que pode ser quebrado ainda tem furos |
| **`Colony cycle took 122 ms`** | Contra o limite de 50. Medido em 09-11. **Instrumentar antes de otimizar** — ver P2.1 do plano |

---

## 📋 Pendências por nível de progressão lógica

A ordem é de dependência: cada nível precisa do anterior de pé.

### Nível 0 — o que já roda em jogo

Detecção · identidade estável · aldeões e profissões · baús · lenhador **em cinco espécies** · fabricante, **inclusive descascando e fazendo o que a obra pede dois degraus abaixo** · construtor **chegando ao bloco** · centro parado pela sonda · **a rua crescendo** · **a obra parada saindo da frente** · **a mina abrindo e sendo mobiliada** · **o mineiro cavando** · **o pastor tosquiando** · casa terminada.

### Nível 1 — a raiz do material *(aberta, e ela se fecha sozinha)*

- **A mina abriu em 2026-08-26** — a busca acertou na primeira camada.
- **O mineiro cavou** — 43 blocos numa tarefa só, descendo até y≈44.
- **A galeria o engoliu, e o E30 fechou em 08-27**.
- **Continuar ainda não está provado em jogo**. O conserto tem teste e **nenhuma sessão o viu rodar**. E o **E32** ficou de pé.
- **A pedra de superfície continua sem prova** — nunca foi exercitada.

### Nível 2 — material processado *(feito, e ainda passando fome)*

- **E18 fechado em 08-22**, pelo caminho genérico que a ADR pediu.
- **Nenhuma sessão viu isso rodar** — a linha a procurar é `Smelter ... made minecraft:smooth_sandstone`.
- O fundidor espera **a areia**, e a cadeia da areia começou em 08-20.

### Nível 3 — a obra termina sem o jogador

- **A casa terminou sozinha em 2026-08-26** — 149 blocos planejados, **127 assentados**, em 4 min 57 s.
- **Mas dezenove peças foram da barreira**, não da colônia. *Casa feita inteira com material da própria colônia* **continua sem prova**.
- **A barreira risca antes de a cadeia ter chance**. Enquanto a Regra 28 valer, a soma da sessão superestima o que está quebrado.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
- ✅ **A varredura recomeçar a cada obra fechada** — resolvido em 08-27, o cursor fica onde achou o lote.
- ✅ **Perguntar só às ruas** — resolvido em 08-27 pelo índice.
- ✅ **O cursor da varredura** — resolvido em 08-27, e **confirmado em jogo**.
- ✅ **O veio que desce** — resolvido em 08-27.
- 🟠 **O mineiro cavando de verdade ainda não foi visto em jogo.** **Falta a sessão que confirme.**
- 🟠 **Mineiro longe demais não caminha até a mina.** Não investigado.

### Nível 5 — o motor da ADR-009

`VillageProfile` · inventário de território · escassez e distância · orçamento de recursos · detecção de dependência circular · reserva mínima de sobrevivência · objetivos graduais. **Nada disso existe.**

### Nível 6 — o que nem modelo tem

Comida · água · o fazendeiro (tem enxada e baú desde a Fase 4 e nunca teve código) · população por capacidade · defesa · especialização · comércio entre vilas.

### Fora dos níveis — dívida que não bloqueia

- **13 arquivos de código acima de 500 linhas**, e 11 de teste. `VillageDetectionHandler` é o pior com **1.107**, e o corte dele é o próximo.
- **ADR-008** (orientação) e **ADR-007** (fusão), decididas e por escrever.
- **Regra 16** — distância mínima e máxima entre construções.
- **O ícone** — 1,95 MB num jar de 2,29 MB.
- **Cenário de teste por bioma.** A planície escondeu **duas vezes** que o deserto estava quebrado.
- **O `Development-Log`** está atualizado até 09-15. Cada lote futuro deve registrar ali a evidência, o escopo e o artefato distribuído.

---

## ⚠️ Incompatibilidades — o que se contradiz hoje

| | o quê |
|---|---|
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |

---

## 👤 Decisões que faltam, na ordem em que travam

| | decisão | trava |
|---|---|---|
| 1 | **E43 — o descanso de 4 ciclos deve valer sempre?** | Anulado pela 2ª passagem do `takeOneTask`. Decisão de projeto |
| 2 | **TASK-048 — o que uma colônia ABANDONED deixa de fazer?** | Hoje nada. Ela é marcada e continua sendo simulada |
| 3 | **TASK-044 — a fusão de vilas** | ADR-007 escrita em 08-21, não implementada |
| 4 | **TASK-046 — a orientação dos blocos** | ADR-008 escrita em 08-21, forma (a). Metade do E8 fechou em 08-15; a orientação fica |
| 6 | **E38 — o baú do trabalhador assoreia** | Dar consumidor ou descarte a vara, maçã e muda. **Decisão de projeto** |
| 7 | **E45 — como a mina troca de rota no fundo?** | Geometria, boca estável, migração do save e novo GameTest; não há ADR atual |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto.

| | o quê | a linha que prova |
|---|---|---|
| **1** | **Portal da mina em vila recém-descoberta** | log `opens a mine at ...`; confirmar entrada visível e mineiro iniciando trabalho |
| **1** | **A mina velha destravando** | `Mine ... finished every branch and went one level deeper` |
| **1** | **A varredura acabando num ciclo** | `no building work: still sweeping` aparecendo **uma vez** e não a sessão inteira |
| **2** | **P0.1-b, P0.1-c, P0.3, P0.5, P0.6** | entregues em 09-11, **nunca vistos** |
| **2** | **As quatro correções de 09-13** | sessão do autor |
| **2** | **O mineiro parando à noite** | o contador de `stall` **congelado** com `off hours` |
| **2** | **A ferramenta de ferro na mão** | ferro, e não madeira nem diamante |
| **2** | **Profissões e crescimento (ADR-011)** | sete funções produtoras, ordem de vagas nos adultos 15/16/30/31/32 e produtor construindo sem perder o ofício |
| **2** | **O nome colorido** | sete cores distintas para as profissões ativas, e o nome do jogador **sem** cor |
| **2** | **O arco da boca** | dois pilares, verga e lanterna — **junto** com o baú do mineiro |
| **2** | **Coleta de superfície do fundidor (ADR-014)** | pá com Toque Suave I; areia no baú pessoal e `grass_block` só com obra solicitando, além de 64 blocos e longe das estruturas |
| **3** | **A cadeia da areia inteira** | meta de vidro → fundidor busca areia → vidro → vidraça |
| **4** | **A casa inteira sem a barreira** | `TEST BARRIER covered for nothing` |
| **5** | **A rua crescendo e a casa nascendo junto** | `extended the road N blocks ...` seguido de `planned ... at ...` no mesmo ciclo |
| **6** | **A casa de deserto subindo** | `blocks left` caindo de 113 |
| **7** | **Fechar e reabrir o mundo com mina aberta** | a **galeria retomada** |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície — taiga, savana, nevada e deserto nunca rodaram, e todas as sessões até hoje foram em planície.

---

## ⚙️ Ciclo de 2026-09-11 — recusados

**Três itens do plano não se fazem**, e os três pelo mesmo motivo: o plano foi escrito a partir deste arquivo, e as linhas que ele copiou **já estavam vencidas**.

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto**: é o primeiro critério de ordenação da obra, e é o que põe mobília depois da casa inteira |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |
| **P1.7** | Separar `WOOD` por espécie **refaria o defeito de 09-10** |

---

## 📌 Notas do ciclo de 09-12

### ✅ 2026-09-12 — duas sessões de jogo, dois defeitos de "uma vez só"

**O arco da mina voltava depois de quebrado.** `Mine.archRaised` entrou no save sob a chave `arch`, **fora do `SHAPE_VERSION` de propósito**. O critério levou três reprovações do `gauntlet-verifier`, e as três viraram gametest.

**O lenhador deixava tronco de pé — e era o E39.** A causa não era alcance. **E o ciclo vicioso que ninguém havia medido:** expulsão deixa tora de pé → `markUnreachable` pula a árvore por 6.000 tiques → a copa decai nesse tempo → o resto vira `N logs without a living canopy`, que é recusa **definitiva**.

**Verificado:** 780 unitários e 304 gametests. Cada um dos quatro testes novos foi visto falhando contra a versão que acusa.

---

## ⚙️ Ciclo de 2026-09-10 — recusados

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto** |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |

---

## 📎 Referências rápidas

- **Plano de correção:** [`docs/technical/Plano-de-Correcao.md`](docs/technical/Plano-de-Correcao.md)
- **Histórico por data:** [`TODO-archive.md`](TODO-archive.md)
- **Estado vivo:** [`STATE.md`](STATE.md)
- **Assinaturas de defeito:** [`docs/PATTERNS.md`](docs/PATTERNS.md)
- **Regras do autor:** [`docs/RULES.md`](docs/RULES.md)
- **Estado detalhado (histórico):** [`docs/technical/Project-State.md`](docs/technical/Project-State.md)
- **Próxima sessão de jogo:** [`docs/proxima-sessao.md`](docs/proxima-sessao.md)
- **Responsabilidade das profissões:** [`docs/technical/Profession-Responsibility.md`](docs/technical/Profession-Responsibility.md)
- **Regressões catalogadas:** [`docs/behavioral-tests/REGRESSION-HISTORY.md`](docs/behavioral-tests/REGRESSION-HISTORY.md)
- **Falhas conhecidas:** [`docs/behavioral-tests/known-failures.md`](docs/behavioral-tests/known-failures.md)

---

## Nota sobre este arquivo

**Regra de manutenção:** quando um item fecha, ele **sai** daqui e vai para o `TODO-archive.md`, na seção `✅ Resolvido` do mês correspondente. Quando um item novo abre, ele entra na seção apropriada — **nunca** em bloco de "ciclo".

**Meta de tamanho:** 150 linhas. Se passar, algo está sendo arquivado no ritmo errado.

**O que este arquivo não é:** não é log de sessão, não é histórico de decisão, não é lugar de guardar "como se chegou aqui". Isso vai para `Development-Log.md` e `TODO-archive.md`.
