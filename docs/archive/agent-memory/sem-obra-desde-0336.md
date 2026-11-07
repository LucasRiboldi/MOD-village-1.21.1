---
name: sem-obra-desde-0336
description: Nenhuma obra nova da 0.3.36 à 0.3.41 — "casa só com falta de cama" deixou a vila presa numa oficina sem lote; 0.3.42 rodízio por recusa + casa no lugar
metadata:
  type: project
---

Os logs mostram a última obra nova (`planned minecraft:...`) na 0.3.35. Da 0.3.36 até a 0.3.41, cinco sessões tiveram zero obras. A causa é a regra de 12-10, "casa só com falta de cama, uma oficina de cada tipo": com as camas sobrando, a vila só oferecia a oficina que faltava, nenhum lote cabia e nada a tirava da vez. Sem obra, tudo para em cascata: o construtor fica parado, o carpinteiro e o pedreiro param com o estoque no teto, e o autor via "todas as profissões produzindo menos".

A 0.3.42 (4290aa87) tira da vez, por 30 min, o tipo sem lote numa volta inteira (`LotTypeRefusals`). Com todos os tipos recusados, a vila constrói casa no lugar.

**Why:** a mudança de rodízio da 0.3.36 não tinha saída para o caso "o tipo escolhido nunca cabe". Antes, a casa alternava e escondia isso.

**How to apply:**
- Para medir obras por sessão: `zcat logs/*.log.gz | grep -c "planned minecraft"`. Faça isso antes de culpar ofício por ofício.
- A volta da varredura leva ~20 min, então cada tipo recusado custa uma volta.
- Ver [[profissoes-paradas-0339]].
