# Reanálise estrutural das profissões (2026-10-13)

> Pedido do autor: *"antes da auditoria anterior estavam funcionando bem, agora estão todas ociosas,
> travadas"*. Este documento mede quando a produção caiu e por quê, sessão a sessão, a partir dos
> logs de jogo (`%APPDATA%/.minecraft/logs/2026-10-*.log.gz`). Scripts: scratchpad `history.py`
> (o `time_ledger.py` por sessão) e `production.py` (produção por minuto).

## 1. Primeiro: a régua mudou em 07-10

O "% de trabalho" do `time_ledger` caiu de 50–99% para 0–25% em **todas** as profissões de uma vez
entre a 0.3.1 e a 0.3.7. [FATO] O commit `0aceda49` (07-10, "o braço do aldeão desce depois de lido
no WorkTime") corrigiu a medição: antes, o braço levantado contava como trabalho e ficava levantado.
[INFERÊNCIA] Os números de antes eram inflados; a comparação honesta entre épocas é a **produção**.

## 2. Produção por minuto, sessão a sessão

| Época (versão) | Minerou/min | Fundiu/min | Árvores/min | Peças/min | Obras novas por sessão |
|---|---|---|---|---|---|
| 03-10 a 06-10 (0.3.0–0.3.1) | **20–86** | 0–50 | 0,2–1,0 | 0–6 (22 numa) | 1–4 |
| 08-10 a 10-10 (0.3.12–0.3.23) | **0** na maioria (mina quebrada) | 0–5 | 1,1–3,4 | **10–20** | 0–3 |
| 11-10 (0.3.24–0.3.31) | 10–25 | 4–11 | 1,1–1,9 | 3,5–12 | 0–5 |
| 12-10 (0.3.37–0.3.44) | 0,1–5 (uma sessão 46) | 0–9 | 0,7–5 | **0,6–2,5** | **0** |

[FATO] A última obra nova foi planejada na 0.3.35. Da 0.3.36 até a 0.3.44, zero obras em todas as
sessões.

## 3. As quatro causas estruturais

### C1 — A economia inteira é puxada por uma porta só: a obra
[FATO] O carpinteiro, o pedreiro e o fundidor produzem para (a) a peça que a obra aberta espera,
(b) o adiantamento com teto (`AdvanceStock`) e (c) "um pouco de tudo" (`ItemStock`, 8 de cada).
O mineiro recebe pedido de pedra pela meta da obra e pelo alívio de baú.
[FATO] Desde a 0.3.36 ("casa só com falta de cama, uma oficina de cada tipo") a vila só oferece a
oficina que falta; nenhuma achou lote; nenhuma obra abriu.
[INFERÊNCIA] Sem obra, (a) não existe, (b) bate o teto em minutos e (c) acaba em 8 de cada — todo o
artesanato para. É a queda das peças de 10–20/min para 0,6–2,5/min.

### C2 — Os tetos de estoque amarram a produção à obra (11-10)
[FATO] `023779ea` (P1) e `f10f8496`: o adiantamento só vai até o maior uso da peça numa casa do
estilo mais o que a obra aberta pede; teto geral 64 fabricados, 256 crus. Peça fora das plantas tem
teto zero.
[INFERÊNCIA] Foi a correção certa para o acúmulo de 318 lajes, mas, junto com C1, deixa o artesão
sem nada a fazer assim que a casa típica está coberta.

### C3 — A mina mudou de desenho quatro vezes em dois dias
[FATO] 0.3.32 (boca só na borda), 0.3.34 (mineiro destravado), 0.3.35 (mina em rede, ADR-042),
0.3.36 (túnel 1×2 e segurança mínima), 0.3.37 (enchimento tirado da parede), 0.3.40 (pedra
recusada). Mineração: 10–25/min antes da rede, 0,1–5/min depois (uma sessão com 46).
[FATO] O fundidor depende do pedregulho do mineiro: "none of 39 colony chests had cobblestone"
(0.3.41). Mina fraca ⇒ fundidor parado (50–57% de espera).

### C4 — A vila só trabalha com o jogador perto, e a busca de lote é lenta e estreita
[FATO] Regra de foco (Emenda 6): sem jogador na caixa, nada roda — até a 0.3.45, que abriu um
perímetro de 48 blocos. Uma volta da busca de lote levou ~20 min (0.3.39–0.3.41); 115–185 mil
candidatos recusados por volta, a maioria por encostar na rua ou fora do nível dela; terreno vazio
longe da rua nunca era candidato (a 0.3.45 aceita lote 3 blocos atrás da rua).

### Agravantes de 12-10 (já corrigidos)
- 0.3.37–0.3.43: anexo de faixa vazia em disparada → centro andou → colônia dada como abandonada
  (0.3.42, 27 aldeões soltos). Corrigido na 0.3.43/0.3.44.
- 0.3.38–0.3.40: lenhador em laço com baú cheio; mineiro preso em pedra recusada. Corrigidos.

## 4. O que recomendo, em ordem

1. **Destravar a obra (C1)** — decisão do autor. Opções:
   - **A.** Voltar ao rodízio de antes da 0.3.36 (casa ↔ outro tipo), com a casa limitada a, por
     exemplo, camas ≤ adultos + 4 — a vila volta a crescer sempre.
   - **B.** Manter "casa só com falta de cama", mas a casa entra no lugar da oficina que não achou
     lote **na primeira volta** (hoje: depois de recusar cada tipo, ~20 min por tipo).
   - **C.** Manter como está (a 0.3.42/0.3.45 já cobrem parte) e medir.
2. **Desamarrar o artesanato da obra (C2)**: sem obra aberta, o teto do adiantamento passa a ser a
   maior casa **mais uma reserva fixa** (por exemplo, 32 de cada peça das plantas), e o "um pouco
   de tudo" sobe de 8 para 16.
3. **Congelar o desenho da mina (C3)**: nenhuma mudança de geometria até um playtest de 30 min
   medir minério/min; se ficar abaixo de 10/min, voltar ao modelo de ramais de 03-10 (20–86/min).
4. **Busca de lote (C4)**: a passagem extra por segundo (`SweepCadence`) só roda com varredura em
   curso; fazê-la rodar também na volta pelo índice de ruas, para a volta levar minutos e não 20.

Cada item é independente; o 1 é o que mais destrava, porque C2 e boa parte de C3 vêm dele.
