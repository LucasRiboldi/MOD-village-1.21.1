# STATE — 2026-10-11

> **0.3.33** (12-10): a obra esperava `bricks` para sempre — a cadeia não descia a receita de peça
> sem recurso declarado ([`docs/research/2026-10-12-obra-esperando-tijolo.md`](docs/research/2026-10-12-obra-esperando-tijolo.md)).
> Verificado: test 1475/0; runGametest 741/741; mutação nos testes de tijolo (0 sem a correção).
> **JAR instalado: `village-colony-0.3.33.jar`** = `021C117693E0310D49C25B5E825956C97C3CA2EDFAEB44D8758F42588172F7D6`
> (três cópias iguais; 0.3.32 como `.jar.disabled`). Aberto: mineiro 0% trabalhando com 3 de 4 ramais esgotados (TODO).

> **0.3.32** (`claude/autossuficiencia`, 11-10 noite) — correções do playtest da 0.3.31 (Spark
> `qWTXLoLP5g`, TPS 20, mod 2,5%; Regra 51 reprovou em todos os ofícios). Análise por profissão:
> [`docs/research/2026-10-11-fluxos-das-profissoes-031.md`](docs/research/2026-10-11-fluxos-das-profissoes-031.md).
> Mina só na borda (Regra 53) e sem cava de superfície; rota da descida até o fim do ramal;
> construtor de volta à obra (vaga do ajudante não é a tarefa); fazendeiro ajuda a obra; tetos de
> tábua e carvão vegetal; obra do save no giro certo; até 4 peças sem apoio largadas; transferência
> antes de surgir; overlay em pixel e em português (pt_pt). **Não jogado.**
> **Verificação (11-10 noite):** `test --rerun-tasks` 1475/0 falhas; `runGametest` 739/739 (1 rodada,
> mundo novo). **JAR instalado: `village-colony-0.3.32.jar`** =
> `C2ABB0B952481164DADEF9401B3B5026F83B72D19D8958C08CBB8C5E0DC8C910`, igual em `build/libs`,
> `downloads/` e `.minecraft/mods`; a 0.3.31 ficou como `.jar.disabled`.

> Arquivo de estado vivo. **Sobrescreve, não acumula.** Se passar de 150 linhas, algo
> está errado: P0 não está fechando. O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md)
> (o STATE de 10-10 inteiro, 709 linhas, sem edição) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.

---

## 🔴 P0 — Autossuficiência: a vila fabrica o que ninguém pediu e falta o que a obra pede

Playtest 0.3.24 de 11-10 (`time_ledger`, `action_report`, Spark `3Q6g9MrO1o`). A mina voltou:
**1022 blocos, 11/min**, um nível descido; o fundidor saiu da roda (2 pegas, 0 soltas); TPS 20.
Mas a Regra 51 reprova em seis ofícios (BUILDER 81%, SHEPHERD 68%, MINER 43%, LUMBERJACK 29%,
MASON 22%, CARPENTER 16%), e **58 pedregulhos e 16 vidraças surgiram sozinhos** pela regra da
4ª tentativa.

Raiz principal: a produção antecipada (`AdvanceStock`) não tem teto. No fim havia 318 lajes de
pedregulho, 282 de pedra lisa e 356 escadas de carvalho, com 15 pedregulhos no baú; a obra deixou
pedregulho de lado 252 vezes. Areia: nenhuma coletada. Análise, alternativas e a escolha do autor
(P1–P9): [`docs/research/2026-10-11-autossuficiencia-e-ocio.md`](docs/research/2026-10-11-autossuficiencia-e-ocio.md).

| Correção anterior (roteiro 10-10) | Commit | Em jogo (11-10) |
|---|---|---|
| RC1: prazo da descida da mina | `08b0c34f` | ✅ mina produziu; "the way down got no closer" nunca precisou sair |
| RC2: a rota segue a escada cavada | `36d3946b` | ⬜ mundo novo; o save antigo não foi aberto |
| 2.3: regra do ócio cede vaga | `da43ef34` | ⬜ não visto (titular único nunca cede) |

**Aceite da autossuficiência:** playtest de 60 min com tudo implementado; Regra 51 ≤ 15% por
ofício; `VC_SUPPLY_ERROR` perto de zero; nenhuma peça antecipada acima do que a próxima obra pede.

## Branches

Pilha #20–#25 **na `main`** desde 11-10 (`38015460`), com merge commit; as branches dela
continuam no remoto (apagar é decisão do autor, AUD-24).

