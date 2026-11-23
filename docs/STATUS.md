# Controle de estado, acompanhamento de tarefas e registro de decisões do projeto. O avanço de fases e a aprovação de artefatos devem ser registrados obrigatoriamente neste arquivo.

## Fase Atual
**Fase 3: Sistemas** — avanço autorizado explicitamente pelo responsável em 08/11/2026. Fase 2 e oitava fatia concluídas com ressalva do cenário 8.
- **Aceite:** cenários 1–7 e 9 aceitos conforme a cobertura e limitações da tabela. Mantido o comportamento offline observado: remessas pagam na próxima virada com o jogador conectado; nenhuma correção solicitada.
- **Pendência obrigatória:** cenário 8 permanece **não executado**, a verificar no primeiro consumidor cliente (HUD/interface): ouro e aniversário do titular, isolamento entre jogadores, atualização, limpeza e reconexão. Mensagens administrativas e testes de codec não comprovam o espelho.
- **Pacotes já aprovados:** `harvestfestival`, `harvestfestival.player`, `harvestfestival.player.command`, `harvestfestival.calendar`, `harvestfestival.calendar.command`, `harvestfestival.tools` (09/11/2026) e `harvestfestival.crops` (11/11/2026), produção e testes. Subpacote interno `harvestfestival.tools.mixin` aprovado explicitamente na fatia 11 para isolar o mixin de umidade.
- **Fatia 9 concluída:** responsável confirmou teste validado e fatia aprovada em 09/11/2026. Enxada/regador de nível único, solo mínimo `prepared_soil`, regras por tags e água persistente no item aceitos em single player. Servidor dedicado e multiplayer permanecem pendentes. Complemento visual do regador solicitado separadamente; cultivo permanece na fatia 10.
- **Aceite explícito do responsável:** remessa de jogador offline na virada só é paga na próxima virada com ele conectado; comportamento adotado.
- **Critério de aceite futuro obrigatório:** o cenário 8 (espelho cliente de ouro e aniversário) deve integrar os critérios de aceite da primeira fatia que introduzir HUD ou interface.
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

### Fatia 20 — aceite global; produtor independente aprovado

Em 23/11/2026, responsável aprovou registro persistente único por árvore para manter frutos após cortar base/topo, sem reproduzir produtores por folhas/toras, e confirmou migração conservadora: árvores antigas com base preservam progresso e vinculam somente base/topo HF; antigas sem base exigem replantio. Árvores novas vinculam as toras efetivamente geradas; último vínculo removido encerra produção. Implementado e aceite global informado para a fatia 20, sem presumir reteste manual ou agricultura inteira concluída. Contrato vigente em [Árvores frutíferas](porting/arvores-frutiferas.md). Build aprovado: 284 testes efetivamente executados, nove novos sobre os 275 anteriores, zero falhas/erros/ignorados; quatro tarefas executadas e cinco UP-TO-DATE. runClient/original não executados neste complemento. Cenário 8 não executado, dedicado/multiplayer pendentes e camas por dimensão adiadas permanecem.

Em 23/11/2026, responsável informou que por enquanto está bom e aprovou a fatia após os ajustes solicitados: Silk Touch coleta folhas; metades maduras do tronco independentes, mantendo o par juvenil após confirmação específica. Alterações implementadas e build aprovado: 275 testes efetivamente executados (um novo e um existente ampliado), zero falhas/erros/ignorados; duas tarefas executadas e sete UP-TO-DATE. runClient não executado neste ajuste; nenhum resultado individual adicional nem reteste manual atribuído ao responsável. Cenário 8, dedicado/multiplayer e camas por dimensão mantêm suas pendências.

Responsável confirmou as decisões específicas de dimensões/Plains, atuação sem proprietário, correção da espécie das folhas, atualização integral do tanque e destinos/remessa. Após esclarecimento, aprovou exigir espaço para crescer, incluindo copa integral antes de amadurecer, sem sobrescrever obstáculos. Implementação de 20-A–E autorizada e entregue; critérios e fronteiras nos contratos. Aprovação não registra execução de testes ainda não realizados.

Revisão de 21/11/2026 por leitura do original, herança Forge e fontes exatas do alvo: [Árvores frutíferas](porting/arvores-frutiferas.md) e [Aspersores](porting/aspersores.md). Escopo geral da 20 aprovado: completar cinco árvores e ambos os aspersores, sem infraestrutura sem consumidor. Distribuição implementada: 20-A infraestrutura funcional de ambos com pessegueiro e aspersor velho definitivos; 20-B completa verão com laranja/banana e regressão do pêssego; 20-C completa outono com maçã/uva; 20-D entrega ferro e fecha as duas variantes; 20-E integra e corrige lacunas internas. Cada entrega tem comportamento testável, dados/recursos, persistência e validação próprios. Proposta anterior A–G preservada nos contratos como substituída; sem arquivo de plano ou compactação; código somente nos pacotes existentes.

