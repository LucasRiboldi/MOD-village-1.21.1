# ADR-036 — Decisões do autor de 2026-10-06

**Status:** Accepted
**Date:** 2026-10-06
**Decision Type:** Gameplay / Process
**Origem:** a lista de 31 decisões levantada depois da ADR-035, respondida pelo
autor no mesmo dia, mais quatro esclarecimentos (pedreiro, felicidade, mina,
limpeza do save).

## Decisões

Numeração da lista apresentada ao autor. "Código" diz o que muda; "Nada"
registra decisão que confirma o que já existe.

### A. Projeto

| # | Decisão | Código |
|---|---|---|
| 1 | Juntar o PR #9 na `main` | merge pelo autor (o modo automático não permite merge sem revisão) |
| 2 | Apagar as branches antigas | pelo autor; `codex/bighousemod` fica: o `Barn_Majest.nbt` dele difere do da integração |
| 3 | Versão 0.3.1 | `gradle.properties` |

### B. Regras do jogo

| # | Decisão | Código |
|---|---|---|
| 4 | Toda casa — da vila original ou construída — tem baú ao lado da cama, **encostado na parede**, nunca na frente de porta ou escada | colocação do baú de cama |
| 5 | **Desfaz a Regra 25** (a maior planta que couber) | ordem das plantas deixa de ser por tamanho; a primeira casa continua a menor |
| 6 | Peça que depende de profissão: na **4ª** tentativa sem sucesso aparece **no baú da profissão** e o erro vai ao log. Peça cuja profissão devolve erro **5 vezes** é pulada | suprimento da obra e barreira |
| 7 | Os conflitos regra × código são listados e decididos pelo autor, um a um | lista na reanálise |
| 8 | Pode haver desempregado. **O pedreiro não fica parado:** sem pedido, faz estoque de peças de pedra da vila (degrau, laje, tijolo) até um mínimo | meta de pedra lavrada |
| 9 | Todo baú guarda no máximo **3 compartimentos do mesmo item**. Baú lotado: os últimos **10 compartimentos** vão para um baú **sem profissão** livre da vila. Sem baú livre na vila, a obra prioritária é um **`storage_majest`** | baús e planejador |
| 10 | Vila abandonada **para de trabalhar**; as profissões dela se desativam; o conteúdo dos baús fica | ciclo de vida da colônia |
| 11 | A vila só trabalha com o jogador **dentro dela há mais de um minuto** | foco da vila |
| 12 | Limpar o mundo de teste: **só os dados do mod**, com backup antes | fora do repositório |
| 13 | Combustível da fornalha e mudas do viveiro **surgem do nada** | Nada (ADR-028) |
| 14 | Baú da vila fora de casa **é** da colônia | Nada (Regra 45) |
| 15 | Aldeão preso que tenta se soltar **5 vezes** sem sair do lugar, ou que à noite não acha caminho para casa, **é teletransportado para a cama dele** | fuga |
| 16 | Levar material para perto do construtor: já funciona | Nada |

### C. Mina e buscas

| # | Decisão | Código |
|---|---|---|
| 17 | No fundo, a mina anda para o lado **oposto ao centro da vila** e abre uma **rampa que sobe** no mesmo ritmo da descida, minerando no caminho | geometria da mina |
| 18 | Busca de recurso vai até **10 blocos além da borda** da vila, **alternando** uma busca a partir do centro e outra a partir de uma borda | varredura |
| 19 | Blocos com mais de uma posição (eixo, metade, formato) saem **na posição da planta** | construtor |

### D. Ideias

| # | Decisão | Código |
|---|---|---|
| 20 | **Felicidade** medida na reunião da vila (30 s): comida por pessoa, camas sobrando, obras concluídas. Feliz → mais filhos; infeliz → nenhum filho novo. Aparece no `/vc log` | ciclo da colônia |
| 21 | Fornalha de verdade: **não por enquanto** | Nada |
| 22 | Colônia decide pela cadeia de fontes: **depois** (ADR-035 §3, fase 2) | Nada |
| 23 | O mineiro **recolhe o veio inteiro** | mineração |

### E. Arrumação

| # | Decisão | Código |
|---|---|---|
| 24 | Arquivar documentos antigos (`git mv`, histórico preservado) | docs |
| 25 | Apagar `agent/` e `mods/` | repositório |
| 26 | Tirar o site e o JAR do repositório; JAR vai para a página de versões do GitHub | repositório |
| 27 | Apagar testes que só conferem texto de mensagem | testes |
| 28 | E43 (descanso vale sempre): **o agente avalia** | reanálise |
| 31 | Viveiro **só do lenhador** | ADR-020 emendada |

## Consequências

- Cada item de código é um commit com teste que falha antes; GameTest quando
  depende do mundo; nada disso é visto em jogo até o próximo playtest.
- A Regra 25 sai da tabela de regras; a Regra 8 é reescrita (item 4); o
  número de tentativas da ADR-022/ADR-034 passa de 3 para 4 (item 6).

