# Como reavaliar as skills

Os resultados da última avaliação estão em `../AVALIACAO.md`. Este diretório
guarda o **aparato**, para que a próxima medição seja comparável em vez de
improvisada.

## Quando rodar de novo

- depois de mudar conteúdo relevante das skills
- ao migrar de versão do Minecraft (os `[FATO]` viram suspeitos)
- ao trocar o modelo (o resultado atual vale para Opus 5)

**Já foi trocado uma vez, em 2026-09-01:** `eval-4` e `eval-5` rodaram também
em Haiku 4.5 (mesmos prompts, `model: haiku` no subagente), e os dois foram
repetidos mais 2x por condição (n=3 cada) depois de um resultado n=1
chamativo demais pra confiar. **O que sobrevive nas duas: em todas as 12
respostas (2 casos × 3 rodadas × 2 condições), pelo menos uma alegação de
API/comando fabricada aparece em algum lugar** — com skill ou sem. **O que
NÃO sobreviveu:** a leitura de n=1 pro `eval-4` era "a skill dá falsa sensação
de rigor" (checklist marcado ✅ sem verificar); só apareceu numa das 3
rodadas — não generalizar a partir de amostra única é o próprio aviso deste
arquivo se confirmando na prática. **Achado novo em `eval-5`, ainda hipótese
(2 de 3, não regra):** o `with_skill` foi investigar arquivos reais do
projeto (nomes de classe/método/linha conferidos e corretos) em 2 das 3
rodadas; nenhuma resposta `without_skill` fez esse tipo de investigação
específica. Detalhes em `AVALIACAO.md`, seções "Rodada 2026-09-01 (2)" a "(4)"
— a (4) é a leitura mais completa.

## As duas rodadas

**Conhecimento** (`prompts.json` → `rodada-1-conhecimento`)
Seis perguntas de uma tacada (4 testadas em 2026-08-29, 2 testadas em
2026-09-01 — ver `AVALIACAO.md`). Dois agentes por caso: um instruído a usar a
skill, outro instruído a **não** usar nenhuma. Cada um salva a resposta em
`<workspace>/iteration-1/<id>/{with_skill,without_skill}/outputs/resposta.md`.
Esta rodada não escreve código no repo (só a resposta, fora dele) — não precisa
de clone, basta restringir os agentes a leitura (ver "Isolamento" abaixo).

**`eval-4-mixin-vs-escada` e `eval-5-perf-antes-de-otimizar`** (2026-09-01) —
os dois primeiros casos para `minecraft-code-research`, a única das três
skills do projeto que tinha ficado de fora em 2026-08-29 (os 4 casos originais
cobriram só `minecraft-villager-systems` e `fabric-development`). Medem se a
skill segue a escada de extensão (`checklists/before-modifying-vanilla.md`,
Mixin como último recurso) e se exige evidência de perfil antes de sugerir uma
otimização (`references/evidence-and-claims.md`), em vez de conhecimento de
API. Resultado: comportamento empatado nos dois (ambas as condições evitaram
Mixin desnecessário e exigiram profiler antes de otimizar), mas a leitura
manual + verificação por `javap` achou 2 erros factuais reais, os dois do lado
`without_skill` — ver `AVALIACAO.md`.

**Aviso metodológico novo:** o campo `skill` de cada caso indica qual skill o
caso foi *desenhado* para exercitar, não uma trava — as skills disparam
sozinhas por descrição, e nada impede outra skill de disparar junto com a
indicada (ver `aviso-metodologico` em `prompts.json`). Isso não foi controlado
nos 4 casos de 2026-08-29; quem rodar os pendentes deve registrar no relatório
quais skills o agente `with_skill` efetivamente invocou.

```bash
python grade-conhecimento.py <workspace>
```

**Processo** (`prompts.json` → `rodada-2-processo`)
Tarefa de implementação real. Mede disciplina lida do **código produzido** e do
**relatório de entrega** — nenhum critério mede conhecimento de API. O caso
agora traz `skills-esperadas: [fabric-development, minecraft-villager-systems]`
— antes não estava atribuído a nenhuma skill, o que tornava o resultado (empate
9/9) difícil de interpretar. `minecraft-code-research` não é o alvo aqui: a
tarefa mexe num sistema que o mod já possui, sem exigir investigar Vanilla do
zero — essa skill continua sem nenhum caso de processo dedicado a ela (só os
dois de conhecimento acima, pendentes).

