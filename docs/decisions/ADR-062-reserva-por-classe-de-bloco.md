# ADR-062 — A reserva por classe de bloco

**Estado:** aceita pelo autor em 2026-11-20, fase 2 do plano de ofícios e reservas
(`docs/research/2026-11-20-oficios-sempre-ativos.md`). Regra 340. Substitui a Regra 5 (fabricar até metade do
armazém) e a reserva de metade do cru da fornalha (Regra 257 tira dela a areia e a argila); muda o teto do fabricado de
`StockCeiling` (regra do autor de 2026-10-11).

## Contexto

O estoque dos baús tinha regras que não conversavam: metade da tora fica em tora, metade do cru fica longe da fornalha,
cada bloco fabricável até 16 ("um pouco de tudo", `ItemStock`), produto de valor até 16 (`ValueGoods`) e teto de 64 para
o fabricado. No jogo da 0.4.93 em planície a reserva de tora parou o carpinteiro 68 vezes com 0 ou 1 peça. Uma tora
guarda o mesmo número de blocos que uma cama, e as vilas usam milhares de tábuas e uma só mesa de cartografia.

## Decisão do autor

O estoque guardado nos baús segue a classe do bloco. Os pedidos das profissões e da obra vêm sempre antes da reserva.

| Classe | O que é | Guarda |
|---|---|---|
| **CRU** | o que o aldeão recolhe do mundo: tora, pedra, terra, areia, trigo e as colheitas, argila, neve | até 256 (teto) |
| **FABRICADO** | o que junta cru ou outros blocos e não aparece nas vilas geradas | 4 |
| **VILA** | fabricado com 10 ou mais blocos nas estruturas das vilas geradas | 12 |
| **ESPECIAL** | fabricado com menos de 10 blocos nas estruturas das vilas geradas (as bancadas, a fogueira, cama e tapete de cor rara) | 1 |

1. **A contagem das vilas** sai das estruturas Vanilla `village/<estilo>/...` dos cinco estilos, sem as vilas zumbi
   (`scripts/village_block_counts.py`), e fica num arquivo de dados do mod
   (`data/villagecolony/stock/village_blocks.json`). Cama conta os dois blocos.
2. **O natural que também tem receita** (pedra, granito, arenito, argila, neve, gelo, trigo, melancia e os outros de
   `data/villagecolony/stock/natural_blocks.json`) é CRU: o aldeão o pega do mundo.
3. **As bancadas do mod** nunca entram na lista de fabricação das profissões nem em reserva.
4. **Sai a metade.** A tora não fica mais guardada pela metade (Regra 5), e o cru vai à fornalha sem a reserva de metade:
   o cru vira fabricado para atender pedido ou completar a reserva da classe, que é pequena. O teto de 256 do cru fica.
5. **O teto do fabricado** passa a ser a reserva da classe; a meta da obra continua passando dele. Abaixo da reserva
   sai ao menos uma receita, mesmo que ela renda mais que a reserva: sem isso, o tijolo de pedra (receita de 4,
   reserva 1) e a vidraça (receita de 16, reserva 12) nunca seriam feitos.
6. **Fora da decisão.** As travas pequenas de cru (lã 16, pó de osso 16, trigo 8, semente 8, areia 16, lingote 64, cru
   dos produtos de valor 64, manutenção 32, peças de bioma 32) ficam como estão até a medida da fase 4 (`VC_KEEP`).

## Consequências

- O carpinteiro deixa de parar pela tora guardada; a barreira que fica é a falta de tora (Regra 339).
- O estoque de fabricado encolhe: 4 de cada em vez de 16 a 64. A obra pede o que precisa por cima disso.
- A classe de cada bloco e a reserva podem ser ajustadas pelos dois arquivos de dados, sem código; a fase 3 (`VC_BLOCKS`,
  `/vc blocos`) mede o gasto de cada bloco para essa revisão.
- Risco: obra que espera peça de fabricado agora começa com menos estoque pronto. A medida da fase 3 mostra se a obra
  passou a esperar mais.
