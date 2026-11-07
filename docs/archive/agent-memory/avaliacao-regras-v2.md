---
name: avaliacao-regras-v2
description: "Avaliação do 'Regras Consolidadas v2' do autor (22-10): numeração 1–109 mantida, 0.3.93 com 74 transacional, 110–112, C-02; configuração desenhada e esperando 4 decisões"
metadata:
  type: project
---

Em 22-10 o autor enviou "Village_Colony_Regras_Consolidadas_v2.md", com 164 regras R-xxx e 10 resoluções C-xx.
A avaliação está em `docs/research/2026-10-22-avaliacao-regras-v2.md`: ~100 regras já valem, ~40 em parte, 13 são
novas e 7 são perigosas.

**Decisões do autor:**
- A numeração 1–109 fica. Cada R-xxx vira redação ou emenda da regra existente, e só o que é novo ganha 110+.
- Feito na 0.3.93 (branch feat/regras-v2-melhorias, PR #34 com merge automático ligado em 22-10):
  - 74 transacional (baú garantido e troca de baú remarcado; `Walks` mede a caminhada);
  - 110 `Cuttings`;
  - 21/32 `FunctionalPieces`;
  - 111 `SupplyException`/`Supplies`/`VC_SUPPLY`;
  - 112 `BlockCode` e o campo `code` no diário.
  JAR F107F3E9… instalado, não jogado.
- Sistema grande escolhido: a **configuração** (o autor trocou "área protegida" por ela).
  - Desenho em `docs/research/2026-10-22-desenho-configuracao.md`.
  - Decisões de 22-10: **por mundo**, **com sobreposição por colônia** (colônia → mundo → padrão), **comando e arquivo JSON** na pasta do mundo (`saves/<mundo>/villagecolony/config.json`, escrita atômica, `/vc config reload`). A lista da F1 não foi perguntada: vale a tabela inteira da §2 até o autor dizer outra coisa. Pronto para implementar; ainda sem código.

**Why:** o documento v2 supõe coisas que o mod não tem (transporte físico, configuração, área protegida) e propõe
regras que reabririam a obra eterna (R-118/R-138) e paralisariam o deserto (R-029).

**How to apply:**
- Qualquer R-xxx do v2 que aparecer de novo deve ser mapeado à regra existente pela matriz da avaliação.
- Futuro 🟢: área protegida, transporte físico, outros mods.

Relacionado: [[regra-74-cama-perto-do-trabalho]], [[decisoes-autor-2026-10-22-deserto]].
