# Village Growth Planner — a vila que nunca termina

> **Especificação do autor, 2026-09-16.** Registrada integralmente porque é
> direção de projeto, não tarefa. **Nada foi implementado.**
>
> A frase que a resume: *"eu não usaria uma fila fixa de prédios que termina
> depois de 10 ou 20 construções. A melhor solução é uma ordem de fundação +
> um ciclo permanente de expansão."*

---

## 1. O que o autor pediu

Uma vila autônoma que **produza seus próprios recursos, construa, aumente a
população e continue crescendo sem depender do jogador** — e que, depois de
ter tudo, **não entre em `STATE = FINISHED`**, mas em **expansão
permanente**.

A peça central é um **planejador acima do atual**, que transforma:

```text
estado econômico → necessidade → estrutura → lote → construção
```

E a regra de implementação que o autor destacou:

> *"Não implemente isso como uma fila 1 → 52. Implemente como prioridades
> condicionais"* — e, melhor ainda, como **`NEED_SCORE(structure)`**, em que
> cada estrutura recebe uma necessidade calculada e a maior pontuação vence.

```text
Farm         food_deficit          × 100
House        housing_deficit       ×  90
Wood         wood_deficit          ×  80
Mining       mineral_deficit       ×  80
Processing   processing_deficit    ×  70
Road         expansion_need        ×  60
Specialized  specialization_need   ×  30
```

O efeito buscado: *"uma vila desértica escolhe uma necessidade diferente de
uma vila de taiga sem criar dois sistemas completamente diferentes."*

## 2. A ordem de fundação — desbloqueio, não fila

| Estágio | Conteúdo |
|---|---|
| **0 — Diagnóstico** | Detectar vila; registrar construções, população, camas, produção, armazenamento, estradas, recursos |
| **1 — Sobrevivência** | Fazenda · Armazenamento · Madeira · Mineral · Processamento |
| **2 — Infraestrutura** | Estrada principal · Primeiros lotes · Primeiras casas · Iluminação |
| **3 — Crescimento** | Casas e fazendas adicionais · Cercados/animais · Mais armazenamento e processamento |
| **4 — Autonomia** | Oficina de madeira e mineral · Fundição · Pedreiro · Ferramenteiro · Armeiro |
| **5 — Especialização** | Açougue · Pescador · Pastor · Curtidor · Flecheiro · Bibliotecário · Cartógrafo · Clérigo |
| **6 — Organização** | Centro da vila · Rede de estradas · Distritos · Iluminação pública |
| **7 — Crescimento infinito** | Rediagnosticar → calcular → estrada → lotes → construir → atualizar capacidade → repetir |

**A lista é ordem de desbloqueio, não fila cega.** O mod começa numa vila
Vanilla que talvez já tenha duas fazendas, cinco casas e um ferreiro — por
isso o Estágio 0 é obrigatório e a primeira pergunta é *"qual é a primeira
capacidade que está faltando?"*.

## 3. As regras que governam o ciclo

- **População controla casas.** `HousingCapacity = camas utilizáveis`. Só
  constrói casa quando `Population > Beds` — senão a vila vira bairro vazio.
- **População controla fazendas.** Fazenda só quando
  `FOOD_RESERVE < MINIMUM`; acima do alvo, não. Evita *"20 fazendas, 3
  casas, 4 aldeões"*.
- **Cada transformação tem receita explícita.** Nunca `GROUP = STONE`
  assumindo que qualquer bloco do grupo serve — é o defeito que já mordeu
  com `SANDSTONE` e `COBBLESTONE`.
- **Paleta por bioma.** Carvalho/cobblestone na planície, spruce na taiga,
  sandstone no deserto. Não "oak para toda vila".
- **Estrada é exclusivamente** `DIRT_PATH`, `GRAVEL`, `TERRACOTTA`, e
  construção não a ocupa (P0.7 / ADR-017).
- **Depois de tudo pronto: expansão por melhoria.** Casa pequena → média →
  grande; fazenda pequena → média → grande; estrada → rede → **distritos**
  (residencial, produção, agrícola).

## 4. O que já existe no mod, e o que falta

⚠️ **Conferido contra o código em 2026-09-16.**

**Já decidido e aceito:** a [ADR-009 — Autonomous Village
Evolution](../decisions/ADR-009-Autonomous-Village-Evolution.md), de
2026-08-22, já estabelece o princípio
(`BIOMA + TERRITÓRIO + RECURSOS + PRODUÇÃO + POPULAÇÃO + NECESSIDADES →
DECISÃO AUTÔNOMA`), o perfil de vila, o inventário do território, a cadeia
produtiva, a reserva de sobrevivência e que *grupo não é equivalência*.
**Esta especificação a confirma e detalha; não a contradiz.**

**Já analisado antes:** [`analise-plano-crescimento.md`](../analise-plano-crescimento.md),
de 2026-09-12, confrontou uma especificação parecida com o código e concluiu
que *"a especificação descreve um mod que em boa parte já existe, e nenhum
dos sistemas novos que ela pede é o que está travando a vila hoje."*

**O que de fato falta — e é o núcleo do pedido:** hoje
`HousePlans.houseFor` escolhe a planta **pelo tamanho** (a maior que cabe,
Regra 25), e não por necessidade da vila. **Não existe `NEED_SCORE`, nem
`VillageInventory`, nem estágios de desbloqueio.** Essa é a lacuna real.

## 5. Por que isto não veio antes do E45/E46

**O playtest de 2026-09-16 21:41 decidiu a ordem.** A colônia tinha em
estoque:

```text
OAK_PLANKS=5134, OAK_LOG=1288, WHEAT=95, CARROT=193,
BEETROOT=77, SMOOTH_STONE=183, GLASS=116 … e 37 camas
```

E construiu **zero** casas em 31 minutos. Não faltava recurso, planta nem
espaço: faltava que o mineiro produzisse pedra (E45) e que a obra não
morresse aos 30 segundos (E46).

**Um `NEED_SCORE` perfeito, rodando nessa vila, teria calculado "preciso de
casa", mandado construir — e a obra morreria igual.** Por isso os dois
defeitos vieram primeiro, por decisão do autor.

## 6. Ordem sugerida para implementar

1. **Playtest do JAR de 2026-09-17** — confirma E45 e E46 corrigidos. Sem
   isso, o planejador nasce sobre chão que não se sabe firme.
2. **ADR do Growth Planner** — decide onde ele mora, como se liga ao
   `ConstructionPlanner` atual e o que acontece com a Regra 25.
3. **`VillageInventory`** (Estágio 0) — o diagnóstico do que a vila já tem.
   É a peça que falta e da qual todo o resto depende; é `core` puro, e
   portanto testável sem servidor.
4. **`NEED_SCORE`** — substitui `houseFor` como quem escolhe o alvo.
5. **Estágios e expansão permanente** — em fatias, cada uma com playtest.

⚠️ **Nada disso está implementado.** Este documento é o registro da
especificação, não um relatório de trabalho feito.
