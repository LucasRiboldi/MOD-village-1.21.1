---
name: sorteio-sem-nada-para-sortear
description: A vila levantava sempre a mesma casa com o sorteio já escrito; o corte de pegadas repetidas esvaziava a lista antes
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-18T04:34:42.556Z
---

O sorteio entre casas de mesma pegada existia em `ConstructionPlanner.open`
desde 2026-09-09, com comentário dizendo que existia para acabar com "a
colônia levanta a mesma casa a vida inteira". **E a vila continuou
levantando a mesma casa até 2026-09-18.**

A causa não estava no sorteio, estava no que sobrava para sortear.
`HousePlans.catalogPlans` corta as pegadas repetidas (`sizes.add(plan.size())`)
e depois corta em `PLANS_OFFERED = 4`. Das 36 peças de planície sobravam 4,
uma por pegada. Sortear entre plantas de tamanhos todos distintos,
**filtradas pelo tamanho do lote**, é sortear entre uma.

**A lição:** um sorteio a jusante de um filtro de deduplicação não produz
variedade nenhuma, e o comentário que promete variedade continua verde e
mentindo. Quando um recurso "já existe e não funciona", leia o que alimenta
a entrada dele antes de mexer nele.

**A correção que não encarece:** o corte serve à busca de lote (dez minutos,
segundo o comentário do `PLANS_OFFERED`) e ficou. As irmãs voltam por
`HousePlans.siblingsOf`, chamada **depois** de o lote estar achado, quando a
pegada já é conhecida e medir acabou.

**A armadilha dentro da correção** — quase entrou: casar a pegada só pelo
eixo do arquivo. Quem chama compara o tamanho **depois** do giro da Regra 17
(ver [[validar-antes-de-transformar]]), então a irmã retangular vinha só
para ser descartada, e a que *só cabe girada* nunca era considerada. Ver
`HousePlans.fitsEitherWay`.

Relacionado: [[ordem-de-mapa-e-prioridade]] — ordem que é contrato e some em
silêncio; [[marca-escrita-e-nao-lida]] — a estrutura existe e ninguém a
consulta.
