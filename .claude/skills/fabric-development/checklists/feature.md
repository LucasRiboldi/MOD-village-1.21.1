# Checklist — feature pronta

> Rode antes de dizer que acabou. Item marcado exige **verificação**, não
> intenção.

## Entendimento

```text
[ ] o objetivo cabe numa frase
[ ] o escopo e os non goals estão escritos
[ ] a pesquisa existente foi consultada, não repetida
[ ] nenhuma hipótese não validada sustenta o resultado
```

## Arquitetura

```text
[ ] é a menor arquitetura correta
[ ] nenhuma abstração criada "para o futuro"
[ ] todo estado tem dono explícito
[ ] a feature não depende de detalhe interno de outra
[ ] o entrypoint só chama, não implementa
```

## Implementação

```text
[ ] cada peça foi integrada com build entre elas
[ ] nada de estado de mundo em static
[ ] leitura de bloco não força carga de chunk
[ ] métodos que mudam o mundo exigem ServerWorld
[ ] nenhuma exceção escapa de método Vanilla
[ ] toda ação tem timeout e condição de desistência
```

## Registro

```text
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
[ ] dependências de ordem respeitadas
```

## Resources — os quatro elos

```text
[ ] lang            senão aparece o id na tela
[ ] modelo/textura  senão cubo preto e rosa
[ ] blockstate      (bloco)
[ ] loot table      (bloco) senão não dropa
[ ] recipe          se aplicável
[ ] tags            ferramenta correta
[ ] item group      senão é invisível no criativo
[ ] renderer        (entidade/block entity, no cliente)
```

## Client / Server

```text
[ ] nenhuma classe de cliente em código comum
[ ] o servidor valida tudo que vem do cliente
[ ] o estado inicial chega ao cliente
[ ] TESTADO em runServer
```

## Persistência

```text
[ ] par escrita/leitura existe
[ ] markDirty após mutação
[ ] o que é redescobrível NÃO é persistido (decisão escrita)
[ ] TESTADO: fechar e reabrir o mundo
```

## Performance

```text
[ ] frequência de cada operação justificada
[ ] raios de busca são os menores que resolvem
[ ] testado com carga realista, não com duas entidades
```

## Compatibilidade

```text
[ ] Mixins classificados
[ ] nenhum @Redirect/@Overwrite sem justificativa escrita
[ ] a falha degrada para comportamento Vanilla
[ ] classificação LOW/MEDIUM/HIGH registrada
```

## Verificação executada

```text
[ ] ./gradlew build
[ ] ./gradlew runGametest
[ ] ./gradlew runClient
[ ] ./gradlew runServer
[ ] a feature funciona em jogo
[ ] edge cases exercitados
[ ] logs limpos, sem spam e sem exceção engolida
```

## Entrega

```text
[ ] implementation-summary.md preenchido
[ ] conhecimento novo devolvido a docs/
[ ] o relato separa "verificado rodando" de "tem teste escrito"
[ ] o que ficou de fora está DECLARADO, com o motivo
```

---

**Se sobraram itens desmarcados que importam:** a feature não está pronta. Diga
quais, e o que falta — escopo reduzido declarado é entrega; reduzido em silêncio
é dívida escondida.
