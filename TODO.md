# TODO

## Playtest de 03-11 — fila de construção

- [x] Isolar a busca da casa do mineiro, preservando índice, cursor geral e pontas de rua.
- [x] Liberar outras obras somente após volta local completa sem lote; manter a casa pendente.
- [ ] Confirmar no próximo playtest ausência das falsas derivas e progresso de outras obras.
- [ ] Quando os testes forem liberados, cobrir busca parcelada, preservação do índice/cursor
      e liberação da fila após volta sem lote no Sentinela Local.
- [ ] Seguir com navegação e profissões antes da integração supervisionada MAE (ordem A, depois B).

## MAE — Fases 1 e 2

- [x] Escolher prova na geração, sem autorização retroativa por carga ou origem da vila.
- [x] Criar reserva supervisionada e aprovação individual reaproveitando `Building`.
- [x] Persistir intenção, progresso e histórico limitado sem migrar terreno legado.
- [x] Aplicar proteção, pré-condições, orçamento, pausa e retomada na escrita MAE.
- [x] Concluir a Fase 1 e integrar pelo PR #48; atualizar a base local antes da Fase 2.
- [ ] Medir custo de captura e escrita em carga real antes de ativar orquestração.
- [x] Apresentar a Fase 2 e concluir o diagnóstico autorizado, sem implementar terreno.
- [x] Escolher alternativa A e preparar desenho de integração local com vegetação baixa controlada.
- [x] Rever desenho a pedido do autor: conexão a âncora de rua comprovada e transitabilidade de toda superfície preparada.
- [x] Ampliar desenho da prova a pedido do autor: reservar antes da rua e encadear alterações certificadas no diário MAE.
- [x] Aprovar integração supervisionada dos sete pontos de rua, publicação após confirmação e limites da prova; sem certificação retroativa.
- [x] Aprovar limites e desenho em `docs/superpowers/specs/2026-11-02-mae-terrain-design.md` (alternativa A, 02-11).
- [x] Registrar o desenho ampliado aprovado na ADR-045 e no índice real do README.
      Registro formal consolidado durante a revisão da implementação.
- [x] Integrar a Fase 1 pelo PR #48 antes de abrir `codex/mae-terrain`.
- [x] Concluir validação focada e revisão do gate da Fase 2: 37 unitários/estáticos
      e 48 GameTests passaram; limites e evidências em `docs/technical/MAE-Fase-2.md`.
- [x] Integrar a Fase 2 pelo PR #49 (`0a7590fc`).
- [ ] Validar carga real e playtest antes de qualquer orquestração automática MAE.

## MAE — Fase 3

- [x] Auditar plantas, construção, terreno e limites de integração.
- [ ] Validar acesso físico completo, modelos e dados de block entities.
- [ ] Integrar reserva, preparação, construção e retomada após cumprir os gates da Fase 2.

## Playtest de 02-11 — escolhas A aprovadas

Plano: [terreno, mina e profissões](docs/technical/Plano-Playtest-2026-11-02.md).

- [ ] Diagnosticar custo de geração e recalculo; processar lotes por tick.
- [ ] Conectar pórtico ao grafo, smoothing local e casa do mineiro com tocha laranja.
- [ ] Garantir reserva exclusiva por bloco e frente exclusiva por mineiro.
- [ ] Preferir terra e usar salto maior somente em recuperação.
- [x] Suspender ajudas cruzadas de pedreiro e fundidor no despacho `VillageHelpers`
      (0.4.8 candidata; falta playtest).
- [ ] Ao liberar testes, cobrir a suspensão no despacho `VillageHelpers.helps`;
      os GameTests legados chamam rotinas auxiliares diretamente e não provam essa restrição.
- [ ] Restringir alternativas ao ofício; fazendeiro atende todas as fazendas,
      lenhador planta nas bordas sem árvore madura disponível.
- [x] Gerar JAR candidato 0.4.8 e copiar para `downloads/` e TLauncher com SHA-256 idêntico.
- [ ] Validar a separação dos ofícios no próximo playtest e concluir os demais itens do plano.

## Sentinela local de testes — 01-11-2026

- [x] Criar varredura manual com perfis `quick`, `fast` e `full`, logs e
      relatórios por execução; não inicia sozinha e não muda CI.
- [x] Corrigir cópia de artefatos por árvore de arquivos e permitir retomada de
      execução interrompida, sem executar Gradle durante o modo de playtest.
- [x] Criar dois testes Python pequenos para cópia recursiva e `--resume`; ambos
      passaram sem iniciar Gradle.
