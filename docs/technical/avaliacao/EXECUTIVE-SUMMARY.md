# Resumo executivo — auditoria técnica de 2026-10-10

**Village Colony 0.3.23** · commit `b290ed10` · Minecraft 1.21.1 / Fabric
**Nota global: 6,9/10** (média simples das 12 dimensões) · régua reprodutível
(`METODOLOGIA.md`): **B, 3,36/4**, que cai para **3,14** com os dois erros de
medida corrigidos (AUD-07). A nota de 09-21 (7,0) usou outro método e não é comparável.

**Em uma frase:** a engenharia está acima da média para um mod alpha (bateria de 690
GameTests estável, `core` isolado, mutação acima de 85%, CI que reprova de verdade),
mas o jogo real não confirma a promessa: no último playtest, metade dos ofícios
ficou parada, e o save não sobrevive a uma entrada ruim.

## Nota por dimensão

| Dimensão | Nota | Motivo principal |
|---|---|---|
| D1 Arquitetura | 8 | `core` sem nenhuma dependência de Minecraft (bytecode); ciclo raiz ↔ `fabric` |
| D2 Correção funcional | 5 | 690/690 na bateria, mas a Regra 51 reprovada em 4/8 ofícios em jogo |
| D3 Concorrência e determinismo | 7 | tudo na thread do servidor; 3 rodadas idênticas; 181 estáticos mutáveis |
| D4 Performance | 6 | ciclo p95 67 ms na 0.3.22, 27,7 ms na 0.3.23 (poucas amostras) |
| D5 Persistência | 5 | migração `BREEDER` testada; save malformado apaga tudo; versão não gravada |
| D6 Mixins | 9 | 3 mixins encadeáveis, prefixados, `defaultRequire = 1`; gate modsmith 14/14 |
| D7 Testes | 8 | 1398 unitários, 690 GameTests, PIT 85,66%; folga de 13 mutantes |
| D8 Build/CI/release | 8 | CI verde e honesto, SHA do JAR conferido; linha Codex fora da `main` |
| D9 Segurança | 7 | política exige nível 2 e tem limites; desserialização sem fronteira |
| D10 Documentação | 5 | `STATE.md` 4× acima do teto; CHANGELOG parado em 0.3.0 |
| D11 UX e telemetria | 7 | esperas acima de 2 min no `/vc log`; Regra 51 só fora do jogo |
| D12 Compatibilidade | 8 | 690/690 também com Fabric API 0.116.17 e Loader 0.19.5 |

## Top 5 riscos

1. **AUD-01 (P0): aldeões parados em jogo.** Na 0.3.22, a Regra 51 foi reprovada pelo
   fundidor (86%), construtor (68%), mineiro (21%) e fazendeiro (16%). O fundidor pegou 883
   tarefas e soltou 882 em uma hora. A sessão 0.3.23 da noite repete: fundidor 91%, construtor
   92%, mineiro 17%, e 311 tarefas soltas sem nenhuma feita. A raiz é o mineiro, que não acha boca
   de mina e não produz pedra nem ferro.
2. **AUD-02 (P1): perda silenciosa do save.** Qualquer exceção no leitor faz o Minecraft
   abrir o mundo sem colônias e gravar o vazio por cima. A sonda de fuzzing derrubou o
   leitor em 6 de 10 formas de NBT ruim.
3. **AUD-03 (P1): a versão do save nunca é gravada**, e a primeira migração não idempotente
   vai rodar a cada abertura de mundo. (O pico de 67 ms do ciclo, AUD-04, não se repetiu na
   0.3.23 e virou P2 até nova medida.)
4. **AUD-05 (P1): duas linhas de desenvolvimento.** 14 commits e a ADR-040 vivem só na
   `codex/fluxo-grafo-waypoints`, com worktree ativo, fora do `STATE.md` (ADR-035 §2).
5. **AUD-06 (P1): a demanda de material de obra não tem asserção.** O PIT está 13
   mutantes acima do limiar.

## Top 5 ganhos rápidos

| # | Item | Esforço | Efeito |
|---|---|---|---|
| 1 | AUD-03: gravar `saveVersion` no `writeNbt` | 1 linha + 1 teste | migração futura segura |
| 2 | AUD-02: `try/catch` por entrada no `ColonySaveReader` + promover `NbtFuzzProbeTest` | ~2 h | fecha o risco de perda total |
| 3 | AUD-06: 8 asserções em `WorkDemandTest` | ~1 h | ~10 mutantes mortos; folga do PIT dobra |
| 4 | AUD-07: corrigir C14 e C01 da régua | ~1 h | nota da avaliação confiável |
| 5 | AUD-12 + AUD-23: `suggests: modmenu` no `fabric.mod.json` e javadoc do mixin | 10 min | metadado e comentário verdadeiros |

## O que esta auditoria não fez

- Não jogou: os vereditos de jogo vêm das duas sessões do autor em 10-10 (0.3.22 e 0.3.23).
- Não rodou Spark, nem o nível 4 do modsmith (pack com outro mod de aldeão).
- Não alterou código de produção, não commitou e não apagou branches.

Roteiro: [`Roteiro-Correcoes-2026-10-10.md`](../Roteiro-Correcoes-2026-10-10.md) · detalhe: [ACTION-QUEUE.md](ACTION-QUEUE.md) · dimensões `D01`–`D12` nesta pasta ·
setup em [agent-setup.md](agent-setup.md) · régua em
[2026-10-11-b290ed10/](2026-10-11-b290ed10/scorecard.md).
