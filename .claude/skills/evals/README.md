# Como reavaliar as skills

Os resultados da última avaliação estão em `../AVALIACAO.md`. Este diretório
guarda o **aparato**, para que a próxima medição seja comparável em vez de
improvisada.

## Quando rodar de novo

- depois de mudar conteúdo relevante das skills
- ao migrar de versão do Minecraft (os `[FATO]` viram suspeitos)
- ao trocar o modelo (o resultado atual vale para Opus 5)

## As duas rodadas

**Conhecimento** (`prompts.json` → `rodada-1-conhecimento`)
Quatro perguntas de uma tacada. Dois agentes por caso: um instruído a usar a
skill, outro instruído a **não** usar nenhuma. Cada um salva a resposta em
`<workspace>/iteration-1/<id>/{with_skill,without_skill}/outputs/resposta.md`.

```bash
python grade-conhecimento.py <workspace>
```

**Processo** (`prompts.json` → `rodada-2-processo`)
Tarefa de implementação real. Mede disciplina lida do **código produzido** e do
**relatório de entrega** — nenhum critério mede conhecimento de API.

```bash
python grade-processo.py <workspace>
```

## Isolamento — obrigatório

Os agentes escrevem código. **Nunca aponte para o repositório real.**

```bash
git clone --local . <workspace>/run-with
git clone --local . <workspace>/run-without
```

Clone local usa hardlinks (o pack aqui tem ~6 MB) e não leva `build/` nem
`.gradle/`, que são ignorados. O repositório de trabalho fica intocado.

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

## Como não enganar a si mesmo

- o baseline precisa ser instruído a **não** invocar skill (elas estão
  instaladas globalmente e disparam sozinhas)
- confira se o baseline citou o projeto ou as skills; se citou, o resultado está
  contaminado
- registre tokens e duração: skill que empata custando mais é resultado, não
  detalhe
- n=1 não prova ausência de diferença. Para isso, repita 3–5 vezes por condição
  e olhe a variância, não a média