- [x] Criar aplicativo Windows `Sentinela/Abrir-Sentinela.cmd`, sem dependências
      extras, para escolher perfil, repetição, simulação e retomada localmente.
- [x] Criar o Observatório local: seleção de suites, orçamento seguro, painel
      HTML, agrupamento de falhas, testes lentos e histórico local.
- [x] Criar análise manual de execuções, painel separado e métricas locais
      opcionais; não inicia Gradle e tolera GPU NVIDIA indisponível.
- [x] Criar tutorial operacional do Sentinela e regra de atualização conjunta.
- [ ] Validar uma futura camada visual de cliente/GPU em spike Fabric isolada,
      no perfil `village-max`; não entra no fluxo diário sem prova reproduzível.
- [ ] Avaliar publicação opcional Allure entre máquinas após necessidade real;
      plano em `docs/superpowers/plans/2026-11-02-sentinela-futuro.md`.
- [ ] Avaliar laboratório serial de carga/JFR após definir orçamento de máquina;
      plano em `docs/superpowers/plans/2026-11-02-sentinela-futuro.md`.

## Rede e currais — implementação 0.4.3 em 02-11-2026

- [x] Grafo puro e codec paralelo inicial, com testes de topologia, compatibilidade e corrupção.
- [x] Ativar grafo em vilas novas após converter o índice legado, sem alterar vilas existentes.
- [x] Criar curral grande único de até 20 animais, com água, cultivos, grama, portão e tochas;
      manter currais pequenos e cercados legados utilizáveis.
- [ ] Explicar a falha intermitente de `PenEscape` antes da integração da rede e da condução.
- [ ] Explicar o desvio intermitente pela água em `ColonyPathing`; o controle A/B/A não reproduziu a rota defeituosa.
- [ ] Revalidar ruas e migração no mundo novo antes de declarar o planejamento hierárquico concluído.
- [ ] Provar condução física de pastor e animal; atualizar modelo, cultivos e crescimento local do curral.
- [x] Registrar a bateria anterior, documentação e JAR local 0.4.3; falta playtest em mundo novo.

Detalhes nos planos de [rede](docs/superpowers/plans/2026-11-01-hierarchical-roads.md)
e [currais](docs/superpowers/plans/2026-11-01-pens.md). Não representam gameplay concluído.

## Diagnóstico de terreno e trabalho — 01-11-2026

Fila priorizada, evidências e critérios de fechamento:
[relatório de falhas](docs/audit/2026-11-01-falhas-priorizadas.md), categoria 1.9.

- [ ] Reproduzir giro do mineiro e retorno repetido do lenhador com navegação concorrente,
      acesso obstruído e retomada após areia. Testes de descida e colheita completa reforçados
      em 01-11 passam; não demonstram a causa do defeito observado no save.
- [ ] Confirmar no playtest a correção candidata: mineiro só libera alvo após falha de quebra;
      fundidor retorna à área construída; ajuda comum espera 1.200 ticks de ociosidade própria;
      folhas e troncos não servem de piso; e lote novo limpa vegetação e árvores na margem de um bloco.
- [ ] Investigar o reprodutor forense preservado em patch diagnóstico: caminho calculado não se traduz em chegada
      física em 140 ticks, inclusive com WorkTargets ativo no expediente. Separar geometria,
      tolerância de chegada e interferência do Brain antes de alterar o código de produção.
- [ ] Investigar F19: condução da vaca ao cercado falhou no CI de push 41254270066,
      mas passou no CI de PR do mesmo código e nos 955 GameTests locais de 01-11.
- [ ] Investigar a intermitência de `GroundPickupGameTest.aPassingWorkerPicksUpAllGroundItems`:
      a bateria da 0.4.1 falhou em 1/979, mas a repetição isolada passou. Não houve
      alteração de produção para este sintoma.
      O CI 552 encontrou terra zero no baú original. Depósito em segundo baú reproduz
      o erro, mas o evento original ainda não foi rastreado. O teste agora registra
      remoção dos itens e estoques; conferir a próxima execução completa do CI.
- [ ] Confirmar no CI completo a fixture hidratada de `BuilderGameTest.groundBlocksAreShapedWithoutTheChest`;
      desidratação reproduziu a falha e os 35 testes isolados passaram após a correção.

- [ ] Jogar o novo JAR em mundo novo e no save `Novo mundo`: medir grama flutuante,
      transição do lote em degraus de 1 bloco e apoio sob o piso; conferir cortes em dunas e estruturas.
