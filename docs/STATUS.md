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

### 🔄 Em Andamento
- Auditoria documental da Fase 0: artefatos produzidos em 30/10/2026 e disponíveis para revisão; encerramento com build validado bloqueado pela ausência de projeto/wrapper Gradle na raiz.
- `FEATURES.md`: sistemas dos 19 diretórios imediatos do Java, variantes de itens/blocos, registros de conteúdo, 123 missões anotadas e recursos de comportamento inventariados; integrações externas marcadas como adiadas.
- `DEPENDENCIES.md`: grafo funcional, matriz de imports, ciclos, ordem sugerida de fatias e pontos de corte documentados. **Ainda não aprovado.**
- `PORTING_NOTES.md`: os 2 mixins do original documentados com alvos/efeitos, alternativa climática consultada em fonte oficial 26.2.x e limites de verificação registrados.

### ⏳ Pendente (Fase 0)
- Revisar os artefatos de auditoria produzidos; os checkboxes de `FEATURES.md` continuam vazios porque controlam portagem, não auditoria.
- Validação de build: `.\gradlew.bat build` foi tentado na raiz em 30/10/2026 e falhou porque o arquivo não existe. `runClient` não executado. A criação do esqueleto pertence à fase seguinte e não foi realizada.
- Aprovação prévia e explícita do arquivo `DEPENDENCIES.md` (a ser registrada neste arquivo após revisão).

---

## Registro de Decisões

### ❓ Em Aberto
- **Revisão arquitetural:** Aprovar `DEPENDENCIES.md`; pontos de corte são apenas análise. Mantida a decisão de mod único de `AGENTS.md`.
- **Stack Tecnológico (26.2):** Confirmar a compatibilidade da combinação Minecraft 26.2 / NeoForge 26.3.0.99 indicada em `AGENTS.md`, distinguindo-a da consulta de APIs NeoForge 26.2 solicitada nesta auditoria; confirmar Java e Gradle antes da Fase 1. Nenhuma versão foi alterada pela auditoria.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Base de Referência:** Branch `1.12.2-0.6.X` do fork Snownee/Harvest-Festival (Licença MIT).
- **Estrutura Interna:** Mod único, organizado internamente com pacotes divididos por sistema.
- **Arquitetura de Compatibilidade:** Implementação baseada em dados e tags (JSON) desde o dia zero, evitando hardcoding.
- **Controle de Versão:** Commits, pushes e demais alterações de repositório Git serão executados de forma manual e exclusiva pelo desenvolvedor humano.
