# TODO

Organizado pelo cronograma da avaliação de qualidade (`docs/audit/2026-11-13-avaliacao-qualidade.md`, seção 5): uma
seção por fase, prioridade dentro dela (🔴 bloqueia o portão, 🟠 importante, 🟡 melhoria). Só o que está aberto: feito
sai daqui e vai para o commit e o `CHANGELOG.md`. Tamanho: P até 1 dia, M 2 a 4 dias, G uma semana ou mais.

- Os 214 abertos do TODO antigo foram triados em 13-11: 139 fechados com o motivo e 75 trazidos para cá; o registro
  está em `docs/technical/TODO-herdado-2026-11-13.md`.
- O TODO antigo inteiro, por jogo, está em `docs/archive/technical/TODO-ate-2026-11-13.md` (`grep`).

## F0 — Sinal limpo (1 semana)

- [ ] 🟠 Decisão do autor: a ADR-024 (índice físico do armazém) é aceita, mas o `WarehouseIndex` não é usado em produção;
  religar ou retirar (P)
- [ ] 🟡 Decisão do autor: o `CLAUDE.md` importa `.fabric/AGENTS.md`, que a ferramenta Fabric cria localmente (pasta no
  `.gitignore`) e não existe em nenhum checkout; manter, reinstalar a ferramenta ou tirar o import (P)

## F1 — A vila trabalha (3 a 5 semanas) → portão Beta 0.5.0

- [ ] 🔴 Ciclos de playtest até todo ofício ficar abaixo de 40% parado em dois biomas, dois jogos seguidos (G)
- [ ] 🔴 Conferir em jogo as candidatas 0.4.69 a 0.4.74 (lista no `STATE.md`) (por jogo)
- [ ] 🟠 Estado e implementação nas 81 regras de `docs/RULES.md` que não têm essas colunas (M)
- [ ] 🟠 Número próprio para o teto de 15% de ócio; "Regra 51" hoje tem dois sentidos (P)
- [ ] 🟠 Fechar os conflitos C-12, C-13 e C-15 de `docs/audit/RULE-RECONCILIATION.md` (P)
- [ ] 🟠 Estado "confirmada em jogo" nas regras e no `STATE.md`, com as já vistas marcadas (P)
- [ ] 🟡 "Recurso que surge" reduzido a exceções declaradas e medidas por jogo (M). No jogo da 0.4.68: trigo ×467,
  lã ×144 (`WAITED_TOO_LONG`); tora ×43, tábua ×20, pedra lisa ×8 e bancadas (`MISSING_PIECE`); terracota ×47
  (`APPEARING_PIECE`); prismarinho ×144, favo ×20 e corantes (`AUTOMATIC_DROP`)
- [ ] 🟠 ADR-055: com dois ou mais construtores, uma obra para cada
- [ ] 🔴 Sentinela `fast` da 0.4.76 em diante com o Minecraft fechado (a 0.4.77 só rodou unitários e GameTests focados)
- [ ] 🟠 ADR-055: mineiro sem ramal livre abre ramal três níveis abaixo do nível em uso
- [ ] 🟠 ADR-055: troca de cama entre quem trabalha em cima e mora embaixo e quem faz o contrário
- [ ] 🟡 **Item MAE:** primeira obra pedida em jogo pela fila do construtor (`/vc mae obra <modelo>`, `VC_MAE_SITE` de
  `queued` a `done`), obra longe da vila, socorro (Regra 222) e a recusa que só mostra o motivo do último lote
- [ ] 🟡 **Nascimentos:** 0 em 63 min na 0.4.43 com 28 camas para 19 aldeões; conferir nascimentos por dia e `UPKEEP`
  no diário, e se a manutenção tira tábua da obra
- [ ] 🟡 **Grafo:** `Corridor` e `StripLot` ainda traçam caminho próprio antes de pedir ao grafo (ADR-048);
  `LINKS_PER_READ`=1 e a ligação que falha gasta a vez das outras partes; obra demolida não solta o ramal fixo
- [ ] 🟡 **Velas:** o carpinteiro faz 16 velas de cada uma das 17 cores para o estoque, e nada as usa
- [ ] 🟡 **Guarda de laço:** cobre oficina e fornalha; a coleta (lenhador, superfície) segue só com os prazos de sempre
- [ ] 🟡 **Reserva da mina:** reserva exclusiva por bloco e frente exclusiva por mineiro (dois mineiros no mesmo túnel)
- [ ] 🟡 **Chegada física:** caminho calculado que não vira chegada em 140 tiques, mesmo com `WorkTargets` ativo;
  separar geometria, tolerância de chegada e interferência do Brain antes de mexer
