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
