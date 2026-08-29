# Checklist — compatibilidade

> Rode antes de fechar qualquer entrega. Compatibilidade é consequência do degrau
> da escada que você escolheu — não algo que se conserta depois.

## Mixins

```bash
grep -rn "@Redirect\|@Overwrite\|@ModifyVariable\|@ModifyConstant\|@ModifyArg" src/main/java
```

```text
[ ] listei todos os Mixins e seus tipos
[ ] nenhum @Redirect ou @Overwrite — ou há justificativa escrita
[ ] considerei @WrapOperation (encadeável) no lugar de @Redirect
[ ] nenhum assume índice de lista
[ ] nenhum remove comportamento Vanilla sem justificativa
[ ] sei quais mods conhecidos miram as mesmas classes
```

> `@Redirect` e `@Overwrite` são **exclusivos**: o segundo mod que mirar o mesmo
> alvo não roda. Sem aviso que o jogador entenda.

## Registro

```text
[ ] namespace próprio, nunca minecraft:
[ ] registro INCONDICIONAL
[ ] ordem DETERMINÍSTICA
[ ] registro na janela certa (entrypoint, antes do freeze)
```

## Eventos

```text
[ ] a lógica NÃO depende de ordem entre listeners de mods diferentes
[ ] se depende, está registrado como risco
[ ] o resultado é o mesmo em qualquer ordem (idempotente)
[ ] não assumo estado que outro mod pode ter mudado
```

## Dados

```text
[ ] tags usam "replace": false
[ ] nenhuma loot table ou recipe Vanilla é substituída
    — ou a substituição está declarada como risco
```

> Tags são **aditivas**. Loot tables e recipes são **substituídas por id**: dois
> mods editando a mesma, um perde silenciosamente.

## Estado

```text
[ ] nenhum estado de mundo em static
[ ] se há acesso global, é limpo no start E no stop
[ ] o estado não vaza entre saves
```

## Client / Server

```text
[ ] environment no fabric.mod.json bate com o código
[ ] nenhuma classe de cliente em código comum
[ ] testado em runServer
[ ] funciona se o cliente não tiver o mod — ou a dependência está declarada
```

## Versão

```text
[ ] assinaturas verificadas NESTA versão com javap
[ ] nenhum código copiado de outra versão sem conferir mapping e semântica
[ ] formato de dados tem número de versão gravado
```

## Degradação

```text
[ ] se a minha integração falhar, o resultado é COMPORTAMENTO VANILLA
[ ] nenhuma exceção minha escapa de dentro de método Vanilla
[ ] falha limpa reservas pendentes
```

> É o padrão que mais rende: o jogo sem o seu mod já funcionava — esse é o piso.

## Vizinhos prováveis

Se o alvo for aldeão / IA / POI:

```text
[ ] mods de aldeão (VillagerConfig, Easy Villagers, Guard Villagers)
    — disputam VillagerEntity, Brain, POI, profissões, trades
[ ] mods de performance (Lithium, Sodium e afins)
    — reescrevem caminhos quentes, inclusive IA e POI
[ ] mods grandes (Create) — muitos touchpoints
```

> Não é lista de inimigos: é onde olhar primeiro.

## Classificação

```text
[ ] classifiquei LOW / MEDIUM / HIGH
[ ] se HIGH: justificativa escrita E plano de degradação
```

| Nível | Significa |
|---|---|
| **LOW** | mecanismo previsto, nada exclusivo, sem premissa de ordem, degrada para Vanilla |
| **MEDIUM** | `@Inject` em classe popular, dependência de ordem, ou substituição de dado Vanilla |
| **HIGH** | `@Redirect`/`@Overwrite`, alteração central, premissa sobre estado que outro mod pode mudar |

> Risco **sem** classificação é decorativo. **Com** classificação, é decisão.

## Documentação

```text
[ ] dependências reais declaradas no fabric.mod.json
[ ] conflito conhecido DOCUMENTADO, não escondido
```

```text
Incompatível com <mod X>: ambos injetam em <classe>.
Sintoma: <o que acontece>. Contorno: <se houver>.
```

> Conflito documentado é suporte. Não documentado é um relatório de bug confuso
> que custa mais tempo do que teria custado escrever a linha.

## Teste

```text
[ ] testado com pelo menos um mod do mesmo domínio, se houver
[ ] cliente conecta a servidor dedicado
```
