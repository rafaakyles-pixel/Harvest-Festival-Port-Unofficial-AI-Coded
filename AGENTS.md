# Diretrizes e regras fundamentais para o desenvolvimento e portabilidade do projeto. 

**Alteração deste arquivo:** `AGENTS.md` só pode ser editado mediante autorização explícita do responsável para a alteração em questão. Autorizações para implementar features, corrigir bugs ou atualizar outros documentos não autorizam modificar este arquivo. A autorização de uma edição não se estende a edições futuras.

## 1. Visão Geral do Projeto
Portabilidade fiel em gameplay do mod Harvest Festival.
- **Fidelidade obrigatória:** O projeto deve se basear completamente no comportamento do mod original indicado abaixo. Toda mecânica a acrescentar, completar ou alterar no port deve ter seu funcionamento conferido no original antes da implementação; não presumir regras vanilla, convenções de outros mods ou comportamentos considerados mais convenientes.
- **Base original:** Branch 1.12.2-0.6.X do repositório Snownee/Harvest-Festival (fork de joshiejack/Harvest-Festival, licença MIT).
- **Alvo:** Minecraft 26.2 com NeoForge 26.2.0.93.
- **Estrutura:** Um único mod contendo todas as features, organizado internamente em pacotes divididos por sistema.
- **Escopo:** Uso pessoal, sem publicação.

## 2. Fluxo de Início de Tarefa (Obrigatório)
Antes de escrever qualquer código, execute os seguintes passos na ordem:
1. Leia este arquivo, `docs/STATUS.md` e `docs/PORTING_NOTES.md`.
2. Leia `docs/FEATURES.md` se a tarefa atual envolver a implementação de features.
3. Leia apenas o catálogo necessário, se houver: - `docs/catalogs/variants.md`: variantes de itens, blocos e ferramentas. - `docs/catalogs/content-registries.md`: identificadores de registro de conteúdo. - `docs/catalogs/quests.md`: missões anotadas. - `docs/catalogs/resources.md`: advancements, edifícios, festivais, árvores e loot tables.
4. Consulte apenas o código de referência estritamente necessário para comprovar a mecânica em questão, incluindo condições, configurações padrão, exceções e interações relevantes. Quando o comportamento for herdado do Minecraft/Forge original, confira também essa implementação; não o deduza pela versão alvo. Se a leitura não esclarecer o funcionamento, valide no jogo original quando possível ou apresente a incerteza ao responsável antes de implementar.

**Atenção:** A fase atual do projeto e os pacotes permitidos estão definidos em `docs/STATUS.md`. Se a tarefa solicitada exigir ações fora da fase atual, interrompa o processo e solicite confirmação explícita antes de prosseguir.

## 3. Estrutura de Diretórios
- `reference/original-1.12.2/`: Código do mod original. **Estritamente leitura.**
- `src/`: Código novo. Único diretório onde a escrita de código é permitida.
- `docs/FEATURES.md`: Inventário de features por sistema, integrações adiadas e cobertura.
- `docs/catalogs/`: catálogos de consulta, lidos somente quando necessário: 
	 -  `variants.md`, `content-registries.md`, `quests.md`, `resources.md`.
- `docs/DEPENDENCIES.md`: Grafo de dependências entre os sistemas do mod.
- `docs/PORTING_NOTES.md`: Registro de equivalências (1.12.2 → 26.2), mixins utilizados, decisões técnicas e integrações com outros mods.
- `docs/STATUS.md`: Controle de fases (pronto, em andamento, pendente, decisões em aberto).

## 4. Regras de Código e Modding
- **Evidência de gameplay:** Registre no contrato pertinente em `docs/porting/` as fontes consultadas e o comportamento comprovado. Diferencie leitura de código de observação em jogo; nunca atribua testes manuais não realizados. Mecânica ausente no original, divergência intencional ou simplificação de comportamento exige decisão explícita do responsável antes da implementação. Recortes de fatia delimitam o que será entregue, mas não autorizam inventar o funcionamento das partes incluídas.
- **Uso de APIs:** Não invente APIs de Minecraft ou NeoForge. Verifique a documentação oficial ou o código-fonte da versão alvo. Se a verificação for impossível, registre a incerteza como "não verificado" em `PORTING_NOTES.md`.
- **Sistemas Modernos:** Utilize sempre o equivalente moderno do NeoForge 26.2 (eventos, capabilities, registros, data packs) em vez de tentar replicar mecanismos obsoletos do Forge 1.12.2. A adaptação técnica deve preservar o gameplay comprovado do original; necessidade de outra API não autoriza mudar a mecânica sem decisão explícita do responsável.
- **Data-Driven:** Priorize dados em vez de código hardcoded. Receitas, loot tables, tags, traduções (lang) e configurações devem ser implementados em JSON.
- **Tags:** É obrigatório o uso de tags para itens e blocos (sementes, cultivos, minérios, comidas, etc.) em vez de IDs fixos. Esta é a base para compatibilidade.
- **Mixins:** Devem ser utilizados como último recurso, apenas quando não houver alternativa viável via API ou evento. Todo novo mixin deve ser documentado em `PORTING_NOTES.md` detalhando o alvo e o motivo.
- **Integrações:** Integrações com mods específicos ficam listadas como "adiadas" em `FEATURES.md` e só devem ser implementadas mediante solicitação explícita.
- **Escopo:** Não refatore nem altere código que não faça parte do escopo direto da tarefa atual.
- **Dúvidas:** Se houver ambiguidade sobre comportamento do original, escolha de API ou decisão de escopo, pergunte antes de implementar. Não decida sozinho.

