# Análise de classe Vanilla — <NomeDaClasse>

> Uma por classe importante do sistema. Guarde em `docs/research/vanilla/`.
> Para o sistema inteiro, use `system-analysis.md`.

**Classe:** `net.minecraft.<pacote>.<Nome>`
**Mapping:** Yarn <versão> · **Minecraft:** <versão>
**Verificado com:** `javap -cp "$MC_JAR" <fqn>` em AAAA-MM-DD

## Responsabilidade

<Uma ou duas frases. O que esta classe existe para fazer — não o que ela tem.>

## Hierarquia

```text
extends    <superclasse>
implements <interfaces>
```

<O que a herança já entrega, e o que ela obriga.>

## Ciclo de vida

<Quem cria, quando, quantas vezes; quando é destruída.>

## Campos importantes

| Campo | Tipo | Mutável | Quem escreve | Significado |
|---|---|---|---|---|

> Campo `final` é identidade; campo mutável é estado, e você quer saber quem
> escreve nele.

## Métodos importantes

| Método | Visibilidade | Assinatura (verificada) | O que faz |
|---|---|---|---|

> Cole a assinatura do `javap`, não a lembrada.

## Estado controlado

<O que esta classe guarda, e o que acontece com isso ao salvar/carregar.>

## Dados

<O que ela lê de fora — registros, tags, JSON, memórias.>

## Dependências

<De que ela precisa para funcionar.>

## Callers

> Quem chama importa mais que a classe. Um método chamado por dois caminhos
> diferentes precisa que sua mudança valha nos dois.

| Chamador | Método | Contexto | Observação |
|---|---|---|---|

Como foram encontrados:

```bash
grep -rn "\.<metodo>(" /tmp/mcsrc/net/minecraft | head
```

## Side effects

<O que muda no mundo além do retorno: memória escrita, evento disparado, bloco
alterado, packet enviado.>

## Eventos

<O que ela emite, e o que a Fabric expõe em cima disso.>

## Persistência

<O que desta classe sobrevive ao save, e por qual mecanismo.>

## Extension points

| Ponto | Tipo | Viável? |
|---|---|---|
| | registro / data-driven / protected / interface / API Fabric | |

## Riscos de modificação

| Risco | Por quê |
|---|---|
| | |

Sinais a verificar:

- [ ] `static` mutável (estado global)
- [ ] chamado no tick de todas as entidades
- [ ] carrega chunk (`getBlockState`/`getBlockEntity` em posição arbitrária)
- [ ] assinatura com `RegistryWrapper.WrapperLookup` (1.20.5+)
- [ ] lambda/classe anônima no método alvo (Mixin não pega o corpo)
- [ ] sobrecargas com mesmo nome (Mixin precisa de descriptor)

## Evidência

| Afirmação | Etiqueta | Como verifiquei |
|---|---|---|
| | `[FATO]` / `[HIPÓTESE]` | `javap` / sources / `mappings.tiny` / experimento |
