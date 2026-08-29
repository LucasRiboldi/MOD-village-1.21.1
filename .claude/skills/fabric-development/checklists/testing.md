# Checklist — testes

> **"Tem teste" e "foi verificado rodando" são coisas diferentes.** As duas entram
> no relato, separadas.

## Escolha dos níveis

```text
[ ] escolhi os níveis proporcionalmente ao risco da mudança
```

| Mudança | Níveis mínimos |
|---|---|
| texto, config | 1, 2 |
| item novo | 1, 2, 3 |
| bloco com estado | 1–4 |
| Mixin | 1–3, 6 + gametest |
| persistência | 1–4, 6 |
| networking | 1–3, **5** |
| IA | 1–3, 6, 7 |
| migração de versão | todos |

```text
1 compile · 2 game start · 3 feature · 4 save/load
5 multiplayer · 6 regressão · 7 performance
```

## Nível 1 — compila

```bash
./gradlew build
```

```text
[ ] compila
[ ] testes de unidade passam
[ ] sem warning novo
```

## Nível 2 — o jogo inicia

```bash
./gradlew runClient
./gradlew runServer
```

```text
[ ] cliente inicia
[ ] SERVIDOR DEDICADO inicia            ← não é opcional
[ ] nenhum aviso de Mixin no log de boot
[ ] nenhuma exceção no boot
```

## Nível 3 — a feature funciona

```text
[ ] faz o que o objetivo dizia
[ ] o conteúdo aparece (registro ok)
[ ] os resources aparecem (nada de cubo preto e rosa, nada de "block.meumod.foo")
[ ] edge cases exercitados
```

## Nível 4 — save / load

```text
[ ] criar mundo → usar → FECHAR → REABRIR → o estado voltou
[ ] mundo da versão anterior do mod abre sem perder dados
```

> Pega o bug de persistência mais comum — `markDirty` esquecido. É barato e quase
> nunca é feito.

## Nível 5 — multiplayer

```text
[ ] cliente conecta a servidor dedicado
[ ] a feature funciona conectado
[ ] o estado aparece correto logo ao entrar
[ ] desconectar e reconectar mantém o estado
```

## Nível 6 — regressão

```text
[ ] o que funcionava continua funcionando
[ ] os comportamentos Vanilla que o mod preserva continuam
[ ] outras features do mesmo sistema
```

## Nível 7 — performance

```text
[ ] testado com carga realista, não com duas entidades
[ ] TPS estável numa sessão de alguns minutos
```

## Gametest

```bash
./gradlew runGametest
```

```text
[ ] passa
[ ] coordenadas relativas convertidas com context.getAbsolutePos
[ ] context.complete() no fim de cada teste
[ ] o entrypoint fabric-gametest está em sourceset SEPARADO
[ ] as classes de gametest NÃO estão no jar publicado
```

> Um servidor dedicado carrega `fabric-gametest` no boot. Apontar para classes
> ausentes **derruba o servidor** antes de o mod iniciar.

```text
[ ] o que o gametest NÃO cobre está DECLARADO
```

> O mundo de teste é **vazio**: sem vila gerada, sem estruturas, sem bioma real.
> Esconder isso faz um teste verde significar menos do que parece.

## Correção de bug

```text
[ ] reproduzi o bug ANTES da correção
[ ] escrevi um teste que FALHAVA antes e passa depois
[ ] cobri todos os caminhos da mesma causa
```

> A melhor prova disponível de que você consertou a coisa certa — e a proteção
> contra o bug voltar.

## Verificação manual

```text
[ ] passos reproduzíveis registrados
[ ] observado mais de uma vez
```

> Reproduzido uma vez não é reprodutível.

## Relato

```text
[ ] separei VERIFICADO RODANDO / TEM TESTE ESCRITO / NÃO VERIFICADO
[ ] nível pulado entrou em "não verificado" — não sumiu da lista
[ ] se o build falhou, mostrei a saída
```

---

**Nunca diga que passou sem ter executado.** É a única regra desta lista que não
admite exceção.
