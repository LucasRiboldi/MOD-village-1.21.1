# Coleta de recursos

O caso de uso mais pedido em mods de aldeão, e o mais fácil de desenhar errado.

## Três sistemas, não um

```text
DESCOBERTA   onde há recurso
EXTRAÇÃO     tirar o recurso do mundo
ARMAZENAMENTO onde guardar
```

Misturá-los produz uma task gigante que faz tudo e não é testável. Separá-los
permite trocar cada parte independentemente — e é o que torna "o mesmo lenhador
colhe outra árvore" uma mudança de uma linha.

## O ciclo completo

```text
Villager
  ↓ Profession        identidade
  ↓ Job Site / POI    a base
  ↓ Schedule          quando
TARGET SEARCH         onde há recurso        ← DESCOBERTA
  ↓ Memory            guarda o alvo
PATHFINDING           ir até lá
  ↓
GATHER                colher                 ← EXTRAÇÃO
  ↓ Inventory
RETURN
  ↓
DEPOSIT               guardar                ← ARMAZENAMENTO
  ↓
IDLE / próximo ciclo
```

## Máquina de estados

Modele explicitamente. Estados implícitos produzem aldeões travados.

```text
IDLE → SEARCHING → TRAVELING → WORKING → RETURNING → DEPOSITING → IDLE
```

Cada transição precisa de:

```text
CONDIÇÃO   o que a dispara
AÇÃO       o que acontece
FALHA      o que dá errado
TIMEOUT    quanto tempo até desistir
```

**Estados impossíveis devem ser inalcançáveis por construção** — viajando sem
destino, depositando sem carga. Não confie em convenção.

## Descoberta — o gargalo de performance

```text
✗  varrer 64³ blocos por aldeão, por ciclo
```

Alternativas, em ordem de retorno:

1. **Índice mantido por evento** — bloco colocado/quebrado atualiza o índice
2. **Cache com validade explícita**
3. **Varredura incremental** — um pedaço por ciclo, guardando o cursor
4. **`PointOfInterestStorage`** — se o alvo pode ser um POI
5. Varredura completa, só se nada acima servir

A terceira é subestimada: dezessete passagens espalhadas não aparecem no tick;
feitas de uma vez, aparecem. E o cursor precisa **persistir**, senão cada sessão
recomeça do zero e as sessões curtas nunca terminam uma volta.

**Raio é cúbico.** Metade do raio é 1/8 do custo — a otimização de maior retorno.

## Reserva entre aldeões

Dois aldeões indo à mesma árvore é desperdício; dois minerando o mesmo bloco é
bug.

```text
[ ] quem reserva o alvo?
[ ] onde a reserva vive?         ← estado por VILA, não por aldeão
[ ] quando expira?
[ ] o que acontece se o dono morrer?
[ ] o que acontece se o alvo sumir?
```

Ver `references/multi-villager-systems.md`.

## Extração — a regra estreita

**Bloco quebrado por engano é dano no save do jogador.** É irrecuperável e custa
a confiança.

Regras estreitas e explícitas valem mais que genéricas:

```text
✗  "árvores num raio de 32"
✓  "só tronco e folha de espécie conhecida, em grupo com copa viva,
    ligados ao que foi encontrado, e nunca tudo num raio"
```

A segunda é auditável e recusável; a primeira derruba a casa da vila.

```text
[ ] o mod pode quebrar bloco que o JOGADOR colocou?     ← quase sempre NÃO
[ ] o mod pode quebrar bloco da vila gerada?             ← quase sempre NÃO
[ ] quais são as exceções, e por quê?
[ ] o que ele repõe? (muda, replantio)
```

## Armazenamento

```text
[ ] onde ele guarda?
[ ] a posição é reservada a ele?
[ ] o que acontece se o baú for quebrado?
[ ] o que acontece se estiver cheio?
[ ] o mod mexe no conteúdo que o JOGADOR pôs lá?
```

A posição do baú **existe no mundo**: redescubra, não persista. E se o dono
morrer, o baú e o conteúdo ficam — o que sai é a promessa de que ele tem dono.

## Bioma e disponibilidade

Quando a profissão depende do bioma:

```text
✗  if (bioma == X) espalhado por dez lugares
✓  uma tabela de domínio: bioma → recursos disponíveis
```

Centralizar não é sobre elegância: é sobre acrescentar um bioma novo em um lugar
em vez de dez, sem esquecer nenhum.

Crie a abstração **quando houver variação real**, não antes.

## Falhas — todas precisam de resposta

```text
alvo sumiu                → esquecer, procurar outro
alvo inalcançável         → desistir após N tentativas
inventário cheio          → depositar antes de continuar
baú cheio                 → procurar outro, ou parar
baú quebrado              → soltar a reserva
chunk descarregou         → pular o ciclo (caso NORMAL)
anoiteceu                 → deixar a Schedule assumir
inimigo perto             → deixar PANIC assumir
aldeão morreu             → liberar reservas
servidor reiniciou        → o ciclo recomeça, sem estado inconsistente
```

As duas de horário e ameaça são as mais esquecidas — e produzem o aldeão que
minera de madrugada durante um raid.

## Performance

```text
[ ] quantos aldeões coletando ao mesmo tempo?
[ ] com que frequência cada um procura?
[ ] qual o raio?
[ ] a busca é escalonada entre aldeões?
[ ] o pathfinding recalcula só quando precisa?
```

Teste com **1, 10, 50, 100**. Ver `references/villager-performance.md`.

## Checklist

```text
[ ] descoberta, extração e armazenamento estão separados
[ ] a máquina de estados é explícita
[ ] a busca não varre por tick
[ ] há reserva entre aldeões, com expiração
[ ] as regras de quebra são estreitas e explícitas
[ ] o mod não danifica construção do jogador nem da vila
[ ] o que é redescobrível não é persistido
[ ] todos os casos de falha têm resposta
[ ] cede lugar a Schedule e PANIC
[ ] testado com vários aldeões
```
