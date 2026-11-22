# Inventário de funcionalidades do original

## Índice dos catálogos

- [Catálogo de variantes de itens, blocos e ferramentas](catalogs/variants.md)
- [Catálogo de registros de conteúdo](catalogs/content-registries.md)
- [Catálogo completo de missões anotadas](catalogs/quests.md)
- [Recursos de comportamento](catalogs/resources.md)


Auditoria estática de 30/10/2026, limitada a `reference/original-1.12.2/` (branch de referência declarada: `1.12.2-0.6.X`). Os checkboxes controlam a **portagem**, não a leitura. A primeira fatia de saldo persistente foi implementada e validada em 02/11/2026; entradas agregadas permanecem pendentes enquanto houver funcionalidades não portadas. Classes abaixo pertencem a `joshie.harvest`; os pacotes indicados identificam sua localização em `src/main/java/` do original. Recursos são relativos a `src/main/resources/assets/harvestfestival/` do original.

O inventário separa comportamento ativo, vestígios comentados e integrações **adiadas**. A existência de uma textura ou constante não comprova uma funcionalidade jogável. Os catálogos ao final detalham variantes e missões sem transformar classes de suporte em novas mecânicas.

## Núcleo, API e infraestrutura (`core`, `api` e raiz)

- [ ] Inicialização, registros, configuração, carregamento ordenado e API pública dos sistemas — `HarvestFestival`, `HFCommonProxy`, `HFClientProxy`, `HFApiLoader`, `HFApi`, `HFRegistry`, `RegistryHelper`, `HFLoader`, `LoadOrder`, `ConfigHelper`.
- [ ] Rede cliente/servidor, sincronização de entrada/saída e atualização de estado — `PacketHandler`, `Packet`, `PacketNBT`, `PenguinNetwork`, `SyncHandler`, `HFTrackers` e pacotes `packet` de cada sistema.
- [ ] Atualizações diárias nas fases PRE/MAIN/POST para blocos carregados, passagem de dia, atualizações a cada duas horas e pausa sem jogadores — `HFDailyTickable`, `DailyTickHandler`, `NewDayHandler`, `OfflineTickHandler`, `ServerHandler`, `DailyTickableBlock`. `DailyTickHandler` só processa posições carregadas; não presumir atualização global de blocos descarregados.
- [ ] Classes base de itens, alimentos, blocos, entidades de bloco, inventários ampliados, renderizadores e seleção por tamanho — `ItemHFEnum`, `ItemHFFood`, `ItemHFSizeable`, `BlockHFEnum`, `TileHarvest`, `TileFillable`, `ContainerExpanded`, `SizeableRegistry`, `SizeableHelper`.
- [ ] Abas criativas, ordenação, sons, traduções, modelos e texturas — `HFTab`, `CreativeSort`, `HFSounds`, `TextHelper`, `ResourceLoader`; recursos `lang`, `models`, `blockstates`, `textures`, `sounds` e `sounds.json`.
- [ ] Avanços e gatilhos de progressão — `HFAdvancements`, `EventTrigger`; todos os JSON de `advancements/`.
- [ ] Comandos administrativos/de diagnóstico de dia, hora, estação, clima, ano, festival, construção, mina, ouro, relações, receitas, notas, ferramentas e missões — `CommandManager`, `HFCommand`, `HFDebugCommand`, `VanillaCommands`, classes `HFCommand*` e `Command*` nos sistemas correspondentes.
- [ ] Regras configuráveis de criaturas hostis no Overworld — `MobHandler`, `HFCore`.

## Itens e blocos compartilhados (`core`)

- [x] Fatia 13: duas abas criativas implementadas, sementes e ferramentas/blocos; produtos na busca global. Novos itens devem receber destino nas abas. Validação visual pendente; cenário 8 aberto.

- [ ] Flores, ervas daninhas e flor da deusa — `HFCore`, `BlockFlower`.
- [ ] Água da deusa e ritual da flor com água/plantas — `BlockGoddessWater`, `GoddessHandler`, `BlockFlower`, `EntityNPCGoddess`.
- [ ] Cesta como bloco/entidade transportável e inventário — `BlockStorage`, `TileBasket`, `EntityBasket`, `BasketHandler`, `GuiBasket`, `ContainerBasket`.
- [ ] Caixa de remessas — `BlockStorage`, `TileShipping`, `ShippingRegistry` (economia abaixo).
- [ ] Caixa de correio — `BlockStorage`, `TileMailbox`, `LetterHelper`.
- [ ] Pratos, suporte/pote de festival e decoração de cozinha — `BlockStand`, `TileStand`, `TilePlate`, `TileFestivalPot`, `BlockDecorative`.
- [ ] Loot com variantes, tamanhos e regras de venda — `SetEnum`, `SetSizeable`, `ISellable`, `ISizeable`, `ShippingRegistry`.

## Calendário, estações, tempo e sono (`calendar`, `core.handlers`)

- [x] Base de aniversários concluída em 08/11/2026: data anual opcional do jogador, attachment persistente com cópia na morte, sincronização restrita ao titular e comando administrativo. Tabela independente de NPCs por datapack e comandos de consulta são extensão aprovada pelo responsável; apenas um exemplo de teste, sem NPCs. Build e testes aprovados; validação single player registrada em `PORTING_NOTES.md`, incluindo confirmação do responsável de ausência persistida após limpeza/reabertura. Mensagem `true`/`false` corrigida e observada no cliente. Inspeção real do espelho cliente e validação integrada permanecem pendentes. Efeitos, presentes, relações e escolha da data pelo jogador permanecem adiados.

- [x] Fatia de calendário básico: dia, estação, ano e dia da semana derivados do relógio persistente, avanço diário e comandos administrativos; validação manual confirmada pelo usuário em 02/11/2026 — originais: `CalendarDate`, `CalendarHelper`, `CalendarServer`, `HFCommandDay`, `HFCommandSeason`, `HFCommandYear`, `HFCommandNewDay`; port: `CalendarDate`, `Season`, `WorldCalendar`, `CalendarCommands`. Base de aniversários implementada na sétima fatia; seus efeitos, duração configurável, HUD, clima e festivais permanecem pendentes.

