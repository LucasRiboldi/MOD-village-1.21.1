# Changelog

Mudanças que chegam ao JAR ou à forma de verificar o mod. O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

Até 03-10 a versão ficou em **0.3.0 alpha**, republicada a cada entrega. Desde
então cada JAR ganha número próprio. As entradas de 0.3.1 a 0.3.23 foram
reconstruídas em 11-10 (AUD-14) a partir do commit que trocou a versão, citado
em cada título; o SHA-256 de cada JAR publicado está no histórico do `STATE.md`
(`docs/archive/technical/Historico-2026-10.md`, `scripts/release_manifest.py`). O histórico anterior a
2026-09-24 está em [`docs/archive/technical/Development-Log.md`](docs/archive/technical/Development-Log.md)
e [`docs/archive/technical/Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md).

## [0.3.32] — playtest da 0.3.31: mina na borda, obra, ócio e overlay (2026-10-11, JAR local, não jogado)

### Mudado
- **Regra 53 — boca da mina só na borda:** com a caixa da vila medida, a boca nasce na faixa
  de até 8 blocos da linha da caixa (também quando a mina recomeça ou muda de lado no fundo).
  Com a mina aberta, o mineiro sem ramal livre não cava mais a encosta em volta da vila.
- **Overlay em pixel:** moldura medieval (madeira, latão, cantoneiras de ferro) com o pixel do
  tamanho do pixel da fonte; ícones 16×16 com contorno para os oito ofícios e os três estados da
  obra; placa da obra em português ("Casa média (planície) 62/292", "Falta: Cama branca ×1");
  `pt_pt.json` (o jogo do autor está em português de Portugal e o overlay saía em inglês).
- **Fazendeiro** com nada maduro e nenhum canteiro vazio ajuda a obra.
- **Tábua e carvão vegetal com teto:** sem obra pedindo, a tora só vira tábua até 64; o fundidor
  não faz nada "para as obras" acima de 64 (o carvão vegetal conta o mineral).
- **Obra com só até 4 peças sem apoio** (tocha de parede sem parede), depois de duas janelas de
  paciência, larga essas peças e fecha (`VC_UNSUPPORTED`), em vez de prender a vaga de obra.
- **Antes de surgir**, o que a vila já tem em outro baú é levado ao baú da profissão.

### Corrigido
- Obra que volta do save voltava girada pela rua nova, por cima dela mesma (292 peças "por pôr",
  27 blocos da própria casa limpos, 120 "no caminho"): volta no giro em que foi levantada.
- Construtor ocioso 85%: a vaga do ajudante contava como a tarefa da obra, e o construtor que a
  largou não recebia outra por 18 min.
- Construtor dentro da mina mandado a uma galeria debaixo do lote.
- Mineiro na superfície andando para a entrada enterrada embaixo dele quando a pedra estava no
  fim do ramal; mineiro 2+ blocos abaixo do destino girando no mesmo buraco (agora sai pela fuga).

## [0.3.31] — pedidos do autor depois do playtest da 0.3.29 (2026-10-11, JAR local, não jogado)

### Mudado
- **O que surge no baú surge na quantidade toda** que a obra ainda pede, e não um por vez.
- **Troca de ofícios desligada:** nem a regra do ócio nem três desistências tiram mais o
  aldeão do ofício. O teto de 15% da Regra 51 continua como medida.
- **Lenhador:** corta a árvore mais perto dele, dentro do alcance da vila, e não mais a do
  centro primeiro (a ordem "do centro para a borda" de 08-10 foi substituída).
- **Pastor:** leva os animais para dentro do cercado com lugar mais perto da casa (até 5 por
  cercado; com mais, tenta outro), entrando pelo portão. Sem cercado com lugar, amarra na cerca
  mais perto, como antes. Sem cerca, põe um poste ao lado da casa com uma cerca do baú e amarra.
  Com cercado com lugar, também leva para dentro quem estava só amarrado.

## [0.3.30] — depois do playtest da 0.3.29 (2026-10-11, JAR local, não jogado)

Playtest de 1h50 (19:26–21:16). TPS 20, mod 3,3%. O Spark falhou ao enviar (sem rede) e
salvou o perfil em `config/spark/`.

### Corrigido
- **Mina:** o degrau de volta da veia que desce não consultava a marca de fora de alcance.
  Os três mineiros ficaram 50 minutos na mesma pedra (30.079 recusas), e a mineração caiu
  de 31 para 13 blocos por minuto.
- **Obra presa na última peça:** a peça posta de lado por alcance só voltava se o apoio
  mudasse. A casa média ficou 25 minutos com 1 bloco faltando e sem tarefa. Agora, sem mais
  nada a pôr, ela volta, e o construtor que segura a obra também a tenta de novo.
- **Ciranda de ofícios:** o ofício parado só cede gente quando outro ofício tem falta para a
  obra. Foram 19 saídas em 90 minutos, de um ofício parado para outro também parado.
- **Fundidor com baú cheio:** o produto vai ao baú da vila com lugar; antes ele largava a
  fundição.

## [0.3.29] — o construtor começa a obra (2026-10-11, JAR local, não jogado)

### Mudado
- Só o construtor pega a tarefa que abre a obra; os outros ofícios entram como ajudantes depois
  (`BuildHelper`), perto da obra. Colônia sem construtor cai na regra do ajudante perto. Na 0.3.26
  o pedreiro tinha pegado a casa nova e o construtor ficou 92% parado.

## [0.3.28] — cobertura de blocos por profissão (2026-10-11, JAR local, não jogado)

Pedido do autor: todo bloco das estruturas da vila, e todo bloco que os produz, com
ofício responsável. Análise e contagens: `docs/research/2026-10-11-cobertura-de-blocos.md`.

### Mudado
- As peças fabricadas vão ao ofício do material: madeira ao carpinteiro; pedra, barro,
  terracota e fornalha ao pedreiro; metal, vidro, gelo e neve ao fundidor; lã colorida,
  tapete, cama e corante ao pastor; papel, livro, farinha de osso e feno ao fazendeiro.
  O carpinteiro tinha 103 dos 187 itens; agora tem 42.

### Novo
- Carpinteiro e fundidor guardam 8 de cada item das plantas da vila e dos ingredientes
  deles, em rodízio, com a obra na frente.
- O fazendeiro colhe flores, grama, samambaia, bambu e cana e replanta; o fundidor
  recolhe gelo e neve.

### Corrigido
- O pedido nominal de peça não é mais encadeado como falta de recurso (o log mostrava
  "chained CRAFT_WOOD_MATERIAL for COBBLESTONE" e o carpinteiro convertia tábua à toa).

## [0.3.27] — depois do playtest da 0.3.26 (2026-10-11, JAR local, não jogado)

Playtest de 6 min (Spark `HGyazYR1VW`): TPS 20, mod 2,5%; a vila ficou 3 min presa numa
vidraça que o pastor pegou e não alcançou, e quase todo ofício parado sem demanda.

### Corrigido
- O ajudante de obra (outro ofício) só pega a obra a até 32 blocos dela e a solta depois de
  30 s andando sem chegar; o construtor mantém os 2 minutos.
- Com só peças de lado faltando, o construtor segura a obra esperando o material (até 5 min)
  e calça a rua enquanto isso, em vez de soltá-la para outro ofício.
- O lenhador larga a árvore depois de 30 s sem chegar mais perto dela; antes eram 2 min, e
  andar até árvore longe também contava.

### Novo
- Teto geral do estoque: 64 para bloco fabricado, 256 para bloco cru, acima só o que a obra pede.
- O carpinteiro e o pedreiro sem tarefa ajudam a obra, como o pastor (até 2 ajudantes).
- O britador é a primeira fonte de areia: enquanto há pedreiro e pedra a moer, ninguém sai
  atrás de areia na natureza e o fundidor sem areia vai ajudar outro ofício.
- A busca de areia volta direto à mancha lembrada mais perto da vila (raio 3 em volta).
- A muda da árvore cortada longe vai para o bosque perto do baú do lenhador (até o dobro).
- O fazendeiro ara canteiro novo na borda da roça, com água perto, quando nada está maduro.
- Aberta a escada da mina, os outros ramais abrem já, pulando a sala comum.

## [0.3.26] — depois do playtest da 0.3.25 (2026-10-11, JAR local, não jogado)

### Corrigido
- A vila decide com a última contagem completa dos baús enquanto a nova rodada de leitura não
  termina; a leitura parcial (8 de 20 baús) fazia o pedreiro produzir laje com 330 no baú.
- A mina nunca fecha por descida travada: um mineiro preso num buraco a 90 blocos fechou a mina
  boa. O ponto fica de castigo e o mineiro cava outro caminho.
- O fundidor parado sem areia solta a fornalha e vai buscá-la; antes os dois seguravam "fundir
  vidro" e a colônia contratou um terceiro só para pegar a areia.
- Fundir sem o cru em baú nenhum não abre vaga de contratação.

### Novo
- O britador do pedreiro entra também quando a obra espera vidro e não há areia nos baús.
- O fundidor guarda 16 areias de reserva; a areia da reserva nunca surge do nada.
- O artesão fabrica onde está: ninguém anda até o baú.
- O ajudante de obra larga a peça que não alcança e a deixa para o construtor.

### Mudado
- O bloco que nenhuma rota entregou surge no baú do construtor depois de 1 minuto (2 ciclos),
  e não 10 (segunda emenda na ADR-040).

## [0.3.25] — autossuficiência (2026-10-11, JAR local, não jogado)

Branch `claude/autossuficiencia`; análise e escolhas em
`docs/research/2026-10-11-autossuficiencia-e-ocio.md`. O aceite é um playtest único.

### Corrigido
- A peça deixada de lado por falta de material voltou a contar na demanda: a vidraça adiada zerava a
  meta de vidro e de areia, e nenhuma areia era coletada.
- Produção antecipada (lajes, escadas, cercas, pedra lisa) tem teto: o que a obra aberta ainda pede
  mais o maior uso numa casa do estilo da vila (antes: 318 lajes e 356 escadas para 15 pedregulhos).
- Peça de estoque e fornalha não gastam o cru que a obra aberta ainda vai pôr.
- Os quatro ramais da mina seguem abertos depois que o ramal zero acaba.
- O mineiro não volta à mesma coluna de caverna: o beco conta as recusas dos últimos 15 minutos.
- Tarefa de fabricação que termina com 0 peça não reabre outra igual.
- A tábua das escadas, lajes e cercas da obra entra na conta da madeira do carpinteiro.
- Fundidor: a reserva do cru é calculada uma vez por cru (era 72% do tempo do mod no Spark).

### Novo
- Britador do pedreiro: sem areia ao alcance, pedregulho vira cascalho e cascalho vira areia (2:1).
- Busca de areia gira de setor a cada varredura vazia; a resposta "há areia" usa a mesma sonda da busca.
- Sem rota para vidro, a vidraça da planta vira cerca de madeira.
- O bloco de pedregulho da planta aceita andesito, granito e diorito.
- Bosque-fazenda: 6 mudas por lenhador, e a árvore madura do bosque é cortada primeiro.
- Lenhador parado no caminho tenta um desvio (cavando) antes de largar a árvore.
- Compostor do fazendeiro: semente acima de 32 vira farinha de osso, usada na lavoura verde.
- Fazendeiro semeia com semente de qualquer baú da colônia.
- Regra do ócio: o titular único parado cede a um ofício em falta para a obra (o construtor nunca).
- O construtor sem tarefa limpa muda, flor alta e arbusto da faixa de construção (6 blocos da rua);
  ninguém planta nessa faixa.
- Planta ou árvore que o jogador tirou não volta naquele lugar (marca salva).

### Mudado
- A peça que surge no baú da obra é último recurso: 10 ciclos (~5 min) sem entrega com a rota
  trabalhando, e não 4 (emenda na ADR-040).

## [Não publicado] — correções do roteiro de 10-10 (2026-10-11, sem JAR publicado)

Nada disto foi visto em jogo; o aceite é o playtest da Fase 2.5 do roteiro.

### Corrigido
- Mineiro: a descida pela boca da mina tem prazo próprio (60 s de expediente sem chegar mais perto
  nem descer mais fundo); antes, quem ia e voltava a caminho de um patamar de pedra maciça segurava
  o único ramal da colônia sem prazo nenhum (roteiro 2.2, RC1).
- Mineiro: a rota até o nível de agora segue a escada cavada. Depois do giro da escada (ADR-037 M1),
  ela era calculada pelo rumo novo, e no save do autor a boca deduzida ficava 50 blocos ao lado da
  real, com patamares de pedra maciça no caminho (roteiro 2.2, RC2).
- Save: uma entrada malformada é descartada com aviso, em vez de apagar todas as colônias (AUD-02);
  o save grava a versão do esquema (AUD-03).
- Regras de profissão do Mod Menu: ordem ruim no save não apaga mais as regras (AUD-11).
- Bateria: `FreeChestCountGameTest` contava os aldeões dos testes vizinhos, que a varredura de 64 blocos
  alcança no mundo compartilhado (2 falhas em 8 rodadas em 11-10); agora conta só os da própria arena,
  com mutação conferida (AUD-21).

### Mudado
- A regra do ócio cede vaga pela soma ocioso + bloqueado + encalhado, a mesma do `time_ledger`
  (ADR-041 #3).
- `/vc log` aberto a qualquer jogador, com o tempo de cada ofício na última janela e a regra do
  ócio (AUD-19).
- O diário de ações grava o motivo de cada tarefa solta; `action_report.py` agrupa por motivo (AUD-25).
- O ciclo avisa, uma vez por aldeão, o trabalhador cujo `Brain` não roda a tarefa da colônia há 10 s,
  sinal de outro mod que trocou a IA do aldeão (AUD-12). `fabric.mod.json` sugere o Mod Menu.
- A fundação BigHouseMOD é colocada com a semente do mundo e do lugar (AUD-22).

### Verificado
- Na RC2: 1442 unitários, 704/704 GameTests em 3 rodadas. PIT não roda: as mudanças são todas em `fabric`.

## [0.3.24] — integração da linha Codex (2026-10-10, sem JAR publicado)

### Adicionado
- Despacho físico de material da obra (ADR-040): a contingência conta despachos de ciclo com tarefa
  aberta na rota, não consultas do construtor.
- Fundidor mantém a tarefa enquanto o cru falta; calcita na meta do mineiro; rota física para material
  de obra sem `ResourceType`; relatório de log separa fallback de estoque.

### Mudado
- O servidor não escreve mais a profissão no nome do aldeão; o painel do cliente com o mod a mostra.
- O texto da profissão no painel é sempre visível (a opção de escondê-lo saiu do Mod Menu).
- Peças adiantadas da obra não gastam mais tentativa de contingência.

### Verificado
- 1402 unitários, 696/696 GameTests em 3 rodadas, 105 Python, PIT 1807/2112 (85,56%). Nada visto em jogo.

### Atenção
- A linha Codex numerou de novo 0.3.7 a 0.3.12 em 09-10 e 10-10 (P0.7, P0.8). Esses números não
  são os das entradas abaixo; o que eles trouxeram entrou aqui, na 0.3.24.

## [Não publicado] — auditoria técnica de 2026-10-10

Nada muda no JAR: a auditoria não alterou código de produção.

### Adicionado
- `scripts/audit/`: oito verificações reprodutíveis (`check-architecture`, `check-tests-green`,
  `check-mutation-threshold`, `check-coverage`, `check-docs-drift`, `check-mixin-safety`,
  `check-nbt-migration`, `check-tick-budget`), as sondas de fuzzing de NBT e as propriedades
  do planejador de desvio (`scripts/audit/probes/`), e um init script de SpotBugs/Checkstyle.
- Skills de auditoria em `.claude/skills/`: `modpack-modsmith` (rdl7999-hub, MIT),
  `java-architecture-review`, `java-static-analysis`, `minecraft-performance-profiling`,
  `fabric-persistence-migration`, `git-workflow-audit`.
- `docs/technical/avaliacao/`: D01–D12, `ACTION-QUEUE.md`, `EXECUTIVE-SUMMARY.md`,
  `agent-setup.md`, avaliação da régua `2026-10-11-b290ed10/`; retrato em
  `docs/archive/technical/Project-Audit-2026-10-10.md`.
- `docs/technical/Roteiro-Correcoes-2026-10-10.md`: roteiro em sete fases (0–6) para os
  achados AUD-01…AUD-25, validado contra o código e as sessões 0.3.22 e 0.3.23 de 10-10.

### Verificado nesta sessão
- 1398 unitários; 690/690 GameTests × 3 rodadas, mais 1 com Fabric API 0.116.17 e Loader 0.19.5;
  104 testes Python; PIT 1804/2106 (85,66%).

### Corrigido (documentação)
- README com as contagens de 03-10 e a frase "GameTests pulados" na 0.3.23.

## [0.3.23] — 2026-10-09 (`fa48cf2c`)
- Obra: fluxos de profissão mais flexíveis (P0.54); a pendência salva de `barn_majest` é descartada
  e a argila é completada (P0.56).
- Fundidor: suprimento corrigido (P0.55). Em jogo, na noite de 10-10, o fundidor ainda soltou 311
  de 311 tarefas (AUD-01).

## [0.3.22] — 2026-10-09 (`3c4a30fb`)
- `barn_majest` sai do rodízio de obras (P0.53).
- O JAR jogado na manhã de 10-10 foi regerado a partir de uma árvore sem commit; não dá para provar
  o que ele trazia (auditoria de 10-10, D2).

## [0.3.21] — 2026-10-08 (`ed92b94f`)
- Regra 52: uma roça por fazendeiro, com dono (P0.52).

## [0.3.20] — 2026-10-08 (`90da1e68`)
- Funil, trilho e vaso aparecem no baú do construtor depois das faltas.
- O pastor procura ovelha com lã onde o rebanho é contado.
- Planta com camada 0 subterrânea assenta no chão.

## [0.3.19] — 2026-10-08 (`2486cb04`)
- Regra 51 no `time_ledger`: ocioso + bloqueado ≤ 15% por profissão.
- Construtor sem obra calça a grama no meio dos caminhos.
- Lenhador: árvore longe por pernas de 24 blocos; bosque de 6 mudas com farinha de osso.

## [0.3.18] — 2026-10-08 (`1a0fc2dd`)
- Lampião e corrente aparecem no baú do construtor depois de três faltas.
- A ajuda do pastor na obra conta pelo menos 10 blocos antes de ele voltar ao rebanho.

## [0.3.17] — 2026-10-08 (`00996e0b`)
- Mineiro: na escada, a próxima entrada é a primeira abaixo dele; a descida não gasta o orçamento
  de caminhada.
- Peça estocada para a obra não diz mais "no recipe" no log.

## [0.3.16] — 2026-10-08 (`103a556c`)
- Fardo de feno no baú do construtor depois de três faltas.
- Bosque de três mudas perto da vila, longe de onde ela vai construir.
- No tempo livre, o pastor ajuda o construtor a pôr blocos.

## [0.3.15] — 2026-10-08 (`1c6c6194`)
- Mineiro: a descida conta a chegada por eixo e mira o lugar de pé da entrada.
- `/vc log`: a mesma profissão esperando pelo mesmo motivo aparece uma vez.

## [0.3.14] — 2026-10-08 (`0f604e5c`)
- O mineiro guarda a boca de superfície da mina, vai até ela e desce nível por nível (`MineEntrance`).
- Lenhador: árvores do centro da vila primeiro, depois as intermediárias, a borda e um pouco fora.
- M2: mapa de alcance a pé da vila (`VC_REACH`, `[ALCANCE]` no `/vc log`).

## [0.3.13] — 2026-10-08 (`4562278b`)
- Fundidor: o pedido de fundir só sai com o cru num baú (`SMELT_INPUT`).
- Mineiro: o E1 escolhe o lugar alcançado mais perto da pedra; pedra acima do teto por tipo não é
  mais tratada como baú cheio.

## [0.3.12] — 2026-10-08 (`02f84a62`)
- Crash "Colony already has an open project" ao retomar obra do save.
- E1–E7: pedra só vira alvo com posição de trabalho alcançável; estado explícito de cada ramal; a
  roça conhecida responde sozinha; forma guardada não é rota de produção; o pastor procria só sob
  déficit do rebanho; `[CADEIA]` no `/vc log`; motivos de recusa no `action_report`.

## [0.3.11] — 2026-10-08 (`f6ac1a91`)
- Diário de ações em JSONL por aldeão (`logs/villagecolony-actions-*.jsonl`) e
  `scripts/action_report.py`.

## [0.3.10] — 2026-10-08 (`4adb6f6d`)
- A peça sem material fica de lado e a obra segue.
- Overlay: nome da profissão dentro do painel, ícones lisos, painel da obra sobre a placa.

## [0.3.9] — 2026-10-08 (`2d9b3451`)
- O funil pede minério de ferro, e o minério é material da obra.

## [0.3.8] — 2026-10-08 (`3696cead`)
- Obra destravada: a roça relida é a planejada, e a trava de reserva diz o que recusa.

## [0.3.7] — 2026-10-07 (`d1821292`)
- Junção e ciclo de vida só na colônia do jogador (ADR-039 item 3, opção B).
- Testes B1–B6 e D2 do plano de testes de 07-10.

## [0.3.6] — 2026-10-07 (`51221f01`)
- Carpinteiro, pedreiro e fundidor adiantam as peças das casas do bioma (ADR-039 item 4).
- O mineiro não perde o ofício por desistência (ADR-039 item 6).
- Carpinteiro e pedreiro guardam o que fazem no baú da profissão (ADR-038 P3c).

## [0.3.5] — 2026-10-07 (`fe32cfc6`)
- Memórias de trabalho escolhidas pelo autor vão ao save (ADR-039 C).

## [0.3.4] — 2026-10-07 (`54de4db6`)
- ADR-039: o tique do servidor de teste não funde mais colônias (KF-003); ramal novo vai para o
  minério que a vila precisa; caminho da rua até a obra longe dela; carpinteiro adianta escada, laje
  e cerca; busca vazia espera de 1 a 5 minutos; a `codex/bighousemod` foi incorporada.

## [0.3.3] — 2026-10-07 (`e684a3ad`)
- ADR-038: ofício parado cede vaga para onde a vila precisa; memória das árvores e mudas da vila;
  o pastor cuida de animal na corda ou em curral; o fundidor adianta vidro e pedra lisa; pedra sem
  lugar de pé ganha o lugar cavado ao lado.

## [0.3.2] — 2026-10-07 (`f6e32557`)
- ADR-037: escada fechada antes do salão não conta como nível (M1); quem vai dormir fecha a porta;
  a reunião doa a comida dos baús; o lenhador busca até a borda + 20; o construtor põe bloco de
  qualquer lugar da zona da obra.

## [0.3.1] — 2026-10-06 (`53a637d1`)
- ADR-036 até o item 12: a peça aparece na 4ª tentativa no baú da profissão; baú da cama em toda
  casa; vila abandonada solta as profissões; a vila só trabalha com o jogador dentro há mais de um
  minuto; Regra 25 desfeita.
- A faixa inclui a ADR-035 e a integração das linhas Claude e Codex de 06-10 (328 commits desde a 0.3.0).

## [0.3.0] — publicação de 2026-10-03, fim da tarde (playtest da manhã)

**Ainda não visto em jogo; a última parte não foi testada** (pedido do autor). JAR SHA-256 `D4FFE0FC…7C99`.

### Corrigido

- Ícones de profissão e de obra trocados: os arquivos de textura estavam com
  os nomes errados.
- Obra parada por peça sem rota uma de cada vez: as tentativas contam para
  todas as peças que faltam no mesmo ciclo.
- Obra com só peças sem apoio sobrando esperava 10 min; agora 1 min.

### Adicionado

- Placa com fundo em pixel art, ícone à esquerda e texto à direita.
- Área da vila marcada com partículas verdes perto do jogador.
- Água e lava lidas uma vez ficam de fora das buscas seguintes (sem teste).

### Alterado

- Base das obras: só a grama fica como chão; a plantação constrói a base
  inteira; bloco que já está no lugar conta como assentado.
- Mina: arco de 5 × 4 com um lampião de cada lado e vão de 3 × 3; salas de
  busca de 3 × 3.

## [0.3.0] — publicação de 2026-10-03, tarde (medida da vila)

**Ainda não visto em jogo.** JAR SHA-256 `00658B2C…92E4`.

### Alterado

- **Medida da vila (ADR-003 Emenda 8):** parte das peças da vila gerada, sem
  a folga de 12 do jogo; 15 blocos em volta de cada obra; só o lado que a obra
  passa avança; lados ímpares, para o centro ser um bloco só.
- **Rua faz a vila crescer só com 10 caminhos conectados fora dela**,
  encostados num caminho de dentro — de quem quer que seja, colônia ou
  jogador. Antes, cada bloco que a colônia assentava empurrava a borda.
- Saves antigos têm a vila medida de novo na primeira visita.

## [0.3.0] — publicação de 2026-10-03, manhã (revisão das profissões)

**Ainda não visto em jogo.** JAR SHA-256 `368A4C4F…FD65`. Relatório em
`docs/research/2026-10-03-paradas-por-profissao.md`.

### Adicionado

- **Arte em pixel nos overlays:** ícone de profissão sobre o aldeão e de
  estado sobre a obra, desenhados no `WorldRenderEvents.LAST` com buffer
  próprio (o cliente com shaders do Iris não mostrava nada).
- **Material que não se acha aparece:** três buscas vazias do alcance da vila
  põem o material no baú de quem o usa (fundidor para areia, argila e cacto;
  construtor para o resto). Vale para material da natureza — revê a decisão
  de 26-09. O castigo entre buscas caiu de 5/10/20 min para 1/2/4 min.

### Corrigido

- Pastor sem ovelha com lã no raio segurava a tarefa calado; agora solta e,
  na 3ª busca, a lã aparece.
- Carpinteiro fechava tarefa de tábua com 0 peça: lia só os baús de
  trabalhador, e a meta todos os da vila.
- Fundidor repetia `stopped — none of … had sand` a cada segundo.

### Retirado

- O atalho que riscava a vidraça na primeira busca vazia de areia (da
  publicação das 02h): com a areia aparecendo, a casa não perde a janela.

## [0.3.0] — publicação de 2026-10-03, 02h (playtest da madrugada)

Correções do playtest de 01:02–01:32 (Spark `r6nErbWNSL`: TPS 20, mod em
1,9% do tick). **Ainda não vistas em jogo.** JAR SHA-256 `D2374106…0615`.

### Corrigido

- **Construtor parado fora da casa fechada:** o lugar de pé que a navegação
  Vanilla marca como inalcançável (`CANT_REACH_WALK_TARGET_SINCE`) é riscado
  em 1 s e ele vai a outro lado; sem nenhum lado alcançável, a peça vai para
  o fim da fila na hora. Antes: 300 tiques parado ou 200 por peça.
- **Vidraça esperando areia que o mundo não tem:** com a varredura de areia
  vazia e nenhum vidro no baú, a barreira da Regra 28 risca a vidraça sem a
  carência de 2,5 min.

## [0.3.0] — publicação de 2026-10-03 (corrigíveis sem jogo)

JAR do commit dos corrigíveis, SHA-256 `923D9769…40C0`, em `downloads/` e em
`.minecraft/mods` (três cópias conferidas). **Nada desta lista foi visto em
jogo.**

### Adicionado

- **Busca de lote mais rápida na vila com jogador dentro** (estudo de 01-10,
  §7-A): uma passagem extra por segundo continua a varredura em curso, com o
  mesmo prazo de 15 ms do ciclo.
- **Encalhado fechado por blocos protegidos** atravessa só a peça da planta
  da própria colônia (o reparo a reconstrói) e, sem saída nem assim, avisa os
  jogadores no chat a cada 5 minutos. Teletransporte continua vetado.
- **Fundidor sem cru** desce um degrau da fornalha: a obra que pede pedra lisa
  põe a pedra na lista, e a pedra sai do pedregulho.
- **Linhas novas no log:** para onde o aldeão ia quando entrou no curral
  (`entered a pen`) e quem mudou as regras de profissão
  (`Profession policy changed by`).
- `scripts/verdict.py` com os itens de 30-09 a 02-10, lendo também os
  `.log.gz` do dia.

### Mudado

- Nenhum arquivo de produção passa de 500 linhas (oito divididos, texto
  movido sem mudança).
- Cobertura JaCoCo da bateria de jogo em `build/reports/jacoco/gametest`.

## [0.3.0] — publicação de 2026-10-02 (consolidação)

JAR do commit da consolidação de 02-10, SHA-256 `AB762691…9FFA`, em
`downloads/` e em `.minecraft/mods` (três hashes conferidos). Junta tudo o que
saiu de 26-09 a 02-10; o detalhe de cada item está no
[`Development-Log.md`](docs/archive/technical/Development-Log.md) e no `TODO.md`.
**Nada desta lista foi visto em jogo depois da consolidação.**

### Adicionado

- **Regras de profissão por mundo** pelo Mod Menu (opcional): ativar,
  limitar, raio de busca e ordem de contratação (ADR-030). A fundação mantém o
  carpinteiro logo depois do lenhador.
- **Sobreposições no cliente:** profissão sobre o aldeão e progresso da obra
  com o primeiro material que falta.
- **Ajudante no tempo ocioso (Regra 48, metade de B-1):** recolhe do chão só
  o item que falta à obra aberta.
- **`/vc log` com as esperas longas (B-5)** e o tempo dos aldeões por
  profissão (Regra 50, linha `VC_TIME`).
- Vila como caixa que cresce e só trabalha com jogador dentro (ADR-003
  Emendas 6 e 7); colônias que se tocam viram uma (ADR-007); rua que cresce
  para fora; pasta de modelos da colônia (Regra 27-e3); ordem das obras
  (Regra 49); índice de árvores (A-2).

### Corrigido

- Aldeão preso no curral (E52), encalhado que não desistia e o retorno pela
  mina (E47), ponta de vila virando colônia nova (E51), tarefa solta voltando
  ao mesmo aldeão (E49), canteiro na grama (E50).
- Obra: carpinteiro fechando tarefa com 0 peça (A-1), piso e base da obra,
  material pedido inteiro (A-3), construtor parando fora do alcance, placa
  órfã, obra que continua longe do jogador.
- Fundidor aceita carvão vegetal onde a obra pede carvão (metade de B-4).

### Mudado

- Ferramenta: velocidade da que ele tem e a melhor do baú (Regras 2-e1,
  37-e1); mina nova na borda, longe da água (A-4); baús da colônia são todos
  os da vila (Regra 45).

## [0.3.0] — entregas de 2026-09-26 a 09-30 (publicadas nos JARs seguintes)

### Corrigido

- **Todo aldeão de profissão tem baú** (decisão do autor). Na sessão longa, o
  lenhador, o mineiro e o pedreiro passaram 4h45 sem baú e, sem baú, sem
  tarefa. Quem não acha baú ganha um ao lado da cama (regra b) ou, sem cama,
  perto do centro; o vínculo é salvo com o mundo.
- **A peça pronta só nasce para bloco de manufatura** (decisão do autor): tora,
  terra, grama, pedra, areia, lã tosquiada, flor e muda vêm da profissão.
- O guarda de alcance não larga mais a obra encostada numa rua só porque o
  índice de ruas ainda não existe.
- A obra que só tem o chão enterrado volta a poder ceder lugar (defeito
  introduzido pela camada da rua).

### Mudado

- **Pastor e fazendeiro trabalham sem depender da obra**, enquanto couber no
  baú deles; a obra continua tendo prioridade.
- O fundidor deixa de perseguir produto de fornalha sem matéria-prima no bioma
  (582 buscas inúteis por arenito numa vila de planície).

## [0.3.0] — publicação de 2026-09-26 (sessões de jogo de 26-09)

JAR do commit `70af4c8`, SHA-256 `9EB559D1…9423`, em `downloads/` e em
`.minecraft/mods` (três hashes conferidos).

### Corrigido

- **A casa nasce com o chão e a porta na altura da rua, sem camada de terra
  na base** (pedido do autor). As casas de vila do jogo gravam uma fundação
  abaixo do piso — na `plains_small_house_5`, uma camada inteira de terra — e
  o mod punha a planta um bloco acima do chão: a casa saía sobre um monte de
  terra, com a porta três blocos acima da rua. Agora a camada em que se pisa
  ao entrar (uma abaixo da porta) vai no chão; o que fica abaixo dela e a
  terra/grama na altura da rua não são construídos, e o piso dessa camada toma
  o lugar da grama. Casas e obras que o mundo já tem não mudam.
- **O viveiro cheio não reconta o terreno a cada chamada.** Com os 10 viveiros
  de pé, cada pedido relia ~166 mil blocos (3º maior custo do mod no spark de
  26-09); agora espera os 5 minutos como quem plantou.

- **O lenhador não é mais expulso enquanto procura árvore.** Na vila sem
  árvore natural, o guarda de imobilidade devolvia a tarefa a cada 300 tiques
  ("while looking for a tree") até ele largar o ofício. Agora os guardas só
  contam com uma árvore escolhida.
- **O tapete verde (e toda peça colorida) não é mais pedido pela receita de
  tingir peça pronta** ("corante + tapete de outra cor", que puxava uma
  corrente de tingimentos que nunca fechava). Tingir lã e misturar corante
  continuam valendo: são produção.
- **A espera por peça sem rota não recomeça ao reabrir o mundo.** O relógio de
  10 ciclos da ADR-022 é salvo com o mundo; na sessão de 26-09 o tapete ia
  chegar 30 s depois de o jogo fechar.
- **O desvio do mineiro:** o próprio corpo parado na beira do bloco não impede
  mais o degrau ("something stood in"); ele volta ao meio do bloco. Quem sai
  do caminho (caiu, foi empurrado) tem o desvio replanejado de onde está, até
  3 vezes, em vez de esperar 100 tiques pelo passo que ficou para trás.

### Mudado

- **Baú das camas da vila vanilla (decisão do autor, regra b):** ao lado da
  cama, encostado numa parede, nunca na frente da porta. A porta deixou de
  decidir se há baú — a regra de 23-09 recusava 3 de 3 camas quando a caixa da
  casa incluía o degrau de fora.
- **O lenhador sem árvore pede um lote de 4 mudas** ao viveiro (no mesmo ritmo
  de 5 min e no mesmo teto de 10), em vez de 1 muda por vez.

## [0.3.0] — publicação de 2026-09-25, tarde (ADR-025, fases 1 e 2)

JAR do commit `a222342`, SHA-256 `8862EC4F…07C0`, em `downloads/` e em
`.minecraft/mods` (os três hashes conferidos pelo `release_manifest.py`).

### Adicionado

- **O mineiro que não chega cava o próprio caminho** (fase 2). Quando fica
  parado, não se aproxima ou anda demais sem chegar, ele planeja um desvio de
  até 16 blocos: abre rocha, põe pedregulho onde falta chão (tirado do baú
  dele) e anda um bloco de cada vez até ter a pedra ao alcance. Só sem desvio
  ele larga a pedra como antes. Log: `takes a detour`, `is through its detour`,
  `its detour failed — …`.
- **O encalhado sem escada natural também desvia.** Onde o E47 desistia com
  "no natural, dry way up", ele procura o nível do terreno cavando e pondo
  bloco; se o raio não basta, anda o trecho que mais se aproxima e tenta de
  novo.
- O desvio nunca cava rocha encostada em líquido nem com areia em cima, nunca
  pisa em líquido, e pergunta de novo antes de cada passo — água que chega
  depois do plano o faz parar.

- **A passagem da mina sai com chão.** Quando a picareta abre uma célula da
  escada ou da sala sobre um vão de caverna que a mina não planejou, o vão
  recebe pedregulho na hora (`The mine floored …` no log). O degrau seguinte e
  a camada de baixo da sala não são tapados; fora da passagem — o veio que o
  mineiro segue — também não.
- **O mineiro não entra na água.** Ele passa a dar a volta seca em vez de
  atravessar nadando (penalidade de água -1 só para ele; lava já era proibida
  no Vanilla).
- A linha de travamento do mineiro traz o estado do cérebro e da navegação
  (`brain: activity …, our walk task …, walk target …, navigation …`), para a
  próxima sessão dizer por que ele para na pedra 553, 39, 158.

### Corrigido

- **O mineiro não quebra mais bloco com líquido atrás** (pedido do autor).
  Nenhuma pedra, minério, areia ou degrau encostado em água ou lava vira alvo;
  a galeria contorna. Antes ele quebrava e vedava depois, e a vedação não
  alcançava a água da vila. Areia de praia colada na água deixa de ser colhida.
- **A pedra recusada volta a ser recusada depois de carregar o mundo.** A
  marca que afasta a pedra inalcançável (E44) é salva
  (`villagecolony_marks.dat`); antes sumia a cada sessão, e o mineiro voltava
  à mesma pedra.

### Ainda não resolvido

- O motivo do travamento em 553, 39, 158: a geometria reconstruída do save é
  andável e a navegação Vanilla chega lá (GameTest forense). Espera a linha
  `brain:` de uma sessão de jogo.

## [0.3.0] — publicação de 2026-09-25, manhã (sessão das 09:13)

### Corrigido

- **A vila presa a templos.** O reparo rodava antes do rodízio casa ↔ outra
  obra e reabria a obra abandonada no mesmo segundo em que a colônia
  desistia dela; ao terminar o templo, a vila reabriu na hora um templo
  abandonado vizinho de 301 blocos. Agora a obra abandonada só volta **na
  vez do tipo dela** (decisão do autor) — nunca logo depois de largada —, e
  a construção terminada que perdeu blocos continua sendo reparada sempre.
  A obra abandonada que o save trouxe aberta também espera a vez.
- A obra retomada e terminada passa a contar como a última tentada no
  rodízio.

### Adicionado

- **O mineiro cava sem parar enquanto os baús dele tiverem espaço**
  (decisão do autor: "galerias novas sem parar"). A meta de pedra passa a
  ser o guardado mais o espaço livre nos baús dos mineiros, como a da
  madeira; a mina continua crescendo sozinha e cada corte segue o minério
  que encontra.

### Ainda não resolvido

- O mineiro empaca sempre na mesma pedra inalcançável (553, 39, 158 no save
  do autor), fica preso e sai da escala até alguém o soltar. A marca que
  afasta a pedra recusada vive só na memória e some ao carregar o mundo
  (E44/E45).

## [0.3.0] — publicação de 2026-09-25, manhã (sessão das 08:31)

### Corrigido

- **A obra que nunca fechava.** O templo da vila ficou aberto por três
  sessões com nove peças "sem apoio" — quatro escadas de mão e cinco tochas
  de parede, todas com pedregulho encostado. A planta não guarda a direção
  do bloco, e as peças do miolo da casa ficavam viradas para o norte,
  procurando apoio num lado onde só havia ar. Agora a peça de parede que não
  se sustenta na direção deduzida se apoia na parede que existe.
- **Peça adiada por uma versão anterior volta à fila** quando já cabe, sem
  esperar a vizinhança mudar. É o que destrava o save em que a obra já está
  presa.
- **A placa conta as peças esperando apoio** ("Obra: 9 blocos · 9 sem
  apoio"), em vez de parecer que a obra voltou atrás.

### Ainda assim

- A direção de verdade (a tocha do lado que o arquivo da estrutura manda)
  continua perdida na leitura da planta: a peça agora se apoia numa parede
  que existe, que pode não ser a do desenho original.
- Peças riscadas (tocha sem carvão pela barreira de teste, bloco no caminho)
  voltam como pendentes a cada carregamento do mundo, porque a retomada relê
  o mundo. Com as peças de parede destravadas a obra fecha; o riscado vira
  lacuna, como antes.

## [0.3.0] — publicação de 2026-09-25, tarde

Correções do que a sessão de jogo de 25-09 (01:19–01:45) mostrou.

### Corrigido

- **Fechar o mundo derrubava a limpeza da memória do servidor**
  (`ConcurrentModificationException` em `ServerMemory.resetAll`): uma limpeza
  carregava outra classe, que se inscrevia no meio da volta, e o resto ficava
  sem limpar. A mesma chamada roda ao abrir o mundo. Agora a volta é sobre uma
  cópia, repetida até ninguém novo aparecer.
- **A placa da obra diz o bloco que realmente falta**, com o nome em
  português que o jogo dá: o do próximo bloco em que o construtor parou, se
  os baús não o têm; senão, o primeiro material em falta; e, sem nada em
  falta, só a contagem de blocos. Antes ela mostrava o primeiro material da
  planta, e no templo de 25-09 podia anunciar o pedregulho (34 no baú)
  enquanto a obra esperava tocha.

### Medido em jogo

- Perfil do spark da sessão: TPS 20 em todos os 22 minutos, MSPT mediano de
  14 a 16 ms. O mod caiu de 4,1% para 0,3% da thread do servidor, e o
  `nearestFirst` de 332 ms para 16 ms — com a ressalva de que o construtor
  passou boa parte da sessão parado esperando tocha.

## [0.3.0] — publicação de 2026-09-25, madrugada

### Adicionado

- Cache de um segundo na varredura dos baús livres da vila
  (`VillageChests`). O perfil do spark de 2026-09-24 apontou
  `ColonyChests.nearestFirst` como o maior custo do mod nos ticks lentos. Baú
  posto ou quebrado invalida o cache na hora; nome, BigHouse e chunk
  carregado são conferidos a cada leitura.
- `ServerMemory`: cada classe com estado de servidor se inscreve sozinha, e o
  ciclo de vida esquece tudo por `resetAll()`. Achou uma limpeza que nunca era
  chamada (`BiomeConstructionSupply`) e duas listas de limpeza divergentes.
- `CHANGELOG.md`.

### Corrigido

- O PIT não rodava desde o `78e7efc`: um teste do `core` que sobe o jogo
  derrubava a rodada inteira, e o CI escondia a falha. O teste saiu da
  rodada do PIT, e o CI agora reprova quando o PIT nem começa.
- `./gradlew javadoc` voltou a passar (tinha 6 links quebrados). Os 32 avisos
  de javadoc do Error Prone foram zerados; vários eram comentários separados
  do método pela divisão de classes.

### Testes

- Mutação (PIT, `core`): de 78% para **87,7%** das mutações mortas (1151 de
  1312), força de teste 95%. `MineShaft`, `Building`, `ColonyCycle`,
  `Worker`, `ProfessionAssigner`, `Mine`, `ColonyGoals`, `BuildingRegistry`,
  `ConstructionProject`, `ColonyRoads`, `VacancyEnforcer` e `HiringLog` ficaram
  sem sobrevivente, ou só com equivalentes.
- Três testes que passavam sem medir nada foram refeitos: o cancelamento de
  pedido no `ColonyCycle`, o adiamento de peça no `ConstructionProject` e o
  recentrar das ruas no `ColonyRoads`.
- GameTest novo para o cache de baús, confirmado por mutação.

### Não verificado

- Nada desta publicação foi visto em jogo. O próximo perfil do spark diz se o
  cache baixou o custo da varredura.
