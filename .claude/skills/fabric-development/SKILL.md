---
name: fabric-development
description: >-
  Desenvolvimento profissional de mods Minecraft Java Edition com Fabric —
  arquitetura, implementação, integração, compilação, testes e validação. Use
  sempre que for escrever, alterar, refatorar ou depurar código de mod Fabric:
  criar item/bloco/entidade/BlockEntity, registrar conteúdo, escrever Mixin,
  networking, persistência, datagen, resources, gametest, migração de versão,
  investigar lag ou crash, resolver bug que só aparece em multiplayer. Dispare
  também em pedidos diretos de implementação ("adiciona X ao mod", "cria um
  bloco que...", "faz o aldeão fazer Y", "meu mod está com lag", "não compila",
  "funciona no singleplayer mas quebra no servidor"). Trabalha em conjunto com
  minecraft-code-research, que investiga antes. Vale em português ou inglês.
---

# Fabric Development

Esta skill transforma conhecimento em mod funcionando. Ela não substitui a
pesquisa — assume que ela existe, e vai buscá-la.

```text
minecraft-code-research   →   fabric-development
   COMPREENDER                  ARQUITETAR
   INVESTIGAR                   IMPLEMENTAR
   VALIDAR                      INTEGRAR
   DOCUMENTAR                   TESTAR · VALIDAR
```

## O ciclo que ela impede

```text
pedido → cria classe → cria Mixin → "funciona" → próxima feature
```

O que falta aí não é teste. É **decidir onde a coisa mora** antes de escrevê-la.
Uma feature implementada como um punhado de classes soltas parece pronta e não
está integrada a nada: não sabe quando começa a existir, quem é dono do estado
dela, o que sobrevive ao restart e o que o cliente vê.

## O princípio

**Não implemente uma feature como um conjunto isolado de classes. Implemente
como parte de um sistema integrado ao ciclo de vida do Minecraft e do Fabric.**

Na prática, isso significa responder cinco perguntas antes de criar o primeiro
arquivo:

```text
1. QUAL PROBLEMA?
2. QUAL SISTEMA EXISTENTE já trata disso?
3. QUEM É O DONO DO ESTADO?
4. QUAL É O CICLO DE VIDA?
5. QUAL É A MENOR SOLUÇÃO CORRETA?
```

Só então: *qual código preciso escrever?*

## Passo 1 — A pesquisa já existe?

**Antes de implementar, procure.** Repesquisar o que já está documentado é
desperdício; implementar sem a pesquisa é adivinhação.

```bash
ls docs/research docs/knowledge docs/architecture docs/decisions docs/experiments 2>/dev/null
find . -name "research-status.md" -not -path "*/build/*"
grep -ril "<sistema que vou tocar>" docs/ 2>/dev/null | head
```

Responda:

```text
ESTE SISTEMA JÁ FOI PESQUISADO?
EXISTE DECISÃO ARQUITETURAL (ADR)?
EXISTE IMPLEMENTAÇÃO ANTERIOR NO PROJETO?
EXISTE HIPÓTESE NÃO VALIDADA que a implementação depende?
EXISTE RISCO CONHECIDO?
EXISTE EXPERIMENTO relacionado?
```

### Quando parar e pesquisar

Se faltar informação **crítica**, pause e acione `minecraft-code-research`:

```text
fabric-development → detecta incerteza → minecraft-code-research
                  → investiga → documenta → volta → implementa
```

Os seis gatilhos que obrigam a parada:

```text
COMPORTAMENTO VANILLA DESCONHECIDO
ALVO DE MIXIN NÃO VERIFICADO
DONO DO ESTADO (client/server) INDEFINIDO
ESTRATÉGIA DE PERSISTÊNCIA INDEFINIDA
API DA VERSÃO INCERTA
ARQUITETURA COM VÁRIAS OPÇÕES DE ALTO IMPACTO
```

O quinto é o mais comum e o mais barato de resolver: um `javap` responde em
segundos. Ver `references/migration.md` para o loop de verificação.

## Passo 2 — Classificar a tarefa

```text
FEATURE · SYSTEM · BUG · REFACTOR · PERFORMANCE · COMPATIBILITY ·
MIGRATION · EXPERIMENT · HOTFIX
```

E o impacto:

```text
SCOPE · DEPENDENCIES · SISTEMAS VANILLA · SISTEMAS FABRIC
CLIENT IMPACT · SERVER IMPACT · DATA · PERSISTÊNCIA · NETWORK
PRECISA MIXIN? · RISCO DE PERFORMANCE · RISCO DE COMPATIBILIDADE
```

Campo que ficar em "não sei" é candidato ao Passo 1.

## Os quatro modos

Escolha pelo tamanho real da mudança. Burocracia em tarefa pequena é desperdício;
sua ausência em tarefa grande é retrabalho.

