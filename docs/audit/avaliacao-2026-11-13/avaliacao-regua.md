# Avaliação de qualidade do Village Colony — régua comum (2026-11-13)

Pedido do autor: avaliação completa, régua ISO/IEC 25010:2023, meta "1.0 pública" (Modrinth e CurseForge, estável, sem perda
de save, servidor dedicado e multijogador, guia do jogador, declaração de uso de IA). Snapshot: `main` em `ac2f1977` (0.4.74).

## Regras para as sessões de levantamento

- **Só diagnóstico.** Não altere código, testes, regras nem documentos do repositório. Não commite, não faça push.
- Escreva o resultado num arquivo seu nesta pasta: `avaliacao-<bloco>.md`.
- **Nunca rode `./gradlew.bat --stop`** (derruba o Gradle das outras sessões). Minecraft deve estar fechado para GameTest.
- Ambiente: `export JAVA_HOME=/c/Users/lucas/.jdks/jdk-21.0.12+8`. Comandos longos em segundo plano.
- Não leia documentos históricos inteiros (`docs/archive/`): use `rg`. Comece por `HANDOFF.md`, `STATE.md` (60 linhas), `CLAUDE.md`.
- Carregue a skill do projeto que couber ao seu bloco (lista no brief).

## Formato de cada dimensão

```
### D<n> — <nome> (ISO 25010: <característica>)
Nota proposta: X/10 — <uma frase>
Evidências (3 a 8), cada uma com marca e fonte:
- CONFIRMADO | PROVÁVEL | SUSPEITO | NÃO CONFIRMADO — <fato> (`arquivo:linha`, comando e saída, ou relatório)
Riscos para a 1.0: <lista curta>
O que falta para 7/10 e para 9/10: <itens concretos, cada um com tamanho P/M/G>
Bloqueia a 1.0? sim/não, por quê
```

Marcas: CONFIRMADO = visto no código, no git ou rodando agora; PROVÁVEL = mecanismo visto, efeito não reproduzido;
SUSPEITO = indício, falta medir; NÃO CONFIRMADO = não verificado. Diga sempre se algo foi **rodado de verdade** ou só lido.

## Faixas de nota

0–2 não existe · 3–4 existe e é frágil · 5–6 funciona com ressalvas · 7–8 sólido · 9–10 exemplar (padrão de mod publicado de
referência).

## Dimensões e blocos

| Bloco (sessão) | Dimensões |
|---|---|
| A — arquitetura e código | D1 Arquitetura e modularidade (manutenibilidade) · D6 Qualidade do código (manutenibilidade) |
| B — testes e análise estática | D5 Testes (confiabilidade/manutenibilidade) · D10 Ferramentas, build e CI |
| C — save, compatibilidade e desempenho | D3 Confiabilidade e save · D4 Desempenho · D7 Compatibilidade (servidor dedicado, multijogador, outros mods, mixins) · D9 Segurança |
| D — regras, documentação e processo | D2 Lógica e regras de negócio (adequação funcional) · D8 Experiência do jogador (usabilidade) · D11 Documentação e organização · D12 Processo, Git e prontidão para publicar |

No fim do arquivo: "Top 5 problemas do bloco" por gravidade, e "O que eu não consegui verificar".
