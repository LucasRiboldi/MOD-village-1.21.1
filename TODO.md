# TODO

Organizado pelo cronograma da avaliação de qualidade (`docs/audit/2026-11-13-avaliacao-qualidade.md`, seção 5): uma
seção por fase, prioridade dentro dela (🔴 bloqueia o portão, 🟠 importante, 🟡 melhoria). Só o que está aberto: feito
sai daqui e vai para o commit e o `CHANGELOG.md`. Tamanho: P até 1 dia, M 2 a 4 dias, G uma semana ou mais.

- Os 214 abertos do TODO antigo esperam triagem em `docs/technical/TODO-herdado-2026-11-13.md`: cada um entra numa
  fase abaixo ou é fechado lá com o motivo.
- O TODO antigo inteiro, por jogo, está em `docs/archive/technical/TODO-ate-2026-11-13.md` (`grep`).

## F0 — Sinal limpo (1 semana)

- [ ] 🔴 Triar os 214 itens de `docs/technical/TODO-herdado-2026-11-13.md` (M)
- [ ] 🟠 Decisão do autor: a ADR-024 (índice físico do armazém) é aceita, mas o `WarehouseIndex` não é usado em produção;
  religar ou retirar (P)
- [ ] 🟡 Decisão do autor: o `CLAUDE.md` importa `.fabric/AGENTS.md`, que a ferramenta Fabric cria localmente (pasta no
  `.gitignore`) e não existe em nenhum checkout; manter, reinstalar a ferramenta ou tirar o import (P)
- [ ] 🟠 `scripts/rule_coverage.py` lê o estado das linhas de duas células (regras 201 a 205 revogadas contam como sem
  teste) e ganha teste (P)
- [ ] 🟠 README: a frase "clientes não precisam instalar" está errada (o mod registra blocos e itens); contagens de teste
  velhas (`scripts/audit/docs_drift.py`) (P)
- [ ] 🟠 Decisão do autor, CI: separar o relatório JaCoCo da bateria do da auditoria (o publicado mostra 7,3% contra 79,0%
  real), "Check whitespace" com base, release só com CI verde do mesmo SHA. Os workflows só mudam com o autor (P)
- [ ] 🟡 Decisão do autor: fechar ou integrar a PR #45 (`codex/local-test-sentinel`) e as branches locais antigas (P)

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
- [ ] 🟡 Mineiro sem ramal livre: "every open branch is taken or finished"; no primeiro nível ele só espera (pulado pelo
  autor em 12-11)

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

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que passa com a regra
desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da borda.

## Decisões do autor que faltam

- [ ] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes?
- [ ] Dois construtores na mesma obra.
- [ ] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33.
- [ ] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24).
- [ ] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
