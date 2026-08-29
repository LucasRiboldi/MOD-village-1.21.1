# Minecraft Research Status

> O índice vivo da pesquisa. Primeiro arquivo a ler numa sessão nova, último a
> escrever antes de fechar. Mantenha em `docs/research-status.md`.
>
> Se estiver desatualizado, é pior que ausente — alguém vai confiar nele.

**Última atualização:** AAAA-MM-DD
**Atualizado por:** sessão / pessoa

---

## Objetivo atual

<Uma frase. O que estamos tentando entender ou decidir.>

**Modo de pesquisa:** QUICK / FEATURE / DEEP / FORENSIC

## Ambiente

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mappings | Yarn 1.21.1+build.3 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.15+1.21.1 |
| Java | 21 |
| Conferido em | AAAA-MM-DD, via `gradle.properties` |

> Versão diferente da registrada **invalida** conclusões sobre nomes, assinaturas
> e APIs. Não apague: marque como `[VALIDAÇÃO NECESSÁRIA]` com a versão antiga.

---

## Sistemas Vanilla investigados

| Sistema | Status | Documento |
|---|---|---|
| <ex.: Villager Brain> | concluído / parcial / não iniciado | `docs/research/vanilla/<arquivo>.md` |

## APIs Fabric investigadas

| Necessidade | API encontrada? | Limitações | Documento |
|---|---|---|---|
| | sim / não / parcial | | |

## Mods investigados

| Mod | Versão MC | Pergunta respondida | Documento |
|---|---|---|---|
| | | | `docs/research/mods/<arquivo>.md` |

---

## Fatos confirmados

> `[FATO]` com fonte. Não precisa ser repesquisado enquanto a versão não mudar.

- `[FATO]` <afirmação> — fonte: <arquivo/classe/método>, versão <X>
- `[FATO]` …

## Inferências importantes

> Conclusões apoiadas em fatos, não observadas diretamente.

- `[INFERÊNCIA]` <afirmação> — apoiada em: <qual fato>

## Hipóteses pendentes

| Hipótese | Prioridade | Como validar |
|---|---|---|
| `[HIPÓTESE]` … | P0/P1/P2 | `[VALIDAÇÃO NECESSÁRIA]` <comando, experimento ou leitura> |

## Experimentos pendentes

| Pergunta | Prioridade | Documento |
|---|---|---|
| | | `docs/experiments/<arquivo>.md` |

---

## Riscos abertos

| Risco | Tipo | Severidade | Mitigação conhecida |
|---|---|---|---|
| `[RISCO]` … | técnico / compatibilidade / performance / versão | baixa / média / alta | |

## Decisões tomadas

| Decisão | Data | Degrau da escada | ADR |
|---|---|---|---|
| `[DECISÃO]` … | AAAA-MM-DD | <1–11> | `docs/decisions/<arquivo>.md` |

---

## Próxima investigação recomendada

1. **P0** — <o que, e por que bloqueia>
2. **P1** — <o que>
3. **P2** — <o que>

## Pesquisa futura (`FUTURE RESEARCH`)

> Coisas que foram consideradas e **deliberadamente adiadas**. Registrar evita
> que a próxima sessão gaste tempo numa trilha já julgada irrelevante.

- <assunto> — adiado porque <motivo>

---

## Notas de revalidação

> Preencher quando o código ou a versão mudarem sob a pesquisa.

| O que mudou | Quando | Conclusões afetadas |
|---|---|---|
| | | |
