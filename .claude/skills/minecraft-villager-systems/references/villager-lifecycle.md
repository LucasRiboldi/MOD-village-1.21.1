# Ciclo de vida do aldeão

Saber por onde o aldeão entra e sai é o que separa um registro que funciona de um
que acumula fantasmas.

## Entrada

```text
spawn natural na vila gerada
reprodução (bebê)
ovo de spawn / comando
cura de aldeão zumbi        ← identidade NOVA
carregamento do save
```

> **Cura de zumbi devolve um aldeão com UUID novo.** Ele não é "o mesmo aldeão de
> volta". Se o seu mod guarda estado por UUID, esse estado não o encontra — e
> isso é correto, mas precisa estar documentado para ninguém "consertar" depois.

## As quatro saídas

```text
MORTE            dano
CONVERSÃO        zumbificação          ← NÃO passa por morte
DESPAWN          raro para aldeão
CHUNK UNLOAD     não é saída — é ausência
```

`[FATO]` Fabric API 1.21.1:

```java
ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> { ... });
ServerLivingEntityEvents.MOB_CONVERSION.register(...);
```

> **Zumbificação é o caso mais comum de perder um trabalhador em jogo**, e ela
> **não dispara `AFTER_DEATH`**. Um mod que só escuta morte corrige o caso raro e
> mantém o bug no caso frequente — e passa em todos os testes.

### Ausência não é morte

Aldeão fora do raio, ou em chunk descarregado, **não morreu**: só não foi visto.

```text
✗  varredura não achou → remover do registro
     → apaga trabalhadores vivos toda vez que o jogador se afasta

✓  só o EVENTO serve como prova de que ele se foi
```

Este é um dos erros mais caros do domínio, porque o sintoma ("a vaga não reabre"
ou "os trabalhadores somem") aparece longe da causa.

## Ordem ao remover

Quando o aldeão sai, a limpeza tem ordem:

```text
1. liberar o que depende da POSIÇÃO dele (marca de baú, reserva de recurso)
2. liberar POI reivindicado
3. esquecer o aldeão do registro
```

Invertido, o passo 1 falha: ele precisa ler dados que o passo 3 apagou.

E decida o que **não** limpar:

> O baú e o conteúdo ficam. Quem morreu era o dono, não o conteúdo. O que sai é a
> promessa de que aquele baú tem dono.

## Bebês

```text
bebê → cresce → adulto
```

```text
[ ] bebê usa Schedule própria (VILLAGER_BABY)
[ ] bebê não trabalha e não adquire profissão
[ ] sua task deve verificar se é adulto, quando fizer diferença
[ ] o crescimento é temporizado e persiste
```

Task que não checa idade faz bebê tentar trabalhar — visualmente estranho e
funcionalmente errado.

## Tick

```text
tick da entidade  →  tick do Brain  →  sensores (frequência própria)  →  tasks
```

O tick do Brain só roda no **servidor**. Toda lógica de IA é server-side.

## Chunk

```text
chunk carrega    → o aldeão volta a tickar
chunk descarrega → ele congela; o estado dele vai com o chunk
```

Um sistema de aldeões **hiberna com o chunk**, como a vila Vanilla. Isso não é
limitação a contornar: forçar carregamento de chunk para manter aldeões ativos é
o caminho mais rápido para destruir o TPS de um servidor.

Consequência de desenho: toda lógica precisa tolerar "o alvo está em chunk
descarregado" como caso **normal**, não como erro.

## Save e load

```text
Vanilla salva     VillagerData (tipo, profissão, nível, XP)
                  inventário · gossips · offers
                  memórias COM CODEC
                  idade, saúde, posição

Vanilla NÃO salva memórias sem codec
                  qualquer estado do SEU mod
```

Se o seu mod atribui um papel próprio ao aldeão, **ele precisa salvar isso** —
ver `references/villager-data.md`.

## Reabertura de mundo

```text
SERVER_STARTED   → limpar registro em memória → carregar do save
SERVER_STOPPING  → salvar → limpar registro
```

Limpar **nos dois lados**. O processo pode abrir outro save sem reiniciar, e
aldeões do mundo anterior vazam para o novo.

## Redescobrir vs. persistir

A decisão que define quanto do seu sistema é frágil:

| Dado | Persistir? | Por quê |
|---|---|---|
| profissão atribuída pelo mod | **sim** | não existe no mundo |
| id da colônia a que pertence | **sim** | não existe no mundo |
| posição do baú dele | **não** | existe no mundo, redescoberta |
| alvo atual de trabalho | **não** | intenção do momento |
| progresso de uma tarefa | depende | se é legível do mundo, não |

Persistir o que o mundo já sabe cria uma **segunda verdade que envelhece**: o
save diz que há um baú em X, o jogador quebrou, e o mod acredita numa coisa que
não existe.

**Registre a decisão de não persistir**, com o motivo. Sem isso, alguém
"conserta" depois.

## Checklist

```text
[ ] AFTER_DEATH registrado
[ ] MOB_CONVERSION registrado         ← o caso mais comum
[ ] ausência na varredura NÃO remove do registro
[ ] a ordem de limpeza está certa
[ ] POI reivindicado é liberado
[ ] cura de zumbi = identidade nova, documentado
[ ] bebês tratados (ou explicitamente ignorados)
[ ] chunk descarregado é caso normal, não erro
[ ] estado próprio do mod é salvo
[ ] registro global limpo no start E no stop
```
