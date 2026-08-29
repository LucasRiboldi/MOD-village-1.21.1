# Checklist — arquitetura

> Rode ao fechar o desenho, antes de escrever muito código. Corrigir arquitetura
> depois de vinte arquivos custa vinte arquivos.

## Responsabilidade

```text
[ ] cada classe tem uma responsabilidade que cabe numa frase
[ ] as NON RESPONSIBILITIES estão escritas (é o campo que impede a God Class)
[ ] não há classe que todo mundo importa
[ ] o entrypoint só chama; não implementa
```

## Tamanho certo

```text
[ ] é a MENOR arquitetura correta
[ ] nenhuma abstração com uma implementação só
[ ] nenhum Manager/Service/Factory criado por reflexo
[ ] toda abstração se justifica por repetição, variação, extensibilidade REAL,
    testabilidade ou separação de domínio — já presentes, não esperadas
[ ] nenhuma classe fazendo coisas demais
```

> God Class e Framework Prematuro são erros **simétricos**. Escolha pelo tamanho
> real do problema.

## Estado

```text
[ ] todo estado tem dono explícito
[ ] o dono é o certo para o ciclo de vida (entidade/bloco/mundo/item)
[ ] nenhum estado de mundo em static
[ ] se há acesso global, é limpo no start E no stop, e a decisão está documentada
[ ] a mesma informação não existe em dois lugares podendo divergir
```

## Dependências

```text
[ ] dependências entre sistemas são explícitas
[ ] nenhuma feature depende de detalhe interno de outra
[ ] a direção das dependências faz sentido (domínio não depende de integração)
```

## Ciclo de vida

```text
[ ] cada código está no ponto certo do ciclo de vida
[ ] registro no entrypoint
[ ] nada que precise de World/MinecraftServer no entrypoint
[ ] as três linhas do tempo não estão confundidas (jogo / entidade / block entity)
```

## Client / Server

```text
[ ] a fronteira foi desenhada, não improvisada com world.isClient() espalhado
[ ] métodos que mudam o mundo exigem ServerWorld na assinatura
[ ] nenhuma classe de cliente em código comum
[ ] quem tem autoridade está definido para cada estado
```

## Integração com o Vanilla

```text
[ ] a integração é a MÍNIMA que resolve
[ ] o degrau da escada de extensão está nomeado
[ ] se subiu a escada, a justificativa está escrita
[ ] Mixins são poucos, rasos e delegam
[ ] nada do Vanilla é removido sem justificativa
```

> **Mais de dois ou três Mixins para uma feature** indica arquitetura lutando
> contra o Vanilla em vez de se encaixar nele.

## Persistência

```text
[ ] o mecanismo de cada estado bate com o que precisa sobreviver
[ ] dados relacionados por id vão no mesmo arquivo
[ ] o que é redescobrível NÃO é persistido, e isso está escrito
```

## Invariantes

```text
[ ] as invariantes do sistema estão escritas
[ ] cada uma é protegida no código, não só documentada
[ ] há comportamento definido para quando são violadas
```

## Falha

```text
[ ] cada operação tem comportamento definido para falha
[ ] a falha degrada para comportamento Vanilla
[ ] há timeout e condição de desistência
[ ] falha limpa reservas pendentes
```

## Performance

```text
[ ] frequência × população foi calculada, com números
[ ] nenhuma operação cara em caminho quente sem justificativa
```

## Compatibilidade

```text
[ ] o degrau escolhido é compatível com o risco aceitável
[ ] nada exclusivo (@Redirect/@Overwrite) sem justificativa
[ ] a lógica não depende de ordem entre mods
```

## Testabilidade

```text
[ ] dá para testar a regra de domínio sem subir o Minecraft?
[ ] se não, isso é aceitável para o tamanho deste sistema?
```

## Sinais de alerta

```text
[ ] uma mudança simples toca cinco arquivos não relacionados
[ ] ninguém sabe dizer quem é dono de um estado
[ ] a mesma informação existe em dois lugares
[ ] não dá para testar nada sem subir o jogo
[ ] há abstração criada "para o futuro"
```

> Três ou mais juntos é dívida que vai cobrar juros na próxima feature.
