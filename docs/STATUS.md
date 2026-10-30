# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 0: Auditoria**
- **Permitido:** Leitura do diretório `reference/original-1.12.2/` e geração de documentação no diretório `docs/`.
- **Bloqueado:** Criação de código de sistemas ou qualquer alteração no diretório `src/` (exceto a geração da estrutura mínima do projeto NeoForge, caso a fase seguinte seja autorizada).

## Fases do Projeto
O desenvolvimento segue uma ordem estrita. O avanço para a próxima fase só é permitido após o registro explícito da mudança de fase neste arquivo.

1. **Auditoria (Atual):** Inventário de features, criação do grafo de dependências e levantamento de mixins/integrações.
2. **Esqueleto:** Configuração do projeto NeoForge 26.2 (compilando e abrindo o jogo com o mod carregado, sem conteúdo).
3. **Base Compartilhada:** Implementação de registros, dados do jogador, calendário/estações e economia básica.
4. **Sistemas:** Implementação modular de sistemas (um por vez), seguindo estritamente a ordem definida em `DEPENDENCIES.md`.
5. **Polimento:** Integrações com outros mods e refinamentos finais.

---

## Acompanhamento de Tarefas

### ✅ Pronto
- Estrutura inicial do repositório (`.gitignore`, `AGENTS.md`, `docs/`).
- Definição das regras permanentes de desenvolvimento em `AGENTS.md`.

### 🔄 Em Andamento
- *Nenhuma tarefa no momento.*

### ⏳ Pendente (Fase 0)
- Gerar `docs/FEATURES.md` a partir de `reference/original-1.12.2/`, agrupando as features por sistema e incluindo checkboxes de controle.
- Gerar `docs/DEPENDENCIES.md` contendo o grafo de dependências entre os sistemas.
- Documentar em `docs/PORTING_NOTES.md` a lista de mixins do mod original e as integrações existentes com outros mods.
- Aprovação prévia e explícita do arquivo `DEPENDENCIES.md` (a ser registrada neste arquivo após revisão).

---

## Registro de Decisões

### ❓ Em Aberto
- **Arquitetura de Distribuição:** Confirmar se será mantido como mod único ou separado em múltiplos mods (decisão será tomada apenas após a aprovação de `DEPENDENCIES.md`).
- **Stack Tecnológico (26.2):** Definir e confirmar a versão exata do NeoForge, Java e Gradle durante o início da Fase 1.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Base de Referência:** Branch `1.12.2-0.6.X` do fork Snownee/Harvest-Festival (Licença MIT).
- **Estrutura Interna:** Mod único, organizado internamente com pacotes divididos por sistema.
- **Arquitetura de Compatibilidade:** Implementação baseada em dados e tags (JSON) desde o dia zero, evitando hardcoding.
- **Controle de Versão:** Commits, pushes e demais alterações de repositório Git serão executados de forma manual e exclusiva pelo desenvolvedor humano.