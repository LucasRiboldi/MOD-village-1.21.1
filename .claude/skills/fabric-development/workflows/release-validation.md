# Workflow — validar antes de publicar

O último portão. A pergunta não é "está pronto?" — é **"o que acontece com o
mundo de alguém que instalar isto?"**

---

## 1. Estado do repositório

```bash
git status
git log --oneline -10
```

```text
[ ] nada não commitado que devesse estar
[ ] nada commitado que não devesse (.env, chave, build/, log, arquivo de IDE)
[ ] a versão do mod foi incrementada em gradle.properties
```

```bash
grep -n "mod_version" gradle.properties
cat .gitignore
```

## 2. Build limpo

```bash
./gradlew clean build
```

```text
[ ] compila do zero
[ ] testes de unidade passam
[ ] sem warning novo
[ ] o jar foi gerado em build/libs/
```

**Do zero.** Build incremental esconde arquivo que só existe na sua máquina.

## 3. O jar

```bash
unzip -l build/libs/*.jar | head -30
```

```text
[ ] fabric.mod.json presente e com a versão certa
[ ] mixins.json presente
[ ] assets/ e data/ presentes
[ ] NÃO contém classes de gametest
[ ] NÃO contém arquivo de desenvolvimento
```

O terceiro item derruba servidores: um servidor dedicado carrega o entrypoint
`fabric-gametest` no boot, e apontar para classes que não estão no jar mata o
servidor antes de o mod iniciar. Por isso gametest mora em sourceset e
`fabric.mod.json` separados.

## 4. Metadados

```bash
cat src/main/resources/fabric.mod.json
```

```text
[ ] id, nome, versão, descrição
[ ] license
[ ] icon existe no caminho declarado
[ ] entrypoints apontam para classes que existem NO JAR
[ ] depends com as faixas certas (minecraft, fabricloader, fabric-api, java)
[ ] environment correto (*, client ou server)
```

## 5. Runtime — os três ambientes

```bash
./gradlew runClient
./gradlew runServer
./gradlew runGametest
```

```text
[ ] cliente inicia
[ ] servidor dedicado inicia          ← o que mais pega problema
[ ] cliente conecta ao servidor dedicado
[ ] gametest passa
```

**Servidor dedicado não é opcional.** É onde classe de cliente vazada em código
comum lança `NoClassDefFoundError` e onde autoridade de cliente aparece.

## 6. Ciclo de vida do mundo

```text
[ ] criar mundo novo → funciona
[ ] fechar e reabrir → o estado voltou
[ ] mundo criado na versão ANTERIOR do mod → abre sem perder dados
[ ] remover o mod de um mundo salvo → não corrompe (ou o aviso está documentado)
```

O terceiro é o que separa uma atualização de um incidente.

## 7. Conteúdo completo

```text
[ ] todo item/bloco tem nome traduzido (nada de "block.mymod.foo")
[ ] todo bloco tem modelo e textura (nada de cubo preto e rosa)
[ ] todo bloco tem loot table (dropa alguma coisa)
[ ] recipes funcionam
[ ] tags aplicadas (ferramenta correta)
[ ] sons registrados, se houver
```

```bash
unzip -l build/libs/*.jar | grep -E "lang/|models/|blockstates/|loot_table"
```

## 8. Logs

```text
[ ] sem spam por tick
[ ] sem stacktrace no boot
[ ] sem aviso de mixin que não aplicou
[ ] sem exceção engolida em silêncio
[ ] as mensagens têm prefixo do mod e são úteis
```

`grep -c` no log de uma sessão curta: mensagem que aparece centenas de vezes é
spam, mesmo que individualmente pareça útil.

## 9. Performance

```text
[ ] TPS estável numa sessão de alguns minutos
[ ] testado com carga realista (não com duas entidades)
[ ] sem travamento ao carregar chunk
```

`workflows/performance-workflow.md` se houver dúvida.

## 10. Compatibilidade

```text
[ ] Mixins classificados (checklists/compatibility.md)
[ ] conflitos conhecidos DOCUMENTADOS
[ ] dependências declaradas no fabric.mod.json
[ ] testado com pelo menos um mod do mesmo domínio, se houver
```

Conflito documentado é suporte. Conflito não documentado é um relatório de bug
confuso que consome mais tempo do que teria custado escrever a linha.

## 11. Documentação

```text
[ ] README no estado de hoje: o que faz, limitações, como instalar, requisitos
[ ] mudanças desta versão registradas
[ ] limitações conhecidas ditas explicitamente
[ ] versões suportadas declaradas
```

## 12. O relato

Ao entregar, separe explicitamente:

```text
VERIFICADO RODANDO   o que você executou e observou
TEM TESTE ESCRITO    o que está coberto mas você não rodou agora
NÃO VERIFICADO       o que ficou de fora, e por quê
```

**Nunca diga que passou sem ter executado.** Se algum item deste workflow não foi
feito, ele entra em "não verificado" — não some da lista.

## Fechamento

`checklists/release.md`.

Um release com limitações declaradas é um release honesto. Um release que omite o
que não foi testado transfere o custo para quem instalar.
