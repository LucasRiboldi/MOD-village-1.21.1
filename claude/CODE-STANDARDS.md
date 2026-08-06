# Code Standards

# Village Colony Java Coding Standards

---

# 1. Linguagem

Java:

* versão compatível com Minecraft 1.21.1 Fabric;
* código orientado a objetos;
* foco em manutenção.

---

# 2. Organização de Pacotes

Padrão:

```
com.villagecolony
```

Estrutura:

```
core

fabric

data
```

---

# 3. Nomes

Classes:

```
PascalCase
```

Exemplo:

```
ColonyManager
ResourceService
```

---

Métodos:

```
camelCase
```

Exemplo:

```
registerWorker()
calculateResources()
```

---

Constantes:

```
UPPER_CASE
```

Exemplo:

```
MAX_TASK_PRIORITY
```

---

# 4. Classes

Uma classe deve possuir uma responsabilidade.

Evitar:

```
God Classes
```

---

Exemplo ruim:

```
VillageManager
```

controlando:

* recursos;
* construção;
* trabalhadores;
* salvamento.

---

Exemplo correto:

```
ResourceManager

ConstructionManager

WorkerManager
```

---

# 5. Modelos

Modelos devem:

* armazenar dados;
* possuir getters/setters necessários;
* validar estado simples.

Não devem:

* acessar Minecraft;
* executar tarefas.

---

# 6. Services

Services:

* possuem lógica;
* coordenam sistemas;
* não armazenam estado global excessivo.

---

# 7. Null Safety

Preferir:

* Optional;
* validações;
* estados claros.

Evitar:

```
NullPointerException
```

---

# 8. Logs

Usar logs para:

* inicialização;
* erros;
* eventos importantes.

Não gerar spam por tick.

---

# 9. Comentários

Comentários devem explicar:

* decisões;
* motivos;
* comportamento incomum.

Não explicar código óbvio.

---

# 10. Performance

Evitar:

```
for every tick
scan world
```

Preferir:

```
scheduled updates

cached data

event based updates
```

---

# 11. Testabilidade

Classes devem permitir:

* testes isolados;
* substituição de componentes;
* baixo acoplamento.

---

# 12. Fabric

Classes Fabric podem conhecer:

* ServerWorld;
* Entity;
* BlockEntity.

Mas nunca devem contaminar o Core.

---

# 13. Finalização de Código

Antes de considerar pronto:

Confirmar:

* compilação;
* organização;
* documentação;
* teste no Minecraft.

---

# Regra Final

Código simples e previsível é preferível a código complexo e "inteligente".

O objetivo é criar uma colônia viva sustentável, não uma arquitetura impossível de manter.
