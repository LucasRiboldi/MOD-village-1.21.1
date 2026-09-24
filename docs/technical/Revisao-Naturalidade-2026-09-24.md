# Revisão de naturalidade — 2026-09-24

**Pedido do autor:** *"faça varredura e localize ideias ruins do projeto que
podem ser melhoradas, para o mod parecer uma vila crescendo naturalmente,
influenciando pouco no vanilla"*.

> **Decisão do autor (24-09), e estado:**
>
> - **N1**: aplicado, com "um aldeão por cama da BigHouseMOD".
> - **N2**: recusado; a casa fica.
> - **N3**: aplicado, com equivalente antes e a peça pronta só sem rota.
> - **N4**: fica como está por ora.
> - **N5**: rua em caminho de terra. **Já era assim** desde o P0.7
>   (`VillageRoad.java:22`). Esta revisão errou ao listá-lo; o resto do N5
>   (tocha e arco grátis) não foi pedido.
> - **N6 e N7**: ficam ligados; a placa sobe para topo + 5.
> - **N8**: recusado.
> - **N9**: aplicado, na ordem casa → oficina de ofício → o que sobrou.
> - **N10**: aplicado.
> - **N11**: aplicado.
>
> Commits e verificação em `STATE.md`.

**Régua usada:** a pergunta é se um jogador que chega sem ler o log
acreditaria que os próprios aldeões fizeram aquilo. Algo que surge do nada,
move item sem ninguém carregar ou sai de graça quebra essa impressão. Uma
marca que só o mod usa também quebra. Custo, lentidão e imperfeição
**ajudam**. Nenhum item abaixo foi implementado: esta lista é para o autor
escolher o que entra.

Em cada item aparecem a evidência (`arquivo:linha`), o motivo pelo qual
parece artificial e a proposta mais próxima do vanilla.

---

## 🔴 Quebram a ilusão de vila viva

### N1 — A população não morre: a fundação repõe titulares a cada detecção

- **Evidência:** `VillageDetectionHandler.java:1287` chama
  `VillageFoundation.ensure` em **toda** adoção, e não só quando
  `created`. Por isso, a cada ciclo de 600 ticks, `VillageFoundation.java:142`
  cria um aldeão adulto do nada (`EntityType.VILLAGER.create`,
  `setBreedingAge(0)`) sempre que falta o titular de uma função fundacional.
  A ADR-018 §6 diz isso explicitamente: *"mortes ou dados ausentes podem
  ser repostos"*.
- **Por que é ruim:** um zumbi mata o mineiro e, 30 s depois, outro adulto
  aparece dentro da casa. No vanilla, a vila só cresce por procriação: é
  preciso ter cama livre e comida. Morte sem consequência também anula o
  golem, a defesa e o cerco de zumbis.
- **Proposta:** a fundação só roda na criação (`created`). Depois disso, a
  reposição vem da **procriação vanilla**. O fazendeiro já produz pão,
  cenoura e batata; basta deixar comida na mão dos aldeões (ou jogar perto
  deles, como o fazendeiro vanilla faz) e deixar camas livres. O mod só
  atribui profissão ao adulto que cresceu. A vaga vazia fica vazia até
  alguém nascer, e isso *é* a história da vila.
- **Custo e risco:** exige reescrever a ADR-018 §6. Colônia com todos os
  titulares mortos e sem comida para de trabalhar, e esse é o resultado
  natural.

### N2 — A BigHouseMOD aparece pronta na adoção

- **Evidência:** ADR-018 §3–5; `BigHouseFoundation.ensure` roda em
  `VillageDetectionHandler.java:1274`, também em toda adoção. É uma casa de
  seis camas e seis baús, **sem móveis nem decoração**, colocada de uma vez.
- **Por que é ruim:** é a estrutura mais visível do mod, surge inteira sem
  ninguém construir e parece um alojamento de fábrica, não uma casa de vila.
- **Proposta, da mais natural para a menos natural:**
  1. **Nada é colocado.** Os titulares usam as camas vanilla que a vila já
     tem (`VanillaBedChests` já cria baús ao lado dessas camas). Se faltar
     cama, a primeira obra do construtor é uma casa (o E48 já força casa
     quando falta cama).
  2. A BigHouseMOD vira a **primeira obra** do construtor: canteiro,
     material e bloco a bloco, como as outras.
  3. Se continuar instantânea, que tenha os móveis e a decoração do bioma,
     e só na criação.

