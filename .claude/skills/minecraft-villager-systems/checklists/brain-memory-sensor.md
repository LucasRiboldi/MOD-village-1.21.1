# Checklist — Brain, memória e sensor

## Antes de criar

```text
[ ] verifiquei se existe memória Vanilla que serve
    javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i <conceito>
[ ] verifiquei se existe sensor Vanilla que serve
[ ] considerei reagir a EVENTO em vez de criar sensor
[ ] considerei consultar o PointOfInterestStorage em vez de varrer
```

> Reusar a Vanilla é melhor: o jogo já preenche, expira e persiste. Uma memória
> paralela para o mesmo conceito produz duas verdades que divergem.

## Memória

```text
[ ] tipo correto (GlobalPos onde é GlobalPos, não BlockPos)
[ ] quem ESCREVE está definido
[ ] quem LÊ está definido
[ ] tempo de vida definido
[ ] default definido — "vazia" é estado válido ou erro?
[ ] LIMPEZA definida para cada situação
[ ] codec: sim se precisa persistir, não se é intenção do momento
```

**Limpeza — a tabela que não pode ficar vazia:**

```text
[ ] tarefa concluída
[ ] alvo sumiu
[ ] mudou de profissão
[ ] morreu ou foi convertido
[ ] chunk descarregou
```

## Registro da memória

```text
[ ] Registries.MEMORY_MODULE_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
[ ] REGISTRADA NO PERFIL DO BRAIN do aldeão
```

> A última é a etapa que falta quando "registrei e não funciona".

## Sensor — a regra rígida

```text
[ ] o sensor NÃO executa ação de gameplay
[ ] NÃO quebra bloco, não move o aldeão, não muda inventário
[ ] só OBSERVA e ESCREVE memória
```

> Sensor roda com frequência própria, fora do controle da Activity e sem o gate de
> memórias. Ação ali acontece quando não deveria.

## Sensor — custo

```text
custo = área × frequência × nº de aldeões
```

```text
[ ] a frequência é a MENOR que resolve
[ ] a área é a MENOR que resolve (raio é cúbico: metade = 1/8)
[ ] leitura de bloco não força carga de chunk
[ ] chunk null tratado como "não sei agora"
[ ] medido com 50 aldeões
```

## Uso na task

```text
[ ] o gate de memórias está no CONSTRUTOR, não em shouldRun
[ ] o estado exigido é o certo (VALUE_PRESENT / VALUE_ABSENT / REGISTERED)
```

```java
super(Map.of(ModMemories.MEU_ALVO, MemoryModuleState.VALUE_PRESENT), MIN, MAX);
```

## Não use static

```text
[ ] nenhum static Map<UUID, ...> guardando estado de IA
```

> É o anti-padrão número um do domínio: não persiste, não expira, não limpa na
> morte, vaza entre saves e briga com o Brain. A memória resolve os cinco.

## Teste

```text
[ ] a memória é escrita quando deveria
[ ] NÃO é escrita quando não deveria
[ ] expira como planejado
[ ] é limpa em cada situação da tabela
[ ] a task que a exige roda
[ ] com codec: sobrevive a fechar e reabrir o mundo
[ ] sem codec: NÃO sobrevive — e isso é intencional
[ ] o sensor não pesa com 50 aldeões (medido)
```
