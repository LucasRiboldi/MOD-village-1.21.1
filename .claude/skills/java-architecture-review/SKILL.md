---
name: java-architecture-review
description: Revisão de arquitetura de código Java — camadas, isolamento entre pacotes, ciclos de dependência, god-classes e acoplamento. Use ao auditar a fronteira core/fabric do Village Colony, antes de dividir uma classe grande, ou quando um teste ArchUnit congelado mudar. Não use para revisar uma correção pontual de lógica.
---

# java-architecture-review

## Propósito

Responder com evidência, não com impressão: **o código respeita as camadas que a
ADR declara?** Onde há ciclo, classe grande demais ou acoplamento que impede
testar a regra sem subir o Minecraft?

## Quando usar

- Auditoria da dimensão **D1 (arquitetura e camadas)**.
- Antes de propor a divisão de uma god-class (`HousePlans`, planejadores, `*Task`).
- Quando o `archunit_store` congelado mudar num commit (ver memória
  `climb-out-nunca-desiste`: qualquer dependência nova o reescreve).

## Mapeamento para a auditoria

| Dimensão | O que este skill entrega |
|---|---|
| D1 Arquitetura | isolamento `core` ↔ Minecraft, ciclos entre pacotes, god-classes, CBO/LCOM |
| D7 Testes | o que o ArchUnit cobre e o que escapa (pacote fora da regra) |

## Checklist

1. **Regra declarada.** Ler a ADR de camadas e o teste ArchUnit
   (`src/test/java/**/Architecture*Test.java`). Anotar *quais pacotes* a regra
   enumera — pacote novo fora da lista escapa em silêncio.
2. **Regra executada.** Rodar o teste e confirmar no XML de
   `build/test-results/test/` que ele rodou (não só que o build passou).
3. **Ciclos.** `jdeps -verbose:package` sobre `build/classes/java/main` e
   procurar arestas nos dois sentidos entre pacotes do mod. Ciclo dentro de
   `core` é achado; ciclo `core` → `fabric` é P0 (quebra a ADR).
4. **Tamanho.** Classes > 500 linhas (regra do `CLAUDE.md`), métodos > 50
   linhas, complexidade ciclomática > 15 — o PMD do projeto
   (`config/pmd/complexity.xml`) já mede parte disso; ler o relatório, não refazer.
5. **Acoplamento.** Para as 10 maiores classes: CBO (tipos distintos
   referenciados) e LCOM aproximado (métodos que não compartilham campo).
   `javap -p -c` + contagem por script basta; não instalar ferramenta nova.
6. **Divisão proposta.** Só propor quebra de classe quando houver eixo natural
   (bioma, profissão, fase da obra) e o teste que prova que nada mudou.

## Comandos

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.12"
./gradlew.bat compileJava --no-daemon
"$JAVA_HOME/bin/jdeps" -verbose:package -filter:none build/classes/java/main \
  | grep 'com.villagecolony' > build/audit/jdeps.txt
find src/main/java -name '*.java' -exec wc -l {} + | sort -rn | head -20
```

## Anti-padrões

- Declarar "camadas isoladas" lendo a ADR, sem rodar o ArchUnit.
- Contar ciclo pelo `import` do código-fonte (pega import não usado, perde
  referência por nome totalmente qualificado). Use o bytecode (`jdeps`).
- Propor split de god-class como P0. Tamanho é dívida; P0 é defeito em jogo.
- Reescrever o `archunit_store` para "passar" — isso apaga o achado.
