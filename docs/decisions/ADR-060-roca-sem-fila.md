# ADR-060 — O fazendeiro trabalha a roça, e não a fila

**Estado:** aceita pelo autor em 2026-11-19 (pacote B), sobre a pesquisa
`docs/research/2026-11-19-logica-do-fazendeiro.md`. Regras 332 e 333. Emenda as Regras 168 (os cultivos do curral
grande) e 267 (quando a plantação grande é pedida).

## Contexto

No jogo da 0.4.93 em planície, o fazendeiro trabalhou 76% nos primeiros 5 minutos. Depois ficou entre 0% e 2% em 19 de
20 janelas, com 50% ocioso e 16% bloqueado, e a plantação grande nunca foi pedida. Duas causas:
- **O dia dele dependia da fila.** O fazendeiro só trabalhava com a tarefa `COLLECT_FOOD` aberta, e ela abre pela falta
  no estoque. Na vila madura, sem falta, nada rodava: arar, compostar, pó de osso e o pedido da plantação grande
  ficavam dentro do passo da tarefa.
- **O pasto do curral virava roça.** O curral grande tem 12 cultivos (Regra 168), e o chão dele fica fora do caminho de
  quem não é pastor (Regra 122). O fazendeiro mirava esse trigo, não chegava (29 desistências, até 5.000 tiques cada)
  e ficava cercado (17 vezes).

## Decisão do autor

1. **O pasto do curral não é roça** (Regra 332). A lavoura no chão de um curral fica fora da busca do fazendeiro
   (`CropPatch`, por `PenFloors.isPenFloor`) e sai da memória de terra arada conhecida.
2. **A ronda da roça** (Regra 333). Sem tarefa de comida na fila, o fazendeiro livre faz a ronda da roça dele, no
   expediente, com o mesmo trabalho de sempre: colhe o maduro e guarda, ara e semeia o canteiro, composta e põe pó de
   osso. A ronda é uma tarefa só dele, fora da fila (`FarmRound`), de 64 colheitas. Ela acaba quando:
   - chega a tarefa da fila, que toma o lugar dela;
   - termina o expediente;
   - não há nada a fazer. Aí a vila pode pedir a plantação grande (Regras 241 e 267), e ele descansa 1 minuto antes
     da próxima ronda.
   A desistência na ronda não pune o fazendeiro (`WorkerStrikes`), e o `WorkTime` conta a ronda como trabalho.

## Consequências

- O fazendeiro deixa de ficar parado com a roça por cuidar. O estoque cheio muda só o destino da colheita, que segue
  as regras de sempre (baú, ceia da Regra 217, ração do rebanho).
- A plantação grande passa a ser pedida também na vila sem falta de comida, quando a roça não tem mais o que fazer.
- **Não decidido aqui:** fardo de feno do excedente de trigo e as culturas novas na borda (opção C do autor).
