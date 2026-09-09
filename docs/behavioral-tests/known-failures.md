# Falhas conhecidas

Falha conhecida é falha **medida e não corrigida**. Nenhuma entra aqui sem
número de execuções e evidência; nenhuma sai daqui sem correção verificada.

---

## KF-001 — `MinerGameTest.aFrozenMinerGivesUpLongBeforeTheStallGuard` é instável

**Classificação:** P4 (comportamento inconsistente) — mas com efeito P0 sobre a
rede de segurança: **a bateria não é confiavelmente verde**, e isso corrói toda
afirmação de "269/269 passaram".

**Medido em 2026-09-09**, commit `3e367d5`, mesma máquina, mesma sessão:

| lote de execuções | resultado |
|---|---|
| 9 execuções | 1 falha |
| 3 execuções | 2 falhas |
| **total 12** | **3 falhas (~25%)** |

Sempre o mesmo teste. Nenhum outro falhou em nenhuma das 12.

**O que o teste promete.** Um mineiro emparedado por seis faces deve ter a tarefa
devolvida pelo guarda de imobilidade (`STILL_LIMIT` = 300 tiques) muito antes do
guarda de travamento (`STALL_LIMIT` = 2400). Ele afirma no tique 360, com
`tickLimit` 400.

**A evidência do mecanismo.** Duas execuções lado a lado, mesmo aldeão, mesmas
paredes — o que muda é a **velocidade com que o contador anda**:

```
bateria que passou:   still 280/300
bateria que falhou:   still  99/300
```

Não é o guarda que oscila. É **quando ele começa a contar**: o contador só anda
quando o mineiro tem alvo, e ganhar alvo custa uma busca. A busca é um
**orçamento global do servidor** — `MinerWork.SEARCHES_PER_TICK` é 1 para todas
as colônias juntas — e a bateria monta 18 cenários de mineiro. Sessenta tiques de
margem contra uma fila global é pouco.

**Hipótese testada e REFUTADA.** Aumentar a folga (asserção 360 → 700,
`tickLimit` 400 → 800) **piorou**: 2 falhas em 6. A razão provável é que 700
passa da fronteira do ciclo da colônia (600 tiques), e o ciclo **re-reserva** a
tarefa que o guarda havia liberado — a asserção então encontra `RESERVED` de
novo. A folga não pode crescer para além do ciclo, e abaixo dele não há espaço
suficiente. Revertido.

**Por que não foi corrigido.** As duas saídas óbvias são ruins: aumentar a folga
esbarra no ciclo (medido acima), e afrouxar a asserção mascararia o defeito, que
é o que a Regra 33 proíbe. A correção real provavelmente exige desacoplar o teste
do orçamento global — dar alvo ao mineiro sem passar pela busca disputada, ou
isolar o cenário num lote sem outros mineiros. Nenhuma das duas foi tentada.

**O que NÃO fazer:** marcar o teste como ignorado, aumentar `tickLimit` sem
entender, ou reexecutar a bateria até dar verde e chamar isso de aprovação.
