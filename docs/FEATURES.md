# Inventário de funcionalidades do original

Auditoria estática de 30/10/2026, limitada a `reference/original-1.12.2/` (branch de referência declarada: `1.12.2-0.6.X`). Os checkboxes controlam a **portagem**, não a leitura: nenhum conteúdo foi implementado. Classes abaixo pertencem a `joshie.harvest`; os pacotes indicados identificam sua localização em `src/main/java/` do original. Recursos são relativos a `src/main/resources/assets/harvestfestival/` do original.

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


## Catálogo de variantes de itens, blocos e ferramentas

Declarações de enum ativas dos itens/blocos e de `ITiered.ToolTier`. Cada linha enumera todas as variantes da classe indicada; variantes de interface/diagnóstico não são automaticamente itens obteníveis em sobrevivência. Regras e disponibilidade estão nas classes e seções acima.

- [ ] Variantes de `animals.block.BlockSizedStorage.SizedStorage`: `INCUBATOR`.
- [ ] Variantes de `animals.block.BlockSizedStorage.Fill`: `EMPTY`, `SMALL`, `MEDIUM`, `LARGE`.
- [ ] Variantes de `animals.block.BlockTray.Tray`: `NEST_EMPTY`, `SMALL_CHICKEN`, `MEDIUM_CHICKEN`, `LARGE_CHICKEN`, `FEEDER_EMPTY`, `FEEDER_FULL`.
- [ ] Variantes de `animals.block.BlockTrough.Trough`: `WOOD`.
- [ ] Variantes de `animals.block.BlockTrough.Section`: `SINGLE`, `END`, `MIDDLE`.
- [ ] Variantes de `animals.item.ItemAnimalProduct.Sizeable`: `EGG`, `MILK`, `MAYONNAISE`, `WOOL`.
- [ ] Variantes de `animals.item.ItemAnimalSpawner.Spawner`: `COW`, `SHEEP`, `CHICKEN`.
- [ ] Variantes de `animals.item.ItemAnimalTool.Tool`: `MILKER`, `BRUSH`, `MEDICINE`, `CHICKEN_FEED`, `MIRACLE_POTION`.
- [ ] Variantes de `animals.item.ItemAnimalTreat.Treat`: `COW`, `SHEEP`, `CHICKEN`, `GENERIC`.
- [ ] Variantes de `api.core.ITiered.ToolTier`: `BASIC`, `COPPER`, `SILVER`, `GOLD`, `MYSTRIL`, `CURSED`, `BLESSED`, `MYTHIC`.
- [ ] Variantes de `buildings.item.ItemCheat.Cheat`: `COORD_SETTER`, `CODE_GENERATOR`, `AIR_PLACER`, `AIR_REMOVER`, `PARK_PLACER`, `PARK_ENDSTONE`, `PARK_LOCATIONS_GENERATOR`, `ORE_CHECKER`, `BUILDING_PREVIEW`.
- [ ] Variantes de `cooking.block.BlockCookware.Cookware`: `FRIDGE_TOP`, `FRIDGE`, `COUNTER`, `POT`, `FRYING_PAN`, `MIXER`, `OVEN_OFF`, `OVEN_ON`, `COUNTER_IC`, `COUNTER_OC`.
- [ ] Variantes de `cooking.item.ItemIngredients.Ingredient`: `CURRY_POWDER`, `DUMPLING_POWDER`, `WINE`, `UNUSED1`, `UNUSED2`, `FLOUR`, `OIL`, `RICEBALL`, `SALT`, `CHOCOLATE`.
- [ ] Variantes de `cooking.item.ItemMeal.Meal`: `PANCAKE_SAVOURY`, `FRIES_FRENCH`, `POPCORN`, `CORNFLAKES`, `EGGPLANT_HAPPY`, `EGG_SCRAMBLED`, `OMELET`, `OMELET_RICE`, `TOAST_FRENCH`, `DOUGHNUT`, `FISH_GRILLED`, `PANCAKE`, `POTSTICKER`, `RISOTTO`, `JUICE_PINEAPPLE`, `JUICE_TOMATO`, `MILK_STRAWBERRY`, `JUICE_VEGETABLE`, `LATTE_VEGETABLE`, `KETCHUP`, `BUTTER`, `FISHSTICKS`, `TURNIP_PICKLED`, `CUCUMBER_PICKLED`, `SALAD`, `SANDWICH`, `SUSHI`, `SASHIMI`, `SASHIMI_CHIRASHI`, `MILK_HOT`, `CHOCOLATE_HOT`, `EGG_BOILED`, `SPINACH_BOILED`, `POTATO_CANDIED`, `DUMPLINGS`, `NOODLES`, `SOUP_RICE`, `PORRIDGE`, `EGG_OVERRICE`, `STEW`, `STEW_PUMPKIN`, `STEW_FISH`, `CORN_BAKED`, `RICEBALLS_TOASTED`, `TOAST`, `DINNERROLL`, `DORIA`, `COOKIES`, `COOKIES_CHOCOLATE`, `CAKE_CHOCOLATE`, `BURNT_COUNTER`, `BURNT_FRYING_PAN`, `BURNT_MIXER`, `BURNT_OVEN`, `BURNT_POT`, `STIR_FRY`, `RICE_FRIED`, `SOUFFLE_APPLE`, `BREAD_CURRY`, `NOODLES_THICK_FRIED`, `TEMPURA`, `CURRY_DRY`, `JUICE_GRAPE`, `JUICE_PEACH`, `JUICE_BANANA`, `JUICE_ORANGE`, `JUICE_APPLE`, `JUICE_FRUIT`, `LATTE_FRUIT`, `JUICE_MIX`, `LATTE_MIX`, `SANDWICH_FRUIT`, `RICE_BAMBOO`, `RICE_MATSUTAKE`, `RICE_MUSHROOM`, `BREAD_RAISIN`, `ICE_CREAM`, `JAM_STRAWBERRY`, `JAM_APPLE`, `JAM_GRAPE`, `MARMALADE`, `NOODLES_TEMPURA`, `RICE_TEMPURA`, `BUN_JAM`, `SWEET_POTATOES`, `CAKE`, `PIE_APPLE`, `SALAD_HERB`, `SOUP_HERB`, `SANDWICH_HERB`.
- [ ] Variantes de `cooking.item.ItemUtensil.Utensil`: `BLADE`, `KNIFE`.
- [ ] Variantes de `core.block.BlockDecorative.DecorativeBlock`: `COOKING`.
- [ ] Variantes de `core.block.BlockFlower.FlowerType`: `GODDESS`, `WEED`, `MOONDROP`, `TOY`, `PINKCAT`, `BLUE_MAGICGRASS`, `RED_MAGICGRASS`.
- [ ] Variantes de `core.block.BlockStand.Stand`: `POT`, `PLATE`.
- [ ] Variantes de `core.block.BlockStorage.Storage`: `SHIPPING`, `MAILBOX`, `BASKET`.
- [ ] Variantes de `crops.block.BlockFruit.Fruit`: `APPLE`, `BANANA`, `GRAPE`, `ORANGE`, `PEACH`.
- [ ] Variantes de `crops.block.BlockHFCrops.CropType`: `FRESH`, `WITHERED`, `FRESH_DOUBLE`, `WITHERED_DOUBLE`.
- [ ] Variantes de `crops.block.BlockLeavesFruit.LeavesFruit`: `APPLE`, `GRAPE`, `ORANGE`, `PEACH`.
- [ ] Variantes de `crops.block.BlockLeavesTropical.LeavesTropical`: `BANANA`.
- [ ] Variantes de `crops.block.BlockSprinkler.Sprinkler`: `IRON`, `OLD`.
- [ ] Variantes de `crops.item.ItemCrop.Crops`: `TURNIP`, `POTATO`, `CUCUMBER`, `STRAWBERRY`, `CABBAGE`, `ONION`, `TOMATO`, `CORN`, `PUMPKIN`, `PINEAPPLE`, `WATERMELON`, `EGGPLANT`, `SPINACH`, `CARROT`, `SWEET_POTATO`, `GREEN_PEPPER`, `BEETROOT`, `GRASS`, `WHEAT`, `NETHER_WART`, `TUTORIAL_TURNIP`, `BANANA`, `GRAPE`, `ORANGE`, `PEACH`.
- [ ] Variantes de `fishing.block.BlockFishTrap.Aquatic`: `TRAP`, `TRAP_BAITED`.
- [ ] Variantes de `fishing.block.BlockHatchery.Floating`: `HATCHERY`.
- [ ] Variantes de `fishing.item.ItemFish.Fish`: `ANCHOVY`, `ANGEL`, `ANGLER`, `BASS`, `BLAASOP`, `BOWFIN`, `BUTTERFLY`, `CARP`, `CATFISH`, `CHUB`, `CLOWN`, `COD`, `DAMSEL`, `ELECTRICRAY`, `GOLD`, `HERRING`, `KOI`, `LAMPREY`, `LUNGFISH`, `MANTARAY`, `MINNOW`, `PERCH`, `PICKEREL`, `PIKE`, `PIRANHA`, `PUFFER`, `PUPFISH`, `SALMON`, `SARDINE`, `SIAMESE`, `STARGAZER`, `STINGRAY`, `TANG`, `TETRA`, `TROUT`, `TUNA`, `WALLEYE`.
- [ ] Variantes de `fishing.item.ItemJunk.Junk`: `CAN`, `BOOT`, `TREASURE`, `BONES`, `FOSSIL`, `BAIT`.
- [ ] Variantes de `gathering.block.BlockNature.NaturalBlock`: `MATSUTAKE`, `BAMBOO`, `MINT`, `CHAMOMILE`, `LAVENDER`.
- [ ] Variantes de `gathering.block.BlockRock.Rock`: `STONE_SMALL`, `STONE_MEDIUM`, `STONE_LARGE`, `BOULDER_SMALL`, `BOULDER_MEDIUM`, `BOULDER_LARGE`.
- [ ] Variantes de `gathering.block.BlockWood.Wood`: `BRANCH_SMALL`, `BRANCH_MEDIUM`, `BRANCH_LARGE`, `STUMP_SMALL`, `STUMP_MEDIUM`, `STUMP_LARGE`.
- [ ] Variantes de `knowledge.item.ItemBook.Book`: `STATISTICS`, `CALENDAR`.
- [ ] Variantes de `mining.block.BlockDirt.TextureStyle`: `BLANK`, `INNER`, `VERTICAL`, `HORIZONTAL`, `OUTER`.
- [ ] Variantes de `mining.block.BlockElevator.Elevator`: `EMPTY`, `JUNK`.
- [ ] Variantes de `mining.block.BlockLadder.Ladder`: `WOOD`, `DECORATIVE`.
- [ ] Variantes de `mining.block.BlockOre.Ore`: `ROCK`, `COPPER`, `SILVER`, `GOLD`, `MYSTRIL`, `GEM`, `EMERALD`, `DIAMOND`, `RUBY`, `AMETHYST`, `TOPAZ`, `JADE`.
- [ ] Variantes de `mining.block.BlockPortal.Portal`: `MINE_TL`, `MINE_TM`, `MINE_TR`, `MINE_BL`, `MINE_BM`, `MINE_BR`, `MINE_TL_EW`, `MINE_TM_EW`, `MINE_TR_EW`, `MINE_BL_EW`, `MINE_BM_EW`, `MINE_BR_EW`, `INTERNAL_TL`, `INTERNAL_TM`, `INTERNAL_TR`, `INTERNAL_BL`, `INTERNAL_BM`, `INTERNAL_BR`, `INTERNAL_TL_EW`, `INTERNAL_TM_EW`, `INTERNAL_TR_EW`, `INTERNAL_BL_EW`, `INTERNAL_BM_EW`, `INTERNAL_BR_EW`, `STONE_TL`, `STONE_TM`, `STONE_TR`, `STONE_BL`, `STONE_BM`, `STONE_BR`, `STONE_TL_EW`, `STONE_TM_EW`, `STONE_TR_EW`, `STONE_BL_EW`, `STONE_BM_EW`, `STONE_BR_EW`.
- [ ] Variantes de `mining.block.BlockPortal.Type`: `MINE`, `INTERNAL`, `OVERWORLD`.
- [ ] Variantes de `mining.block.BlockStone.Type`: `REAL`, `DECORATIVE_BLANK`, `DECORATIVE_PURPLE`, `DECORATIVE_SILVER`, `DECORATIVE_GREEN`, `DECORATIVE_BLUE`, `DECORATIVE_RED`, `LADDER_HOLE`.
- [ ] Variantes de `mining.item.ItemDarkDrop.DarkDrop`: `FEATHER`, `LEATHER`.
- [ ] Variantes de `mining.item.ItemDarkSpawner.DarkSpawner`: `COW`, `SHEEP`, `CHICKEN`, `CHICK`.
- [ ] Variantes de `mining.item.ItemMaterial.Material`: `JUNK`, `COPPER`, `SILVER`, `GOLD`, `MYSTRIL`, `MYTHIC`, `ADAMANTITE`, `AGATE`, `ALEXANDRITE`, `AMETHYST`, `FLUORITE`, `MOON_STONE`, `ORICHALC`, `PERIDOT`, `PINK_DIAMOND`, `RUBY`, `SAND_ROSE`, `TOPAZ`, `JADE`, `COIN_GOLD`, `COIN_SILVER`, `COIN_COPPER`.
- [ ] Variantes de `mining.item.ItemMiningTool.MiningTool`: `ESCAPE_ROPE`, `ELEVATOR_CABLE`.
- [ ] Variantes de `npcs.item.ItemNPCTool.NPCTool`: `BLUE_FEATHER`, `NPC_KILLER`, `GIFT`, `SPEECH`, `MAIL`.
- [ ] Variantes de `quests.block.BlockQuestBoard.QuestBlock`: `BOARD`, `ACTIVE`.

