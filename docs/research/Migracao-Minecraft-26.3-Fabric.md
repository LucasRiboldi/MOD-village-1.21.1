# Pesquisa: futura migracao para Minecraft 26.3 com Fabric

**Status:** pesquisa para consulta futura; nao aprova nem inicia a migracao.

**Data da pesquisa:** 2026-10-04

**Linha estavel preservada:** Minecraft 1.21.1 / Java 21. A matriz aprovada
continua em `docs/technical/Fabric-Version.md`. A migracao deve nascer em uma
branch e worktree proprias, sem converter a linha que recebe correcao e
playtest atualmente.

## Conclusao curta

Levar o Village Colony para Minecraft 26.3 com Fabric e viavel, mas nao e uma
atualizacao de numeros no Gradle. A partir de Minecraft 26.1 o ambiente Fabric
usa os nomes oficiais Mojang, nao Yarn, e exige Java 25. Portanto, a primeira
etapa e uma conversao de mappings que muda referencias de Minecraft em todo o
adaptador Fabric e exige revisao manual de mixins, renderizacao e serializacao.

O nucleo `core/` deve ser protegido durante o porte: a regra arquitetural
continua sendo portar os adaptadores `fabric/`, `client/` e integracoes com o
mundo, e nao misturar APIs Minecraft nas regras de colonia.

## Fotografia do projeto e alvo

| Item | Linha atual | Alvo a validar na abertura da migracao |
| --- | --- | --- |
| Minecraft | 1.21.1 | 26.3 |
| Java | 21 | 25 |
| Mappings | Yarn `1.21.1+build.3` | nomes oficiais Mojang |
| Gradle | 9.6.1 | 9.6.x compativel com o Loom escolhido |
| Fabric Loom | `1.17.18` | Loom 1.17+ com plugin `net.fabricmc.fabric-loom` |
| Fabric Loader | `0.19.3` | 0.19.5 ou minimo realmente exigido pelo codigo |
| Fabric API | `0.116.15+1.21.1` | serie 26.3; confirmar a versao fixa mais recente antes do primeiro build |
| Mod Menu | `11.0.5` | release compativel com 26.3, a confirmar |

O Fabric anunciou para 26.3 o uso de Loom 1.17 e Gradle 9.6.0, e a release
estavel do Loader indicada no anuncio era 0.19.5. O indice oficial ja contem
Fabric API para 26.3; a versao exata deve ser pesquisada novamente no dia em
que a branch for aberta, sem usar `latest`, `+` ou SNAPSHOT.

