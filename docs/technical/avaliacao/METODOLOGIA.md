# Metodologia de avaliação técnica — Village Colony

**Versão da régua:** 1 (2026-09-24) · **Coletor:** `scripts/assess/assess_project.py`
· **Régua em código:** `scripts/assess/rubric.py` · **Teste da régua:**
`tests/test_assess_rubric.py`

Esta é a forma fixa de avaliar o projeto inteiro: lógica, organização,
ferramentas, nomenclatura, registros, testes, desempenho e complexidade.
Ela existe para que duas avaliações feitas em datas diferentes, por pessoas
ou agentes diferentes, sejam **comparáveis**. Por isso tudo o que dá para
medir é medido por script, com limiares escritos antes da medição, e o que
depende de julgamento é declarado como julgamento e exige evidência.

---

## 1. Princípios

1. **Medir antes de opinar.** Toda nota numérica sai do coletor. Quem avalia
   não ajusta nota à mão.
2. **Mesma régua, sempre.** Os limiares estão abaixo e em `rubric.py`. Mudar um
   limiar quebra a comparação com o passado; se for inevitável, suba a
   versão da régua (§8) e recalcule as avaliações anteriores com a nova.
3. **Medida ausente não é zero.** Se um artefato não existe (sem sessão de
   jogo, sem log de compilação), o critério fica **sem nota** e sai da média.
   O relatório diz o que faltou.
4. **Separar medido de julgado.** A parte qualitativa (§5) usa uma escala
   própria e, em cada item, cita arquivo:linha, métrica ou log.
5. **Uma rodada da bateria de jogo não prova estabilidade.** O critério C09 só
   dá nota máxima com duas ou mais rodadas seguidas sem falha.

---

## 2. Dimensões

| Dimensão | O que responde | Critérios |
|---|---|---|
| Organização | O código está repartido em peças de tamanho que se lê? | C01, C02 |
| Complexidade | Quanta decisão cabe num método? | C03 |
| Arquitetura | As camadas se respeitam? Quanto estado é global? | C04, C05 |
| Testes | Quanto se testa, e se o teste pega mudança de fato | C06, C07, C08, C09 |
| Qualidade estática | O que um analisador acha sem rodar | C10 |
| Legibilidade | Quanto texto para cada linha de código | C11 |
| Processo | O que roda sozinho a cada mudança | C12 |
| Desempenho | Quanto o mod pesa no tique do servidor em jogo real | C13 |
| Registros | Quanta dívida crítica está aberta | C14 |
| Lógica e domínio | As regras do jogo estão certas e bem modeladas? | qualitativo (§5) |

---

## 3. Critérios e limiares (régua v1)

Nota de 0 a 4: a primeira faixa atingida vale.

| # | Critério | Fórmula e fonte | 4 | 3 | 2 | 1 | 0 |
|---|---|---|---|---|---|---|---|
| C01 | Arquivos > 500 linhas | % dos `.java` de `src/main` acima de 500 linhas físicas | 0% | ≤2% | ≤5% | ≤10% | >10% |
| C02 | Tamanho de método | p90 das **linhas de código** por método (sem comentário e branco) | ≤20 | ≤35 | ≤50 | ≤80 | >80 |
| C03 | Complexidade ciclomática | % de métodos com CC > 10; se CC máx > 50, teto 2 | ≤3% | ≤6% | ≤10% | ≤15% | >15% |
| C04 | Regra de camadas | imports de `net.minecraft`, `net.fabricmc`, `fabric` ou `data` dentro de `core` | 0 | 0 | ≤2 | ≤5 | >5 |
| C05 | Estado estático mutável | campos `static` não finais + `static final` de coleção mutável, por mil linhas de código | ≤1 | ≤3 | ≤6 | ≤10 | >10 |
| C06 | Volume de teste | linhas de código de teste (unitário + jogo) ÷ linhas de código de produção | ≥1,0 | ≥0,7 | ≥0,5 | ≥0,3 | <0,3 |
| C07 | Cobertura do core+data | % de linhas cobertas pelo JaCoCo nos pacotes `core` e `data` | ≥80 | ≥65 | ≥50 | ≥35 | <35 |
| C08 | Mutação | % de mutações mortas pelo PIT em `core` | ≥85 | ≥75 | ≥65 | ≥50 | <50 |
| C09 | Estabilidade da bateria de jogo | falhas em N rodadas de `runGametest --rerun-tasks` | 0 falha e N≥2 | 0 falha e N=1 | ≥99,5% | ≥98% | <98% |
| C10 | Análise estática | avisos do Error Prone por mil linhas de código | ≤2 | ≤5 | ≤10 | ≤20 | >20 |
| C11 | Densidade de comentário | linhas de comentário ÷ linhas de código (abaixo de 0,15 vale 1) | ≤0,6 | ≤1,0 | ≤1,5 | ≤2,5 | — |
| C12 | Práticas automatizadas | quantas de 7: CI, unitário no CI, jogo no CI, análise estática, mutação, cobertura, hook de commit | 7 | 6 | 5 | 4 | <4 |
| C13 | Desempenho em jogo | linhas `Colony cycle took N ms — longer than a server tick` por hora da última sessão analisada | 0 | ≤5 | ≤15 | ≤40 | >40 |
| C14 | Dívida crítica | itens `- [ ] 🔴` abertos no `TODO.md` | 0 | ≤2 | ≤5 | ≤10 | >10 |

**Por que cada limiar.**
- **C01:** 500 linhas é a regra do projeto (`CLAUDE.md`).
- **C02 e C03:** usam as faixas usuais da literatura (McCabe: CC > 10 pede
  atenção; > 20, risco alto).
