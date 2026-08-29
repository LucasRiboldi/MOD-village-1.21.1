# Checklist — release

> O último portão. A pergunta não é "está pronto?" — é **"o que acontece com o
> mundo de alguém que instalar isto?"**

## Repositório

```bash
git status
cat .gitignore
```

```text
[ ] nada não commitado que devesse estar
[ ] nada commitado que não devesse: .env, chave, token, build/, run/, logs, .idea
[ ] a versão do mod foi incrementada em gradle.properties
```

> Segredo já rastreado: **pare e avise**. Não empurre.

## Build limpo

```bash
./gradlew clean build
```

```text
[ ] compila DO ZERO
[ ] testes de unidade passam
[ ] sem warning novo
[ ] o jar foi gerado em build/libs/
```

> Do zero. Build incremental esconde arquivo que só existe na sua máquina.

## O jar

```bash
unzip -l build/libs/*.jar | head -30
```

```text
[ ] fabric.mod.json presente, com a versão certa
[ ] *.mixins.json presente
[ ] assets/ e data/ presentes
[ ] NÃO contém classes de gametest
[ ] NÃO contém arquivo de desenvolvimento
```

> Gametest no jar **derruba servidores dedicados**: o entrypoint é carregado no
> boot e aponta para classes que não estão lá.

## Metadados

```text
[ ] id, nome, versão, descrição
[ ] license
[ ] icon existe no caminho declarado
[ ] entrypoints apontam para classes que existem NO JAR
[ ] depends com faixas certas (minecraft, fabricloader, fabric-api, java)
[ ] environment correto
```

## Runtime — os três ambientes

```bash
./gradlew runClient
./gradlew runServer
./gradlew runGametest
```

```text
[ ] cliente inicia
[ ] SERVIDOR DEDICADO inicia            ← onde mais pega problema
[ ] cliente conecta ao servidor dedicado
[ ] gametest passa
```

## Ciclo de vida do mundo

```text
[ ] criar mundo novo → funciona
[ ] fechar e reabrir → o estado voltou
[ ] mundo da versão ANTERIOR do mod abre sem perder dados
[ ] remover o mod de um mundo salvo não corrompe — ou o aviso está documentado
```

> O terceiro separa uma atualização de um incidente.

## Conteúdo completo

```bash
unzip -l build/libs/*.jar | grep -E "lang/|models/|blockstates/|loot_table"
```

```text
[ ] todo item/bloco tem nome traduzido
[ ] todo bloco tem modelo e textura
[ ] todo bloco tem loot table
[ ] recipes funcionam
[ ] tags aplicadas (ferramenta correta)
[ ] tudo aparece em alguma aba do criativo
[ ] sons registrados, se houver
```

## Logs

```text
[ ] sem spam por tick
[ ] sem stacktrace no boot
[ ] sem aviso de mixin que não aplicou
[ ] sem exceção engolida em silêncio
[ ] mensagens com prefixo do mod e conteúdo útil
```

> Mensagem que aparece centenas de vezes numa sessão curta é spam, mesmo que
> individualmente pareça útil.

## Performance

```text
[ ] TPS estável numa sessão de alguns minutos
[ ] testado com carga realista
[ ] sem travamento ao carregar chunk
```

## Compatibilidade

```text
[ ] Mixins classificados
[ ] conflitos conhecidos DOCUMENTADOS
[ ] dependências declaradas no fabric.mod.json
[ ] testado com pelo menos um mod do mesmo domínio, se houver
```

## Documentação

```text
[ ] README no estado de hoje: o que faz, limitações, instalação, requisitos
[ ] mudanças desta versão registradas
[ ] limitações conhecidas ditas EXPLICITAMENTE
[ ] versões suportadas declaradas
```

## O relato final

```text
VERIFICADO RODANDO   o que você executou e observou
TEM TESTE ESCRITO    coberto, mas não executado agora
NÃO VERIFICADO       o que ficou de fora, e por quê
```

```text
[ ] nenhum item deste checklist foi marcado sem verificação
[ ] o que não foi feito entrou em "não verificado" — não sumiu da lista
```

---

Um release com limitações declaradas é um release honesto. Um que omite o que não
foi testado transfere o custo para quem instalar.