## Catálogo de registros de conteúdo

Constantes dos pontos centrais de registro; as funcionalidades estão descritas acima. A integração Blood Magic permanece adiada.

- [ ] Registro `TURNIP` — classe `HFCrops`; identificador inicial `turnip`.
- [ ] Registro `POTATO` — classe `HFCrops`; identificador inicial `potato`.
- [ ] Registro `CUCUMBER` — classe `HFCrops`; identificador inicial `cucumber`.
- [ ] Registro `STRAWBERRY` — classe `HFCrops`; identificador inicial `strawberry`.
- [ ] Registro `CABBAGE` — classe `HFCrops`; identificador inicial `cabbage`.
- [ ] Registro `ONION` — classe `HFCrops`; identificador inicial `onion`.
- [ ] Registro `TOMATO` — classe `HFCrops`; identificador inicial `tomato`.
- [ ] Registro `PUMPKIN` — classe `HFCrops`; identificador inicial `pumpkin`.
- [ ] Registro `PINEAPPLE` — classe `HFCrops`; identificador inicial `pineapple`.
- [ ] Registro `WATERMELON` — classe `HFCrops`; identificador inicial `watermelon`.
- [ ] Registro `CORN` — classe `HFCrops`; identificador inicial `corn`.
- [ ] Registro `BANANA` — classe `HFCrops`; identificador inicial `banana`.
- [ ] Registro `ORANGE` — classe `HFCrops`; identificador inicial `orange`.
- [ ] Registro `PEACH` — classe `HFCrops`; identificador inicial `peach`.
- [ ] Registro `EGGPLANT` — classe `HFCrops`; identificador inicial `eggplant`.
- [ ] Registro `SPINACH` — classe `HFCrops`; identificador inicial `spinach`.
- [ ] Registro `CARROT` — classe `HFCrops`; identificador inicial `carrot`.
- [ ] Registro `SWEET_POTATO` — classe `HFCrops`; identificador inicial `sweet_potato`.
- [ ] Registro `GREEN_PEPPER` — classe `HFCrops`; identificador inicial `green_pepper`.
- [ ] Registro `BEETROOT` — classe `HFCrops`; identificador inicial `beetroot`.
- [ ] Registro `APPLE` — classe `HFCrops`; identificador inicial `apple`.
- [ ] Registro `GRAPE` — classe `HFCrops`; identificador inicial `grape`.
- [ ] Registro `GRASS` — classe `HFCrops`; identificador inicial `grass`.
- [ ] Registro `WHEAT` — classe `HFCrops`; identificador inicial `wheat`.
- [ ] Registro `NETHER_WART` — classe `HFCrops`; identificador inicial `nether_wart`.
- [ ] Registro `TUTORIAL` — classe `HFCrops`; identificador inicial `tutorial_turnip`.
- [ ] Registro `GODDESS` — classe `HFNPCs`; identificador inicial `goddess`.
- [ ] Registro `CARPENTER` — classe `HFNPCs`; identificador inicial `yulif`.
- [ ] Registro `FLOWER_GIRL` — classe `HFNPCs`; identificador inicial `jade`.
- [ ] Registro `GS_OWNER` — classe `HFNPCs`; identificador inicial `jenni`.
- [ ] Registro `MILKMAID` — classe `HFNPCs`; identificador inicial `candice`.
- [ ] Registro `BARN_OWNER` — classe `HFNPCs`; identificador inicial `jim`.
- [ ] Registro `POULTRY` — classe `HFNPCs`; identificador inicial `ashlee`.
- [ ] Registro `TRADER` — classe `HFNPCs`; identificador inicial `girafi`.
- [ ] Registro `FISHERMAN` — classe `HFNPCs`; identificador inicial `jacob`.
- [ ] Registro `MINER` — classe `HFNPCs`; identificador inicial `brandon`.
- [ ] Registro `CAFE_OWNER` — classe `HFNPCs`; identificador inicial `liara`.
- [ ] Registro `CAFE_GRANNY` — classe `HFNPCs`; identificador inicial `katlin`.
- [ ] Registro `BLACKSMITH` — classe `HFNPCs`; identificador inicial `daniel`.
- [ ] Registro `CLOCKMAKER` — classe `HFNPCs`; identificador inicial `tiberius`.
- [ ] Registro `CLOCKMAKER_CHILD` — classe `HFNPCs`; identificador inicial `fenn`.
- [ ] Registro `PRIEST` — classe `HFNPCs`; identificador inicial `thomas`.
- [ ] Registro `MAYOR` — classe `HFNPCs`; identificador inicial `jamie`.
- [ ] Registro `DAUGHTER_ADULT` — classe `HFNPCs`; identificador inicial `cloe`.
- [ ] Registro `DAUGHTER_CHILD` — classe `HFNPCs`; identificador inicial `abi`.
- [ ] Registro `BARN` — classe `HFBuildings`; identificador inicial `barn`.
- [ ] Registro `BLACKSMITH` — classe `HFBuildings`; identificador inicial `blacksmith`.
- [ ] Registro `CAFE` — classe `HFBuildings`; identificador inicial `cafe`.
- [ ] Registro `CARPENTER` — classe `HFBuildings`; identificador inicial `carpenter`.
- [ ] Registro `CHURCH` — classe `HFBuildings`; identificador inicial `church`.
- [ ] Registro `CLOCKMAKER` — classe `HFBuildings`; identificador inicial `clockmaker`.
- [ ] Registro `FISHING_HOLE` — classe `HFBuildings`; identificador inicial `fishing_hole`.
- [ ] Registro `FISHING_HUT` — classe `HFBuildings`; identificador inicial `fishing_hut`.
- [ ] Registro `GODDESS_POND` — classe `HFBuildings`; identificador inicial `goddess_pond`.
- [ ] Registro `MINING_HILL` — classe `HFBuildings`; identificador inicial `mining_hill`.
- [ ] Registro `POULTRY_FARM` — classe `HFBuildings`; identificador inicial `poultry_farm`.
- [ ] Registro `SUPERMARKET` — classe `HFBuildings`; identificador inicial `supermarket`.
- [ ] Registro `TOWNHALL` — classe `HFBuildings`; identificador inicial `townhall`.
- [ ] Registro `FESTIVAL_GROUNDS` — classe `HFBuildings`; identificador inicial `festivals`.
- [ ] Registro `BARN` — classe `HFShops`; identificador inicial `barn`.
- [ ] Registro `CAFE` — classe `HFShops`; identificador inicial `cafe`.
- [ ] Registro `CARPENTER` — classe `HFShops`; identificador inicial `carpenter`.
- [ ] Registro `POULTRY` — classe `HFShops`; identificador inicial `poultry`.
- [ ] Registro `SUPERMARKET` — classe `HFShops`; identificador inicial `general`.
- [ ] Registro `MINER` — classe `HFShops`; identificador inicial `miner`.
- [ ] Registro `BAITSHOP` — classe `HFShops`; identificador inicial `baitshop`.
- [ ] Registro `CLOCKMAKER` — classe `HFShops`; identificador inicial `clockmaker`.
- [ ] Registro `BLOODMAGE` — classe `HFShops`; identificador inicial `bloodmage`. **Integração Blood Magic adiada.**
- [ ] Registro `KITCHEN` — classe `HFShops`; identificador inicial `kitchen`.
- [ ] Registro `TRADER` — classe `HFShops`; identificador inicial `trader`.
- [ ] Registro `COOKING_FESTIVAL_FOOD` — classe `HFShops`; identificador inicial `cooking`.
- [ ] Registro `COOKING_FESTIVAL_RECIPES` — classe `HFShops`; identificador inicial `recipes`.
- [ ] Registro `COW_FESTIVAL_BARGAINS` — classe `HFShops`; identificador inicial `cow`.
- [ ] Registro `COW_FESTIVAL_DAIRY_QUEEN` — classe `HFShops`; identificador inicial `dairy`.
- [ ] Registro `SHEEP_FESTIVAL_SALE` — classe `HFShops`; identificador inicial `sheep`.
- [ ] Registro `SHEEP_FESTIVAL_KNITTNG` — classe `HFShops`; identificador inicial `knitting`.
- [ ] Registro `CHICKEN_FESTIVAL_MFC` — classe `HFShops`; identificador inicial `mfc`.
- [ ] Registro `NEW_YEARS` — classe `HFFestivals`; identificador inicial `new_years`.
- [ ] Registro `COOKING_CONTEST` — classe `HFFestivals`; identificador inicial `cooking`.
- [ ] Registro `CHICKEN_FESTIVAL` — classe `HFFestivals`; identificador inicial `chicken`.
- [ ] Registro `COW_FESTIVAL` — classe `HFFestivals`; identificador inicial `cow`.
- [ ] Registro `HARVEST_FESTIVAL` — classe `HFFestivals`; identificador inicial `harvest`.
- [ ] Registro `SHEEP_FESTIVAL` — classe `HFFestivals`; identificador inicial `sheep`.
- [ ] Registro `STARRY_NIGHT` — classe `HFFestivals`; identificador inicial `starry_night`.
- [ ] Registro `NEW_YEARS_EVE` — classe `HFFestivals`; identificador inicial `new_years_eve`.

