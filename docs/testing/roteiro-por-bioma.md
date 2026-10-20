# Roteiro de teste por bioma

Pedido do autor (2026-10-20): entrar num mundo de **um bioma só** e varrer os problemas
daquele bioma (profissões, recursos, construção, locomoção), até cobrir todos os biomas
do Minecraft. Este roteiro junta três camadas, da mais barata à mais fiel:

| Camada | O que mede | Como roda | Quanto custa |
|---|---|---|---|
| 1. Cobertura | o que o mod faz em **cada** bioma do registro (estilo, madeira, viveiro, enchimento da mina, limpeza de mata) | GameTest `BiomeCoverageGameTest` → `biome-coverage.csv` | segundos, na bateria |
| 2. Bancada | uma vila por bioma, sem jogador, dias de jogo acelerados | `python scripts/biome_sim.py <alvo> --days N` | ~1,5 min por dia de jogo |
| 3. Sessão de jogo | o que só o olho vê: casa torta, aldeão preso, rua estranha | você joga um mundo de bioma único; `/vc bioma` + `biome_report.py` | uma sessão por bioma |

A camada 1 diz **o que esperar**; a 2 mede **se acontece**; a 3 acha **o que a medida não
pega**. Todas usam a mesma régua de nota (0 a 10; ver §5).

## 1. Criar o mundo de um bioma só

No cliente (TLauncher, Minecraft 1.21.1, mod instalado):

1. **Um jogador → Criar novo mundo → Mais → Tipo de mundo: Bioma único → Personalizar**, e
   escolher o bioma. Comandos: **Sim**. Anote a semente (mesmo mundo, mesmo teste).
2. **Achar a vila:**
   - biomas onde o jogo gera vila (planície, deserto, savana, neve, taiga, meadow — e
     `sunflower_plains`, `snowy_taiga`, taigas antigas e `savanna_plateau` geram as vizinhas):
     `/locate structure #minecraft:village` e `/tp` até lá;
   - nos outros: `/place structure minecraft:village_plains ~ ~ ~` (ou `village_desert` nos
     ermos, `village_snowy` nos picos de gelo).
3. **Limite da vila posta:** o `/place structure` assenta os blocos mas não registra a vila
   como estrutura gerada. O mod a trata como vila feita pelo jogador: camas agrupadas por 32
   blocos (casa de uma cama longe das outras fica fora) e estilo pela maioria das camas.
   É o pior caso, e vale registrar como tal.

## 2. Durante a sessão

- **Fique dentro da vila**: ela só trabalha com jogador dentro (ADR-003 Emenda 6).
- `/vc log` — situação da vila e tempo dos ofícios.
- `/vc bioma` — o boletim do bioma: bioma e estilo, camas e aldeões, obra aberta, o que barra
  o lote, peças que surgiram sem rota, quem passa da Regra 51. Grava também uma linha
  `VC_BIOME` no log (a linha sai sozinha a cada 5 min).
- `/vc bioma nota <texto>` — anote **todo** problema visto, na hora e no lugar: a nota vai ao
  log com posição e bioma e aparece no relatório. Ex.: `/vc bioma nota pastor preso no cacto`.
- Acelerar: `/tick sprint 1d` (um dia de jogo). Desliga sozinho.

### O que olhar em cada bioma

| Tema | Onde olhar | Esperado hoje |
|---|---|---|
| Nascimento | log "Colony created" | nasce em qualquer bioma; casa fundadora limpa árvore e aplaina até 2 blocos |
| Casa fundadora | `levelled`/`cleared the BigHouseMOD lot` | assentada sem cortar a vila |
| Obras | "finished … at" | ≥ 1 obra por dia de jogo |
| Lote | `refusals=` no `VC_BIOME` | relevo (`OFF_ROAD_LEVEL`, `NO_GROUND`) ainda barra montanha e duna — terraplanagem, fora |
| Caminho | chão da rua nova | terra → caminho de terra; areia → arenito liso; areia vermelha → arenito vermelho liso; lama → lama compactada; neve/gelo → gelo compactado; pedra (só bioma de rocha) → cascalho |
| Mata fechada | árvores no lote | saem antes da obra (florestas, taigas, selvas, cerejeira, mangue); viveiro e bosque da colônia ficam |
| Mina | `SHORED` no diário | teto de areia vira arenito antes de quebrar; enchimento de arenito no deserto |
| Lavoura | fazendeiro | ⚠ roça nova exige grama/terra com ar em cima: neve em camada e deserto barram |
| Lenhador | lenhador | ⚠ bioma sem árvore depende do viveiro |
| Pastor | pastor | ⚠ não cria animal: neve e deserto quase não têm ovelha |
| Faltas | `supply-gaps.jsonl` | cada peça que surgiu sem rota fica registrada para ganhar rota própria |
| Locomoção | encalhe, teleporte pela cama | sem encalhe repetido no mesmo lugar |