Fontes: [Fabric para 26.3](https://www.fabricmc.net/2026/09/15/263.html),
[migracao de mappings](https://docs.fabricmc.net/develop/porting/mappings/),
[Loom](https://docs.fabricmc.net/develop/loom/),
[indice oficial da Fabric API 26.3](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.160.4%2B26.3/).

## O que o codigo atual torna sensivel

Uma leitura inicial do repositorio separa a migracao em quatro frentes:

1. **Adaptador de servidor e mundo.** Ha uso intenso de `net.minecraft` em
   persistencia de vila, estruturas, blocos, entidades, inventarios, busca de
   terreno e minas. Cada erro de mapping deve ser corrigido pelo contrato da
   API alvo, nunca por substituicao textual em massa.
2. **Cliente e overlays.** `ProfessionOverlayRenderer`,
   `ConstructionOverlayRenderer` e `WorldPixelPanelRenderer` usam
   `WorldRenderEvents`, `WorldRenderContext` e `RenderLayer`. Esta e uma area
   de alto risco de compilacao e, sobretudo, de regressao visual. Nao ha uso
   direto de OpenGL/LWJGL encontrado na varredura inicial; ainda assim, a
   renderizacao deve ser validada no cliente 26.3 porque o Fabric alerta que o
   caminho de renderizacao esta em transicao para Blaze3D e Vulkan.
3. **Mixins.** O mod possui dois mixins sobre `VillagerEntity`: um `@Inject`
   em `initBrain` e um `@ModifyVariable`. Mappings e assinaturas de alvo devem
   ser revistos manualmente apos a conversao; o build verde nao basta para
   provar que a injecao ocorre no ponto correto.
4. **Rede e dados persistidos.** Payloads usam `CustomPayload`, `PacketCodec`
   e `RegistryByteBuf`; persistencia usa NBT e wrappers de registry. Devem ser
   recompilados, ter os codecs revisados e ser exercitados em mundo existente
   copiado. A fonte da verdade continua sendo o mundo, sem criar estado
   paralelo apenas para contornar a portabilidade.

## Mudancas externas que precisam de revisao

O Fabric 26.3 removeu registries de compostagem, combustivel, preparo de
pocoes e interacoes de bloco como `StrippableBlockRegistry`,
`TillableBlockRegistry` e `FlattenableBlockRegistry`; os substitutos sao
componentes/receitas e transformers de bloco dirigidos por dados. A busca
inicial nao encontrou esses simbolos no projeto, portanto nao ha uma correcao
direta ja identificada. A verificacao deve ser repetida na branch de porte,
inclusive sobre dependencias transitivas.

O mesmo anuncio reorganiza registries dinamicos e parte de worldgen. O mod nao
declara hoje um feature proprio como ponto central da colonia, mas scanners,
templates, estruturas e qualquer registro de mundo devem receber GameTests
especificos depois de compilar.

Fonte: [notas de portabilidade do Fabric 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Metodo recomendado

### 1. Preparar uma linha de migracao reproduzivel

- Criar branch `codex/port-26.3` a partir de um commit 1.21.1 verde e uma
  worktree isolada.
- Registrar hashes do JAR 1.21.1, resultado dos testes e uma copia de mundo de
  playtest. Nunca abrir o mesmo mundo salvo sem backup.
- Fixar Java 25 no toolchain, atualizar o wrapper se o Loom alvo pedir e
  atualizar o CI/local de build para usar a mesma JDK.
- Copiar a forma dos arquivos Gradle do exemplo oficial 26.3, sem substituir
  configuracoes proprias do projeto cegamente. Para 26.1+ o plugin indicado e
  `net.fabricmc.fabric-loom`, em vez do plugin de remapeamento usado nas linhas
  obfuscadas antigas.

### 2. Migrar mappings antes de corrigir comportamento

- Executar a ferramenta `migrateMappings` do Loom ou a ferramenta Ravel no
  IntelliJ em um commit dedicado. O projeto e Java, portanto a limitacao
  documentada para Kotlin nao se aplica, mas todo diff deve ser revisado.
- Corrigir primeiro imports, assinaturas e chamadas de `fabric/`; depois
  `client/`; por ultimo dados, testes e mixins. O `core/` e um alarme: uma
  grande quantidade de alteracoes ali indica vazamento de plataforma.
- Converter uma familia de erros por vez, recompilando frequentemente. Nomes
  parecidos entre Yarn e Mojang nao sao prova de semantica equivalente.

### 3. Atualizar metadados e dependencias

- Atualizar `gradle.properties`, `build.gradle`,
  `src/main/resources/fabric.mod.json` e a matriz de
  `docs/technical/Fabric-Version.md` no mesmo commit de configuracao.
- Remover a propriedade Yarn; fixar Loader, Fabric API e Mod Menu em versoes
  efetivamente testadas para 26.3.
- Ajustar `fabric.mod.json` para `minecraft: ~26.3` e `java: >=25`; escolher o
  minimo de Loader pelo uso real, nao apenas pela versao usada para compilar.
- Verificar o ambiente de desenvolvimento cliente/servidor. Separar os source
  sets de cliente e comum e uma opcao do Loom moderno, nao uma precondicao;
  so deve ser adotada se reduzir risco sem misturar a migracao com uma
  reorganizacao ampla.

### 4. Reparar pontos de integracao por prioridade

| Prioridade | Area | Criterio de aceite |
| --- | --- | --- |
| P0 | Build, mappings e Fabric Loader/API | Compila cliente e servidor sem APIs antigas nem versoes flutuantes. |
| P0 | Mixins de aldeao | Logs de auditoria confirmam aplicacao e aldeao mantem IA vanilla. |
| P0 | Persistencia, inventarios, redes e construcao | Abrir mundo copiado, carregar vila, receber payload e concluir obra. |
| P1 | Overlays pixelados e tela Mod Menu | Placas e icones aparecem, acompanham entidades e nao geram erro de render. |
| P1 | Mina, scanner e templates | GameTests de terreno/mina passam e playtest nao corrompe rotas. |
| P2 | Refatoracao opcional de source sets e limpeza de warnings | So depois do porte estar funcional; nao bloqueia o diagnostico do porte. |

### 5. Validar em camadas

1. `./gradlew.bat build --no-daemon` para toolchain, compilacao e recursos.
2. `./gradlew.bat test --rerun-tasks --no-daemon` para preservar regras de
   dominio independentes do Minecraft.
3. `./gradlew.bat runGametest --rerun-tasks --no-daemon` para servidor,
   estruturas, profissoes, caminhos e mina. Capturar a contagem inicial antes
   de qualquer alteracao e exigir que ela nao diminua sem uma decisao escrita.
4. Executar a auditoria de mixin ja configurada no projeto e inspecionar os
   arquivos exportados quando algum alvo mudar.
5. Playtest separado em mundo novo e em uma copia de mundo 1.21.1: aldeoes,
   profissoes, baus, obras, overlays, noite, mina e expansao de vila.
6. So entao gerar o JAR 26.3 e instalar em um perfil Fabric 26.3 limpo com
   Java 25, Loader, Fabric API e Mod Menu compativeis.

O Fabric recomenda a ferramenta de migracao de mappings, mas declara que ela
nao e perfeita e que mixins merecem revisao manual. A documentacao 26.1
tambem estabelece Java 25 e a mudanca para nomes Mojang.

Fontes: [guia de mappings do Fabric](https://docs.fabricmc.net/develop/porting/mappings/),
[Fabric para Minecraft 26.1](https://fabricmc.net/2026/03/14/261.html),
[exemplo oficial Fabric 26.3](https://github.com/FabricMC/fabric-example-mod/blob/26.3/build.gradle).

## Necessidades para iniciar quando a migracao for aprovada

- JDK 25 instalado e selecionado no Gradle/IDE/CI.
- Perfil de jogo separado em Minecraft 26.3 com Fabric Loader, Fabric API e
  Mod Menu em versoes compativeis e fixadas.
- Copia do mundo de teste e do diretorio de saves; nao e uma atualizacao
  reversivel para a linha 1.21.1.
- Branch e worktree exclusivas; correcao de gameplay continua na linha 1.21.1
  enquanto o porte nao estiver homologado.
- Inventario de dependencias e de APIs usadas, incluindo qualquer mod opcional
  que entre no perfil de teste.
- Tempo de playtest manual reservado para renderizacao, mixins, dados de vila,
  construcao e mina. Teste automatizado nao substitui esta etapa.

## Decisoes que devem ser tomadas antes do primeiro commit de porte

1. A versao 26.3 sera uma linha nova mantida em paralelo ou substituira a
   distribuicao 1.21.1 depois da homologacao?
2. O objetivo inclui abrir saves de vila 1.21.1 no 26.3, ou o suporte comeca
   por mundos novos? Se incluir saves, sera necessario um protocolo de
   migracao de dados e uma matriz de saves reais anonimizados/copiados.
3. Mod Menu continua obrigatorio ou se torna integracao opcional para reduzir
   o conjunto de dependencias cliente?
4. A separacao de source sets cliente/comum deve entrar no porte ou ficar para
   uma refatoracao posterior? A recomendacao inicial e adiar essa mudanca.

## Fora do escopo desta pesquisa

Esta nota nao muda versoes, codigo, JAR, saves, CI nem a especificacao tecnica
aprovada da linha 1.21.1. Ela tambem nao promete compatibilidade de save: isso
so pode ser afirmado apos uma migracao implementada e playtests em copias de
mundo.
