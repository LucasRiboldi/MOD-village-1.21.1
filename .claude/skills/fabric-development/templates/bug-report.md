# Relato de bug — <título curto>

> Preencha **antes** de tocar em código. Escrever isto força a separar observação
> de interpretação — que é onde a depuração costuma sair do trilho.

**Data:** AAAA-MM-DD · **Status:** aberto / em investigação / corrigido

## Expected

<O que deveria acontecer.>

## Actual

<O que acontece. **Observação, não interpretação.** "O aldeão fica parado ao lado
do baú" — não "a memória não está sendo escrita".>

## Reprodução

> **Não corrija o que você não conseguiu reproduzir.** Sem reprodução, você não
> tem como saber se consertou.

1.
2.
3.

**Reproduz sempre / às vezes / uma vez só:**

## Ambiente

| | |
|---|---|
| Singleplayer / servidor dedicado | |
| Minecraft | |
| Fabric Loader / API | |
| Mappings | |
| Versão do mod | |
| Java | |
| Mundo novo ou save antigo | |

## Outros mods presentes

<Reproduz sem eles?>

## Logs

```text
<trecho relevante — a PRIMEIRA exceção, não a última>
```

```text
[ ] há aviso de Mixin no log de boot?
[ ] há crash report em run/crash-reports/?
```

## Sistema suspeito

<Qual, e por quê.>

Triagem rápida:

```text
[ ] esquece ao reabrir o mundo      → markDirty / par escrita-leitura
[ ] SP funciona, MP quebra          → autoridade ou sync
[ ] NoClassDefFoundError no server  → classe de cliente em código comum
[ ] trava ao carregar chunk         → getBlockState forçando carga
[ ] some ao trocar de save          → estado em static
[ ] some quando o jogador se afasta → chunk tratado como ausência
[ ] entidade parada                 → memória do Brain nunca escrita
[ ] some após atualizar o MC        → mapping / semântica
```

---

## Investigação

### O que o programa acha que sabe?

| Estado | Valor esperado | Valor real | Quem escreveu |
|---|---|---|---|

> Comece pelo **estado**, não pelo código. Muito bug "de lógica" é estado que
> nunca foi escrito.

### Minimização

| Removido | O bug sumiu? | Conclusão |
|---|---|---|

## Causa raiz

> Em termos de **mecanismo**, não de sintoma.
>
> ✗ "o registro estava errado"
> ✓ "`remove` nunca era chamado, porque a única fonte considerada não distingue
>    morte de chunk descarregado"

<…>

### Por quantos caminhos essa causa se manifesta?

> A pergunta que evita a correção parcial — a que passa nos testes e falha em
> jogo.

| Caminho | Coberto pela correção |
|---|---|

## Correção

<A menor mudança que resolve a causa.>

```text
[ ] resolve a CAUSA, não o sintoma
[ ] cobre TODOS os caminhos da causa
[ ] não mistura refatoração
[ ] não mistura feature nova
[ ] não muda comportamento além do bug
```

## Teste de regressão

<Teste que **falhava antes** da correção e passa depois — a melhor prova
disponível.>

```text
[ ] o caso reportado não acontece mais
[ ] teste automatizado escrito
[ ] o comportamento correto anterior continua
[ ] save antigo continua carregando
[ ] verificado RODANDO
```

## Conhecimento gerado

<Descobriu algo sobre o Vanilla, a Fabric ou o próprio mod? Registre — bug
corrigido sem causa registrada volta, e a próxima pessoa refaz a investigação
inteira.>

| Onde registrar | O quê |
|---|---|
| `docs/research/` | |
| ADR | |