## Estado

Preenchido a cada item aplicado.

| # | Commit | Verificação |
|---|---|---|
| 1 | `454ab29d` | PR #9 juntado na `main` pelo autor; o #10 (esta ADR) em seguida, `1a77332a`; CI verde nos dois |
| 2 | fora do código | apagadas, remotas e locais, as branches inteiras na `main` (`claude/auditoria-recuperacao`, `claude/corrigiveis-sem-jogo`, `claude/decisoes-2026-10-06`, `codex/village-visuals-logistics-mine-sweep`, `integra/linhas-2026-10-06` e duas de worktree). Ficam `codex/bighousemod` e a branch local do worktree do Codex |
| 3 | `53a637d1` | versão 0.3.1; JAR `2C2F849F…3180` em `downloads/` e na pasta `mods` |
| 4 | `36891a42` | baú de cama em toda casa; 2 GameTests, escada por mutação |
| 5 | `55ed3b2d` | Regra 25 desfeita; HousePlansTest 27/27 |
| 6 | `dcaed4ac`, `afce8d76` | 4 tentativas no baú da profissão; 5ª falha ao pôr pula; barreira removida |
| 8 | `18c1bef5` | estoque do pedreiro; pedido nominal protegido no ciclo |
| 9a | `73a40c75` | teto de 3 compartimentos por item no depósito e na leitura; baú nomeado sem teto; 615/615 ×3 |
| 9b/9c | `22f6fd0d` | alívio dos 10 últimos compartimentos respeita o teto no destino; sem baú livre a colônia fica marcada e a próxima obra é o `storage_majest` (prova por mutação); 617/617 ×3. Conflitos para o item 7: o armazém de emergência da mina (Codex) continua como socorro até o armazém subir; o teto de 256 por tipo do mineiro (`MinerHaul.TYPE_CAP`, 4 compartimentos) fica abaixo do novo teto de 3 compartimentos e na prática vale 192 |
| 10 | `a3cbe44e` | vila abandonada para; dispensa provada por mutação |
| 11 | `1bb32380` | foco com 1 minuto de presença |
| 12 | fora do repositório | dados do mod apagados do "Novo mundo", backup em `.minecraft/saves-backup` |
| 15 | `65e63f03` | `BedTeleport`: 5 trocas de jeito da subida no mesmo lugar (raio 1,5) ou, à noite (`WorkClock.isRestTime`), cama sem caminho (`NightHome`, a cada 30 s por aldeão) levam o aldeão para a cama da memória HOME; sem cama conhecida, a subida continua. 3 GameTests com controle; 620/620 em 8 de 9 baterias (a 9ª: KF-003) |
| 17 | `e2a09ff2` | `MineShaft.ramp`: no fundo, a mina sobe do piso do salão em degraus de 3×3, um para a frente e um para cima, no rumo oposto ao centro (`Side.awayFrom`), até a altura do centro; um ramal só; save com a chave `ascent` (sem trocar `SHAPE_VERSION`). Terminada a rampa, a boca nova abre do lado para onde ela subiu. Na rampa o armazém de emergência da mina fica desligado (o índice dele cairia num degrau). Mutação prova o GameTest; 621 ×3 (uma rodada com KF-003) |
| 18 | `45d4f116` | `ResourceReach` (core): limite = caixa da vila + 10; vez par parte do centro, ímpar do meio de uma borda (N, L, S, O). Ligado ao lenhador (`TreeChoice`) e à areia (`SandGathering`, a vez só passa com a varredura inteira); a coleta de superfície usa só o alcance novo (a varredura dela já anda pela caixa). Sem caixa medida, raio escolhido no menu ou encurtado por teste: como antes, do centro. 622 ×5 sem falha |
| 19 | `316cca56` | `ShapeStates` (core, texto): `axis`, `half` (top/bottom), `type` de laje (top/bottom/double) e `shape` de escada passam da paleta (e do `final_state` do encaixe) à colocação; giro de 90° troca eixo X↔Z. `BlockShaping.withPlanStates` aplica antes do `facing` da parede; propriedade que o bloco ou o substituto não tem fica como estava. Fica de fora: trilho (o formato gira diferente), metade de porta e tipo de baú (têm dono). Laje dupla custa 1 laje. ADR-005 emendada por este item. 624 ×3 (uma rodada com o intermitente do fundidor, TODO 🟡) |
| 20 | `8a84a87c` | `VillageHappiness` (core): comida por adulto (pontos do Vanilla nos baús) ≥12 soma, <4 tira; camas sobrando ≥2 soma, ≤0 tira; ≥1 obra concluída soma; ≥2 feliz, ≤−1 infeliz. **Limiares escolhidos pelo agente — o autor revisa.** `VillageMood` na reunião (ciclo de 30 s): infeliz segura a espera de reprodução dos adultos em ≥1.200 tiques; feliz corta a espera pela metade e passa 1 pão do baú a quem tem <12 pontos no bolso. Linha "Felicidade" no `/vc log`; log só na troca de humor. 626 ×5 (uma com FarmPlan intermitente, KF-003) |
| 23 | `19fba971` | `MineVein`: o veio é seguido em profundidade — rastro dos minérios já cavados por ramal (teto 256, em memória); sem vizinho no último, volta pelo rastro atrás das ramificações. Veio novo achado pelo túnel recomeça o rastro. As guardas de antes (sem onde ficar de pé, fluido, marca, degrau) continuam encerrando o veio. Mutação prova o GameTest; 627 ×3 |
| 24 | `8400134f` | `git mv` conforme `docs/audit/RECOVERY-ROADMAP.md` §3: design de agosto, `AUDIT_REPORT`, `docs/historical/` e `claude/` para `docs/archive/design-2026-08/` (`claude/CLAUDE.md` virou `CLAUDE-antigo.md`, para não ser lido como instrução aninhada); `Development-Log`, `Project-State`, `Historico-2026-09`, `Backlog`, auditorias, revisões, E45/E46 e guias de setup para `docs/archive/technical/`. Aviso HISTÓRICO no topo de cada um; links dos documentos vivos corrigidos. 1366 unitários e 95 Python ok |
| 25 | `edfde4fd` | `agent/` (53 arquivos, cópia divergente de `.claude/skills` que nenhuma ferramenta carrega) saiu do git; `mods/` estava vazia e fora do git, só apagada. O histórico guarda os dois |
| 26 | `c508f42c` | `Website/` e `downloads/` saíram do git (`git rm --cached`, ficam na máquina do autor) e entraram no `.gitignore`; README aponta para a página de versões. A versão `v0.3.1` com o JAR final é criada como **rascunho** — publicar fica com o autor, porque nada desta branch foi visto em jogo. A Regra 33 (JAR em `build/libs` → `downloads/` → `.minecraft/mods`) segue valendo como cópia local |
| 27 | `abc9a4d5` | Apagados os testes que só conferiam a redação de linha de log: `LumberjackGiveUpReasonTest` e `SmelterReasonTest` inteiros; 3 métodos de `DrawnFromTest`, 3 de `ChestSurveyCoverageTest`, 1 de `HiringLogTest`; asserções de texto tiradas de `theFirstTimeSpeaks`, `LentHandTest` e de 3 métodos de cobertura (ficam as de contagem e estado). Ficam de propósito: `CycleCostTest` (a linha `VC_COST` é lida pelo `cost_ledger.py`), `SiteLabelTest` e `VillageLogPresenterTest` (texto que o jogador vê). Unitários 1366 → 1348, 0 falhas; PIT 86% (limiar 85) |
| 28 | `d2ac9bb4` | **Avaliado: o descanso vale sempre, e fica.** A 2ª passagem do `takeOneTask` citada em `RULES.md` não existe mais: toda reserva passa por `WorkAssignment.reserveOne` → `WorkEligibility.canReserve`, que recusa capacidade em descanso, e nenhum outro código reserva tarefa. Custo aceito: sem outra tarefa, o mineiro que desistiu espera 4 ciclos (2 min) em vez de voltar ao alvo que acabou de recusar — o laço de E44/E45. `RULES.md` e a tabela de decisões do `TODO.md` atualizadas (com TASK-048 e TASK-046, fechadas pelos itens 10 e 19) |


