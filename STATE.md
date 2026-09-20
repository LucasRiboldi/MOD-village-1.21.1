# STATE — 2026-09-19

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Este arquivo chegou a **2.277 linhas** antes de
> 09-19: ele passou a atrasar a sessão que devia orientar.

---

## Onde a vila está

**Ela produz, escolhe lote e constrói — e ainda não fechou uma casa.**
Zero `the house is up` em todos os playtests de 09-19.

A cadeia foi percorrida degrau a degrau no dia: índice de ruas → Regra 3 →
Regra 22 → baú cheio → pedreiro sem material → arenito cru esgotado → vaso
sem item → acavalamento → **madeira**.

**O bloqueio de agora é madeira.** A vila de deserto não tem floresta ao
alcance: `looking for a tree`, zero árvores derrubadas, e a obra parou em
`needs 4 minecraft:oak_planks and has 0`. O viveiro do fazendeiro plantou 4
mudas — é a saída projetada, e ela **depende de tempo de jogo**.

---

## O que espera playtest

| item | sinal a procurar |
|---|---|
| obra retomada com acavalamento | `drops the saved … it sits inside something` |
| fornalha com um pouco de cada | dois ou mais `made … out of` por sessão |
| reserva do cru | `SANDSTONE` e `SMOOTH_SANDSTONE` convivendo |
| viveiro dá madeira | qualquer `_log` no estoque |

⚠️ A obra que está no mundo tem **2 blocos de pé**, então a guarda nova
**não** a larga — casa pela metade é do jogador. Quebrar esses 2 blocos faz
a colônia recomeçar limpo.

---

## 🔴 Dívida conhecida

**`ColonyDetectionGameTest` falha 2 de 2 rodadas na base limpa.** É
**pré-existente** ao trabalho de 09-19 e foi medido com `git stash`:
`esperava ao menos 30 trabalhadores, achei 24` — sempre **24**, nunca 25 ou
29. Número fixo não é corrida de relógio, é **teto**; a causa ainda não foi
achada.

**Isto custou caro:** eu atribuí essa falha a três mudanças minhas antes de
medir a base. A lição é a regra que já valia — *isolar contra a base antes
de acusar a própria mudança*.

**O cenário do acavalamento em obra aberta** está no código e coberto por
teste unitário (`OverlapGuardTest`), mas sem gametest: toda versão que
escrevi derrubava o cenário acima.

---

## Como investigar aqui

**Instrumentar antes de consertar.** É o que funciona neste projeto, e o dia
09-19 mediu: três defeitos foram decididos **numa única leitura** depois de
instrumentados — P1.3 (`ProtectionSample`), P1.6 (`VolumeSample`) e o
`cut_sandstone` (`CraftReasons`).

E a instrumentação pega erro de quem a escreve: o `CraftReasons` acusou o
jogo de não ter uma receita que ele tem, e foi a própria linha que mostrou.

**Quando a mesma causa reaparece, desconfie da ferramenta.** Vários itens
caindo juntos com o mesmo sinal é assinatura compartilhada, não regressão
múltipla.

---

## Ferramentas de diagnóstico

| ferramenta | responde |
|---|---|
| `scripts/verdict.py` | o veredito de cada item pendente, lendo o log |
| `ChainRootsGameTest` | onde cada cadeia começa, por bioma — `chain-roots.txt` |
| `StructureCoverageGameTest` | quem fabrica cada peça — `structure-coverage.txt` |
| `CraftReasons` | por que a peça não saiu, no log |
| `VolumeSample` / `ProtectionSample` | de que as recusas de lote são feitas |