- [ ] 🟡 **Medida do diário:** trabalho auxiliar sem marca ainda conta como espera no `time_ledger`; medir `NO_PATH` e
  `NO_PROGRESS` por ofício
- [ ] 🟡 **Entrega física da obra:** medir no save a entrega de `CALCITE` pelo mineiro contra as inserções instantâneas;
  bloco sem catálogo ganha cadeia de profissão ou fica no recurso terminal; reserva sem retirada não deve impedir
  outra profissão de atender a peça
- [ ] 🟡 **Biomas** (jogos de bioma do portão Beta):
  - taiga: casa presa no último bloco por tronco natural no volume e por peça sem apoio (`UNSUPPORTED`);
  - deserto: mineiros andando 36% da vila alta até a mina do lago; `storage_majest` pedindo deepslate, calcita, folha de
    acácia e pinheiro; fundidor e fazendeiro esperando 41–42%;
  - rotas que travam na execução (`STALLED`): arenito liso, cortado e comum no deserto; bloco e camada de neve;
    vidraça e terracota colorida na savana;
  - terraplanagem fase 2 (subida de 1 em 1) para ermos e taiga nevada;
  - lavoura e viveiro com camada de neve em cima do chão;
  - reserva de madeira onde não há árvore (lenhador 85–88% bloqueado no deserto);
  - mineiro com alvo alto demais e sem onde pisar (`TOO_HIGH`, `NO_STANDING_POSITION`);
  - `NO_TASK` de tronco e madeira de acácia na savana; melancia sem rota

## F2 — Save e desempenho (3 a 4 semanas, em paralelo com a F1)

- [ ] 🔴 Cópia de segurança do `villagecolony_colonies.dat` antes de gravar, ou leitura de recuperação que não grava o
  estado vazio quando o arquivo existia e falhou (M)
- [ ] 🔴 Saves de 2 ou 3 versões publicadas lidos num teste; GameTest de parar e reabrir o servidor (M)
- [ ] 🔴 Ciclo da colônia repartido entre tiques, com orçamento de tempo e retomada (hoje até 93 ms com 1 vila e
  458 ms com 4 ou mais) (G)
- [ ] 🟠 Versão de esquema nos outros 4 arquivos que o mod grava (P)
- [ ] 🟠 NullAway em `data/save` e `fabric/adapter` (P/M)
- [ ] 🟠 Overlay recortado pela vila do jogador ou pela distância (hoje manda todos os trabalhadores a todo jogador) (P)
- [ ] 🟠 Gravação dos ledgers e do diário fora da thread do servidor (P)
- [ ] 🟡 Perfil "carga" no Sentinela, com N vilas e teto de custo por tique (M)
- [ ] 🟠 **Estado só em memória:** o plano do caminho (`PlannedPaths`) e as bocas antigas da mina (`MineEdge.PAST`) se
  perdem ao reiniciar o jogo
- [ ] 🟡 **Custo do MAE:** cada `/vc mae obra` travava o servidor cerca de 2,5 s (`MaeSites.start`) e a
  `MaeGenerationEvidence.onChunkGenerate` custava 6,4% do servidor (Spark `btHEJgIYWb`); medir de novo
- [ ] 🟡 **Estoque sem teto:** pedregulho de 554 a 13.579 em 16 min na 0.4.42 e 2.256 na 0.4.68; ver se falta teto de
  produção ou se é leitura de baús
- [ ] 🟡 **Testes do expediente:** `WorkClockTest`, `WorkHoursGameTest`, `OffHoursReleaseGameTest` e
  `BuildProgressGameTest` usavam o expediente antigo; `TaskLoopGuard` e `MineSideWork` sem teste
- [ ] 🟡 **Bateria de escassez** (R-158): falta total de madeira, água, semente, animal, combustível, ferramenta, baú
  livre e lote; esperado: alternativa válida ou bloqueio explícito

## F3 — Servidor e compatibilidade (2 semanas) → portão versão candidata 0.9.0

- [ ] 🔴 `ColonyTorches` fora da geração de mundo ou seguro entre threads (`HostileSpawnMixin` em `CHUNK_GENERATION`
  usa `HashMap` estático nas threads `Worker-Main`) (P)
- [ ] 🔴 Servidor dedicado com 2 clientes por 30 minutos, com Spark e `cost_ledger` (M)
- [ ] 🔴 Matriz mínima com Lithium, FerriteCore, ModernFix e um mod de aldeão: o jogo sobe e o mundo abre (M)
- [ ] 🟠 Permissão de op em `/vc bioma nota` e `/vc dados`, com limite no texto (P)
- [ ] 🟠 Teto ou rotação do diário e dos ledgers; interruptor das linhas `VC_*` em produção (P)