## Conflitos para o autor decidir (item 7)

Achados ao aplicar os itens; cada um ficou como descrito, até o autor dizer o contrário.

| # | Conflito | Como ficou |
|---|---|---|
| C1 | Armazém de emergência no salão da mina (linha Codex) × `storage_majest` do item 9 | os dois: a emergência socorre até o armazém subir |
| C2 | Teto de 256 por tipo do mineiro (`MinerHaul.TYPE_CAP`, 4 compartimentos) × teto de 3 compartimentos | vale o menor, 192 por tipo no baú do mineiro |
| C3 | E47, "nenhum teletransporte" × item 15 | o item 15 vence nos dois casos que ele nomeia; fora deles, a fuga continua cavando |
| C4 | ADR-005, o Core só guarda `facing` × item 19 | ADR-005 emendada: eixo, metade e formato entram como texto (`ShapeStates`) |
| C5 | Rampa do item 17 × leituras pela boca da mina | na rampa, o baú antigo da boca e o armazém de emergência saem da conta da colônia (o conteúdo fica no mundo) |
| C6 | Item 18 × varredura de superfície da linha Codex (já anda pela caixa) | na superfície só o alcance mudou; a alternância centro/borda vale para lenhador e areia |
| C7 | Item 20 não deu limiares | o agente escolheu (12/4 pontos de comida, 2 camas, 1 obra; soma ≥2 / ≤−1) — revisar |
| C8 | Item 26 × nada desta branch visto em jogo | a versão `v0.3.1-alpha` foi publicada como pré-lançamento, a pedido do autor ("merge total") |
| C9 | Itens 1 e 2 (merge do PR #9, apagar branches) | feitos: ver a tabela Estado |
