# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 2: Base Compartilhada** — continuação autorizada pelo usuário em 30/10/2026, após confirmar pessoalmente o funcionamento do cliente da Fase 1.
- **Sexta fatia concluída:** preços de remessa por item e tag. Build aprovado em 06/11/2026 com 26 testes. O responsável executou os passos 1 a 6 com runClient em mundo single player em 06/11/2026, sem erros.
- **Sétima fatia concluída (08/11/2026):** base de aniversários do jogador e tabela de NPCs por datapack, com correção da mensagem `true`/`false` e validação em single player. O responsável confirmou que a ausência persiste após limpar, salvar e reabrir. Cobertura efetivamente observada e limites registrados em `PORTING_NOTES.md`; validação integrada permanece pendente.
- **Pacotes permitidos:** `harvestfestival`, `harvestfestival.player` e `harvestfestival.player.command`, `harvestfestival.calendar` e `harvestfestival.calendar.command`, conforme `DEPENDENCIES.md` aprovado.
- **Critério atualizado:** preços por tags compõem a sexta fatia. A sétima entrega apenas dados de aniversário, persistência, sincronização e comandos administrativos; efeitos e escolha pelo jogador ficam adiados.
- **Bloqueado:** Antecipação de sistemas de fases posteriores e alterações no código de referência.

## Fases do Projeto
O desenvolvimento segue uma ordem estrita. O avanço para a próxima fase só é permitido após o registro explícito da mudança de fase neste arquivo.

0. [x] **Auditoria:** Inventário de features, criação do grafo de dependências e levantamento de mixins/integrações.
1. [x] **Esqueleto (validado):** Configuração do projeto NeoForge 26.2 compilando e abrindo o jogo com o mod carregado, sem conteúdo; funcionamento confirmado pelo usuário.
2. [ ] **Base Compartilhada (Atual):** Implementação de registros, dados do jogador, calendário/estações e economia básica, em fatias fechadas.
3. [ ] **Sistemas:** Implementação modular de sistemas (um por vez), seguindo estritamente a ordem definida em `DEPENDENCIES.md`.
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
- A Fase 2 permanece aberta. Servidor dedicado, multiplayer e reconexão continuam pendentes.

### ⏳ Próximos passos
- Definir a próxima fatia fechada da Fase 2; nenhuma transição de fase autorizada nesta conclusão.
- Pendências: validação integrada (servidor dedicado, dois jogadores, permissões, reconexão, persistência e espelho cliente do aniversário); escolha futura da data pelo jogador e efeitos de aniversário continuam adiados.
- HUD/calendário visual continuam adiados por decisão do usuário.

### Leitura utilizada na sétima fatia — aniversário do jogador
1. Ler `AGENTS.md`, `docs/STATUS.md` e `docs/PORTING_NOTES.md`, nessa ordem.
2. Consultar `docs/DEPENDENCIES.md` (dados básicos do jogador e relógio) e `docs/FEATURES.md` (calendário e dados por UUID).
3. No port, ler `CalendarDate.java` e `Season.java` para a representação da data, e `PlayerGold.java` e `PlayerGoldPersistenceTest.java` para a persistência do jogador e a sincronização com o titular.
4. No original, consultar os trechos de aniversário em `player/stats/StatsServer.java`, `player/stats/StatsClient.java` e `player/packet/PacketSyncBirthday.java` para conferir definição da data, gravação, leitura e envio ao jogador.
---

## Registro de Decisões

### ❓ Em Aberto
- **Validações futuras:** Servidor dedicado, dois jogadores simultâneos, reconexão e rejeição dos comandos para jogador sem permissão não foram testados em execução. O isolamento de dados tem teste automatizado e os comandos exigem `LEVEL_GAMEMASTERS` no servidor.
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