- **C07:** mede só `core` e `data` porque é lá que o teste unitário é o
  instrumento. A camada `fabric` é exercida pela bateria de jogo, que o
  JaCoCo desta configuração não instrumenta. O número dela aparece no
  relatório, mas não entra na nota.
- **C11:** comentário demais também custa. Acima de 1:1 o texto compete com
  o código na leitura.
- **C13:** o tique do servidor tem 50 ms, e cada linha é um tique perdido
  que o jogador sente.

**Conceito global:** média simples das notas existentes.

| Conceito | Média mínima |
|---|---|
| A | 3,5 |
| B | 2,75 |
| C | 2,0 |
| D | 1,25 |
| E | abaixo de 1,25 |

---

## 4. Procedimento

1. Partir de uma árvore limpa, no commit a avaliar (`git status` vazio).
2. Se houve sessão de jogo nova, atualizar o histórico de desempenho:

   ```text
   python scripts/analyze_village_log.py
   ```

3. Coletar e pontuar, com duas rodadas da bateria para o C09. Leva de 6 a 8
   minutos.

   ```text
   python scripts/assess/assess_project.py --run --gametest-runs 2
   ```

   Isso grava `docs/technical/avaliacao/<data>-<commit>/metricas.json` e
   `scorecard.md`.
4. Escrever `RELATORIO.md` na mesma pasta, pelo modelo do §6, com a parte
   qualitativa do §5.
5. Comparar com a avaliação anterior e colar a tabela no relatório:

   ```text
   python scripts/assess/assess_project.py --compare docs/technical/avaliacao/<anterior>/metricas.json docs/technical/avaliacao/<atual>/metricas.json
   ```

6. Registrar uma linha em `docs/technical/avaliacao/README.md` e commitar a
   pasta.

---

## 5. Parte qualitativa (julgamento com evidência)

Cada item recebe uma destas quatro notas, e cada nota vem com evidência:

| Nota | Quer dizer |
|---|---|
| **Forte** | Pronto para crescer. |
| **Adequado** | Funciona, com atrito conhecido. |
| **Frágil** | Já causou defeito, ou vai causar ao crescer. |
| **Crítico** | Impede o próximo passo. |

| # | Item | Perguntas |
|---|---|---|
| Q1 | Modelo de domínio | As regras do jogo (profissão, obra, economia, mina) estão em classes com nome de domínio? A regra mora num lugar só? |
| Q2 | Fluxo e estado | Quem muda o estado de uma colônia, quando e em que ordem? Há estado global que sobrevive entre mundos ou testes? |
| Q3 | Repetição entre ofícios | Cada profissão reimplementa o ciclo de tarefa (achar, andar, trabalhar, travar, desistir)? |
| Q4 | Robustez e recuperação | O que acontece quando algo não dá certo: guarda, desistência, reparo, migração de save? |
| Q5 | Desempenho | Onde está o custo por tique? Há orçamento por passagem, cache, varredura limitada? |
| Q6 | Nomenclatura | Nomes dizem o que a coisa é? Há mistura de idiomas ou de convenções? |
| Q7 | Registros | ADR, estado, TODO e log de desenvolvimento estão atualizados, do tamanho que se lê e com uma fonte por assunto? |
| Q8 | Ferramentas | Build, teste, análise, perfil, release: o que é reprodutível por comando? |
| Q9 | Testes | Os testes provam comportamento (mutação), são estáveis, rápidos e isoláveis? |
| Q10 | Processo | Commits por marco, CI no fluxo real, integração com a `main`, convenção de mensagem. |

---

## 6. Modelo do relatório

`RELATORIO.md` de cada avaliação tem estas seções, nesta ordem:

1. **Resumo executivo**: o conceito, três forças, três riscos e a recomendação
   principal.
2. **Scorecard**: a tabela do coletor, sem edição.
3. **Comparação** com a avaliação anterior, quando houver.
4. **Métricas detalhadas**: tamanho, complexidade, os métodos mais complexos,
   testes, cobertura, mutação, análise estática, desempenho e registros.
5. **Análise qualitativa**: Q1 a Q10, com nota e evidência.
6. **Riscos priorizados**: 🔴 🟠 🟡 🟢, com impacto e probabilidade.
7. **Recomendações** com **critério de aceite medível** na próxima avaliação
   (por exemplo, "C13 de 1 para 3", "C05 ≤ 3/kLOC").
8. **Limitações desta avaliação**: o que não foi medido e por quê.

---

## 7. Limitações conhecidas do coletor

- **Complexidade:** a CC é contada por pontos de decisão, sem árvore
  sintática. É aproximada, mas é a mesma conta em toda avaliação.
- **Métodos fora da contagem:** métodos de classe aninhada (recuo de 8
  espaços), construtores e lambdas.
- **JaCoCo:** não instrumenta o `runGametest`, então a camada `fabric` fica
  sem cobertura medida.
- **C13:** depende do `Log-Stall-History.json`, que só muda quando alguém
  joga e roda o analisador de log. Nota velha de desempenho aparece como
  tal no relatório.
- **Estabilidade:** a intermitência do GameTest já foi medida em ~1 falha a
  cada 8 rodadas. Duas rodadas verdes não excluem isso; para afirmar cura,
  use `--gametest-runs 8`.

---

## 8. Versões da régua

| Versão | Data | Mudança |
|---|---|---|
| 1 | 2026-09-24 | Primeira régua: 14 critérios, 10 dimensões, conceito A–E. |
