---
name: teste-vazio-por-condicao-impossivel
description: Teste cuja condicao nunca pode ocorrer passa para sempre sem medir nada; so a mutacao revela
metadata:
  node_type: memory
  type: feedback
  modified: 2026-09-25T05:01:41.122Z
---

Escrevi **dois** testes vazios em dias seguidos, pelo mesmo mecanismo: a
condição que eles procuravam **não podia ocorrer**, então passavam sempre.

1. **Gametest do fundidor (09-18):** punha pedregulho num baú e pedia vidro.
   O fundidor saía em *"nenhum baú tinha areia"* sem tocar em nada. Passava
   **com a devolução de material apagada do código**.
2. **Teste de ambiguidade (09-18):** procurava duas chaves de *mesmo
   comprimento* em que *uma contivesse a outra*. Isso as torna idênticas, e
   um `Map` não admite chave repetida. A condição nunca ocorreria.

**Why:** um teste verde é indistinguível de um teste vazio pelo resultado.
Os dois casos passaram na primeira execução e pareciam prontos.

**How to apply:** **sempre provar por mutação** — quebrar de propósito o
código que o teste afirma proteger e confirmar que ele falha. É o único
sinal que separa "passou porque está certo" de "passou porque não mediu".
Antes disso, perguntar em voz alta: *qual entrada concreta faz este assert
falhar?* Se não souber responder com um exemplo, o teste é suspeito.

**A pista do segundo caso:** eu raciocinei sobre as *chaves entre si* quando
o defeito vive nos *nomes reais*. O empate não exige que uma chave contenha
a outra — `x_ab_cd` casa com `ab` e com `cd`, dois caracteres cada. Varrer
os dados reais achou `dirt_path` (empate entre `dirt` e `path`), que meu
raciocínio abstrato tinha deixado passar.

**Terceiro caso (25-09, achado pelo PIT, não escrito por mim):**
`ColonyCycleTest.theTaskIsCancelledWhenTheDeficitIsGone` checava
`availableFor` vazio, mas o `run` de 5 argumentos já **atribui** a tarefa no
mesmo ciclo (`hasStorage = w -> true`), e atribuída nunca está disponível.
Ao testar fila de tarefas no `core`, rode com `worker -> false` e afirme o
estado `AVAILABLE` antes do passo que interessa. Pelo mesmo motivo,
`Task.reprioritize` não faz nada em tarefa já atribuída.

**Quarto caso (25-09): teste de regressão que não reproduz.** Para o
`ConcurrentModificationException` do `ServerMemory.resetAll`, as duas primeiras
versões do teste passaram SEM a correção: (1) reinscrevia uma chave que já
existia — `LinkedHashMap.put` em chave existente não muda a estrutura; (2) a
inscrição vinha do ÚLTIMO item — o iterador já sabe que não há próximo e não
confere o mapa. Regra: teste de regressão tem de FALHAR antes da correção; rode
antes de corrigir, sempre.

Relacionado: [[gametest-copia-do-predicado]] — a outra forma de teste que
não mede; [[ordem-de-mapa-e-prioridade]] — o defeito que estes trancam.