Decisões específicas aprovadas e implementadas: árvores com estação nula/Plains, proteção do topo/template/limpeza acima e chunks, atuação autônoma sem dono versus permissões por proprietário, correção da espécie nos bits antigos de folhas, sincronização do tanque após toda alteração e destinos criativos/remessa da maçã. Suporte Plains, quebra/Silk Touch, callbacks de folhas/templates, clamp e transferência transacional foram conferidos por leitura; critérios concretos nos contratos, sem alegar teste em jogo. Hidratação da máquina original independe de disable_growth por caminho próprio; não é extensão automática da exceção dos regadores. Lojas, NPCs/missões e aquisição normal permanecem excluídos; NPCs começam na 21. 19-B integralmente adiada, inclusive saco HF, sem tocar nether wart vanilla; decisões específicas “reavaliar se retomada”.

Validação documental inicial: build aprovado, uma tarefa executada e oito UP-TO-DATE; test reutilizou 251 resultados anteriores. **Revisão documental anterior:** build aprovado, nove tarefas UP-TO-DATE, inclusive test; 251 resultados anteriores reutilizados, zero falhas/erros/ignorados, nenhum teste novo ou reexecutado. Nenhum jogo executado naquela preparação.

**Entrega da implementação:** builds próprios de A, B, C e D aprovados, respectivamente 261/262/264/267 testes executados. Validação final da E: 274 testes efetivamente executados, 23 novos sobre a base de 251, zero falhas/erros/ignorados; duas tarefas executadas e sete UP-TO-DATE. runClient iniciou e carregou os recursos finais sem erro de modelo/textura nos logs, mas controle visual da janela Java não foi autorizado pela ferramenta. Não houve teste manual de gameplay, inspeção visual ou execução do original. Naquela entrega, os aceites manuais permaneciam pendentes; o aceite global posterior está registrado acima, sem atribuir resultados individuais. Não confundir inicialização com validação funcional em mundo. Cenário 8 não executado, dedicado/multiplayer pendentes, indicadores do regador funcionais e camas por dimensão adiadas permanecem. Não declara sistema agrícola inteiro concluído.


Confirmação final após ajuste apenas de whitespace: build aprovado, uma tarefa executada e oito UP-TO-DATE; test reutilizou os mesmos 274 resultados da execução efetiva anterior, sem testes adicionais.

### Fatia 19 — 19-A aceita globalmente; 19-B integralmente adiada

**Aceite global informado nesta revisão pelo responsável:** 19-A completamente aprovada; Nabo Robusto plantado normalmente. Sucede a pendência manual da entrega abaixo, sem inventar resultados individuais dos demais testes nem atribuir execução ao agente. Cenário 8 não executado, dedicado/multiplayer pendentes e camas por dimensão adiadas permanecem.

