# Evidência e afirmações

Pesquisa técnica de Minecraft falha de um jeito específico: o agente lê metade de
uma classe, completa o resto com o que "costuma ser", e escreve tudo no mesmo tom
de certeza. O documento fica convincente e parcialmente falso — que é pior que
incompleto, porque ninguém sabe qual metade conferir.

A solução é barata: **etiquetar cada afirmação pelo que a sustenta.**

## As sete etiquetas

| Etiqueta | Significa | Teste |
|---|---|---|
| `[FATO]` | Você viu, no código/log/execução desta versão | "Posso apontar arquivo, classe e método?" |
| `[INFERÊNCIA]` | Conclusão a partir de evidência, não vista diretamente | "Qual fato sustenta isso?" |
| `[HIPÓTESE]` | Plausível, ainda não verificado | "O que provaria que estou errado?" |
| `[VALIDAÇÃO NECESSÁRIA]` | O próximo passo técnico concreto | "Qual comando/teste roda isso?" |
| `[DECISÃO]` | Escolha do projeto | "Quem decidiu, quando, por quê?" |
| `[RISCO]` | Problema possível | "Em que condição isso morde?" |
| `[VERSÃO]` | Escopo de validade | "Para qual MC/mappings isso vale?" |

Não misture numa frase só. Se você precisa de duas etiquetas, são duas frases.

### Exemplos

```text
[FATO] MultiTickTask expõe shouldRun(ServerWorld, E) e
shouldKeepRunning(ServerWorld, E, long) como protected.
Fonte: javap sobre minecraft-merged 1.21.1 / yarn 1.21.1+build.3.

[INFERÊNCIA] Uma task que devolve false em shouldKeepRunning é parada pelo Brain
no mesmo tick, porque tick() é final e consulta esse método.

[HIPÓTESE] Registrar a task em Activity.CORE faz ela rodar mesmo durante PANIC.
[VALIDAÇÃO NECESSÁRIA] Gametest com um zumbi próximo, observando se a task roda.

[RISCO] Outro mod que chame setTaskList na mesma Activity com a mesma prioridade
pode ter a ordem de execução trocada entre inicializações.
[VERSÃO] Vale para 1.21.1; a assinatura mudou em versões anteriores.
```

Repare no que a segunda linha faz: ela **admite** que é inferência, e com isso
diz ao próximo leitor exatamente onde olhar se o comportamento surpreender.

## Registro de fonte

Todo `[FATO]` carrega de onde veio.

**Tipo de fonte** — em ordem decrescente de força:

1. **Experimento / teste em runtime** — você rodou e observou.
2. **Código Vanilla** — decompilado desta versão.
3. **Código Fabric API** — sources jar do projeto.
4. **Código de mod** — funciona *para aquele mod*, não é regra geral.
5. **Documentação Fabric** — boa, mas pode estar atrás do código.
6. **Tutorial / post / memória** — **não é fonte.** É pista para verificar.

**Coordenadas:**

```text
PROJETO · ARQUIVO · CLASSE · MÉTODO · VERSÃO · OBSERVAÇÃO
```

### Nunca invente número de linha

Linha inventada é a pior falha possível aqui: parece o dado mais verificável do
documento e é o menos. Quem confere perde a confiança em tudo o mais.

Se você não tem a linha — e frequentemente não terá, porque leu via `javap`,
`unzip -p` ou busca — então `arquivo + classe + método` **é suficiente e é
honesto**. Escreva assim e siga.

O mesmo vale para assinaturas: se você não rodou `javap`, a assinatura é
`[HIPÓTESE]`, por mais que você "saiba" que é aquela.

## O teste do contraditório

Antes de escrever `[FATO]`, pergunte: **o que eu veria se isto fosse falso?**

Se a resposta for "não sei", não é fato — é inferência confortável. Muitas vezes
a checagem custa um comando:

```bash
javap -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -i brain
grep -P "\tinitBrain$" "$MAPPINGS"
```

Trinta segundos convertem uma hipótese num fato. Vale quase sempre.

## Grau de confiança

Quando a conclusão sustenta uma decisão de arquitetura, declare a confiança:

- **Alta** — fato direto, verificado nesta versão, com fonte primária.
- **Média** — inferência sólida a partir de fatos, sem contradição conhecida.
- **Baixa** — hipótese plausível; a decisão deveria ser reversível.

Decisão cara apoiada em confiança baixa é o sinal para gastar um experimento
antes (`experiment-methodology.md`).

## Quando a evidência falta

Diga isso. Textualmente. As três formas honestas:

```text
[HIPÓTESE] ... — não consegui verificar porque os sources não estão gerados.
[VALIDAÇÃO NECESSÁRIA] ./gradlew genSources e reler o método.

Não encontrei nenhuma API Fabric para isto. Procurei em <lista>. Pode existir e
eu não ter achado — a busca não é exaustiva.

Isto vale para 1.21.1. Não verifiquei outras versões e não afirmo nada sobre elas.
```

"Não sei" com o próximo passo anexado é uma contribuição. "Provavelmente é assim"
sem etiqueta é uma armadilha para a próxima sessão.
