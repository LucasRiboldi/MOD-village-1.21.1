# Modelos do Claude Code por atividade

Revisado em 2026-11-04, conforme o seletor do Claude Code na interface do autor:
Opus 5.5, Sonnet 5.5, Fable 5.1 (requer créditos de uso), Haiku 5.5; esforço Baixo,
Médio, Alto, Extra e Máx (3,5× ou mais de uso). Recomendações iniciais, não resultados
de benchmark do projeto. A tabela equivalente do Codex está em `Modelos-Codex.md`.

| Atividade | Modelo | Esforço |
|---|---|---|
| Consulta simples, localizar arquivo, status Git | Haiku 5.5 | Baixo |
| Executar comando conhecido e resumir relatório | Haiku 5.5 | Médio |
| Triagem de logs/relatórios | Sonnet 5.5 | Médio |
| Analisar tela e defeito visual | Sonnet 5.5 | Alto |
| Correção isolada com causa conhecida | Sonnet 5.5 | Médio; Alto se necessário |
| Diagnóstico em várias classes de um subsistema | Opus 5.5 | Alto |
| Implementação e revisão entre subsistemas | Opus 5.5 | Alto |
| Travamento intermitente ou divergência entre jogo e log | Opus 5.5 | Alto |
| Impasse arquitetural, migração difícil ou diagnóstico inconclusivo | Opus 5.5 | Extra |
| Documentação de alterações conhecidas | Haiku 5.5 ou Sonnet 5.5 | Médio |
| Commit/push/JAR por procedimento definido | Haiku 5.5 | Médio |

O conjunto atual de caminhos, terreno, mina e profissões usa **Opus 5.5, Alto**.
Fable 5.1 só com créditos de uso e motivo concreto (impasse que Opus Extra não resolveu).
Esforço Máx e Ultracode exigem ganho observado que compense tempo e consumo.

A interface confirma disponibilidade; não confirma preço, limite ou consumo do plano.
Antes de cada etapa, anunciar atividade, modelo e esforço; respeitar a confirmação
manual do autor conforme `CLAUDE.md` §0.2.2. Não afirmar o modelo ou o esforço
selecionado sem evidência. Não trocar modelo automaticamente.
