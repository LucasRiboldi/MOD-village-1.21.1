# ADR-021 - Pastor canonico e migracao do Criador

**Estado:** Aceita pela solicitacao do autor em 2026-09-21.
**Escopo:** profissao da colonia, fundacao e compatibilidade de save.

## Contexto

O mod mantinha duas constantes para o mesmo oficio. `BREEDER` ocupava as vagas,
a fundacao e o nome visivel "Criador", enquanto `SHEPHERD` sobrevivia como alias
e `ShepherdWork` ja executava a coleta de la. Isso tornava a profissao visivel
diferente da responsabilidade implementada.

## Decisao

1. `SHEPHERD` e a unica profissao de Pastor. Substitui `BREEDER` nas ordens de
   produtores e titulares fundacionais, no registro de capacidades e nas
   permissoes para ajudar na construcao quando ocioso. As seis vagas da
   `BigHouseMOD` e as sete profissoes produtoras nao mudam em quantidade.
2. `BREEDER` deixa de ser valor do enum e nao e gravado em novos saves. A
   leitura de um save antigo converte esse valor diretamente para `SHEPHERD`.
3. O nome antigo "Criador" escrito pelo mod sobre o aldeao passa a "Pastor"
   na proxima rotulagem. O icone de tesoura de um bau antigo continua indicando
   Pastor e seu texto e corrigido sem criar outro quadro. Nomes particulares
   dados pelo jogador continuam preservados pela regra de rotulagem existente.
4. Esta ADR substitui apenas as referencias a `BREEDER` nas ADR-011 e ADR-018;
   mantem a politica de crescimento e o contrato fisico da fundacao.

## Consequencias

- O executor `ShepherdWork` e a tarefa `COLLECT_WOOL` nao mudam de algoritmo.
- A migracao ocorre ao carregar o save; nenhum bloco ou aldeao e recriado por
  causa da troca de nome.
- Validacao no save real continua necessaria para confirmar os rotulos e o
  ritmo de trabalho apos a migracao.
