# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 0: Auditoria**
- **Permitido:** Leitura do diretório `reference/original-1.12.2/` e geração de documentação no diretório `docs/`.
- **Bloqueado:** Criação de código de sistemas ou qualquer alteração no diretório `src/` (exceto a geração da estrutura mínima do projeto NeoForge, caso a fase seguinte seja autorizada).

## Fases do Projeto
O desenvolvimento segue uma ordem estrita. O avanço para a próxima fase só é permitido após o registro explícito da mudança de fase neste arquivo.

0. [ ] **Auditoria (Atual):** Inventário de features, criação do grafo de dependências e levantamento de mixins/integrações.
1. [ ] **Esqueleto:** Configuração do projeto NeoForge 26.2 (compilando e abrindo o jogo com o mod carregado, sem conteúdo).
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

### 🔄 Em Andamento
- Nenhuma implementação em andamento. Mantida a Fase 0; a reorganização documental não inicia a Fase 1.

### ⏳ Pendente (Fase 0)
- Registrar explicitamente a mudança para a Fase 1 quando autorizada. Os checkboxes de `FEATURES.md` e dos catálogos continuam vazios porque controlam portagem, não auditoria.
- Preparar e validar o ambiente de build na Fase 1: ainda não existem `gradlew.bat`, `build.gradle` ou `build.gradle.kts` na raiz. Builds passam a ser obrigatórios a partir dessa fase, conforme orientação do usuário e `AGENTS.md`; a ausência atual não bloqueia tarefas documentais da Fase 0.

---

## Registro de Decisões

### ❓ Em Aberto
- **Ambiente da Fase 1:** Confirmar Java e Gradle e gerar o esqueleto na fase autorizada; validar compilação e cliente.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Versão alvo:** Minecraft 26.2 com NeoForge 26.2.0.93, verificada e confirmada pelo usuário em 30/10/2026. A confirmação da versão não substitui a verificação individual de APIs.
- **Limite de escopo:** Portar somente o conteúdo do mod até 0.6.x; não implementar versões posteriores nem restaurar funcionalidades apenas planejadas em comentários/TODOs.
- **Dependências:** `DEPENDENCIES.md` aprovado pelo usuário em 30/10/2026. Pontos de corte permanecem análise; distribuição em mod único mantida.
- **Builds:** Obrigatórios a partir da Fase 1. A tentativa anterior na Fase 0 falhou pela ausência do wrapper; não houve nova tentativa nesta reorganização documental.
- **Base de Referência:** Branch `1.12.2-0.6.X` do fork Snownee/Harvest-Festival (Licença MIT).
- **Estrutura Interna:** Mod único, organizado internamente com pacotes divididos por sistema.
- **Arquitetura de Compatibilidade:** Implementação baseada em dados e tags (JSON) desde o dia zero, evitando hardcoding.
- **Controle de Versão:** Commits, pushes e demais alterações de repositório Git serão executados de forma manual e exclusiva pelo desenvolvedor humano.
