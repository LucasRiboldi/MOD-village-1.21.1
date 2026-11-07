---
name: sem-bau-nao-recebe-tarefa
description: "no task open for it" pode ser trabalhador SEM BAÚ — confira "Registered N storages" ao carregar antes de caçar a meta
metadata:
  type: feedback
---

Sessão de 2026-09-26 (4h51): lenhador e mineiro "sem tarefa" a sessão inteira, e eu segui a meta (ColonyGoals) e o espaço dos baús antes de achar a causa: ao carregar, `Registered 5 storages` — lenhador, mineiro e pedreiro sem baú, e `WorkAssignment.assign` exige `hasStorage`. As tarefas estavam abertas (7–11) e ninguém elegível. `LumberjackReport` chama isso de NO_TASK porque só conta tarefa reservada/em execução.

**Why:** o relatório de ócio mente sobre a causa quando o trabalhador não tem baú; custou uma hora de leitura de código.

**How to apply:** diante de "no X work: no task open", primeiro conte `claimed the chest` / `Registered N storages` por profissão no log. Ver [[frase-ausente-refuta-mecanismo]].