- [ ] Observar pastor junto do animal ao alimentar e abater, reserva de 6 adultos por espécie,
      currais intercalados de 15×15 e cercados existentes aproveitados.
- [ ] Confirmar limpeza de árvores e copas nos novos lotes, folhas removidas durante a passagem,
      itens recolhidos a 5 blocos e reposição de 16 produtos por tipo nos três ofícios.
- [ ] Comparar o novo diário de profissões e Spark com o jogo de 31-10: pastor 3%, carpinteiro 11%,
      pedreiro 15%, construtor 46% trabalhando; investigar esperas sem matéria-prima ou caminho.
- [ ] Validar entrada da mina e ruas desconectadas: caminho natural quando o trecho da rede não for alcançável.
- [ ] Conferir produção contínua e reposição de 16 itens por tipo nos três ofícios, incluindo produtos de fornalha;
      manter prioridade das obras e baús nomeados reservados.
- [ ] Repetir diário de trabalho: distinguir espera real de trabalho auxiliar sem marcação e medir NO_PATH/NO_PROGRESS.

**Atualizado:** 2026-11-01 (diagnóstico de terreno e trabalho; histórico de biomas: 2026-10-20). Só o que está **aberto**. Os checklists de playtest de
versões ≤0.3.44 foram arquivados em `docs/archive/technical/TODO-ate-2026-10-07.md` (grep, nunca
inteiro). Estado vivo: `STATE.md`. Plano de melhoria: `docs/technical/Plano-Melhoria-2026-10-14.md`.

Prioridade: 🔴 bloqueia · 🟠 importante · 🟡 melhoria · 🟢 futuro.

## Produção futura (estudo "Regras v2", 2026-10-22)

Guardado a pedido do autor para as próximas versões. Avaliação ponto a ponto em
`docs/research/2026-10-22-avaliacao-regras-v2.md`; numeração 1–112 mantida.

- [ ] 🟠 **Configuração** (R-153/154) — desenho com decisões do autor em
      `docs/research/2026-10-22-desenho-configuracao.md` §8: por mundo, sobreposição por colônia
      (colônia → mundo → padrão), arquivo `saves/<mundo>/villagecolony/config.json` + `/vc config
      list|get|set|reset|reload`, operador nível 2, `VC_CONFIG` a cada mudança. F1 = tabela da §2
      inteira (14 ajustes) até o autor dizer outra coisa. Falta implementar.
- [ ] 🟡 **Ausência com invalidação por evento** (R-021/R-027): `KnownAbsent` só expira pelo relógio
      (1 dia); invalidar quando o alcance cresce, aparece rota nova ou o terreno muda.
- [ ] 🟡 **Contrato de tarefa** (R-047): prazo de progresso e critério de sucesso declarados na `Task`;
      centralizar os prazos hoje espalhados por ofício (R-050).
- [ ] 🟡 **Estados de disponibilidade** (R-019): disponível, inacessível, não encontrado,
      temporariamente indisponível, não examinado — hoje espalhados.
- [ ] 🟡 **Bateria de escassez** (R-158): falta total de madeira, água, semente, animal, combustível,
      ferramenta, baú livre e lote; esperado: alternativa válida ou bloqueio explícito.
- [ ] 🟡 **Animal inacessível não conta como plantel** (R-107).
- [ ] 🟡 **Cobrir com teste o registro de cada suprimento** (Regra 111): só a fundação tem asserção.
- [ ] 🟢 **Área protegida pelo jogador** (R-035) — sistema novo; sem decisão.
- [ ] 🟢 **Transporte físico de itens** (R-140 a R-142) — hoje a transferência entre baús é
      instantânea; sistema novo e grande; sem decisão.
- [ ] 🟢 **Itens de outros mods** (R-026) — sem decisão.
- [ ] 🟢 **Adaptador para versões futuras do Minecraft** (R-003/R-163) — o Core já não depende do jogo;
      só com ambiente da versão.

## 🔴 Biomas — o mod em todos os biomas (bancada de 20-10, 0.3.69–0.3.76)

Medido em `scripts/biome_sim.py` (uma vila por bioma, 2 dias, margem ±1–2 pontos; régua em
`docs/testing/roteiro-por-bioma.md` §5). Notas atuais: savana 9,3 · meadow 9,0 · planície 7,2–9,9 ·
neve 7,2 · taiga 6,8–8,8 · deserto 5,3–5,8 · taiga nevada 4,2 · floresta 4,0 · cerejeira 3,0 ·
ermos 3,0. **Nada disto foi jogado** — o próximo passo de verdade é jogar pelo roteiro.

