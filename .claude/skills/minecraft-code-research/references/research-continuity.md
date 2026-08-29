# Continuidade entre sessões

Uma sessão de Claude Code termina e leva junto tudo que estava só no contexto. Se
a pesquisa vive apenas na conversa, a próxima sessão refaz o caminho, chega a
conclusões ligeiramente diferentes e ninguém percebe a divergência.

**Regra:** pesquisa que importa mora em arquivo no repositório, não na conversa.

## Onde a pesquisa mora

Estrutura recomendada. Adapte ao que o projeto já usa — se ele já tem
`docs/technical/` e `docs/decisions/`, encaixe ali em vez de criar uma segunda
árvore paralela.

```text
docs/
├── research/
│   ├── vanilla/        análises de sistemas Vanilla
│   ├── fabric/         o que a Fabric oferece, e os limites
│   ├── mods/           análise de mods externos, um arquivo por mod
│   ├── systems/        sistemas do próprio projeto
│   └── comparisons/    matrizes comparativas
├── knowledge/
│   ├── class-map.md    classe Vanilla → o que faz → onde é usada
│   ├── system-map.md   sistema → classes → entrypoints
│   ├── glossary.md     termos do domínio e do jogo
│   └── patterns.md     padrões que se repetem e valem reusar
├── architecture/
├── decisions/          ADRs
├── experiments/
└── research-status.md  ← o índice vivo
```

**Não crie documento duplicado.** Antes de escrever um arquivo novo, procure o
que já cobre o assunto e atualize-o. Dois documentos sobre o mesmo sistema
divergem em semanas, e aí ninguém sabe qual vale.

```bash
ls docs/research/**/*.md 2>/dev/null
grep -ril "villager\|brain\|poi" docs/ | head
```

## `research-status.md` — o índice vivo

É o primeiro arquivo a ler e o último a escrever. Modelo em
`templates/research-status.md`.

Ele responde, sem abrir mais nada:

- qual é o objetivo atual e em que modo
- para qual versão as conclusões valem
- o que já é `[FATO]` (e não precisa ser repesquisado)
- o que é `[HIPÓTESE]` pendente e qual validação falta
- quais riscos estão abertos
- qual é o próximo passo recomendado, com prioridade

Se ele estiver desatualizado, ele é pior que ausente: alguém vai confiar nele.
Atualizá-lo é parte de terminar a pesquisa, não um extra.

## Protocolo de retomada

Ao começar uma sessão que continua trabalho anterior:

1. **Ache o status.** `find . -name "research-status.md"`. Sem ele, você está na
   primeira sessão — crie-o no fim.
2. **Leia o objetivo.** Ele ainda é o objetivo? Se o usuário pediu outra coisa,
   diga que o status aponta para outro alvo e confirme antes de seguir.
3. **Revalide a versão.** Compare o que o documento diz com o `gradle.properties`
   de hoje:

   ```bash
   grep -E "minecraft_version|yarn_mappings|fabric_version|loader_version" gradle.properties
   ```

   Versão diferente **invalida** conclusões sobre mappings, assinaturas e APIs.
   Não as apague — marque-as como `[VALIDAÇÃO NECESSÁRIA]` com a versão antiga
   registrada.
4. **Verifique se o código mudou.** Se a análise citava arquivos do projeto:

   ```bash
   git log --oneline -10 -- src/main/java/<caminho analisado>
   ```

   Arquivo tocado desde a análise = conclusão a revalidar.
5. **Não repita pesquisa concluída.** Um `[FATO]` com fonte registrada é um fato.
   Reabra apenas se a versão mudou ou se a evidência era fraca.
6. **Continue pela prioridade mais alta pendente** (P0 antes de P1, etc.).

## Quando o código muda debaixo da pesquisa

Aconteceu, e vai acontecer. O que não pode é passar silencioso.

- Conclusão que dependia do arquivo alterado → `[VALIDAÇÃO NECESSÁRIA]`.
- Conclusão sobre Vanilla numa versão que não mudou → continua válida.
- Decisão arquitetural (`[DECISÃO]`) → continua valendo até alguém decidir outra
  coisa; mudança de código não revoga decisão, mas pode contradizê-la — e aí vale
  registrar a contradição em vez de escolher em silêncio.

## Escrevendo para a próxima sessão

Escreva para alguém que não viu a conversa. Concretamente:

- **Caminhos completos**, não "aquele arquivo do scanner".
- **Versão em toda conclusão** que dependa dela.
- **A pergunta que gerou a pesquisa**, não só a resposta. Sem a pergunta, ninguém
  sabe se a resposta ainda serve.
- **O que você decidiu não investigar e por quê** — isso evita que a próxima
  sessão gaste uma hora numa trilha que já foi julgada irrelevante.

## Alimentando o conhecimento de volta

Quando uma implementação revelar algo que a pesquisa não sabia — "essa Activity
tem prioridade X", "esse POI é reivindicado assim", "esse método é chamado duas
vezes por tick" — isso volta para `docs/research/` ou `docs/knowledge/`.

Conhecimento descoberto durante implementação é o mais caro de todos: custou o
bug. Deixá-lo apenas no commit message é jogá-lo fora.
