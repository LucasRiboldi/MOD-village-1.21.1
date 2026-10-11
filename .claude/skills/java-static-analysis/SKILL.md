---
name: java-static-analysis
description: Análise estática e métricas de teste em Java — PMD, Error Prone/NullAway, SpotBugs, Checkstyle, JaCoCo e PIT. Use para auditar qualidade, ler cobertura por pacote, listar mutantes sobreviventes ou rodar uma ferramenta que o build não aplica sem alterar o build. Não use para julgar se uma regra de jogo está certa.
---

# java-static-analysis

## Propósito

Transformar "o código parece bom" em números com fonte: violações do PMD,
erros do NullAway, cobertura por pacote, mutantes que sobrevivem.

## Quando usar

- Dimensões **D1, D7 e D9** da auditoria.
- Antes de afirmar uma porcentagem de cobertura ou mutação em qualquer documento.
- Quando o pedido citar uma ferramenta que o projeto **não** aplica (SpotBugs,
  Checkstyle) — rode por fora, sem mexer no `build.gradle`.

## Mapeamento para a auditoria

| Dimensão | O que este skill entrega |
|---|---|
| D1 Arquitetura | complexidade e tamanho via PMD |
| D7 Testes | JaCoCo por pacote, top sobreviventes do PIT |
| D9 Robustez | NullAway, SpotBugs (deserialização, NPE, igualdade) |

## O que o projeto já tem (não reinstalar)

| Ferramenta | Onde | Saída |
|---|---|---|
| Error Prone + NullAway | `build.gradle`, só `compileJava`, `OnlyNullMarked` | erro de compilação |
| PMD | `config/pmd/complexity.xml`, só `main` | `build/reports/pmd/main.html` |
| JaCoCo unitário | `jacocoTestReport` | `build/reports/jacoco/test/` |
| JaCoCo da bateria | `jacocoGametestReport` | `build/reports/jacoco/gametest/` |
| PIT | `pitest`, só `com.villagecolony.core.*`, limiar 85 | `build/reports/pitest/` |

## Checklist

1. Rodar **de verdade** e guardar a saída bruta em `build/audit/raw/`.
2. Cobertura: ler o **XML** do JaCoCo (não o HTML), somar por pacote,
   apontar pacote < 60 % de linhas. Somar unitário e GameTest separadamente —
   o mesmo pacote pode ter 10 % em um e 80 % no outro.
3. PIT: extrair `mutations.xml`, filtrar `status="SURVIVED"` e
   `NO_COVERAGE`, ordenar por classe crítica (planejador, reserva, persistência).
   Confirmar no log que houve `Generated N mutations` (memória
   `ci-verde-esconde-pit-parado`).
4. Ferramenta ausente: usar *init script* Gradle em `build/audit/` ou o JAR
   da ferramenta direto sobre `build/classes`. **Nunca** editar `build.gradle`
   numa auditoria.
5. Separar sempre: *"configurado"* ≠ *"rodou nesta sessão"* ≠ *"passou"*.

## Comandos

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.12"
./gradlew.bat test jacocoTestReport pmdMain --rerun-tasks --no-daemon
./gradlew.bat pitest --no-daemon
python -I scripts/audit/coverage_by_package.py build/reports/jacoco/test/jacocoTestReport.xml
```

## Anti-padrões

- Citar a porcentagem do README/TODO em vez da saída do comando.
- Rodar `test` incremental e somar só as classes que rodaram.
- Considerar `NO_COVERAGE` do PIT igual a `SURVIVED`: o primeiro pede teste,
  o segundo pede asserção melhor.
- "Corrigir" violação de PMD em código de produção durante uma auditoria.