## 3. Depois da sessão

```bash
python scripts/biome_report.py --out docs/testing/relatorios/<bioma>-<data>.md
```

Lê `%APPDATA%/.minecraft/logs/latest.log`, o diário de ações e o registro de faltas, e
escreve: a nota, a vila no fim, os sinais do log, o tempo das profissões, os problemas com a
pista provável e as suas notas. Complementos: `time_ledger.py`, `action_report.py`,
`cost_ledger.py` (desempenho) e `supply_gaps.py` (itens sem rota, acumulado de todas as
sessões em `villagecolony/supply-gaps.jsonl`).

**O log rota a cada sessão**: gere o relatório antes de abrir o jogo de novo.

## 4. Bancada automática (sem jogar)

```bash
python scripts/biome_sim.py desert taiga fixed-forest --days 2
```

Cada alvo gera `run/biome-sim/reports/<alvo>/` com log, diário, faltas e o mesmo
`relatorio.md`. `python scripts/biome_score.py` dá as notas lado a lado. Alvos: `--list`.

## 5. A régua (igual nas três camadas)

| Item | Pontos | Como |
|---|---|---|
| nasce | 2 | a colônia nasceu |
| constrói | 2 | obras concluídas por dia de jogo: ≥ 1 dá 2, > 0 dá 1 |
| trabalha | 3 | média de ocioso+bloqueado+encalhado: ≤ 15% (Regra 51) dá 3, ≥ 70% dá 0 |
| nada parado | 2 | nenhuma profissão ≥ 80% parada dá 2, uma dá 1 |
| estável | 1 | sem abandono, população não caiu |

**Aceite de um bioma:** nota ≥ 7, nenhuma profissão parada, pelo menos uma obra concluída por
dia e nenhum problema novo nas notas sem item correspondente no `TODO.md`.

## 6. Ordem dos biomas (de `biome-coverage.csv`, 53 do Overworld)

1. **Vila Vanilla (6):** plains, desert, savanna, snowy_plains, taiga, meadow.
2. **Estilo próprio sem vila (5):** sunflower_plains, snowy_taiga, old_growth_pine_taiga,
   old_growth_spruce_taiga, savanna_plateau.
3. **Mata fechada — limpeza de lote (11):** forest, birch_forest, old_growth_birch_forest,
   flower_forest, dark_forest, cherry_grove, jungle, sparse_jungle, bamboo_jungle,
   mangrove_swamp, grove.
4. **Terreno extremo (terra firme):** badlands, eroded_badlands, wooded_badlands,
   mushroom_fields, swamp, ice_spikes, snowy_slopes, frozen_peaks, jagged_peaks, stony_peaks,
   windswept_hills, windswept_gravelly_hills, windswept_forest, windswept_savanna, beach,
   snowy_beach, stony_shore.
5. **Água e subsolo (vila improvável):** rios, oceanos, deep_dark, dripstone_caves,
   lush_caves — só para ver se nada quebra.

Resultado de cada bioma: `docs/testing/relatorios/`, e a nota em
`docs/research/2026-10-20-biomas-previsao.md`.

## Referências

- Fabric GameTest — https://maven.fabricmc.net/docs/fabric-api-0.101.2+1.21/net/fabricmc/fabric/api/gametest/v1/package-summary.html
- Mundo de bioma único — Minecraft Wiki, *Single Biome* (tipo de mundo do 1.21) e a fonte de
  biomas `minecraft:fixed` num pacote de dados (o modo `fixed` da bancada).
- Comandos: `/place structure`, `/locate structure`, `/fillbiome`, `/forceload`, `/tick sprint`
  (Minecraft Wiki).