## F4 — Experiência do jogador (2 a 3 semanas)

- [ ] 🔴 Todas as mensagens em chaves de idioma, com inglês (hoje 49 em `Text.literal` e 25 traduzíveis) (M)
- [ ] 🔴 `/vc config` com interruptor por vila e pausa da colônia (M)
- [ ] 🔴 Guia curto do jogador em inglês e português: o que acontece, o que dá para fazer, como parar (M)
- [ ] 🟠 README de jogador separado do de desenvolvimento; capturas e GIF (P)
- [ ] 🟡 **Mensagem do fundidor:** "none of N colony chests had" mente quando o cru existe e a reserva impede

## F5 — Publicação (1 a 2 semanas) → portão 1.0.0

- [ ] 🔴 Declaração de uso de IA no README e nas páginas (o Modrinth exige desde 08-2026) (P)
- [ ] 🔴 Procedência do ícone, das texturas e das estruturas; `NOTICE` com a `big_house_mod.nbt` (cópia editada da
  Mojang); decidir refazer ou declarar (P/M)
- [ ] 🔴 Páginas no Modrinth e no CurseForge com descrição em inglês, ícone, galeria e changelog (M)
- [ ] 🔴 Release que roda os testes e publica o mesmo JAR jogado, amarrada ao CI do mesmo SHA (M)
- [ ] 🟠 Versão semântica a partir do corte de funcionalidades; canais beta e estável (P)
- [ ] 🔴 Fechamento: dois playtests limpos na candidata e Regra 51 (≤ 15%) em três biomas (G)

## Depois da 1.0 (dívida declarada)

- [ ] Contexto por servidor no lugar dos serviços estáticos de `VillageColonyMod` (G)
- [ ] `fabric/work` em grupos por profissão com dependência num só sentido (G)
- [ ] NullAway em toda a camada fabric (G)
- [ ] Nenhum método acima de complexidade cognitiva 25; teto por complexidade no lugar do de 500 linhas (M)
- [ ] Comentários datados abaixo de 300 (contínuo, ADR-035 §6)
- [ ] PIT estendido a pacotes puros de `fabric/work`; 10 a 20 propriedades jqwik nos sistemas com mais sobreviventes (M)
- [ ] Migração da fronteira da mina entre formas, em vez de descartá-la (G)
- [ ] `doMobGriefing` ou gamerule própria para o trabalho que altera terreno (M)
- [ ] **Regras provadas só por classe:** subir as ~95 regras em que o teste só usa a classe indicada para um teste que
  cita a regra (`docs/testing/regras-e-testes.md`); provas parciais das Regras 178, 172 e 171; sem teste próprio as
  Regras 220, a desistência da obra pedida, `MaeSites.move`, a busca de lote pela rede, o fundidor nas ajudas de chão, a
  pedreira em `MineDigging`, o teto de pastor, o diário com produto, a zona que cresce na ponta e o registro de cada
  suprimento (Regra 111)
- [ ] **Frutos de caule:** melancia, abóbora e jarro fora da plantação grande até o fazendeiro saber colher
- [ ] **Quarteirões:** o grafo só fecha retângulo pelo anel, pelas retas e pela ponta; sem busca própria
- [ ] **Sentinela futuro:** camada visual de cliente em spike isolado, publicação Allure entre máquinas, laboratório
  serial de carga com JFR (`docs/superpowers/plans/2026-11-02-sentinela-futuro.md`); bancada de bioma mais fiel
- [ ] **Contratos de tarefa:** ausência com invalidação por evento (R-021/R-027), prazo e critério de sucesso na `Task`
  (R-047/R-050), estados de disponibilidade (R-019), animal inacessível fora do plantel (R-107)
- [ ] **Sistemas novos, sem decisão:** transporte físico de itens (R-140 a R-142), itens de outros mods (R-026),
  adaptador para versões futuras do Minecraft (R-003/R-163)
- [ ] **Estilo do bioma vizinho** para biomas fora da tabela (ermos, picos de gelo, selva, floresta escura, cerejeira)
- [ ] **Cobertura:** `fabric.command` em 27% e o cliente em 0% (AUD-11); imports não usados (AUD-15)

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que passa com a regra
desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da borda.

## Decisões do autor que faltam

Nenhuma: as 14 de 13-11 estão na ADR-055.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
