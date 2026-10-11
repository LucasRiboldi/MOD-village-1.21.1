# Avaliações técnicas

A forma de avaliar está em [METODOLOGIA.md](METODOLOGIA.md). Cada avaliação é
uma pasta `<data>-<commit>/` com três arquivos:
- `metricas.json`, gerado pelo coletor;
- `scorecard.md`, gerado pelo coletor;
- `RELATORIO.md`, escrito pelo avaliador.

Para comparar duas avaliações:

```text
python scripts/assess/assess_project.py --compare <antes>/metricas.json <depois>/metricas.json
```

| Data | Commit | Régua | Média | Conceito | Critério mais fraco | Relatório |
|---|---|---|---|---|---|---|
| 2026-09-24 | `17613fa` | v1 | 3,21 | B | C13 Desempenho (1) | [RELATORIO](2026-09-24-17613fa/RELATORIO.md) |
| 2026-10-10 | `b290ed10` | v1 | 3,36 (3,14 corrigida, AUD-07) | B | C13 Desempenho (1) | [RELATORIO](2026-10-11-b290ed10/RELATORIO.md) |

## Auditoria técnica atual (2026-10-10)

Além da régua, a auditoria de 10-10 avaliou 12 dimensões com evidência
`arquivo:linha`. Os arquivos na raiz desta pasta são **da auditoria mais
recente**: a próxima os substitui, e a cópia de cada data fica em
`docs/archive/technical/Project-Audit-<data>.md`.

- [EXECUTIVE-SUMMARY.md](EXECUTIVE-SUMMARY.md): uma página, notas, riscos e ganhos rápidos.
- [ACTION-QUEUE.md](ACTION-QUEUE.md): fila AUD-01…AUD-25 com teste de aceite; espelhada no `TODO.md`.
- `D01`…`D12`: uma dimensão por arquivo.
- [agent-setup.md](agent-setup.md): skills usadas e de onde vieram.
- Scripts reprodutíveis: [`scripts/audit/`](../../../scripts/audit/README.md).
