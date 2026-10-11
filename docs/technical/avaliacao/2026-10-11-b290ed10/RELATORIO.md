# Relatório — avaliação de 2026-10-10 (`b290ed10`)

> A pasta tem a data UTC do coletor (2026-10-11 00:xx UTC = 2026-10-10 21:xx em Brasília).
> O texto completo desta avaliação está nos arquivos da auditoria técnica de 10-10
> em `docs/technical/avaliacao/` (D01–D12, ACTION-QUEUE, EXECUTIVE-SUMMARY). Este
> relatório segue o modelo da `METODOLOGIA.md` §6 e não repete o que está lá.

## 1. Resumo executivo

Conceito **B (3,36/4)** pela régua v1; **3,14** com os erros de medida de AUD-07
corrigidos. Forças: camadas (C04 = 4), bateria estável (C09 = 4, 3 rodadas),
mutação (C08 = 4). Riscos: desempenho em jogo (C13 = 1), estado estático (C05 = 2) e
a Regra 51 reprovada em jogo (qualitativo). Ver [EXECUTIVE-SUMMARY.md](../EXECUTIVE-SUMMARY.md).

## 2. Scorecard

Ver [scorecard.md](scorecard.md), gerado pelo coletor e sem edição. As entradas
vieram desta sessão: `test jacocoTestReport pmdMain --rerun-tasks`, 3 rodadas de
`runGametest --rerun-tasks` e `pitest`, com os logs copiados para `build/assess/`
antes de `assess_project.py` sem `--run`.

## 3. Comparação com 2026-09-24 (`17613fa`)

| Critério | 17613fa | b290ed10 | Δ |
|---|---|---|---|
| C01 Arquivos > 500 | 4 | 3 (real: 4, AUD-07) | −1 (0) |
| C03 Complexidade | 4 | 3 | −1 |
| C08 Mutação | 3 | 4 | +1 |
| C10 Error Prone | 3 | 4 | +1 |
| C14 Dívida crítica | 2 | 4 (real: 0, AUD-07) | +2 (−2) |
| **Média** | 3,21 | 3,36 (real: 3,14) | +0,15 (−0,07) |

Os demais critérios não mudaram. Tabela completa: `assess_project.py --compare`.

## 4. Métricas detalhadas

454 arquivos de produção (76 330 linhas físicas), 341 de teste; p90 de método com 24
linhas; 83 métodos com CC > 10, 9 com CC > 20 (máx. 27); 181 campos estáticos
mutáveis; 1398 unitários; 690 GameTests × 3; PIT 1804/2106; JaCoCo `core` + `data`
92,9%; 0 avisos do Error Prone; C13 de 39,4 ciclos acima do tique por hora no log
de 10-10 manhã (0.3.22). Detalhe em D01, D04 e D07.

## 5. Análise qualitativa

| # | Item | Nota | Evidência |
|---|---|---|---|
| Q1 | Modelo de domínio | Adequado | regras no `core` com nome de domínio; a Regra 51 mora em dois lugares que discordam (D2) |
| Q2 | Fluxo e estado | Frágil | `VillageColonyMod` como localizador global (D1, AUD-09); 181 estáticos (D3) |
| Q3 | Repetição entre ofícios | Adequado | não auditado a fundo nesta rodada |
| Q4 | Robustez e recuperação | **Crítico** | save malformado apaga todas as colônias (D5, AUD-02) |
| Q5 | Desempenho | Frágil | p95 67 ms (D4, AUD-04) |
| Q6 | Nomenclatura | Forte | nomes de domínio consistentes; prefixo `villagecolony$` nos mixins |
| Q7 | Registros | Frágil | `STATE.md` 663/150, CHANGELOG parado em 0.3.0 (D10) |
| Q8 | Ferramentas | Forte | build, testes, PIT, ledgers e agora `scripts/audit/` reprodutíveis |
| Q9 | Testes | Adequado | estáveis e fortes no `core`; rede e cliente sem teste (D7) |
| Q10 | Processo | Frágil | linha Codex paralela fora da `main` (D8, AUD-05) |

## 6–7. Riscos e recomendações

[ACTION-QUEUE.md](../ACTION-QUEUE.md), com o teste de aceite de cada item. Critérios
medíveis para a próxima avaliação: **C13 de 1 para ≥ 2**; **C14 medido por seção**;
Regra 51 sem violação no `time_ledger`; `NbtFuzzProbeTest` 12/12.

## 8. Limitações

Sem Spark; C13 vem de uma sessão com 0.3.22. Três rodadas não excluem a
intermitência de 1/8 (§7). Q3 não foi auditado.
