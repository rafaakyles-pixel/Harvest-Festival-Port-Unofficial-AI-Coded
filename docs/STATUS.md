# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 3: Sistemas** — avanço autorizado explicitamente pelo responsável em 08/11/2026. Fase 2 e oitava fatia concluídas com ressalva do cenário 8.
- **Aceite:** cenários 1–7 e 9 aceitos conforme a cobertura e limitações da tabela. Mantido o comportamento offline observado: remessas pagam na próxima virada com o jogador conectado; nenhuma correção solicitada.
- **Pendência obrigatória:** cenário 8 permanece **não executado**, a verificar no primeiro consumidor cliente (HUD/interface): ouro e aniversário do titular, isolamento entre jogadores, atualização, limpeza e reconexão. Mensagens administrativas e testes de codec não comprovam o espelho.
- **Pacotes já aprovados:** `harvestfestival`, `harvestfestival.player`, `harvestfestival.player.command`, `harvestfestival.calendar` e `harvestfestival.calendar.command`. Nenhum novo pacote criado nesta transição.
- **Próxima implementação:** definir uma fatia vertical pequena da Fase 3 com critérios verificáveis, conforme `DEPENDENCIES.md`; registrar os novos pacotes quando a fatia for definida.
- **Limites:** referência somente leitura; integrações específicas adiadas; esta transição não implementa sistemas nem autoriza HUD ou efeitos de aniversário.

## Fases do Projeto
O desenvolvimento segue uma ordem estrita. O avanço para a próxima fase só é permitido após o registro explícito da mudança de fase neste arquivo.

0. [x] **Auditoria:** Inventário de features, criação do grafo de dependências e levantamento de mixins/integrações.
1. [x] **Esqueleto (validado):** Configuração do projeto NeoForge 26.2 compilando e abrindo o jogo com o mod carregado, sem conteúdo; funcionamento confirmado pelo usuário.
2. [x] **Base Compartilhada:** concluída pelo responsável em 08/11/2026, com cenário 8 transportado como pendência obrigatória.
3. [ ] **Sistemas (Atual):** Implementação modular de sistemas (um por vez), seguindo estritamente a ordem definida em `DEPENDENCIES.md`.
4. [ ] **Polimento:** Integrações com outros mods e refinamentos finais.

---
## Acompanhamento de Tarefas

