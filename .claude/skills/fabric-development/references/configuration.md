# Configuração

Config é para **preferência**, não para dados. Confundir os dois produz um mod
que perde estado, ou um arquivo de config que vira banco de dados corrompível.

## Três coisas diferentes

| | O que é | Onde vive | Muda em runtime |
|---|---|---|---|
| **Code configuration** | constantes de design (raio, intervalo, limite) | constantes no código | não |
| **Player/server configuration** | preferências de quem instala | arquivo de config | entre sessões |
| **World data** | estado do jogo | `PersistentState`, NBT | o tempo todo |

**Config não é banco de dados.** Se o valor muda porque o jogo aconteceu, ele é
world data — não config.

## Constantes no código

Muita coisa que vira config deveria ser constante nomeada:

```java
/** Raio de busca por trabalho. Além disso o custo cresce cubicamente. */
private static final int RAIO_DE_BUSCA = 64;
```

Vantagens sobre config: não precisa validação, não pode vir absurdo do usuário,
não precisa sincronizar, e o comentário explica **por que** aquele valor.

Pergunta de corte: **alguém realmente vai querer mudar isso?** Se a resposta é
"talvez, um dia", é constante. Config existe para o que as pessoas pedem, não
para tudo que é ajustável.

## Quando config se justifica

```text
[ ] pessoas com setups diferentes precisam de valores diferentes
[ ] há trade-off legítimo (performance × comportamento)
[ ] o dono do servidor precisa desligar uma feature
[ ] balanceamento que varia por modpack
```

## A regra que quebra multiplayer

**Config nunca decide se um registro existe.**

```java
// ✗ ids divergentes entre cliente e servidor → conexão cai no handshake
if (config.recursoAtivado) { Registry.register(...); }

// ✓ registra sempre; a config decide o COMPORTAMENTO
Registry.register(...);
if (config.recursoAtivado) { ativarComportamento(); }
```

Registro é identidade e precisa ser idêntico nos dois lados. Ver `registration.md`.

## Client config vs. server config

```text
CLIENT   preferência visual — não afeta o jogo
SERVER   regra do jogo — vale para todos, mora no servidor
```

Config de **regra** lida no cliente é um bug de autoridade: cada jogador jogaria
com uma regra diferente. Se a regra vale para o mundo, quem manda é o servidor —
e o cliente, no máximo, é informado.

## Validação

Arquivo de config é editado à mão por gente que não leu a documentação.

```text
[ ] valor fora da faixa → corrigir para o limite e avisar, não crashar
[ ] tipo errado → default e aviso
[ ] arquivo corrompido → recriar com defaults e avisar
[ ] chave desconhecida → ignorar (config de versão futura)
[ ] chave ausente → default
```

**Nunca crashe por config inválida.** O jogador não sabe o que fez; ele sabe que
o mod parou de funcionar depois que ele mexeu num arquivo.

```java
int raio = Math.clamp(lido, MIN, MAX);
if (raio != lido) {
    LOGGER.warn("[meumod] raio {} fora da faixa [{}, {}], usando {}", lido, MIN, MAX, raio);
}
```

## Migração

Config sobrevive a atualizações do mod.

```text
[ ] config antiga carrega na versão nova?
[ ] chave removida é ignorada sem erro?
[ ] chave nova tem default?
[ ] há versão no arquivo?
```

Sobrescrever a config do jogador numa atualização apaga escolhas que ele fez de
propósito. Acrescente com default; não reescreva o arquivo inteiro.

## Quando carregar

```text
onInitialize     → ler a config
                   (mas o registro não depende dela)
SERVER_STARTED   → aplicar o que depende do mundo
```

Config lida tarde demais deixa o mod rodar um tempo com defaults; lida cedo
demais, não tem onde aplicar.

## Bibliotecas

Existem bibliotecas de config para Fabric, e usar uma é legítimo. O custo é uma
dependência a mais que o jogador precisa instalar.

Para poucas opções, um JSON lido à mão resolve e não acrescenta dependência. Para
muitas opções com tela de configuração, a biblioteca se paga.

Decida pelo tamanho real, e declare a dependência no `fabric.mod.json` se adotar.

## Checklist

```text
[ ] o que é constante de design está como constante, não como config
[ ] config não decide existência de registro
[ ] config de regra é server-side
[ ] todo valor é validado com faixa e default
[ ] config inválida avisa, não crasha
[ ] config antiga continua carregando
[ ] chaves desconhecidas são ignoradas
[ ] dependência declarada, se usar biblioteca
```