### N3 — Peça sem rota no bioma é criada no baú (ADR-022)

- **Evidência:** `BiomeConstructionSupply.java` (javadoc da classe): *"a
  peça final entra no baú mais próximo da obra"*.
- **Por que é ruim:** o item nasce do nada. Tudo o que o mod construiu para
  a economia de produção ser honesta perde valor numa única peça.
- **Proposta:** escolher entre (a) **substituir** pela peça equivalente que
  o bioma produz, com a mesma lógica da madeira de qualquer espécie
  (porta de acácia no lugar de porta de selva, lampião no lugar de lanterna
  de alma); (b) **pular** a peça e deixar a vaga, que a ADR-023 já
  permite; (c) usar o **mercador ambulante**, que é vanilla: a obra espera
  até ele passar. Recomendação: (a), com (b) quando não houver
  equivalente.

### N4 — O que é produzido vai direto para o baú, à distância

- **Evidência:** `ChestDepositor.java` (javadoc): *"A madeira derrubada vai
  direto para cá, sem passar por item no chão"*. Treze chamadores, entre
  eles `TreeFelling.java:99`, `MinerHaul.java:110`, `FarmerWork`,
  `ShepherdWork`, `SmelterWork`, `SurfaceGatheringWork` e `StrandedEscape`.
- **Por que é ruim:** o lenhador derruba a árvore a 60 blocos e a tora
  aparece no baú na mesma hora. Ninguém vê alguém carregando nada, e a vida
  da vila (as idas e vindas) desaparece.
- **Proposta:** usar o **inventário do aldeão**. O vanilla dá 8 slots e o
  fazendeiro vanilla já carrega trigo. O trabalhador coleta no inventário
  e, quando enche ou o expediente acaba, caminha até o baú e deposita.
  Assim a regra do autor de 08-08 continua valendo (nada fica no chão para
  despawnar) e a viagem passa a ser visível. O depósito à distância fica
  só como fallback, quando o baú está fora de chunk carregado.
- **Custo e risco:** alto. É uma mudança de fluxo nas sete profissões, e a
  produção cai. Sugiro fazer primeiro com o lenhador e medir.

---

## 🟠 Artificiais, mas baratos de corrigir

### N5 — Infraestrutura sem custo de material

- **Evidência:** `MineLighting.java:31` (*"Não custa material"*, a tocha
  sai de graça, `:244`); `MineMouth.java:23` (arco de pedregulho `:320`,
  lanterna `:294` e baús `:339/:372`, todos grátis); `RoadExtension.java:46`
  (*"Calçar não custa material"*).
- **Por que é ruim:** a tocha grátis é o de menos. O problema é a rua e a
  boca da mina, que surgem sem que nenhum bloco saia de estoque.
- **Proposta:** a tocha custa carvão e graveto quando a colônia tem os dois
  e sai grátis só enquanto não houver carvão (é o impasse de ovo e galinha
  descrito na própria classe). O arco usa o pedregulho que o mineiro acabou
  de cavar. A rua vira **caminho de terra** (`dirt_path`), que é o que as
  vilas vanilla usam e se faz com pá em grama, sem material.

### N6 — Nome flutuante sobre cada trabalhador

- **Evidência:** `WorkerNameplate.java:108-109` (`setCustomName` e
  `setCustomNameVisible(true)` permanente).
- **Por que é ruim:** nenhum aldeão vanilla tem nome sempre visível, e a
  vila vira um painel de debug. O nome também sobrescreve um nome que o
  jogador tenha dado.
- **Proposta:** opção de configuração ou gamerule `villagecolony:showLabels`,
  **desligada por padrão**. O quadro no baú (`ChestMarker`) já diz a
  profissão de forma diegética. Outra saída: nome visível só a curta
  distância (`setCustomNameVisible(false)` mostra o nome só quando o
  jogador mira).

### N7 — Placa flutuante e partículas no lote