### ✅ Pronto
- Registro final em 08/11/2026: build `BUILD SUCCESSFUL in 7s`; 38 testes nos relatórios, zero falhas, erros ou ignorados. A tarefa de testes estava `UP-TO-DATE` neste build documental, reutilizando a execução aprovada da correção. Última linha literal: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`.
- Correção e validação da sétima fatia: `NpcBirthdayCommands` envia o resultado booleano como texto, preservando `true`/`false` na mensagem recebida. Novo teste de serialização da mensagem. Em 07/11/2026, o agente verificou no cliente a sobreposição por datapack, descarte de regra inválida com aviso, continuidade de regra válida, retorno do exemplo após desativar o pack, definição do aniversário, rejeição do dia 31, morte e salvar/reabrir. Em 08/11/2026, o responsável confirmou ausência após limpar e reabrir usando `/hf birthday get`. Não representa execução de todas as variações do roteiro nem inspeção do espelho cliente.
- Sétima fatia implementada em 07/11/2026: attachment opcional de aniversário com `copyOnDeath`, comandos administrativos, tabela recarregável de datas por identificador e exemplo de teste. A base independente de NPCs por datapack é extensão decidida pelo responsável; nenhum NPC foi implementado. Build `BUILD SUCCESSFUL in 28s`, 37 testes, zero falhas, erros ou ignorados. Última linha literal: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`.
- Sexta fatia implementada em 06/11/2026. Regras de datapack recarregáveis com prioridade e desempate entre tags, rejeição de regras inválidas e de IDs de item desconhecidos. Sete novos testes aprovados. O trigo foi aceito pela tag `c:crops/wheat`. O pagamento após `/reload` e `/hf newDay` foi de 240, e o responsável conferiu os avisos sobre arquivos inválidos em `latest.log`.
- Quinta fatia concluída conforme validação manual informada pelo responsável em 05/11/2026, com `runClient` em mundo single player: `/hf gold set`, colocação da caixa no chão, graveto validado e consulta por `/hf gold get`; valor mantido após morte e destruição da caixa; sair/reabrir o mundo; `/hf newDay` com valores corretos e sem duplicação, inclusive após sair e entrar. O relato não amplia a cobertura para servidor dedicado ou reconexão multiplayer.
- Quinta fatia implementada em 04/11/2026: caixa `harvestfestival:shipping_box`, depósito manual de uma unidade por clique, attachment persistente `shipping_queue` com `copyOnDeath` e pagamento pelo consumidor de `NewDayEvent`, reutilizando a soma de `PlayerGold`. Sem novos pacotes de sistema. Seis testes novos de remessas; total de 19 testes, zero falhas e zero erros. Build aprovado (`BUILD SUCCESSFUL in 13s`). Última linha literal: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`. Roteiro e limitações em `PORTING_NOTES.md`.
- Quarta fatia concluída em 04/11/2026: sincronização de saldo de ouro com o jogador proprietário. Validação manual no jogo pelo responsável, com runClient em mundo single player: `/hf gold set` e `/hf gold add`, consultados por `/hf gold get`; renascimento após morte; atravessar portal e voltar; sair e reabrir o mundo.
- Build desta validação (última linha fornecida pelo responsável): **não informado**. Testes automatizados (quantidade informada pelo responsável): **não informado**.
- Cenários pendentes da quarta fatia: servidor dedicado, dois jogadores e reconexão. Sair e reabrir o mundo single player não encerra a pendência de reconexão.
- Terceira fatia concluída: atualização diária interna. Usuário confirmou todos os testes manuais do roteiro e build bem-sucedido após runClient; commit realizado pelo usuário.
- Segunda fatia da Fase 2 concluída em 02/11/2026: calendário básico persistente. Usuário confirmou aprovação dos testes manuais de comandos, troca de estação, virada do ano, salvar/reabrir e passagem natural do tempo. Cinco testes automatizados de calendário e três de ouro aprovados.
- Estrutura inicial do repositório (`.gitignore`, `AGENTS.md`, `docs/`).
- Definição das regras permanentes de desenvolvimento em `AGENTS.md`.
- Auditoria documental da Fase 0: inventário dos 19 diretórios imediatos do Java, 123 missões anotadas, recursos, integrações adiadas e os 2 mixins do original.
- Separação dos quatro catálogos em `docs/catalogs/`, com índice em `FEATURES.md`; conteúdo original preservado integralmente, incluindo cabeçalhos e linhas em branco, verificado por reconstrução.
- `DEPENDENCIES.md` aprovado explicitamente pelo usuário em 30/10/2026; grafo, ciclos, ordem de portagem e pontos de corte disponíveis para as próximas fases.
- Conferência de encerramento da Fase 0: todos os diretórios Java cobertos, 123 missões anotadas presentes e os 2 mixins documentados; nenhuma lacuna de cobertura encontrada que bloqueasse a Fase 1.
- Esqueleto da Fase 1: wrapper Gradle 9.2.1, ModDevGradle 2.0.148, Java 25, NeoForge 26.2.0.93, classe `harvestfestival.HarvestFestival` e metadados, sem registros de gameplay.
- Build aprovado em 30/10/2026: `build/libs/harvestfestival-0.1.0-dev.jar` gerado. Não há testes automatizados de gameplay nesta fase sem conteúdo.
- `runClient` validado no menu principal: Minecraft 26.2, NeoForge 26.2.0.93 e 3 mods; log confirma `Harvest Festival 0.1.0-dev` e a execução da classe de entrada.
- Primeira fatia da Fase 2, validada em 02/11/2026: saldo por jogador com attachment persistente, preservação no renascimento e comandos administrativos `/hf gold [player] <get|set|add>` (`set`/`add` recebem valor).
- Três testes automatizados: zero inicial/dados ausentes, isolamento entre titulares e gravação/releitura comprimida com valores negativos e extremos de `long`.
- Cliente/servidor integrado: saldo inicial zero, `set 3000000000`, `Dev add -25`, saída/reabertura do mundo e morte/renascimento; saldo final `2999999975` preservado. Mundo local `run/saves/HF Gold Validation`, fora dos entregáveis versionados.
- Build final aprovado em 02/11/2026: `BUILD SUCCESSFUL`, três testes sem falhas; `runClient` encerrado normalmente com sucesso. Ajustes locais de codificação e diretório temporário documentados em `PORTING_NOTES.md`.

### 🔄 Em Andamento
- A sincronização do attachment de aniversário com o cliente foi verificada só por teste de codificação de rede (data e ausência), sem inspeção do espelho cliente. A verificação real fica pendente na validação integrada. A mensagem administrativa `true`/`false` foi observada no cliente, o que não comprova o espelho do attachment.
- Fase 3 autorizada; primeira fatia ainda não definida. Espelho cliente permanece pendente.

### ⏳ Próximos passos
- Definir a primeira fatia fechada da Fase 3 conforme DEPENDENCIES.md.
- Pendência de validação: cenário 8 no primeiro consumidor cliente. Escolha da data pelo jogador e efeitos continuam adiados.
- HUD/calendário visual continuam adiados por decisão do usuário.

### Leitura utilizada na sétima fatia — aniversário do jogador
1. Ler `AGENTS.md`, `docs/STATUS.md` e `docs/PORTING_NOTES.md`, nessa ordem.
2. Consultar `docs/DEPENDENCIES.md` (dados básicos do jogador e relógio) e `docs/FEATURES.md` (calendário e dados por UUID).
3. No port, ler `CalendarDate.java` e `Season.java` para a representação da data, e `PlayerGold.java` e `PlayerGoldPersistenceTest.java` para a persistência do jogador e a sincronização com o titular.
4. No original, consultar os trechos de aniversário em `player/stats/StatsServer.java`, `player/stats/StatsClient.java` e `player/packet/PacketSyncBirthday.java` para conferir definição da data, gravação, leitura e envio ao jogador.
---

## Registro de Decisões

### ❓ Em Aberto
- **Validação integrada:** cenário 8 pendente; demais resultados aceitos pelo responsável em 08/11/2026, sem ampliar a cobertura observada.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Precedência da versão:** O usuário confirmou que deve prevalecer o conteúdo em disco: Minecraft 26.2 / NeoForge 26.2.0.93.
- **Continuação:** Em 30/10/2026, o usuário confirmou que o cliente rodou e aprovou prosseguir para a Fase 2. Não é necessária nova aprovação da transição; permanece necessária a definição de cada fatia conforme a seção 5 de `AGENTS.md`.
- **Versão alvo:** Minecraft 26.2 com NeoForge 26.2.0.93, verificada e confirmada pelo usuário em 30/10/2026. A confirmação da versão não substitui a verificação individual de APIs.
- **Limite de escopo:** Portar somente o conteúdo do mod até 0.6.x; não implementar versões posteriores nem restaurar funcionalidades apenas planejadas em comentários/TODOs.
- **Dependências:** `DEPENDENCIES.md` aprovado pelo usuário em 30/10/2026. Pontos de corte permanecem análise; distribuição em mod único mantida.
- **Builds:** Obrigatórios a partir da Fase 1; build do esqueleto aprovado. A tentativa anterior na Fase 0 havia falhado pela ausência do wrapper, agora incluído.
- **Ambiente validado:** Oracle JDK 25.0.5.0.2, Gradle 9.2.1 e ModDevGradle 2.0.148. Nesta sessão Windows, `TEMP` e `TMP` foram direcionados a `build/tmp` apenas no processo para evitar falha de socket local do Java; procedimento em `PORTING_NOTES.md`.
- **Base de Referência:** Branch `1.12.2-0.6.X` do fork Snownee/Harvest-Festival (Licença MIT).
- **Estrutura Interna:** Mod único, organizado internamente com pacotes divididos por sistema.
- **Arquitetura de Compatibilidade:** Implementação baseada em dados e tags (JSON) desde o dia zero, evitando hardcoding.
- **Controle de Versão:** Commits, pushes e demais alterações de repositório Git serão executados de forma manual e exclusiva pelo desenvolvedor humano.
- **Controle de Ambiente (02/11/2026):** Projeto movido para `C:\dev\harvest-festival-port`; diretório `C:\dev` oculto, sem impedir o acesso. Eclipse Temurin 21.0.13+9 instalado para iniciar o Gradle 9.2.1; a toolchain do projeto permanece Java 25. Build e três testes reexecutados com sucesso no novo caminho, usando `TEMP`/`TMP` locais e override `file.encoding=COMPAT`; procedimento atual em `PORTING_NOTES.md`.

## Oitava fatia — tabela de validação integrada (08/11/2026)

Plano aprovado e preparação executada. A tabela distingue verificação do agente e ações do responsável; nenhuma expectativa abaixo equivale a resultado observado. Cenário parcialmente executado mantém `não executado` até completar suas subetapas.

| Nº | Cenário | Resultado | Quem executou | Evidência e pendência |
| --- | --- | --- | --- | --- |
| 1 | Servidor dedicado com mod, sem erro | passou | Agente | Log dedicado: carregamento HF; `Done (0.244s)!` às 11:33:55; `list`, `hf day` e consulta de NPC responderam às 11:33:57. Nenhum ERROR/FATAL na subida e inspeção até 11:44. Às 11:51:25 houve Connection reset após queda nativa do cliente; servidor permaneceu ativo. Avisos e ocorrência registrados nas notas. |
| 2 | Dois clientes com nomes distintos | passou | Responsável conectou; agente verificou | HFTester2 entrou às 11:43:35; HFTester1 às 11:43:49; `list` às 11:44:17: `There are 2 of a max of 2 players online: HFTester2, HFTester1`. Primeiro runClient2 falhou nativamente; repetição conectou. |
| 3 | Isolamento de ouro, aniversário e fila | passou | Responsável depositou; agente preparou e inspecionou | `data get entity <nome> "neoforge:attachments"` às 11:45:36: HFTester1 ouro 100, primavera 5, fila de um graveto a 25; HFTester2 ouro 200, inverno 20, fila de um graveto a 40. Dados distintos preservados simultaneamente. |
| 4 | Permissões dos três comandos | passou | Responsável (gold sem OP) e agente (demais consultas) | Cliente HFTester2: get de gold, birthday e npcBirthday negados sem OP; os três aceitos após op nível 2 (11:49–11:50). Variantes mutadoras/alvo explícito do roteiro ampliado não foram executadas nesta comparação; não ampliar a cobertura. |
| 5 | Reconexão preserva ouro, aniversário e fila | passou | Agente (HFTester2), responsável (HFTester1); agente inspecionou | HFTester2 saiu/entrou às 11:50:38/11:50:51: 200, inverno 20, fila 40. HFTester1 reconectou após queda nativa às 11:52:29: 100, primavera 5, fila 25. NBT e comandos às 11:50:59/11:52:41. |
| 6 | Relógio único e pagamentos sem duplicação | passou | Agente (viradas/inspeção e consulta inicial HFTester2), responsável (consultas dos clientes) | Ambos mostraram primavera 1/ano 1; viradas às 11:52:52 e 11:53:07 geraram uma atualização cada. Ouro 125/240, filas vazias; segunda virada manteve valores. Logs dos clientes às 11:53:36 e 11:53:43: primavera 3/ano 1. |
| 7 | Remessa offline na virada | passou | Responsável depositou/desconectou/reconectou HFTester2; agente executou viradas e inspeções | Observação concluída: não pagou offline nem ao reconectar (240 e fila 40); pagou na próxima virada conectado (280 e fila vazia às 16:36:59). Leituras offline precedidas de save-all. Aceito no encerramento pelo responsável em 08/11/2026; não constitui prova adicional de paridade. |
| 8 | Espelho cliente e isolamento do titular | não executado | — | Sem depurador preparado. Pendência obrigatória quando houver primeiro consumidor cliente (HUD/interface). Mensagens administrativas e testes de codec não comprovam o espelho. |
| 9 | Preços e aniversários por datapack e reload | passou | Responsável depositou; agente operou console/pack e inspecionou | Às 16:45:58, fila real 100+140+140 (segundo clique extra informado pelo responsável); crédito 380 levou 280 a 660. Nova virada manteve 660. Pack desativado/reload: depósito a 100, saldo 760 e fila vazia às 16:46:49. NPC retornou primavera 15 e sibling ausente; regras inválidas e continuidade da válida verificadas antes. |

### Build e evidências desta preparação

- Build com reexecução de testes: `BUILD SUCCESSFUL in 16s`; 38 testes, zero falhas, erros ou ignorados, tarefa `test` executada. `run/validation/build.log`.
- Build final após ajuste de console: `BUILD SUCCESSFUL in 8s`; 38 testes nos XML, `test UP-TO-DATE`, reutilizando a execução acima. `run/validation/build-final.log`.
- Última linha literal do build final:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

- Logs dos jogos: `run/server/logs/latest.log`, `run/client1/logs/latest.log`, `run/client2/logs/latest.log`. Roteiro completo e detalhes do ambiente em `PORTING_NOTES.md`, seção da oitava fatia.
- Ocorrência de ambiente: primeiro segundo cliente encerrou às 11:34:55 com `EXCEPTION_ACCESS_VIOLATION` em `glfw.dll`, fora da JVM; `run/client2/hs_err_pid15572.log`. Reiniciado sem alteração de código; posteriormente conectado. Causa não determinada; nenhum bug do mod diagnosticado ou corrigido.
- Pendências: cenário 8 obrigatório no primeiro consumidor cliente; aceite da Fase 2 e avanço à Fase 3 registrados em 08/11/2026. Variantes adicionais de permissão do roteiro ampliado não foram exercitadas; consultas dos três comandos passaram. Falhas nativas de GLFW registradas sem causa comprovada. Servidor encerrado normalmente por stop às 16:46:49, com salvamento de jogadores e todas as dimensões.
- **Fase 2 encerrada pelo responsável, exceto cenário 8; Fase 3 autorizada em 08/11/2026.**

### Encerramento da execução desta fatia — 08/11/2026

- Cenários 1–7 e 9 observados, com limites explícitos na tabela; cenário 8 pendente obrigatório. O cenário 7 foi posteriormente aceito pelo responsável no encerramento, sem alteração de comportamento.
- Build mais recente: `BUILD SUCCESSFUL in 6s`; 38 testes, zero falhas, erros ou ignorados; `test UP-TO-DATE`, reutilizando a reexecução desta sessão. Saída `run/validation/build-conclusion.log`; última linha literal igual à transcrita acima.
- Servidor encerrado por `save-all` e `stop` às 16:46:49; jogadores e todas as dimensões salvos; runServer terminou com sucesso. Evidência `run/validation/server-final.log`.
- Nenhum bug do mod corrigido e nenhuma feature criada. **Fase 2 posteriormente encerrada pelo responsável com ressalva do cenário 8; Fase 3 autorizada.**

## Aceite e transição — 08/11/2026

O responsável deu a Fase 2 e a oitava fatia como concluídas, exceto o cenário 8, e autorizou seguir à Fase 3 antes dessa verificação. Esta decisão sucede os registros históricos que mantinham a fase aberta ou aguardavam aceite do comportamento offline. Mantida a regra observada de pagamento na próxima virada com o jogador conectado, sem correção e sem alegar prova adicional de paridade.

O cenário 8 continua não executado e obrigatório no primeiro consumidor cliente (HUD/interface): ouro/aniversário do titular, isolamento entre jogadores, atualização, limpeza e reconexão. Mensagens administrativas e testes de codec não comprovam o espelho. Avançar não elimina a pendência nem autoriza implementar HUD agora.

Somente STATUS.md e PORTING_NOTES.md atualizados. Nenhum código, pacote ou sistema novo; primeira fatia da Fase 3 ainda precisa de escopo e critérios verificáveis conforme DEPENDENCIES.md. Limitações dos testes e incidentes nativos de GLFW preservados. Nenhum comando Git ou controle de interface realizado.

Validação documental da transição: `BUILD SUCCESSFUL in 6s`; 38 testes nos XML, zero falhas, erros ou ignorados. `test UP-TO-DATE`, sem nova execução de testes. Comando: `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`; saída em `run/validation/build-phase3-transition.log`. Nenhum cliente aberto nesta atualização. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```
