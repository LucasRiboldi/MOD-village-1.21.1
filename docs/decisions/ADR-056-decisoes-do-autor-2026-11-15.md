# ADR-056 — Decisões do autor de 15-11 (notas dos jogos da 0.4.79 à 0.4.86)

**Estado:** aceita pelo autor em 2026-11-15. Cada item vira regra própria em `docs/RULES.md` quando é implementado,
com o teste que a prova.

## Contexto

O placar das notas do autor nos quatro últimos jogos (0.4.79, 0.4.84, 0.4.85 e 0.4.86) somou 33 pontos: 4 resolvidos e
vistos em jogo, 10 corrigidos sem jogo e 19 abertos. Os abertos foram juntados em nove grupos, cada um com opções; esta
ADR registra a escolha.

## Decisões

| Grupo | Notas de origem | Escolha | Estado |
|---|---|---|---|
| G1 | 0.4.84 nº 3; 0.4.86 nº 6 e 7 | O grafo aceita qualquer tábua como chão de rua, e na geração a rua de tábua vira `dirt_path` | aberto |
| G2 | 0.4.85 nº 3; 0.4.86 nº 5 e 8 | A obra entrega a plantação grande semeada; o fazendeiro ara e semeia com sementes variadas, e a semente surge quando nenhum baú tem (exceção declarada `SEED_FOR_THE_FARM`); no máximo 2 fazendeiros na plantação grande | feito na Regra 289 |
| G3 | 0.4.84 nº 1; 0.4.85 nº 5; 0.4.86 nº 3 | Corrigir a proteção que faz o `VC_SMOOTH` recusar com `protected:air` | aberto |
| G4 | 0.4.85 nº 6 e 7; 0.4.86 nº 2 | Arco da mina com disco de pedra de raio 6, área limpa sem nada acima, baú ao pé e nó de entrada logo à frente | aberto |
| G5 | 0.4.84 nº 7 e 8 | O nó de entrada sai do chão em frente à porta ou ao portão, na altura do piso, seja qual for o material | aberto |
| G6 | 0.4.85 nº 1 e 2 | No ócio, os aldeões retiram da vila o bloco sem vizinhos e a tora isolada | aberto |
| G7 | 0.4.85 nº 4 | Quem corta árvore, por qualquer motivo do mod, tira todos os troncos; todo aldeão quebra a folha que tranca o caminho dele | aberto |
| G8 | 0.4.85 nº 8 | Casa do mineiro em modelo medieval novo (telhado, decoração, luz externa) | aberto |
| G9 | 0.4.84 nº 6 | Regra de processo: a cada leitura de log, analisar a atividade dos aldeões e propor melhorias para que fiquem mais ativos | em vigor |

## Consequências

- A semente que surge é exceção nova de recurso sem origem física, ao lado das da ADR-028; ela só acontece quando
  nenhum baú da vila tem semente e a roça do fazendeiro tem canteiro vazio, e sai no `VC_SUPPLY`.
- A ordem de execução segue a ociosidade medida: G2, G7, G1 e G5, G3, G4, G6, G8.
