# Plano de implementação — <feature / sistema>

> Quebra o trabalho em passos que **compilam sozinhos**. Existe para impedir o
> "escrever tudo e tentar compilar no fim", onde os erros se mascaram.

**Data:** AAAA-MM-DD · **Plano de feature:** `<caminho>`

## Estado de partida

```bash
git status
./gradlew build
```

```text
[ ] o build passa HOJE, antes de qualquer mudança
[ ] estou numa branch, não na principal
```

<O que já existe e será reaproveitado. O que será removido.>

## Passos

> Regra: **se você não compila há mais de meia hora de trabalho, o passo é grande
> demais.**

### Passo 1 — <nome>

**Faz:**
**Arquivos:**
**Critério de pronto:** `./gradlew build`
**Se falhar:** <o que investigar primeiro>

### Passo 2 — <nome>

**Faz:**
**Depende de:** passo 1
**Critério de pronto:**

### Passo 3 — …

---

## Ordem recomendada

Para sistema novo, esta ordem costuma poupar retrabalho:

```text
1. modelo de domínio + testes de unidade      → build
2. registros                                   → build · aparece no jogo
3. resources                                   → build · aparece direito
4. persistência (salvar/carregar vazio)        → save/load
5. lógica mínima ponta a ponta                 → runClient
6. integração com Vanilla                      → gametest
7. demais capacidades, uma a uma               → build a cada uma
8. client/networking                           → runServer
9. performance                                 → medir
```

**Persistência no passo 4, não no fim.** Descobrir no vigésimo arquivo que o
estado não cabe no formato escolhido custa a refatoração inteira.

**Resources no passo 3.** Ver o conteúdo no jogo já ali valida o registro inteiro
de uma vez.

## Pontos de verificação

| Após o passo | Verificação | Resultado |
|---|---|---|
| 1 | `./gradlew test` | |
| 2 | conteúdo aparece no criativo | |
| 3 | sem cubo preto e rosa, sem `block.meumod.foo` | |
| 4 | fechar e reabrir o mundo, estado voltou | |
| 6 | `./gradlew runGametest` | |
| 8 | `./gradlew runServer` + cliente conectando | |

## O que NÃO fazer neste trabalho

<Refatorações tentadoras, features adjacentes, limpezas — listadas aqui para
serem deliberadamente adiadas em vez de entrarem sem querer.>

## Rollback

<Se um passo der errado, como voltar? Qual commit é o último bom?>

## Estado atual

> Atualize conforme avança. É isto que uma sessão nova lê para continuar.

| Passo | Status | Nota |
|---|---|---|
| 1 | feito / em andamento / não iniciado | |
| 2 | | |

**Próximo passo:** <o menor passo seguro>
**Bloqueios:** <o que impede, se houver>
