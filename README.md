# Village Colony

Mod Fabric para Minecraft 1.21.1 que transforma vilas Vanilla em colônias
autônomas. Os aldeões recebem profissões, usam baús reais, produzem recursos,
plantam, mineram e constroem estruturas do próprio jogo sem exigir menu ou
mod no cliente.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.3.1%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)
[![CI](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml)

Download: na [página de versões](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases)
do GitHub — o JAR saiu do repositório (ADR-036, item 26). Cada versão traz o
SHA-256 do JAR na descrição.

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
- Põe o aldeão ocioso para recolher do chão só o item que falta à obra aberta
  (Regra 48) e mostra no `/vc log` quem espera o quê há mais de dois minutos.
- Regras de profissão por mundo, editáveis pelo [Mod Menu](https://modrinth.com/mod/modmenu)
  (opcional): ativar ou desativar a profissão, limite de trabalhadores, raio de
  busca do lenhador, fazendeiro e pastor, e ordem de contratação (ADR-030).
- Com o mod no cliente, mostra a profissão sobre o aldeão e o progresso da
  obra, com o primeiro material que falta; o cliente Vanilla continua vendo as
  placas.

O mod é alpha. A bateria automática está verde (unitários, GameTests e
mutação), mas vários fluxos só se confirmam em playtest num save, e as
entregas desde 2026-09-24 ainda não foram vistas em jogo. O que mudou em cada
publicação está no [`CHANGELOG.md`](CHANGELOG.md); a avaliação técnica mais
recente, em
[`docs/technical/avaliacao/`](docs/technical/avaliacao/).

## Profissões e funções

Estas são as oito funções operacionais atuais (revistas em 2026-09-30). `BREEDER` foi encerrado;
saves antigos com esse código são carregados como `SHEPHERD`.
`MANUFACTURER` não existe no código atual; documentos que usam esse nome estão
desatualizados.

| Código | Nome | Ferramenta inicial | Responsabilidade | Executor |
|---|---|---|---|---|
| `MINER` | Mineiro | Picareta de ferro | Abre e amplia a mina, coleta pedra e minério; guarda até 256 de cada tipo que a tarefa não pediu | `MinerWork` |
| `LUMBERJACK` | Lenhador | Machado de ferro | Derruba árvores, replanta e mantém o viveiro da borda | `LumberjackWork` |
| `MASON` | Pedreiro, equivalente ao ferreiro do catálogo | Nenhuma | Produz alvenaria e peças de pedra exigidas pelas obras | `CraftingWork` |
| `SMELTER` | Fundidor | Pá de ferro com Silk Touch | Funde materiais e coleta areia, relva, cacto e argila sob demanda | `SmelterWork` |
| `CARPENTER` | Carpinteiro | Nenhuma | Artesão geral: madeira e toda peça que não é alvenaria (vidraça, tear, cama, lampião, tocha) | `CraftingWork` |
| `FARMER` | Agricultor/Fazendeiro | Enxada de ferro | Mantém lavouras, cava terra para obra e faz pão do trigo acima da reserva | `FarmerWork` |
| `SHEPHERD` | Pastor | Tesoura | Tosquia e faz o rebanho procriar com trigo da colônia (até 12 ovelhas) | `ShepherdWork` |
| `BUILDER` | Construtor | Nenhuma | Reserva lotes, repara e assenta estruturas | `BuilderWork` |

Compatibilidade: `BREEDER` é convertido para `SHEPHERD` ao ler saves antigos.
O agricultor continua profissão completa, sem cama ou baú fundacional dentro
da `BigHouseMOD`. O carpinteiro é titular da fundação desde 2026-09-30.

Regras transversais de 2026-09-30:

- Cada bloco é quebrado com a ferramenta de ferro certa para ele
  (`ActionTool`); a da mão fica quando é tão boa ou melhor.
- A contratação atende primeiro a profissão de que a obra depende agora
  (`ProfessionDemand`), depois a ordem fixa.
- Aldeão com profissão do mod não recebe profissão Vanilla (ADR-029).
- A regra "a colônia não cria recurso" foi retirada (ADR-028): corante,
  linha, pó de osso e drops de inimigo e animal aparecem no baú quando a
  receita do artesão pede; bloco sem item é montado no local; a **lava nunca**
  é assentada.

## Regras do mod

### Fundação e aldeões

1. Toda vila adotada cria uma `BigHouseMOD` uma única vez.
2. A `BigHouseMOD` é uma cópia editada da big house Vanilla e não altera a
   estrutura Vanilla original.
3. A casa contém sete camas e sete baús para `MINER`, `LUMBERJACK`,
   `CARPENTER`, `MASON`, `SMELTER`, `SHEPHERD` e `BUILDER`. Os do
   carpinteiro ficam à direita da porta, junto à parede, com o corredor
   livre até a escada.
4. O agricultor continua existindo e trabalhando normalmente, mas não recebe
   cama ou baú reservados dentro da `BigHouseMOD`.
5. Cada titular recebe cama `HOME` e um baú próprio dentro da casa.
6. A `BigHouseMOD` não entra no catálogo de casas ou oficinas profissionais.

### Profissões, recursos e rotina

1. O mundo é a fonte da verdade; os baús são inventários reais.
2. A colônia só cria demanda por material quando há trabalho que o utiliza.
3. As receitas são consultadas do jogo e não duplicadas em uma tabela paralela.
4. A melhor ferramenta disponível pode substituir a ferramenta inicial.
5. O mineiro não cava estruturas Vanilla, construções da colônia ou blocos
   protegidos e, no deserto, reavalia a frente após areia ou cascalho cair.
6. O mineiro coleta os blocos quebrados até 256 de cada tipo no próprio baú;
   passado o teto, o tipo que a tarefa não pediu deixa de ser guardado.
7. A entrada de mina rejeita água próxima e procura uma posição seca, distante,
   acessível e preferencialmente voltada para terreno alto.
8. O lenhador mantém o viveiro: até dez árvores da madeira do bioma, com muda
   sobre terra enraizada no anel mais distante acessível.
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
11. Uma Tocha das Almas, inclusive a versão de parede, dentro do volume de
    uma obra profissional aberta cancela a reserva/projeto e as tarefas `BUILD`.
    Uma construção parcial sai do registro, mas os blocos já colocados ficam no
    mundo; a tocha também fica. A fundação `BigHouseMOD` é ignorada.
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
mod; clientes que entram em um servidor dedicado não precisam instalá-lo. No
cliente, o mod liga as sobreposições de profissão e de obra, e o Mod Menu
(opcional) abre a tela de regras de profissão — mudar regras exige operador
nível 2.
Use um mundo de teste: o mod corta árvores, minera e coloca blocos no mundo.

## Desenvolvimento e verificação

```powershell
./gradlew.bat test
./gradlew.bat build
./gradlew.bat runGametest
./gradlew.bat pitest
./gradlew.bat javadoc
python -m unittest discover -s tests
```

O build exige Java 21 (`JAVA_HOME` apontando para um JDK 21).

Cobertura: `test` gera `build/reports/jacoco/test/html` (unitários) e
`runGametest` gera `build/reports/jacoco/gametest/html` (bateria de jogo, que
é quem exercita a camada `fabric`).

Verificação de 2026-10-03 (corrigíveis sem jogo):

- 1295 testes unitários: aprovados (`test --rerun-tasks`).
- 91 testes Python: aprovados.
- 575 GameTests: todos aprovados, em duas rodadas seguidas com `--rerun-tasks`.
- `./gradlew build`: passa.

A mutação (PIT, pacote `core`) e o `javadoc` não foram rodados nesta
verificação; a última medida é a de 2026-09-25: 1151 de 1312 mutações mortas
(87,7%), força de teste 95%.

## Estado e nota da auditoria

A nota abaixo é da auditoria de 2026-09-21 e ficou como registro. A GameTest
que falhava então foi corrigida, e a avaliação posterior está em
[`docs/technical/avaliacao/`](docs/technical/avaliacao/).

Nota técnica global daquela varredura: **7,0/10**.

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
[`docs/archive/technical/Project-Audit-2026-09-21.md`](docs/archive/technical/Project-Audit-2026-09-21.md)
e a fila viva em [`TODO.md`](TODO.md).

## Documentos de entrada

- [`CLAUDE.md`](CLAUDE.md): regras para trabalhar no repositório.
- [`STATE.md`](STATE.md): estado vivo e pendências de playtest.
- [`TODO.md`](TODO.md): backlog canônico.
- [`CHANGELOG.md`](CHANGELOG.md): o que mudou em cada publicação.
- [`docs/decisions/`](docs/decisions/): decisões arquiteturais.
- [`docs/behavioral-tests/`](docs/behavioral-tests/): estratégia e falhas de
  GameTest.
- [`docs/archive/technical/Project-Audit-2026-09-21.md`](docs/archive/technical/Project-Audit-2026-09-21.md):
  auditoria desta varredura.

Licença MIT. Feito para Minecraft 1.21.1 com Fabric.