- **Evidência:** `SiteMarker.java:312-330` (suporte de armadura invisível
  com o nome da obra) e partículas de chama e fumaça no contorno.
- **Por que é ruim:** é a mesma coisa do N6, ferramenta de diagnóstico
  deixada no mundo. Foi pedida para depurar, mas fica ligada para sempre.
- **Proposta:** usar a mesma chave do N6. Ligada, serve para quem depura;
  desligada, o lote se reconhece como no vanilla, pelo canteiro aberto e
  pelo material empilhado.

### N8 — Viveiro de terra enraizada

- **Evidência:** `TreeNursery.java:51` (`Blocks.ROOTED_DIRT`). O javadoc
  diz que é de propósito, para *"se distinguir à vista"*.
- **Por que é ruim:** a marca de que foi o mod fica permanente e visível
  num terreno que devia parecer bosque.
- **Proposta:** plantar o rebento em grama ou terra que já existem.
  No deserto, pôr **terra comum** (não enraizada) e espaçar as mudas de forma
  irregular, não em grade. Para evitar replantio no mesmo ponto, guardar a
  posição plantada no save em vez de marcá-la no bloco.

---

## 🟡 Decisões que merecem revisão

### N9 — Rodízio casa/infraestrutura

- **Evidência:** `HousePlans.nextConstructionIsHouse`. O E48 já adicionou
  "casa quando falta cama".
- **Proposta:** trocar a alternância pela **necessidade**. Casa quando
  adultos ≥ camas; oficina quando há profissão sem posto; decoração
  (poço, lampião) quando as duas anteriores estão satisfeitas. É o
  critério que um prefeito usaria, e o E48 já cobre a primeira metade.

### N10 — Fuga do encalhado cava escada no terreno (E47)

- **Evidência:** `StrandedEscape.java`. Cava até 32 degraus, só em terreno
  natural.
- **Avaliação:** é o que um jogador faria, então passa na régua. O ponto
  fraco é a escada que fica como cicatriz. Proposta de baixo custo: depois
  que ele sai, **tampar a escada com a terra ou pedra cavada**, para que
  outro aldeão não caia no mesmo buraco. Como os drops já estão no baú, dá
  para fazer.

### N11 — Alcance de coleta maior que a vila

- **Evidência:** `LumberjackWork.java:78` (64), `SurfaceGatheringWork.java:49`
  e `SandGathering.java:36` (48). A proteção de solo vai a 96
  (`FarthestVillageSector.java:22`).
- **Por que é ruim:** o desmatamento e as crateras de areia aparecem longe
  da vila, onde o jogador não espera.
- **Proposta:** o alcance cresce com a vila, por exemplo 24 + 4 × casas,
  até chegar a 64. A vila jovem só mexe no próprio entorno.

---

## O que **já está natural**, e não deve ser mexido

- Nitwit e bebê ficam fora do trabalho (`VillagerScanner.java:276`,
  `WorkHours.java:47`).
- O expediente é diurno (`WorkClock.java:45`: trabalha até o
  anoitecer). À noite ninguém trabalha.
- A ferramenta não muda a velocidade (`WorkerEquipment`, Regra 2): o
  trabalho leva o tempo de um jogador com ferro.
- `BlockProtection.mayBreak`: nada do jogador é tocado.
- O mineiro veda água como um jogador faria (`MineFlooding`).
- Não há mixin de renderização nem cliente obrigatório: roda em servidor
  vanilla.
- A profissão e as trocas vanilla do aldeão não são alteradas.

---

## Ordem sugerida

1. **N6 + N7** (chave de marcadores, desligada por padrão). Custo pequeno e
   ganho visual imediato.
2. **N1** (fim da reposição, procriação vanilla). É a maior mudança de
   sentido do mod e precisa emendar a ADR-018.
3. **N3** (substituir em vez de criar). A ADR-022 é substituída.
4. **N2** (BigHouseMOD como obra ou fora da fundação). Depende de N1.
5. **N5, N8, N10**, que são trocas pontuais de bloco ou custo.
6. **N4** (carregar no inventário). Mudança grande; primeiro com o
   lenhador e medição da produção.
7. **N9, N11**, que são ajustes de política.
