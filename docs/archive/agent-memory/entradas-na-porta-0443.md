---
name: entradas-na-porta-0443
description: "0.4.43, Regra 223, publicada; jogo u6XC34ZC0k confirmou sem bloco de nós, com 2 falhas de branco, socorro cedo e ruas cobertas; achados no TODO"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-07T13:14:45.732Z
---

0.4.43 publicada como `v0.4.43-alpha` (2026-11-07). O autor desenhou "como está / como deveria ser" sobre o jogo da 0.4.42 (Spark 7d0PErq0RJ).

**Why:** o desenho derrubou minha recomendação de juntar encontros próximos: ele manteve dois rosas a 1 bloco um do outro. Imagem do autor vale mais que a minha leitura do texto dele.

**How to apply:** o jogo seguinte (Spark u6XC34ZC0k, vila nova em 1270, 91, save "Novo mundo" recriado) deu 0 quadrados e 0 brancos soltos, e o socorro disparou pela primeira vez (52 ms, lado norte). Achados na branch local `docs/jogo-0443`, seção do `TODO.md`:

- `RoadGraph.hasLineNear` ignora toda aresta que termina no ponto: quina em cima da rua segue branca (`1242, 63, 66`).
- `plains_accessory_1` ganha branco sem porta (`1286, 72, 102`).
- Socorro dispara por tipo de obra, não por vila sem lote: a busca normal abriu obra 29 s depois.
- 218 colunas de rua cobertas por grama na primeira leitura; suspeita no solo suave, sem prova.
- Não fazer de novo: assumir pela imagem que dois encontros devem virar um. Em `114–115, -267` o teste dá um encontro só, diferente do desenho.

Para ler o save sem abrir o jogo: ler `villagecolony_colonies.dat` (`roadGraphs`) e os `.mca`.

Relacionado: [[tudo-pelo-grafo-0440]], [[mae-socorro-0442]].
