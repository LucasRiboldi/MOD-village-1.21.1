# Diagnóstico da 0.3.103 e decisões do autor (2026-10-28)

O que se mediu na 0.3.103, os problemas encontrados, as opções de correção e a escolha do autor para cada
um. Este documento é a **base de comparação**: depois de jogar (ou simular) a versão com as correções, refaça as
medidas da §1 com os mesmos comandos e compare item a item (§4).

Fontes: duas simulações de 10 dias de uma taiga (semente `4522030530979676935`), a simulação das cinco vilas
naturais por 5 dias (semente padrão da bancada, `20261020`), o `PROJECT_AUDIT.md` e os testes da sessão.

## 1. Base de comparação (0.3.103, cinco vilas, 5 dias cada)

Comando: `python scripts/biome_sim.py plains desert savanna taiga snowy --days 5 --snapshot`.
Relatórios em `run/biome-sim/reports/<vila>/` (fora do git).

| Vila | Nota | Aldeões | Obras (concl./plan.) | Encalhes | Teleportes | Peças surgidas | Ciclo médio / p95 / pior | Ciclos > 1 tique | Níveis da mina |
|---|---|---|---|---|---|---|---|---|---|
| Planície | 8,0 | 9 → 16 | 4 / 5 | 46 | 7 | 40 | 28 / 63 / 445 ms | 29 | 4 (y −29) |
| Deserto | 7,6 | 7 → 14 | 8 / 9 | 30 | 13 | 8 | 16 / 25 / 1.386 ms | 8 | 0 |
| Savana | 7,7 | 7 → 14 | 3 / 4 | 12 | 5 | 30 | 21 / 52 / 536 ms | 17 | 1 |
| Taiga | 7,7 | 6 → 12 | 4 / 6 | 38 | 14 | 7 | 28 / 79 / 494 ms | 24 | 0 |
| Neve | 8,9 | 14 → 21 | 9 / 10 | 28 | 8 | 23 | 15 / 27 / 1.178 ms | 7 | 1 |

**Tempo parado por profissão (Regra 51: ócio + bloqueio + encalhe; teto 15%):**

| Profissão | Planície | Deserto | Savana | Taiga | Neve |
|---|---|---|---|---|---|
| Construtor | 62% | 41% | 66% | 40% | 47% |
| Carpinteiro | 29% | 42% | 53% | 37% | 37% |
| Fazendeiro (trabalho %) | ok (1%) | 42% (3%) | ok (4%) | 25% (0%) | ok (1%) |
| Lenhador | 17% | 23% | 37% | 35% | 32% |
| Pedreiro | 66% | 23% | 73% | 43% | 48% |
| Mineiro | ok | 26% | 41% | 49% | 34% |
| Pastor | 61% | 82% | 40% | 74% | 41% |
| Fundidor | ok | 41% | ok | ok | 28% |
| Sem profissão | 100% | 100% | 100% | 91% | 99% |

**Peças postas de lado por falta de material (soma das cinco vilas):**
- Por quem pôs de lado: construtor 642, pedreiro 276, carpinteiro 205.
- Por material:
  - bloco de neve 185, gelo compactado 162, neve 113;
  - tronco de acácia 138, tábua de acácia 50, degrau de acácia 46;
  - tronco de carvalho 86, tábua de carvalho 67;
  - tronco de espruce descascado 57, tronco de carvalho descascado 44;
  - pedregulho 55;
  - arenito liso 36.

**Motivos de recusa e desistência mais comuns (soma):**
- **Fundidor:** "não alcança" 82; "sem matéria-prima para o forno" 81.
- **Mineiro:** alvo alto demais (`TOO_HIGH`) 65; sem caminho 28; parado sem andar 21+13; não chegou perto 15+15; desvio falhou 9+9.
- **Pastor:** preso no curral ("fenced in") 45; sem ovelha com lã 19.
- **Fazendeiro:** "não alcança a planta" 27.
- **Lenhador:** "não chega perto da árvore" 15.
- **Encalhes** "parado duas vezes no mesmo lugar": mineiro 18, fazendeiro 18, construtor 18, lenhador 14.
- **Presos no curral** sem ser pastor: sem profissão 19, fazendeiro 14.

**Peças sem rota (`supply-gaps.jsonl`):** argila `NOT_FOUND` 30; tronco de acácia `NO_TASK` 26; tocha
`STALLED` 11; bloco de neve `STALLED` 11; carvalho `STALLED` 8; pedregulho `STALLED` 7; lã `NOT_FOUND` 5; neve
`STALLED` 5; vidraça `STALLED` 5 e `NO_ROUTE` 5.

