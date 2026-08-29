# Erros e degradação

Num mod, a pergunta não é só "como trato o erro?" — é **"o que o jogador perde se
isto falhar?"**. Um mod que crasha o servidor por um erro seu causa mais dano do
que a feature valia.

## A regra de ouro

**Nenhuma exceção sua escapa de dentro de um método Vanilla.**

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] não instalou a task — este aldeão fica vanilla", falha);
    }
}
```

Custo de degradar: um aldeão comum — que é exatamente o estado de antes da sua
mudança. Custo de propagar: possivelmente o tick do servidor.

Isso vale para Mixin, para listener de evento, e para qualquer callback que o
Vanilla chama.

## Degradar para Vanilla

O padrão que guia todo tratamento de erro em mod:

```text
se a minha integração falhar, o resultado deve ser o comportamento Vanilla
```

Não "estado pela metade", não "crash", não "silêncio com dados inconsistentes".
O jogo sem o seu mod já funcionava; esse é o piso.

## Classificar o erro

Tratamento diferente por tipo:

| Tipo | Exemplo | O que fazer |
|---|---|---|
| **Esperado** | chunk não carregado, alvo sumiu, baú quebrado | trate como caso normal — **não é erro** |
| **Recuperável** | POI ocupado, caminho não encontrado | desista desta vez, tente depois |
| **De configuração** | valor fora da faixa | corrija para o limite, avise uma vez |
| **De programação** | NPE, índice inválido | logue com stack, degrade; é bug seu |
| **De integração** | outro mod removeu algo | logue uma vez, degrade |
| **De versão** | alvo de mixin sumiu | falhe no boot (`defaultRequire: 1`) |

A primeira linha é a mais importante e a mais mal tratada: **chunk não carregado
não é erro.** É "não sei agora". Tratar como exceção enche o log e esconde
problemas reais.

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
if (chunk == null) {
    return Optional.empty();     // normal, não excepcional
}
```

## Nunca engula em silêncio

```java
// ✗ o bug fica invisível para sempre
try { ... } catch (Exception ignored) { }

// ✓ degrada E deixa rastro
try {
    ...
} catch (RuntimeException falha) {
    LOGGER.warn("[meumod] falha ao processar {} — pulando", pos, falha);
}
```

Exceção engolida é a pior escolha possível: o sintoma aparece longe da causa,
sem nenhum rastro que ligue os dois. Quando alguém finalmente investigar, não vai
haver nada no log.

## Não faça spam

O oposto também é ruim. Erro que acontece por tick, por entidade, enche o disco e
**muda o timing**, escondendo bugs de concorrência.

```text
[ ] erro esperado e frequente → nem logue, ou logue em debug
[ ] erro recuperável → uma vez por ocorrência real, não por tentativa
[ ] erro de programação → uma vez com stack, depois conte
[ ] NUNCA logue dentro de laço quente sem limite
```

Padrão de "avisar uma vez":

```java
if (avisado.add(id)) {
    LOGGER.warn("[meumod] ...");
}
```

## Log útil

```text
[MOD_ID] Contexto — operação falhou: motivo
```

```java
// ✗ não diz nada
LOGGER.error("erro");
LOGGER.info("entrei no método");

// ✓ diz o que precisa para agir
LOGGER.warn("[meumod] colheita em {} abortada: baú do trabalhador {} não existe mais", pos, uuid);
```

Log de **estado** (valores que explicam a decisão) vale muito mais que log de
**passagem** ("cheguei aqui"). Passagem prova que o código rodou, o que quase
nunca é a dúvida.

**Prefixo do mod** faz toda linha ser filtrável num log com trinta mods — que é o
que você vai receber do jogador.

## Falhar cedo onde deve

Nem tudo deve degradar. Algumas coisas precisam falhar alto:

```text
[ ] Mixin que não aplica    → defaultRequire: 1, falha no boot
[ ] registro inválido       → falha no boot
[ ] recurso obrigatório ausente → falha no boot
```

O critério: **falhe cedo quando continuar produziria estado inconsistente ou
corromperia save.** Degrade quando continuar produz um jogo Vanilla funcional.

## Validação de entrada

Duas fronteiras onde a entrada não é confiável:

**Packets do cliente** — um cliente modificado envia qualquer coisa. Valide tudo,
sempre. Ver `networking.md`.

**Config** — editada à mão por quem não leu a documentação. Corrija para o limite
e avise; **nunca crashe**. Ver `configuration.md`.

## Estados de falha em comportamento

Todo comportamento de entidade precisa de:

```text
[ ] TIMEOUT                    quanto tempo até desistir
[ ] condição de DESISTÊNCIA    o que torna isto impossível
[ ] o que fazer DEPOIS         limpar memória, liberar reserva, escolher outro
```

Sem isso, a entidade fica presa tentando o impossível para sempre — gastando CPU,
sem progresso, e parecendo quebrada.

**Retry infinito é bug, mesmo sem exceção nenhuma.**

## Limpar ao falhar

Falha que deixa reserva pendurada é pior que a falha:

```text
[ ] POI reivindicado foi liberado?
[ ] memória do Brain foi limpa?
[ ] o registro do mod ficou consistente?
[ ] o baú marcado foi desmarcado?
```

## Checklist

```text
[ ] nenhuma exceção escapa de método Vanilla
[ ] a falha degrada para comportamento Vanilla
[ ] nada é engolido em silêncio
[ ] nenhum log dentro de laço quente sem limite
[ ] logs com prefixo do mod e conteúdo acionável
[ ] chunk não carregado tratado como caso normal
[ ] packets do cliente validados
[ ] config inválida avisa, não crasha
[ ] todo comportamento tem timeout e desistência
[ ] falha limpa reservas pendentes
```