- [ ] Datas, dias da semana, quatro estações, anos, aniversários e duração do dia — `CalendarDate`, `Season`, `Weekday`, `CalendarAPI`, `CalendarHelper`, `CalendarServer`, `HFCalendar`.
- [ ] Previsão e intensidade de sol, chuva, tufão, neve e nevasca — `Weather`, `SeasonData`, `CalendarServer`, `PacketSyncForecast`, `PacketSyncStrength`.
- [ ] Renderização sazonal, precipitação/neblina e remoção de neve — `WeatherRenderer`, `CalendarRender`, `SnowLoader`, `EntityRendererMixin`.
- [ ] Provedores sazonais por dimensão e calendário oculto no Nether/End — `SeasonProvider`, `SeasonProviderHidden`, `CalendarAPI`, `HFCalendar`. A substituição do Overworld por `HFWorldProvider` está comentada; não tratá-la como registro ativo.
- [ ] HUD de data, relógio e ouro, posição e formato configuráveis — `CalendarHUD`, `HFCalendar`, `HFCommandEdit`, `PacketEdit`.
- [ ] Sono durante o dia, restrições configuráveis e avanço do mundo — `SleepHandler`, `NewDayHandler`, `HFCore`; recuperação/desmaio em ferramentas.

## Cultivos e árvores frutíferas (`crops`, `api.crops`, `api.trees`)

