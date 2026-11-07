---
name: arco-ressuscita-por-isreplaceable
description: "isReplaceable() não distingue \"o jogador quebrou\" de \"ainda não construí\" — o arco da mina reaparecia infinitamente"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-12T03:12:21.216Z
---

Relato do autor em jogo, 2026-09-11: o arco de pedra com lanterna na entrada
da mina funciona, **mas "deve permitir que seja destruído normalmente e não
reaparecendo infinitamente"**. Ele quebrava o arco e o arco voltava.

A causa é um teste de estado insuficiente em `MineMouth.raiseArch` /
`lightTheTop`: o único portão é `world.getBlockState(at).isReplaceable()`.
Quebrar a pedra deixa ar, ar é substituível, e na passagem seguinte o mod
repõe. **Para o código, "o dono do mundo desfez" e "eu ainda não construí"
são o mesmo estado do mundo** — e a javadoc da classe promete o oposto
(*"o mod não discorda do dono do mundo"*).

O baú da boca **não** tem o defeito, e a diferença explica tudo: `furnish`
sai na primeira linha quando acha um baú nos vizinhos. Esse portão é o que
o arco não tinha.

A repetição da chamada é legítima e não deve ser removida:
`MineDigging.mineOf` (`MineDigging.java:704`) chama `furnishAndLight` em toda
passagem de mina já conhecida de propósito — mina de save anterior à Regra 30
(2026-08-22) nunca foi mobiliada, e boca em chunk descarregado falha na
primeira tentativa.

**Why:** "idempotente" lido do mundo só é verdade quando existe um vestígio
que o jogador não apaga ao desfazer. Com construção gratuita e reposta por
ciclo, ler o mundo não basta: apagar o vestígio *é* o ato de desfazer.

**How to apply:** para mobília que o mod põe de graça e reconfere por ciclo,
guarde no save que já foi posta uma vez, em vez de perguntar ao mundo se o
lugar está vazio. Decisão do autor (2026-09-11): sinalizador por mina no
save, e **uma** linha de log quando o mod desiste de reerguer — não por ciclo.
Save antigo lê `false` e mobilia uma vez, que é o conserto que a chamada
repetida existia para fazer.

**E a primeira correção teve o mesmo erro de forma menor** (2026-09-12): ela
marcava o arco com `if (!archWasUp && chest.isPresent())`, amarrando o arco ao
sucesso do baú. O `gauntlet-verifier` provou o buraco com um teste adversarial
— boca cercada, sem lugar para baú, o arco nunca é marcado e volta para
sempre ali. Dois sinais sem relação amarrados por conveniência. Fechado com um
record `MineMouth.Furnished(chest, archRaisedNow)` que os separa, e o critério
passou a ser **"a passagem tentou erguer"**, não "o baú apareceu". O caso
permanente é `MinerGameTest.theArchIsMarkedEvenWhenNoChestFits`.

**A lição de forma:** ao introduzir um sinalizador de "já fiz isto uma vez",
pergunte de quem é o "isto". Condicioná-lo ao sucesso de um vizinho no mesmo
método deixa o defeito vivo exatamente nos cantos onde o vizinho falha — e são
os cantos que ninguém testa.

Relacionado: [[sessao-de-jogo-destrava-o-plano]], [[jar-difere-so-por-crlf]].
