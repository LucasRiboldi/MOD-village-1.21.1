---
name: encerrar-processos-ociosos
description: "Sempre encerrar processos em segundo plano já terminados ou inúteis (Gradle daemon/workers ociosos, servidores de teste, shells de fundo)"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-04T17:50:20.077Z
---

Sempre encerrar tarefas e processos em segundo plano que já terminaram ou não servem mais — sem esperar
pedido. Pedido do autor em 2026-11-04.

**Why:** num dia de builds, o daemon do Gradle acumulou 22 workers parados (~2,8 GB com o daemon) sem
nenhum build rodando.

**How to apply:** ao fim de cada etapa com build/teste, rodar `./gradlew.bat --stop` se nenhum build
estiver ativo, e matar shells/servidores de fundo que esta sessão abriu e já concluíram. Não matar
processos de outras sessões ativas (Codex, outra sessão do Claude, MCP com pai vivo) — só órfãos ou
desta sessão. Conferir com `Get-CimInstance Win32_Process` (pai vivo = em uso).
