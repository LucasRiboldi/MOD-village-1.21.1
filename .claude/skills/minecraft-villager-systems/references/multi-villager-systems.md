# Vários aldeões

Um aldeão é um problema de IA. Vários aldeões disputando os mesmos recursos é um
problema de **coordenação** — e coordenação não se resolve com mais IA.

## O que muda com N

```text
1 aldeão   → funciona
2 aldeões  → aparecem as disputas
10         → aparecem os custos
50+        → aparece o lag
```

Um sistema testado com dois aldeões não foi testado.

## Onde o estado compartilhado mora

```text
✗  cada aldeão guarda o que acha que é dele
✓  a VILA guarda o que é de todos
```

| Estado | Escopo |
|---|---|
| o que este aldeão está fazendo | individual (memória) |
| qual árvore está reservada | **vila** |
| quantos trabalhadores de cada função | **vila** |
| qual baú tem dono | **vila** |
| até onde a mina já chegou | **vila** |

Estado compartilhado guardado por aldeão diverge assim que dois aldeões escrevem.

## As quatro perguntas

```text
AUTORIDADE     quem decide? (sempre o servidor; dentro dele, quem?)
PROPRIEDADE    de quem é este recurso agora?
SINCRONIZAÇÃO  quando o estado compartilhado é lido/escrito?
CONFLITO       o que acontece quando dois querem o mesmo?
```

**Nunca permita que dois aldeões modifiquem estado compartilhado sem uma regra
clara.** No Minecraft o tick é sequencial, então não há corrida de threads — mas
há corrida **lógica**: o aldeão A decide no tick 1 com base num estado que o
aldeão B muda no tick 2.

## Reserva — o padrão que resolve a maioria

```text
DESCOBRIR → RESERVAR → USAR → LIBERAR
```

```text
[ ] a reserva tem DONO (UUID do aldeão)
[ ] a reserva tem VALIDADE (expira)
[ ] a reserva é liberada ao concluir
[ ] a reserva é liberada na MORTE ou CONVERSÃO do dono
[ ] a reserva é liberada se o alvo sumir
[ ] a reserva é liberada no restart, ou persiste junto com o dono
```

Sem expiração, uma reserva órfã bloqueia o recurso para sempre — e o sintoma é
"os aldeões pararam de trabalhar", horas depois, sem causa aparente.

`[FATO]` O próprio Vanilla usa esse padrão: `ticketCount` no
`PointOfInterestType` é exatamente uma reserva com contagem.

## Distribuição de função

```text
[ ] há limite por função?
[ ] quem atribui — o aldeão escolhe, ou a vila decide?
[ ] o que acontece quando um morre? a vaga reabre?
[ ] um novo aldeão adulto ganha função automaticamente?
```

O segundo item é uma decisão de design real:

| Modelo | Consequência |
|---|---|
| **aldeão escolhe** (Vanilla) | emergente, imprevisível, robusto |
| **vila decide** | controlável, precisa de autoridade central e persistência |

O modelo Vanilla usa POI livre: quem chega primeiro reivindica. Se você quer
controle (dois lenhadores e um minerador), precisa de um coordenador — e ele
precisa persistir.

## A vaga que nunca reabre

O bug clássico de sistema com vagas:

```text
aldeão morre → o registro não sabe → a contagem continua cheia →
nenhum novo é atribuído → a colônia para
```

A causa é sempre a mesma: **não há fonte de verdade sobre a saída.**

```text
[ ] AFTER_DEATH registrado
[ ] MOB_CONVERSION registrado    ← o caso mais comum
[ ] ausência na varredura NÃO conta como saída
```

Ver `references/villager-lifecycle.md`.

## Escalonar o trabalho

Se N aldeões precisam de trabalho periódico, **não faça todos no mesmo tick**:

```java
if ((villager.getId() + world.getTime()) % INTERVALO != 0) return;
```

Custo total igual; pico por tick dividido por N. **Picos são o que o jogador
sente** — o TPS médio pode estar ótimo e o jogo travando a cada segundo.

## Ordem determinística

Se a ordem de processamento importa (quem reivindica primeiro), ela precisa ser
**estável**. Iterar um `HashSet` dá ordem diferente a cada execução, e o
comportamento vira loteria — difícil de reproduzir e de depurar.

## Falha em cascata

```text
[ ] um aldeão travado bloqueia os outros?
[ ] uma reserva órfã trava o recurso para sempre?
[ ] um erro num aldeão interrompe o ciclo dos demais?
```

O terceiro pede tratamento por aldeão: uma exceção ao processar um não pode
abortar o laço de todos.

```java
for (Worker w : trabalhadores) {
    try {
        processar(w);
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] falha ao processar {} — seguindo", w.id(), falha);
    }
}
```

## Testar com escala

```text
[ ] 1 aldeão   — funciona?
[ ] 2 aldeões  — disputam corretamente?
[ ] 10         — a distribuição faz sentido?
[ ] 50         — o TPS aguenta?
[ ] 100        — ainda aguenta?
```

Os testes de 2 e de 50 são os que mais revelam. O de 2 pega conflito; o de 50
pega custo.

## Checklist

```text
[ ] estado compartilhado mora na vila, não no aldeão
[ ] autoridade e propriedade definidas
[ ] toda reserva tem dono e validade
[ ] reserva liberada em conclusão, morte, conversão e sumiço do alvo
[ ] vaga reabre quando um trabalhador se vai
[ ] AFTER_DEATH e MOB_CONVERSION registrados
[ ] trabalho periódico escalonado
[ ] ordem de processamento determinística
[ ] falha em um aldeão não interrompe os outros
[ ] testado com 1, 2, 10, 50
```
