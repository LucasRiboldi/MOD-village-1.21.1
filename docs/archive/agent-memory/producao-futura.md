---
name: producao-futura
description: "Produção futura guardada a pedido do autor (22-10): configuração pronta para implementar, resto do estudo v2 por prioridade, sistemas grandes sem decisão — fonte no TODO.md"
metadata:
  type: project
---

O autor pediu em 22-10: "guarde essas informações para produção futura". A fonte única é a seção **"Produção
futura"** do `TODO.md`, e o README tem um resumo com links.

**Pronto para implementar: a configuração** (R-153/154).
- O desenho e as decisões do autor estão em `docs/research/2026-10-22-desenho-configuracao.md` §8.
- Por mundo, com sobreposição por colônia (colônia → mundo → padrão).
- Arquivo JSON em `saves/<mundo>/villagecolony/config.json`, mais o comando `/vc config list|get|set|reset|reload`
  para operador nível 2, e a linha `VC_CONFIG` a cada mudança.
- A F1 é a tabela de 14 ajustes da §2, a menos que o autor diga outra coisa.

**Depois, por prioridade 🟡:**
- ausência invalidada por evento (R-021/027);
- contrato de tarefa (R-047/050);
- estados de disponibilidade (R-019);
- bateria de escassez (R-158);
- animal inacessível fora do plantel (R-107);
- teste do registro de cada suprimento.

**Sem decisão do autor 🟢:** área protegida (R-035), transporte físico (R-140–142), outros mods (R-026), adaptador
de versão.

**Why:** o autor quer retomar daqui sem refazer a avaliação.

**How to apply:** ao retomar, comece pela configuração (F1). Qualquer R-xxx do v2 se mapeia pela matriz em
`docs/research/2026-10-22-avaliacao-regras-v2.md`.

Relacionado: [[avaliacao-regras-v2]], [[regra-74-cama-perto-do-trabalho]].
