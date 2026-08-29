# Checklist — entidade e IA

> Use ao analisar ou estender comportamento de entidade. Cobre as Fases 9 e 10 do
> `references/research-protocol.md`.

## Identificar o sistema

- [ ] **Descobri se o mob usa `Goal` ou `Brain`** — não assumi
      `javap -cp "$MC_JAR" <fqn> | grep -i "brain\|goal"`
- [ ] Se tem os dois, sei qual decide o quê

## Ciclo de vida

- [ ] Sei por quais caminhos a entidade é criada (spawn, ovo, conversão, comando, load)
- [ ] Sei o que é inicializado onde (construtor vs. initialize vs. primeiro tick)
- [ ] **Sei todas as formas de ela sair:** morte, despawn, chunk unload, **conversão**

> Zumbificação de aldeão passa por `MOB_CONVERSION`, **não** por `AFTER_DEATH`.
> Um mod que só escuta morte perde o caso mais comum.

- [ ] Sei que **ausência na varredura não é morte** — pode ser chunk descarregado

## Dados

- [ ] Para cada estado, escolhi o mecanismo certo:
      campo (tick) / DataTracker (cliente vê) / NBT (sobrevive ao save)
- [ ] Estado que precisa dos dois tem **os dois códigos**, não um

## Se for Brain

- [ ] Sei qual **memória** representa o estado necessário
- [ ] Sei qual **sensor** a preenche — e com que frequência
- [ ] Sei qual **activity** controla, e quem a escolhe (`Schedule`)
- [ ] Sei qual **task** executa
- [ ] Sei a condição de início e a de interrupção
- [ ] A memória está **registrada no perfil do Brain**
- [ ] Usei o **mapa de memórias exigidas** da task como gate, em vez de checar
      dentro de `shouldRun`

- [ ] **Não vou criar Activity nova sem verificar a Schedule** — Activity que a
      Schedule não conhece nunca é escolhida
- [ ] Se uso `setTaskList`, sei que ele **acrescenta** e não remove Vanilla
- [ ] Prioridade escolhida sabendo que é ordem **dentro** da Activity

## Movimento

- [ ] Sei que em mobs de Brain quem manda é a memória `WALK_TARGET`
- [ ] **Não estou usando `getNavigation().startMovingTo`** esperando que funcione
      — o cérebro reescreve o destino no mesmo tick
- [ ] Mantenho a memória enquanto o destino valer

## Se for Goal

- [ ] Sei a prioridade **e os controles** (`MOVEMENT`, `LOOK`, `JUMP`, `TARGET`)
- [ ] Sei com quais goals ele compete pelo mesmo controle

## Estados de falha

Cada comportamento trata:

- [ ] alvo sumiu
- [ ] caminho não encontrado
- [ ] recurso indisponível
- [ ] chunk descarregado
- [ ] entidade morreu / foi convertida
- [ ] jogador interrompeu
- [ ] mundo mudou entre a decisão e a ação
- [ ] servidor reiniciou

- [ ] Há **timeout** e condição de desistência
- [ ] **Não há retry infinito** num alvo impossível

## Performance

- [ ] Sei quantas entidades desse tipo existem num mundo real (não no teste com duas)
- [ ] Frequência de sensor definida — não é por tick sem motivo
- [ ] Frequência de pathfinding definida — recalcula só quando muda ou falha
- [ ] Raio de busca justificado (raio é cúbico: dobrar multiplica por 8)
- [ ] Leitura de bloco usa chunk carregado, não `world.getBlockState` direto

## Client / Server

- [ ] A lógica de IA roda só no servidor
- [ ] O que o cliente precisa ver está sincronizado

## Saída

- [ ] `templates/entity-analysis.md` e/ou `templates/ai-system-analysis.md`
- [ ] Afirmações etiquetadas com fonte
