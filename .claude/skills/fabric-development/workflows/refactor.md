# Workflow — refatorar

Refatoração é **mudar a estrutura sem mudar o comportamento**. No momento em que
o comportamento muda, deixou de ser refatoração e virou outra coisa — que precisa
de outro workflow.

---

## 1. Justificar

```text
QUAL PROBLEMA concreto isto resolve?
QUAL COMPLEXIDADE está atrapalhando hoje?
QUAL DEPENDÊNCIA está errada?
QUAL COMPORTAMENTO NÃO PODE MUDAR?
```

Motivos válidos: uma mudança que deveria ser simples toca sete arquivos; ninguém
entende quem é dono de um estado; a classe cresceu além do que cabe na cabeça;
duplicação real está divergindo.

Motivo inválido: **"está feio"**. Código feio que funciona, é testado e ninguém
precisa tocar não custa nada. Refatorá-lo custa risco.

Pergunta de corte: **qual trabalho futuro isto destrava?** Sem resposta concreta,
não refatore agora.

## 2. Rede de segurança

Refatoração sem verificação é reescrita às cegas.

```bash
./gradlew build
./gradlew runGametest
```

```text
[ ] o build passa HOJE, antes de qualquer mudança
[ ] existe teste cobrindo o comportamento que deve permanecer
[ ] se não existe, ESCREVA ANTES de refatorar
```

O teste escrito antes é o que distingue "refatorei" de "mexi e parece que ainda
funciona".

## 3. Uma coisa por vez

```text
✗  REFACTOR + FEATURE NOVA + MIGRAÇÃO no mesmo passo
✓  refatorar → verificar → feature → verificar
```

Misturados, um erro em qualquer um deles fica indistinguível dos outros. O
diff vira ilegível e a revisão, impossível.

Se a refatoração é pré-requisito de uma feature, faça **em dois commits
separados**: primeiro a estrutura sem mudança de comportamento, depois a feature.

## 4. Passos pequenos

```text
mudança pequena → build → mudança pequena → build → ...
```

Regra prática: **se você não compila há mais de meia hora, pare e compile.**

Refatorações que valem em mod Fabric, do mais seguro ao menos:

| Refatoração | Risco | Observação |
|---|---|---|
| renomear (via IDE) | baixo | cuidado com nome referenciado em string: mixin `method =`, ids, NBT keys |
| extrair método | baixo | |
| mover classe de pacote | baixo | **atualize `*.mixins.json` e `fabric.mod.json`** |
| extrair classe | médio | quem fica dono do estado? |
| trocar estado estático por instância | médio | é quase sempre uma melhoria real |
| mudar formato de persistência | **alto** | quebra save — ver passo 6 |
| mudar contrato de packet | **alto** | quebra compatibilidade de rede |

## 5. O que NÃO se renomeia livremente

Estes nomes são **contratos externos**, não detalhes internos:

```text
Identifier de conteúdo registrado    → renomear quebra saves e datapacks
chave de NBT / nome de arquivo de PersistentState  → renomear abandona o save
chave de lang                        → renomear apaga a tradução
alvo de mixin (method = "...")       → é string, o compilador não avisa
entrypoint no fabric.mod.json
mod id
```

O caso mais traiçoeiro é o `method =` do Mixin: renomear o método Vanilla no seu
lado não muda nada, mas mover o **seu** mixin de pacote quebra o
`*.mixins.json` — e o compilador fica calado.

```bash
grep -rn "method = \"" src/main/java | head
cat src/main/resources/*.mixins.json
```

## 6. Se o formato de persistência mudar

Deixou de ser refatoração pura: o save existente é um contrato com o jogador.

```text
[ ] o save antigo ainda carrega?
[ ] campos novos têm default seguro?
[ ] campos removidos são ignorados sem erro?
[ ] há número de versão no NBT?
[ ] testado: mundo criado na versão anterior, aberto na nova
```

Ver `references/persistence.md` e `references/migration.md`.

## 7. Verificar

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

```text
[ ] tudo que passava continua passando
[ ] o comportamento em jogo é idêntico
[ ] save antigo carrega
[ ] nenhum aviso novo de mixin no boot
[ ] os logs não mudaram de forma inesperada
```

**"O comportamento é idêntico" precisa ser verificado, não presumido.** É a única
afirmação que uma refatoração faz, e é a que ninguém testa.

## 8. Limpar de verdade

Refatoração que deixa o antigo ao lado do novo piorou o código:

```text
[ ] código morto removido
[ ] a versão antiga não ficou "por segurança"
[ ] comentários que descreviam a estrutura antiga foram atualizados
[ ] documentação de arquitetura atualizada
```

O git guarda o que foi removido. Comentar código para "não perder" é guardar duas
versões e confundir a próxima leitura.

## Fechamento

`checklists/architecture.md`.

Relate **o que mudou de estrutura, o que provadamente não mudou de comportamento,
e como isso foi verificado.** Uma refatoração cuja verificação foi "parece que
está funcionando" não está pronta.