**Diário da vila:** 5 `VC_DAY` e 5 `VC_AUDIT` por vila; toda auditoria com problema é "peças restantes já
no lugar" (deserto: 128 de 416).

## 2. Diagnóstico

### Corrigido nesta sessão (com teste)

1. Carregar o mundo movia o centro fixo da vila que cresceu (`3003e1a1`).
2. A mina salva antes da 0.3.96 retomava no cursor da geometria antiga (`d20c0d10`).
3. A obra pronta ficava presa em "construindo" e era largada meio pronta (Regra 148).
4. A nota do lote sobrevivia à troca de mundo (`16dfe634`).
5. O log dizia "0 blocks placed" numa casa construída (Regra 147).
6. Os fazendeiros voltavam 79 vezes ao mesmo alvo inalcançável (Regra 144).
7. O lenhador largava toras no chão com a vila a 38% (Regra 143).
8. As camas oscilavam de 15 a 7 e a 18 (Regra 145).
9. A luz e a lanterna da rua carregavam chunk (`95aaaa72`, `ce977bee`).
10. A obra era largada meio pronta (Regra 148).
11. Todo aldeão era teleportado toda noite (Regra 142).
12. O curral sem lote arrastava a vila para longe (Regra 141).

### Abertos

| Item | Problema | Evidência |
|---|---|---|
| E1 | Auditoria "peças restantes já no lugar" | `VC_AUDIT` nas cinco vilas; deserto 128/416 |
| E2 | 9 classes com estado estático fora do `ServerMemory` | registro congelado do ArchUnit (`archunit_store/9f6005cc-…`) |
| E3 | `ColonyState` com nome repetido (enum e retrato) | `core.colony.model` e `fabric.work` |
| E4 | Nota da bancada gravada no ponto de nascimento | relatório "forest (-128, 67, 32)" |
| T1 | Obra sem material | 642 + 276 + 205 peças de lado (§1) |
| T2 | Aldeão preso no curral | pastor 45, sem profissão 19, fazendeiro 14 |
| T3 | Teleportes que restaram | 5–14 por vila em 5 noites |
| T4 | Mineiro com alvo inviável | `TOO_HIGH` 65, `NO_PATH` 28 |
| T5 | Peças sem rota | argila, acácia, vidraça, tocha, neve |
| L1 | Picos do ciclo | 445–1.386 ms; não medido pelo Spark |
| L2 | Leituras de bloco sem guarda de chunk | 323 leituras contra 113 guardas |
| L3 | Mina parada no nível 0 | deserto (2.106 blocos cavados) e taiga |
| O1–O8 | Tempo parado acima da Regra 51 | tabela da §1 |
| D1 | Terreno sem lote | até 21 varreduras sem lote (taiga) |
| D2 | Código no limite | 8 arquivos com 500 linhas; 113 classes com mais de 10 dependências |
| D3 | Cobertura de teste do Fabric | 10–14% unitário (GameTest não medido) |
| D4 | Bancada com raio fixo | forceload de 96 blocos; a vila passa disso |

## 3. Opções e decisão do autor

