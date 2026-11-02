# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 2: Base Compartilhada** — continuação autorizada pelo usuário em 30/10/2026, após confirmar pessoalmente o funcionamento do cliente da Fase 1.
- **Permitido agora:** Primeira fatia aprovada: saldo individual persistente e comandos administrativos de consulta/alteração; infraestrutura e testes necessários.
- **Pacotes permitidos:** `harvestfestival`, `harvestfestival.player` e `harvestfestival.player.command`, conforme `DEPENDENCIES.md` aprovado.
- **Critérios da fatia:** saldo fiel ao original, individual e preservado ao salvar/carregar; comandos administrativos; build aprovado.
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
- Nenhuma implementação em andamento; primeira fatia concluída. A Fase 2 continua aberta para definição da próxima fatia.

### ⏳ Próximos passos
- Definir e aprovar a próxima fatia da Fase 2. Calendário, HUD, lojas e remessas ainda não foram implementados.

---

## Registro de Decisões

### ❓ Em Aberto
- **Validações futuras:** Servidor dedicado, dois jogadores simultâneos e rejeição dos comandos para jogador sem permissão não foram testados em execução. O isolamento de dados tem teste automatizado e os comandos exigem `LEVEL_GAMEMASTERS` no servidor.
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
