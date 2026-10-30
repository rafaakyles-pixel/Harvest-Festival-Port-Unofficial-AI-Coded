# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 1: Esqueleto** — início autorizado pelo usuário em 30/10/2026 após conferência da cobertura da Fase 0.
- **Permitido:** Configuração Gradle/wrapper na raiz, recursos e classe de entrada mínima em `src/`, execução do build/cliente e atualização de documentação.
- **Pacote permitido nesta fase:** `harvestfestival`, somente para a classe de entrada do mod vazio.
- **Bloqueado:** Implementação de sistemas de gameplay e alterações no código de referência.

## Fases do Projeto
O desenvolvimento segue uma ordem estrita. O avanço para a próxima fase só é permitido após o registro explícito da mudança de fase neste arquivo.

0. [x] **Auditoria:** Inventário de features, criação do grafo de dependências e levantamento de mixins/integrações.
1. [x] **Esqueleto (Atual, validado):** Configuração do projeto NeoForge 26.2 compilando e abrindo o jogo com o mod carregado, sem conteúdo. A Fase 2 ainda não foi iniciada.
2. [ ] **Base Compartilhada:** Implementação de registros, dados do jogador, calendário/estações e economia básica.
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

### 🔄 Em Andamento
- Nenhuma implementação adicional em andamento. Critérios do esqueleto atendidos; a fase permanece 1 até autorização explícita para a próxima.

### ⏳ Próximos passos
- Definir e autorizar uma fatia fechada da Fase 2 antes de implementar base compartilhada. Os checkboxes de `FEATURES.md` e dos catálogos continuam vazios porque controlam portagem de conteúdo, não o esqueleto.

---

## Registro de Decisões

### ❓ Em Aberto
- **Validações futuras:** Servidor dedicado, criação/carregamento de mundo e gameplay não foram testados nesta fatia de mod vazio.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Versão alvo:** Minecraft 26.2 com NeoForge 26.2.0.93, verificada e confirmada pelo usuário em 30/10/2026. A confirmação da versão não substitui a verificação individual de APIs.
- **Limite de escopo:** Portar somente o conteúdo do mod até 0.6.x; não implementar versões posteriores nem restaurar funcionalidades apenas planejadas em comentários/TODOs.
- **Dependências:** `DEPENDENCIES.md` aprovado pelo usuário em 30/10/2026. Pontos de corte permanecem análise; distribuição em mod único mantida.
- **Builds:** Obrigatórios a partir da Fase 1; build do esqueleto aprovado. A tentativa anterior na Fase 0 havia falhado pela ausência do wrapper, agora incluído.
- **Ambiente validado:** Oracle JDK 25.0.5.0.2, Gradle 9.2.1 e ModDevGradle 2.0.148. Nesta sessão Windows, `TEMP` e `TMP` foram direcionados a `build/tmp` apenas no processo para evitar falha de socket local do Java; procedimento em `PORTING_NOTES.md`.
- **Base de Referência:** Branch `1.12.2-0.6.X` do fork Snownee/Harvest-Festival (Licença MIT).
- **Estrutura Interna:** Mod único, organizado internamente com pacotes divididos por sistema.
- **Arquitetura de Compatibilidade:** Implementação baseada em dados e tags (JSON) desde o dia zero, evitando hardcoding.
- **Controle de Versão:** Commits, pushes e demais alterações de repositório Git serão executados de forma manual e exclusiva pelo desenvolvedor humano.
