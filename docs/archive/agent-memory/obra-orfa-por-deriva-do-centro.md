---
name: obra-orfa-por-deriva-do-centro
description: O centro da vila oscila entre leituras; obra planejada num centro pode ficar fora do raio de outro e travar a vaga única em silêncio
metadata:
  type: project
---

O centro da colônia **não é estável**: é recalculado das camas vistas, e
camas entram e saem de chunk carregado. O log de 2026-09-15 registrou
**oito centros diferentes** na mesma sessão, de `616,-2863` a `640,-2891`.

Consequência: uma obra planejada quando o centro estava perto pode ficar
**fora do raio de 64** quando outro centro prevalece. A do log nasceu a
62,4 blocos e virou 78.

Por que passa despercebido — quatro coisas ao mesmo tempo:
- o relógio de paciência (`WaitingWork.giveUpIfStalled`) **só conta para
  `WAITING_RESOURCES`**, e a obra fica em `BUILDING`;
- `BuilderWork.step` sai com `return true` **silencioso** quando o aldeão
  não está em chunk carregado;
- a obra ocupa a **vaga única** (`no building work: one is already open`);
- o log repete `N blocks left` idêntico, sem dizer que nada anda.

**Why:** "obra aberta" e "obra que anda" são estados diferentes, e o log
não os distinguia. Estoque cheio + `blocks left` congelado + `one is
already open` é a assinatura.

**How to apply:** ao ver obra parada, compare `origin()` com **todos** os
centros que o log mostrou (`grep -oE "beds at ColonyPos\[...\]" | sort -u`)
e meça a distância — não só com o último. `ConstructionProject.isOutOfReach`
é o predicado. Relacionado a [[casa-nao-sobe-e-fome-nao-travamento]] e a
[[marca-escrita-e-nao-lida]] (o mesmo padrão: o sinal existe, ninguém lê).
