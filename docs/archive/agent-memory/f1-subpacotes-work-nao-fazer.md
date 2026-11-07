---
name: f1-subpacotes-work-nao-fazer
description: "F1 do plano (mover work/ para subpacotes work.mine etc.) foi CANCELADO 15-10 — as classes usam package-private de propósito (147 métodos nas 35 de mina; 24 não-mina chamam mina sem import). Mover forçaria tornar tudo público = pior encapsulamento, não melhor."
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-15T12:20:17.634Z
---

**Ciclo F1 do plano (A2, subpacotes em `work/`): NÃO FAZER.** Premissa refutada por medição (15-10).

O `work/` tem 173 arquivos num pacote plano, e a varredura de 14-10 (A2) chamou de god-package,
sugerindo subpacotes `work.mine`/`work.build`/… com fronteira ArchUnit. **Mas:** as classes de `work/`
usam **visibilidade de pacote de propósito**. Nas 35 classes de mina há **147 métodos package-private**,
e **24 classes não-mina** de `work/` chamam classes de mina **sem import** (mesmo pacote).

Mover a mina para `work.mine` forçaria **tornar ~147 métodos públicos** + imports em dezenas de arquivos
— **enfraquece** o encapsulamento (tudo vira público) em vez de fortalecê-lo. É o oposto do objetivo do
F1, e a armadilha que [[climb-out-nunca-desiste]] já anota ("ArchUnit congelado muda com qualquer
dependência").

**Decisão:** F1 cancelado. O A2 é incômodo de organização, não defeito. O ArchUnit já impõe o que
importa (camada core↔fabric, ciclos sem subpacote via slices de fabric, ServerMemory). **Alternativa
leve, só se o acoplamento voltar a doer:** uma regra ArchUnit por convenção de nome (`Mine*`/`Miner*`
como slice lógico), sem mover arquivo nem mexer em visibilidade.

**Padrão desta sessão:** 3 itens do plano de melhoria foram resolvidos por **verificação/refutação**, não
por código — H2 (time_ledger já é só expediente), F3 (aceite "nenhum método>20" desproporcional) e F1
(subpacotes pioram o encapsulamento). Medir antes de executar poupou trabalho ruim. Ver [[time-ledger-ja-so-expediente]].

Relacionado: [[reanalise-estrutural-2026-10-13]], [[climb-out-nunca-desiste]].