| Modo | Quando | Roteiro |
|---|---|---|
| **SMALL** | item novo, texto, ajuste de config, correção óbvia | identificar → implementar → compilar → validar |
| **FEATURE** | funcionalidade normal | pesquisa → plano → arquitetura → implementar → testar → documentar |
| **SYSTEM** | sistema grande, decisão que dura | domínio → pesquisa → contratos → arquitetura → plano → implementação incremental → integração → testes → performance → documentação |
| **SURGERY** | Mixin crítico, persistência, networking, migração, compatibilidade | entender → pesquisar → **menor mudança possível** → compilar → testar → regressão → documentar risco |

Anuncie o modo. Mudar no meio é normal — SMALL que revela decisão de arquitetura
vira FEATURE. Diga que mudou e por quê.

O roteiro executável de cada um está em `workflows/`.

## Implementação incremental

**Nunca escreva um sistema inteiro antes de compilar.**

```text
✗  20 classes + 15 mixins + 10 registries + 5 packets → tentar compilar
✓  núcleo mínimo → compila → integra uma peça → compila → roda → valida
```

O motivo não é cerimônia: com cinquenta arquivos novos, o primeiro erro de
compilação pode estar em qualquer um, e os erros se mascaram. Com cinco, o
compilador aponta o lugar.

```bash
./gradlew build          # compila e roda testes de unidade
./gradlew runGametest    # sobe servidor headless, roda os testes de jogo
./gradlew runClient      # sessão de jogo
./gradlew runServer      # servidor dedicado ← onde bugs de side aparecem
```

Compile **cedo e com frequência**. É o feedback mais barato que existe.

## A menor arquitetura correta

Para toda feature, pergunte: **qual é a menor arquitetura que ainda está certa?**

Não crie `Manager`, `Service`, `Factory`, `Builder`, `Controller`, `Handler`,
`Processor` ou `Util` por reflexo. Abstração só se paga quando existe:

```text
REPETIÇÃO · VARIAÇÃO · EXTENSIBILIDADE REAL · TESTABILIDADE · SEPARAÇÃO DE DOMÍNIO
```

Um item novo não precisa de `ItemFactory`. Um sistema de trabalho com sete
profissões, biomas e persistência precisa de separação de domínio — e aí a
abstração é o que impede a classe de mil linhas.

**Os dois erros são simétricos.** Framework prematuro custa tanto quanto God
Class; escolha pelo tamanho real do problema, não pelo tamanho que ele pode ter
um dia. Ver `references/project-architecture.md`.

## Anatomia de uma feature

Analise toda feature por estas dimensões — e **corte as que não se aplicam**:

```text
FEATURE
├── DOMAIN         a regra, independente do Minecraft
├── DATA           o que ela lê
├── STATE          o que ela guarda, e quem é dono
├── LOGIC          a decisão
├── INTEGRATION    onde encosta no Vanilla/Fabric
├── EVENTS         o que dispara
├── PERSISTENCE    o que sobrevive
├── NETWORK        o que o cliente precisa saber
├── CLIENT         o que ele exibe
└── RESOURCES      lang, modelo, textura, loot, recipe, tag
```

Dimensão marcada como "não se aplica: <motivo>" é informação. Dimensão esquecida
é bug — e `RESOURCES` é a esquecida campeã: o código está certo, o bloco existe,
e aparece como cubo preto e rosa chamado `block.mymod.foo`.

## Regras que não se negociam

Estas custam caro quando violadas, e o custo aparece tarde.

**Servidor manda.** O cliente pede, o servidor valida e executa, o servidor
sincroniza. Nunca o contrário — e funcionar no singleplayer não é evidência de
nada, porque ali os dois lados compartilham memória.

**Registro no entrypoint, incondicional e determinístico.** Registro condicional
produz ids diferentes entre cliente e servidor e derruba a conexão.

**Nada de estado de mundo em `static`.** O processo abre outro save sem
reiniciar, e o estado vaza.

**Nada é salvo automaticamente.** Se não há par escrita/leitura, o campo some no
restart.

**Nenhuma exceção sua escapa de dentro de método Vanilla.** Capture, logue, e
degrade para comportamento Vanilla.

**Mixin é integração, não arquitetura.** Suba a escada de extensão só com
justificativa escrita.

## Roteamento

### Workflows — o passo a passo executável

| Tarefa | Workflow |
|---|---|
| Implementar uma feature | `workflows/feature-development.md` |
| Construir um sistema grande | `workflows/new-system.md` |
| Corrigir um bug | `workflows/bug-fix.md` |
| Refatorar | `workflows/refactor.md` |
| Escrever ou alterar um Mixin | `workflows/mixin-workflow.md` |
| Migrar de versão | `workflows/migration-workflow.md` |
| Investigar lag | `workflows/performance-workflow.md` |
| Validar antes de publicar | `workflows/release-validation.md` |

### Referências — o conhecimento de domínio

