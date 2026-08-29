# Análise de mod — <NOME DO MOD>

> Uma por mod externo estudado. Guarde em `docs/research/mods/`.
> Objetivo: responder **uma pergunta**, não descrever o mod inteiro.

**Mod:** <nome> · **Versão:** <do mod>
**Minecraft:** <versão> · **Mappings:** <Yarn/Mojmap + versão> · **Loader:** <…>
**Fonte:** <caminho local ou URL> · **Licença:** <MIT / Apache-2.0 / GPL / …>
**Analisado em:** AAAA-MM-DD

## Pergunta

<A pergunta que este mod deve responder. Uma frase, escrita antes de ler o código.>

> Se a versão do mod for diferente da sua, ele responde "é possível?" mas não
> responde "como escrevo isso hoje".

## Objetivo do mod

<O que o mod se propõe a fazer, em uma ou duas frases.>

## Dependências

| Dependência | Versão | Obrigatória |
|---|---|---|

## Arquitetura

<Como os pacotes estão organizados: por tipo técnico ou por domínio? Onde mora o
estado?>

## Entry points

| Tipo | Classe |
|---|---|
| main / client / server / gametest | |

## Registries

| Registro | O que registra | Onde |
|---|---|---|

## Sistemas Vanilla modificados

| Sistema | Como | Degrau da escada |
|---|---|---|

## APIs Fabric usadas

> Obtido por: `grep -rn "net.fabricmc.fabric.api" src/main/java | sed 's/.*import //' | sort -u`

| API | Para quê |
|---|---|

## Eventos usados

| Evento | Para quê |
|---|---|

## Mixins

> O indicador mais direto de risco de compatibilidade.

| Alvo | Método | Tipo | Ponto | Risco |
|---|---|---|---|---|
| | | `@Inject` / `@Redirect` / `@Overwrite` / … | HEAD/TAIL/INVOKE | baixo/médio/alto |

**Total:** <n> mixins, sendo <n> de alto risco.

## Sistemas de cliente

<O que existe só no cliente. — não se aplica.>

## Sistemas de servidor

<O que é autoritativo.>

## Armazenamento de dados

| Dado | Mecanismo | Escopo |
|---|---|---|

## Networking

| Packet | Direção | Payload | Validação |
|---|---|---|---|

## Features principais

<Só as relevantes para a sua pergunta.>

## Fluxos de execução importantes

```text
TRIGGER → ENTRY → … → RESULTADO
```

> Detalhamento: `execution-flow.md`.

## Classes importantes

| Classe | Papel |
|---|---|

---

## Estratégia de extensão

**Degrau predominante:** <1–11>

<Como o mod escolheu se conectar ao Vanilla, e o que isso sugere sobre os
extension points disponíveis. Um mod que usa poucos Mixins provavelmente achou um
ponto que os outros não viram — vale investigar esse.>

## Estratégia de compatibilidade

<O que o mod faz para conviver — ou não faz.>

## Pontos fortes

## Pontos fracos

## Riscos

| Risco | Severidade |
|---|---|

## Conceitos reutilizáveis

> A **ideia**, não o código. Copiar código carrega a licença de origem.

## Conceitos a evitar

> Igualmente valioso: um mod que resolveu com `@Overwrite` ensina o que não fazer.

## Lições

<O que isto muda na sua decisão. Se não muda nada, diga — também é resultado.>

---

## Evidência

| Afirmação | Etiqueta | Fonte |
|---|---|---|
| | `[FATO]` / `[INFERÊNCIA]` | arquivo · classe · método |

## Aplicabilidade ao nosso projeto

**Versões compatíveis?** sim / não — <o que muda se não>
**Abordagem aplicável?** sim / parcial / não — <por quê>