```bash
python grade-processo.py <workspace>
```

## Isolamento — obrigatório

**Rodada de processo:** os agentes escrevem código. **Nunca aponte para o
repositório real.**

```bash
git clone --local . <workspace>/run-with
git clone --local . <workspace>/run-without
```

Clone local usa hardlinks (o pack aqui tem ~6 MB) e não leva `build/` nem
`.gradle/`, que são ignorados. O repositório de trabalho fica intocado.

Em Windows, cuidado com caminho de workspace muito profundo/aninhado: o clone
pode falhar com `Filename too long` nos objetos do `.git` (aconteceu tentando
clonar dentro do diretório de scratchpad da sessão, em 2026-09-01). Se isso
acontecer, use um caminho mais curto (ex. `C:\temp\<algo-curto>`) em vez de
tentar contornar com `core.longpaths` — não mexer em config do git.

**Rodada de conhecimento:** não escreve código no repo, só a resposta (fora
dele). Clone não é obrigatório aqui — basta restringir o agente a ferramentas
de leitura (sem Edit/Write/Bash de escrita) e instruir explicitamente para não
criar/editar/apagar nada dentro do repositório. Foi assim que os casos
`eval-4`/`eval-5` rodaram em 2026-09-01.

**Limitação encontrada em 2026-09-01, ainda sem solução:** o mecanismo de
subagente nomeado usado para essa rodada não expôs `total_tokens`/`duration_ms`
por notificação, ao contrário do que o fluxo de `skill-creator` assume — só deu
pra estimar duração por relógio de parede (checagens periódicas de status), sem
contagem de tokens nenhuma. Isso é pior do que a medição de 2026-08-29, que
tinha os dois números. Se for repetir, vale investigar um mecanismo de
subagente que devolva essas métricas antes de rodar, em vez de aceitar a perda
de novo.

## O aviso que mais importa

**Os scripts erraram quatro vezes na avaliação de 2026-08-29, e três dos erros
favoreciam a skill.**

Correção por palavra-chave não atravessa sinônimo nem markdown: o baseline
escreveu `### O que **não** foi verificado` e o regex procurava
"não verificado"; cobriu a manutenção do `WALK_TARGET` com outra estrutura de
frase; tratou NBT com `Math.max(0, getInt(...))` numa forma não prevista.

**Use o script para triagem e leia as respostas à mão antes de concluir.**
Número de script sozinho não aprova nem reprova nada — e o viés dele tende a
favorecer a hipótese de quem escreveu os critérios.

**2026-09-01 — dois problemas a mais, corrigidos:**

1. `grade-conhecimento.py` não tinha critério pra `eval-4`/`eval-5` — a
   pasta desses casos era simplesmente ignorada pelo loop principal (nem
   aparecia como "(pendente)", só sumia). Critérios adicionados, calibrados
   contra os nomes de método/classe reais confirmados por `javap` em
   2026-09-01 (`AVALIACAO.md`) — incluindo checks que flagram especificamente
   os dois nomes inventados que o Haiku produziu (`canDespawn()`,
   `shouldDespawn()`, `findClosestPoiPosition`), sem virar lista genérica de
   alucinação (são sentinelas de regressão conhecida, não detecção geral).
   Validado rodando o script contra as respostas reais desta sessão — bateu
   com a leitura manual nos dois casos.
2. Nenhum aviso existia quando uma pasta de eval ficava sem critério em
   `CHECKS` — ela só desaparecia do relatório, silenciosamente (foi assim que
   o problema acima passou despercebido até agora). O script agora compara as
   pastas em disco contra `CHECKS.keys()` e avisa no fim se sobrar alguma.

## Como não enganar a si mesmo

- o baseline precisa ser instruído a **não** invocar skill (elas estão
  instaladas globalmente e disparam sozinhas)
- confira se o baseline citou o projeto ou as skills; se citou, o resultado está
  contaminado
- registre tokens e duração: skill que empata custando mais é resultado, não
  detalhe
- n=1 não prova ausência de diferença. Para isso, repita 3–5 vezes por condição
  e olhe a variância, não a média
