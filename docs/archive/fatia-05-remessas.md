### Planejamento anterior — remessas e pagamento diário (histórico)

Escopo definido: depósito de produtos, fila persistente e crédito sem duplicação. Critérios: a fila sobrevive a salvar e reabrir o mundo; cada item é creditado uma única vez. Fora do escopo: preços por tags e aniversário. Nenhuma implementação de remessas foi realizada nesta atualização documental.

Permanecem pendentes: aniversário (definição conforme o original, persistência e sincronização), preços (registros por dados/tags e consulta de valores) e validação integrada (servidor dedicado, dois jogadores, permissões, reconexão e persistência).

Naquele planejamento, a lista de leitura estava em STATUS.md. O evento diário está em `src/main/java/harvestfestival/calendar/`, junto de `DailyUpdates` e `DailyTransition`; `src/main/java/harvestfestival/daily/` não existe. A implementação abaixo consumiu o evento existente, sem mudança de pacote.

## Fase 2 — remessas e pagamento diário implementados (04/11/2026)

### Comportamento desta fatia

- O usuário autorizou explicitamente preço gravado no próprio item para testes anteriores ao sistema por tags. Usa `minecraft:custom_data` com `SellValue` numérico positivo, equivalente ao override original. Itens vazios, sem preço, com zero ou valor negativo são recusados sem consumo. Não existe catálogo de produtos, ID fixo de produto vendável ou registro de preços nesta implementação.
- Referências consultadas: `BlockStorage.hasShippedItem/onBlockActivated`, `TileShipping`, `ShippingRegistry.getSellValue`, `TrackingServer.addForShipping/newDay`, `PlayerTrackerServer.newDay`, `NewDayHandler` e `EntityHelper.getPlayerFromUUID`. O depósito manual original envia uma unidade para quem interage, independentemente do dono da caixa; fixa o preço no depósito. O port preserva esse caminho, incluindo consumir uma unidade no criativo. Agachar não deposita.
- `harvestfestival:shipping_box` é um bloco simples, sem tela ou entidade de bloco. Não armazena a fila no chunk: o attachment `harvestfestival:shipping_queue` pertence ao jogador e usa `copyOnDeath`. Quebrar a caixa ou descarregar seu chunk não elimina as remessas. Modelo provisório usa textura vanilla de barril; tag `minecraft:mineable/axe` e loot table em JSON. Disponível por `/give`, sem receita nesta fatia.
- Cada entrada contém cópia de uma unidade do item, incluindo componentes, e valor `long` fixado no depósito. A fila usa snapshots imutáveis; alterar o item restante não muda o preço aceito. A fila é consumida no servidor pelo evento diário existente, e a soma usa `PlayerGold.addTo`, também utilizado por `PlayerGold.add`; não há segundo saldo nem segunda implementação da aritmética. Preservados os valores negativos e overflow de `long` do saldo original.
- O consumidor percorre jogadores conectados. Sem jogador conectado, sua fila permanece salva e aguarda um novo evento diário com ele presente; entrar ou reabrir o mundo não dispara pagamento por si só. Não foram adicionados pagamentos retroativos, manipulação de arquivos de jogadores offline nem mudança no detector diário.
- Após consumir a fila, eventos repetidos encontram-na vazia e não pagam novamente os mesmos itens. A garantia é por remessa consumida, não um bloqueio global por data: itens novos entram numa nova fila. Ouro e fila são serializados no mesmo NBT de jogador, e a atualização síncrona ocorre na thread do servidor sem salvamento intermediário. Isso cobre salvamento normal e reabertura; não é promessa de transação durável contra crash, falha de disco ou exceções de outros mods.
- Sem novos pacotes, mixins, integrações, HUD, telas, cultivos, animais, aniversário ou preços por tags. Outros caminhos originais (funis, itens lançados e cesta), proprietário da caixa para automação e histórico de remessas permanecem fora desta implementação manual.

### APIs verificadas e testes

