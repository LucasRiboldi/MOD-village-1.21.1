---
name: movimentacao-natural
description: Regra do autor 21-10 — nada de escada fixa nem subida por pilar; a movimentação dos aldeões precisa ser a mais natural possível
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-21T12:46:46.122Z
---

O autor jogou a 0.3.81 e rejeitou: "o sistema de escadaria quebra o visual do jogo" e "o sistema do aldeão subir botando blocos embaixo dele de modo vertical eu também não quero, a movimentação precisa ser a mais natural possível". 0.3.82 (9eae5855) tirou VillageStairs e o modo PILLAR do ClimbOut (todas as profissões); só o mineiro cava (degrau/túnel), os outros só degrau natural (regra de 15-10).

**Why:** o mod não pode deixar marcas artificiais no mundo nem movimentos estranhos para resolver navegação.

**How to apply:** para problema de caminho (vila em duas alturas, [[deserto-jogado-0377]]), não proponha pôr bloco, escada ou pilar; procure saída natural (escolher quem trabalha onde alcança, casa/cama no nível da obra, caminho que já existe) e pergunte ao autor antes de alterar o terreno. Pendente: quem mora na parte baixa (y63) volta a não chegar à obra de cima.

**Saída escolhida pelo autor (21-10): "mudar de casa"** — 0.3.83 (d4b1c794): HomeMove dá ao aldeão que desiste 2× morando 2+ abaixo do trabalho a cama livre da vila na altura dele (ou troca com um sem ofício que mora lá); NightHome já o leva à cama à noite. Bancada 9,3/9,3/5,9; caso aberto: todas as camas de cima com aldeões de ofício. Limpeza das escadas/pilares da 0.3.81 no mundo do autor: ele escolheu "deixar".
