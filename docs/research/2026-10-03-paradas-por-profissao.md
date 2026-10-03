# Paradas por profissão e revisão da vila — 2026-10-03

Pedido do autor depois do playtest da madrugada (Spark `r6nErbWNSL`): rever
cada profissão, onde ela trava e por que fica parada, achar outro caminho
quando insiste sem sucesso, rever como a vila é calculada, varrida, acha
baús e põe todos para trabalhar, e fazer a arte em pixel aparecer.

**Fontes:** 12 logs de jogo de 30-09 a 03-10 (`scripts` ad hoc sobre
`VC_ACTIVITY` e frases de parada) e o `VC_TIME` da sessão das 01:02, que é a
única com medição de tempo (Regra 50). **Nada aqui foi visto em jogo depois
das correções.**

## Tempo por profissão (sessão de 01:02–01:32)

| Profissão | Trabalho | Andando | Esperando | Bloqueado | Ocioso | Veredito |
|---|---|---|---|---|---|---|
| Lenhador | 98% | 2% | 0 | 0 | 0 | ok |
| Fazendeiro | 98% | 2% | 0 | 0 | 0 | ok |
| Carpinteiro | 55% | 11% | 4% | 2% | 29% | ok, mas fecha tarefa com 0 peça |
| Minerador | 52% | 18% | 5% | 1% | 21% | ok |
| Fundidor | 42% | 0 | 0 | 0 | 58% | fluxo — sem areia |
| Pedreiro | 30% | 3% | 0 | 0 | 67% | fluxo — sem tarefa |
| Construtor | 26% | 6% | 0 | 3% | 65% | fluxo — obra única, material |
| Pastor | 0% | 42% | 58% | 0 | 0 | **travado** — segurava a tarefa sem ovelha |
| Sem ofício | 0 | 0 | 0 | 0 | 100% | ~5 aldeões a sessão inteira |

## Onde cada uma trava, por quê, e o que foi feito

| Profissão | Onde trava (contagem desde 30-09) | Por quê | Solução | Estado |
|---|---|---|---|---|
| **Fundidor** | 6.391 × `none of 12 colony chests had sand` | Não há areia no alcance; a busca dizia "não há", o castigo dobrava e nada mais acontecia | Regra nova: na **3ª busca vazia** o material aparece no baú de quem o usa (`LocateFallback`); castigo entre buscas de 5/10 min para 1/2 min; a frase repetida sai uma vez e a cada 100 (`StopLog`) | ✅ feito, testado |
| **Pastor** | 0% trabalho por 30 min, nenhuma linha | Sem ovelha com lã no raio, `findSheep` voltava calado e a tarefa nunca era solta | Um ciclo sem ovelha = busca vazia: solta a tarefa (castigo), e na 3ª a lã aparece (`EmptyFlock`); a lã entrou no castigo de reserva | ✅ feito, testado |
| **Carpinteiro** | 603 × `stopped because half of the colony's wood stays in logs`, 12 com 0 peça só nesta sessão | A meta contava a tora de **todos** os baús da vila; o carpinteiro, só a dos baús de trabalhador | O portão lê os mesmos baús da meta (`CraftingSteps.halfTheWoodMayStillBeConverted`) | ✅ feito, testado |
| **Construtor** | 6 × `has not moved a block` (300 tiques), dezenas de `sets … aside` | O lugar de pé escolhido ficava dentro da casa fechada | `UnreachableSpots`: o lugar que o Vanilla marca como inalcançável é trocado em 1 s | ✅ feito em 51b7accc |
| **Construtor** | 33 × `no ITEM in the colony chests` | Uma peça sem material para a obra inteira | Material natural sem rota agora aparece após 3 tentativas; **pendente**: adiar só a peça que falta e seguir com as outras | 🟠 parcial |
| **Construtor** | 19 × `next piece has no physical support` | Peça de telhado/parede sem apoio ainda | Já adia e reitera; observar | 🟢 observar |
| **Minerador** | 116 × `found no detour`, 109 × `gave up the stone … not moved`, 57 × `got no closer` | Encalhe no fundo da mina, desvio impossível | Já existe ClimbOut/MineReturn; **pendente**: medir com `/vc log` qual degrau trava | 🟠 pendente |
| **Ex-mineiros** | 52 × `gave up COLLECT_STONE once too often and left the trade` | As desistências acima contam como falha do trabalhador | **Pendente**: não punir quando a causa é a mina (encalhe), só a obstinação do aldeão | 🟠 pendente |
| **Fazendeiro** | 134 × `still sweeping — the budget ran out` | A varredura da roça não fecha no prazo de 15 ms | Pendente (F13 / §3.3): continuar a varredura entre ciclos sem recomeçar | 🟡 pendente |
| **Fazendeiro** | 57 × `could not reach the crop` | Plantio cercado ou atrás de água | Observar com o rastro | 🟡 observar |
| **Lenhador** | 32 × `has not moved … on the tree` | Árvore alta/em encosta | Já castiga a árvore; ok | 🟢 ok |
| **Pedreiro** | 67% ocioso, `no task open` | Só trabalha quando a obra pede peça de pedra | **Decisão do autor**: pré-fabricar peças de pedra da próxima casa, ou ajudar na obra | 🟠 decisão |
| **Sem ofício** | ~5 aldeões, 100% ocioso; `hiring … at target x80` | Cota da ADR-011: a cada 15 adultos, só 8 vagas (uma por profissão) | **Decisão do autor** (muda ADR-011): (a) vagas = adultos, rodízio pela ordem de crescimento; ou (b) ociosos viram ajudantes (recolher do chão, carregar para a obra) | 🔴 decisão |
| **Encalhados** | 80 × `frozen twice`, 24 × `fenced in` | Buracos e cercas | ClimbOut e PenEscape funcionam (verdict); ok | 🟢 ok |

## A vila: cálculo, varredura, baús, profissões

- **O que é a vila:** a caixa medida pelas camas (Emenda 6/7), com o centro
  fixado na colônia. O log mostra `saw 6–21 beds … keeping 24 — view not
  provably complete`: a contagem de camas oscila com os chunks carregados e a
  colônia guarda o maior valor provado. Correto; o raio de coleta cresce com
  essas camas (N11).
- **Varredura:** espiral por anéis (`RingSweep`), 15 ms por passagem, retomada
  de onde parou. As buscas vazias agora levam ao material no baú na 3ª vez,
  e não a castigos crescentes sem fim.
- **Baús:** todos os baús dentro da caixa entram — de trabalhador, da boca da
  mina e os sem dono, **inclusive os do jogador** (desde 09-16). Baú com nome
  e baú fora da caixa ficam de fora. O material pedido pode estar em qualquer
  um deles. Corrigido hoje: o carpinteiro não enxergava os baús sem dono.
- **Profissões:** contratação pela ordem de crescimento e pela demanda da
  obra; a cota da ADR-011 deixa ociosos (ver tabela).

## Arte em pixel

As 14 texturas de `textures/gui/overlays/` (c40d4a1f) **nunca eram
desenhadas**: os overlays escreviam só texto, e saíam calados quando os
buffers do jogo vinham nulos — o cliente do autor roda shaders do Iris.
Agora: ícone de profissão sobre o aldeão (32 blocos) e ícone de estado sobre
a obra (64 blocos), desenhados no `WorldRenderEvents.LAST` com buffer
próprio. Teste de unidade confere que cada ícone pedido existe. **Precisa ser
visto em jogo**, com e sem shaders.