| Assunto | Leia |
|---|---|
| Estrutura de projeto, pacotes, minimalidade | `references/project-architecture.md` |
| Onde cada código deve rodar no ciclo de vida | `references/mod-lifecycle.md` |
| Registrar conteúdo | `references/registration.md` |
| Eventos e entrypoints da Fabric | `references/fabric-events.md` |
| Escrever Mixin | `references/mixin-development.md` |
| Blocos, itens, block entities, inventário | `references/content-development.md` |
| Entidades | `references/entity-development.md` |
| IA: Brain, tasks, memórias, sensores | `references/ai-development.md` |
| Packets e sincronização | `references/networking.md` |
| Fronteira client/server | `references/client-server.md` |
| Salvar e carregar estado | `references/persistence.md` |
| Data Components e NBT de item | `references/data-components.md` |
| Datagen, resources, lang, modelos | `references/datagen-and-resources.md` |
| Configuração do mod | `references/configuration.md` |
| Tick, scans, chunk, alocação | `references/performance.md` |
| Conviver com outros mods | `references/compatibility.md` |
| Gametest, unit test, níveis de teste | `references/testing.md` |
| Ler crash, achar causa raiz | `references/debugging.md` |
| Erro, log, degradação | `references/error-handling.md` |
| Trocar de versão do Minecraft | `references/migration.md` |
| O que não fazer | `references/anti-patterns.md` |

### Templates

| Vou produzir | Use |
|---|---|
| Plano de feature | `templates/feature-plan.md` |
| Plano de implementação em passos | `templates/implementation-plan.md` |
| Contrato de um sistema | `templates/system-contract.md` |
| Plano de registro | `templates/registry-plan.md` |
| Contrato de packet | `templates/network-contract.md` |
| Plano de persistência | `templates/persistence-plan.md` |
| Plano de Mixin | `templates/mixin-plan.md` |
| Plano de teste | `templates/test-plan.md` |
| Relato de bug | `templates/bug-report.md` |
| Relatório de migração | `templates/migration-report.md` |
| Resumo de entrega | `templates/implementation-summary.md` |

> Para **decisões de arquitetura (ADR)**, use o template da skill de pesquisa:
> `minecraft-code-research/templates/architecture-decision.md`. Um formato só,
> num lugar só.

### Checklists

| Antes de | Rode |
|---|---|
| Considerar uma feature pronta | `checklists/feature.md` |
| Fechar o desenho | `checklists/architecture.md` |
| Dar por registrado um conteúdo | `checklists/registration.md` |
| Aprovar um Mixin | `checklists/mixin.md` |
| Dizer que funciona em multiplayer | `checklists/client-server.md` |
| Dizer que o estado sobrevive | `checklists/persistence.md` |
| Dizer que o custo é aceitável | `checklists/performance.md` |
| Dizer que convive com outros mods | `checklists/compatibility.md` |
| Dizer que está testado | `checklists/testing.md` |
| Publicar | `checklists/release.md` |

### Exemplos

| Situação | Exemplo |
|---|---|
| "Adiciona um item ao mod" (SMALL) | `examples/simple-feature.md` |
| Organizar registros conforme o projeto cresce | `examples/registry-system.md` |
| Bloco com estado, tick e inventário | `examples/blockentity-system.md` |
| Sincronizar dado do servidor para o cliente | `examples/networking-system.md` |
| Uma feature do começo ao fim (FEATURE completo) | `examples/full-feature-workflow.md` |

## Definition of Done

Uma feature não está pronta porque compila. **Marque só o que se aplica** — mas
marque com verificação, não com intenção.

```text
[ ] Código implementado
[ ] Compila                          ./gradlew build
[ ] O jogo inicia                    ./gradlew runClient
[ ] O servidor dedicado inicia       ./gradlew runServer
[ ] A feature funciona
[ ] Edge cases testados
[ ] Client/Server validado
[ ] Persistência validada (fechar e reabrir o mundo)
[ ] Performance aceitável
[ ] Resources completos (lang, modelo, textura, loot, recipe, tag)
[ ] Logs limpos — sem spam, sem exceção engolida
[ ] Compatibilidade avaliada e classificada
[ ] Documentação atualizada
```

**Nunca diga que passou sem ter executado.** "Tem teste" e "foi verificado
rodando" são coisas diferentes, e as duas entram no relato separadas.

## Ao terminar

Produza o resumo de `templates/implementation-summary.md` e devolva conhecimento
novo à base:

- descobriu algo sobre o Vanilla → `docs/research/`
- mudou a arquitetura → `docs/architecture/`
- tomou decisão que dura → `docs/decisions/`
- rodou experimento → `docs/experiments/`

Conhecimento descoberto durante implementação é o mais caro que existe: custou o
bug. Deixá-lo só na mensagem de commit é jogá-lo fora.

## Continuidade

Uma sessão nova não tem o contexto da anterior. Ao começar:

1. Identifique a versão atual (`gradle.properties`), não a lembrada.
2. Leia a documentação arquitetural e as decisões relevantes.
3. Verifique o estado da implementação — o que está pela metade?
4. Compile antes de grandes alterações, para saber de onde está partindo.
5. **Não duplique sistema existente** — procure antes de criar.
6. Continue pelo menor próximo passo seguro.

O projeto precisa conter contexto suficiente para isso. Se não contém, a primeira
contribuição da sessão é fazer com que passe a conter.
