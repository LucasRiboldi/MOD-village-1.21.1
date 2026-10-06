# ADR-034 — Emendas à Constituição e reconciliação da fundação

**Status:** Accepted
**Date:** 2026-10-06
**Decision Type:** Governance / Gameplay
**Origem:** auditoria de recuperação, conflitos C-01, C-04/C-05, C-06, C-10 e
C-11 de `docs/audit/RULE-RECONCILIATION.md`; decisões D-04 e D-05 do autor.

## Contexto

A auditoria de 2026-10-06 achou a Constituição dizendo o contrário do código
em quatro seções. Em todas, a decisão nova já existia (ADR ou Regra), mas a
seção da Constituição nunca recebeu a emenda — a Amendment Policy exige ADR
para cada exceção, e a ADR não tinha voltado à Constituição.

| Seção | Texto original | Decisão que já valia | Código |
|---|---|---|---|
| §4 Preserve Vanilla AI | aldeão volta à rotina Vanilla | ADR-029: ofício da colônia exclui o ofício Vanilla | `VanillaProfessionGuard`, `VillagerDataMixin` |
| §5 Preserve Vanilla Villages | nenhum bloco da vila gerada é tocado | Regra 3 e exceções de rua (30-09), lote (18-09), fuga e base de obra (02-10) | `BlockProtection.mayDigOut`, `mayBuildOver` |
| §7 Construction Uses Vanilla Structures | só templates Vanilla | Regra 27-e3: modelos próprios da colônia têm prioridade | `ColonyModels`, `big_house_mod.nbt` |
| §9 Resource Conservation | nada surge sem origem | ADR-028 retirou a regra em 2026-09-30 | ver abaixo |

E uma ADR contradizia outra: a ADR-030 (10-02) diz que o carpinteiro fica fora
da fundação e o construtor fora do crescimento; a Regra 35 e a emenda da
ADR-011 dizem o contrário, e o código segue a Regra 35.

## Decisão

Palavras do autor (2026-10-06): *"D04 emendar (alguns itens podem sim surgir do
nada)"*, marcando os quatro itens perguntados; *"O código atual"* para a
fundação.

1. **§9 — o que pode surgir sem origem.** Confirma a ADR-028 e acrescenta os
   itens que a ADR-028 não listava. São comportamento previsto:
   - ferramenta inicial do trabalhador contratado (`WorkerEquipment`);
   - a BigHouseMOD colocada de uma vez na fundação, e um adulto por cama dela
     (`BigHouseFoundation`, `VillageFoundation`);
   - pedregulho criado pelo aldeão preso para sair, quando o baú não tem
     (`ClimbOut`);
   - peça sem rota no bioma, no baú na 3ª tentativa (`BiomeConstructionSupply`);
   - os quatro caminhos e os ingredientes de drop da ADR-028.

   Continuam valendo os limites da ADR-028: **lava nunca é assentada**; o que
   tem cadeia dentro da colônia usa a cadeia; tora, pedra, terra, areia, lã e
   trigo só vêm das profissões.
2. **§4** — exceção da ADR-029: o aldeão com ofício da colônia não mantém
   ofício nem comércio Vanilla. Fora da tarefa, o resto da rotina Vanilla
   (dormir, comer, fugir, socializar) continua.
3. **§5** — o chão do bioma dentro de peça da vila (`LotGround.isBiomeGround`)
   não é peça da vila: pode virar rua, lote, base de obra e saída do aldeão
   preso. Cerca, escada, baú, cama, posto de trabalho e qualquer bloco
   construído da vila continuam intocáveis.
4. **§7** — os modelos próprios da colônia (`data/villagecolony/structure/`)
   têm prioridade sobre o catálogo Vanilla quando existem (Regra 27-e3); sem
   modelo, vale o catálogo do bioma.
5. **Fundação (D-05)** — vale o código e a Regra 35: sete titulares na
   fundação (MINER, LUMBERJACK, CARPENTER, MASON, SMELTER, SHEPHERD, BUILDER;
   agricultor fora) e o construtor também no crescimento. O parágrafo da
   ADR-030 que diz o contrário fica superado por esta ADR; a política do Mod
   Menu (habilitar, teto, ordem, raio) continua como a ADR-030 a descreve.

## Consequências

- `PROJECT_CONSTITUTION.md` sobe para 1.2.0, com uma nota de emenda em §4,
  §5, §7 e §9. O texto original fica, para que a história continue legível.
- Nenhum código muda. Os comentários que ainda dizem "seis titulares"
  (`VillageChests`, `VillageFoundation`) são corrigidos junto com a
  integração das linhas de trabalho.
- Toda decisão nova que contrariar a Constituição emenda a seção **no mesmo
  commit** (Protocolo PASSO, `docs/audit/RECOVERY-ROADMAP.md` §6).
