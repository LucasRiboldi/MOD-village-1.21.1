# Development Rules

# Village Colony Development Rules

---

# 1. Processo Obrigatório

Todo desenvolvimento segue:

```
Planejamento

↓

Implementação

↓

Teste

↓

Revisão

↓

Próxima etapa
```

---

# 2. Nunca Programar Fora do Escopo

Não implementar funcionalidades futuras sem aprovação.

Exemplo:

Não criar:

* mineração;
* comércio;
* defesa;
* distritos;

antes da fase correspondente.

---

# 3. Uma Tarefa por Vez

Cada implementação deve estar relacionada a uma tarefa do:

```
MVP-Tasks.md
```

---

# 4. Mudanças Arquiteturais

Qualquer mudança importante exige ADR.

Exemplos:

* alterar armazenamento;
* mudar arquitetura;
* adicionar dependência;
* mudar modelo de dados.

Criar:

```
docs/decisions/ADR-XXX.md
```

---

# 5. Preferir Simplicidade

Entre duas soluções:

Escolher:

* menor complexidade;
* menor manutenção;
* melhor integração Vanilla.

---

# 6. Não Criar Sistemas Paralelos

Não substituir:

* IA Vanilla;
* aldeões Vanilla;
* receitas Vanilla;
* estruturas Vanilla.

Adicionar camadas de organização.

---

# 7. Performance

Minecraft possui limite de processamento.

Nunca:

* varrer o mundo inteiro por tick;
* procurar blocos sem limite;
* criar milhares de entidades;
* manter referências inválidas.

Sempre usar:

* cache;
* intervalos;
* eventos.

---

# 8. Persistência

Salvar somente:

* conhecimento da colônia;
* referências;
* estados.

Nunca salvar:

* inventários completos;
* blocos individuais;
* dados que Minecraft já possui.

---

# 9. Código Novo

Toda classe nova deve responder:

"Qual responsabilidade única esta classe possui?"

Se a resposta envolver várias áreas:

Dividir.

---

# 10. Testes

Toda funcionalidade deve possuir:

* teste de inicialização;
* teste dentro do mundo;
* teste após salvar e carregar.

---

# 11. Compatibilidade

Código deve funcionar em:

* Singleplayer;
* Dedicated Server.

Nunca assumir presença de jogador.

---

# 12. Revisão Antes de Finalizar

Antes de concluir uma tarefa:

Verificar:

* arquitetura respeitada;
* documentação atualizada;
* código compilando;
* ausência de erros.