## Catálogo completo de missões anotadas

Declarações ativas `@HFQuest`, com identificador e classe qualificada. Missões-base e auxiliares sem anotação estão cobertas na seção de missões; identificadores preservam a rastreabilidade ao original.

- [ ] Missão `friendship.abii.parents` — `quests.player.friendship.QuestAbii10KHappyFamily`.
- [ ] Missão `friendship.abii.town` — `quests.player.friendship.QuestAbii15KHappyTown`.
- [ ] Missão `friendship.abii.cookies` — `quests.player.friendship.QuestAbii5KFreeCookies`.
- [ ] Missão `friendship.ashlee.notes` — `quests.player.friendship.QuestAshlee15KNotes`.
- [ ] Missão `friendship.brandon.ore` — `quests.player.friendship.QuestBrandon15KOres`.
- [ ] Missão `friendship.candice.notes` — `quests.player.friendship.QuestCandice15KNotes`.
- [ ] Missão `friendship.cloe.gift` — `quests.player.friendship.QuestCloe15KGift`.
- [ ] Missão `friendship.danieru.notes` — `quests.player.friendship.QuestDanieru15KNotes`.
- [ ] Missão `friendship.fenn.parents` — `quests.player.friendship.QuestFenn10KHappyDad`.
- [ ] Missão `friendship.fenn.plants` — `quests.player.friendship.QuestFenn15KForaging`.
- [ ] Missão `friendship.fenn.salad` — `quests.player.friendship.QuestFenn5KSalad`.
- [ ] Missão `friendship.goddess.strawberry` — `quests.player.friendship.QuestGoddess15KStrawberries`.
- [ ] Missão `friendship.jacob.hatchery` — `quests.player.friendship.QuestJacob15KHatchery`.
- [ ] Missão `friendship.jade.flowers` — `quests.player.friendship.QuestJade15KFlowers`.
- [ ] Missão `friendship.jenni.wednesday` — `quests.player.friendship.QuestJenni15KWednesday`.
- [ ] Missão `friendship.jim.notes` — `quests.player.friendship.QuestJim15KNotes`.
- [ ] Missão `friendship.johan.vanilla` — `quests.player.friendship.QuestJohan15KStore`.
- [ ] Missão `friendship.katlin.kitchen` — `quests.player.friendship.QuestKatlin15KStore`.
- [ ] Missão `friendship.liara.meals` — `quests.player.friendship.QuestLiara15KMeals`.
- [ ] Missão `friendship.tiberius.dark` — `quests.player.friendship.QuestTiberius15KStore`.
- [ ] Missão `friendship.tomas.cheaper` — `quests.player.friendship.QuestTomas15KCheaperBlessing`.
- [ ] Missão `friendship.yulif.axe` — `quests.player.friendship.QuestYulif100FreeAxe`.
- [ ] Missão `friendship.yulif.sprinkler` — `quests.player.friendship.QuestYulif15KSprinkler`.
- [ ] Missão `meeting.abi` — `quests.player.meetings.QuestMeetAbii`.
- [ ] Missão `tutorial.chicken` — `quests.player.meetings.QuestMeetAshlee`.
- [ ] Missão `tutorial.mining` — `quests.player.meetings.QuestMeetBrandon`.
- [ ] Missão `meeting.candice` — `quests.player.meetings.QuestMeetCandice`.
- [ ] Missão `meeting.cloe` — `quests.player.meetings.QuestMeetCloe`.
- [ ] Missão `tutorial.upgrading` — `quests.player.meetings.QuestMeetDanieru`.
- [ ] Missão `meeting.fenn` — `quests.player.meetings.QuestMeetFenn`.
- [ ] Missão `tutorial.intro` — `quests.player.meetings.QuestMeetGoddess`.
- [ ] Missão `meeting.jacob` — `quests.player.meetings.QuestMeetJacob`.
- [ ] Missão `tutorial.crops` — `quests.player.meetings.QuestMeetJade`.
- [ ] Missão `meeting.jeimmi` — `quests.player.meetings.QuestMeetJeimmi`.
- [ ] Missão `tutorial.supermarket` — `quests.player.meetings.QuestMeetJenni`.
- [ ] Missão `tutorial.cow` — `quests.player.meetings.QuestMeetJim`.
- [ ] Missão `meeting.johan` — `quests.player.meetings.QuestMeetJohan`.
- [ ] Missão `meeting.katlin` — `quests.player.meetings.QuestMeetKatlin`.
- [ ] Missão `tutorial.cafe` — `quests.player.meetings.QuestMeetLiara`.
- [ ] Missão `meeting.tiberius` — `quests.player.meetings.QuestMeetTiberius`.
- [ ] Missão `meeting.tomas` — `quests.player.meetings.QuestMeetTomas`.
- [ ] Missão `tutorial.carpenter` — `quests.player.meetings.QuestMeetYulif`.
- [ ] Missão `recipe.popcorn` — `quests.player.recipes.QuestAshlee10KPopcorn`.
- [ ] Missão `recipe.curry.dry` — `quests.player.recipes.QuestAshlee20KDryCurry`.
- [ ] Missão `recipe.corn.baked` — `quests.player.recipes.QuestAshlee5KBakedCorn`.
- [ ] Missão `recipe.omelet.rice` — `quests.player.recipes.QuestBrandon10KOmeletRice`.
- [ ] Missão `recipe.rice.bamboo` — `quests.player.recipes.QuestBrandon20KBambooRice`.
- [ ] Missão `recipe.soup.rice` — `quests.player.recipes.QuestBrandon5KRiceSoup`.
- [ ] Missão `recipe.icecream` — `quests.player.recipes.QuestCandice10KIceCream`.
- [ ] Missão `recipe.latte.mix` — `quests.player.recipes.QuestCandice20KMixLatte`.
- [ ] Missão `recipe.milk.hot` — `quests.player.recipes.QuestCandice5KHotMilk`.
- [ ] Missão `recipe.risotto` — `quests.player.recipes.QuestCloe10KRisotto`.
- [ ] Missão `recipe.noodles.thick.fried` — `quests.player.recipes.QuestCloe20KFriedThickNoodles`.
- [ ] Missão `recipe.noodles` — `quests.player.recipes.QuestCloe5KNoodles`.
- [ ] Missão `recipe.egg.scrambled` — `quests.player.recipes.QuestDanieru10KScrambledEgg`.
- [ ] Missão `recipe.mayonnaise` — `quests.player.recipes.QuestDanieru20KMayonnaise`.
- [ ] Missão `recipe.butter` — `quests.player.recipes.QuestDanieru5KButter`.
- [ ] Missão `recipe.milk.strawberry` — `quests.player.recipes.QuestGoddess10KStrawberryMilk`.
- [ ] Missão `recipe.jam.strawberry` — `quests.player.recipes.QuestGoddess5KStrawberryJam`.
- [ ] Missão `recipe.fish.grilled` — `quests.player.recipes.QuestJacob10KGrilledFish`.
- [ ] Missão `recipe.fish.stew` — `quests.player.recipes.QuestJacob5KFishStew`.
- [ ] Missão `recipe.toast.french` — `quests.player.recipes.QuestJade10KFrenchToast`.
- [ ] Missão `recipe.jam.apple` — `quests.player.recipes.QuestJade20KJam`.
- [ ] Missão `recipe.bun_jam` — `quests.player.recipes.QuestJade25KJamBun`.
- [ ] Missão `recipe.toast` — `quests.player.recipes.QuestJade5KToast`.
- [ ] Missão `recipe.potsticker` — `quests.player.recipes.QuestJenni10KPotsticker`.
- [ ] Missão `recipe.stir.fry` — `quests.player.recipes.QuestJenni20KStirFry`.
- [ ] Missão `recipe.salad` — `quests.player.recipes.QuestJenni5KSalad`.
- [ ] Missão `recipe.pancake.savoury` — `quests.player.recipes.QuestJim10KSavouryPancakes`.
- [ ] Missão `recipe.bread.curry` — `quests.player.recipes.QuestJim20KCurryBread`.
- [ ] Missão `recipe.fishsticks` — `quests.player.recipes.QuestJim5KFishsticks`.
- [ ] Missão `recipe.fries.french` — `quests.player.recipes.QuestJohan10KFrenchFries`.
- [ ] Missão `recipe.potato.candied` — `quests.player.recipes.QuestJohan5KCandiedPotato`.
- [ ] Missão `recipe.stew` — `quests.player.recipes.QuestKatlin10KStew`.
- [ ] Missão `recipe.pie_apple` — `quests.player.recipes.QuestKatlin20KApplePie`.
- [ ] Missão `recipe.porridge` — `quests.player.recipes.QuestKatlin5KPorridge`.
- [ ] Missão `recipe.cookies.chocolate` — `quests.player.recipes.QuestLiara10KChocolateCookies`.
- [ ] Missão `recipe.cake` — `quests.player.recipes.QuestLiara12KCake`.
- [ ] Missão `recipe.tempura` — `quests.player.recipes.QuestLiara17KTempura`.
- [ ] Missão `recipe.tempura.rice` — `quests.player.recipes.QuestLiara20KTempuraRice`.
- [ ] Missão `recipe.tempura.noodles` — `quests.player.recipes.QuestLiara22KTempuraNoodles`.
- [ ] Missão `recipe.chocolate.hot` — `quests.player.recipes.QuestLiara5KHotChocolate`.
- [ ] Missão `recipe.cookies` — `quests.player.recipes.QuestLiara7KCookies`.
- [ ] Missão `recipe.doughnut` — `quests.player.recipes.QuestTiberius10KDoughnut`.
- [ ] Missão `recipe.bread.raisin` — `quests.player.recipes.QuestTiberius20KRaisinBread`.
- [ ] Missão `recipe.dinnerroll` — `quests.player.recipes.QuestTiberius5KDinnerroll`.
- [ ] Missão `recipe.dumplings` — `quests.player.recipes.QuestTomas10KDumplings`.
- [ ] Missão `recipe.latte.fruit` — `quests.player.recipes.QuestTomas20KFruitLatte`.
- [ ] Missão `recipe.latte.vegetable` — `quests.player.recipes.QuestTomas5KVegetableLatte`.
- [ ] Missão `recipe.cake.chocolate` — `quests.player.recipes.QuestYulif10KChocolateCake`.
- [ ] Missão `recipe.souffle.apple` — `quests.player.recipes.QuestYulif20KAppleSouffle`.
- [ ] Missão `recipe.juice.pineapple` — `quests.player.recipes.QuestYulif5KPineapple`.
- [ ] Missão `schedule.katlin.baking` — `quests.player.schedule.QuestKatlinSundayBakingFreeCake`.
- [ ] Missão `trade.upgrade` — `quests.player.trade.QuestBlacksmithing`.
- [ ] Missão `trade.cursed` — `quests.player.trade.QuestBlessing`.
- [ ] Missão `trade.seeds` — `quests.player.trade.QuestFlowerTrader`.
- [ ] Missão `trade.carpenter` — `quests.player.trade.QuestGoddessCarpenter`.
- [ ] Missão `trade.bless` — `quests.player.trade.QuestPriestRepair`.
- [ ] Missão `trade.tools` — `quests.player.trade.QuestToolTrader`.
- [ ] Missão `building.goddess` — `quests.town.building.QuestGoddessPond`.
- [ ] Missão `festival.chicken` — `quests.town.festivals.QuestContestChicken`.
- [ ] Missão `festival.cooking` — `quests.town.festivals.QuestContestCooking`.
- [ ] Missão `festival.cow` — `quests.town.festivals.QuestContestCow`.
- [ ] Missão `festival.sheep` — `quests.town.festivals.QuestContestSheep`.
- [ ] Missão `festival.harvest` — `quests.town.festivals.QuestHarvestFestival`.
- [ ] Missão `festival.new.years` — `quests.town.festivals.QuestNewYears`.
- [ ] Missão `festival.new.years.eve` — `quests.town.festivals.QuestNewYearsEve`.
- [ ] Missão `festival.starry.night` — `quests.town.festivals.QuestStarryNight`.
- [ ] Missão `item.hatchery` — `quests.town.friendship.QuestHatchery`.
- [ ] Missão `item.meals` — `quests.town.friendship.QuestMeals`.
- [ ] Missão `item.ores` — `quests.town.friendship.QuestOres`.
- [ ] Missão `item.sprinkler` — `quests.town.friendship.QuestSprinkler`.
- [ ] Missão `shop.wednesday` — `quests.town.friendship.QuestWednesday`.
- [ ] Missão `seeds.cabbage` — `quests.town.seeds.QuestCabbage`.
- [ ] Missão `seeds.pineapple` — `quests.town.seeds.QuestPineapple`.
- [ ] Missão `seeds.progress` — `quests.town.seeds.QuestProgress`.
- [ ] Missão `seeds.sweetpotato` — `quests.town.seeds.QuestSweetPotato`.
- [ ] Missão `collect.crops` — `quests.town.tasks.QuestCollectCrops`.
- [ ] Missão `collect.fish` — `quests.town.tasks.QuestCollectFish`.
- [ ] Missão `collect.materials` — `quests.town.tasks.QuestCollectWoodStone`.
- [ ] Missão `slay` — `quests.town.tasks.QuestSlay`.
- [ ] Missão `buyer.flowers` — `quests.town.trade.QuestFlowerBuyingAllow`.
- [ ] Missão `trader.flowers` — `quests.town.trade.QuestFlowerBuyingDo`.

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

