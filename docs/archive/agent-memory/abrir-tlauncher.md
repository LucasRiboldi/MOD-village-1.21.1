---
name: abrir-tlauncher
description: Como abrir o TLauncher e entrar no mundo pelo controle de tela — nomes de processo que a permissão aceita e as coordenadas da sequência
metadata:
  node_type: memory
  type: reference
  modified: 2026-10-08T01:31:29.278Z
---

Funcionou em 07-10 (o autor aprovou a permissão nesta vez):

1. `request_access(["TLauncher"])` e depois `request_access(["java.exe"])` — a janela do TLauncher
   pertence ao `java.exe` do `.tlauncher\starter\jre_default`.
2. `open_application("TLauncher")`; esperar ~20 s a tela de abertura.
3. Conferir na barra de baixo: conta "Majest", "Versão Fabric 1.21.1". Clicar **Entrar no jogo**.
4. Esperar ~60 s. A janela "Minecraft* 1.21.1" é do `javaw.exe` (`.minecraft\runtime\java-runtime-delta`):
   `request_access(["javaw.exe"])` só aceita **depois que a janela existe**; "Minecraft", "javaw" e
   "OpenJDK Platform binary" são recusados como inexistentes.
5. **Um jogador** → mundo único "Novo mundo" → **Jogar no mundo selecionado**.

Sem permissão de tela, só abrir: `Start-Process -FilePath "C:\Users\lucas\AppData\Roaming\.minecraft\TLauncher.exe"`.
Não contornar permissão negada com teclas simuladas ou iniciando o jogo por comando (o verificador barra).
Ver [[minecraft-test-install]].
