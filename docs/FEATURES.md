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

- [ ] Flores, ervas daninhas e flor da deusa — `HFCore`, `BlockFlower`.
- [ ] Água da deusa e ritual da flor com água/plantas — `BlockGoddessWater`, `GoddessHandler`, `BlockFlower`, `EntityNPCGoddess`.
- [ ] Cesta como bloco/entidade transportável e inventário — `BlockStorage`, `TileBasket`, `EntityBasket`, `BasketHandler`, `GuiBasket`, `ContainerBasket`.
- [ ] Caixa de remessas — `BlockStorage`, `TileShipping`, `ShippingRegistry` (economia abaixo).
- [ ] Caixa de correio — `BlockStorage`, `TileMailbox`, `LetterHelper`.
- [ ] Pratos, suporte/pote de festival e decoração de cozinha — `BlockStand`, `TileStand`, `TilePlate`, `TileFestivalPot`, `BlockDecorative`.
- [ ] Loot com variantes, tamanhos e regras de venda — `SetEnum`, `SetSizeable`, `ISellable`, `ISizeable`, `ShippingRegistry`.

## Calendário, estações, tempo e sono (`calendar`, `core.handlers`)

- [x] Fatia de calendário básico: dia, estação, ano e dia da semana derivados do relógio persistente, avanço diário e comandos administrativos; validação manual confirmada pelo usuário em 02/11/2026 — originais: `CalendarDate`, `CalendarHelper`, `CalendarServer`, `HFCommandDay`, `HFCommandSeason`, `HFCommandYear`, `HFCommandNewDay`; port: `CalendarDate`, `Season`, `WorldCalendar`, `CalendarCommands`. Aniversários, duração configurável, HUD, clima e festivais permanecem pendentes.

- [ ] Datas, dias da semana, quatro estações, anos, aniversários e duração do dia — `CalendarDate`, `Season`, `Weekday`, `CalendarAPI`, `CalendarHelper`, `CalendarServer`, `HFCalendar`.
- [ ] Previsão e intensidade de sol, chuva, tufão, neve e nevasca — `Weather`, `SeasonData`, `CalendarServer`, `PacketSyncForecast`, `PacketSyncStrength`.
- [ ] Renderização sazonal, precipitação/neblina e remoção de neve — `WeatherRenderer`, `CalendarRender`, `SnowLoader`, `EntityRendererMixin`.
- [ ] Provedores sazonais por dimensão e calendário oculto no Nether/End — `SeasonProvider`, `SeasonProviderHidden`, `CalendarAPI`, `HFCalendar`. A substituição do Overworld por `HFWorldProvider` está comentada; não tratá-la como registro ativo.
- [ ] HUD de data, relógio e ouro, posição e formato configuráveis — `CalendarHUD`, `HFCalendar`, `HFCommandEdit`, `PacketEdit`.
- [ ] Sono durante o dia, restrições configuráveis e avanço do mundo — `SleepHandler`, `NewDayHandler`, `HFCore`; recuperação/desmaio em ferramentas.

## Cultivos e árvores frutíferas (`crops`, `api.crops`, `api.trees`)

- [ ] Sementes, plantio, estágios por dia, rega, morte/murchamento fora da estação, colheita, rebrota e requisitos de foice — `Crop`, `CropData`, `CropRegistry`, `CropHelper`, `BlockHFCrops`, `ItemHFSeeds`, `ItemCrop`, `TileCrop`, `TileWithered`, `WateringTickHandler`.
- [ ] Cultivos: nabo, batata, pepino, morango, repolho, cebola, tomate, abóbora, abacaxi, melancia, milho, berinjela, espinafre, cenoura, batata-doce, pimentão verde, beterraba, grama/forragem, trigo e nabo de tutorial — `HFCrops`, `StateHandlerGrass`, `StateHandlerPumpkin`, `StateHandlerStem`, `StateHandlerWatermelon`, `GrowthHandlerPumpkin`, `GrowthHandlerSide`, `DropHandlerGrass`, `DropHandlerPotato`.
- [ ] Árvores de maçã, banana, laranja, pêssego e uva; folhas, frutos e crescimento próprio — `Tree`, `GrowthHandlerTree`, `StateHandlerTree`, `GrowthHandlerHFTree`, `GrowthHandlerApple`, `GrowthHandlerBanana`, `GrowthHandlerOrange`, `GrowthHandlerPeach`, `GrowthHandlerGrape`, `DropHandlerTree`, `BlockFruit`, `BlockLeavesFruit`, `BlockLeavesTropical`, `TileFruit`; `trees/*.json`.
- [ ] Nether wart registrado com crescimento no Nether; comentário do original pede reintrodução adequada em 0.7 — `HFCrops.NETHER_WART`, `GrowthHandlerNether`, `DropHandlerNetherWart`. Registrar a implementação existente; não presumir disponibilidade normal na loja.
- [ ] Desbloqueios de sementes por ano, rancho ou missão; preços e alimentação animal — `HFCrops`, `SpecialRulesYear`, `SpecialRulesRanch`, `SpecialRulesQuest`, `Crop`.
- [ ] Aspersores e compatibilidade do aspersor antigo — `BlockSprinkler`, `TileSprinkler`, `TileSprinklerOld`.
- [ ] Opções de crescimento diário, farinha de osso e restrição sazonal, consumo do aspersor e morte por falta de água distinta em servidor integrado/dedicado — `HFCrops`, `BlockHFCrops`, `TileCrop`.
- [ ] Conversão/receitas de sementes, associação com cultivos vanilla, bloqueios configuráveis de sementes, enxadas, drops, umidade e crescimento vanilla — `SeedRecipeHandler`, `HFCrops`, `DisableHandler`, `CropRegistry`.
- [ ] Modelos/tintas de cultivos e folhas e loot com tipo de cultivo — `CropStateMapper`, `BakedCrops`, `BakedLeaves`, `BakedTintedQuad`, `SetCropType`.

## Ferramentas, esforço e efeitos (`tools`, `core.base.item`)

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
- [ ] Próxima fatia: preços de cultivos — apenas registrada, sem implementação iniciada. Permanecem pendentes aniversário (definição conforme o original, persistência e sincronização) e validação integrada (servidor dedicado, dois jogadores, permissões, reconexão e persistência).
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