**Sessão em curso:** `claude/autossuficiencia`, PR LucasRiboldi/MOD-village-1.21.1#26 aberta para a `main`.
Ciclo 1 (0.3.25): P1–P9, construtor limpando a faixa de construção, planta do jogador não volta.
Ciclo 2 (0.3.26), depois do playtest curto da 0.3.25: contagem completa dos baús (a parcial furava
o teto do P1), mina que nunca fecha por descida travada, fundidor que vai buscar a areia, britador
pela obra esperando vidro, reserva de 16 areias, bloco no baú do construtor em 1 minuto, artesão
fabrica sem andar até o baú, sem contratação de fundidor sem cru, ajudante larga peça que não
alcança. **Não feito:** P5a (sem defeito), coleta de areia por estoque além da reserva, iluminar a rua.

## O que o próximo jogo precisa mostrar

JAR 0.3.24 já instalado. Em ordem:

| # | Item | Sinal |
|---|---|---|
| 1 | Mineiro chega ao nível pela escada real | `the surface entrance is remembered at -768, 66, -923 … 5 levels down` no save do autor; `MINED` por minuto acima de 0,8 |
| 2 | Ninguém segura o ramal parado | nenhum mineiro com o ramal por mais de 60 s de descida; se houver, `the way down got no closer than` |
| 3 | Regra 51 por ofício | `python scripts/time_ledger.py`; `/vc log` mostra a última janela de cada ofício |
| 4 | Fundidor sai da roda | `python scripts/action_report.py`: soltas/pegas < 10%, com o motivo de cada soltura |
| 5 | Custo do ciclo (AUD-04) | `python scripts/cost_ledger.py`: ≥ 20 amostras de `VC_COST`; p95 > 50 ms abre a Fase 4 |
| 6 | Alternância de obras (AUD-13) | quatro obras seguidas, `grep "opened"` |

Os playtests pendentes de antes (0.3.5 a 0.3.23) estão por versão no `TODO.md`; os de setembro,
no `Historico-2026-09.md`. Depois de jogar: `scripts/analyze_village_log.py`,
`scripts/time_ledger.py`, `scripts/action_report.py` e `scripts/cost_ledger.py` (CLAUDE.md §0.4).

## 🔴 Aberto

A fila completa e priorizada está no `TODO.md` (seção AUD) e no roteiro
(`docs/technical/Roteiro-Correcoes-2026-10-10.md`, tabela "Estado da execução").

- **2.4, fundidor e despacho:** o código da linha Codex está integrado; falta medir em jogo.
- **AUD-11 (parcial):** `fabric.network` já passa dos 60% (67%); faltam `fabric.command` (27%) e o cliente (0%).
- **AUD-04 e AUD-16:** só com gatilho (Fase 4). Sem número, sem otimização (ADR-035 §5).
- **Fase 5 (dívida):** AUD-15, 20, 10 e 09; o AUD-09 pede ADR.
- **Decisões do autor:** apagar as 7 branches já contidas na `main` (AUD-24); o número da
  regra do ócio, que divide o "Regra 51" com a da peça na 4ª tentativa (AUD-26); a ADR do AUD-09.
- **AUD-18:** tag por JAR publicado. Não foi feito: criar tags é publicação.

## Dívida conhecida

- Nenhum arquivo de produção acima de 500 linhas; 12 entre 488 e 500 em 11-10 (AUD-20; eram 15 na auditoria).
- `runGametest` não filtra por teste dentro da bateria completa; `-PgametestOnly=Classe` roda
  uma classe isolada. Testes rodam juntos num mundo só: quem conta entidades num raio grande
  afirma sobre as suas (o `FreeChestCountGameTest` falhava 2 em 8 por isso).
- Cobertura da bateria: `fabric.command` e `fabric.network` baixas; cliente 0% (sem cliente no teste).
- Hooks do Claude Code em `scripts/hooks/`; ligá-los no `.claude/settings.json` é do autor.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/` (auditoria de 10-10, régua v2 na
  `METODOLOGIA.md`); `python scripts/assess/assess_project.py --run --gametest-runs 2`.
- **Verificações reprodutíveis:** `scripts/audit/check-*.sh` (README na pasta).
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse.
- **Ler o save quando o log não basta:** a RC2 só se decidiu lendo as tochas do mundo.

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest`, `StructureCoverageGameTest` | onde cada cadeia começa; quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
