# STATE — 2026-09-30

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto três vezes (2.277 linhas
> em 09-19; 659 em 09-24; 348 em 09-30): o texto antigo foi arquivado lá, sem
> edição. O texto completo de cada correção abaixo está na seção
> "Arquivado do STATE.md em 2026-09-30".

---

## Em uma linha

Branch `claude/sync-local-github-80cc8a` (PR #8): revisão das profissões de
30-09 (`ef84143`, ADR-028/029, `docs/technical/Revisao-Profissoes-2026-09-30.md`)
com 1.196 unitários e 528/528 GameTests locais; fluidez F1–F14 na §8 da Fluidez.

## 30-09, noite — playtests 18h e 21h (Spark `YUm45D9Sw4`, `qI5h6MXtDA`)

- **E49** visto em jogo (fundidor 6.324 → 214 paradas); E50 não exercitado.
- **E51 (`2895d71`):** a ponta da vila nascia colônia nova com BigHouseMOD e
  7 adultos — `44cd9e5a` tem 7 BigHouseMOD. Análise e o que falta em
  `docs/technical/Identidade-da-Vila-2026-09-30.md`. Não visto em jogo.
- **Caixa da vila (ADR-003 Emenda 6), 01-10:** busca de camas em coluna com
  janela de altura; caixa que cresce (construção/lote +12, rua inclui o bloco);
  identidade e fusão pela caixa; só trabalha com jogador dentro (+5 min). Aberto: limpar o save.
- **E52 (01-10):** curral — `PenEscape` mede a cada 2 s quem está a ≤6 de cerca,
  abre o portão (fecha atrás) ou pula a cerca, por rota própria. **E47 revisto:**
  o mineiro 199ad062 parou a y=56 após 32 degraus; agora o encalhado larga o
  ofício e `ClimbOut` sobe por escada, pilar ou túnel, sem desistir. Não visto em jogo.
- **01-10:** Regra 45 (baús = os da caixa da vila); ADR-003 Emenda 7 (lote: ½ diagonal + 12, sem teto).

## Git

- PR #8 → `codex/bighousemod`; PR #3 → `main` aberto. Mesclar pede aval.

## Corrigido e testado, pendente de playtest

Cada item tem teste que falhou antes da correção. Nenhum foi visto em jogo.

| Data | Correção | O que confirmar no save |
|---|---|---|
| 30-09 | Revisão das profissões: lava nunca, carpinteiro titular, viveiro do lenhador, pão, rebanho, drops automáticos, teto 256, ofício Vanilla bloqueado | `baked`, `fed two sheep`, `leaves the vanilla trade`, `lava is never placed`, `drop ingredients appear`; BigHouseMOD nova com 7 camas |
| 30-09 | Rebento não nasce no espaço de obra (viveiro e replantio) | nenhum `planted … sapling` dentro de lote com obra aberta |
| 30-09 | Busca de lote limitada pelo prazo; motivo da ponta recusada | `sweep:` com 1–2 passagens por volta; `no road end … — N motivo`; `Planner turns` e `Colony cycle took` |
| 30-09 | Placa órfã (PR #4) | nenhuma placa sobre lote sem obra; obra aberta com placa |
| 30-09 | Obra anda longe do jogador (PR #6) | `blocks left` caindo entre 64 blocos e a distância de simulação; vila pausa depois dela; `Colony cycle took` com várias vilas |
| 28-09 | Obra pede a próxima peça artesanal; troncos brutos reservados | obra com escadas ou `oak_log` sem ficar sem peça |
| 28-09 | Levantamento de baús em rodadas de 8; baú inalcançável degrada sem travar | ciclo sem 76–122 ms no levantamento; baú descarregado ao lado de baú compartilhado |
| 28-09 | Obra só reservada com ponto de apoio; fundação completa lacunas de 1 camada | fim do `WORK_STALLED` do construtor; trabalhador saindo de ponto preso |
| 27-09 | Boca seca exige degraus em solo firme; vila na água tenta escada selada | mina seca 3x3 e mina aquática |
| 27-09 | Lote validado na orientação final da planta | obra retangular ao lado de lavoura original |
| 27-09 | Construtor mede alcance pela posição física | fim da falsa caminhada a 5,1 blocos |
| 27-09 | Mina esgotada sem boca oposta espera 600 tiques | um único estado de espera por ciclo |
| 27-09 | Viveiro sem ponto livre espera 6.000 tiques | lenhador sem árvore sem custo no Spark |
| 27-09 | Desvio do mineiro mira a perna intermediária | os dois alvos antes abandonados avançam até a galeria |
| 27-09 | Rua fechada abre ramo perpendicular | vila sem ponta acha lote sem invadir rua, lavoura ou estrutura |
| 27-09 | Trabalhador legado sem casa migra para cama livre | aviso sem repetir a cada varredura |
| 27-09 | Construtor na evolução por população; déficit de camas prioriza moradia | vila com mais adultos que camas; mensagem do `/vc log` |
| 27-09 | Mineiro volta depois de cair dois blocos (pedregulho do baú) | retorno num poço real |
| 27-09 | `WaitingWork` conserva a obra com rota no bioma; `BuilderApproach` escolhe ponto livre | retomada de obra já marcada como abandonada |
| 26-09 | BigHouseMOD atômica; cama e baú só dentro de estrutura | vila nova sem cama ou baú fora de estrutura |
| 26-09 | `/vc log` (diagnóstico no chat) | estados ativo, aguardando e travado coerentes |
| 26-09 | Construção concluída não reabre como reparo | fazenda antiga sem blocos quebrados |
| 26-09 | Bosque fundacional | duas árvores maduras a 48–56 blocos; +1 por dez adultos |
| 26-09 | Obra só no nível da rua; baú ao lado da cama | nenhuma casa sobre monte de terra |
| 26-09 | Alternativa A (obra adiada entregue); peça que ninguém fabrica vai ao baú | obra que antes nunca fechava |
| 25-09 | ADR-025 fases 1 e 2 (mineiro autônomo) | linha `brain:` no travamento; encalhado sai cavando |

## O que o próximo jogo precisa mostrar

O roteiro completo está em [`docs/proxima-sessao.md`](docs/proxima-sessao.md).
Em ordem:

| # | Item | Sinal no log |
|---|---|---|
| 1 | Perfil de desempenho (spark) | link do `/spark profiler stop`; ver `docs/technical/Profiling-spark.md`. O de 30-09 (`hUQeDXo9U6`) não foi lido: o ambiente remoto bloqueia `lucko.me` |
| 2 | Colônia presente e ciclo mais leve | `Planner turns`, **menos** `Colony cycle took` |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |
| 8 | Bosque fundacional | duas árvores maduras distintas a 48–56 blocos |
| 9 | Placa órfã e obra longe (PR #4 e #6) | ver a tabela acima |

Depois de jogar, rodar `python scripts/analyze_village_log.py`, que conta
todas essas assinaturas.

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`.

- **R1, desempenho.** Prazo de 15 ms e cota ajustável no código; falta medir
  em jogo, agora também com a execução em toda colônia `ACTIVE`. É o único
  critério da avaliação com nota 1.
- **`surface_worker_unreachable`** (7 no log de 28-09): reproduzir em
  GameTest antes de mudar a coleta.
- **Estado global (R2).** `ServerMemory` garante a limpeza, mas os 89 campos
  estáticos mutáveis continuam; consolidá-los é o que falta para o C05.
- **Itens 9 e 10 (ciclo de tarefa comum aos ofícios; regras de decisão
  para o `core`).** Pedem ADR antes do código.
- **ADR-025 fase 3** (veios por valor), depois do playtest das fases 1 e 2.
  Ver `docs/research/2026-09-25-mineiro-autonomo.md` §8-§11.
- **Decisões do autor** (ver `TODO.md`, "Decisões que faltam", e
  `docs/research/2026-09-25-decisoes-simples.md`): E43, TASK-048, TASK-044
  (fusão; a ADR-007 está aceita e não implementada), TASK-046, E38, E45.
- **Sessão longa de 26-09** (`docs/research/2026-09-26-sessao-longa.md`):
  lote que não cresce (§7.1; o motivo da rua sai no próximo log), obra
  largada sem blocos prendendo o lote (decisão) e aldeão ocioso preso.
- **Mineiro que não entrega (E44/E45)** e **segunda obra que não abre:**
  sem playtest novo desde as correções; detalhe no `Historico`.

## Dívida conhecida

- **Oito arquivos de produção acima de 500 linhas** (30-09, `wc -l`); ver
  o topo do `TODO.md`.
- **Hooks do Claude Code:** os scripts estão em `scripts/hooks/`; quem liga
  no `.claude/settings.json` é o autor.
- **Bateria de jogo:** 3 testes intermitentes isolados em 24-09; a taxa
  histórica era ~1 falha a cada 8 rodadas. `runGametest` não filtra teste.
  O timeout isolado do construtor alcançando o topo (27-09) não se repetiu.
- **`ColonyDetectionGameTest`:** a falha de 09-19 (24 trabalhadores em vez
  de 30) nunca foi reproduzida.
- **Cobertura da camada `fabric`** não é medida: o JaCoCo não instrumenta a
  bateria de jogo.
- **Sem GameTest com jogador real** para planejar perto e executar onde
  simula; a arena não cria jogador.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/METODOLOGIA.md`, rodada
  por `python scripts/assess/assess_project.py --run --gametest-runs 2`. A
  última deu B, 3,21 de 4.
- **Instrumentar antes de consertar.** Três defeitos de 09-19 se decidiram
  numa única leitura depois de instrumentados.
- **Quando a mesma causa reaparece em vários itens, desconfie da
  ferramenta.**
- **Ferramentas:**

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest` | onde cada cadeia de produção começa |
  | `StructureCoverageGameTest` | quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
