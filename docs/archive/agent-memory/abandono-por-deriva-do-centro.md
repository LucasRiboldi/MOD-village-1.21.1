---
name: abandono-por-deriva-do-centro
description: 0.3.42 soltou os 27 aldeões — anexos em série moveram o centro 68 blocos e a sonda (64 do centro) declarou ABANDONED; 0.3.43 corrige; save do autor gravado com 0 trabalhadores
metadata:
  type: project
---

Na 0.3.42, cada tipo de obra recusado contava como "vila cheia", e a vila anexou terreno 4 vezes em 5 minutos. A caixa cresceu 64 blocos a leste e o centro ficou a 68 blocos das camas. A sonda de abandono parte do centro e só aceita vila a até 64 blocos (`DUPLICATE_DISTANCE`), então marcou a colônia como ABANDONED e chamou `dismissColony`. O save foi gravado com 0 trabalhadores. O autor viu "sumiram as placas, o overlay, tudo retrocedeu".

A 0.3.43 (e4ef98ac) corrige:
- `ColonyAbandonment.sees` passou a aceitar vila dentro da caixa, o que também revive a colônia em 2 leituras;
- `peopleStillHome`: 3 ou mais trabalhadores vivos dentro da caixa impedem o abandono;
- anexo no máximo a cada 30 min, e só no lado aonde chega rua (`StripRoad.find`).

**Why:** tudo que move o centro (anexo, crescimento) afeta a sonda de abandono e as buscas que partem do centro.

**How to apply:**
- Se "tudo sumiu", procure `is now ABANDONED` e `workers dismissed` no log.
- A caixa do save ficou larga a leste.
- Nos GameTests de colônia, a colônia fundada pela detecção veta lados ("foreign") e cobre arenas; isole retirando-a do registro durante a pergunta.
- Ver [[sem-obra-desde-0336]] e [[playtest-0337-baus-publicos]].

**0.3.44 (583b334a):** a 0.3.43 reviveu a colônia, mas anexou de novo sem rua nem obra. O autor rejeitou a "faixa vazia": a caixa só cresce com rua e obra. A expansão C agora só puxa a rua para fora (`StripRoad.lead`). `VillageBoundsSave.CURRENT_MODEL` = 3 manda medir de novo a caixa inchada, procurando a vila gerada no meio das construções.

**How to apply:** não volte a anexar terreno vazio. Crescimento da caixa vem de `byRoad` e `byPiece`.
