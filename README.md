# Village Colony

Para alternar entre Claude Code e Codex, consulte [HANDOFF.md](HANDOFF.md) e
[AGENT-SWITCH-SETUP.md](AGENT-SWITCH-SETUP.md). O hook local de pré-commit
sincroniza as referências `@AGENTS.md`; cada worktree precisa instalá-lo.

Mod Fabric para Minecraft 1.21.1 que transforma vilas Vanilla em colônias
autônomas. Os aldeões recebem profissões, usam baús reais, produzem recursos,
plantam, mineram e constroem estruturas do próprio jogo sem exigir menu ou
mod no cliente.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.4.74%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)
[![CI](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml)

Download: na [página de versões](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases)
do GitHub — o JAR saiu do repositório (ADR-036, item 26). Cada versão traz o
SHA-256 do JAR na descrição.

**Estado atual: 0.4.74 na branch `claude/jogo-0468-grafo-06c8d9`, não jogada; a última publicada é `v0.4.68-alpha`.** Para continuar o trabalho, comece por [`HANDOFF.md`](HANDOFF.md). O estado vivo, a contagem de testes e o que espera
playtest ficam só no [STATE.md](STATE.md); o que está aberto, no [TODO.md](TODO.md); as regras, em
[docs/RULES.md](docs/RULES.md) (1 a 269).

Da 0.4.69 à 0.4.74 (jogo da 0.4.68, planície, análise em `docs/audit/2026-11-13-jogo-0468.md`):

- **Grafo de ruas:** nenhuma linha sobre a água, nem ponte (Regra 264, revoga a 174); o trecho planejado que uma construção
  cobriu sai da linha (Regra 265).
- **Ofícios:** o fundidor funde onde está (Regra 263); uma casa do mineiro basta (Regra 266); a plantação grande sai em todo
  bioma (Regra 267); o bosque do lenhador acha o chão em qualquer altura (Regra 268); o prazo do pastor cresce com a
  distância, e o animal largado fica amarrado (Regra 269).

Da 0.4.54 à 0.4.68, em resumo:

- **Terreno da vila:** a rua do jogo nasce no chão novo do solo suave (Regra 250); flor e muda não seguram coluna de pé (Regra 251).
- **Mina:** patamar de 3 x 3 com tocha de parede nas dobras da escada dos níveis novos (Regra 254, ADR-053); arco de pinheiro
  descascado na boca, reconhecido pelas peças (Regra 256); casa do mineiro, mina e arco prontos na fundação (Regra 260).
- **Fundação:** a vila nova nasce com um aldeão de cada um dos oito ofícios (Regra 259).
- **Luz:** poste de pedra, cerca e tocha da colônia ao longo das ruas fixas, obra do construtor na vaga ociosa (Regra 261).
- **Biomas:** as estruturas do mod saem na madeira e na pedra do bioma da vila (Regra 262).
- **Ofícios:** baú de qualquer lugar (Regra 243); construtor e pedreiro nivelam o chão (Regras 246 e 247); fundição sem cru
  chama o pedreiro (Regra 249); areia e argila sem reserva de cru (Regra 257); o adiantamento lê todas as estruturas da vila
  (Regra 258); o pastor não amarra com curral na vila (Regra 255); o lenhador só corta árvore com folha (Regra 256);
  o curral leva terra no solo (Regra 253).
- **Verificação:** `python scripts/test_lab.py <perfil>` alimenta o Sentinela; `scripts/rule_coverage.py` lista as regras sem
  teste. Teste verde não substitui jogo: toda correção é candidata até o playtest seguinte.

Da 0.4.29 à 0.4.53, em resumo:

- **Grafo de ruas:** no máximo 4 pontas azuis, uma por quadrante (Regra 206); toda porta térrea é entrada, com o marcador colado nela
  (Regra 207); a plantação não tem anel e fica com uma entrada (Regra 208); porta de quintal fechado só conta se for a única
  da casa (Regra 209). O chafariz mantém o anel de caminho (Regra 183). Toda estrutura onde se entra, e o portão da mina,
  têm entrada de porta no grafo, sem porta no mundo (Regra 210); poste, poço e chafariz não têm, e o ramal deles sai do chão (Regra 223).
- **Mineiro e velas:** a casa do mineiro acompanha a boca em uso (Regra 211); nenhum aldeão põe tocha sozinho, e vela ou tocha
  soltas não recusam lote (Regra 212).
- **Obra MAE:** a obra pedida é a próxima do construtor e sempre termina (Regra 213).
- **Caminho:** todo caminho e toda rede pelo grafo, em toda vila; toda entrada aparece na hora (Regras 214 e 216); o pedreiro ocioso calça e nivela (Regra 215).
- **Pastor e lenhador:** o pastor leva os animais amarrados aos pastos e só prende em poste sem curral livre (Regra 196);
  todo aldeão corta folha que trava o caminho (Regra 197); o lenhador prefere o interior da vila e não replanta a menos de
  3 blocos de estrutura (Regras 198 e 199).
- **MAE:** a obra pedida limpa o lote (Regra 200) e entra na fila do construtor (Regra 213). A expansão automática continua desligada.
- **Verificação:** `python scripts/test_lab.py <perfil>` alimenta o Sentinela; `scripts/rule_coverage.py` lista as regras sem
  teste (hoje só a 33 e a 47). Teste verde não substitui jogo: toda correção é candidata até o playtest seguinte.

Planejamento do ciclo de correção por playtest: [terreno, mina e profissões](docs/technical/Plano-Playtest-2026-11-02.md).
Histórico das versões anteriores (0.4.8 a 0.4.16, Fases 1 a 3 do MAE): [CHANGELOG.md](CHANGELOG.md),
[MAE-Fase-1](docs/technical/MAE-Fase-1.md), [MAE-Fase-2](docs/technical/MAE-Fase-2.md),
[MAE-Fase-3](docs/technical/MAE-Fase-3.md).

Rede hierárquica e currais têm [desenho aprovado](docs/superpowers/specs/2026-11-01-hierarchical-roads-and-pens-design.md).
O grafo é ativado somente em vilas novas depois de validar o índice legado; vilas
existentes preservam o índice anterior. O curral grande único segue a mesma regra de
geração nova. Evidências e limites ficam no [STATE](STATE.md) e na
[ADR-043](docs/decisions/ADR-043-hierarchical-road-graph.md).

## O que o mod faz

- Adota vilas Vanilla e mantém o estado no mundo, nos aldeões, baús e blocos.
- Atribui profissões e garante camas e baús distintos para a fundação.
- Usa receitas e blocos Vanilla em vez de inventário ou economia virtual.
- Busca madeira, pedra, lã, comida, areia, terra, relva e minérios quando há
  demanda real.
- Planta e mantém até dez árvores da madeira do bioma, fora do centro da vila,
  usando muda e terra enraizada.
- Cria uma `BigHouseMOD` automaticamente para a fundação de cada vila.
- Escolhe estruturas Vanilla permitidas pelo bioma e constrói uma por vez.
- Revarre construções incompletas ciclicamente antes de abrir uma obra nova.
- Cancela uma obra profissional quando o jogador coloca uma Tocha das Almas
  dentro dela, liberando a fila; a `BigHouseMOD` é protegida dessa regra.
- Evita sobreposição com estruturas existentes, projetos pendentes e blocos
  físicos dentro da área vertical protegida.
- Trabalhadores recolhem itens soltos a até 5 blocos; a tarefa auxiliar do aldeão
  ocioso também busca o item que falta à obra aberta (Regra 48).
  Mostra no `/vc log` quem espera o quê há mais de dois minutos e,
  por ofício, a última janela de cinco minutos do expediente (trabalho,
  caminhada, espera, bloqueio, ócio e encalhe), marcando quem passa de 15% na
  regra do ócio. O comando é aberto a todo jogador.
- Regras de profissão por mundo, editáveis pelo [Mod Menu](https://modrinth.com/mod/modmenu)
  (opcional): ativar ou desativar a profissão, limite de trabalhadores, raio de
  busca do lenhador, fazendeiro e pastor, e ordem de contratação (ADR-030).
- Com o mod no cliente, mostra a profissão sobre o aldeão, na cor do que ele
  faz agora — laranja no ofício, azul ajudando, vermelho travado há 30 s,
  branco à toa, cinza fora do expediente (Regra 129) — e o progresso da obra,
  com o primeiro material que falta. Sem o mod no cliente, a profissão
  não aparece: desde 2026-10-10 (P0.8) o servidor não escreve mais o nome da
  profissão no aldeão.
- Liga a porta de cada construção pronta à rede de caminhos da vila: caminho
  de até 3 blocos de largura, **rente ao chão** — troca só o bloco de cima,
  cobre só o buraco de 1 bloco, corta no máximo 1 e nunca põe bloco acima do
  terreno; sobe no máximo 1 bloco por passo, estreita onde o lado não cabe,
  contorna o desnível maior, nunca passa por construção nem pela boca da mina e
  se junta aos caminhos que já existem; vai ao chafariz por atalho quando a
  rede dá mais de 2 vezes a volta (Regras 123 a 125). Quem assenta é somente o
  construtor sem obra, um por vez, conforme a regra temporária de ofícios.
- No deserto usa o bloco próprio "caminho de areia" (Regra 73).
- A mina desce em caracol de 4 lances curtos, abre um salão de 17×17 e desce de
  novo pelos 4 cantos; a escada de descida é o caminho de volta, com no máximo
  1 bloco por degrau, e nunca é fechada; a luz é de tocha no chão dos salões
  (Regra 119). Sobre a boca fica o portão de entrada desenhado pelo autor —
  pilares de pedra e tijolo, telhado de carvalho escuro e lanternas —, com as
  três pistas da escada no centro do vão (Regra 155).
- O pastor precisa estar a até 5 blocos para alimentar, cruzar e abater;
  preserva 6 adultos por espécie. Em vilas novas, há no máximo um curral grande
  de até 20 animais, com água, cultivos, grama, portão e tochas. Currais pequenos
  e cercados existentes válidos continuam utilizáveis (Regras 68, 72 e 130).
  Falta confirmar o conjunto em jogo.
- Antes de cada obra nova, nivela o chão em volta na altura da rua: um anel de
  2 blocos com no máximo 1 de diferença e uma rampa de 3 até o terreno natural
  (Regra 131).
- O solo da vila nasce suave na geração do mundo: na vila, buraco tampado,
  calombo pequeno cortado e degrau de no máximo 1, com casas e água no lugar;
  em volta, o chão mistura o nível da vila com o terreno natural numa faixa de
  4 vezes o desnível, de 16 a 64 blocos, sem escada nem paredão, e a árvore
  sobre chão cortado sai inteira (Regra 152; só em mundo novo).
- O mineiro não quebra o bloco que segura, acima e pelo vão, cascalho, areia,
  água ou lava (Regra 157); ninguém dorme no andar de baixo da casa de escada
  de mão (Regra 158).
- O construtor sem tarefa alisa o chão da vila aos poucos, uma janela de 24×24
  por vez: tapa buraco, tira calombo de qualquer tamanho e deixa degrau de 1,
  pondo e tirando solo, sem tocar em construção, bloco do jogador, roça, água
  nem na boca e na escada da mina (Regra 156, restrita pela regra temporária
  de ofícios de 02-11). O construtor com a obra parada
  limpa e nivela o lote dela e 6 blocos em volta.
- Nenhuma profissão espera mais de 5 minutos: quem fica parado com tarefa a
  larga e vai ajudar a vila; se a mesma tarefa trava de novo em 10 minutos, o
  produto surge no baú (Regra 128).
- Quem está sem tarefa ajuda: o pastor colhe a roça, o lenhador cuida do
  bosque (1 em cada 10 mudas é de outra espécie), o carpinteiro põe a tocha da
  colônia nos caminhos escuros, o pedreiro recolhe neve com a pá (Regra 127).
- Cada aldeão dorme na cama mais perto de onde trabalha: uma vez por dia, só
  com ganho de 16 blocos de caminhada, levando o baú junto, como uma transação
  (Regra 74).
- Cama, porta e baú são peças obrigatórias da obra; sem apoio, ganham o chão
  embaixo (Regra 21).
- Diagnóstico: tudo o que surge sem uma profissão obter está numa lista
  declarada e deixa a linha `VC_SUPPLY` no log (Regra 111); toda desistência
  no diário de ações leva um código único de bloqueio (Regra 112).

O mod é alpha. A candidata 0.4.9 prepara a segurança MAE conforme a
[ADR-044](docs/decisions/ADR-044-mae-region-safety.md). A Fase 2 está em validação
local na branch de desenvolvimento, conforme a [ADR-045](docs/decisions/ADR-045-mae-terrain-road-chain.md), sem ativação automática.
A 0.4.8 (2026-11-02) suspende ajudas cruzadas em `VillageHelpers`
e aguarda playtest. A 0.4.7 (2026-11-02) adiciona análise manual e métricas locais do
Sentinela. A 0.4.6 (2026-11-02) adiciona o Observatório local do Sentinela.
A 0.4.5 (2026-11-02) adiciona a janela local do Sentinela.
A 0.4.4 (2026-11-02) adiciona o fechamento rastreável de
entregas. A 0.4.3 consolida trabalho, curral e ruas sob as
regras de geração somente para vilas novas. A versão ainda não foi jogada; use
**mundo novo**. O estado vivo está no
[`STATE.md`](STATE.md). O que mudou em cada
publicação está no [`CHANGELOG.md`](CHANGELOG.md); a avaliação técnica mais
recente, em
[`docs/technical/avaliacao/`](docs/technical/avaliacao/).

## Profissões e funções

Estas são as oito funções operacionais atuais (revistas em 2026-09-30). `BREEDER` foi encerrado;
saves antigos com esse código são carregados como `SHEPHERD`.
`MANUFACTURER` não existe no código atual; documentos que usam esse nome estão
desatualizados.

| Código | Nome | Ferramenta inicial | Responsabilidade | Executor |
|---|---|---|---|---|
| `MINER` | Mineiro | Picareta de ferro | Abre e amplia a mina; sem demanda, extrai na frente planejada até 64 pedras por dia, sem tocar a escadaria | `MinerWork` |
| `LUMBERJACK` | Lenhador | Machado de ferro | Derruba árvores, replanta e mantém o viveiro da borda | `LumberjackWork` |
| `MASON` | Pedreiro, equivalente ao ferreiro do catálogo | Nenhuma | Produz alvenaria e peças de pedra exigidas pelas obras | `CraftingWork` |
| `SMELTER` | Fundidor | Pá de ferro com Silk Touch | Funde materiais e coleta areia, relva, cacto e argila sob demanda; ocioso mantém somente lingotes físicos até 64 por tipo | `SmelterWork` |
| `CARPENTER` | Carpinteiro | Nenhuma | Artesão geral: madeira e toda peça que não é alvenaria (vidraça, tear, cama, lampião, tocha) | `CraftingWork` |
| `FARMER` | Agricultor/Fazendeiro | Enxada de ferro | Mantém lavouras, cava terra para obra e faz pão do trigo acima da reserva | `FarmerWork` |
| `SHEPHERD` | Pastor | Tesoura | Tosquia, mantém reprodução e abate somente acima de 6 adultos; vilas novas recebem um curral grande para até 20 animais | `ShepherdWork` |
| `BUILDER` | Construtor | Nenhuma | Reserva lotes, repara e assenta estruturas | `BuilderWork` |

Cada ofício tem a sua bancada do mod (estação de trabalho, produção em dobro e
passivas na vila); ver [Blocos do mod](#blocos-do-mod).

Compatibilidade: `BREEDER` é convertido para `SHEPHERD` ao ler saves antigos.
O agricultor continua profissão completa, sem cama ou baú fundacional dentro
da `BigHouseMOD`. O carpinteiro é titular da fundação desde 2026-09-30.

Regras transversais de 2026-09-30:

- Cada bloco é quebrado com a ferramenta de ferro certa para ele
  (`ActionTool`); a da mão fica quando é tão boa ou melhor.
- A contratação atende primeiro a profissão de que a obra depende agora
  (`ProfessionDemand`), depois a ordem fixa.
- Aldeão com profissão do mod não recebe profissão Vanilla (ADR-029).
- A regra "a colônia não cria recurso" foi retirada (ADR-028): corante,
  linha, pó de osso e drops de inimigo e animal aparecem no baú quando a
  receita do artesão pede; bloco sem item é montado no local; a **lava nunca**
  é assentada.

## Regras do mod

Todas as regras do autor, numeradas, com a data da decisão, o estado e as classes que as aplicam. A fonte é
o [`docs/RULES.md`](docs/RULES.md): esta seção é gerada por `python scripts/rules_to_readme.py` e não se
edita à mão (`--check` acusa quando ela ficou para trás).

<!-- REGRAS:INICIO (gerado por scripts/rules_to_readme.py a partir de docs/RULES.md; não editar à mão) -->

### Regras ativas

> **Emenda temporária — 02-11, candidata 0.4.8:** somente construtores
> executam obra, pavimentação e terraplanagem. Ficam suspensas as ajudas
> cruzadas descritas nas Regras 59, 149 e 156. `VillageHelpers` não despacha
> pedreiro nem fundidor para essas ajudas. Falta confirmar o efeito em jogo.

| # | Enunciado | Data | Estado | Implementação |
|---|---|---|---|---|
| 1 | Colher até os baús encherem | 08-08 | ✅ feita | `ColonyGoals` (meta = guardado + espaço) |
| 2 | Colher no tempo de um jogador com ferro | 08-08 | ✅ feita | `BlockBreakTime`, `LumberjackWork.tick` |
| 2-e1 | **Emenda 1:** o aldeão trabalha na velocidade da ferramenta que tem — começa com ferro; havendo no baú dele uma melhor (mais rápida ou encantada), troca a de ferro por ela | 10-02 | ✅ feita | `ToolUpgrade` (desde 09-04) + `ActionTool.speedOf` com Eficiência e desempate por encantamento; troca pela pilha exata (`ChestWithdrawer.takeExact`, `ChestDepositor.depositExact`) — `BetterToolGameTest` |
| 3 | Nunca destruir construções da vila original, da colônia ou do jogador | 08-13 | ✅ feita, com limite de autoria manual | `BlockProtection`, `TreeHarvester` |
| 3-e1 | **Emenda 1:** lenhador respeita estruturas protegidas ao cortar árvores | 09-14 | ✅ feita | `TreeHarvester.plan`, `breakOne` |
| 4 | Dois trabalhadores por profissão | 08-13 | ⚠️ substituída na prática | a constante `MAX_PER_PROFESSION` não existe mais; a cota cresce com a população (`ProfessionAssigner.targetCount`, lotes de 15 adultos) e a demanda passa uma cabeça acima dela (`ProfessionDemand`, 09-30) |
| 5 | Fabricar até metade do armazém | 08-13 | ✅ feita | `ColonyGoals` (tábua) |
| 5-e1 | **Emenda 1:** a obra usa todos os recursos de todos os baús — a tora vira a tábua que a obra pede, além da reserva; a reserva de um terço em tora (Regra 248) vale só para os outros ofícios. A meta e o fabricante fazem a mesma conta | 10-02 | ✅ feita | `StockRules.logsThatMayBeConverted`, `WorkDemand.rawLogs` |
| 6 | Estrada primeiro, casa ligada a ela | 08-14 | ✅ feita | `RoadExtension`, `BuildSiteScanner` |
| 7 | O lenhador planta onde cortou | 08-15 | ✅ feita | `LumberjackWork.closePlan` |
| 8 | Um baú ao lado de cada cama — **em toda casa** (da vila e construída), encostado na parede, nunca diante de porta ou escada (ADR-036 item 4) | 08-15, 10-06 | ✅ feita | `ChestPlacer` (porta, escada, parede), `VanillaBedChests.ensure` (vila e casas da colônia) |
| 9 | Subir e descer para alcançar, e poder voltar | 08-15 | ✅ feita | `BuilderApproach`, `TreeMarks` |
| 10 | O construtor fabrica o que a expansão pede | 08-15 | ✅ feita | `CraftingLookup.billFor`, `ColonyChests` |
| 11 | Uma de cada profissão em cada vila | 08-15 | ✅ feita | `ProfessionAssigner`, `ProfessionFloorTest` |
| 12 | O centro fica em bloco que existe | 08-15 | ✅ feita | `VillageDetector.evaluate` |
| 13 | A obra é uma que a colônia consiga fazer | 08-15 | ✅ feita | `ColonyHut` (aposentada pela 27) |
| 14 | O construtor alcança o alto da obra — desde a ADR-037 B1, de qualquer lugar da zona da obra (pegada + 4) | 08-18 | ✅ feita | `BuilderApproach.isWithinReach`, `BuilderApproach.isInsideZone` |
| 15 | A estrada cresce com a vila: a ponta pede as colunas ao grafo, e o aldeão calça (Regra 214) | 08-18 | ✅ feita | `RoadExtension` |
| 16 | Cada casa com espaço em volta | 08-18 | ⚠️ parcial | volume no `BuildSiteScanner` |
| 17 | A casa com uma lateral na estrada | 08-19 | ✅ feita | `Blueprint.doorSide`, `rotated` |
| 18 | O dia inteiro é expediente | 08-19 | ✅ feita | `WorkClock`, `WorkHours` |
| 18-e1 | **Emenda 1:** ninguém para de trabalhar por chuva, e não há pausa ao meio-dia (R-6 e R-7 do estudo de 10-02 recusadas) | 10-02 | ✅ é o que já acontece | `WorkHours` não olha o tempo |
| 19 | O lote fica no nível da estrada | 08-19 | ✅ feita | `LotLevel.flatGroundAt` |
| 20 | Cada vila constrói no estilo do bioma | 08-19 | ✅ feita | `VillagePalette`, `VillageBiomesGameTest` |
| 21 | Toda casa nasce com cama, baú e lampião — **resolvida em 10-22 (C-02, autor):** cama, porta e baú vêm da planta e são **obrigatórios**; sem apoio, ganham o chão do bioma embaixo e são postos; só quando o apoio é impossível (algo protegido embaixo) caem nas saídas de sempre, com `VC_FUNCTIONAL_MISSING`. As demais peças (lampião inclusive) mantêm as saídas atuais, sem obra eterna | 08-19 | ✅ feita (0.3.93) | `FunctionalPieces`, `BuilderPlacement`, `UnsupportedClosing` |
| 22 | O lote é livre no volume | 08-19 | ✅ feita | `BuildSiteScanner.isNothing` |
| 23 | O que já foi analisado se analisa de novo | 08-19 | ✅ feita | `TreeMarks` (prazos), `RingSweep` |
| 24 | A vila de planície levanta a casa do jogo | 08-19 | ✅ feita | `VillageStructures` |
| 25 | ~~A maior planta que couber no lote~~ | 08-20 | ❌ **desfeita** em 10-06 (ADR-036 item 5) | a ordem das plantas não favorece tamanho (`PlanOrdering.mixed`); a primeira casa continua a menor |
| 26 | Cadeia de produção, e paleta por bioma | 08-20 | ✅ feita | `ResourceType.production`, `VillagePalette` |
| 27 | Só o catálogo do jogo, e o construtor aguarda | 08-20 | 🔒 imutável | `VillageStructures`, `MaterialChoice` |
| 27-e1 | **Emenda 1:** abre para pedra só | 08-26 | ✅ feita | `Substitution.ALTERNATIVE` |
| 27-e2 | **Emenda 2:** e para a madeira junto | 08-26 | ✅ feita | `MaterialChoice.INTERCHANGEABLE_IN_THE_WALL` |
| 27-e3 | **Emenda 3:** os modelos próprios da colônia (`data/villagecolony/structure/colony/`, `.nbt`) têm prioridade sobre a estrutura do jogo quando existem — casa de cada ofício e troca de estrutura; sem modelo, vale o catálogo do jogo | 10-02 | ✅ feita | `ColonyModels`, `CATALOGO.md` (`scripts/structure_catalog.py`) |
| 28 | ~~Barreira de teste: casa pequena, mobília dispensada~~ | 08-20 | ❌ **retirada** em 10-06 (ADR-036 item 6) | peça que falta não é mais riscada: aparece na 4ª tentativa (Regra 51; na obra, 1 minuto (2 ciclos) sem avanço desde 11-10); só a 5ª falha ao pôr pula a peça |
| 29 | A mina em escada, duas salas, galeria sem fim | 08-20 | ✅ feita | `MineShaft` |
| 30 | O mineiro recolhe tudo, e a boca tem endereço | 08-22 | ✅ feita | `MineMouth`, `MinerHaul` |
| 30-e1 | **Emenda 1:** manter piso de carvão e ferro bruto mesmo sem obra ativa | 09-14 | ✅ feita | `ColonyGoals.MINERAL_FLOOR` |
| 31 | O fazendeiro planta o que tem e colhe o que está pronto | 08-26 | ✅ feita | `FarmerWork`, `CropPatch` |
| 32 | ~~Móveis e cama entram depois da casa pronta~~ — **substituída em 10-22 pela 21 (C-02, autor):** não há segunda passada de mobília; cama, porta e baú vêm da planta, como peças obrigatórias | 08-29 | ❌ substituída | — |
| 33 | Pedido de commit+push também atualiza o JAR local | 09-14 | ✅ registrada | `build/libs/` → `downloads/` → `%APPDATA%/.minecraft/mods/`; conferir SHA-256 |
| 34 | A lava nunca surge automática | 09-30 | ✅ feita | `BlockShaping.isNeverPlaced`, `BuilderPlacement.placeOne` |
| 35 | Carpinteiro titular na BigHouseMOD, cama e baú à direita da porta, corredor livre até a escada | 09-30 | ✅ feita | `FOUNDATION_ORDER`, `big_house_mod.nbt` |
| 36 | O viveiro (terra enraizada + rebento) é do lenhador | 09-30 | ✅ feita | `LumberjackNursery` |
| 37 | Cada aldeão usa a ferramenta de ferro apropriada para a ação | 09-30 | ✅ feita | `ActionTool`, `BlockBreakTime` |
| 37-e1 | **Emenda 1:** a de ferro é a primeira, não a única — ver 2-e1 | 10-02 | ✅ feita | `ActionTool`, `ToolUpgrade` |
| 38 | Contratar primeiro a profissão de que a demanda depende, depois a lista | 09-30 | ✅ feita | `ProfessionDemand`, `ProfessionAssigner.demandedVacancy` |
| 39 | Comida é feita pelo fazendeiro (pão do trigo acima de 32) | 09-30 | ✅ feita | `FarmerBakery` |
| 40 | Piso de fundido só com cadeia do cru | 09-30 | ✅ feita | `StockRules.rawOf`, `ColonyGoals` |
| 41 | Corante, linha, pó de osso e drops de inimigo e animal aparecem automáticos | 09-30 | ✅ feita | `DropIngredients`, `ColonySupply` (ADR-028) |
| 42 | Mineiro para de guardar um tipo aos 256 no baú (fora o pedido) | 09-30 | ✅ feita | `MinerHaul.TYPE_CAP` |
| 43 | O pastor faz o rebanho procriar | 09-30 | ✅ feita | `ShepherdFlock` |
| 44 | Ofício do mod exclui ofício Vanilla | 09-30 | ✅ feita | `VanillaProfessionGuard`, `VillagerDataMixin` (ADR-029) |
| 46 | A fome não influencia o trabalho: a comida decide só a procriação (Vanilla) | 10-02 | ✅ é o que já acontece | `VillageMeals` |
| 47 | O aldeão não envelhece nem morre de velhice | 10-02 | ✅ é o que já acontece | — |
| 48 | Todo aldeão da colônia recolhe qualquer item caído a até 5 blocos ao passar e o guarda em baú compartilhado. Quem está sem tarefa também pode buscar ativamente peça que falta à obra; baús nomeados pelo jogador ficam reservados | 10-02; emenda 10-31 | ✅ feita | `GroundPickup` |
| 49 | **A ordem das obras (consolida 55 e 60, 1A de 13-10):** faltando cama, casa; senão a oficina de cada ofício que ainda não tem (uma de cada tipo); senão o rodízio casa ↔ outro tipo, que levanta casa enquanto as camas forem menos que adultos + 4 (`HOUSE_SURPLUS`). O tipo sem lote numa volta inteira da varredura sai da vez por 30 min, e com todos recusados a casa entra no lugar | 10-02 (1A 10-13) | ✅ feita | `ConstructionPriority.buildsHouse/roomForAHouse`, `ConstructionTurn`, `ConstructionOrder`, `LotTypeRefusals`, `HousePlans.plansForNext` |
| 50 | Toda verificação mede o tempo dos aldeões — trabalhando, andando, bloqueado, ocioso, encalhado — por profissão, e usa a proporção como critério de melhoria e de correção | 10-02 | ✅ feita | `WorkTime` (linha `VC_TIME`), `scripts/time_ledger.py`, `CLAUDE.md` §0.4 |
| 45 | Os baús da colônia são todos os da vila, e só eles: com a vila medida, todo baú livre dentro da caixa (na janela de altura das camas) conta, de dentro ou de fora de casa; baú livre fora da caixa não é da vila; o baú de trabalhador e o da boca da mina valem de qualquer lugar (Regra 243); baú de trabalhador de qualquer colônia nunca é livre | 10-01 | ✅ feita | `ColonyChests.nearestFirst`, `VillageChests` (`VillageChestReachGameTest`) |
| 51 | Peça que falta aparece na **4ª** tentativa sem sucesso, no baú da profissão que tentou (coletor; artesão recebe o ingrediente sem rota; construtor se nenhuma profissão faz), com `VC_SUPPLY_ERROR` no log. A peça só é pulada se o construtor falhar **5 vezes ao pô-la** | 10-06 | ✅ feita (ADR-036 item 6) | `LocateFallback`, `BiomeConstructionSupply.ATTEMPTS_BEFORE_STOCKING`, `ConstructionProject.failsForTheLastTime` |
| 54 | **Mina em rede** (ADR-042): passada a escadaria e o salão, cada nível é uma grade de nós a cada 4 blocos (corredor 1 × 2, raio 6), um mineiro por nó; o alvo é sempre a frente do próprio túnel, alcançável andando pelo que já está aberto, e a célula que ele não alcança é pulada (o nó só falha com toda a pedra que falta recusada — mina M, 0.3.48); o minério ao alcance do braço a partir de um túnel aberto vem antes do túnel; piso, fluido encostado e beirada de queda garantidos com enchimento que **surge** (a mina não gasta recurso da vila — 0.3.51, como o `MineFlooding` já fazia para o fluido; antes vinha dos baús e travava quando faltava, playtest 0.3.50); caminho só pelo que está pronto; nível esgotado desce; no fundo, caminhos laterais e volta pela rampa. O retorno subindo nunca quebra as 5 camadas sob a vila | 10-12 | ✅ feita (0.3.40) | `MineNetwork`, `NetworkDigging`, `NetworkSafety`, `NetworkVeins`, `MineReturn` |
| 54 | Emenda (10-12, 0.3.36) | Túnel de ramal 1 × 2 a cada 4 blocos (raio 6); minério ao alcance do braço vem antes do túnel; segurança só de piso, fluido e beirada; retorno nunca quebra as 5 camadas sob a vila (`MineReturn.SHELL_DEPTH`) |
| 55 | Roça grande primeiro; fundidor ocioso adianta o cru; lenhador ocioso planta mudas até 10 fora da caixa; pastor alimenta com qualquer comida; grama e flores surgem sozinhas (a ordem das obras saiu para a Regra 49, consolidada em 13-10) | 10-12 | ✅ feita (0.3.36) | `FarmPlans`, `SmelterAdvance`, `OutskirtsPlanting`, `ShepherdFlock`, `BlockShaping.growsByItself` |
| 56 | **Expansão do terreno:** a caixa cresce **só com rua e obra**, nunca com faixa vazia anexada (0.3.44). A vila cheia (uma volta da varredura sem lote) escolhe o melhor lado alcançável por rua — mais chão plano e seco, outra colônia veta — e puxa a rua para fora por ele, no máximo uma vez a cada 30 min; as construções da borda crescem a caixa. Casa reivindica 16 em volta, oficina e roça 24. Sem teto de tamanho (Emenda 7). **Emenda A5 (autor, 2026-10-22):** se a rua não entra na faixa (duna na entrada), o lote nasce **dentro da faixa, o mais fundo que couber**, ligado à rua por um caminho a pé que segue o terreno (degrau ≤1, sem cortar nem aterrar, até 32 blocos), calçado na abertura da obra | 10-12 (10-13, 10-22) | ✅ feita (0.3.44; A5 0.3.87) | `VillageExpansion`, `VillageExpansionSurvey`, `StripRoad`, `StripLot`, `VillageGrowth.byPiece/byRoad` |
| 57 | O pastor põe no máximo um poste por casa e um por cercado (nenhum poste novo a até 6 blocos de um poste solto) | 10-12 | ✅ feita (0.3.38) | `ShepherdPens.postBesideHome`, `hasLonePostNear` |
| 58 | Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto por tipo: o excedente vai para os baús públicos da vila (sem ofício, fora a boca da mina) em vez de cair no chão ou se perder. Árvore nunca fica pela metade | 10-12 | ✅ feita (0.3.38) | `PublicChests`, `TreeFelling.finishTheTrunk`, `MinerHaul.deposit` |
| 55 | Emenda (10-12, 0.3.38) | Grama, relva alta e flores da planta surgem sozinhas na obra, sem o construtor ir até elas (`GrowingPieces`) |
| 56 | Emenda (10-12, 0.3.39) | Uma em cada 5 obras novas procura lote só na faixa de 16 blocos da borda da caixa (dentro ou fora); sem lote, a vez cai e não conta para anexar (`VillageExpansion.buildOpened/allowsLotAt`) |
| 58 | Emenda (10-12, 0.3.39) | Sem baú público, a vila põe um numa casa da colônia, encostado na parede e a mais de 2 blocos de porta (`PublicChestPlacement`); baú público é só o de dentro da caixa |
| 59 | Ajudam a obra só carpinteiro e pedreiro (fazendeiro e pastor não); o carpinteiro guarda um pouco de toda peça de madeira e, sem tarefa, calça a rua como o construtor | 10-12 | ✅ feita (0.3.39) | `BuildHelper.HELPS`, `ItemStock.isWoodPiece`, `BuilderPaving.PAVERS` |
| 55 | Emenda (10-12, 0.3.42) | O lenhador ocioso planta uma muda fora da vila por vila a cada 5 minutos (`OutskirtsPlanting.EVERY`) |
| 60 | O tipo de obra que passa uma volta inteira da varredura sem lote fica 30 min fora da vez; com todas as oficinas que faltam recusadas, a vila constrói casa no lugar | 10-12 | ✅ feita (0.3.42) | `LotTypeRefusals`, `HousePlans.plansForNext`, `RoadGrowthPlanning.extendTheRoad` |
| 56 | Emenda (10-13) | Consolidada no texto principal da Regra 56 (caixa do modelo 2 é medida de novo, `VillageBoundsSave.CURRENT_MODEL` = 3; anexo no máximo a cada 30 min, `VillageExpansionSurvey.ANNEX_EVERY`) |
| 56 | Emenda (10-14, 0.3.58) | A vila com uma faixa anexada que não rendeu lote anexa de novo já na volta seguinte sem lote (`FULL_AGAIN_AFTER = FULL_AFTER`, era 3×): crescia devagar demais e ficava ociosa entre anexos (playtest `iALIIoOwEK`: produtores 70–98% ociosos com estoque cheio). O teto de 1 anexo a cada 30 min (`ANNEX_EVERY`) continua — é a proteção contra a deriva do centro que soltou 27 aldeões na 0.3.42 |
| 62 | A vila trabalha com o jogador dentro da caixa ou a até 48 blocos dela (`VillageFocus.PERIMETER`, emenda a ADR-003 Emenda 6, que valia só dentro da caixa); lote pode ficar encostado na rua ou 3 blocos atrás, com caminho calçado até ela (`RoadsideSites.SETBACKS`) | 10-13 | ✅ feita (0.3.45) | `VillageFocus`, `RoadsideSites.siteAt`, `SiteOpening.pavePathTo` |
| 61 | A colônia só é dada como abandonada sem vila à vista do centro **nem** dentro da caixa, e com menos de 3 trabalhadores vivos dentro da caixa | 10-12 | ✅ feita (0.3.43) | `ColonyAbandonment.sees`, `VillageAdoption.peopleStillHome` |
| 63 | **Socorro contra parada com prazo único:** o ofício que anda sem chegar ao alvo há 1 minuto de expediente é socorrido — a ação é a de cada ofício, o prazo é um só (era seis cópias de 2 min) | 10-13 | ✅ feita (0.3.47) | `WorkRescue.LIMIT` |
| 64 | As ruas da vila são medidas como **rede de caminhos conectados** a partir do centro (de bloco em bloco, degrau de 1, até 32 além da caixa); a busca de lote anda por ela em vez de varrer o raio em anéis | 10-13 | ✅ feita (0.3.46) | `RoadNetwork` |
| 65 | **Bancadas exclusivas do mod**: três blocos próprios (`carpenter_bench`, `mason_cutter`, `blacksmith_forge`), réplicas das estações do carpinteiro/pedreiro/ferreiro com luz 10. Só o **player** as fabrica (receita = estação Vanilla + 4× ouro, sempre disponível); a colônia nunca as faz nem coloca. Vínculo por POI próprio do mod (sem profissão Vanilla); o titular que trabalha com a bancada da sua profissão perto **dobra a produção por ciclo**. Aparecem na aba do criativo "Village Colony — Bancadas" (0.3.53) | 10-13 | ✅ feita (0.3.52) | `ModContent`, `ModBenchBonus`, `CraftStation.modBench`, `CraftingSteps`, `SmelterWork` |
| 66 | **Cercado no nível da rua**: estrutura cuja camada y=0 é só solo natural (grama, terra, areia) tem essa camada tratada como a camada da rua — o chão de pisar fica no nível da rua e o que sobe dela (a cerca do `animal_pen`) acima, em vez de o recinto afundar um bloco (bug 0.3.52) | 10-13 | ✅ feita (0.3.53) | `StreetLayer.soilBase` |
| 67 | **Plantação por população**: uma roça a cada 15 adultos, no mínimo uma desde a fundação; ao passar de 15 adultos (o 16º), a obra de fazenda vira prioridade e fura a fila antes de oficinas e do rodízio (só a falta de cama vem antes). Substitui a Regra 52 (uma por fazendeiro) | 10-13 | ✅ feita (0.3.54) | `ConstructionPriority.FARM_DEFICIT`, `ColonyFarms.owed` |
| 68 | **O pastor aproveita o rebanho** (manutenção do ofício, não toca a obra): somente um pastor vivo a até 5 blocos abate o excedente, deixando sempre ≥6 adultos por espécie, e deposita o drop Vanilla no baú (couro/carne); a carne crua acima de 16 é cozida sem combustível; o ovo que a galinha larga no chão é recolhido. Reserva viva (6 adultos) e reserva de item (16) são limites distintos | 10-14; emenda 10-31 | ✅ código; falta jogo | `FlockHarvest`, `ShepherdFlock.tend`, `CraftingLookup.smelted` |
| 69 | **A horta da casa surge sem gastar recurso**: os blocos de chão da planta que não têm item nem receita (farmland, água, caminho, cultivo) são moldados no local, com o terreno garantido embaixo, em vez de passar pela cadeia de material (que falhava, "could not make farmland", travando a obra nos últimos blocos — playtest 0.3.56). Só sobre lugar livre; bloco posto não é sobrescrito (Regra 3) | 10-14 | ✅ feita (0.3.57) | `BuilderPlacement.placeOne`, `BlockShaping.isShapedFromTheGround`/`groundFor` |
| 70 | **Forma do caminho:** um bloco de largura, reto a partir da porta, virando só em ângulo reto. Quem pede o caminho é o lote recuado; quem decide e calça é o grafo (Regra 214) | 10-22 | ↪️ substituída pela 123 (10-27) nos caminhos da rede; o corredor do lote segue, com serpentina e talude (0.3.101) | `Corridor`, `WorkPath`, `StripLot`, `BuilderPaving.isGap`, `LotTerraform.cutAbove`, `CorridorCut` |
| 71 | **O mineiro não constrói para fora da montanha:** o desvio dele não põe bloco ao céu aberto (só dentro da montanha) e, se precisar construir para andar, desce antes de ir de lado (cavar descendo custa o de sempre, de lado o dobro, subindo o triplo; pôr bloco o dobro); com a entrada da mina inalcançável a pé, ele cava um acesso só cavando, descendo da vila até o nível; debaixo da terra não entra na água (ADR-025), ao ar livre nada pelo custo do Vanilla — a margem de lago cercada por paredão só se deixa nadando (autor, 10-22) | 10-22 | ✅ feita (0.3.88; água 0.3.89) | `MinerTerrain`, `DetourMoves.Costs.MINER`, `MineAccess`, `MinerCaution` |
| 72 | **Um curral por espécie:** animais iguais ficam no mesmo curral; com mais de um curral na vila, cada tipo vai para o seu — o pastor leva o animal para o curral só daquela espécie, senão um vazio (que passa a ser dela), senão um misto que já a tem, senão qualquer um com lugar; sem animal solto a recolher, tira de um curral misto o animal que tem curral próprio ou vazio com lugar e o leva até lá. **Lotação pelo tamanho:** um animal a cada 4 blocos de chão. Com pares adultos de espécies distintas, constrói currais até haver um por espécie, intercalados com outras obras, contando cercados fechados existentes; todos usam o mesmo `animal_pen.nbt` de 15×15 | 10-22; emenda 10-31 | ✅ código; falta jogo | `ShepherdPens.forSpecies`, `ShepherdHerding.separate`, `ShepherdPens.capacityOf`, `PenPlans`, `UrgentPlans` |
| 73 | **Caminho de areia:** a vila do deserto tem caminho próprio — cópia do caminho de terra do Vanilla (mesma altura, mesmo comportamento: coberto por bloco sólido volta a ser o chão, aqui areia; quebrado dá areia) com a cor do arenito. No bioma de deserto todo caminho que a colônia assenta (rua, corredor, caminho de obra, buraco fechado pelo construtor) é ele; fora do deserto ele não é usado, e o caminho segue o do chão (Regra 70 e pedido de 10-20). Conta como rua em qualquer lugar | 10-22 | ✅ feita (0.3.89) | `SandPathBlock`, `ModContent.SAND_PATH`, `VillageRoad.pavingOn(world, at, …)`, `VillageRoad.isPathBlock` |
| 74 | **A cama mais perto do trabalho:** cada aldeão dorme na cama da vila mais perto de onde trabalha de fato (a mediana das últimas 32 ações com lugar do diário; com menos de 8, fica onde está). A vila reavalia uma vez por dia, ao amanhecer, e só muda quem fica ao menos 16 blocos mais perto; quem anda mais escolhe primeiro. Cama livre só vale se se alcança a pé da vila **e tem baú livre ao lado**; cama ocupada troca de dono quando a soma das duas caminhadas cai ao menos 16 blocos, mesmo que o dono passe a andar um pouco mais (o sem ofício sempre cede; autor 10-27, 0.3.97); e a tarefa é oferecida primeiro a quem do ofício mora mais perto do lugar dele — boca da mina, obra aberta, bosque, ou o próprio baú (0.3.97); quem está dormindo não muda nem cede. **A distância é a caminhada pelo mapa a pé** (linha reta só onde o mapa não alcança o trabalho, como a mina). **A mudança é uma transação** (emenda de 10-22, R-043 do estudo v2): antes de mexer, cada mudança tem a cama e o baú que vai com ela — numa troca, cada um fica com o baú do outro, remarcado com o próprio ofício; se falta algo, a mudança cai com a outra metade da troca e ninguém fica sem baú. O `HomeMove` (desistiu 2× abaixo do trabalho) continua como reação imediata | 10-22 | ✅ feita (0.3.92; transação 0.3.93) | `BedPlan`, `BedChoice`, `Walks`, `ChestScanner.freeChestBeside`, `ActionJournal` |
| 53 | A boca da mina só nasce na faixa da borda da caixa da vila (até 8 blocos da linha, dentro ou fora); com a mina aberta, o mineiro sem ramal livre não cava pedra exposta na superfície | 10-11 | ✅ feita (0.3.32) | `MineEdge.nearTheEdge`, `MineDigging.mineOf`, `MineTrouble`, `MineDigging.exposedStoneFallback` |
| 75 | **Movimentação natural:** só o mineiro remodela o terreno para subir (degrau, túnel); ninguém põe bloco debaixo dos pés, e não há escada fixa | 10-15, 10-21 | ✅ no código (registrada 10-22) | `ClimbPolicy` |
| 76 | **O encalhado nunca desiste:** larga o ofício e sobe até a superfície; a escada da fuga é tampada depois que ele sai | 10-01 (E47), 09-24 (N10) | ✅ no código (registrada 10-22) | `ClimbOut`, `StrandedEscape`, `EscapeBackfill` |
| 77 | **Cama como último recurso:** preso 5 vezes no mesmo lugar, ou sem caminho para casa à noite, ele vai para a cama | ADR-036 item 15 | ✅ no código (registrada 10-22) | `BedTeleport`, `NightHome` |
| 78 | **Caminhada longa por etapas:** um trajeto de 50 a 130 blocos vira etapas na superfície | 10-08 (A7) | ✅ no código (registrada 10-22) | `WalkLegs` |
| 79 | **A pedra é emprestada:** o alvo do mineiro vale por um prazo curto, e só o progresso renova | 09-10 (E44) | ✅ no código (registrada 10-22) | `MineLease` |
| 80 | **Só vira alvo o bloco com posição de trabalho válida e alcançável** | 10-08 (E1) | ✅ no código (registrada 10-22) | `MiningTarget` |
| 81 | **Ramal com dono:** cada ramal da mina é reservado a um mineiro, com o estado dito pelo nome | 09-04, 10-08 (E2) | ✅ no código (registrada 10-22) | `MineArm`, `MineBranch` |
| 82 | **O mineiro memoriza a entrada da mina** e chega por ela, por etapas, até o nível de agora | 10-08, 10-14 | ✅ no código (registrada 10-22) | `MineEntrance`, `MineApproachPath` |
| 83 | **Espera a areia assentar** antes de voltar a cavar | 09-19 | ✅ no código (registrada 10-22) | `MineSettling` |
| 84 | **Tapa e escora com o bloco do bioma** (arenito no deserto) | 10-20 | ✅ no código (registrada 10-22) | `MineFill` |
| 85 | **O ferro descansa onde a mina não o tem:** o piso de ferro cru não força busca sem veio | 10-21 | ✅ no código (registrada 10-22) | `IronRest` |
| 86 | **A peça que o construtor não alcança surge no lugar** | 10-20 | ✅ no código (registrada 10-22) | `UnreachablePieces` |
| 87 | **Peça equivalente:** sem a exata e sem tag do jogo, vale a equivalente da tabela | 09-24 (N3) | ✅ no código (registrada 10-22) | `EquivalentPieces` |
| 88 | **Terracota, feno, argila, gelo e neve aparecem quando a obra pede** (gelo, gelo compactado e gelo azul desde 10-28: o jogo só os dá com toque suave; bloco e camada de neve desde 10-29, T1; a argila crua do forno aparece no baú do fundidor sem ele sair a buscá-la, T5) | 10-21 | ✅ no código (registrada 10-22) | `AppearingPieces` |
| 89 | **Degrau na frente da porta** do lote que fica um bloco acima da rua | 09-30 | ✅ no código (registrada 10-22) | `DoorStep` |
| 90 | **Terraplanagem do lote:** no legado, corta até 5 e aterra até 2 blocos do chão do bioma, com um corredor de 2 em volta da casa. No MAE (ADR-045), somente células aprovadas: corte/aterro até 2, duas camadas de apoio e transição até 6; `FOUNDATION_GROUND` inclui superfície, aterro e apoio do corredor/transição; calçamento usa `SHAPED_GROUND` | 10-20, 10-21, 11-02 | ✅ contratos locais; MAE sem ativação automática | `LotTerraform`, `TerrainPlanner`, `MaeWorldWriter` |
| 91 | **Casa fundadora:** primeiro o lote plano exato, depois o aplainável; a vegetação do lote sai | 10-20 (G1) | ✅ no código (registrada 10-22) | `FoundationLotSearch`, `LotGrowth` |
| 92 | **Deserto:** a areia da planta vira arenito (emenda à Regra 27), e o chão da vila de areia não desaba | 09-19, 10-21 | ✅ no código (registrada 10-22) | `DesertSand`, `DesertGround` |
| 93 | **Obra abandonada tem retomada limitada** antes de a vila aceitar obra nova | — | ✅ no código (registrada 10-22) | `BuildingRepairPlanner` |
| 94 | **Faixa de construção:** o lote novo nasce ao lado da rua; muda, flor alta ou arbusto crescendo ali tiram o lote da faixa | 10-11 | ✅ no código (registrada 10-22) | `ConstructionZone` |
| 95 | **Todo ofício trabalha independente do pedido** (produção contínua até os tetos) | 09-26 | ✅ no código (registrada 10-22) | `StandingWork` |
| 96 | **Todo trabalhador de ofício tem um baú nascido para ele** (emenda à Regra 8) | 09-26 | ✅ no código (registrada 10-22) | `ChestSpawner` |
| 97 | **Baú cheio chama os outros ofícios**, e os 10 últimos espaços dos baús da colônia ficam sempre livres | 09-19, ADR-036 item 9 | ✅ no código (registrada 10-22) | `ChestRelief`, `ProfessionChestOverflow` |
| 98 | **O que a vila provou não ter no alcance não é mais buscado**, e o raio varrido vazio espera | 10-20, 10-02 (F-1) | ✅ no código (registrada 10-22) | `KnownAbsent`, `EmptySweeps` |
| 99 | **O alcance da coleta cresce com a vila** | 09-24 (N11) | ✅ no código (registrada 10-22) | `GatheringReach` |
| 100 | **Autossuficiência:** britador do pedreiro (pedra → areia), janela sem vidro, compostor do fazendeiro, pedreiro faz o insumo da fornalha, fundidor sem cru solta a fornalha, areia de beira d'água, argila | 09-19 a 10-21 | ✅ no código (registrada 10-22) | `Crusher`, `WindowEquivalent`, `FarmerCompost`, `FurnaceFeed`, `SmelterGathers`, `SandNearWater`, `ClayPatch` |
| 101 | **Receita de tingir peça pronta é ignorada** | 09-26 | ✅ no código (registrada 10-22) | `RecolorRecipes` |
| 102 | **Toda vila nasce com 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos** | 10-20 | ✅ no código (registrada 10-22) | `VillageFarmAnimals` |
| 103 | **Grama ao pé do poste da ovelha** amarrada, e **o golem sai do curral** pela porteira | 10-20, 10-02 | ✅ no código (registrada 10-22) | `PostGrass`, `PenGolems` |
| 104 | **O que o jogador tirou não volta:** árvore, muda ou flor quebrada pelo jogador não é replantada naquela coluna | 10-11 | ✅ no código (registrada 10-22) | `PlayerClearedPlants` |
| 105 | **Duas colônias da mesma vila viram uma**, e a identidade é a vila gerada, não o centro | ADR-007, 09-30 (E51) | ✅ no código (registrada 10-22) | `ColonyMergeTrigger`, `ColonyIdentity` |
| 106 | **A vila é uma caixa** medida pela vila gerada, e não um raio | 09-30 (ADR-003 Emenda 6) | ✅ no código (registrada 10-22) | `VillageBounds`, `VillageMeasure` |
| 107 | **Marcas no mundo:** contorno de chamas no lote, área da vila, dono de cada baú, nome do aldeão no painel do ofício | 08-12 a 10-08 | ✅ no código (registrada 10-22) | `SiteMarker`, `VillageMarker`, `ChestMarker`, `WorkerNameplateMixin` |
| 108 | **A Tocha das Almas cancela a obra:** uma tocha das almas (também a de parede) posta pelo jogador dentro do volume de uma obra aberta cancela o canteiro e as tarefas de construção; o que já foi posto fica no mundo, e a tocha também; a fundação `BigHouseMOD` não é afetada | — | ✅ no código (registrada 10-22) | `ConstructionCancellation` |
| 109 | **A casa fundadora `BigHouseMOD`:** toda vila adotada a recebe uma única vez — cópia editada da casa grande Vanilla, com sete camas e sete baús para os titulares (mineiro, lenhador, carpinteiro, pedreiro, fundidor, pastor, construtor); o fazendeiro trabalha sem cama reservada nela; ela nunca entra no catálogo de obras | — | ✅ no código (registrada 10-22) | `BigHouseFoundation`, `VillageFoundation` |
| 110 | **Nada que a colônia corta some** (R-059 do estudo v2, autor 10-22): o que a terraplanagem do lote e o corte do corredor tiram do terreno vai para os baús públicos da vila; o que não couber cai no chão ao lado da obra, inteiro | 10-22 | ✅ feita (0.3.93) | `Cuttings`, `LotTerraform.level(…, taken)`, `CorridorCut` |
| 111 | **Suprimento excepcional só pela lista declarada** (R-080 do estudo v2, autor 10-22): o que surge sem uma profissão obter está numa lista única — peça que falta (51), drops automáticos (41), chão moldado (69), peça inalcançável (86), terracota/feno/argila (88), chão do aterro e do apoio (90, 21), baú do trabalhador (96), animais da fundação (102), grama da ovelha (103), peça de lado da obra parada (113), ração do rebanho (117); cada vez que algo surge fica uma linha `VC_SUPPLY` (exceção, regra, colônia, item, quantidade), uma por evento — o aterro de um lote é uma linha só —, e `scripts/supply_report.py` soma por exceção e item | 10-22 | ✅ feita (0.3.93) | `SupplyException`, `Supplies`, `supply_report.py` |
| 112 | **Vocabulário único de bloqueio** (R-052 do estudo v2, autor 10-22): toda soltura, desistência, adiamento, recusa ou encalhe no diário leva também um código — `NO_RESOURCE`, `RESOURCE_INACCESSIBLE`, `NO_PATH`, `SITE_BLOCKED`, `NO_VALID_SITE`, `NO_RECIPE`, `TOOL_MISSING`, `CHEST_FULL`, `TARGET_RESERVED`, `PROTECTED_BLOCK`, `NO_PROGRESS`, `UNSUPPORTED_PIECE`, `WORLD_UNAVAILABLE`, `NO_STORAGE` —, traduzido da frase pelo trecho mais específico; a frase que não casa fica `UNCLASSIFIED` (sinal para acrescentar o trecho); o `action_report.py` agrupa por código | 10-22 | ✅ feita (0.3.93) | `BlockCode`, `ActionJournal`, `action_report.py` |
| 113 | **A obra parada só em peças de lado não prende a vila** (autor 10-22, playtest do deserto: o armazém ficou duas horas em "5 blocks left — no build task" e nenhuma obra nova abriu): a cada ciclo, se a obra em construção não tem mais nada a pôr e não terminou, toda peça de lado volta à fila — seja por material, apoio ou alcance — e a tarefa de obra reabre; depois de 4 voltas a peça surge no lugar (`VC_SUPPLY` `STUCK_PIECE`, Regra 111), sem sobrescrever bloco que já esteja ali (Regra 3), e a obra fecha | 10-22 | ✅ feita (0.3.94) | `PieceRescue`, `StuckPieces`, `UnreachablePieces.surge` |
| 114 | **Ninguém é posto onde o corpo não cabe, e ninguém fica preso para sempre** (autor 10-27, jogo da 0.3.93: 12 aldeões da vila da neve e 2 do deserto morreram sufocados segundos depois de levados para a cama; o mineiro da taiga ficou 3,5 h cercado e o construtor da savana 4 h preso sem cama): todo teleporte de aldeão pousa onde o corpo inteiro cabe — chão firme, nada sólido até a cabeça e sem líquido —, em cima do colchão só com espaço até a cabeça, senão no chão livre mais perto da cama; sem lugar, ele não é levado; o preso sem cama vai ao chão do centro da vila; quem passa 3 rodadas seguidas com todas as saídas do cercado falhando é posto no chão logo atrás da cerca | 10-27 | ✅ feita (0.3.95) | `SafeLanding`, `BedTeleport.toSafety`, `PenLift` |
| 115 | **O armazém do mod sobe do chão e é feito do material do bioma** (autor 10-27: o armazém nascia dentro do solo; e no jogo da 0.3.93 a ardósia funda, a calcita e as folhas de acácia dele surgiam no lugar em todo bioma): o modelo do mod sem porta pisa na camada 0, que vai no nível do chão, e o lote é nivelado como o de uma casa; cada estilo (planície, deserto, savana, taiga, neve) tem o seu armazém, com a madeira e a pedra do bioma — no deserto, arenito no lugar da madeira —, sem folhas, alçapões nem teias, e com baú comum no lugar do baú do fim; o de todos os estilos só vale para o estilo sem o seu | 10-27 | ✅ feita (0.3.95) | `StructureBlueprintReader`, `ColonyModels.tradeHousesFor`, `scripts/storage_variants.py` |
| 116 | **A argila aparece na terceira busca vazia, em todo bioma** (autor 10-27, jogo da 0.3.93: na planície sem argila o fundidor passou a sessão parado na fornalha, milhares de linhas "no clay in reach"): argila e bola de argila surgem no baú de quem busca depois de 3 varreduras vazias seguidas do alcance da vila, e não 4 como os outros materiais (Regra 40); depois, a ausência fica registrada e o próximo lote vai direto ao baú | 10-27 | ✅ feita (0.3.95) | `LocateFallback.attemptsFor` |
| 117 | **O pastor recebe ração quando o fazendeiro não produziu** (autor 10-27, jogo da 0.3.93: "the shepherd has no food" milhares de vezes em todo bioma, rebanho parado abaixo da meta): quando o par está pronto para procriar e nenhum baú tem nenhuma comida que a espécie aceite (com um pouco do fazendeiro guardado, o par espera a colheita), a ração vem sozinha — trigo para ovelha e vaca, cenoura para porco, semente de trigo para galinha — e o par é alimentado, com uma linha `VC_SUPPLY` `ANIMAL_FEED` (Regra 111); o ritmo continua o do pastor (um par por espécie por rodada, até 8 adultos) | 10-27 | ✅ feita (0.3.95) | `ShepherdFlock.ANIMAL_FEED` |
| 118 | **No deserto o bosque do lenhador é o dobro e se espalha pelas bordas** (autor 10-27, jogo da 0.3.93: três lenhadores do deserto 61% ociosos, 39 desistências "made no progress" esperando as mudas, todas juntas perto do baú, crescerem): na vila de estilo deserto o bosque tem o dobro de mudas por lenhador e cada muda nova vai a um ponto sorteado do anel em volta da caixa da vila, com as mesmas folgas de casa, obra e rua | 10-27 | ✅ feita (0.3.95) | `VillageGrove.DESERT_FACTOR`, `VillageGrove.isDesert` |
| 119 | **A mina desce em caracol, abre salão e desce de novo pelos quatro cantos, e o caminho que desce é o de volta** (autor 10-27): cada nível é um caracol de uma volta — 4 lances de 3 degraus, um bloco abaixo por degrau, três pistas de largura e três de altura —, um salão de 9 x 9 x 3 ao pé dele (autor 10-28: era 17 x 17, ~4.875 blocos por nível; 9 x 9 dá ~1.755 e mantém o traçado — 7 cavaria o chão dos degraus), e quatro ramais, um em cada canto do salão: outro caracol, virado para fora, que termina num salão do mesmo tamanho no quadrante diagonal daquele canto; o nível seguinte desce do meio do salão do ramal zero, e o ciclo se repete; nenhuma célula planejada é o chão de um degrau ou de um salão (o caracol fica fora das colunas do salão, e cada ramal fora das do salão e dos outros), e o passo entre caracol, salão e ramal é de no máximo um bloco; a rota do mineiro passa pelo fim de cada lance, pela borda do salão e pelo canto, em trechos curtos; o ramal novo segue indo primeiro para o minério que a vila pede (ADR-039 D3), e o veio que ele segue nunca sobe acima da altura em que começou — de lado e para baixo segue, com o degrau de volta (item 7, 0.3.97); a rede de corredores (ADR-042) foi removida do código (0.3.97; o save antigo ignora as chaves dela). Emenda de 10-29 (autor): ao recomeçar, a mina volta à mesma boca e vira para o rumo seguinte; boca nova nunca (`MineRestart`) Emendada pela Regra 254: patamar de 3 x 3 com tocha nas dobras dos níveis novos. | 10-27 | ✅ feita (0.3.96) | `MineShaft`, `MineShaft.waypoints`, `MineEntrance.route`, `MineShaftLayoutTest` |
| 120 | **A tarefa do lenhador fecha concluída quando o alcance esvazia** (autor 10-27, jogo da 0.3.93: nenhuma tarefa de 256 toras chegava ao fim; todas saíam como "made no progress" e o relatório dava 0% de conclusão): sem árvore madura no alcance depois de ter cortado alguma, a tarefa termina CONCLUÍDA com o que rendeu, e o ciclo seguinte abre outra se a vila ainda pedir madeira; sem ter cortado nada, ele segue esperando o bosque | 10-27 | ✅ feita (0.3.96) | `TreeChoice.closeEmptied` |
| 121 | **A roça conhecida responde primeiro, sempre** (autor 10-27, jogo da 0.3.93: 622 "still sweeping" com roça madura conhecida esperando, e a planície parou em 16 aldeões com 41 camas): o registro de canteiros da colônia é olhado inteiro a cada passagem — o custo é o número de canteiros, não o tamanho da vila —, e o canteiro arado pelo fazendeiro entra nele na hora; lavoura madura do registro vira trabalho mesmo com a varredura do raio no meio da volta; com só canteiro vazio no registro, a varredura anda mais um trecho atrás de lavoura madura e o canteiro fica de reserva; a varredura do raio serve para achar roça nova | 10-27 | ✅ feita (0.3.96) | `CropPatch.survey`, `KnownFarmland.register`, `FarmerTill` |
| 122 | **O curral é do pastor** (autor 10-27, jogo da 0.3.93: 217 entradas em curral na planície e o mineiro da taiga 3,5 h cercado com a cama dentro): o chão de dentro dos currais que o pastor mede fica fora do caminho de todo aldeão de ofício que não é pastor e está fora do curral (nó bloqueado; autor 11-04, 0.4.18) — quem já está dentro anda nele com custo de perigo leve para sair; quem entra por portão aberto sai por ele e mantém a tarefa; o aldeão achado cercado sem portão junto da própria cama larga essa cama, e o jogo escolhe outra; o portão que um aldeão abre fecha sozinho quando ninguém está nele (como já era) | 10-27 | ✅ feita (0.3.96) | `PenFloors`, `LandPathNodeMakerMixin`, `PenLift.leaveBedInPen`, `PenEscape` |
| 123 | **A rede de caminhos da vila:** toda porta se liga à rede pelo grafo, com o trajeto mais curto de chão andável, nunca sobre a água (Regra 264). Regras 214 e 216 | 10-27 | ✅ feita (0.3.96; atalho ao chafariz quando a rede passa de 2× o direto, D5, 0.3.97; rente ao chão, 0.3.99) | `VillagePathPlanner`, `WorldPathTerrain`, `PathLinks`, `PathPaving`, `NetworkDetour`, `GeneratedVillages.housesAt` |
| 124 | **Construção nova nasce a no mínimo 3 blocos de qualquer construção** (autor 10-27, D1): o lote cuja pegada fica a menos de 3 blocos da caixa de outra construção, ou da obra aberta, é recusado (`TOO_CLOSE`); o caminho pode encostar no lote | 10-27 | ✅ feita (0.3.96) | `LotSpacing`, `LotLevel.flatGroundAt` |
| 125 | **Quem assenta o caminho** (autor 10-27, D6): o pedreiro, o carpinteiro e o fundidor sem tarefa do ofício, no expediente e soltos, um por vez por colônia, revezando nessa ordem a cada 8 passos; cada passo é a seção de 3 colunas — aterro, corte do que está em cima do piso, caminho de terra e 2 livres —, e se o mundo mudou e a seção não serve mais, o traçado sai e é refeito | 10-27 | ✅ feita (0.3.96) | `PathPaving` |
| 126 | **Peça colorida sai branca** (autor 10-27, item 5; jogo da 0.3.93: 64 "cama sem lã" e lã preta parada por corante): cama, lã, tapete e estandarte de qualquer cor entram na planta brancos, onde ela é montada — a lã que o rebanho dá é branca e nenhum corante é pedido; as outras peças não mudam | 10-27 | ✅ feita (0.3.96) | `WhitePieces`, `StructureBlueprintReader` |
| 127 | **Ajudas de quem está sem tarefa do ofício, cada uma com um ofício fixo** (autor 10-27, item 8; 11-06): o lenhador vai ao bosque, repõe muda e põe farinha de osso, no máximo a cada meio minuto; o pastor alimenta o rebanho; o pedreiro calça (Regra 215), nivela e recolhe com a pá a neve natural da vila quando a obra abre; o adulto sem ofício carrega. A **tocha da colônia** é do jogador (Regra 212): luz 15, um anel de 8 luzes invisíveis a 10 blocos, e monstro não nasce sozinho a 30 blocos dela; quebrada, o anel sai junto | 10-27 | ✅ feita (0.3.97) | `IdleHelps`, `KnownFarmland`, `ColonyTorchBlock`, `ColonyTorches`, `HostileSpawnMixin`, `SnowShovel` |
| 128 | **Nenhuma profissão espera mais de 5 minutos por nada** (autor 10-27, emenda 11-02): 300 s sem progresso útil na tarefa — caminhar não conta — libera a reserva e registra a tentativa diária. Na segunda falha da mesma demanda no dia, o item físico não construtivo pode ser reposto no baú do ofício com `WAITED_TOO_LONG`; obra nunca surge por essa via. Progresso útil reinicia apenas a tarefa que alterou o mundo | 10-27; emenda 11-02 | ✅ código; falta jogo | `TaskProgressWatch`, `DemandRetries` |
| 129 | **O nome sobre a cabeça tem a cor do estado** (autor 10-27): laranja no ofício, azul em ajuda, vermelho bloqueado ou encalhado há 30 s, branco à toa no expediente, cinza fora dele | 10-27 | ✅ feita | `WorkTime.Badge`, `ProfessionOverlayRenderer` |
| 130 | **Cinco animais no mesmo poste pedem curral, com prioridade** (autor 10-27): o curral do estilo entra pela fila urgente; pronto, o pastor leva os do poste um a um | 10-27 | ✅ feita | `PenPlans.crowdedPost`, `UrgentPlans` |
| 131 | **O chão em volta da obra nova fica na altura da rua** (autor 10-27): na abertura, um anel de 2 blocos na altura da rua ±1 e uma rampa de 3 que abre a faixa em 1 por passo; só chão natural é cortado e só o vazio é aterrado; nunca construção, peça da vila, bloco do jogador, caminho, água, árvore ou a caixa de outra construção | 10-27 | ✅ feita | `SiteSurroundings` |
| 132 | **As bancadas do mod têm painel e passivas** (autor 10-27): shift+clique mostra titulares, estado, tarefa e espera; só com a bancada do ofício na vila valem as passivas — carpinteiro (ferramenta fabricada sai encantada, conserta ferramenta a 8), fundidor (armadura encantada, restaura armadura a 8, fornalhas 2× a 32), lenhador (mudas crescem a 32), fazendeiro (roça a 24, compostagem), pastor (lã rápida e recolha dos drops; alimentação, cruzamento e abate exigem pastor próximo, Regra 167), pedreiro (tapa buraco de rua, +1 pedregulho a cada 4), construtor (1 bloco a mais por minuto, material vem ao baú), mineiro (mina sem monstro a 64, minério em dobro, gema +1, 1% de ferramenta) | 10-27; emenda 10-31 | ✅ código; falta jogo | `BenchPanel`, `BenchPassives`, `BenchUpkeep`, `BenchYield`, `CraftedEnchant` |
| 133 | **Estoque cheio vira obra e produto** (autor 10-28, jogo da 0.3.99: lenhador 74%, pedreiro 48% e fundidor 44% parados com 413 toras, 1.202 pedregulhos e 695 pedras guardados): com 80% dos espaços dos baús da vila ocupados (até cair abaixo de 70%), a próxima obra é o armazém e o adulto que sobra vira mais um construtor; sem tarefa, os ofícios repõem produtos de valor até 16 por tipo conforme o consumo, preservando cru para a obra | 10-28; emenda 10-31 | ✅ código; falta jogo | `ProfessionChestOverflow.measureFullness`, `ProfessionDemand.setSpare`, `ValueGoods` |
| 134 | **O adulto que sobra vai ao ofício que a vila espera** (autor 10-28, jogo da 0.3.99: 6 de 18 aldeões sem profissão): com todas as profissões na meta do crescimento, a vaga a mais vai à profissão que a obra espera agora, na ordem da falta, e ao construtor quando os baús estão cheios; sem espera, vale a meta do crescimento; o máximo de cada profissão continua | 10-28 | ✅ feita (0.3.100) | `ProfessionAssigner.spareHand`, `ProfessionDemand` |
| 135 | **Tarefa só abre quando pode render** (autor 10-28, jogo da 0.3.99): a ordem de estoque de peça de tábua não abre sem tábua no baú quando a reserva em tora (Regra 248) não deixa converter (69 tarefas fechadas com 0 peça); a tarefa de lã só é reservada com ovelha lanuda na área do pastor e com menos de 64 da lã pedida nos baús (28 tarefas sem ovelha com lã) | 10-28 | ✅ feita (0.3.100) | `ItemStock.woodHeldInLogs`, `ReservationGate.WOOL_ON_A_SHEEP`, `WoolGate` |
| 136 | **O fazendeiro alcança como o jogador** (autor 10-28, jogo da 0.3.99: roça 2 blocos acima, a 3 de distância, e 9 desistências): colhe e planta a até 4,5 blocos do olho até o meio da planta, com a linha livre, além do alcance de 3 do braço | 10-28 | ✅ feita (0.3.100) | `FarmerWork.seesWithinEyeReach` |
| 137 | **A escada da mina recomeça do topo** (autor 10-28, jogo da 0.3.99: o mineiro passou 30 min na boca, 30 blocos acima do nível, sem descer — a escada entre a superfície e o nível era de antes do caracol): com 2 desistências seguidas em 10 min de um mineiro a 8 blocos ou mais acima da entrada do nível, a mina recomeça na boca de superfície (a lembrada, se está no chão da superfície; senão o chão em cima da entrada, se se alcança a pé) com o cursor no primeiro degrau | 10-28 | ✅ feita (0.3.100) | `MineRestart` |
| 138 | **A rua da colônia é só caminho e é da vila** (autor 10-28; depois do jogo da 0.3.104: "voltar para utilizar apenas blocos de caminho"): meio e borda são o caminho do chão (`dirt_path` na terra, arenito liso na areia, caminho de areia no deserto, cascalho na pedra); o cascalho, a terra grossa e o arenito que a mistura gasta antiga assentou voltam a ser caminho na passada do pedreiro; o chão de rua numa coluna que a colônia assentou nenhuma profissão minera nem recolhe | 10-28 | ✅ feita | `StreetSurface`, `StreetGaps.mend`, `BlockProtection.mayBreak` |
| 139 | **O lote sai do mapa de interesse** (autor 10-28, jogo da 0.3.102: casa a 140 blocos, 24 acima do centro): a busca percorre a rua do centro para fora e, achado o primeiro lote, olha mais 64 colunas e fica com a menor nota — distância ao centro + 3 × a diferença de altura + o caminho até a rua; a nota e a distância média das construções saem no `VC_STATE` | 10-28 | ✅ feita | `LotScore`, `RoadIndex.findAmongRoads`, `ColonyState` |
| 140 | **A rua estica pela ponta mais perto do centro**, com um bloco de largura (Regra 186), pedindo as colunas ao grafo (Regra 214) | 10-28 | ✅ feita | `RoadPaving.layEdges`, `RoadPaving.isMiddleOf`, `RoadExtension` |
| 141 | **O centro da vila não muda** (autor 10-28, taiga simulada: o curral sem lote foi a 98 blocos, a caixa cresceu, o centro andou 40 blocos e 4 obras foram atrás): a primeira medida põe o centro no meio da caixa; depois a caixa só cresce e o centro fica; a busca de lote vai do centro ao canto mais longe da caixa + 15; o lote da faixa anexada fica a até 48 blocos do centro, e cada volta inteira pela faixa sem lote estica o alcance em 10 | 10-28 | ✅ feita | `Colony.measure`, `ConstructionPlanner.reachOf`, `StripLot.REACH` |
| 142 | **À noite só o preso vai à cama** (autor 10-28, taiga simulada: 114 teleportes, um por aldeão por noite, 66 de mais de 48 blocos): o caminho de casa é buscado a até 128 blocos com 4× os nós do jogo; sem caminho, o aldeão só vai à cama se ficou a menos de 3 blocos do lugar em 2 conferências seguidas (2 × 600 tiques) | 10-28 | ✅ feita | `NightHome` |
| 143 | **O baú público enche inteiro** (autor 10-28, taiga simulada: 27 vezes o lenhador largou toras no chão com a vila a 38%): o que não cabe no baú do ofício vai aos baús públicos sem o teto de 3 compartimentos por item, do mais perto ao mais longe, um enchendo antes do seguinte | 10-28 | ✅ feita | `PublicChests.store`, `ChestDepositor.deposit` |
| 144 | **Alvo de coleta inalcançável sai da memória** (autor 10-28, taiga simulada: 3 fazendeiros voltaram 79 vezes à mesma terra fora de alcance): o alvo que o coletor não alcança é esquecido pela memória de manchas e a coluna, com 2 em volta, fica fora das buscas por 1 dia de jogo | 10-28 | ✅ feita | `SurfaceHits.unreachable` |
| 145 | **As camas da vila são as da caixa** (autor 10-28, taiga simulada: a sonda de 64 blocos via parte da vila e o número oscilava 15→7→18→10): medida a vila, a cada 30 s contam-se as camas que o jogo registrou dentro da caixa, na faixa de altura da vila, só com todos os chunks dela carregados; a sonda não muda mais esse número | 10-28 | ✅ feita | `BoxBeds`, `Colony.countBeds` |
| 146 | **Fundidor e pedreiro produzem mais** (autor 10-28, taiga simulada: fundidor 80% e pedreiro 64% parados com 1.209 pedras): o fundidor funde minério cru (ferro, ouro, cobre), assa carne e faz pedra, pedra lisa, vidro, tijolo, terracota e arenito liso; o pedreiro faz tijolo de pedra, degrau, laje e muro de pedregulho, tijolo entalhado, arenito cortado e pedras polidas — cada receita na proporção dela, com teto auxiliar de 16 por tipo (emenda da Regra 133) | 10-28; emenda 10-31 | ✅ código; falta jogo | `ValueGoods` |
| 147 | **O diário da vila bate com o jogo** (autor 10-28): cada fato vira `VC_EVENT` (obra aberta e fechada com as peças contadas no mundo, caixa que cresce, camas, teleporte, nível da mina, morte); ao virar o dia de jogo, `VC_DAY` com o tempo das profissões no dia inteiro e `VC_AUDIT` conferindo camas, trabalhadores vivos e peças da obra aberta que já estão no lugar; tudo no arquivo do mundo, lido pelo `state_report.py` | 10-28 | ✅ feita | `VillageDiary` |
| 148 | **A obra nunca fica largada meio pronta** (autor 10-28, taiga simulada: casa em "0 blocks left" 28 min sem tarefa e largada): sem peça a pôr, a obra fecha pelo ciclo da colônia; parada no fim (só peças adiadas por material ou apoio, ou nenhuma peça na janela de paciência numa obra começada), tudo o que falta surge no lugar e cada peça vai ao registro de faltas com o motivo END_OF_BUILD; só a obra sem nenhuma peça posta é largada | 10-28 | ✅ feita | `BuildClosing`, `WaitingWork` |
| 149 | **Rua sem falha:** falha de até 2 blocos de chão natural numa linha reta entre duas colunas de rua é pedida ao grafo (Regra 214). As demais ajudas cruzadas seguem suspensas pela regra temporária de 11-02; a exceção é a Regra 215 | 10-28 | ✅ feita | `StreetGaps`, `VillageHelpers`, `SiteSurroundings.level` |
| 150 | **O pastor tosquia por cima da cerca** (autor 10-28, diagnóstico da 0.3.103: 45 pastores presos no curral): ele tosquia a ovelha a até 4,5 blocos do olho ao meio dela, como o fazendeiro, e anda só até 3 blocos dela, em vez do bloco dela; não entra no curral para tosquiar (a condução de animal, que já mira o lado de fora, segue igual) | 10-28 | ✅ feita | `ShepherdWork.EYE_REACH` |
| 151 | **O aldeão da colônia prefere a rua e evita a água** (autor 10-28, jogo da 0.3.104: 4 de 7 teleportes noturnos saíram do meio do lago): só o custo dos nós da navegação do jogo muda — o nó de água custa 48 (o Vanilla cobra 8; a busca do jogo aceita caminho até 1,5× mais caro, então o custo é alto de propósito), e ele nada só quando a volta a pé não existe ou é muito longa; dentro da caixa da vila, o nó andável sem chão de rua embaixo custa 1, e a rua ganha quando o desvio por ela não passa de ~2× a linha reta; **o trajeto de 24 blocos ou mais vai pela rede de ruas**: ele vai à coluna de rua mais perto (até 8), anda pela rede em trechos de até 32 passos até a coluna mais perto do destino e sai dali — não quando a rua não liga os dois pontos ou a volta passa de 2× o direto | 10-28 | ✅ feita | `ColonyPathing`, `StreetRoute`, `LandPathNodeMakerMixin`, `MoveToTargetTaskMixin` |
| 152 | **O solo da vila nasce suave na geração** (autor 10-29: crateras e montanhas no meio das vilas; refeita no mesmo dia depois do jogo da 0.3.108: "quebrou uma montanha no meio e deixou partes voando"): ao gerar cada chunk da vila, antes de as peças serem postas, **na vila** (as peças + 4) a depressão que não escoa é tampada, o calombo de até 4 colunas é cortado e nenhuma coluna fica mais de 1 acima ou abaixo da vizinha, com o piso das casas e a água no lugar; **em volta**, o chão é a mistura do nível da vila com o terreno natural, numa faixa de 4 vezes o desnível, de 16 a 64 blocos, em curva suave — o relevo natural fica, achatado na proporção, sem escada nem paredão; a vila referencia os chunks até 64 além das peças (o jogo, 12), e o que o jogo não alcança (8 chunks do início da vila) fica natural; a árvore em pé sobre chão cortado sai inteira; os vãos de caverna até 8 abaixo do chão novo são tampados; as ruas do Minecraft nascem sobre o chão novo. Só em chunks gerados depois (mundo novo) | 10-29 | ✅ feita | `VillageBlend`, `VillageSurface`, `VillageGround`, `ChunkGeneratorMixin` |
| 153 | **Mais ajudas de quem está sem tarefa** (autor 10-28, O1/O3/O4/O8; pedido de novo em 10-29), uma vez por minuto, no expediente: o adulto **sem profissão é o carregador** — leva o baú da boca da mina aos baús públicos e alivia os baús de profissão quase cheios; o **pastor** com o rebanho abaixo da meta (8) dá 2 trigos dos baús públicos a duas ovelhas adultas, e elas cruzam; o **carpinteiro** adianta a madeira — tábua da tora que a vila tem mais, e dela escada e laje da mesma espécie; no **pedreiro e no carpinteiro**, a peça que a obra aberta pede (pela planta) é feita primeiro; cada receita guarda a reserva das obras (Regra 146) | 10-29 | ✅ feita | `VillageHelpers.helps`, `FlockCare`, `ValueGoods` |
| 154 | **Lenhador, fundidor e fazendeiro: os ajustes do O5–O7** (autor 10-28; pedido de novo em 10-29): a árvore que o lenhador não alcança fica de fora **um dia** na primeira recusa (era 5 min; a recusa de parede segue em 10 ciclos) e o bosque nasce de 4 a 6 blocos da vila (era até 10); o fundidor busca areia e argila até **40** além da borda (a busca comum, 10); o fazendeiro faz **composto** — 24 sementes acima da reserva viram uma farinha de osso | 10-29 | ✅ feita | `TreeMarks.outOfReachFor`, `VillageGrove`, `SurfaceReach`, `ValueGoods` |
| 155 | **A boca da mina ganha o portão do autor** (10-29, estrutura `mine_entrance.nbt`, 9×7×5): no lugar do arco de pedregulho, dois pilares de pedra e tijolo, telhado de carvalho escuro e três lanternas penduradas; a entrada da mina — as três pistas da escada — fica no centro do vão de 3 por 4, e o vão corre no rumo da descida; baú e barril são só enfeite; cada bloco entra só sobre ar, planta ou chão natural, nunca sobre casa ou obra (Regra 3); sobe uma vez só, e o que o jogador derrubar não volta; a mina nunca escava um bloco do portão | 10-29 | ✅ feita | `MineGate`, `MineMouth` |
| 156 | **O pedreiro alisa o chão da vila** (autor 10-29, 11-06): sem tarefa, uma vez por minuto ele pega a próxima janela de 24×24 que varre a caixa da vila e deixa o chão com degrau de no máximo 1, pondo e tirando solo com o bloco de cima e o de baixo da própria coluna | 10-31 | 🟡 em validação | `GroundSmoothing`, `VillageSurface`, `VillageHelpers.masonHelps`, `GroundSmoothing.aroundTheOpenBuild` |
| 157 | **O mineiro não quebra o que segura algo solto** (autor 10-29, jogo da 0.3.108: o cascalho pendurado sobre um vão caiu no mineiro e o matou): ele não quebra o bloco que tem, acima e pelo ar, até 8 blocos, um bloco que cai, água ou lava; a pedra fica marcada e a galeria contorna; o bloco que cai logo acima segue escorado antes de o alvo sair (pedido de 10-20) | 10-29 | ✅ feita | `MineSettling.looseAbove`, `MinerHands` |
| 158 | **Sem cama no andar de baixo da casa de escada de mão** (autor 10-29, jogo da 0.3.108: 9 de 21 encalhes e teleportes junto da escada da `plains_small_house_5`): uma vez por dia, quem dorme ali muda para a cama livre mais perto do trabalho (ou da cama de hoje, sem ofício), e a cama de lá não entra na troca do dia; sem cama livre, fica | 10-29 | ✅ feita | `BedPlan.leave`, `BedChoice`, `GeneratedVillages.ladderLowerFloorsAt` |
| 159 | **O aldeão da colônia pula 2 blocos e por cima da cerca** (autor 10-29, depois do jogo da 0.3.109: pastor preso 4 vezes num curral sem portão): o caminho dele aceita subir 2 blocos de um passo e passar a cerca, e o pulo dele vai a uns 2,2 blocos (força de pulo 0,42 → 0,62); o corpo continua com o passo do jogo, então ele pula de verdade em vez de deslizar; vale só para os aldeões da colônia | 10-29 | ✅ feita | `ColonyPathing.stepFor`, `ColonyPathing.prepare`, `LandPathNodeMakerMixin` |
| 160 | **O construtor trabalha do chão, e quem fica num telhado desce** (autor 10-29, jogo da 0.3.109: construtor preso 3 vezes no telhado de uma casa, a obra 32 min com 1 peça): o lugar de pé que ele procura fica no máximo 1 acima do chão da obra — da zona da obra (pegada + 4) ele põe qualquer bloco sem subir; o encalhado em pé sobre construção, em 5 das 8 colunas a 4 blocos, se joga do telhado rumo ao chão mais perto, até 8 blocos, com o chão 2 ou mais abaixo do pé (autor, 10-29: "deve se atirar dos telhados"); depois de 3 pulos sem sair é posto no chão | 10-29 | ✅ feita | `BuilderApproach`, `RoofDescent`, `StrandedEscape` |
| 161 | **Comer enche a vida** (autor 10-29): todo aldeão que come (a comida do inventário, ou para cruzar) ou recebe comida (pega do chão pão, batata, cenoura ou beterraba, o que o colega joga) fica com a vida no máximo | 10-29 | ✅ feita | `VillagerMeals`, `VillagerEntityMixin` |
| 162 | **O miolo do cercado fica livre** (autor 10-29, jogo da 0.3.110: o cercado nasceu com blocos de grama dentro, à altura da cerca): o alisamento do pedreiro e do construtor não mexe dentro da caixa de construção ou obra da colônia (com 1 de folga) — o vão entre as cercas parecia buraco e era tampado; e o pedreiro tira o chão natural e a planta até 3 acima do piso, dentro dos cercados da colônia que já nasceram assim | 10-29 | 🟡 feita, sem teste próprio | `GroundSmoothing.footprintsOf`, `GroundSmoothing.clearPens` |
| 163 | **Roça por fazendeiro e pela maior das contas** (autor 10-30): a vila abre uma roça para cada 15 adultos ou para cada fazendeiro, valendo a maior conta; fazendeiro sem roça põe a roça na frente da fila | 10-30 | ✅ feita | `ColonyFarms`, `ConstructionPriority` |
| 164 | **Casa de reserva respeita teto de camas** (autor 10-30): sem outro tipo com lote, a casa de reserva só abre quando obrigatória ou quando as camas ficam abaixo de adultos + 4 | 10-30 | ✅ feita | `ConstructionPriority.fallsBackToAHouse` |
| 165 | **Armazém pequeno antes do grande** (autor 10-30): quando a vila precisa de armazenamento, abre primeiro o `storage_small` de 7×7; o `storage_majest` só abre após o galpão pequeno terminar | 10-30 | ✅ feita | `StoragePlans`, `ConstructionPriority` |
| 166 | **Transição em volta do lote:** após cortar o terreno para a obra, o corredor de seis colunas sobe em degraus de um bloco até a encosta; a superfície cortada volta ao topo, com apoio sólido. Não corta caminho, fluido ou peça protegida | 10-31 | ✅ código; falta jogo | `LotTerraform`, `LotTerraformGameTest` |
| 167 | **Cuidado físico do rebanho:** só pastor vivo a até 5 blocos alimenta ou abate; a passiva da bancada não cruza animais sozinha. O abate preserva pelo menos seis adultos por espécie (emenda das Regras 43, 68 e 132) | 10-31 | ✅ código; falta jogo | `ShepherdFlock`, `FlockCare`, `FlockHarvest`, `BenchPassives` |
| 168 | **Um curral grande por vila nova:** a vila gerada após esta regra constrói no máximo um curral grande, intercalado com outras obras, para até 20 animais. O modelo único em todos os biomas tem portão, tochas, água, 12 cultivos e grama no interior. Currais pequenos e cercados legados continuam reconhecidos e funcionam; eles não bloqueiam nem substituem o curral grande | 10-31; emenda 11-02 | ✅ código; falta jogo | `PenPlans`, `ShepherdPens`, `colony/large_animal_pen.nbt` |
| 169 | **Passagem livre:** aldeão da colônia quebra aos poucos as folhas naturais que bloqueiam seu trajeto; folhas persistentes e estruturas protegidas permanecem | 10-31 | ✅ código; falta jogo | `LeafPaths` |
| 170 | **Lote sem árvores:** ao escolher terreno para qualquer estrutura, remove a árvore natural inteira, copa conectada inclusive, antes da construção; madeira vai aos baús e excesso cai no mundo | 10-31 | ✅ código; falta jogo | `LotGrowth`, `SitePreparation` |
| 171 | **Pulo do aldeão** (autor 11-04): a altura padrão do jogo; o maior só com obstáculo, cerca, encalhe ou saída de curral | 11-04 | ✅ feita (0.4.19) | `VillagerMoves` |
| 172 | **Água aberta** (autor 11-04): com mais de 5 blocos de água à frente, todo aldeão anda cerca de 5× mais rápido que no Vanilla | 11-04 | ✅ feita (0.4.19) | `VillagerMoves` |
| 173 | **Zona da obra limpa** (autor 11-04): árvore, tronco, folha e flor saem em todas as alturas, com o que encosta, até 16 blocos além da zona e 2.000 blocos; emenda a Regra 22 | 11-04 | ✅ feita (0.4.19) | `TreeClearing` |
| 174 | **Ponte** (autor 11-04): sem caminho a seco na zona da vila, o construtor atravessa a água, até 64 colunas, com tábua e cerca dos baús; a casa principal prefere lote ligado a seco | 11-04 | ❌ revogada pela Regra 264 (0.4.70) | `BridgeDeck`, `DryLand`, `WorldPathTerrain` |
| 175 | **Casa do mineiro:** ver Regra 211. A casa e a boca da mina entram na rede pelo grafo (Regra 214) | 11-04 | ✅ feita (0.4.20) | `MineHomePlans`, `MineHomeSiteSearch`, `PathLinks` |
| 176 | **Sinalizador dos caminhos** (autor 11-04): `/vc mae mostrar ligar\|desligar` desenha o que a vila guarda como caminho; só desenvolvimento, operador, não persiste | 11-04 | ✅ feita (0.4.20) | `RoadMapSync`, `RoadMapRenderer` |
| 177 | **Grafo como mapa de trilhos** (autor 11-05): uma linha contínua por corredor, no bloco central da faixa; nó só onde linhas se encontram, nunca nó ao lado de nó; vão de até 2 blocos ainda liga; degrau de no máximo 1 bloco; toda a rede conectada, sem grafo isolado; entrada de estrutura com ramal até um nó; relida de tempos em tempos, porque o jogador pode mudar o caminho; emenda a ADR-043 | 11-05 | ✅ feita (0.4.22) | `RoadLines`, `RoadMask`, `RoadThinning`, `RoadChains`, `RoadRouting`, `RoadGraphRefresh` |
| 178 | **Acesso livre** (autor 11-05): o primeiro bloco fora de toda porta e portão é caminho; portão é entrada do grafo; a correção do caminho tira árvore e chão natural acima do piso no trajeto; o pedreiro limpa o anel de 2 blocos em volta das construções e nivela o vão de até 5 blocos entre construções do mesmo nível, em calombo de até 3; níveis diferentes ficam com o alisamento de degrau 1 | 11-05 | ✅ feita (0.4.23) | `RoadLines`, `RoadGraphRefresh`, `TreeClearing`, `WorldPathTerrain.canCut`, `BuildingGaps` |
| 179 | **Zona da obra limpa até o céu** (autor 11-05): tudo que é natural e desprotegido na pegada, do piso para cima, é colhido antes da obra; a obra não fica sobre árvore nem mantém bloco de antes; bloco protegido não é tirado e obra sem peça; emenda a Regra 22 | 11-05 | ✅ feita (0.4.23) | `TreeClearing.clearZone`, `SitePreparation` |
| 180 | **Caminho coberto não é caminho** (autor 11-05; emendada pela Regra 231): bloco de caminho sem 2 blocos livres em cima vira terra, ou areia no deserto, para o grafo ler só o que se anda | 11-05 | ✅ feita (0.4.24) | `RoadGraphRefresh` |
| 181 | **Pulo confere o teto** (jogo 11-05): o caminho do aldeão da colônia só sobe onde o corpo cabe, também no nó fora da rua | 11-05 | ✅ feita (0.4.25) | `LandPathNodeMakerMixin` |
| 182 | **A mina cresce por dentro** (autor 11-05): fim de escada abre ramal a partir dos salões que existem, sem cortar degrau; boca nova só com 50 falhas de ramal ou com tudo o que resta na água, e nunca perto de outra boca; emenda a Regra 53 | 11-05 | ✅ feita (0.4.25) | `MineBranches`, `MineEdge` |
| 183 | **Anel de caminho** (autor 11-05): o chão em volta do chafariz é caminho, sem quina fixa no grafo. Plantação: Regra 208 | 11-05 | ✅ feita (0.4.25) | `RoadShape` |
| 184 | **Quina é entrada** (autor 11-05): da terceira releitura em diante, a quina da plantação mais perto do centro é entrada do grafo, se nenhuma linha passa a até 2 blocos dela (Regra 223) | 11-05 | ✅ feita (0.4.25) | `RoadShape` |
| 185 | **A ponta cresce** (autor 11-05, emendada 11-10): por dia, as 4 pontas, uma por lado, avançam um bloco para fora (Regra 229); o construtor calça | 11-05 | ✅ feita (0.4.25) | `RoadShape` |
| 186 | **Rua de um bloco** (autor 11-05): o caminho a até 3 blocos da linha que não é linha nem frente de porta volta ao solo do bioma, pelo pedreiro | 11-05 | ✅ feita (0.4.25) | `RoadLines`, `PathTrim` |
| 187 | **Revogada** (autor 11-06, ADR-047). A obra pedida não reserva região | 11-05 | ✅ feita (0.4.26) | `MaeHandover`, `MaeSiteRecord`, `MaeSites`, `MaeStructureSite` |
| 188 | **Pedido só do que falta** (jogo 11-05): o pedido de peça de obra não abre quando os baús cobrem o que a obra ainda vai pôr | 11-05 | ✅ feita (0.4.27) | `CraftsmanRequest` |
| 189 | **Madeira** (autor 11-05): dentro da zona de prioridade, o lenhador pega a árvore conhecida mais perto dele; quem está sem ofício vira lenhador quando a obra espera madeira, mesmo sem vaga aberta | 11-05 | ✅ feita (0.4.27) | `TreeChoice`, `HiringQuota` |
| 190 | **Cru na reserva** (jogo 11-05): com o cru nos baús e a reserva impedindo assar, o fundidor larga o pedido em vez de esperar na fornalha | 11-05 | ✅ feita (0.4.27) | `SmelterGathers` |
| 191 | **Pedido sem resultado espera** (autor 11-05): 3 fechamentos seguidos sem peça seguram o pedido de oficina ou fornalha por 1 min, dobrando a cada nova falta até 30 min; peça feita zera; o cru aumentar ou obra nova solta a espera | 11-05 | ✅ feita (0.4.28) | `TaskLoopGuard`, `TaskOutcome` |
| 192 | **Tora é do carpinteiro** (autor 11-05): sem peça a fazer, os outros ofícios fecham a tarefa em vez de converter tora em tábua | 11-05 | ✅ feita (0.4.28) | `CraftingWork` |
| 193 | **Mineiro alarga o nível** (autor 11-05): quem espera ramal cava em volta do salão, até 6 anéis e 2 alturas, sem cortar degrau nem o chão dele e sem encostar em fluido | 11-05 | ✅ feita (0.4.28) | `MineSideWork` |
| 194 | **Coleta no expediente** (autor 11-05): abate do pastor e item do chão só no expediente; emenda a Regra 48 | 11-05 | ✅ feita (0.4.28) | `ShepherdFlock`, `GroundPickup` |
| 195 | **Reunião do fim do dia** (autor 11-05): o expediente acaba em 10000 tiques; até 11000 os aldeões vão ao sino da vila ou ao centro, menos quem está encalhado, longe ou fundo na mina | 11-05 | ✅ feita (0.4.28) | `WorkClock`, `EveningGathering` |
| 196 | **Curral antes do poste** (autor 11-05): o pastor leva o animal solto ou amarrado ao curral com lugar; poste só em vila sem curral ou com todos lotados. Curral longe vai por pernas. Portão com rua na saída conta como acesso. Emendada pela Regra 255: com curral na vila, nunca amarra, e o amarrado vai primeiro. | 11-05 |
| 197 | **Folha no caminho se corta** (autor 11-05): todo aldeão da vila com destino, parado ao lado de folha natural, corta uma por passada. Folha posta à mão e de obra da colônia ficam. | 11-05 |
| 198 | **Lenhador no interior da vila** (autor 11-05): a árvore é escolhida por faixas de 32 blocos a partir do centro; dentro da faixa, a mais perto dele. | 11-05 |
| 199 | **Sem muda perto de estrutura** (autor 11-05): árvore a até 3 blocos de casa, obra ou peça da vila gerada não é replantada; rua da vila não conta como estrutura. | 11-05 |
| 200 | **A obra pedida limpa o próprio lote** (autor 11-05): árvore natural, folha e mato alto não recusam o lote. Lote sem árvore tem preferência, e nunca sai obra da colônia, peça da vila gerada nem o que foi posto à mão | 11-05 |
| 201 | **Revogada** (autor 11-06, ADR-047). A régua do lote é a do construtor (Regra 213) | 11-05 |
| 202 | **Revogada** (autor 11-06, ADR-047). A obra pedida não usa a prova por seção | 11-05 |
| 203 | **Revogada** (autor 11-06, ADR-047). A água entra pelo construtor | 11-05 |
| 204 | **Revogada** (autor 11-06, ADR-047). A peça da obra é posta pelo construtor | 11-05 |
| 205 | **Revogada** (autor 11-06, ADR-047). A obra pedida não reserva região e não falha | 11-05 |
| 206 | **As pontas do grafo são os lados da vila** (autor 11-05, emendada 11-10): fica a ponta mais distante do centro em cada lado, norte, sul, leste e oeste (Regra 229), no máximo 4, e só ela é azul e cresce. As outras perdem o trecho morto até o encontro ou a entrada; o caminho a até 3 blocos da linha volta ao solo (Regra 186). Emenda a Regra 177. | 11-05 |
| 207 | **Toda porta térrea é entrada** (autor 11-05): o marcador branco fica no primeiro bloco em frente à porta, mesmo dentro da caixa ou sobre degrau (Regra 223); porta de dentro não conta e porta dupla conta uma vez. O ramal da Regra 177 cobre o que houver entre ele e a rua. Emenda a Regra 178. | 11-05 |
| 208 | **Plantação sem anel** (autor 11-06): o chão em volta da plantação não é pedido como caminho, e ela tem uma entrada só, a quina da Regra 184, com ramal até a rua. O caminho já colado nela sai do grafo quando a rede continua inteira sem ele e volta ao solo pelo pedreiro, a qualquer distância da linha; encontro, entrada, centro e o trecho que é a única ligação ficam. Emenda a Regra 183. | 11-06 |
| 209 | **Porta de quintal fechado** (autor 11-06): a caixa da peça pode incluir o quintal. Porta cuja saída para em barreira dentro da caixa, como cerca, portão ou rebolo, só é entrada se nenhuma outra porta da casa sair, e então onde o quintal abre para fora, ou no primeiro chão depois dela se ele for fechado (Regra 223); assoalho de tábua não é quintal. Emenda a Regra 207. | 11-06 |
| 210 | **Toda estrutura onde se entra tem entrada** (autor 11-06, 11-07): toda estrutura da vila gerada e do mod entra no grafo com o marcador branco de porta, sem porta nova no mundo, menos os enfeites da Regra 223. Porta, portão; sem eles, o lado com a maior parte da borda aberta na altura de quem anda, ou o lado do centro da vila se todos estão fechados. O portão da mina é lido do mundo e tem a entrada do lado de onde se chega, fora da folga da escada. Rua, ponta de rua, árvore, enfeite e peça que só põe bicho não são estrutura; plantação segue a Regra 184. Emenda a Regra 207. Emendada pela Regra 256: arco de pinheiro, lido pela boca da mina. | 11-06 |
| 211 | **Casa do mineiro na boca em uso** (autor 11-06): a casa do mineiro fica ao lado da boca da superfície da mina de hoje, e uma casa pronta basta (Regra 266). A cama vai para o mineiro pela Regra 74. Emenda a Regra 175. | 11-06 |
| 212 | **Sem tocha sozinha, vela não trava obra** (autor 11-06): nenhum aldeão põe tocha da colônia pelo chão da vila; a que já está lá fica. Vela e tocha soltas não recusam o lote e saem quando a obra abre, menos a tocha das almas (Regra 108) e a de dentro da caixa de uma estrutura. Emenda a Regra 127. O poste de luz da Regra 261 é obra, e não tocha solta. | 11-06 |
| 213 | **A obra MAE é a próxima do construtor** (autor 11-06, ADR-047): a obra pedida entra na fila do construtor, antes do reparo e da busca de lote, e é levantada por ele, bloco a bloco, com material dos baús. O lote passa pela régua do construtor: caixa da busca, trava da base e volume livre. O mundo vivo não a para. Ela não deixa blocos soltos: não é largada por distância nem por espera, volta para a fila se sumir sem terminar e procura outro lote perto se o dela não abrir. Só o jogador a encerra, com `/vc mae parar` ou a tocha das almas (Regra 108) | 11-06 |
| 214 | **Todo caminho é do grafo, em toda vila** (autor 11-06, ADR-048): quem quer caminho só pede a coluna, e quem calça é o aldeão. Toda entrada aparece no grafo na hora, branca, mesmo antes de ter caminho; o nó central é sempre da rede. O caminho traçado não é esquecido porque uma releitura deixou de pedi-lo: sai calçado, recusado ou depois de 60 releituras sem ser pedido. Sem saída a seco, a ligação cruza até o limite da ponte. Mina sem portão entra pela borda da folga da escada, do lado do centro | 11-06 |
| 215 | **O pedreiro ocioso ajuda no caminho e no chão** (autor 11-06): sem tarefa do ofício, ele calça a rede no rodízio com o construtor e nivela o chão da vila (Regras 156, 178 e 186). É a função secundária dele e exceção à regra temporária de uma profissão não fazer atividade de outra | 11-06 |
| 216 | **A rede é o grafo** (autor 11-06, ADR-048): quem pergunta por onde se anda, o que está perto da rua ou onde procurar lote ouve as linhas do grafo. O índice físico continua como memória de cada coluna de caminho: é o que a releitura lê, o que o chão não nivela e o que o pedreiro sustenta. Antes da primeira releitura, ele responde. | 11-06 |
| 217 | **A ceia alimenta com o trigo de qualquer baú** (autor 11-06): com fazendeiro na vila, a ceia assa o trigo guardado em qualquer baú da vila, até o que os adultos sem comida precisam, e deixa a reserva crua do rebanho. Sem isso a colheita ficava em baús alheios, a vila parecia feliz e só 3 a 5 adultos por dia ficavam dispostos a procriar. Candidata | 11-06 |
| 218 | **A vila continua construindo** (autor 11-06, muda a decisão de 10-12 de não repetir): com um de cada tipo de pé, cada 5 adultos dão direito a mais uma obra repetida, sempre o tipo que a vila tem menos. Roça e casa de ofício têm conta própria. O rodízio aceita até 6 camas além dos adultos (eram 4). Sem nada a levantar, o motivo é "a vila tem o que precisa por enquanto". Candidata | 11-06 |
| 219 | **Rotina à vista de quem está sem tarefa** (autor 11-06): no expediente, depois de um minuto sem tarefa, o aldeão vai à bancada, confere o baú, procura um vizinho e anda pela rua, uma coisa de cada vez, a até 32 blocos e dentro da caixa da vila. É aparência: esse tempo continua contando como ócio. Candidata | 11-06 |
| 220 | **Quem não tem ofício trabalha na vila** (autor 11-06, exceção à regra temporária de 11-02): além de carregar (O8) e recolher o material solto da obra (Regra 48), o adulto sem ofício calça a rede no rodízio com construtor e pedreiro, e tira a neve. Ele não vira profissão nenhuma. Candidata | 11-06 |
| 221 | **A vila gasta o que produz** (autor 11-06): depois da ceia, cada adulto come 1 ponto de comida do que sobrou. A ferramenta que a colônia entrega dura 3 dias, e a renovada custa 2 tábuas e 3 pedras; sem material ela fica no último ponto e o trabalho segue. Sem tarefa, carpinteiro e pedreiro visitam cada construção uma vez por dia e gastam 4 tábuas ou pedras; com obra aberta ou menos de 36 no estoque, não gastam. Candidata | 11-06 |
| 222 | **O socorro do MAE** (autor 11-06, ADR-049): depois de 2 voltas inteiras sem lote para a próxima obra, e sem a rua abrir um, a vila procura sozinha um lote a até 24 blocos da borda da caixa, um lado por vez, e pede a obra na fila do construtor (Regra 213), com a porta virada para a vila. Só avisa no chat, sem esperar aprovação; cancela-se com `/vc mae parar` ou a tocha das almas. Lote que toca água é recusado, e roça fica de fora. Candidata | 11-06 |
| 223 | **Branco só onde se entra** (autor 11-07, desenho sobre o jogo da 0.4.42): o marcador fica colado na porta, e a saída da caixa é só o caminho até ele. Poste, poço e chafariz não têm entrada, e nada dentro de curral fechado tem. Se um caminho já leva da porta para fora da caixa, a linha segue por ele. O ramal que perdeu a entrada sai do grafo e do chão, a qualquer distância, até 32 colunas; trecho maior é rua e fica. Degrau e assoalho em frente à porta levam o branco, mas não viram caminho. A rua não forma quadrado de nós; encontros de ruas diferentes podem ficar lado a lado. Candidata | 11-07 |
| 224 | **Todo caminho conta e liga em reta** (autor 11-10, ADR-050): o grafo lê o caminho da caixa e o de até 32 blocos fora dela que encosta no de dentro. Acima de 6.000 colunas ficam as mais perto do centro, e o corte vai para o log. Toda parte solta liga antes em reta, com uma dobra só, ao ponto mais perto da linha, até 64 blocos; todas na mesma releitura. Candidata | 11-10 |
| 225 | **O trecho que ficou trava** (autor 11-10, ADR-050): coluna que passa 10 dias de jogo na linha sem sair fica fixa; a releitura não a move, e o resto do traçado se ajeita em volta. Some o caminho dela no mundo, ou ela sai da linha, e a conta recomeça. Candidata | 11-10 |
| 226 | **Socorro da vila inteira, em bairro** (autor 11-10, ADR-051): só a volta sem lote da casa conta para o socorro da Regra 222; a oficina sem lote passa a vez. A obra seguinte do socorro fica do mesmo lado da anterior e o mais perto dela, formando um bairro. `/vc mae obra` sem lote diz cada motivo e quantos lotes recusou. Emenda a Regra 222. Candidata | 11-10 |
| 227 | **Água interna sob a obra pedida** (autor 11-10, ADR-051): a obra do jogador aterra a água da zona interna da vila, ligada a no máximo 512 colunas e toda dentro da caixa; mar e rio aberto recusam o lote (`open_water`). A obra automática não aterra água. Candidata | 11-10 |
| 228 | **Ruína de save antigo sai** (autor 11-10, ADR-051): a obra MAE que uma versão anterior deixou e que não volta para a fila tem a pegada limpa; sai só o bloco igual ao da planta. Com chunk descarregado, espera. Candidata | 11-10 |
| 229 | **As pontas levam a vila para fora** (autor 11-10, ADR-052): uma ponta azul por lado da caixa, norte, sul, leste e oeste, a mais distante do centro naquele lado. Por dia de jogo, cada uma avança 1 bloco para fora pelo seu lado, e o construtor calça; até 128 blocos além da vila gerada. Ramal que termina numa entrada retirada não é ponta. Emenda as Regras 185 e 206. Candidata | 11-10 |
| 230 | **A zona alcança a ponta** (autor 11-10, ADR-052): a ponta calçada que sai 7 blocos ou mais da caixa da vila leva a caixa até ela. Candidata | 11-10 |
| 231 | **Caminho sob laje e ponta na extremidade** (autor 11-10, sobre o jogo da 0.4.47): o grafo lê o caminho coberto por laje, telhado ou degrau, a até 8 blocos abaixo do topo da coluna, quando restam 2 blocos livres em cima; com menos, ele vira solo (emenda a Regra 180). A ponta azul fica só na extremidade do grafo inteiro no lado dela, a até 8 blocos da coluna mais externa, e a que aponta para fora vem antes; ponta entre linhas, no meio da vila, sai com o trecho morto. Emenda a Regra 229. Candidata | 11-10 |
| 232 | **A obra manda no artesão** (autor 11-10, sobre a análise `docs/audit/2026-11-10-qualidade-das-profissoes.md`): com a obra aberta parada esperando material, nenhuma ordem de estoque de enfeite abre, e a que está em curso cede a bancada. O item de estoque que não se consegue fazer sai do rodízio por uma hora de jogo. O lenhador que anda até a árvore longe, chegando mais perto, não larga a tarefa por travamento. O diário grava o produto feito. Candidata | 11-10 |
| 233 | **A vaga segue a falta** (autor 11-10): com comida em falta, o fazendeiro vem primeiro na lista de contratação; com 1.024 pedregulhos ou mais guardados, o mineiro sobra e não ganha vaga nova. Candidata | 11-10 |
| 234 | **O trecho travado sai verde** (autor 11-11): no sinalizador dos caminhos (`/vc mae mostrar ligar`), o passo da linha entre duas colunas já travadas pela Regra 225 é verde, coluna a coluna; o resto segue amarelo. Só desenho: o trecho travado continua na releitura. Candidata | 11-11 |
| 235 | **Pastor pelo rebanho** (autor 11-11, opção P1 de `docs/audit/2026-11-11-atividade-das-profissoes.md`): o pastor só ganha vaga nova com 16 animais da vila (ovelha, vaca, porco e galinha dentro da caixa) por pastor; sempre há vaga para 1. Quem já é pastor fica. Candidata | 11-11 |
| 236 | **Pedreira de superfície no deserto** (autor 11-11, opção M1): na vila de deserto, o mineiro sem ramal livre e sem nível a alargar tira areia e arenito da superfície, fora da caixa e a até 32 blocos dela. Só raspa a coluna que não fica abaixo das vizinhas, uma camada por vez, até 4 blocos abaixo do centro, longe da água, de caminho, da vila gerada e do que a colônia construiu. Exceção à Regra 53, só no deserto. Candidata | 11-11 |
| 237 | **Composteira pela roça** (autor 11-11, opção F2): o fazendeiro sem colheita nem canteiro composta a semente acima de uma por canteiro conhecido da roça, e nunca menos de 8; a farinha de osso vai na lavoura verde. Antes a reserva fixa de 32 não sobrava na roça pequena. Candidata | 11-11 |
| 238 | **Rebanho cresce com comida sobrando** (autor 11-11, opção P2): com 32 ou mais da comida da espécie nos baús, o pastor procria até 16 adultos de cada espécie, e não só até 8. Acima de 8 só serve a comida do fazendeiro; a ração que surge sozinha (Regra 117) não passa de 8. O abate acima de 6 segue como está. Candidata | 11-11 |
| 239 | **Todo animal que procria com comida** (autor 11-11, opção P4): o pastor cria ovelha, vaca, porco, galinha, coelho, vaca-cogumelo, cabra, camelo, raposa, panda, tartaruga, abelha, sapo, farejador e tatu, com a comida que cada um aceita no jogo. Cavalo, burro, lhama, lobo e gato pedem doma, e os do Nether não vivem na vila. Só a criação de corte (as quatro de antes, coelho, vaca-cogumelo e cabra) vai ao curral; o abate acima de 6 vale para as que dão carne. No deserto, a fundação solta também 6 coelhos. Candidata | 11-11 |
| 240 | **Lã guardada vira peça de lã** (autor 11-11, opção P5): o pastor sem tarefa, com mais de 16 lãs nos baús, faz tapete (2 lãs dão 3) e tinge lã branca com o corante que a vila tiver; a cor de cada lote é sorteada entre as que dá para fazer, cada peça para em 16 e a lã nunca desce de 16. Sem corante, só a cor que o rebanho deu. Candidata | 11-11 |
| 241 | **Plantação grande do deserto** (autor 11-11): a plantação do mod tem 16x16, borda de tora como a roça da planície e 192 terras aradas com 4 buracos de água, todas a até 4 blocos da água e molhadas (`scripts/gen_large_farm.py`). No deserto, quando o fazendeiro não acha colheita, canteiro nem semente para compostar e passa mais de 70% do expediente sem trabalhar, ela entra como obra urgente, uma por vila. Ele semeia a lavoura que a vila tem menos, então planta um pouco de cada semente de lavoura que houver (trigo, beterraba, cenoura, batata, flor-tocha); melancia, abóbora e jarro ficam de fora até o fazendeiro saber colher fruto de caule. A borda e o miolo tomam o lugar da areia e da terra, como a base da roça da vila (0.4.54). Candidata | 11-11 |
| 242 | **Quem arruma o chão** (autor 11-11): nenhum bloco de caminho muda sozinho. Pedreiro, carpinteiro e fundidor sem tarefa do ofício calçam a linha do grafo com o construtor, devolvem ao solo do bioma o caminho de dentro da caixa sem nenhuma marca do grafo e o caminho coberto (Regra 180), tapam buraco e nivelam o vão de até 8 blocos entre duas construções em rampa de uma base à outra, cortando ou aterrando até 4 por coluna; a rampa fica fora do anel de 2 blocos de cada construção, que segue a base dela (0.4.53). Emenda as Regras 178, 186 e 215. Candidata | 11-11 |
| 243 | **Baú de qualquer lugar** (autor 11-11, opção T4 de `docs/audit/2026-11-11-jogo-0453.md`): todo aldeão guarda e tira de qualquer baú da colônia de onde estiver, sem andar até ele. O baú do trabalhador e o da boca da mina contam mesmo fora da caixa da vila; o baú livre segue sendo o de dentro da caixa (Regra 45). Baú em chunk descarregado fica de fora até o chunk carregar. Emenda a Regra 45. Candidata | 11-11 |
| 244 | **Fora da simulação não congela** (autor 11-11, opção T1c de `docs/audit/2026-11-11-jogo-0453.md`): o aldeão em chunk carregado que não simula entidades não pode andar, então o guarda de imobilidade não o conta e ele não vira encalhado; a fuga de quem já está preso tenta os outros jeitos quando ele volta à simulação. Candidata | 11-11 |
| 245 | **Cama longe vem para perto** (autor 11-11, opção T3b): na troca de camas do amanhecer (Regra 74), o aldeão de ofício sem trabalho medido cuja cama está a mais de 128 blocos do centro, na horizontal, conta o centro como trabalho e muda para a cama livre mais perto dele. Vale para quem estiver carregado no amanhecer. Emenda a Regra 74. Candidata | 11-11 |
| 246 | **Arrumar o chão é trabalho** (autor 11-11, opção O2a): quem nivela, tapa buraco ou devolve ao solo o caminho da vila (Regras 215 e 242) conta como trabalhando no `WorkTime` até a passada seguinte, um minuto depois; o `time_ledger` passa a ver esse trabalho. Candidata | 11-11 |
| 247 | **Construtor sem obra arruma o chão** (autor 11-11, opção O1b): o construtor sem obra aberta, uma vez por minuto no expediente, nivela e tapa o chão da vila como o pedreiro (Regras 215 e 242), começando pelo lote da obra aberta. Exceção do autor à regra temporária de uma profissão não fazer atividade de outra. Candidata | 11-11 |
| 248 | **Um terço da madeira fica em tora** (autor 11-11, opção O2c): o carpinteiro converte em tábua até a madeira da vila, tora e tábua contadas na mesma moeda, ficar com um terço em tora, e não mais metade. A obra continua convertendo além da reserva o que pede (Emenda 1 da Regra 5). Emenda a Regra 5. Candidata | 11-11 |
| 249 | **Fundição sem cru chama o pedreiro** (autor 11-11, opção O2b-1): a cada ciclo, a tarefa de fundir aberta sem o cru dela em nenhum baú pede o cru ao pedreiro, que o faz pela receita com o que há nos baús (arenito de quatro areias); antes o pedido só saía da mão do fundidor, que a trava do cru não deixava pegar a tarefa. Mantém a reserva de peças da vila do deserto (arenito liso) andando. Candidata | 11-11 |
| 250 | **A rua nasce no chão novo** (autor 11-11, opção A): na geração, a coluna que o solo da vila corta ou aterra leva junto a altura de geração (`WORLD_SURFACE_WG`, `OCEAN_FLOOR_WG`), que o chunk em geração não acompanha sozinho; a rua do Vanilla segue essa altura e antes nascia na altura de antes do corte, no ar ou enterrada (21% das colunas de caminho no jogo da 0.4.56). Vale para chunks gerados daqui em diante. Candidata | 11-11 |
| 251 | **Flor e muda não seguram a coluna** (autor 12-11, opção A): na geração, a varredura do topo do solo da vila passa por flor e muda, como já passava por folha e tronco; antes a flor virava o "topo", o corte recusava a coluna e ela ficava de pé, e a rua do Vanilla nascia sobre ela. Candidata | 12-11 |
| 252 | **Acessório é enfeite** (autor 12-11, opção A): peça `accessory` da vila gerada (`plains_accessory_1`) não recebe entrada nem nó no grafo, como poste, poço e chafariz (Regra 223). Candidata | 12-11 |
| 253 | **O curral leva terra no solo** (autor 12-11): o piso de grama da planta de curral sobre pedra do terreno não conta como chão já pronto (Regra 5-d, 10-03): o construtor troca a pedra natural pela terra da planta, sem gastar recurso, e o reparo refaz o curral já construído sobre pedra. No jogo da 0.4.57, o curral grande tinha 15 de pedra e 3 de terra comum de 169 células de piso. Casa e outras plantas seguem a regra do chão já pronto. Candidata | 12-11 |
| 254 | **Patamar nas dobras da escada da mina** (autor 12-11, ADR-053): cada dobra do caracol ganha um patamar plano de 3 x 3 na altura do último degrau, com uma tocha na parede de fora, presa em rocha que nenhum nível cava. Só nos níveis novos: o que já está sendo cavado termina sem patamar. O patamar é cavado no fim do ramal; o nível passa de 1.755 para 2.025 blocos. Emenda a Regra 119. Candidata | 12-11 |
| 255 | **Animal na corda vai para o curral** (autor 12-11, opções A e B): com curral na vila, o pastor nunca amarra animal em cerca ou poste; o animal espera a próxima tentativa, e o log diz por que o curral não serviu (cheio, portão sem chão livre dos dois lados, ou sem caminho). O animal já amarrado é recolhido antes do solto. No jogo da 0.4.57 o pastor amarrou 17 animais com o curral grande na vila, e 8 ficaram presos. Emenda a Regra 196. Candidata | 12-11 |
| 256 | **O arco da mina e a árvore com folha** (autor 12-11, opções 1 e 2): a boca da mina ganha um arco de tora descascada (pinheiro no arquivo; a madeira da vila pela Regra 262) — dois pilares e uma viga sobre o vão de 3 x 4, um degrau em cada ponta da viga com um lampião pendurado, e um baú no chão do lado de fora, à direita de quem entra (`mine_arch.nbt`). O arco surge sozinho, sem profissão nem material. O portão de pedra de 10-29 (`mine_entrance.nbt`) não é mais erguido, mas segue reconhecido e protegido onde está de pé. O grafo reconhece o arco pelas peças na boca da mina da colônia, e não só pelo topo. O lenhador só corta tronco com folha natural perto do alto: no jogo da 0.4.57 ele levou o topo do portão ("1 logs and 0 leaves") e o grafo perdeu a entrada. Emenda a Regra 210. Candidata | 12-11 |
| 257 | **Areia e argila sem reserva de cru** (autor 12-11, solução 3 da reanálise dos logs): areia, argila e bola de argila vão à fornalha inteiras, sem a metade guardada que segue valendo para os outros crus, menos o que a obra aberta ainda vai pôr. Nos jogos de 11-07 e 11-08 o fundidor largou 58 pedidos "held by the reserve". Candidata | 12-11 |
| 258 | **O adiantamento lê todas as estruturas da vila** (autor 12-11, solução 2 da reanálise dos logs): as filas do carpinteiro, do pedreiro e do fundidor tiram peças das casas do jogo e também das estruturas da colônia — casas de ofício, armazém, casa do mineiro, curral grande e plantação grande. Cada peça segue no teto de 64. Nas vilas maduras cada peça das casas batia 64 e o artesão ficava 54–93% ocioso. Candidata | 12-11 |
| 259 | **Um aldeão de cada profissão na vila nova** (autor 12-11): a fundação conta os adultos da vila e faz nascer, ao lado da BigHouseMOD, os que faltam para os oito ofícios (construtor, carpinteiro, fazendeiro, lenhador, pedreiro, mineiro, pastor, fundidor). Os sete moradores seguem com cama na casa; quem nasce a mais fica sem cama, que vem da obra. Só vila nova. Candidata | 12-11 |
| 260 | **Casa do mineiro na fundação** (autor 12-11): quando a BigHouseMOD acaba de ser posta, a fundação escolhe a boca da mina pela mesma regra do mineiro (na borda da caixa, Regra 53; longe da água), abre a mina, ergue o arco e põe a casa do mineiro (Regra 175) pronta no lote ao lado da boca, sem obra e sem material, registrada como construção terminada. Sem boca ou sem lote, nada é posto e a casa segue como obra. Só vila nova. Candidata | 12-11 |
| 261 | **Poste de luz nas ruas fixas** (autor 12-11, opção B): base de 1 bloco de pedra do bioma (arenito no deserto), duas cercas da madeira do bioma e a tocha da colônia no topo. É obra do construtor, com material dos baús, e só abre quando a busca de lote termina sem obra (nada a construir ou nenhum lote): a casa nunca atrasa pelo poste. Só depois de existir linha fixa do grafo (Regra 225), no bloco ao lado dela, na linha do solo, da coluna fixa mais perto do centro até a ponta; nenhuma estrutura a 2 blocos em volta (construção, obra, peça da vila gerada), céu aberto acima da base (folha bloqueia) e 30 blocos ou mais de outro poste: tocha da colônia de pé, poste do mod e poste da vila gerada. O poste é enfeite, sem entrada no grafo (Regras 223 e 252); quebrado, o reparo refaz. Emenda a Regra 212: o poste é obra, não tocha solta. Candidata | 12-11 |
| 262 | **Cada estrutura do mod na versão do bioma** (autor 12-11, troca automática): ao erguer uma estrutura do mod (arco da mina, curral pequeno e grande, plantação grande, celeiro, armazém e casa do mineiro sem arquivo do estilo, casas do mod), toda madeira do arquivo vira a madeira da vila na mesma forma (tora, tábua, laje, cerca, porteira, porta, alçapão) e o pedregulho ou o arenito vira a pedra da paleta das casas (arenito no deserto), também na mesma forma (bloco, laje, degrau, muro). O resto do arquivo fica igual. O bioma é o da vila, o mesmo que escolhe as casas; o arco usa o da vila em volta da boca. Um arquivo serve todos os biomas; onde há arquivo do estilo (`colony/<estilo>/`), ele vale como está. Estrutura já de pé não muda: a peça de outra madeira ou pedra faz o mesmo papel na conferência (arco reconhecido, peça de pé no reparo e na retomada, peça da colônia), e só a peça que falta sai na madeira da vila. Emenda a Regra 256. Candidata | 12-11 |
| 263 | **O fundidor funde onde está** (autor 13-11, jogo da 0.4.68): a fundição não exige que o fundidor esteja dentro de construção registrada da colônia; baú numa casa da vila Vanilla original funde como qualquer outro. Candidata | 13-11 |
| 264 | **O grafo nunca passa sobre a água** (autor 13-11, jogo da 0.4.68): nenhuma linha do grafo de ruas é traçada sobre a água, nem como ponte; sem volta a seco, a parte fica sem ligação. O trecho planejado que ficou sobre a água sai na releitura, e o calçador recusa coluna sobre a água. Revoga a Regra 174 e emenda a Regra 123. Candidata | 13-11 |
| 265 | **O grafo não atravessa estrutura** (autor 13-11, jogo da 0.4.68, opção "parede e casas Vanilla"): a linha não passa por coluna de construção da colônia, obra aberta ou casa da vila Vanilla (parede, piso, móvel e cerca até 2 blocos acima do chão). O trecho planejado que uma construção cobriu depois sai na releitura (feito na 0.4.70, junto da Regra 264). Sob laje e telhado continua valendo a Regra 231. Candidata | 13-11 |
| 266 | **Uma casa do mineiro basta** (autor 13-11, jogo da 0.4.68): com uma casa do mineiro pronta na vila, nenhuma outra é procurada, mesmo que a boca da mina mude ou haja mais mineiros. Emenda a Regra 211. Candidata | 13-11 |
| 267 | **Plantação grande em todo bioma** (autor 13-11, jogo da 0.4.68: fazendeiro da planície com 0% de trabalho): a plantação grande da Regra 241 entra como obra urgente em qualquer bioma, nas mesmas condições (fazendeiro sem colheita, canteiro nem semente para compostar, e mais de 70% do expediente sem trabalhar), uma por vila. Emenda a Regra 241. Candidata | 13-11 |
| 268 | **O bosque acha o chão em qualquer altura** (autor 13-11, jogo da 0.4.68: 0 mudas plantadas, lenhador 67% andando): a borda do bosque do lenhador (4 a 6 blocos fora da caixa) lê o chão pelo topo do terreno, qualquer que seja a altura, e não só de 4 acima a 8 abaixo do centro da vila. Água continua recusada. Candidata | 13-11 |
| 269 | **O prazo do pastor pela distância** (autor 13-11, jogo da 0.4.68: o curral teve no máximo 4 animais e o pastor largou o animal 5 vezes no caminho): a coleta dura 2.400 tiques mais 20 por bloco de caminho (do pastor ao animal e do animal ao curral ou ao poste), medido quando ela começa; se ainda assim acabar sem chegar, o animal que estava na corda fica amarrado na cerca mais perto, e só sem cerca ao alcance fica solto. Candidata | 13-11 |

Das 75 às 109 são decisões do autor que já estavam no código sem número; foram registradas em 2026-10-22
(levantamento em `docs/research/2026-10-22-logicas-candidatas-a-regra.md`; a 108 e a 109 vieram do README).

---

### Regras provisórias

Regras que o autor declarou **temporárias** e vão sair.

| # | Enunciado | Por que existe | O que destrava a saída |
|---|---|---|---|
| 28 | ~~Barreira de teste~~ (**retirada em 10-06**, ADR-036 item 6): ~~só `plains_small_house_1`~~ (caiu em 09-09) e peça dispensada | Tornar as sessões comparáveis entre si | O planejador saber desistir de um objetivo (P1.1) |

**Enquanto a metade que resta valer:**
- A casa sobe com peças da barreira (playtest de 02-10: 16 de 69).
- `TEST BARRIER covered for N of M pieces` é a linha que mede.

---

### Regras emendadas

| # | Emenda | O que mudou |
|---|---|---|
| 27 | Emenda 1 (08-26) | Abriu substituição **só para pedra** na parede |
| 27 | Emenda 2 (08-26) | Estendeu para **madeira, tábua e pedra** |
| 3 | Emenda 1 (09-14) | A árvore não é exceção: o lenhador não corta bloco que pertença a estrutura protegida; consulta no plano e em cada quebra |
| 30 | Emenda 1 (09-14) | A colônia mantém 64 carvão e 64 minério de ferro bruto; demanda de obra soma ao piso |
| 1, 5 | Emenda (10-11) | **Teto geral do estoque:** nenhuma meta passa de 64 para bloco fabricado (bancada ou fornalha) nem de 256 para bloco cru, a não ser o que a obra aberta ainda pede (`StockCeiling`). Artesão sem tarefa ajuda a obra (`BuildHelper`) |
| 7 | Emenda (10-11) | A árvore cortada longe do bosque não é replantada lá: a muda vai para o bosque perto do baú do lenhador, que cresce até o dobro (`VillageGrove.takesTheSapling`) |
| 31 | Emenda (10-11) | Com nada maduro e nenhum canteiro vazio, o fazendeiro ara a terra **ao lado da roça existente**, com água a até 4 blocos, longe da rua e de obra, com semente nos baús (`FarmerTill`). Arar em qualquer lugar continua proibido |
| 31 | Emenda (10-28) | O canteiro novo é um **retângulo 9×9** em chão plano, ao lado da fazenda com 1 bloco de folga e alinhado a ela, com a **água na linha do meio** (posta quando ele abre); nunca por cima de fazenda que existe, de rua ou de obra; um canteiro aberto por colônia, até 2 por fazendeiro (`FarmPlots`, autor 10-28, jogo da 0.3.104: a roça crescia em manchas). As manchas aradas antes ficam |
| 26 | Emenda (10-11) | **Divisão por material:** madeira → carpinteiro; pedra, barro, terracota, fornalha → pedreiro; metal, vidro, gelo, neve → fundidor; tecido e corante → pastor; papel, livro, farinha de osso, feno → fazendeiro (`CraftFamily`). Carpinteiro e fundidor guardam 8 de cada item das plantas (`ItemStock`); flores, grama, bambu, cana (fazendeiro) e gelo, neve (fundidor) têm coleta (`PlantPatch`) |
| 51 | Emenda (10-11) | A peça que surge no baú surge **na quantidade toda** que a obra ainda pede (`BiomeConstructionSupply.askedByTheBuild`) |
| — | Decisão (10-11) | **Troca de ofícios desligada:** ninguém troca de ofício por ócio nem por desistência; a Regra 51 (15%) fica só como medida |
| 43 | Emenda (10-11; lotação trocada em 10-22 pela Regra 72) | **Pastor:** cercado com lugar primeiro (~~≤ 5 animais~~ um a cada 4 blocos de chão, Regra 72; cheio, outro cercado), pelo portão; sem cercado, a cerca mais perto; sem cerca, um poste novo ao lado da casa, com cerca do baú (`ShepherdPens`) |
| 14 | Emenda (10-11, 0.3.32) | A vaga do ajudante não é a tarefa da obra: com o construtor fora, o planejador abre outra para ele (`ConstructionPlanner.ensureTask`) |
| 51 | Emenda (10-11, 0.3.32) | ~~O fazendeiro com a roça descansando ajuda a obra~~ — **revogada em 13-10 pela Regra 59**: fazendeiro e pastor não ajudam a obra |
| 1, 5 | Emenda (10-11, 0.3.32) | Sem obra pedindo, a tora só vira tábua até o teto de 64 tábuas (`StockRules.logsToConvert`) |
| 51 | Emenda (10-11, 0.3.32) | Antes de surgir, o que a vila já tem em outro baú é **levado** ao baú da profissão (`ChestTransfer`); só surge o que não há em baú nenhum |
| 14 | Emenda (10-11, 0.3.32) | Obra que só tem até 4 peças sem apoio, depois de duas janelas de paciência, larga essas peças e fecha (`UnsupportedRelease`, linha `VC_UNSUPPORTED`) |
| 14 | Emenda (10-11, 0.3.32) | A obra que volta do save volta no giro em que foi levantada: das quatro orientações, a que mais casa com o que está de pé (`BlueprintPlacement.resumedBlueprintOf`) |
| 26 | Emenda (10-12, 0.3.33) | A cadeia da obra desce a receita também da peça sem recurso declarado (tijolos → tijolo assado → argila), até 3 degraus (`WorkMaterialRecipeNeeds`) |
| 7 | Emenda 2 (10-11) | O lenhador corta a árvore **mais perto dele** dentro do alcance da vila (a ordem centro → borda de 10-08 saiu) |
| 14 | Emenda (10-11) | **O construtor sempre começa a obra**, podendo receber ajuda ou não: a tarefa que o planejador abre é só dele; ajudante entra pela vaga do `BuildHelper`, a até 32 blocos (`HelperReach.mayStart`) |
| 29 | Emenda (10-11) | Aberta a escada, os ramais 1 a 3 abrem já e pulam a sala comum, que fica com o ramal zero (`Mine.skipTheSharedHall`) |

**As emendas nasceram do mesmo defeito:** a conta e o construtor
discordavam. A conta aceitava o substituto, a parede exigia o exato.
Duas correções foram feitas em 08-22 (a conta deixou de aceitar) e em
08-26 (a parede passou a aceitar). **A lição:** a conta e o consumidor
precisam **concordar**.

---

### Regras revogadas

A regra de arquitetura "a colônia não cria recurso" (`Construction-System.md`,
sem número nesta tabela) foi **retirada em 2026-09-30** — ADR-028. A 25 foi
**desfeita em 2026-10-06** — ADR-036 item 5. A 52 (uma roça por fazendeiro, 10-08) foi **substituída pela 67**
em 2026-10-13. Continua dela a posse: cada fazendeiro sem roça recebe a roça
sem dono mais perto do baú dele (`FarmOwners`, `VillageFarms`).

---

### Decididas em 2026-10-02 (estudo de profissões)

Do `docs/research/2026-10-02-melhorias-profissoes-vida-natural.md`, o autor:
recusou a pausa e a chuva (18-e1); trocou a ferramenta fixa pela melhor do baú
(2-e1, 37-e1); fixou que a fome não pesa no trabalho (46) e que ninguém envelhece
(47); limitou o recolher do chão ao que a obra espera (48); deu a ordem das obras
(49); e fez do tempo dos aldeões o critério de toda verificação (50).

---

### Regras que faltam decidir

Nenhuma. A última, P0.7 (pedra como solo de lote), foi aceita pelo autor em 07-10 (ADR-039 F1):
o código já tratava todo bloco sólido como solo desde 15-09 (`LotGround.isLotGround`).

<!-- REGRAS:FIM -->

## Blocos do mod

Todos os blocos aparecem no criativo (bancadas em "Blocos funcionais", ao lado
das estações Vanilla) e têm receita para o jogador fabricar. Quebrados, devolvem
o próprio bloco.

### Bancadas de ofício

Cada bancada é a estação de trabalho de uma profissão: atrai o aldeão para o
ofício, funciona para o jogador como a estação Vanilla equivalente, ilumina
como uma tocha (luz 14) e **dobra a produção** do titular que trabalha perto
dela. Receita de todas: ouro, diamante e ouro em cima; a estação Vanilla do
ofício no meio; ouro nos dois cantos de baixo (4 lingotes de ouro e 1 diamante).

Só por estar na vila, cada bancada liga **passivas** (Regra 132). Shift+clique
em qualquer uma abre no chat o **painel** do ofício: os titulares, o estado de
cada um na cor do nome, a tarefa, há quanto tempo esperam, a obra aberta (no
construtor) e as passivas ligadas.

| Bloco | Estação na receita | Para o jogador funciona como | Passivas na vila (Regra 132) |
|---|---|---|---|
| Bancada do Carpinteiro | mesa de trabalho | mesa de trabalho | ferramenta e arma que o jogador fabrica na vila saem com 1 encantamento sorteado; repara aos poucos a ferramenta de quem está a até 8 blocos |
| Cortador do Pedreiro | cortador de pedra | cortador de pedra | tapa um buraco de rua por minuto a até 32 blocos; a cada 4 pedregulhos minerados, 1 a mais |
| Forja do Ferreiro (fundidor) | alto-forno | fornalha de verdade | fornalhas, alto-fornos e defumadores a até 32 (também os do jogador) com o dobro da velocidade; restaura armadura a até 8; armadura fabricada na vila sai encantada |
| Bancada do Lenhador | mesa de trabalho | mesa de trabalho | mudas a até 32 blocos crescem mais rápido |
| Bancada do Fazendeiro | composteira | composteira | a roça a até 24 blocos ganha um passo de crescimento; a sobra dos baús (sementes acima da reserva, folhas) vira farinha de osso |
| Bancada do Pastor | tear | tear | abaixo da meta, um par cruza sozinho a cada 10 min (a até 36); lã volta mais rápido; o que os animais derrubam vai para os baús; o abate rende o dobro de carne e couro |
| Bancada do Construtor | andaime | mesa de trabalho | a obra a até 48 blocos assenta 1 bloco a mais por minuto; o material da obra guardado em outros baús vem para o baú do construtor |
| Bancada do Mineiro | cortador de pedra | mesa de trabalho | nenhum monstro nasce na mina (até 64 blocos da entrada, abaixo dela); ferro, cobre, carvão, ouro, lápis e redstone rendem o dobro; diamante, esmeralda, ametista e quartzo rendem +1; 1% de chance de soltar uma ferramenta de ferro, ouro ou diamante |

### Outros blocos

| Bloco | Como se obtém | O que faz |
|---|---|---|
| Tocha da Colônia | 4 tochas em volta de 1 lingote de ouro → 4 tochas | Mesmo formato da tocha do jogo, com cabeça de chama laranja de 3 pixels e o centro claro, sobre o cabo marrom; vai no chão ou na parede, como a tocha comum, com luz 15; acende um anel de luz invisível em volta (área clara cerca de 3 vezes maior) e impede que monstro nasça sozinho a até 30 blocos. O carpinteiro sem tarefa a põe sozinho, sem custo, ao lado dos caminhos escuros da vila (Regra 127) |
| Caminho de Areia | pá na areia, como o caminho de terra | Igual ao caminho de terra do jogo, na cor do arenito; é o caminho das vilas do deserto (Regra 73) e volta a ser areia quando algo sólido o cobre |

## Construções e biomas

O catálogo profissional está em
`src/main/java/com/villagecolony/fabric/work/HousePlans.java` e é validado
contra os nomes reais do Minecraft 1.21.1. As famílias permitidas são:

- Planície: casas, oficinas, fazendas, currais, estábulos e lampião.
- Deserto: casas, oficinas, fazendas, currais e lampião.
- Savana: casas, oficinas, fazendas, currais e lampião.
- Taiga: casas, oficinas, fazendas, curral e lampião.
- Tundra nevada: casas, oficinas, fazendas, currais e lampião.

O armazém do mod tem uma versão por estilo de bioma, nasce sempre do solo
para cima e não leva folhas nem alçapões (Regra 115). Toda obra nova é
afastada no mínimo 3 blocos de outra construção (Regra 124) e ganha o chão
nivelado em volta (Regra 131).

O detalhe dos IDs e a regra da estrutura própria estão em
[`src/main/resources/data/villagecolony/structure/houses/README.md`](src/main/resources/data/villagecolony/structure/houses/README.md).

## Instalação

Requisitos: Minecraft Java 1.21.1, Fabric Loader compatível, Fabric API e
Java 21. Coloque o JAR e a Fabric API na pasta `mods`. O servidor precisa do
mod; clientes que entram em um servidor dedicado não precisam instalá-lo. No
cliente, o mod liga as sobreposições de profissão e de obra, e o Mod Menu
(opcional) abre a tela de regras de profissão — mudar regras exige operador
nível 2.
Use um mundo de teste: o mod corta árvores, minera e coloca blocos no mundo.
O que o mod grava no computador, como conferir o SHA-256 do JAR e como reportar
uma falha estão em [`SECURITY.md`](SECURITY.md).

## Comandos e dados de análise

Todos os comandos são abertos a qualquer jogador e respondem sobre a vila em que você está.

| Comando | O que mostra ou faz |
|---|---|
| `/vc log` | Situação da vila: prioridade das obras, felicidade, **estado agora** (quem está no ofício, ajudando, travado — com os travados nomeados —, estoque e baús cheios, obra aberta, tarefas, mina, ruas, bancadas e animais), atividades recentes, o que a obra espera e a cadeia que a traz, alcance a pé, tempo dos ofícios na última janela de 5 min (Regra 51) e esperas longas |
| `/vc bioma` | Boletim do bioma (estilo, camas, aldeões, lotes e recusas, peças que surgiram, ofícios acima da Regra 51) e o mesmo retrato completo do `/vc log` |
| `/vc bioma nota <texto>` | Anota uma observação sua, com posição e bioma, no log (`VC_BIOME_NOTE`) e no arquivo de análise do mundo |
| `/vc dados` | Grava agora o retrato completo da vila no arquivo de análise e diz onde ficou |

**O retrato da vila (`VC_STATE`).** A cada 5 minutos, por vila com jogador perto, o mod registra num
JSON só:

- dia e hora do jogo, MSPT do servidor, bioma, estilo e tamanho da vila;
- aldeões, ofícios, camas, encalhados, e quantos estão no ofício, ajudando, travados, à toa ou fora do
  expediente (Regra 129), com os travados nomeados;
- tempo de cada ofício na última janela e a parte da Regra 51;
- estoque por grupo, número de baús, quanto estão cheios e se pedem armazém;
- obras prontas, obra aberta (estado, peças que faltam e de lado), lotes aceitos e recusas;
- tarefas abertas por tipo e quantas têm dono;
- mina (altura do nível, níveis abaixo da boca, posições cavadas);
- colunas de rua, bancadas presentes e animais por espécie.

O registro sai no log como `VC_STATE` e fica, junto das notas, em
`<mundo>/villagecolony/analytics/AAAA-MM-DD.jsonl` — uma linha por registro, com o instante real
(`at`) para cruzar com o Spark. O arquivo fica com o mundo entre sessões e versões, e cada registro
traz `schema` e `mod`, para comparar versões.

Relatórios (rodar depois de jogar):

```bash
python scripts/state_report.py
```

```bash
python scripts/time_ledger.py
```

```bash
python scripts/action_report.py
```

O `state_report.py` lê os arquivos de análise de todos os mundos da instalação de teste (ou os arquivos e
logs passados) e mostra, por colônia:

- o período e a versão do mod;
- a evolução de aldeões, ofícios e obras por hora;
- o enchimento dos baús e os travados;
- a média da Regra 51 por ofício;
- o MSPT;
- as notas do jogador.

Os outros relatórios:

| Script | O que mostra |
|---|---|
| `time_ledger.py` | O tempo por ofício (`VC_TIME`) |
| `spark_decode.py` | Resumo de um perfil do Spark baixado |
| `save_read.py` | Grafo e mapa do save, sem abrir o jogo |
| `gametest_focus.py` | Roda só algumas classes de GameTest |
| `action_report.py` | O diário de ações por aldeão, com os motivos de recusa |
| `cost_ledger.py` | O custo por fase do ciclo (`VC_COST`) |
| `supply_report.py` | O que surgiu sem um ofício obter (`VC_SUPPLY`) |
| `biome_report.py` | O boletim por bioma (`VC_BIOME`) |

## Desenvolvimento e verificação

### Skills do projeto

Os perfis de trabalho e as skills utilizadas neste repositório estão em
`AGENTS.md`. A matriz operacional é:

- `fabric-development` para código Fabric e integração com Minecraft 1.21.1.
- `minecraft-villager-systems` e `minecraft-code-research` para profissão,
  rotina, caminho, estoque e diagnóstico de aldeões.
- `minecraft-testing` para testes unitários e GameTests.
- `minecraft-resource-pack` e `minecraft-world-generation` só quando a tarefa for
  textura ou geração de mundo.
- Skills fora do escopo de um mod Fabric (Paper, servidor, datapack, multiloader…)
  saíram em 2026-10-31 e estão guardadas em `docs/archive/skills/`.
- Auditoria (2026-10-10): `java-architecture-review`, `java-static-analysis`,
  `minecraft-performance-profiling`, `fabric-persistence-migration` e
  `git-workflow-audit`. Origem e uso em
  [`docs/technical/avaliacao/agent-setup.md`](docs/technical/avaliacao/agent-setup.md).

Os perfis Codex são `village-quick` (consulta), `village-audit` (diagnóstico),
`village-dev` (implementação comum) e `village-max` (mudança entre subsistemas).

### Fechamento de entrega

Use as palavras-chave **FECHAR-RAPIDO** e **REGISTRO-RELEASE** ao pedir este
fluxo ao agente. Os comandos locais equivalentes são:

```powershell
python scripts/release_workflow.py fechar-rapido
python scripts/release_workflow.py registro-release --jar build/libs/village-colony-X.Y.Z.jar --output release-evidence.md
```

`FECHAR-RAPIDO` só confere e relata: não altera documentos. `REGISTRO-RELEASE`
exige [`docs/releases/vX.Y.Z.md`](docs/releases/README.md), recusa evidência
pendente e gera a seção que o workflow do GitHub inclui na descrição da release.
O PR continua responsável pela revisão humana de README, STATE e CHANGELOG.

```powershell
./gradlew.bat test
./gradlew.bat build
./gradlew.bat runGametest
./gradlew.bat pitest
./gradlew.bat javadoc
python -m unittest discover -s tests
```

### Sentinela Local

**Sentinela Local** é nome oficial da varredura manual. Para pedir a execução
completa ao Codex, use `rodar Sentinela Local`; ela usa o perfil `full`. O
Sentinela não roda no login, não agenda tarefas e não altera o CI. Cada chamada
preserva logs, XMLs e um resumo em
`reports/test-runs/<UTC-id>/`.

```powershell
python scripts/test_lab.py quick --dry-run
python scripts/test_lab.py fast --only unit,property --repeat 100
python scripts/test_lab.py full --budget-minutes 120
python scripts/test_lab.py --list
python scripts/test_lab.py quick
python scripts/test_lab.ps1 fast --repeat 3
python scripts/test_lab.py full
python scripts/test_lab.py --resume 20261101T120000Z
python scripts/sentinel_analysis.py
```

### Aplicativo Windows

Abra [`Sentinela/Abrir-Sentinela.cmd`](Sentinela/Abrir-Sentinela.cmd) por duplo
clique. A janela configura perfil, suites, repetições, limite seguro, simulação
e retomada; mostra a saída ao vivo e abre `reports/test-runs/`. Ela chama o mesmo
`scripts/test_lab.py`, não usa IA, não agenda nada e não altera CI. Requer
Python 3.9+ com Tkinter.

Tutorial completo: [`Sentinela/TUTORIAL.md`](Sentinela/TUTORIAL.md). Alterações
no Sentinela atualizam esse tutorial no mesmo commit.

- `quick`: JUnit e propriedades jqwik.
- `fast`: `quick` mais GameTests em servidor.
- `full`: `fast` mais auditoria GameTest e PIT.

Os casos MAE de elegibilidade, proteção, interrupção e persistência entram nas
suites `unit` e `gametest` existentes. Comandos focados e limites constam no
[tutorial](Sentinela/TUTORIAL.md#segurança-mae--fases-1-e-2).

Se uma suite falhar em uma repetição e passar em outra, o resumo é
`UNSTABLE` e o comando sai não zero. GPU não acelera estas suites de servidor;
uma futura camada visual de cliente precisa de validação Fabric separada.

O resumo e `dashboard.html` mostram duração, casos XML, falhas agrupadas,
testes lentos e até 50 execuções locais. `--only unit,property` escolhe suites
do perfil. `--budget-minutes 120` termina a suite corrente e registra
`INTERRUPTED` antes de iniciar outra. `--resume <UTC-id>` executa somente as
suites ainda não registradas. A cópia de
artefatos compara todos os arquivos internos, impedindo que um XML atualizado
seja omitido porque o diretório pai conservou o mesmo horário. O script exige
Python 3.9+; o wrapper `test_lab.sh` requer Git Bash, WSL ou outro shell POSIX
e escolhe `python3` ou, na ausência dele, `python`.

`analisar Sentinela Local` executa somente `scripts/sentinel_analysis.py`.
Ele lê execuções já preservadas e cria `reports/sentinel-analysis/<UTC-id>/`
com JSON, Markdown e painel HTML. Não chama Gradle. Durante uma nova execução,
o Sentinela registra CPU, RAM e memória dos processos Java; GPU NVIDIA é
opcional e aparece como indisponível quando `nvidia-smi` não existe.

O build exige Java 21 (`JAVA_HOME` apontando para um JDK 21).

Cobertura: `test` gera `build/reports/jacoco/test/html` (unitários) e
`runGametest` gera `build/reports/jacoco/gametest/html` (bateria de jogo, que
é quem exercita a camada `fabric`).

Verificação de 2026-10-31 (0.4.0, promoção de `fix/estruturas-0110`), rodada
antes da release:

- 1667 testes unitários: aprovados (`test`, com ArchUnit, Error Prone e o teto
  de 500 linhas por arquivo).
- 153 testes Python: aprovados (o CI instala o `nbtlib` e o `pillow` para os
  testes das versões do armazém e dos mapas).
- 940 GameTests: todos aprovados. O
  `runGametest` começa cada bateria com o mundo de teste apagado, como o CI.
- `clean build`, `test` e `runGametest`: aprovados; JAR
  `village-colony-0.4.0.jar`, SHA-256
  `9E011A3F256C22B1695D2A2AA44366CB4A38AE03DD4C356F138B958B86BF040D`.
- Regras 161 e 162: sem teste próprio.
- Mutação (PIT, pacote `core`): medida na 0.3.109, 2514 de 2957 mutações
  mortas (85%, limiar 85%); não refeita na 0.3.110.
- CI do GitHub ("Build and test"): a `main` exige esse check para mesclar.

Para refazer: `scripts/audit/check-tests-green.sh --runs 3` e
`scripts/audit/check-mutation-threshold.sh --run` (ver
[`scripts/audit/README.md`](scripts/audit/README.md)).

## Estado e nota da auditoria

Auditoria técnica de 2026-10-10: nota global **6,9/10** (12 dimensões); pela
régua reprodutível da [`METODOLOGIA`](docs/technical/avaliacao/METODOLOGIA.md),
conceito **B**. Resumo, riscos e ganhos rápidos em
[`EXECUTIVE-SUMMARY.md`](docs/technical/avaliacao/EXECUTIVE-SUMMARY.md); fila
priorizada em [`ACTION-QUEUE.md`](docs/technical/avaliacao/ACTION-QUEUE.md) e no
[`TODO.md`](TODO.md).

Os maiores riscos medidos: a Regra 51 reprovada em jogo (P0), um save com uma
entrada malformada que faz o mundo abrir sem colônias (P1) e o ciclo da colônia
sem versão gravada (P1). A regressão de alternância de obras citada pela
auditoria de 2026-09-21 não se reproduz mais na bateria: o teste dela passa,
mas falta a prova em jogo. O retrato de 09-21 está em
[`docs/archive/technical/Project-Audit-2026-09-21.md`](docs/archive/technical/Project-Audit-2026-09-21.md),
e o de 10-10 em
[`docs/archive/technical/Project-Audit-2026-10-10.md`](docs/archive/technical/Project-Audit-2026-10-10.md).

## Produção futura

O que já está decidido ou estudado e ainda não foi feito — o detalhe e a ordem
ficam no [`TODO.md`](TODO.md), seção "Produção futura":

- **Configuração por mundo e por colônia** (R-153/154 do estudo v2): desenho
  pronto em
  [`docs/research/2026-10-22-desenho-configuracao.md`](docs/research/2026-10-22-desenho-configuracao.md)
  — arquivo JSON na pasta do mundo, comando `/vc config`, 14 ajustes com faixa
  segura. Decisões do autor tomadas; falta implementar.
- **O resto do estudo "Regras v2"**, avaliado ponto a ponto em
  [`docs/research/2026-10-22-avaliacao-regras-v2.md`](docs/research/2026-10-22-avaliacao-regras-v2.md):
  contrato explícito de tarefa (R-047), estados de disponibilidade de recurso
  (R-019) com invalidação por evento (R-021/R-027), bateria de escassez (R-158),
  e os sistemas grandes ainda sem decisão — área protegida pelo jogador (R-035),
  transporte físico de itens (R-140 a R-142) e itens de outros mods (R-026).
- **Pendências de jogo**: o percurso dos mineiros até a mina do lago no deserto
  e as peças de ardósia que o armazém pede no deserto.

## Documentos de entrada

- [`CLAUDE.md`](CLAUDE.md): regras para trabalhar no repositório.
- [`STATE.md`](STATE.md): estado vivo e pendências de playtest.
- [`TODO.md`](TODO.md): backlog canônico, com a produção futura.
- [Falhas e prioridades de 01-11-2026](docs/audit/2026-11-01-falhas-priorizadas.md):
  problemas conhecidos, urgência e evidência; [plano aprovado](docs/technical/Plano-playtest-2026-11-01.md).
- [`docs/RULES.md`](docs/RULES.md): todas as regras do autor (fonte da tabela
  acima).
- [`CHANGELOG.md`](CHANGELOG.md): o que mudou em cada publicação.
- [`SECURITY.md`](SECURITY.md): o que o mod faz no computador, build verificado e
  como reportar falha.
- [`docs/decisions/`](docs/decisions/): decisões arquiteturais.
- [`docs/behavioral-tests/`](docs/behavioral-tests/): estratégia e falhas de
  GameTest.
- [`docs/technical/avaliacao/`](docs/technical/avaliacao/): auditoria técnica
  atual e avaliações pela régua.

Licença MIT. Feito para Minecraft 1.21.1 com Fabric.