Feito neste ciclo (não repetir): vila espalhada e vila na borda de bioma viram colônia (ADR-003
Emenda 9); casa fundadora com busca maior, aplainamento e limpeza de mata; G2 (peça que trava
surge no baú, registro `villagecolony/supply-gaps.jsonl`); caminho com o bloco do bioma; obra
comum limpa lote na mata; mineiro escora areia; terraplanagem do lote fase 1 (±2); rotas
próprias (madeira e argila com prova, descascar, ingrediente de superfície); `/vc bioma`,
`VC_BIOME`, `biome_report.py`, `BiomeCoverageGameTest`.

- [ ] 🟠 **Casa presa no último bloco — falta jogo.** Taiga: 44 ciclos em "1 blocks left". A causa
      vista na bancada era a copa de pinheiro de fora do lote: 29 peças de parede puladas por
      "spruce_leaves is in the way" e a tocha da parede sem apoio. 0.3.77: folha natural dá lugar à
      peça; a peça inalcançável surge depois de 3 vezes de lado (`UnreachablePieces`,
      `villagecolony/unreachable-pieces.jsonl`, `VC_UNREACHABLE`). Bancada: taiga 0 → 2 casas
      concluídas, nota 7,6. Pendente: tronco natural no volume ainda é pulado ("spruce_log is in the
      way", 3×); peça de lado por apoio (`UNSUPPORTED`) que nunca ganha apoio ainda segura a obra.
- [ ] 🟠 **Deserto jogado (0.3.77, 20-10, Spark IgpFCEon8m): 0 obras em 2,8 dias.** 0.3.78 corrige três
      causas medidas na bancada com a semente do autor (`--near=-460,601`): a rua de arenito liso da
      vila gerada não era rua (só no índice, que só se enche de rua), a busca de rua só via +2 do
      centro, e a duna acima da rua não era cortada (corte até 5). Bancada: 0 → 4 casas, nota 5,1 → 8,8.
      0.3.84 (deserto jogado na 0.3.83, nota 7,8, Spark 5tus3Etvsq): fundidor atende já a peça de
      fornalha que a obra espera (arenito liso 67× de lado); ferro descansa um dia onde a mina não o tem
      (IronRest); rede da mina menor por nível (1º raio 2, demais 3; antes 6) para descer mais rápido;
      chão firme da vila de areia (2 camadas abaixo da superfície de areia viram arenito, DesertGround).
      0.3.85 (autor, opção 1): o lote aplainado ganha um corredor de 2 blocos em volta da casa, cortado
      até a base (só chão do bioma; nunca rua, peça da vila, obra ou beira d'água; não aterra). Bancada:
      9,4 / 9,1 / 8,9, a casa do buraco (148,78,-507) fecha; desistências de lado 570 → 63–107.
      0.3.83 (autor, 21-10: "mudar de casa"): quem desiste do trabalho morando 2+ abaixo dele passa a
      dormir numa cama livre da vila na altura do trabalho, ou troca de casa com um aldeão sem ofício
      que mora lá (HomeMove); à noite o jogo já o leva para a cama. Bancada: 9,3 / 9,3 / 5,9 — na de
      5,9 todas as camas de cima eram de aldeões com ofício e o construtor não tinha para onde ir.
      Pendente 🟠: esse caso (decisão do autor: trocar com quem tem ofício? construir casa em cima?).
      0.3.93 (avaliação do "Regras v2"): 74 transacional, 110 (itens não somem), 21/32 (peças funcionais),
      111 (lista de suprimentos), 112 (códigos de bloqueio). **Falta jogo.**
      Pendente 🟠: decisões do autor para a configuração (escopo, por colônia, edição, lista da F1).
      Pendente 🟢: área protegida (R-035), transporte físico (R-140–142), outros mods (R-026) — futuro.
      0.3.92: Regra 74 — cama mais perto do trabalho (BedPlan/BedChoice, 1×/dia ao amanhecer, troca se os
      dois ganham). **Falta jogo.** Pendente 🟡: o carpinteiro, o pedreiro e o fundidor não marcam lugar no
      diário (CRAFTED sem posição), então ficam onde estão.
      0.3.91 (deserto jogado na 0.3.90, nota 7,6, Spark LYMx4yHjKk): a faixa deu lote (curral, 486 blocos
      cortados, caminho de areia). Corrigido: a busca com corte travava 3 s (memória por passo, teto de
      50 ms); a bateria agora começa com o mundo limpo.
      Pendente 🟠: os mineiros andam 36% do expediente da vila alta até a mina do lago (9 desistências a
      32–61 blocos).
      Pendente 🟡: o `storage_majest` pede deepslate, calcita, folha de acácia e madeira de pinheiro no
      deserto (55 peças deixadas de lado).
      Pendente 🟡: espera de 42% do fazendeiro e de 41% do fundidor.
      0.3.89 (deserto jogado na 0.3.88, nota 4,0, Spark YFIroI1U1x; escolhas do autor de 22-10):
      - alcance do fazendeiro com a borda;
      - mineiro nada ao ar livre;
      - corredor corta degraus na duna (vale de 3, até 12 de fundo);
      - rebanho 8/6, lotação pelo tamanho, curral novo com todos cheios;
      - caminho de areia (Regra 73).
      **Falta jogo.** Pendente 🟡: KF-009 (MineReturn 1 em 4 baterias); o `PenPlans` não tem GameTest.
      0.3.88 (deserto jogado na 0.3.87, nota 4,9, Spark CnNU79zqZ5): Regra 71 (mineiro não constrói
      fora da montanha, desce antes de ir de lado, acesso cavado), Regra 70 (corredor de caminho), a faixa
      continua esperando lote. **Falta jogo.** Pendente 🟡: caminhos em mancha já calçados não foram
      desfeitos; a extensão de rua e o recuo de 3 não passam pelo `Corridor` (já são retos).
      0.3.87 (deserto jogado na 0.3.86, nota 3,7, Spark j7jTrVIaPQ; decisões do autor de 22-10): encalhe
      sem vaivém (BedTeleport 4,5), trigo pela semente mais rara e reserva de semente, mina insiste em 5
      pontos e só muda para boca alcançável, lote no fundo da faixa com caminho pelo terreno (A5, Regra 56).
      **Falta jogo.** Pendente 🟡: o fundidor larga a fornalha a cada ~30 s com o pedido ao pedreiro aberto
      (~7% do tempo dele; cooldown por tarefa); D2 ainda não exercido (a sessão não teve ceia).
      0.3.86 (deserto jogado na 0.3.85, nota 6,1, Spark IAXTyE0LXM; estudo em
      `docs/research/2026-10-21-deserto-ociosidade-alternativas.md`): a vila ficou 34 de 57 min sem
      obra porque **não havia lote** (a busca termina as voltas; não é falta de prazo). Feito: A1 (a
      rua em crescimento que para registra o motivo — a da faixa anexada parava em silêncio), C1 (o
      mineiro que chega ao lugar de trabalho alcança a pedra; antes ficava a 4,1 com alcance 4), B1
      (sem arenito, o pedreiro o faz da areia da vila para o fundidor), D2 (a ceia deixa um par da
      comida do rebanho). **Falta jogo.** Pendente 🟠: com o motivo da rua em mãos, A2 (rua da faixa
      com alternativas) ou A3 (aterro além de 2); decisões do autor A5 (lote e rua juntos na faixa),
      F2/G1 (lenhador e sem ofício ajudando a obra); D1 (o trato do rebanho contar como trabalho).
      0.3.82 (autor, 21-10, depois de jogar a 0.3.81): a escada fixa e a subida por pilar saíram —
      "quebra o visual", "a movimentação precisa ser a mais natural possível". Só o mineiro cava
      (degrau, túnel) para subir; os outros, só degrau natural (regra de 15-10 de volta). Pendente 🔴:
      quem mora na parte baixa da vila do deserto volta a não chegar à obra de cima (bancada: 0 obras
      em 2 rodadas com o construtor morando em y63) — falta uma saída natural, decisão do autor.
      0.3.81 (deserto jogado na 0.3.80, nota 8,2, Spark ETMetpWVS9; a vila tem duas alturas, y63 e y80+):
      toda profissão que desiste abaixo do destino sobe até a altura dele (pondo bloco ou cavando — a
      regra de 15-10 "só o mineiro" vale fora desse caso), com prazo de 2 min e descanso de 5; escada
      fixa de arenito entre as alturas (VillageStairs); viveiro do anel do baú aceita areia da vila a
      céu aberto, só na vila em duas alturas e dentro da caixa; canteiro novo na areia além da borda da
      roça (mais trigo para o pastor cruzar os bichos). Pendente: na bancada a nota variou de 4,4 a 9,0
      (sem ganho claro sobre a 0.3.80); escada parou em 4 de 18 degraus por peça da vila; KF-006.
      0.3.80 (deserto jogado na 0.3.79, nota 9,1, Spark jiRJWBtgDi): encalhado abaixo do caminho sobe
      até a altura dele (pilar/escada rumo ao destino — o mineiro da casa baixa saía "after 0 steps" 9×);
      grama também no chão do curral; viveiro primeiro em volta do baú do lenhador, na altura dele;
      terracota, feno e argila aparecem quando a obra pede (AppearingPieces, sem pedido nem falta).
      Pendente: KF-005 (teste do carvão do mineiro cai na bateria inteira, 3/7).
      0.3.79: fazendeiro vai ao lugar de pé ao lado da planta (FarmerStand) e, como o construtor, anda
      por etapas quando longe — desistências 7 → 0 na bancada; grama ao pé do poste da ovelha amarrada
      (PostGrass, pedido do autor). Pendente: a grama do poste não foi exercida na bancada (o pastor não
      amarrou ovelha nenhuma) — só GameTest; pastor segue 75% ocioso; lenhador e carpinteiro parados
      (40–60% ócio); cacto e cana contam como chão do lote; a bancada varia muito entre rodadas (nota
      3,9 a 8,8 com o mesmo código) — comparar com 2+ rodadas.
- [ ] 🔴 **Jogar 0.3.77 pelo roteiro** (`docs/testing/roteiro-por-bioma.md`): deserto e uma floresta,
      com `/vc bioma nota`; relatório com `scripts/biome_report.py`. A bancada não vê casa torta,
      aldeão preso nem rua estranha.
- [ ] 🟠 **Rotas que travam na execução (`STALLED`).** Deserto: arenito liso (7×), cortado (3×) e comum —
      fundidor e pedreiro não pegam a tarefa; deserto também tem fundidor 100% parado. Neve: bloco e
      camada de neve agora têm tarefa e travam (6× e 2×). Savana: vidraça, terracota colorida.
- [ ] 🟠 **Terraplanagem fase 2** (a vila inteira em subida de 1 em 1, plano de 15-10): ermos e taiga
      nevada têm 69% e 28% das recusas "sem chão na janela", relevo acima de 2.
- [ ] 🟠 **Lavoura e viveiro na neve e no deserto.** `FarmerTill` e `TreeNursery` exigem ar em cima do
      chão: a camada de neve barra. No deserto, roça nova exige grama/terra (aterrar com terra da mina) e
      o fazendeiro "could not reach the crop" (9–11×). Fazendeiro 75–81% parado no deserto.
- [ ] 🟠 **Pastor sem ovelha.** Deserto e neve: 84–96% parado, "no sheep with wool within the radius";
      lã branca é a falta `NOT_FOUND` mais comum (todas as vilas). Decisão: o pastor traz/cria animais?
- [ ] 🟡 **Estilo e madeira do bioma vizinho** para biomas fora da tabela (ermos → deserto, picos de
      gelo → neve; selva, floresta escura, cerejeira com a madeira local). Hoje caem em planície de carvalho.
- [ ] 🟡 **Reserva de madeira** onde não há árvore (lenhador 85–88% bloqueado no deserto).
- [ ] 🟡 **Mineiro: alvo alto demais e sem onde pisar** (TOO_HIGH/NO_STANDING_POSITION em todo bioma) —
      a mina 2ª fase.
- [ ] 🟡 **Ofício sem trabalho ajuda os outros** (cadeia da Regra 51 para pastor e fazendeiro) — mexe
      na Regra 59, **decisão do autor**.
- [ ] 🟡 `NO_TASK` novos: tronco e madeira de acácia na savana. Melancia (`melon`) sem rota: o
      fazendeiro não planta melancia.
- [ ] 🟡 **Bancada mais fiel:** 3 sementes por bioma; vila posta por `/place structure` não fica
      registrada como estrutura (mede o pior caso); medir o custo da limpeza de mata (`cost_ledger`).
- [ ] 🟢 `MinerDirectPathGameTest` intermitente (~1/3, já no 690dee98) — card de tarefa aberto.
- [ ] 🟢 PR e merge na `main` das branches `fix/varredura-g1-g3` e `feat/biomas-simulacao` (ADR-035).

## 🔴 Auditoria técnica 10-10 (fila AUD, P0→🔴 P1→🟠 P2→🟡 P3→🟢)

Detalhe, evidência e teste de aceite: `docs/technical/avaliacao/ACTION-QUEUE.md`.
Ordem de execução: `docs/technical/Roteiro-Correcoes-2026-10-10.md`.
Scripts: `scripts/audit/check-*.sh`.

- [ ] 🔴 **AUD-01** Regra 51 reprovada nas duas sessões de 10-10 (0.3.23: SMELTER 91%, BUILDER 92%, MINER 17%;
      fundidor 311/311 soltas). Raiz (diagnóstico de 11-10): o dono do único ramal vai e volta numa descida até
      um patamar de pedra maciça sem nenhum prazo correr; a rota vinha do rumo da escada girada, não da cavada.
      RC1 (prazo da descida) e RC2 (rota pela escada cavada) feitas; falta o playtest.
      Roteiro: `docs/technical/Roteiro-Correcoes-2026-10-10.md`, 2.2.
- [x] 🟠 **AUD-02** Save malformado não apaga mais as colônias: entrada ruim é descartada com aviso
      (`ColonySaveReader.readEntry`), versão negativa vira 0, `toBlock` não lança. `ColonySaveFuzzTest` 13/13.
- [x] 🟠 **AUD-03** `saveVersion` gravado no save (`SaveMigration.stamp`, `11bd4799`).
- [x] 🟠 **AUD-05** `codex/fluxo-grafo-waypoints` integrada (ADR-041 #2) na `claude/integra-fluxo-grafo-waypoints`,
      0.3.24: 1402 unitários, 696/696 GameTests × 3, PIT 85,56%. Falta: PR na `main` e playtest.
- [x] 🟠 **AUD-06** `WorkDemandTest` fixa cada material da obra: `WorkDemand` 20/20 mutantes mortos; PIT 86,17%, folga 24.
- [x] 🟡 **AUD-07** Régua v2: C14 conta seções 🔴, C01 conta como o `wc -l`, sem quebra em cp1252. Recalculadas: 3,14 e 3,14.
- [x] 🟡 **AUD-08** `STATE.md` reescrito em 11-10 (709 → 111 linhas); o texto antigo, sem edição, em `docs/archive/technical/Historico-2026-10.md`.
- [ ] 🟡 **AUD-09** Ciclo raiz ↔ `fabric.*` por `VillageColonyMod` como localizador global.
- [ ] 🟡 **AUD-10** 181 estáticos mutáveis; config estática não-final (`MineLease`, `TreeChoice`, `MinerDetours`, `FarmerWork`). A regra ArchUnit já existe congelada: esvaziar o congelado.
- [ ] 🟡 **AUD-11** `fabric.network` com 67% de linhas no unitário (meta de 60% batida em 11-10): codec, os três
      pacotes pelo fio (`ProfessionPolicyPayloadsTest`), save da política e porta de permissão, com mutação conferida.
      Falta: `fabric.command` em 27% (bateria) e o cliente em 0%; receptores por lambda e o resto do `/vc log`.
- [x] 🟡 **AUD-12** Aviso quando outro mod tira a tarefa da colônia do `Brain` (`BrainHeartbeat`, `BrainHeartbeatGameTest`); `suggests: modmenu`. `breaks` fica para quando houver mod testado (nível 4 do modsmith).
- [ ] 🟡 **AUD-04** Ciclo p95 67 ms na 0.3.22, 27,7 ms na 0.3.23: medir de novo (≥ 20 amostras) antes de otimizar.
- [x] 🟡 **AUD-25** `TASK_RELEASED` grava o motivo (`Task.release(why)` nas 15 solturas das profissões) e o
      `action_report.py` agrupa as solturas por motivo.
- [ ] 🟡 **AUD-13** Alternância de obras: `docs/behavioral-tests/README.md` atualizado (11-10); falta provar em jogo.
- [x] 🟢 **AUD-14** CHANGELOG com 0.3.1 a 0.3.23, reconstruídas do commit que trocou a versão (11-10).
- [ ] 🟢 **AUD-15** 848 imports não usados (`RemoveUnusedImports` no Error Prone).
- [ ] 🟢 **AUD-16** Cache de `CraftingLookup.listAllOfType`, se o Spark mostrar.
- [x] 🟢 **AUD-17** `/vc log` continua aberto (ADR-041 #4).
- [ ] 🟢 **AUD-18** Tag por JAR publicado (0.3.4–0.3.23 sem tag).
- [x] 🟡 **AUD-19** `/vc log` mostra a última janela do `WorkTime` por ofício e marca a regra do ócio (ADR-041 #4).
- [ ] 🟢 **AUD-26** **Escolha do autor:** número da regra do ócio (colide com a Regra 51 do `docs/RULES.md`).
- [ ] 🟢 **AUD-20** Dividir as 15 classes em 488–500 linhas antes da próxima mudança nelas.
- [x] 🟢 **AUD-21** 8 rodadas em 11-10: a primeira série teve 2 falhas do `FreeChestCountGameTest` (aldeões dos vizinhos no raio de 64), corrigido; a série final deu 8/8, 705/705.
- [x] 🟢 **AUD-22** A fundação usa a semente do mundo e do lugar. Sem teste: a planta não tem processador nem baú com loot, então hoje a semente não muda nada observável.
- [x] 🟢 **AUD-23** Javadoc de `VillagerEntityMixin` não diz mais "o único mixin" (são três).
- [ ] 🟢 **AUD-24** **Escolha do autor:** apagar as 7 branches já contidas na `main`.

## 🟠 Fluxo físico de materiais da obra

- [ ] Medir no save real a entrega de `CALCITE` pelo mineiro e comparar contra as 63 inserções
      instantâneas registradas; rota declarada e meta nominal não substituem a confirmação de entrega.
- [ ] Para cada bloco sem catálogo, criar primeiro uma cadeia executável de profissão ou mantê-lo no
      fallback terminal. Não voltar a declarar uma rota genérica sem trabalhador que a cumpra; folhas
      permanecem fora da coleta do lenhador pela regra de preservar a copa.
- [ ] Conciliar demanda da obra, reserva de tarefa, estoque em baú e entrega para que uma reserva
      sem retirada não impeça outra profissão de atender a peça.
- [x] Remover o ciclo de reserva/liberação do fundidor quando falta insumo, mantendo
      alternativa de construção produtiva enquanto espera. `SmelterGameTest` 5/5 e
      GameTests 652/652; confirmar no próximo save a redução de liberações e a
      ociosidade abaixo de 15%.
- [x] Registrar no relatório cada fallback terminal (`VC_SUPPLY_ERROR`) por peça, profissão e
      tentativas; `test_analyze_village_log` 11/11 e Python 96/96. Comparar o próximo playtest
      com os 334 abastecimentos instantâneos da sessão anterior.
- [x] Implementar a ADR-040: quatro despachos executáveis sem entrega física acionam o fallback;
      consulta do construtor e reserva de tarefa não contam, e a entrega no baú zera o relógio.
      `BuilderMaterialsGameTest` e GameTests 654/654; medir no save a entrega profissional e
      cada fallback terminal antes de reduzir a política.

## 🟠 Erros abertos

- [ ] **KF-002** (aldeão de coleta some no tique 1): não reapareceu em ~27 baterias de 07-10.
      Correção recomendada: montar o cenário dentro da própria arena, com o raio protegido encurtado
      por gancho de teste (como `MineDigging.shortenSurfaceRadiusTo`), em vez de 65–97 blocos fora.
- [ ] **KF-003**: causa achada e corrigida (fusão de colônias no tique de teste). Fecha com
      `python scripts/gametest_battery.py 30` limpo; até agora 7 + 6 limpas.
- [ ] **`lumber_stall_guard`** caiu 1 vez em ~6 baterias de 07-10 (tarde), sem relação com a mudança
      da hora. Medir com o script antes de investigar.

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que
passa com a regra desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da
borda (ela já fez um teste passar sem medir nada, o D2).

Plano de 07-10 aplicado: D2, B1, B3, B4, B5, B6 e o arquivo do save provados por mutação (ADR-039).
Falta só fechar e abrir um mundo de verdade (playtest).

## 🟡 Estrutura

- [ ] Outros seis arquivos de produção estão em 497–500 linhas (`SmelterWork`,
      `Mine`, `ClimbOut`, `SurfaceGatheringWork`, `Worker`, `BuilderWork`) e devem ser divididos
      antes da próxima mudança neles. `ColonyGoals` foi reduzido a 500 linhas com a extração de
      `AdvanceConstructionStock`; `FileSizeRuleTest` voltou a passar.
- [ ] Branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`): já toda na
      `main`; remover o worktree.

## Decisões do autor que faltam

- [ ] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes?
- [ ] Dois construtores na mesma obra.
- [ ] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33.
- [ ] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24).
- [ ] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
- Peça sem rota: depois de 4 tentativas aparece no baú de quem precisa (`BiomeConstructionSupply`,
  já em vigor, testado em `LocateFallbackGameTest`).
