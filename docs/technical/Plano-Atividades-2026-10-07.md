# Plano — atividades, trajetos e ócio das profissões (2026-10-07)

Base: o playtest de 06-10 23:41 → 07-10 00:43 (JAR 0.3.1, `278086C5`), o `time_ledger.py`
(janelas de 5 min), as frases do log contadas por tipo, e o código depois da ADR-037.
Nada aqui está implementado; cada item é uma proposta para o autor decidir.

## 1. O que o playtest mostrou, por profissão

Pessoas por ofício na vila 33a6b9c4 (segundos ÷ 300 da janela): fundidor 3, carpinteiro 2,
pedreiro 2, lenhador 2, mineiro 2–3, construtor 1, fazendeiro 1, pastor 1, **sem ofício 6**.

| Ofício | Sem trabalhar | O que ocupava ou travava (frases do log em 1 h) | Já resolvido pela ADR-037 |
|---|---|---|---|
| Construtor | 67% | esperando `oak_log` (255); tora fora de alcance (15) | B1 (põe da zona), F1–F2 (árvores) |
| Lenhador | 4% | alvos a 84–167 blocos; 15 árvores em 1 h, todas replantio | F1–F3, L1 |
| Mineiro | 32% | esperando ramal (183); escada em cascata (5 níveis em 1 s) | M1 parte 1 |
| Fundidor | 86% | sem areia no raio (75); sem tarefa (48); 900 pedras convertidas sem pedido | — |
| Pastor | 74% | sem ovelha com lã (15); sem tarefa (14) | — |
| Carpinteiro | 47% | "metade da madeira fica em tora" (117) | — (depende de madeira) |
| Pedreiro | 14% | caminhando ao baú (45) | — |
| Fazendeiro | 0% | 31 colheitas; comida da vila: 1 ponto por adulto | R1, C7 (pão do trigo guardado) |
| Sem ofício | 100% | o mod apagava a caminhada deles à noite | V1 |

**A vila tinha 3 fundidores e 1 fazendeiro, 1 construtor.** A produção estava onde não havia
demanda (pedra) e faltava onde a obra parava (madeira, comida).

## 2. Propostas, por prioridade

### 🔴 P1 — Contratar pela demanda, não por cota fixa
- **Visto:** 3 fundidores com 86% de ócio enquanto a obra esperava madeira com 1 construtor e
  2 lenhadores; 6 aldeões sem ofício.
- **Proposta:** a cada reunião (30 s), a colônia pesa cada ofício pela espera que ele causa —
  material que a obra espera e quem o produz, comida por adulto, ócio medido no `WorkTime`.
  O ofício com ócio > 60% por duas janelas cede uma vaga; a vaga vai para quem produz o que a
  obra espera há mais tempo (madeira → lenhador; comida → fazendeiro; obra parada com
  material → segundo construtor). Desempregado é o primeiro candidato.
- **Mede:** proporção de "esperando material" do construtor e ócio do fundidor no `time_ledger`.

### 🔴 P2 — Rotina secundária de cada ofício (ADR-037 M1: cada um independente)
Sem tarefa, cada ofício faz algo do próprio ofício que ajuda a vila, em vez de ficar parado:

| Ofício | Rotina sem tarefa |
|---|---|
| Fundidor | recolhe areia e argila na beira d'água; sem fonte, coze o minério do baú da boca |
| Pastor | junta as ovelhas na vila e as faz procriar com o trigo da reserva; tosquia ao crescer a lã |
| Carpinteiro | faz as peças de madeira que a próxima obra da fila vai pedir (como o pedreiro já faz com pedra, ADR-036 8) |
| Mineiro | já feito: raspa pedra exposta enquanto espera ramal (M1) |
| Construtor | sem material, consertar caminhos e calçar a rua até a obra (Regra 15) |

- **Mede:** ócio por ofício abaixo de 40% (Regra 50).

### 🟠 P3 — Trajetos mais curtos
- **Lenhador:** com a floresta nas bordas (F1), a busca alternada centro/borda (item 18) manda
  o lenhador para o lado oposto da vila a cada vez. Proposta: buscar a partir **do próprio
  lenhador** primeiro, e só alternar centro/borda quando não achar.
- **Mina:** a boca nova abriu a ~115 blocos do centro. Proposta: a boca nova fica a no máximo
  a borda + 10, e entre bocas candidatas vence a mais perto do baú do mineiro.
- **Baú de trabalho:** pedreiro e carpinteiro andam ao baú a cada peça (45 + 31 frases).
  Proposta: tirar o material de 4 peças por viagem.

### 🟠 P4 — Conferir a medida de caminhada
- **Visto:** `walk` = 0% em 7 de 9 ofícios, incompatível com o log (o lenhador com alvos a
  84–167 blocos saiu "100% trabalhando"). O `WorkTime.classify` dá `walk` a quem se move e
  `work` a quem está com o braço balançando; a causa do zero **não está confirmada** — braço
  balançando enquanto anda, amostra por segundo, ou outra.
- **Proposta:** achar a causa com um GameTest de aldeão andando com tarefa, corrigir, e só
  então usar o `time_ledger` para provar P3.

### 🟡 P5 — Alívio do baú cheio que não gasta à toa (M3 manteve o alívio)
- **Visto:** ~900 pedregulhos viraram pedra e pedra lisa sem nenhuma obra pedir, com o teto de
  3 compartimentos fazendo o baú "passar da metade" mais cedo.
- **Proposta:** o alívio converte só para peças que alguma planta do catálogo do bioma usa, e
  para quando o produto chega a 3 compartimentos.

### 🟡 P6 — Mina mais útil e persistente (resto de M1)
- Ramais em direção ao minério que a colônia mais precisa (o `OreVein.BY_USE` já ordena).
- Ramal que acaba por "sem lugar de pé" põe degrau em vez de encerrar.
- Galerias guardadas no save para o mineiro voltar a elas depois de uma sessão (hoje o rastro do
  veio é só da memória).

### 🟢 P7 — Varredura de superfície (C6, pesquisa)
- A varredura já percorre a caixa da borda para dentro (`VillageSpiralSweep`) e pausa por
  orçamento. Duas melhorias baratas: (a) lembrar as colunas com recurso achadas em sweeps
  anteriores (índice como o das ruas) e começar por elas; (b) não reabrir a varredura de um
  recurso que deu "nada no raio" antes de 5 min (já existe para areia e lã; estender a terra,
  grama e argila).

## 3. Ordem sugerida

P4 primeiro (sem medida não se prova nada), depois P1 e P2, P3, P5, P6, P7. Cada um com
GameTest que falha antes e `time_ledger` no playtest seguinte.
