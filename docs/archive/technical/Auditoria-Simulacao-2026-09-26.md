> **HISTÓRICO** — arquivado em 2026-10-06 (ADR-036, item 24). Não descreve o estado atual do mod; o estado vivo está em `STATE.md` e as regras em `docs/RULES.md`.

# Auditoria de Simulação - 2026-09-26

## Evidência executada

- `gradlew.bat build`: sucesso em 13 segundos.
- Testes unitários: 1.145 testes sem falhas nesta sessão.
- Simulação Fabric: `480 GAME TESTS COMPLETE`, com **59 falhas obrigatórias**.
- `scripts/analyze_village_log.py` no `latest.log`: nenhum candidato a loop.
  O arquivo analisado contém a rodada GameTest, não um playtest novo de save;
  portanto não prova o comportamento de uma vila real.

## Bloqueios críticos confirmados

1. **P0 - A bateria GameTest não está publicável.** 59 de 480 cenários falharam.
   A concentração é construção (20), mineração (9), fabricação (8), lenhador
   (7), fundidor (3), fazendeiro (3), plano de fazenda (2) e um cenário em
   ciclo, rotação de casas, arrendamento de mina, transbordo do mineiro,
   pastor e fuga de encalhado. Não atribuir ainda uma causa única: eles cruzam
   subsistemas e precisam ser reproduzidos em grupos menores.
2. **P0 - Não publicar JAR nem commit de release desta correção enquanto a
   bateria Fabric permanecer vermelha.** A compilação e os unitários verdes
   não substituem esses testes de integração.
3. **P1 - A regra de colônia ativa por jogador tem só teste unitário direto.**
   A execução normal está protegida pelo manipulador do servidor, mas os
   GameTests que chamam trabalho diretamente preservam uma exceção sem jogador.
   Falta um GameTest que exercite presença e distância reais de jogador.

## Riscos estruturais e inconsistências a corrigir

1. **P1 - A simulação de sete profissões ainda tem ciclos próprios.** O TODO
   já exige ADR antes de consolidar o ciclo comum; a duplicação amplia o risco
   de uma profissão obedecer pausa, baú ou recuperação de travamento de modo
   diferente das demais.
2. **P1 - Parte das regras de decisão continua na camada `fabric`.** A
   migração ao `core` também depende de ADR; sem ela, testes puros não cobrem
   toda a política de mundo.
3. **P2 - 25 avisos Error Prone na compilação.** Incluem conversões implícitas
   de `long` para `double`, risco de estouro em cálculo de coordenada, acessores
   de record sem `@Override`, ordinais de enum, campos de blocos em enum e um
   parâmetro de trabalho do lenhador não usado. Não são travamentos confirmados,
   mas devem ser removidos para que alertas novos voltem a ser visíveis.
4. **P2 - Gradle informa APIs depreciadas incompatíveis com Gradle 10.** É
   dívida de compatibilidade da ferramenta, não falha atual de Fabric.
5. **P2 - A cobertura JaCoCo não mede a bateria `fabric`.** A pendência #5 do
   TODO continua aberta.
6. **P2 - O estado vivo e o TODO contêm resultados históricos verdes e a rodada
   atual vermelha.** Toda publicação deve citar a rodada exata e não reutilizar
   o número 480/480 de outra alteração.

## Melhorias priorizadas

1. Isolar os 59 GameTests por grupo e encontrar a primeira causa comum antes
   de alterar produção.
2. Criar GameTest da regra de proximidade com jogador real.
3. Mostrar diagnóstico para o jogador por `/vc log`: estado atual, esperas e
   travamentos traduzidos do `ActivityTrace`.
4. Depois dos bloqueios, criar ADR para ciclo comum de profissões e mover regras
   puras ao `core`.
5. Fechar os avisos Error Prone, a cobertura GameTest e a compatibilidade Gradle.

## Menu de diagnóstico entregue nesta sessão

`/vc log` funciona no chat do servidor sem instalar um cliente adicional.
Ele encontra a colônia próxima, mostra quantos profissionais estão registrados
e reduz o traço técnico ao último estado de cada um: ativo, aguardando, atenção
ou travado. Fora do raio, explica que a vila está pausada para economizar
processamento. O menu não inventa causa: ele apresenta apenas estados e motivos
que a simulação já registrou.
