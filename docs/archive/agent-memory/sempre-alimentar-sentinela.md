---
name: sempre-alimentar-sentinela
description: "Regra do autor: toda rodada de teste passa pelo scripts/test_lab.py, para o Sentinela guardar a evidência"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-05T21:19:27.186Z
---

Sempre alimentar o Sentinela quando possível (autor, 2026-11-05). Rodar testes por `python scripts/test_lab.py <perfil>`, e não
por `gradlew test` ou `runGametest` direto, e depois `python scripts/sentinel_analysis.py`.

**Why:** a bateria de 2026-11-05 rodou direto no Gradle e não deixou rastro: `reports/test-runs/` nem existia no checkout
principal, e as 6 rodadas conhecidas estavam numa cópia de trabalho do Codex, a última de 2026-11-02.

**How to apply:** usar o perfil mais barato que responde à dúvida (`quick`, `fast`, `full`), com o Minecraft fechado. Cada regra
nova precisa de teste em suite do Sentinela, citando "Regra N" no teste. Relacionado: [[gametest-relogio-global-e-relatorio-velho]].
