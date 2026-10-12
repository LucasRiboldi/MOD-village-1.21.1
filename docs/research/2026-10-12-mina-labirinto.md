# Por que o mineiro fazia labirinto (playtest 0.3.35, 2026-10-12)

> Pergunta do autor: *"por que os mineiros estão construindo labirintos em vez de
> minerar?"* Spark: https://spark.lucko.me/qVwFynoGoi

## Resposta

A mina em rede da 0.3.35 (ADR-042) foi desenhada como uma grade de corredores, e o
mineiro cumpria a grade em vez de buscar minério:

| Medida no log | Valor |
|---|---|
| Nós de corredor abertos | 54 em 34 min (y 28–30) |
| Geometria | grade 9×9, corredor 3×3, nó a cada 7 blocos |
| Blocos cavados | 1.411 |
| Minério tirado | 2 de ferro cru |
| Blocos postos pela segurança | 2.392 (parede em toda caverna atravessada) |
| Recusas de alvo em laço | 4.210 |
| Peso do mod no Spark | 6,4% (busca de enchimento nos baús a cada tique) |

1. **Só o minério exposto contava** — a veia dentro da parede, a um bloco do túnel,
   não entrava; o túnel seguia a grade.
2. **Corredor largo demais**: 3×3 cava 9 blocos por passo; 1×2 cava 2 e enxerga a
   mesma parede.
3. **A segurança emparedava tudo**: cada caverna ao lado do corredor virava parede,
   gastava o pedregulho e devolvia a mina a esperar enchimento.
4. **Alvo recusado voltava**: o bloco que a marca recusava era oferecido de novo.

## Correção (5aa1af84)

- Túnel de ramal 1×2 a cada 4 blocos, raio 6 (`MineNetwork`).
- Todo minério ao alcance do braço (4) a partir de qualquer túnel aberto vem antes do
  túnel, na ordem de prioridade da veia (`NetworkVeins`), com varredura a cada 100 tiques.
- Segurança mínima: piso, fluido encostado e beirada de queda — não toda parede.
  Sem enchimento, espera 200 tiques em vez de reler os baús (`NetworkSafety`).
- Alvo recusado conta falha no nó; 3 falhas selam o nó (`NetworkDigging`).
- Retorno subindo nunca quebra as 5 camadas sob a vila (`MineReturn.SHELL_DEPTH`).

Verificação: GameTest do minério dentro da parede, do piso furado, da beirada e do
nível esgotado. **Falta ver em jogo** que a proporção minério/bloco cavado subiu.