**Decisão posterior do responsável:** implementar somente 19-A completa, ampliada para estação nula no Nether/End em todos os cultivos HF. 19-B integralmente adiada, inclusive saco HF; manter crescimento vanilla. Modelos vanilla e recusa de estágios HF 3/4: **“reavaliar se retomada”**. Essa decisão sucede o escopo anterior abaixo, cujo levantamento fica preservado. Implementação e cobertura vigentes em [Cultivos](porting/cultivos.md#19-a--entrega-autorizada-e-decisão-dimensional-ampliada).

19-A entregue nos pacotes existentes: semente/produto Nabo Robusto, dados/loot/tags/preço 10, recursos originais, abas e slots 107–109; estação nula para todos os HF no Nether/End, sem alterar calendário/vanilla. Build aprovado com 251 testes efetivamente executados (10 novos), zero falhas/erros/ignorados; cinco tarefas executadas e quatro UP-TO-DATE. Cliente/original não executados; aceite manual pendente. Cenário 8 não executado, dedicado/multiplayer pendentes e camas por dimensão adiadas permanecem. Não conclui agricultura inteira.

### Sequência aprovada pelo responsável — 19 → 20 → 21

- Fatia 19: escopo aprovado para nabo de tutorial e nether wart, conforme o prompt apresentado. Conferir o original e apresentar os recortes concretos antes de escrever código; a aprovação de escopo já está dada, sem necessidade de repeti-la. Ajustes de recorte podem acomodar esta sequência; ambiguidades e divergências de gameplay continuam exigindo decisão explícita.
- Fatia 20: concluir árvores frutíferas e aspersores em entregas menores, nomeadas 20-A, 20-B e assim por diante. Número, conteúdo e ordem dos subrecortes serão definidos pelo levantamento do original e das dependências, com critérios e validação por entrega; não presumir que um único subrecorte cobre todas as árvores ou todos os aspersores.
- Fatia 21: iniciar o desenvolvimento de NPCs. Essa prioridade sucede a sugestão de iniciar NPCs na 20 e antecipa seu início em relação à ordem geral sugerida no grafo. Delimitar as dependências necessárias de cidade, residência, diálogo e progressão sem antecipar automaticamente os sistemas completos.
- A Fase 3 permanece vigente. A sequência não declara concluídos os demais sistemas ou complementos agrícolas, nem aprova APIs, novos pacotes concretos ou divergências ainda não examinados. Milho de duas camadas continua adiado até texturas e recorte próprios; demais pendências obrigatórias permanecem.

Validação desta atualização de sequência: build aprovado, nove tarefas UP-TO-DATE, incluindo test; nenhum teste novo ou reexecutado, 241 resultados anteriores reutilizados. Somente STATUS, FEATURES, DEPENDENCIES e PORTING_NOTES alterados nesta atualização; sem implementação ou execução de cliente.

### Resumo de Fatias Concluídas

| Fatia | Sistema / Escopo | Aceite / Data | Documento de referência |
| --- | --- | --- | --- |
| 1 | Saldo por jogador (`PlayerGold`) e comandos administrativos | 02/11/2026 | [fatia-01-ouro.md](archive/fatia-01-ouro.md) |
| 2–3 | Calendário básico persistente e transição diária (`NewDayEvent`) | 02/11/2026 | [fatias-02-03-calendario.md](archive/fatias-02-03-calendario.md) |
| 4 | Sincronização de saldo com o titular | 04/11/2026 | [fatia-04-sincronizacao.md](archive/fatia-04-sincronizacao.md) |
| 5 | Caixa de remessas e pagamento na virada do dia | 05/11/2026 | [fatia-05-remessas.md](archive/fatia-05-remessas.md) |
| 6 | Preços por datapack recarregáveis e tags de item | 06/11/2026 | [fatia-06-precos.md](archive/fatia-06-precos.md) |
| 7 | Aniversário do jogador e comandos de consulta | 08/11/2026 | [fatia-07-aniversarios.md](archive/fatia-07-aniversarios.md) |
| 8 | Validação integrada dedicada/multiplayer (2 clientes, Fase 2 encerrada) | 08/11/2026 | [fatia-08-validacao-integrada.md](archive/fatia-08-validacao-integrada.md) |
| 9 | Enxada e regador básicos, solo preparado (`PreparedSoil`) | 09/11/2026 | [fatia-09-ferramentas.md](archive/fatia-09-ferramentas.md) |
| 10 | Nabo comum, ciclo de cultivo de 4 dias e colheita | 12/11/2026 | [cultivos.md](porting/cultivos.md) |
| 11 | Farmland compartilhada e mixin de umidade (`FarmlandBlockMixin`) | 12/11/2026 | [ferramentas.md](porting/ferramentas.md) |
| 12 | Pepino, rebrota limitada (8 colheitas) e correções de estágio | 14/11/2026 | [cultivos.md](porting/cultivos.md) |
| 13 | Trigo, corte por foice e abas criativas | 14/11/2026 | [cultivos.md](porting/cultivos.md) |
| 14 | Cebola, espinafre, esforço, desmaio e sono instantâneo | 15/11/2026 | [ferramentas.md](porting/ferramentas.md) |
| 15 | Tiers de enxada/regador até prata (15-F), cenoura/beterraba/batata (15-A), política vanilla (15-V) | 16/11/2026 | [ferramentas.md](porting/ferramentas.md) / [cultivos.md](porting/cultivos.md) |
| 16 | Morango, tomate, berinjela e foices de cobre/prata | 16/11/2026 | [cultivos.md](porting/cultivos.md) / [ferramentas.md](porting/ferramentas.md) |
| 17 | Repolho, abacaxi, milho, batata-doce, pimentão e ferramentas ouro/mystril | 17/11/2026 | [cultivos.md](porting/cultivos.md) / [ferramentas.md](porting/ferramentas.md) |
| 18 | Melancia, abóbora e grama dupla de corte único | 18/11/2026 | [cultivos.md](porting/cultivos.md) |
| 19 | Nabo Robusto (19-A) com estação nula dimensional; 19-B adiada | 21/11/2026 | [cultivos.md](porting/cultivos.md#19-a--entrega-autorizada-e-decisão-dimensional-ampliada) |

> Histórico detalhado das fatias 10 a 18 e registros de encerramento da Fase 2 preservados em [fatias-10-18-historico.md](archive/fatias-10-18-historico.md).

### 🔄 Em Andamento
- Atualização do responsável em 14/11/2026 nesta solicitação: indicadores do regador já conferidos e funcionais. Substitui a pendência visual nos registros anteriores, sem atribuir testes individuais ao agente. Cenário 8 e dedicado/multiplayer continuam pendentes.
- A sincronização do attachment de aniversário com o cliente foi verificada só por teste de codificação de rede (data e ausência), sem inspeção do espelho cliente. A verificação real fica pendente na validação integrada. A mensagem administrativa `true`/`false` foi observada no cliente, o que não comprova o espelho do attachment.
- Complemento visual autorizado após o aceite da fatia 9: barra de água no item e quantidade exata no tooltip do regador. Sem novo pacote ou alteração do estado persistido. Por introduzir interface, seus critérios de aceite incluem o cenário 8 de espelho cliente de ouro/aniversário, ainda não executado; o indicador de água não comprova esse cenário.

### ⏳ Próximos passos
- Próximo passo: responder somente às decisões específicas da 20 registradas nos contratos e implementar a 20-A funcional de árvores/aspersores conforme recorte apresentado na conversa. Escopo geral da 20 já aprovado; 19-A aceita globalmente e 19-B integralmente adiada. Complementos vanilla continuam separados em [FEATURES](FEATURES.md#sequência-agrícola-pendente).
- Executar cenário 8, ainda vinculado ao complemento visual do regador. Indicadores funcionais e texturas confirmados não comprovam o espelho cliente de ouro/aniversário.
- Obrigação da Fase 3 reafirmada pelo responsável em 12/11/2026: completar o sistema agrícola e seus itens/ferramentas, procurando paridade de gameplay com o original. Fatias continuam pequenas e verificáveis, mas seus mínimos não encerram o sistema nem transferem funções faltantes ao polimento. Próximas fatias devem tratar ferramentas/sementes vanilla, solo compartilhado, itens e comportamentos agrícolas restantes conforme FEATURES/DEPENDENCIES; dependências de lojas/NPCs permanecem ordenadas pelo grafo.
- Pendência de validação: cenário 8 no primeiro consumidor cliente. Escolha da data pelo jogador e efeitos continuam adiados.
- HUD/calendário visual continuam adiados por decisão do usuário.

---

## Registro de Decisões

### ❓ Em Aberto
- **Validação integrada:** cenário 8 pendente; demais resultados aceitos pelo responsável em 08/11/2026, sem ampliar a cobertura observada.
- **Referência Externa:** O conteúdo de `reference/piscary-1.16.5-pirate-treasure/` está fora do escopo até aprovação e direcionamento explícito.

### 🔒 Tomadas
- **Prompts de próximas fatias (14/11/2026):** preferência do responsável por prompts curtos, direcionados à fatia exata, com objetivo, escopo e critérios essenciais explícitos para evitar busca aberta de objetivos. Histórico de no máximo uma fatia anterior; referenciar os documentos vigentes em vez de repetir regras e contratos. Preservar apenas decisões e pendências indispensáveis ao recorte. O prompt já fornecido para a fatia 13 será utilizado como está; aplicar esta preferência aos próximos.
- **Texturas originais obrigatórias (13/11/2026):** aprovação da fatia 12 condicionada pelo responsável a que todo item novo de cada fatia venha com textura original. Pepino e saco de sementes cumprem a condição; exigência registrada sem modificar AGENTS.md. Não autoriza reabrir retroativamente outros itens fora do escopo.
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

---

## Validação e Encerramento da Fase 2

A Fase 2 e a oitava fatia foram concluídas com ressalva do cenário 8, e a Fase 3 foi autorizada em 08/11/2026. A tabela completa de validação integrada (cenários 1 a 7 e 9), comandos e evidências do servidor dedicado e clientes multiplayer estão preservados na íntegra em [fatia-08-validacao-integrada.md](archive/fatia-08-validacao-integrada.md). A documentação e os testes da fatia 9 estão preservados em [fatia-09-ferramentas.md](archive/fatia-09-ferramentas.md).