## 5. Pacotes e Arquitetura
- A criação de novos pacotes de sistema (ex: plantação, mineração, NPCs) é condicionada à existência prévia e aprovação do arquivo `docs/DEPENDENCIES.md`, com o devido registro em `docs/STATUS.md`.
- Cada tarefa deve consistir em uma fatia vertical pequena e de escopo fechado, contendo critérios de conclusão verificáveis definidos na própria solicitação.

## 6. Build e Validação
- **Obrigatório:** O build deve ser executado antes da conclusão de qualquer tarefa, válido a partir da fase 1. Corrija falhas antes de reportar.
- **Comandos (Windows):** `.\gradlew.bat build` e `.\gradlew.bat runClient`.
- Limitações de verificação (exemplo: impossibilidade de abrir o cliente do jogo no ambiente atual) devem ser relatadas explicitamente na resposta.
- Nunca declare uma tarefa como concluída com sucesso sem ter validado o build.

## 7. Controle de Versão (Git) e Entregáveis
- **Proibição:** Não execute comandos que alterem o repositório Git (`git add`, `git commit`, `git push`, `git rm`, `git reset`, `git checkout`, `git stash`, etc.).
- Ao finalizar uma tarefa, forneça como resposta:
  1. A lista exata de arquivos criados ou modificados.
  2. Um resumo técnico das alterações (máximo de 5 linhas).
  3. Instruções de verificação (comando ou ação).
  4. Uma sugestão de mensagem de commit (em português), seguindo o formato `tipo: descrição curta da mudança`.
- Atualize `docs/STATUS.md` e `docs/PORTING_NOTES.md` sempre que a tarefa impactar o status estrutural ou técnico do projeto.

## 8. Convenções de Idioma
- Toda a documentação (arquivos `.md`) e mensagens de commit devem ser escritas em **Português**.
- Nomes de classes, pacotes, métodos, variáveis e identificadores devem seguir as convenções Java e Minecraft, escritos em **Inglês**.

## 9. Manutenção da documentação (controle de tamanho)

- **Leitura inicial:** leia `AGENTS.md`, `docs/STATUS.md` e `docs/PORTING_NOTES.md`. Leia somente os contratos de `docs/porting/` indicados pelo índice e pertinentes ao sistema alterado ou consumido. `docs/archive/` só é consultado quando necessário para evidência histórica ou procedimento referenciado.
- **PORTING_NOTES é uma entrada estável:** escopo, ambiente consolidado, decisões transversais, pendências obrigatórias e índice por sistema. Não acrescentar uma seção por fatia nem tabela global acumulativa de APIs. Meta de até 200 linhas, sem omitir informação nem juntar parágrafos artificialmente para cumprir contagem; detalhes vigentes vão ao contrato pertinente.
- **Ao concluir uma fatia**, atualizar no lugar o contrato de `docs/porting/<sistema>.md`: comportamento final, decisões do responsável, formato persistido, API pública, APIs verificadas com versão/evidência, limitações e validação. Evitar duplicar APIs já documentadas. Cerca de 15 linhas é meta para resumo, somente quando não omitir informações; contratos completos não têm esse limite.
- **Contagens de testes:** distinguir novos testes da suíte total e execução efetiva de resultados em cache. Não ampliar cobertura manual a partir de build ou codec.
- **Não registrar** em STATUS/PORTING_NOTES: roteiros manuais, horários/logs intermediários, tentativas falhas ou última linha literal do build. Roteiros novos vão na resposta; pendências obrigatórias e procedimentos reutilizáveis ficam no arquivo histórico pertinente ou seção de ambiente, com link.
- **Histórico:** evidências, incidentes e decisões substituídas vão para `docs/archive/fatia-NN-nome.md`, nunca apagados sem aprovação; referenciar no STATUS diretamente ou pelo índice histórico. Contratos ainda consumidos pertencem a `docs/porting/`, não exclusivamente ao arquivo histórico.
- **Ambiente:** consolidar procedimento e limitações no PORTING_NOTES; não repetir por fatia. Preservar evidências antigas no histórico.
- **Compactação existente:** exige tarefa própria autorizada e preservação de decisões, pendências obrigatórias (inclusive cenário 8), limitações e rastreabilidade. A manutenção rotineira dos contratos e índice segue esta regra sem exigir novas compactações periódicas.