Fontes locais exatas: `minecraft-patched-26.2.0.93-sources.jar` e `neoforge-26.2.0.93-sources.jar`. Conferidos `BlockBehaviour.useItemOn`, `DeferredRegister.Blocks.registerBlock`, `Items.registerSimpleBlockItem`, `ItemStack.CODEC/copyWithCount`, `DataComponents.CUSTOM_DATA`, `CustomData.copyTag`, `CompoundTag.getLongOr`, `ExtraCodecs.POSITIVE_LONG`, `AttachmentType.Builder.serialize/copyOnDeath`, `TagValueOutput.createWithContext` e `RegistryAccess.fromRegistryOfRegistries`. Compilação confirma as chamadas usadas. O codec inicialmente tentado `Codec.longRange` não existe nesta versão e foi substituído pelo `ExtraCodecs.POSITIVE_LONG` verificado.

`PlayerShippingTest` acrescenta seis testes: depósito unitário e isolamento da cópia/preço; rejeição sem consumo; persistência da fila e saldo em arquivo NBT comprimido; pagamentos repetidos antes/depois de nova leitura; isolamento entre titulares e fila ausente; reutilização da aritmética de ouro com overflow. Os testes exercitam as funções usadas pelo depósito e pelo consumidor diário, usando os attachments registrados. Não simulam interação de cliente nem publicam um evento com servidor real.

O launcher unitário carrega os registros, mas não vincula os componentes de itens de um mundo. A tentativa de inicializar todos os componentes via dados vanilla encontrou validação NeoForge em `HolderSet$1`; o teste final prepara somente o graveto de teste com `MAX_STACK_SIZE=64` por `Holder.Reference.bindComponents`. Isso fica exclusivamente em `src/test`, sem alteração da inicialização do jogo. O codec real de `ItemStack`, os componentes personalizados e a serialização dos attachments continuam exercitados.

Build da implementação aprovado com 19 testes, zero falhas e zero erros. Resultado: `BUILD SUCCESSFUL in 13s`. Última linha literal da saída: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`. O cache Gradle desta sessão estava em `C:\gradle-home`, e seu acesso exigiu execução autorizada fora da sandbox; não foi alterada configuração global. Procedimento de Java/TEMP/TMP/COMPAT descrito acima continua válido. O agente não executou `runClient` na implementação; a validação manual posterior pelo responsável está registrada abaixo.

### Validação informada pelo responsável — 05/11/2026

Fatia de remessas e pagamento concluída. Validação manual no jogo pelo responsável, com `runClient` em mundo single player:

- `/hf gold set`, caixa colocada no chão, graveto validado e valor consultado por `/hf gold get`.
- Valor mantido após morte e destruição da caixa.
- Sair e reabrir o mundo.
- `/hf newDay` resultou em valores corretos e não duplicados, inclusive após sair e entrar no mundo.

O registro reproduz os cenários relatados, sem atribuir inspeção de rede ou testes multiplayer ao responsável. A saída de um novo build após essa validação não foi informada; o build e os 19 testes anteriores permanecem registrados como evidência da implementação.

Próxima fatia: **preços de cultivos**, apenas registrada, sem início de implementação. Permanecem pendentes: aniversário (definição conforme o original, persistência e sincronização) e validação integrada (servidor dedicado, dois jogadores, permissões, reconexão e persistência). Sair/reabrir single player não encerra a pendência de reconexão multiplayer.

### Roteiro manual de remessas

1. Executar `./gradlew.bat runClient` com o ambiente Windows documentado; abrir um mundo de teste com comandos.
2. Executar `/give @s harvestfestival:shipping_box` e colocar a caixa.
3. Executar `/hf gold set 100` e `/give @s minecraft:stick[minecraft:custom_data={SellValue:25L}] 3`.
4. Sem agachar, clicar uma vez na caixa com os gravetos. Devem restar dois; `/hf gold get` ainda deve mostrar 100. Um graveto comum sem preço não deve ser consumido.
5. Salvar/sair antes da virada e reabrir: o saldo ainda deve ser 100. Executar `/hf newDay` e aguardar o relógio avançar; consultar saldo 125.
6. Salvar/sair e reabrir novamente; executar outro `/hf newDay` sem depositar: saldo continua 125. A repetição direta da rotina de pagamento é coberta pelo teste automatizado.
7. Depositar os dois gravetos restantes em dois cliques e avançar o dia: saldo 175. Opcionalmente quebrar a caixa depois do depósito para verificar que a fila pertence ao jogador.

