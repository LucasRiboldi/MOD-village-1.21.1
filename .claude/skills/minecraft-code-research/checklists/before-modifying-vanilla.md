# Checklist — antes de modificar Vanilla

> **A mais importante desta skill.** Passe por ela antes de escrever qualquer
> código que toque um sistema do Minecraft.
>
> Item marcado exige evidência, não impressão. "Acho que sim" conta como não
> marcado — e "não procurei" também.

## Compreensão

- [ ] **Sistema Vanilla identificado** — sei o nome e o que ele faz
- [ ] **Classe principal localizada** — e confirmei que existe nesta versão
      (`unzip -l "$MC_JAR" | grep "<Classe>.class"`)
- [ ] **Ciclo de vida compreendido** — sei em que ponto de qual linha do tempo
      (jogo / entidade / block entity) isto acontece
- [ ] **Dados compreendidos** — sei o que o sistema lê e de onde
- [ ] **Estado compreendido** — sei quem é o dono e onde ele mora
- [ ] **Callers analisados** — sei quem chama, por quantos caminhos diferentes
- [ ] **Side effects identificados** — sei o que muda além do retorno

> Se o método é chamado por dois caminhos (spawn e load, por exemplo), a sua
> mudança precisa valer nos dois.

## Alternativas — a escada de extensão

Verificadas **nesta ordem**, com evidência de cada "não":

- [ ] **1.** O sistema Vanilla já faz isso?
- [ ] **2.** Existe registro que aceita entrada nova?
      (profissão, POI, memória, sensor, tipo de entidade…)
- [ ] **3.** É data-driven? (tag, loot table, recipe, datapack)
- [ ] **4.** Existe API Fabric? — procurei nos sources jar, não de memória
- [ ] **5.** Existe evento Fabric?
- [ ] **6.** Dá para resolver por composição?
- [ ] **7.** Existe interface implementável?
- [ ] **8.** Existe método `protected` extensível por herança?
- [ ] **—** Access widener seria suficiente?
- [ ] **9–11.** Só então: Mixin — e do tipo menos invasivo que resolve

- [ ] **Se subi a escada, escrevi a justificativa** do salto

> "Foi mais rápido" não é justificativa. O custo do Mixin desnecessário aparece
> no primeiro relatório de incompatibilidade, não hoje.

## Versão e mappings

- [ ] **Versão confirmada** no `gradle.properties` de hoje
- [ ] **Mappings confirmados** (Yarn ou Mojmap, e qual build)
- [ ] **Assinaturas verificadas com `javap`** — não lembradas
- [ ] Se copiei código de outro projeto: conferi versão, mapping, existência das
      classes, assinaturas e **se o comportamento mudou apesar do nome igual**

## Outros mods

- [ ] **Pesquisei se alguém já resolveu isto** — e como
- [ ] Sei quais mods conhecidos mexem neste mesmo sistema

## Impacto

- [ ] **Client/Server analisado** — sei quem tem o estado verdadeiro e quem valida
- [ ] **Persistência analisada** — sei o que sobrevive a quê
- [ ] **Performance considerada** — tenho os números: frequência × população
- [ ] **Compatibilidade classificada** — LOW / MEDIUM / HIGH, com justificativa

## Degradação

- [ ] **Sei o que acontece se a minha mudança falhar** — e o resultado é
      comportamento Vanilla, não estado inconsistente
- [ ] **Nenhuma exceção minha escapa de dentro de método Vanilla**

## Registro

- [ ] A análise está num documento, não só nesta conversa
- [ ] As afirmações estão etiquetadas (`[FATO]`, `[INFERÊNCIA]`, `[HIPÓTESE]`)
- [ ] O `research-status.md` foi atualizado

---

**Se sobraram itens desmarcados que importam:** volte à pesquisa. Escrever código
sobre premissa não verificada é como o patch nasce torto — e você não descobre
hoje, descobre quando alguém relata que o mod não funciona junto com outro.