| Item | Opção escolhida | O que fazer |
|---|---|---|
| **T1** | **A** — por material | O gelo compactado (o jogo só dá com toque suave) entra nas peças que surgem com registro; a neve ganha prioridade na rota da pá; o lenhador derruba a espécie que a obra pede (acácia); depois de N tentativas, a obra aceita a madeira equivalente do bioma |
| **T2** | **A** — pastor por cima da cerca | O curral não muda. O pastor tosquia e abate de fora da cerca, a até 4,5 blocos do olho (como o fazendeiro); só entra para levar animal, e fecha o portão ao sair. (A primeira leitura, "trocar o portão por porta", foi descartada pelo autor.) |
| **T3** | **B** — mapa de encalhe | A posição de cada teleporte e de cada encalhe vira um mapa (script) para achar os lugares que prendem |
| **T4** | **B** — filtrar antes | O mineiro descarta o alvo alto demais ou sem caminho antes de escolher, em vez de tentar e recusar |
| **T5** | **A** — rota própria | A argila vem do fundo de água rasa e de rio (busca mais longe), e surge depois de 3 buscas vazias; a acácia segue o T1; a vidraça ganha a cadeia areia → vidro → vidraça no fundidor |
| **L3** | **B**, só no deserto e na taiga | Espinha de peixe (escada direta às camadas de minério e corredores com ramais a cada 3) nos biomas em que a mina parou no nível 0 |
| **O1** | **A** | O pedreiro faz antes as peças de pedra que a próxima obra vai pedir (pela planta) e ajuda a assentar a pedra da obra aberta |
| **O2** | **A** | O construtor, com a obra parada por material, vai buscar o que falta (neve com a pá, árvore) ou prepara o próximo lote |
| **O3** | **A** | O pastor, sem ovelha com lã, cuida do rebanho (alimenta e cruza) e ajuda o fazendeiro com o trigo |
| **O4** | **B** | O carpinteiro faz antes as peças de madeira |
| **O5** | **A** | Árvore inalcançável é marcada e pulada por um dia; o bosque é plantado mais perto |
| **O6** | **A** | O fundidor coleta areia e argila mais longe |
| **O7** | **A** | O fazendeiro abre roça nova quando a colheita está baixa, faz pão e composto, e aplaina o canteiro irregular |
| **O8** | **A** | O adulto sem profissão vira carregador: leva do baú da boca da mina, do baú cheio e das peças da obra para os baús públicos |
| **D1** | **B** | "Smooth" na geração da vila: o terreno em volta das peças da vila Vanilla é suavizado na geração (`terrain_adaptation`) |
| **D4** | **A** | O raio forçado da bancada cresce junto com a caixa da vila durante a simulação |

**Não escolhidos agora** (ficam abertos): E1, E2, E3, E4, L1, L2, D2 e D3. As opções estudadas estão no histórico
da conversa de 2026-10-28; as recomendações eram:

- E1: riscar a peça que já está no lugar;
- E2: registrar no `ServerMemory`;
- E3: renomear para `ColonySnapshot`;
- E4: nota com `execute positioned`;
- L1: Spark antes de fatiar;
- L2: leitura segura comum;
- D2: dividir o `ColonyCycleRunner` por fase;
- D3: medir o `jacocoGametestReport`.

## 4. Como comparar depois de jogar ou simular

| Item | Medida | Antes (§1) | Como medir |
|---|---|---|---|
| T1, O2, O4 | Peças postas de lado por falta de material | 642 / 276 / 205 | `action_report.py` → `SET_ASIDE: no material` |
| T1 | Gelo compactado, neve e acácia de lado | 162 / 298 / 234 | `SET_ASIDE` por `what` no diário de ações |
| T2 | Presos no curral | 45 + 19 + 14 | `STRANDED: fenced in` |
| T3 | Teleportes por vila e lugares que prendem | 5–14; taiga: 9 casos na ponta oeste, 7 na rua sul | `stuck_map.py LOG diários --region DIR --center X,Z` |
| T4 | Alvos recusados do mineiro | `TOO_HIGH` 65, `NO_PATH` 28 (70 em cadeia, 23 depois de andar) | `TARGET_REJECTED` (recusa de quem andou) e `SKIPPED what=stone` (posta de lado antes de virar alvo) no diário |
| T5 | Argila e vidraça sem rota | 30 / 10 | `supply-gaps.jsonl` → `supply_gaps.py` |
| L3 | Níveis da mina no deserto e na taiga | 0 / 0 | `VC_STATE` → `mine.levels` |
| O1–O8 | Tempo parado por profissão | tabela da §1 | `time_ledger.py`, `VC_DAY` (`dayTime.rule51`) |
| D1 | Varreduras sem lote | até 21 | `found no lot` no log |
| Todos | Nota do bioma | 7,6–8,9 | `biome_report.py` |

Comparar com a mesma semente e os mesmos dias: `python scripts/biome_sim.py plains desert savanna taiga
snowy --days 5 --snapshot`. Em jogo: `/vc log`, `/vc bioma`, `/vc dados` e `python scripts/state_report.py`.

## 5. Achados durante as correções (abertos)

- **Mineiro anda até uma pedra 9 blocos acima e a recusa** (achado no T4). Das 23 recusas depois de andar, 21 são
  `TOO_HIGH` numa pedra 5–10 blocos acima da última que ele cavou (planície: +9 em todos os níveis, −26 → −17,
  −2 → 7, 22 → 31, 46 → 55). De longe a pedra passa (ele "ainda vai mudar de altura"), de perto não tem pé ao
  alcance. Custa ~175 tiques de caminhada por caso. Causa ainda não achada: falta saber de que fonte vem a pedra
  (escada, salão de 3 de altura ou o `roomBeside`). O T4 não resolve este caso — só tira as cadeias de recusa sem
  caminhada.