### Recursos de comportamento

Dados complementares ao código; texturas/modelos/sons/traduções estão cobertos como recursos de apresentação.

- [ ] Dados de `advancements` (35) — classes: `HFAdvancements`, `EventTrigger`: `advancements/tasks/birthday.json`, `advancements/tasks/cabbage.json`, `advancements/tasks/cooking.json`, `advancements/tasks/copper.json`, `advancements/tasks/corn.json`, `advancements/tasks/cucumber.json`, `advancements/tasks/egger.json`, `advancements/tasks/egger_large.json`, `advancements/tasks/eggplant.json`, `advancements/tasks/first_christmas.json`, `advancements/tasks/first_shipping.json`, `advancements/tasks/friend.json`, `advancements/tasks/gold.json`, `advancements/tasks/green_pepper.json`, `advancements/tasks/harvest.json`, `advancements/tasks/junk.json`, `advancements/tasks/kill_chick.json`, `advancements/tasks/kill_chicken.json`, `advancements/tasks/kill_cow.json`, `advancements/tasks/kill_sheep.json`, `advancements/tasks/milker.json`, `advancements/tasks/milker_large.json`, `advancements/tasks/millionaire.json`, `advancements/tasks/mystril.json`, `advancements/tasks/onion.json`, `advancements/tasks/pineapple.json`, `advancements/tasks/recipes.json`, `advancements/tasks/root.json`, `advancements/tasks/silver.json`, `advancements/tasks/spinach.json`, `advancements/tasks/strawberry.json`, `advancements/tasks/summon.json`, `advancements/tasks/sweet_potato.json`, `advancements/tasks/the_mine.json`, `advancements/tasks/tomato.json`.
- [ ] Dados de `buildings` (14) — classes: `HFBuildings`, `BuildingRegistry`: `buildings/barn.json`, `buildings/blacksmith.json`, `buildings/cafe.json`, `buildings/carpenter.json`, `buildings/church.json`, `buildings/clockmaker.json`, `buildings/festivals.json`, `buildings/fishing_hole.json`, `buildings/fishing_hut.json`, `buildings/goddess_pond.json`, `buildings/mining_hill.json`, `buildings/poultry_farm.json`, `buildings/supermarket.json`, `buildings/townhall.json`.
- [ ] Dados de `festivals` (9) — classes: `HFFestivals`, `BuildingFestival`: `festivals/chicken.json`, `festivals/cooking.json`, `festivals/cow.json`, `festivals/harvest.json`, `festivals/new_years.json`, `festivals/new_years_eve.json`, `festivals/none.json`, `festivals/sheep.json`, `festivals/starry_night.json`.
- [ ] Dados de `trees` (5) — classes: `HFCrops`, `GrowthHandlerHFTree`: `trees/apple.json`, `trees/banana.json`, `trees/grape.json`, `trees/orange.json`, `trees/peach.json`.
- [ ] Dados de `loot_tables` (53) — classes: `LootStrings`, `LootHelper`, `MiningHelper`, `FishingHelper`: `loot_tables/chests/blacksmith.json`, `loot_tables/chests/cafe.json`, `loot_tables/chests/carpenter_jade.json`, `loot_tables/chests/carpenter_yulif.json`, `loot_tables/chests/clockworker_bedroom.json`, `loot_tables/chests/festival_cow.json`, `loot_tables/chests/festival_sheep.json`, `loot_tables/chests/fishing.json`, `loot_tables/chests/market_basement.json`, `loot_tables/chests/market_bedroom.json`, `loot_tables/chests/miner.json`, `loot_tables/chests/poultry.json`, `loot_tables/chests/townhall_passage.json`, `loot_tables/chests/townhall_teenager.json`, `loot_tables/frames/barn.json`, `loot_tables/frames/blacksmith.json`, `loot_tables/frames/cafe.json`, `loot_tables/frames/carpenter.json`, `loot_tables/frames/church.json`, `loot_tables/frames/clockworker.json`, `loot_tables/frames/fishing.json`, `loot_tables/frames/market_bedroom.json`, `loot_tables/frames/market_entry.json`, `loot_tables/frames/miner.json`, `loot_tables/frames/poultry.json`, `loot_tables/frames/townhall_child.json`, `loot_tables/frames/townhall_entry.json`, `loot_tables/frames/townhall_mayor.json`, `loot_tables/frames/townhall_priest.json`, `loot_tables/frames/townhall_teenager.json`, `loot_tables/gameplay/entities/dark_chick.json`, `loot_tables/gameplay/entities/dark_chicken.json`, `loot_tables/gameplay/entities/dark_cow.json`, `loot_tables/gameplay/entities/dark_sheep.json`, `loot_tables/gameplay/fishing/lake_autumn.json`, `loot_tables/gameplay/fishing/lake_spring.json`, `loot_tables/gameplay/fishing/lake_summer.json`, `loot_tables/gameplay/fishing/lake_winter.json`, `loot_tables/gameplay/fishing/ocean_autumn.json`, `loot_tables/gameplay/fishing/ocean_spring.json`, `loot_tables/gameplay/fishing/ocean_summer.json`, `loot_tables/gameplay/fishing/ocean_winter.json`, `loot_tables/gameplay/fishing/pond_autumn.json`, `loot_tables/gameplay/fishing/pond_spring.json`, `loot_tables/gameplay/fishing/pond_summer.json`, `loot_tables/gameplay/fishing/pond_winter.json`, `loot_tables/gameplay/fishing/river_autumn.json`, `loot_tables/gameplay/fishing/river_spring.json`, `loot_tables/gameplay/fishing/river_summer.json`, `loot_tables/gameplay/fishing/river_winter.json`, `loot_tables/gameplay/fishing/trap_junk.json`, `loot_tables/gameplay/mining/gems.json`, `loot_tables/gameplay/mining/junk.json`.
