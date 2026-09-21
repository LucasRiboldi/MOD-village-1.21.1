# Village Colony

Mod Fabric para Minecraft 1.21.1 que transforma vilas Vanilla em colônias
autônomas. Os aldeões recebem profissões, usam baús reais, produzem recursos,
plantam, mineram e constroem estruturas do próprio jogo sem exigir menu ou
mod no cliente.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.3.0%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)
[![CI](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml)

Download: [village-colony-0.3.0.jar](downloads/village-colony-0.3.0.jar?raw=1)

SHA-256 do JAR publicado nesta árvore:
`983638B6FE18D5814B4BC5A5192FBA23A0214FE31B59B6BA642B4A9B773DF81B`.

## O que o mod faz

- Adota vilas Vanilla e mantém o estado no mundo, nos aldeões, baús e blocos.
- Atribui profissões e garante camas e baús distintos para a fundação.
- Usa receitas e blocos Vanilla em vez de inventário ou economia virtual.
- Busca madeira, pedra, lã, comida, areia, terra, relva e minérios quando há
  demanda real.
- Planta e mantém até dez árvores da madeira do bioma, fora do centro da vila,
  usando muda e terra enraizada.
- Cria uma `BigHouseMOD` automaticamente para a fundação de cada vila.
- Escolhe estruturas Vanilla permitidas pelo bioma e constrói uma por vez.
- Revarre construções incompletas ciclicamente antes de abrir uma obra nova.
- Cancela uma obra profissional quando o jogador coloca uma Tocha das Almas
  dentro dela, liberando a fila; a `BigHouseMOD` é protegida dessa regra.
- Evita sobreposição com estruturas existentes, projetos pendentes e blocos
  físicos dentro da área vertical protegida.

O mod é alpha. A rede de testes é ampla, mas a rodada atual ainda tem uma
falha obrigatória de GameTest e alguns fluxos dependem de playtest em um save.
O diagnóstico completo está em
[`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md).

## Profissões e funções

Estas são as oito funções operacionais atuais. `SHEPHERD` continua no enum
apenas para compatibilidade com saves antigos e é normalizado para `BREEDER`.
`MANUFACTURER` não existe no código atual; documentos que usam esse nome estão
desatualizados.

| Código | Nome | Ferramenta inicial | Responsabilidade | Executor |
|---|---|---|---|---|
| `MINER` | Mineiro | Picareta de ferro | Abre e amplia a mina, coleta pedra, areia e minério | `MinerWork` |
| `LUMBERJACK` | Lenhador | Machado de ferro | Derruba árvores, coleta madeira e participa do viveiro | `LumberjackWork` |
| `MASON` | Pedreiro, equivalente ao ferreiro do catálogo | Nenhuma | Produz alvenaria e peças de pedra exigidas pelas obras | `CraftingWork` |
| `SMELTER` | Fundidor | Pá de ferro com Silk Touch | Funde materiais e coleta areia, terra ou relva sob demanda | `SmelterWork` |
| `CARPENTER` | Carpinteiro | Nenhuma | Processa madeira, tochas, vidraças e peças derivadas | `CraftingWork` |
| `FARMER` | Agricultor/Fazendeiro | Enxada de ferro | Mantém lavouras e planta árvores do viveiro | `FarmerWork` |
| `BREEDER` | Criador/Pastor | Tesoura | Coleta lã e mantém a cadeia de materiais de origem animal | `ShepherdWork` |
| `BUILDER` | Construtor | Nenhuma | Reserva lotes, repara e assenta estruturas | `BuilderWork` |

Compatibilidade: `SHEPHERD` é o nome legado de `BREEDER`. Agricultor e
carpinteiro continuam profissões completas: estão no registro, podem ser
atribuídos, recebem tarefas e participam do crescimento normal. A exceção é
apenas física: não têm cama ou baú fundacional dentro da `BigHouseMOD`.

## Regras do mod

### Fundação e aldeões

1. Toda vila adotada cria uma `BigHouseMOD` uma única vez.
2. A `BigHouseMOD` é uma cópia editada da big house Vanilla e não altera a
   estrutura Vanilla original.
3. A casa contém somente seis camas e seis baús para `MINER`, `LUMBERJACK`,
   `MASON`, `SMELTER`, `BREEDER` e `BUILDER`.
4. Agricultor e carpinteiro continuam existindo e trabalhando normalmente, mas
   não recebem cama ou baú reservados dentro da `BigHouseMOD`; esses dois
   conjuntos foram omitidos somente para liberar a porta e o acesso à escada.
5. Cada titular recebe cama `HOME` e um baú próprio dentro da casa.
6. A `BigHouseMOD` não entra no catálogo de casas ou oficinas profissionais.

### Profissões, recursos e rotina

1. O mundo é a fonte da verdade; os baús são inventários reais.
2. A colônia só cria demanda por material quando há trabalho que o utiliza.
3. As receitas são consultadas do jogo e não duplicadas em uma tabela paralela.
4. A melhor ferramenta disponível pode substituir a ferramenta inicial.
5. O mineiro não cava estruturas Vanilla, construções da colônia ou blocos
   protegidos e, no deserto, reavalia a frente após areia ou cascalho cair.
6. O mineiro coleta todos os blocos quebrados, inclusive o excedente levado ao
   baú ou deixado como overflow no chão.
7. A entrada de mina rejeita água próxima e procura uma posição seca, distante,
   acessível e preferencialmente voltada para terreno alto.
8. O agricultor e o lenhador compartilham o viveiro: até dez árvores da madeira
   do bioma, com muda sobre terra enraizada no anel mais distante acessível.
9. O viveiro não cresce indefinidamente e árvores naturais não contam como
   árvores marcadas da vila.
10. O trabalho ocorre durante o expediente; noite e retorno ao alojamento são
    respeitados.

### Construção e seleção de lotes

1. Somente estruturas Vanilla explicitamente permitidas por bioma podem ser
   escolhidas pelas profissões.
2. A `BigHouseMOD` nasce pela fundação e nunca é escolhida como obra comum.
3. A sequência de crescimento intercala casa e obra não residencial; o próximo
   tipo não residencial deve ser diferente do anterior.
4. O primeiro projeto profissional é uma casa.
5. O lote precisa ser acessível, apoiado em terreno natural válido e livre de
   estruturas, blocos existentes e projetos pendentes.
6. A verificação cobre a pegada inteira, todas as colunas acima do nível-base e
   uma janela absoluta de 25 blocos acima de toda a área.
7. Uma construção nunca pode nascer sobre uma casa, rua protegida, fundação,
   bloco elevado ou outra obra registrada.
8. O construtor conclui o último bloco como `COMPLETED`; a obra não volta para
   a fila ao terminar.
9. Depois de concluir, abandonar ou liberar uma obra, o planejador tenta uma
   reparação cíclica de uma construção incompleta antes de abrir outra.
10. Uma tentativa sem progresso cede a vez para a fila avançar, mas permanece
    elegível para uma varredura posterior.
11. Uma Tocha das Almas dentro de uma obra profissional cancela zona, projeto,
    tarefas e registro parcial. A fundação `BigHouseMOD` é ignorada.
12. Blocos colocados pelo jogador e estruturas da vila permanecem protegidos.

## Construções e biomas

O catálogo profissional está em
`src/main/java/com/villagecolony/fabric/work/HousePlans.java` e é validado
contra os nomes reais do Minecraft 1.21.1. As famílias permitidas são:

- Planície: casas, oficinas, fazendas, currais, estábulos e lampião.
- Deserto: casas, oficinas, fazendas, currais e lampião.
- Savana: casas, oficinas, fazendas, currais e lampião.
- Taiga: casas, oficinas, fazendas, curral e lampião.
- Tundra nevada: casas, oficinas, fazendas, currais e lampião.

O detalhe dos IDs e a regra da estrutura própria estão em
[`src/main/resources/data/villagecolony/structure/houses/README.md`](src/main/resources/data/villagecolony/structure/houses/README.md).

## Instalação

Requisitos: Minecraft Java 1.21.1, Fabric Loader compatível, Fabric API e
Java 21. Coloque o JAR e a Fabric API na pasta `mods`. O servidor precisa do
mod; clientes que entram em um servidor dedicado não precisam instalá-lo.
Use um mundo de teste: o mod corta árvores, minera e coloca blocos no mundo.

## Desenvolvimento e verificação

```powershell
./gradlew.bat test
./gradlew.bat build
./gradlew.bat runGametest
python -m unittest discover -s tests
```

Verificação de 2026-09-21:

- 960 testes unitários em 103 suítes: aprovados.
- 74 testes Python: aprovados.
- 397 GameTests: 396 passaram e 1 falhou.
- Falha atual: `FarmPlanGameTest.thenextturnafterahouseisnonresidential`.

## Estado e nota da auditoria

Nota técnica global desta varredura: **7,0/10**.

Arquitetura e isolamento: **8,0/10**. A separação `core`/`fabric` é protegida
por teste e o grafo atualizado não encontrou ciclos de importação.

Funcionalidade: **7,0/10**. O ciclo de vila, fundação, mineração, viveiro,
seleção segura de lotes e reparo existem, mas a alternância de obras ainda tem
uma regressão reproduzível.

Testes: **7,0/10**. Há 960 unitários e 397 GameTests, porém a bateria não está
verde e existe uma instabilidade histórica de spawn do fundidor.

Manutenção e documentação: **6,0/10**. Os documentos principais foram
alinhados nesta auditoria, mas há arquivos Java grandes, documentos históricos
com nomenclatura antiga e uma política de versões que merece decisão.

Release: **7,0/10**. CI, build e artefato estão presentes; o CI não deve ser
considerado liberável enquanto o GameTest obrigatório falhar.

Consulte a lista priorizada de erros, melhorias, inconsistências e conflitos em
[`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md)
e a fila viva em [`TODO.md`](TODO.md).

## Documentos de entrada

- [`CLAUDE.md`](CLAUDE.md): regras para trabalhar no repositório.
- [`STATE.md`](STATE.md): estado vivo e pendências de playtest.
- [`TODO.md`](TODO.md): backlog canônico.
- [`docs/decisions/`](docs/decisions/): decisões arquiteturais.
- [`docs/behavioral-tests/`](docs/behavioral-tests/): estratégia e falhas de
  GameTest.
- [`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md):
  auditoria desta varredura.

Licença MIT. Feito para Minecraft 1.21.1 com Fabric.