- [x] 19-A implementada: Nabo Robusto, ciclo de duas viradas regadas, produto distinto/preço 10/recursos originais/abas. Escopo ampliado autorizado: estação nula no Nether/End para **todos os cultivos HF**, com morte sazonal na próxima virada processada; frutos já vanilla e nether wart vanilla excluídos. Contrato e limites em [Cultivos](porting/cultivos.md#19-a--entrega-autorizada-e-decisão-dimensional-ampliada); aceite manual pendente.
- [ ] **19-B integralmente adiada por decisão do responsável**, inclusive saco HF, transformação, preço e demais dados. Manter nether wart vanilla; não estender política restritiva. Levantamento preservado abaixo e no contrato; modelos vanilla 26.2 e recusa de estágio HF 3/4: **“reavaliar se retomada”**. Não é entrega pendente dentro da implementação da 19-A.

- [x] Fatia 10 implementada e aceita pelo responsável em 12/11/2026: nabo comum, semente/produto, bloco genérico com entidade, definição JSON, plantio 3×3, quatro viradas regadas, morte sazonal/por seca, colheita única e remessa por tag (60). 24 testes novos/81 totais executados na implementação. Aceite single player sem detalhamento adicional por passo; não conclui o catálogo nem os itens abrangentes abaixo. Contrato em `porting/cultivos.md`.
- [ ] Completar o sistema agrícola e seus itens ainda na Fase 3, em fatias fechadas: demais cultivos e comportamentos, ferramentas completas, regras vanilla e solo compartilhado. Reafirmado pelo responsável em 12/11/2026; funções ausentes não são polimento. Respeitar dependências de lojas/NPCs e manter integrações específicas adiadas.
- [x] Fatia 12 implementada em 13/11/2026: pepino com nove viradas iniciais, rebrota em três, oito colheitas, contador persistente e preço 40 por tag. Políticas de loot vazio, falhas e recarga aprovadas; nabo preservado. Texturas originais do produto e saco de sementes incluídas como condição do responsável. Build aprovado, 15 testes novos/102 totais executados. Comportamento aceito pelo responsável em 14/11/2026 («Tudo certo com o pepino»), sem detalhamento por cenário. Correções posteriores de textura por estágio, itens do nabo e centro clicado arado: build aprovado, 3 novos/105 totais executados; aceite final da fatia e correções confirmado pelo responsável em 14/11/2026 («Confirmado a fatia»), sem detalhamento individual adicional. Contrato em `porting/cultivos.md`.
- [ ] Recortes restantes da fatia 10: chuva/clima HF e substituição aleatória de plantas mortas por recursos continuam ausentes. Rebrota limitada incluída na fatia 12; perda do preparo e pisoteio na fatia 11. Nabo e pepino possuem estágios visuais originais desde a correção de 14/11/2026; demais cultivos/renderizações continuam pendentes. Todo item novo de cada fatia deve incluir textura original, conforme decisão de 13/11/2026.
- [ ] Sementes, plantio, estágios por dia, rega, morte/murchamento fora da estação, colheita, rebrota e requisitos de foice — `Crop`, `CropData`, `CropRegistry`, `CropHelper`, `BlockHFCrops`, `ItemHFSeeds`, `ItemCrop`, `TileCrop`, `TileWithered`, `WateringTickHandler`.
- [ ] Cultivos: nabo, batata, pepino, morango, repolho, cebola, tomate, abóbora, abacaxi, melancia, milho, berinjela, espinafre, cenoura, batata-doce, pimentão verde, beterraba, grama/forragem, trigo e nabo de tutorial — `HFCrops`, `StateHandlerGrass`, `StateHandlerPumpkin`, `StateHandlerStem`, `StateHandlerWatermelon`, `GrowthHandlerPumpkin`, `GrowthHandlerSide`, `DropHandlerGrass`, `DropHandlerPotato`.
- [ ] Árvores de maçã, banana, laranja, pêssego e uva; folhas, frutos e crescimento próprio — `Tree`, `GrowthHandlerTree`, `StateHandlerTree`, `GrowthHandlerHFTree`, `GrowthHandlerApple`, `GrowthHandlerBanana`, `GrowthHandlerOrange`, `GrowthHandlerPeach`, `GrowthHandlerGrape`, `DropHandlerTree`, `BlockFruit`, `BlockLeavesFruit`, `BlockLeavesTropical`, `TileFruit`; `trees/*.json`.
- [ ] Nether wart registrado e saco exposto no criativo; primeiro crescimento HF substitui a planta por vanilla AGE=2, sem restrição ativa ao Nether (teste dimensional comentado). Comentário pede reintrodução adequada em 0.7 — `HFCrops.NETHER_WART`, `GrowthHandlerNether`, `DropHandlerNetherWart`. Sem aquisição survival do saco encontrada, venda automática ou conversão vanilla→HF; evidências e decisões da 19 em [Cultivos](porting/cultivos.md#evidências-da-fatia-19--levantamento-de-18112026).
- [ ] Desbloqueios de sementes por ano, rancho ou missão; preços e alimentação animal — `HFCrops`, `SpecialRulesYear`, `SpecialRulesRanch`, `SpecialRulesQuest`, `Crop`.
- [ ] Aspersores e compatibilidade do aspersor antigo — `BlockSprinkler`, `TileSprinkler`, `TileSprinklerOld`.
- [ ] Opções de crescimento diário, farinha de osso e restrição sazonal, consumo do aspersor e morte por falta de água distinta em servidor integrado/dedicado — `HFCrops`, `BlockHFCrops`, `TileCrop`.
- [ ] Receitas de sacos HF para sementes vanilla e controles independentes de plantio, enxadas, drops, sementes da grama, umidade e crescimento — `SeedRecipeHandler`, `HFCrops`, `DisableHandler`, `RegistryHelper`. 15-V entrega o recorte dos quatro cultivos presentes e quatro receitas; melancia/abóbora incluídas na 18-V, enquanto opções de ferramentas/umidade continuam abertas. Escopo em [sequência agrícola](#sequência-agrícola-pendente), sem conversão automática vanilla→HF.
- [ ] Modelos/tintas de cultivos e folhas e loot com tipo de cultivo — `CropStateMapper`, `BakedCrops`, `BakedLeaves`, `BakedTintedQuad`, `SetCropType`.

## Ferramentas, esforço e efeitos (`tools`, `core.base.item`)

- [x] Fatia 9 validada e aprovada pelo responsável em single player: enxada/regador de nível único, ação sobre bloco por regras/tags, solo preparado com marca persistente de rega e reserva de 128 unidades no item. Não conclui ferramentas por nível, esforço ou cultivo. Contrato e roteiro em `PORTING_NOTES.md`.
- [x] Indicadores do regador (barra/tooltip) confirmados funcionais pelo responsável nesta solicitação, em 14/11/2026. Cenário 8 continua pendente e vinculado ao complemento; não atribuir testes individuais ao agente.
- [x] Fatia 10: rega ao clicar na planta resolve o solo abaixo; crescimento avalia água antes da secagem diária, inclusive em solo vazio carregado. Testes automatizados aprovados e aceite manual global informado em 12/11/2026, sem atribuir resultados individuais não relatados.
- [x] Fatia 11 implementada e aceita em single player pelo responsável em 12/11/2026, sem relato individual por cenário: ferramentas HF e vanilla compartilham farmland; regras/tags de rega usam moisture, com migração de prepared_soil preservando água. Build aprovado, 6 testes novos/87 totais executados. Não inclui bloqueio configurável de ferramentas vanilla nem integrações específicas.
- [x] Texturas básicas originais de enxada/regador aplicadas e confirmadas pelo responsável em 12/11/2026 como correspondentes ao original. Não encerra barra/tooltip nem cenário 8. Releitura artística posterior pode integrar polimento; itens e comportamentos completos continuam trabalho da Fase 3.
- [x] Solo da fatia 11 implementado e aceito globalmente pelo responsável em 12/11/2026: ciclo 7→3→0; retorno à terra em atualização iniciada em zero sem planta que preserve farmland; queda via NeoForge com condições conferidas no Forge 1.12.x. Mixin restrito impede tick aleatório de umidade, com aplicação exercitada em teste. O complemento visual do regador e o cenário 8 permanecem pendentes.
- [ ] Machado, martelo, enxada, foice e regador por nível; carga e área de ação, derrubada de árvores, reserva de água — `HFTools`, `ItemAxe`, `ItemHammer`, `ItemHoe`, `ItemSickle`, `ItemWateringCan`, `TreeTasks`, `ItemTool`, `ItemToolChargeable`, `ItemToolSmashing`, `ITiered.ToolTier`.
- [ ] Experiência/níveis, dano, melhoria, conserto e ferramentas amaldiçoadas/abençoadas — `ToolHelper`, `ItemTool`, `QuestBlacksmithing`, `QuestPriestRepair`, `QuestBlessing`, `QuestToolTrader`.
- [ ] Consumo de fome, fadiga, exaustão, maldição, desmaio, retorno à cama e recuperação pelo sono — `ToolEvents`, `ToolHelper`, `HFPotion`, `HFTools`, `SleepHandler`.
- [ ] Indicadores de nível e animação de quebra — `RenderToolLevel`, `RenderToolBreak`.

## Animais (`animals`)

- [ ] Vacas, ovelhas e galinhas, filhotes e associação/substituição de animais vanilla — `HFAnimals`, `AnimalRegistry`, `AnimalEvents`, `AnimalRemover`, `EntityHarvestCow`, `EntityHarvestSheep`, `EntityHarvestChicken`, `AnimalCow`, `AnimalSheep`, `AnimalChicken`.
- [ ] Alimentação, carinho, escovação, felicidade, exposição ao sol/abrigo, doença, remédio, idade e morte — `AnimalStatsHF`, `AnimalStatsLivestock`, `AnimalAbstract`, `EntityAIAnimal`, `EntityAIEat`, `EntityAIEatLivestock`, `EntityAIFindShelterOrSun`, `ItemAnimalTool`.
- [ ] Gestação, poção milagrosa, postura, ninhos e incubação — `AnimalStatsLivestock`, `AnimalStatsPoultry`, `EntityAILayEgg`, `TileNest`, `TileIncubator`, `ItemAnimalTool`.
- [ ] Ordenha, tosquia, ovos, leite, lã e maionese em tamanhos e variantes douradas; produção diária e petiscos — `AnimalStatsMilkable`, `AnimalStatsHF`, `ItemAnimalProduct`, `ItemAnimalTreat`, `SizeableRegistry`.
- [ ] Cochos, bandejas de alimentação, ração e armazenamento dimensionado — `BlockTrough`, `BlockTray`, `BlockSizedStorage`, `TileTrough`, `TileFeeder`.
- [ ] Compra/invocação, rastreamento, sincronização e renderização dos animais — `ItemAnimalSpawner`, `AnimalTracker`, `AnimalTrackerServer`, `PacketSyncAnimal`, `PacketSyncHappiness`, `RenderHarvestAnimal`.
- [ ] Transporte de galinhas sobre a cabeça e colocação no chão; configurações de nascimento natural, reprodução, maturação e modelos vanilla — `AnimalEvents`, `HFAnimals`.

## Coleta e recursos renováveis (`gathering`)

- [ ] Geração sazonal/diária de ervas, flores, bambu, cogumelos, galhos, tocos, pedras e rochas em tamanhos diferentes — `HFGathering`, `GatheringRegistry`, `GatheringDaily`, `GatheringData`, `GatheringLocation`, `BlockNature`, `BlockWood`, `BlockRock`.
- [ ] Locais válidos, quantidades/tentativas configuráveis, resistência e coleta por ferramentas — `GatheringRegistry`, `HFGathering`, `ISmashable`, `BlockHFSmashable`, `ItemToolSmashing`.

## Pesca e criação aquática (`fishing`)

- [ ] Varas por nível, anzol próprio, lançamento/recolhimento, iscas e apresentação da linha — `HFFishing`, `ItemFishingRod`, `EntityFishHookHF`, `FishingHelper`, `RenderBait`, `RenderFishHookMixin`.
- [ ] Espécies, tamanho/peso, estações, horários, níveis, lixo e tesouros — `ItemFish`, `ItemJunk`, `FishingAPI`, `ConditionTier`, `ConditionTime`, `SetWeight`; `loot_tables/gameplay/fishing/`.
- [ ] Armadilhas aquáticas e captura — `BlockFishTrap`, `ItemBlockAquatic`, `TileTrap`.
- [ ] Criatório flutuante, reprodução por dias, capacidade e desgaste — `BlockHatchery`, `ItemBlockFishing`, `TileHatchery`, `FishingAPI`.
- [ ] Coleção de peixes e associação de espécies reproduzíveis/iscas — `FishingAPI`, `PageFishing`, `TrackingServer`.

## Mineração (`mining`)

- [ ] Dimensão de mineração, geração de andares/chunks, controle de minas e renovação de minérios — `HFMining`, `MiningProvider`, `MiningChunk`, `MineManager`, `MiningDaily`, `MiningRegistry`.
- [ ] Portal, escadas, elevador, teleporte e progresso de andar — `BlockPortal`, `BlockLadder`, `BlockElevator`, `TileElevator`, `ItemBlockElevator`, `TeleportPlayer`, `MiningHelper`, `PacketSyncMineFloor`.
- [ ] Minérios, pedras, chão de mina, materiais, gemas e utensílios de mineração — `BlockOre`, `BlockStone`, `BlockDirt`, `ItemMaterial`, `ItemMiningTool`.
- [ ] Loot por estação, faixa/exatidão/múltiplo/final de andar e itens já obtidos — `Seasonal`, `Obtained`, `FloorCondition`, `Between`, `Between100`, `Exact`, `EndsIn`, `From`, `MultipleOf`; `loot_tables/gameplay/mining/`.
- [ ] Animais sombrios e seus drops/invocadores — `EntityDarkChick`, `EntityDarkChicken`, `EntityDarkCow`, `EntityDarkSheep`, `ItemDarkDrop`, `ItemDarkSpawner`.

## Culinária (`cooking`)

- [ ] Ingredientes, refeições, equivalências e receitas próprias/vanilla — `HFIngredients`, `HFRecipes`, `Ingredient`, `IngredientStack`, `ItemIngredients`, `ItemMeal`, `RecipeBuilder`, `RecipeHF`, `RecipeVanilla`, `RecipeMaker`, `RecipeHelper`, `CookingAPI`.
- [ ] Bancada, panela, frigideira, forno e mixer; utensílios, aquecimento e preparo — `BlockCookware`, `ItemUtensil`, `TileCooking`, `TileCounter`, `TilePot`, `TileFryingPan`, `TileOven`, `TileMixer`, `TileHeatable`, `CookingHelper`.
- [ ] Resultados queimados por utensílio e multiplicador de preço das refeições — `ItemMeal`, `CookingAPI`, `HFCooking`.
- [ ] Geladeira de ingredientes com inventário ampliado — `TileFridge`, `FridgeData`, `ContainerFridge`, `GuiFridge`, `PacketExpandedSlot`.
- [ ] Aprendizado/seleção de receitas e livro por utensílio — `ItemRecipe`, `ItemCookbook`, `GuiCookbook`, `PageRecipe`, `PageRecipeList`, `PageUtensilList`, `PacketSelectRecipe`, `TrackingServer`.
- [ ] Exibição de ingredientes/refeições nos utensílios e pratos — `IngredientMappingEvent`, `MealDefinition`, `SpecialRendererCookware`, `TilePlate`.

## Economia e lojas (`shops`, `player`, `core`)

- [x] Fatia de saldo individual persistente e comandos administrativos `get`, `set` e `add`, preservado ao salvar/carregar e renascer — originais: `Stats`, `StatsServer`, `PlayerLoader`, `HFCommandGold`; port: `harvestfestival.player.PlayerGold`, `harvestfestival.player.command.GoldCommands`. Sem HUD, lojas ou remessas nesta fatia.

- [x] Fatia de remessas e pagamento diário concluída: depósito manual de uma unidade na caixa, fila persistente por jogador e crédito consumindo a fila por `NewDayEvent`, com a soma existente de `PlayerGold`. Preço provisório `SellValue` no próprio item autorizado pelo usuário. Seis testes novos, 19 no total, build aprovado. Validação manual pelo responsável, confirmada em 05/11/2026, com `runClient` single player: set/get de ouro, caixa colocada e graveto validado; valor mantido após morte e destruição da caixa; sair/reabrir; `/hf newDay` com valores corretos e sem duplicação após sair/entrar. Automação por funil, coleta de itens lançados, cesta, histórico, preços por tags e aniversário não implementados nesta fatia.
- [ ] Validação integrada pendente: servidor dedicado, dois jogadores, permissões, reconexão, persistência e espelho cliente do aniversário. Preços por item/tag foram entregues na sexta fatia; a base de aniversários foi implementada na sétima, sem efeitos.
- [ ] Saldo individual, preços, envio de produtos, histórico de remessas e pagamento na virada do dia — `StatsServer`, `ShippingRegistry`, `TileShipping`, `TrackingServer`, `StackSold`, `PlayerTrackerServer`, `TrackingRenderer`.
- [ ] Lojas de rancho, café, carpinteiro, avicultura, supermercado, minerador, pesca, relojoeiro, cozinha e comerciante; lojas festivas — `HFShops`, `Shop`, `ShopHours`, `ShopHelper`, `ShopData`, `ShopInventory`.
- [ ] Horários, dias, estoque, condições por amizade/missão, produtos obtidos/enviados e finais de semana — `OpeningHandler`, `SpecialRulesFriendship`, `SpecialRulesQuest`, `PurchasableStock`, `PurchasableWeekend`, `PurchasableObtained`, `PurchasableRecipeShipped`.
- [ ] Compra com ouro e materiais, trocas, venda de sementes/receitas/refeições/animais/construções e demolição — `Purchasable`, `PurchasableMaterials`, `PurchasableTrade`, `PurchasableCropSeeds`, `PurchasableRecipe`, `PurchasableMeal`, `PurchasableEntity`, `PurchasableBuilding`, `PurchasableDestroy`, classes de `shops.requirement`.
- [ ] Interface de loja e transação no servidor — `GuiNPCShop`, `ContainerNPCShop`, `ShopSelection`, `PacketPurchaseItem`, `PacketSyncSold`.

## Cidades e construções (`town`, `buildings`)

- [ ] Criação/localização de cidades, centro, aniversário, edifícios, moradores, missões e festival da cidade — `TownHelper`, `TownTrackerServer`, `TownDataServer`, `TownBuilding`, `BuildingLocations`.
- [ ] Projetos, prévia, orientação, validação de espaço/terreno, etapas e construção pelo carpinteiro — `ItemBlueprint`, `ItemBuilding`, `BuildingHelper`, `BuildingRegistry`, `BuildingStage`, `BuildingError`, `BuildingRenderer`, `PreviewEvent`, `BuildingKey`, `EntityAIBuild`.
- [ ] Estruturas: rancho, ferreiro, café, carpinteiro, igreja, relojoeiro, lago de pesca, cabana de pesca, lago da deusa, colina da mina, avicultura, supermercado, prefeitura e recinto de festivais — `HFBuildings`, `Building`, `SpecialRuleBuildings`, `SpecialRuleChurch`, `SpecialRuleFestivals`; `buildings/*.json`.
- [ ] Colocação de blocos, baús/loot, placas, vasos, entidades, pinturas, molduras, moradores e inventários — `PlaceableHelper`, `PlaceableBlock`, `PlaceableChest`, `PlaceableSign`, `PlaceableFlowerPot`, `PlaceableEntity`, `PlaceablePainting`, `PlaceableItemFrame`, `PlaceableNPC`, `PlaceableMoveIn`, `PlaceableFillable`, `LootHelper`.
- [ ] Troca de estruturas de festival e ferramentas de geração/diagnóstico — `BuildingFestival`, `BuildingFestivalDebug`, `ItemCheat`, `CodeGeneratorBuildings`, adaptadores de `buildings.placeable`/`buildings`.

## NPCs e relações (`npcs`, `player.relationships`)

- [ ] Cadastro de habitantes, deusa, construtor, minerador, comerciantes e visitantes de festivais — `HFNPCs`, `NPC`, `EntityNPC`, `EntityNPCHuman`, `EntityNPCVillager`, `EntityNPCBuilder`, `EntityNPCMiner`, `EntityNPCGoddess`, `EntityNPCShopkeeper`, `NPCHolidayStore`, `NPCHolidayStoreSpecial`.
- [ ] Rotinas por hora/local/dia, movimento, construção e interrupção para conversa — `HFSchedules`, `Schedule`, `ScheduleBuilder`, `EntityAISchedule`, `EntityAIPathing`, `EntityAITalkingTo`, `EntityAIBuild`.
- [ ] Diálogo contextual por tempo, clima, localização, festival e progressão; escolhas e scripts — `GreetingTime`, `GreetingWeather`, `GreetingLocation`, `GreetingFestival`, `GreetingBeforeQuest`, `GreetingCarpenter`, `GreetingPriestBlessing`, `GreetingFlowerBuyer`, `Script`, `HFScript`, `TaskList`, `TaskSpeech`, `TaskMove`, `TaskWait`.
- [ ] Amizade, conversa/presente diário, preferências por NPC/categoria, blacklist e aniversários — `RelationshipDataServer`, `GiftRegistry`, `Gifts`, classes `Gifts*`, `HFGifts*`, `ContainerNPCGift`, `PacketGift`, `PacketGoddessGift`.
- [ ] Menus de conversa, presentes, seleção, máscaras e informações — `GuiNPCChat`, `GuiNPCGift`, `GuiNPCSelect`, `GuiNPCMask`, `ContainerNPCChat`, `PacketInfo`.
- [ ] Invocadores, remoção de NPCs e presentes especiais — `ItemNPCSpawner`, `ItemNPCTool`.
- [ ] Vestígios de casamento/pena azul: **inativos ou incompletos no original**, não equivalem a casamento funcional — `PurchasableBlueFeather` está comentada, `PacketSyncMarriage` e `ItemNPCTool.NPCTool.BLUE_FEATHER` permanecem como vestígios. Qualquer restauração requer definição explícita de escopo.

## Missões e progressão (`quests`)

- [ ] Registro, condições, etapas, escolhas, início/conclusão/revogação, alvos jogador/cidade e sincronização — `HFQuest`, `Quest`, `QuestQuestion`, `Selection`, `TargetType`, `QuestHelper`, `QuestEvents`, `QuestDataServer`, `Quests`.
- [ ] Quadro de missões, seleção e atualização diária — `HFQuests`, `BlockQuestBoard`, `TileQuestBoard`, `GuiQuestBoard`, `PacketQuestSelect`, `PacketQuestStart`.
- [ ] Apresentações e tutoriais de habitantes/deusa/carpinteiro — `QuestMeeting`, `QuestMeetingTutorial`, `QuestGoddessCarpenter`, `TutorialSelection`, classes de `quests.player.meetings`.
- [ ] Recompensas por amizade: itens, notas, lojas, refeições, flores, minérios, criatório, aspersor e desbloqueios — `QuestFriendship`, `QuestFriendshipStore`, classes de `quests.player.friendship`.
- [ ] Receitas concedidas por amizade — `QuestRecipe`, todas as classes de `quests.player.recipes`.
- [ ] Serviços de ferreiro, reparo, bênção, troca de ferramentas/flores e bolo dominical — `QuestBlacksmithing`, `QuestPriestRepair`, `QuestBlessing`, `QuestToolTrader`, `QuestFlowerTrader`, `QuestKatlinSundayBakingFreeCake`.
- [ ] Progressão comunitária por remessas/cultivos e desbloqueio de edifícios/serviços — `QuestTown`, `QuestProgress`, `QuestShipping`, `QuestCabbage`, `QuestPineapple`, `QuestSweetPotato`, `QuestHatchery`, `QuestMeals`, `QuestOres`, `QuestSprinkler`, `QuestWednesday`, `QuestGoddessPond`.
- [ ] Pedidos recorrentes de coleta, pesca, madeira/pedra e combate; negociação de flores — `QuestDaily`, `QuestWeekly`, `QuestTrade`, `QuestTradeTown`, `QuestCollectCrops`, `QuestCollectFish`, `QuestCollectWoodStone`, `QuestSlay`, `QuestFlowerBuyingAllow`, `QuestFlowerBuyingDo`.

## Festivais (`calendar.HFFestivals`, `quests.town.festivals`, `buildings`)

- [ ] Ano-novo (primavera 1) — `HFFestivals`, `QuestNewYears`.
- [ ] Concurso de culinária (primavera 22; café) — `QuestContestCooking`, `CookingContestEntries`, `CookingContestEntry`.
- [ ] Festival de galinhas (verão 7; avicultura) — `QuestContestChicken`, `QuestAnimalContest`.
- [ ] Festival de vacas (verão 20; rancho) — `QuestContestCow`, `QuestAnimalContest`.
- [ ] Festival da colheita (outono 9), refeição comunitária — `QuestHarvestFestival`, `HarvestSelection`, `ScriptHarvestDecide`, `TaskEat`, `TileFestivalPot`.
- [ ] Festival de ovelhas (outono 21; rancho) — `QuestContestSheep`, `QuestAnimalContest`.
- [ ] Noite estrelada (inverno 24) — `QuestStarryNight`, `StarryNightData`, `ScriptStarry`.
- [ ] Véspera de ano-novo (inverno 30) — `QuestNewYearsEve`.
- [ ] Inscrições, menus, avaliação, vencedores, recompensas e estado dos concursos — `QuestContest`, `ContestEntries`, `ContestEntrySelection`, `ContestInfoMenu`, `ContestStartMenu`, `ContestJudgingScript`, `ContestWinningScript`, `ContestTaskWinner`, `AnimalContestEntries`.
- [ ] Convites/cartas, requisitos de construção, horários, visitantes/lojas e cenário temporário — `Festival`, `HFFestivals`, `LetterFestival`, `QuestFestival`, `QuestFestivalTimed`, `QuestFestivalMultichat`, `BuildingFestival`, `HFShops`; oito cenários em `festivals/` mais `none.json` (restauração, não um nono festival).

## Conhecimento, cartas e interfaces (`knowledge`)

- [ ] Livro de estatísticas, coleções de cultivos/remessas, pesca, mineração e culinária, busca/filtros — `ItemBook`, `GuiStats`, `CollectionHelper`, `PageCollection`, `PageShipping`, `PageFishing`, `PageMining`, `PageCooking`.
- [ ] Notas de atividades, agricultura, cidade e ajuda de ferramentas — `HFNotes`, `PageNotes`, `PageActivities`, `PageFarming`, `PageTown`, `PageOther`, `NoteRenderCursedTools`, `NoteRenderRepairing`, `NoteRenderUpgrading`.
- [ ] Páginas de missões e relações com NPCs/animais — `PageQuests`, `PageRelationship`, `PageNPC`, `PageAnimals`.
- [ ] Calendário de parede e navegação entre datas — `BlockCalendar`, `GuiCalendar`, `CalendarEntry`.
- [ ] Recebimento/leitura/remoção de cartas, aceite/rejeição e cartas festivas — `Letter`, `LetterHelper`, `LetterDataServer`, `LetterDataClient`, `LetterFestival`, `GuiLetter`, `PacketButtonAccept`, `PacketButtonReject`.
- [ ] Configuração de cliente — `HFGuiFactory`, `ConfigHelper`; demais GUIs estão listadas nos sistemas proprietários, inclusive cesta, geladeira, receitas, lojas, NPCs e quadro de missões.

## Dados salvos e multiplayer (`player`, `town`, `calendar`, `core` e sistemas proprietários)

- [ ] Dados por UUID: ouro/aniversário, relações, missões, cartas, receitas/notas, itens obtidos, remessas, estatísticas e andar de mina — `PlayerLoader`, `PlayerTrackerServer`, `StatsServer`, `RelationshipDataServer`, `QuestDataServer`, `LetterDataServer`, `TrackingServer`.
- [ ] Calendário e previsão globais — `CalendarSavedData`, `CalendarServer`, `HFTrackers`.
- [ ] Cidades, edifícios, festival e missões compartilhadas — `TownSavedData`, `TownTrackerServer`, `TownDataServer`, `QuestDataServer`.
- [ ] Minas e controle de atualização por chunk — `MineManager`, `MiningDaily`, `NBTHelper`.
- [ ] NBT de entidades/animais e entidades de bloco (cultivo, inventários, utensílios, criatório, cochos etc.) — `AnimalStatsHF`, `EntityNPC`, `TileHarvest` e subclasses.
- [ ] Migração histórica de `TownTracker` para dados próprios de cidades — `HFSavedData.setWorld`, `TownSavedData`. `HFSavedData.writeToNBT` retorna o composto sem gravar os rastreadores: não atribuir a ele a persistência atual de todos os sistemas.
- [ ] Espelhos cliente, pacotes iniciais/incrementais e atualização ao novo dia — `PlayerTrackerClient`, `TownTrackerClient`, `CalendarClient`, `SyncHandler`, `PacketSharedSync`, `PlayerTrackerServer.newDay`.

## Mixins (`mixin`)

- [ ] Supressão de partículas/efeitos de chuva incompatíveis com o clima sazonal — `EntityRendererMixin`; alvo e alternativa em `PORTING_NOTES.md`.
- [ ] Reconhecimento de varas HF pelo renderizador vanilla do anzol — `RenderFishHookMixin`; alvo e alternativa em `PORTING_NOTES.md`.

## Integrações de terceiros — adiadas (`plugins`)

- [ ] **Adiada — Aquaculture:** venda, coleção, reprodução, ingredientes, iscas e varas na loja — `plugins.Aquaculture`.
- [ ] **Adiada — Biomes O' Plenty:** nabo/sementes, cogumelos culinários, solo/umidade e locais de coleta — `plugins.BiomesOPlenty`.
- [ ] **Adiada — Blood Magic:** mercadorias do mago e valor de sacrifício zero para NPCs — `plugins.BloodMagic`, `HFShops.BLOODMAGE`.
- [ ] **Adiada — Immersive Engineering:** cânhamo sazonal, sementes/drops e receita de espremedor para óleo vegetal — `ImmersiveEngineering`, `HempGrowthHandler`, `StateHandlerHemp`, `DropHandlerHemp`.
- [ ] **Adiada — CraftTweaker:** scripts de cultivos, blacklist, presentes, remessas, lojas, requisitos, preços e comandos de inspeção — `CraftTweaker`, `BaseCrop`, `BaseOnce`, `Crops`, `Blacklist`, `Gifting`, `Shipping`, `Shops`, `PurchasableWrapper`, `PurchasableWrapperMaterials`, `RequirementItemWrapper`, `RequirementOreWrapper`, `MultiDropHandler`, `HFCommandNPC`, `HFCommandPurchasable`, `HFCommandShops`.
- [ ] **Adiada — Buttons:** botões de estação/clima; código comentado com TODO de reativação — `Buttons`, `ButtonSeason`, `ButtonWeather`.
- [ ] **Adiada — Morpheus:** adaptação do sono coletivo; `Morpheus` e `SleepHandlerOverworld` estão comentados; `HFTools` ainda detecta o mod para configuração. Interfaces externas em `src/api/java/net/quetzi/morpheus/api/`.
- [ ] **Adiada — JEI:** dependência declarada em `build.gradle`; nenhum plugin/import de JEI encontrado no Java desta cópia. Não presumir categorias de receitas implementadas. Classes principais de integração: nenhuma localizada.
- [ ] **Adiada — MixinBooter/MixinExtras:** infraestrutura legada de carregamento/injeção, não conteúdo de gameplay — `InitMixins`, `HFModInfo`, `RenderFishHookMixin`, `build.gradle`. Não transportar a dependência legada automaticamente ao NeoForge.


## Cobertura da árvore de referência

Contagem de arquivos `.java` (inclui arquivos comentados; não equivale a classes ativas). Todo diretório imediato foi associado às seções acima; `api` contém contratos dos respectivos sistemas.

| Diretório | Arquivos Java |
| --- | ---: |
| `animals` | 47 |
| `api` | 100 |
| `buildings` | 50 |
| `calendar` | 27 |
| `cooking` | 45 |
| `core` | 161 |
| `crops` | 42 |
| `fishing` | 19 |
| `gathering` | 8 |
| `knowledge` | 56 |
| `mining` | 42 |
| `mixin` | 2 |
| `npcs` | 82 |
| `player` | 36 |
| `plugins` | 29 |
| `quests` | 184 |
| `shops` | 51 |
| `tools` | 13 |
| `town` | 19 |

Raiz Java: `HarvestFestival.java`, `InitMixins.java`. Interfaces externas em `src/api/java/net/quetzi/morpheus/api/`: `IMorpheusAPI.java` e `INewDayHandler.java` (integração adiada).


## Progresso da infraestrutura diária

- [x] Evento interno de novo dia implementado; validação manual aprovada pelo usuário — originais: `NewDayHandler`, `CalendarHelper`; port: `DailyTransition`, `DailyUpdates`, `NewDayEvent`. Não conclui as fases diárias de blocos nem os consumidores de gameplay.

## Progresso da sincronização individual

- [x] Sincronização do ouro somente ao proprietário — originais: `StatsServer`, `PacketSyncGold`; port: `harvestfestival.player.PlayerGold`. Concluída conforme validação manual do responsável em 04/11/2026, com runClient single player: set/add consultados por get, morte/renascimento, portal e volta, sair/reabrir. Pendentes: servidor dedicado, dois jogadores e reconexão. Build e quantidade de testes automatizados informados pelo responsável: **não informado**.

- [x] Fatia 13: trigo HF com 27 viradas regadas, corte por foice e produto vanilla a 100; foice básica com dano/nível persistentes e esforço básico. Contratos em porting/cultivos.md e porting/ferramentas.md. Fatia 13 aceita pelo responsável.
- [x] Fatia 14: fadiga, desmaio e recuperação implementados; aceite global do responsável em 15/11/2026.





## Sequência agrícola pendente

Em 18/11/2026, o responsável declarou a fatia 18 aprovada: aceite global de 18-A1/A2, 18-V e 18-B, sucedendo as pendências de aceite dessas entregas abaixo. Relatou ter visto a grama crescer em duas camadas; não foram informados resultados individuais dos demais testes. Nenhuma execução em jogo é atribuída ao agente. Cenário 8 não executado, dedicado/multiplayer pendentes e memória de camas por dimensão adiada permanecem explícitos.

Intenção futura registrada em 18/11/2026: milho com duas camadas, após o responsável preparar as texturas correspondentes. Não altera o milho atual nem autoriza implementação nesta etapa ou na 19. Estágio de formação do topo, aparência na rebrota, permissões, remoção do par e compatibilidade precisarão de recorte e decisões próprios; não copiar automaticamente o ciclo de corte único da grama.

Sequência aprovada pelo responsável: 19 com nabo de tutorial e nether wart (escopo do prompt aprovado, adaptável à sequência); 20 para concluir árvores frutíferas e aspersores em subfatias 20-A, 20-B etc.; início do desenvolvimento de NPCs na 21. Os recortes concretos devem se apoiar no original e ser apresentados na conversa antes da implementação. Não pressupor disponibilidade normal do nether wart a partir de registro privado, provider ou TODO; não antecipar tutorial/missões na 19. A sequência não declara o sistema agrícola inteiro completo e não inclui o milho de duas camadas. Dependências e prioridade atualizadas em DEPENDENCIES.md; não é necessário solicitar novamente a aprovação geral já dada.

Implementação da 18 autorizada em 18/11/2026: abóbora vanilla lisa 26.2 e demais recortes/adaptações apresentados. Levantamento anterior por leitura do original e motor/Forge: evidências em [Cultivos](porting/cultivos.md) e [Ferramentas](porting/ferramentas.md). Melancia/abóbora transformam a própria posição; abóbora moderna exige escolha explícita de representação. Política/receitas serão entrega separada. Grama padrão remove base/topo no primeiro corte (maxHarvests=1 antes de markSafe); rebrota ilimitada não é comportamento comprovado. Obstrução no topo e irregularidade sazonal permanecem decisões explícitas. Proposta na conversa, sem arquivo de plano; as pendências de decisão desse levantamento ficam sucedidas pela aprovação de 18/11, registrada no contrato.

Em 17/11/2026, o responsável informou que tudo ocorreu bem e declarou a fatia 17 concluída. Aceite global de 17-A/17-B1/17-B2/17-F1/17-F2/17-F3/17-F4, sucedendo as pendências de aceite dessas entregas abaixo, sem atribuir resultados individuais ou execução de cliente ao agente. Cenário 8 não executado, dedicado/multiplayer pendentes e memória de camas por dimensão adiada permanecem explícitos. A solicitação inicial previa levantamento da 18-A/18-B, posteriormente delimitado e autorizado em 18/11/2026.

Responsável confirmou a fatia 16 finalizada e solicitou avanço ao levantamento da 17. Aceite global de 16-A/16-F1/16-F2 sucede pendências de aceite dessas entregas abaixo, sem atribuir resultados individuais. Recortes da 17 apresentados somente na conversa e posteriormente aprovados na solicitação de implementação: 17-A/B1/B2/F1/F2/F3/F4 entregues nessa ordem, com builds próprios e aceites manuais independentes pendentes.

Melhoria futura solicitada, sem prioridade e fora da 17: área da foice também afetar mato, flores e folhas vanilla. O original já admite IPlantable/ferramenta eficaz, mas isso não comprova toda a lista desejada; conferir alvos por tags, plantas duplas, loot normal versus tesoura, permissões, custos e duplicação em tarefa própria. Divergências intencionais serão delimitadas nessa tarefa; não ampliar automaticamente política vanilla ou integração com outros mods.

16-A e repartições 16-F1/16-F2 aprovadas explicitamente pelo responsável após o levantamento. Estrutura de alimentação de todos os alimentos adiada para uma única fatia posterior, incluindo os produtos HF anteriores e novos; não antecipar alimentação parcial na 16.

Em 16/11/2026 o responsável aceitou 15-F, 15-A, 15-V no recorte delimitado e a ampliação da rega. Substitui as pendências de aceite das entregas na tabela abaixo, sem ampliar cobertura individual de testes. 16-A, 16-F1 e 16-F2 foram posteriormente aprovadas e implementadas; validação manual independente pendente. Cenário 8 e dedicado/multiplayer permanecem pendentes; memória de camas por dimensão adiada.

| Fatia | Recorte | Aceite próprio |
| --- | --- | --- |
| 14 | Implementada: esforço/sono, cebola/espinafre | Build aprovado, 20 novos/139 totais executados; aceite global do responsável em 15/11/2026 e memória de camas por dimensão adiada por decisão explícita |
| 15-F | Implementada: enxada/regador até prata, anterior aos cultivos | Carga persistente da enxada até 1×3; regador sem carga até 3×3; custos/nível/dano/água por alteração. 12 novos/151 testes executados; aceite manual pendente. Recorte de alvos aprovado em Ferramentas |
| 15-A | Implementada e aceita: cenoura e beterraba comuns; batata com drop especial | Produtos/visuais vanilla com sementes HF; uma/duas entradas, substituição individual por venenosa, sorteio em cada avaliação do laço e preço 1. Build aprovado, 9 novos/160 testes executados; responsável informou testes aprovados e solicitou avanço ao levantamento da 15-V |
| 15-V | Plano aprovado e implementado para trigo/cenoura/beterraba/batata vanilla; aceite manual pendente | Opções independentes de plantio, crescimento/uso no bloco, drops e sementes da grama; quatro receitas HF/produto→sementes vanilla, tags e recarga com política transitória sincronizada. Melancia/abóbora incluídas na 18-V; umidade/enxadas opcionais exigem recorte próprio. Não conclui toda a política vanilla |
| 16-A | Implementada: morango, tomate e berinjela | Rebrota comum 12/8/8, dados/recursos, persistência, sazonalidade e remessas. Sete testes novos; aceite manual pendente. Comércio e alimentação adiados |
| 16-F1/F2 | Implementadas: foice cobre e foice prata | Carga persistente e áreas próprias 3×1/3×2, callbacks/permissões/custos por candidato. Oito/quatro testes novos; aceites manuais independentes pendentes. Suíte conjunta 192, build final aprovado com testes reutilizados |
| 17-A/B1/B2 | Implementadas: repolho; abacaxi/milho; batata-doce/pimentão verde | 7 testes novos por entrega; builds com 199/206/213 totais executados. Colheita única e rebrota 3/7/14/9, recursos/remessas/abas; aceites manuais separados pendentes |
| 17-F1/F2/F3/F4 | Implementadas: enxada/regador ouro; enxada/regador mystril; foice ouro; foice mystril | 4 testes novos por entrega; builds com 217/221/225/229 totais executados. Geometria/custos próprios, codecs até carga 4 com limite físico; aceites manuais separados pendentes |
| 18-A1/A2 | Implementadas; aceite manual pendente | Melancia/abóbora no verão, transformação na própria posição sem rebrota; abóbora lisa vanilla 26.2 por decisão explícita, sem orientação. Produtos e drops vanilla sem segunda emissão HF |
| 18-V | Implementada; aceite manual pendente | Política estendida somente às sementes/hastes de melancia e abóbora; frutos livres, três receitas próprias, sem conversão vanilla→HF |
| 18-B | Implementada; aceite manual pendente | Planta dupla desde 6, sem água, foice entrega 1–6 e remove o par no primeiro corte. Permissões do par, obstrução protegida e murchamento coerente aprovados; sem consumidores animais |

15-F, 15-A e o recorte delimitado da 15-V tiveram implementação autorizada nesta sequência após a fatia 14. A 16 e os sete recortes da 17 também foram aprovados e implementados; a 18 foi aprovada em 18/11/2026; recortes posteriores exigem aprovação própria; não há promessa de concluir o catálogo em cinco implementações. Trigo da fatia 13 não concluiu 15-V; a entrega atual também não encerra suas extensões pendentes. Tiers amaldiçoados/abençoados/míticos, reparos e obtenção por serviços ficam em recortes posteriores com dependências próprias; árvores/aspersores mantêm mecanismos separados.


Fatia 14 implementada em 15/11/2026 conforme aprovação: efeitos próprios, desmaio padrão sem morte, recuperação e esforço nas ações existentes de foice/enxada/regador; cebola e espinafre com recursos originais/remessas/abas. Contratos em [Ferramentas](porting/ferramentas.md) e [Cultivos](porting/cultivos.md). Memória de camas por dimensão mantida pendente pelo responsável; accessor mínimo de sono autorizado. Não conclui maldição, ferramentas completas, política vanilla, cenário 8, dedicado/multiplayer ; aceite global do responsável em 15/11/2026. Planos transitórios removidos a pedido; roteiro manual somente na resposta final.

Diretriz de 15/11/2026: distribuir tiers para facilitar testes. Levantamento seguido inicialmente de aprovação explícita somente da 15-F: regador até prata (3×3 sem carga), enxada até prata (1×3 com carga). A enxada original não oferece 3×3 nesse tier. Custos, nível, durabilidade, orientação, recortes autorizados e validação em [Ferramentas](porting/ferramentas.md). A aprovação inicial não abrangia cultivos novos nem política vanilla; 15-A e o recorte da 15-V receberam autorizações próprias depois.
