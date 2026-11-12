# Remessas e preços — contratos vigentes

Consultar para vendas, fila, preços e pagamentos.

### Comportamento desta fatia

- O usuário autorizou explicitamente preço gravado no próprio item para testes anteriores ao sistema por tags. Usa `minecraft:custom_data` com `SellValue` numérico positivo, equivalente ao override original. Itens vazios, sem preço, com zero ou valor negativo são recusados sem consumo. Os preços por item/tag são definidos na seção de regras abaixo.
- Referências consultadas: `BlockStorage.hasShippedItem/onBlockActivated`, `TileShipping`, `ShippingRegistry.getSellValue`, `TrackingServer.addForShipping/newDay`, `PlayerTrackerServer.newDay`, `NewDayHandler` e `EntityHelper.getPlayerFromUUID`. O depósito manual original envia uma unidade para quem interage, independentemente do dono da caixa; fixa o preço no depósito. O port preserva esse caminho, incluindo consumir uma unidade no criativo. Agachar não deposita.
- `harvestfestival:shipping_box` é um bloco simples, sem tela ou entidade de bloco. Não armazena a fila no chunk: o attachment `harvestfestival:shipping_queue` pertence ao jogador e usa `copyOnDeath`. Quebrar a caixa ou descarregar seu chunk não elimina as remessas. Modelo provisório usa textura vanilla de barril; tag `minecraft:mineable/axe` e loot table em JSON. Disponível por `/give`, sem receita nesta fatia.
- Cada entrada contém cópia de uma unidade do item, incluindo componentes, e valor `long` fixado no depósito. A fila usa snapshots imutáveis; alterar o item restante não muda o preço aceito. A fila é consumida no servidor pelo evento diário existente, e a soma usa `PlayerGold.addTo`, também utilizado por `PlayerGold.add`; não há segundo saldo nem segunda implementação da aritmética. Preservados os valores negativos e overflow de `long` do saldo original.
- O consumidor percorre jogadores conectados. Sem jogador conectado, sua fila permanece salva e aguarda um novo evento diário com ele presente; entrar ou reabrir o mundo não dispara pagamento por si só. Não foram adicionados pagamentos retroativos, manipulação de arquivos de jogadores offline nem mudança no detector diário.
- Após consumir a fila, eventos repetidos encontram-na vazia e não pagam novamente os mesmos itens. A garantia é por remessa consumida, não um bloqueio global por data: itens novos entram numa nova fila. Ouro e fila são serializados no mesmo NBT de jogador, e a atualização síncrona ocorre na thread do servidor sem salvamento intermediário. Isso cobre salvamento normal e reabertura; não é promessa de transação durável contra crash, falha de disco ou exceções de outros mods.
- Outros caminhos originais (funis, itens lançados e cesta), proprietário da caixa para automação e histórico de remessas permanecem fora desta implementação manual.

### APIs verificadas e testes

Fontes locais exatas: `minecraft-patched-26.2.0.93-sources.jar` e `neoforge-26.2.0.93-sources.jar`. Conferidos `BlockBehaviour.useItemOn`, `DeferredRegister.Blocks.registerBlock`, `Items.registerSimpleBlockItem`, `ItemStack.CODEC/copyWithCount`, `DataComponents.CUSTOM_DATA`, `CustomData.copyTag`, `CompoundTag.getLongOr`, `ExtraCodecs.POSITIVE_LONG`, `AttachmentType.Builder.serialize/copyOnDeath`, `TagValueOutput.createWithContext` e `RegistryAccess.fromRegistryOfRegistries`. Compilação confirma as chamadas usadas. O codec inicialmente tentado `Codec.longRange` não existe nesta versão e foi substituído pelo `ExtraCodecs.POSITIVE_LONG` verificado.

`PlayerShippingTest` acrescenta seis testes: depósito unitário e isolamento da cópia/preço; rejeição sem consumo; persistência da fila e saldo em arquivo NBT comprimido; pagamentos repetidos antes/depois de nova leitura; isolamento entre titulares e fila ausente; reutilização da aritmética de ouro com overflow. Os testes exercitam as funções usadas pelo depósito e pelo consumidor diário, usando os attachments registrados. Não simulam interação de cliente nem publicam um evento com servidor real.

O launcher unitário carrega os registros, mas não vincula os componentes de itens de um mundo. A tentativa de inicializar todos os componentes via dados vanilla encontrou validação NeoForge em `HolderSet$1`; o teste final prepara somente o graveto de teste com `MAX_STACK_SIZE=64` por `Holder.Reference.bindComponents`. Isso fica exclusivamente em `src/test`, sem alteração da inicialização do jogo. O codec real de `ItemStack`, os componentes personalizados e a serialização dos attachments continuam exercitados.

## Regras de preços

Uma regra por recurso em `data/harvestfestival/shipping_prices/items/<namespace>/<caminho>.json` ou `tags/<namespace>/<caminho>.json` sob o mesmo diretório de preços. O datapack de maior precedência fornece o arquivo efetivo para cada caminho. A recarga recompõe a tabela do zero e a publica inteira. Se o arquivo efetivo for inválido, aquela regra é descartada com aviso e as demais continuam disponíveis. Um arquivo removido perde efeito na próxima recarga, respeitada a precedência dos packs.

`price` é inteiro positivo até o máximo de long. A regra de tag exige também `priority`, inteiro de 32 bits com sinal. Vale primeiro o item específico. Entre as tags vigentes do item vale a maior prioridade e, em empate nessa prioridade, o maior preço. O aviso identifica as tags empatadas e sai uma vez por item por recarga. Sem regra aplicável, usa-se a leitura existente de SellValue e exige-se valor positivo. O depósito grava o preço na fila e o pagamento usa o valor gravado.

No original, ShippingRegistry consulta SellValue antes do registro. A precedência desta fatia foi definida pelo responsável. HFCrops usa os preços de venda de Crop e registra os produtos. A tabela inicial aproveita somente os valores de cenoura (160 por item) e trigo (100 pela tag comum c:crops/wheat).

APIs conferidas nos artefatos locais de 26.2.0.93 e exercitadas pelo build: AddServerReloadListenersEvent, SortedReloadListenerEvent.addListener, SimplePreparableReloadListener.prepare/apply, ResourceManager.listResources, BuiltInRegistries.ITEM.getOptional e Holder.is(TagKey). A busca opcional permite rejeitar um identificador desconhecido antes da inclusão na tabela.

## Atualizações e validação

SellValue é fallback autorizado; precedência item > tag > SellValue definida pelo responsável. Fatia 5: 6 testes novos, suíte total 19. Fatia 6: 7 novos, suíte total 26; builds e single player aprovados. Cobertura dedicada aceita conforme [fatia 8](../archive/fatia-08-validacao-integrada.md). Pagamento apenas na próxima virada com jogador conectado aceito em 08/11/2026; não paga offline nem ao reconectar.
