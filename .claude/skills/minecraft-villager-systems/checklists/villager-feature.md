# Checklist — feature de aldeão pronta

> Item marcado exige **verificação**, não intenção.

## Escopo

```text
[ ] a pergunta "identidade ou capacidade?" foi respondida
[ ] escolhi a MENOR camada que resolve
[ ] o escopo de estado está declarado (individual / POI / vila / mundo)
[ ] non goals escritos
```

## Pesquisa

```text
[ ] o comportamento Vanilla envolvido foi compreendido
[ ] nenhuma hipótese não validada sustenta o desenho
[ ] as assinaturas foram verificadas com javap NESTA versão
```

## Arquitetura

```text
[ ] sensor percebe, task age — sem inversão
[ ] memória guarda estado de IA (NÃO static Map)
[ ] memórias Vanilla reusadas quando servem
[ ] GlobalPos usado onde é GlobalPos (JOB_SITE, HOME, MEETING_POINT)
[ ] o gate de memórias da task está no construtor
[ ] Activity nova evitada — ou justificada e a Schedule a conhece
[ ] WALK_TARGET usado, e MANTIDO
```

## Integração com o Vanilla

```text
[ ] setTaskList ACRESCENTA — nada Vanilla foi removido
[ ] no máximo UM Mixin em VillagerEntity (initBrain), delegando
[ ] a task perde para PANIC, PRE_RAID e RAID
[ ] a Schedule Vanilla não foi alterada (ou está justificado)
[ ] exceção capturada — degrada para aldeão Vanilla
```

## Falha

```text
[ ] alvo sumiu · caminho falhou · recurso indisponível
[ ] chunk descarregado tratado como caso NORMAL
[ ] TIMEOUT definido
[ ] limite de tentativas — sem retry infinito
[ ] o que fazer ao desistir está definido
[ ] falha libera reservas pendentes
```

## Ciclo de vida

```text
[ ] AFTER_DEATH registrado
[ ] MOB_CONVERSION registrado          ← o caso mais comum
[ ] ausência na varredura NÃO remove do registro
[ ] a ordem de limpeza está certa
[ ] bebês tratados (ou explicitamente ignorados)
```

## Persistência

```text
[ ] estado do mod é salvo (o Vanilla não salva por você)
[ ] memórias com codec onde precisa persistir
[ ] o que é redescobrível NÃO é persistido — decisão ESCRITA
[ ] nenhum static com estado de mundo
[ ] registro global limpo no start E no stop
```

## Vários aldeões

```text
[ ] estado compartilhado mora na vila, não no aldeão
[ ] reservas têm dono e validade
[ ] a vaga reabre quando um trabalhador se vai
[ ] falha em um não interrompe os outros
```

## Performance

```text
[ ] nenhuma leitura de bloco força carga de chunk
[ ] frequências justificadas e escalonadas entre aldeões
[ ] raios são os menores que resolvem
[ ] pathfinding não recalcula por tick
[ ] POI consultado em vez de varredura, quando aplicável
```

## Resources

```text
[ ] lang para tudo que foi registrado
[ ] modelo, textura, blockstate, loot table (blocos)
[ ] som (profissão)
[ ] textura de roupa (profissão)
```

## Verificação executada

```text
[ ] ./gradlew build
[ ] ./gradlew runGametest
[ ] ./gradlew runClient
[ ] ./gradlew runServer
[ ] a feature funciona em jogo
[ ] o CICLO DO DIA Vanilla continua (acorda · sino · trabalha · socializa · dorme)
[ ] cede em PANIC e RAID
[ ] fechar e reabrir o mundo
[ ] 1, 2, 10, 50 aldeões
```

> O item do ciclo do dia é o que separa "funciona" de "funciona sem quebrar o
> Vanilla" — e é o mais pulado.

## Entrega

```text
[ ] o plano preenchido está guardado
[ ] descobertas sobre o Vanilla voltaram para docs/research/
[ ] o relato separa "verificado rodando" de "tem teste escrito"
[ ] o que ficou de fora está DECLARADO, com o motivo
```
