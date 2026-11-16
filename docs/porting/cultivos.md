# Cultivos — contrato vigente

Aceite global da fatia 15 informado pelo responsável em 16/11/2026, incluindo 15-A, 15-V e ampliação da rega. Sucede as pendências de aceite dessas entregas abaixo, sem atribuir resultados individuais de testes ou execução de cliente ao agente. Cenário 8 e dedicado/multiplayer permanecem pendentes; indicadores do regador funcionais e memória de camas por dimensão adiada.

## Levantamento original da 16-A — 16/11/2026

Somente evidência por leitura, sem execução do original ou implementação da 16. Plano e decisões de recorte permanecem na conversa, aguardando aprovação independente de 16-F.

| Espécie | Estação | Limites visuais / maturidade | Retorno / máximo total | Compra / venda / cor |
| --- | --- | --- | --- | --- |
| strawberry | Primavera | 3/6/8/9 | 7 / 12 | 800 / 50 / 0xFF7BEA |
| tomato | Verão | 2/4/6/9/10 | 7 / 8 | 500 / 70 / 0xE60820 |
| eggplant | Outono | 3/6/9/10 | 7 / 8 | 600 / 80 / 0xA25CC4 |

- Fontes: `HFCrops`, `Crop`, `RegistryHelper.addHandlersToCrop`, prefixos de `HFModInfo` e diretórios `crops/handlers/{drop,growth,state,rules}`. Não existem classes convencionadas Strawberry/Tomato/Eggplant nesses diretórios; permanecem DropHandler/GrowthHandler padrões. `setStages(int...)` instala StateHandlerBasic, e as regras comerciais explícitas são YEAR3/YEAR2/YEAR2. Busca dirigida dos usos não encontrou sobrescrita posterior no núcleo; integrações/scripts continuam adiados.
- `ItemHFSeeds`, `CropData`, `GrowthHandler`: estágio inicial 1, seca 1, colheitas zero; água obrigatória, um bloco, sem requiresSickle/double. Primeira maturidade em 8/9/9 viradas regadas; rebrota em 2/3/3. StateHandlerBasic usa limites inclusivos: retorno 7 seleciona imagem 3 do morango/berinjela e imagem 4 do tomate. Ciclo completo exige 30 viradas em cada espécie (8+11×2 ou 9+7×3), além do limite da estação se plantado no dia 1; teste administrativo não autoriza mudar calendário.
- `TileCrop`, `WateringHandler`, `CropData.newDay`: umidade 7 ou chuva HF hidrata antes de avaliar morte/crescimento; fora da estação morre, mesmo regado. Seca >2 mata quando opção habilitada (integrado true/dedicado false); água zera seca, crescimento diário padrão true, farinha de osso padrão false. Chuva e substituição aleatória na morte continuam fora do contrato entregue, sem alegar paridade dessas partes.
- `CropData.harvest`, `CropRegistry.harvestCrop`, `CropHelper`, `BlockHFCrops`: produto maduro único; incrementa contador antes de comparar máximo, inclusive lista vazia elegível. Interação sem agachar retorna a 7 até a última, que remove; quebrar remove mesmo com foice, sem preservar rebrota. Murcho/imaturo não entrega produto. Criativo também passa por removedByPlayer; callback da ferramenta não é executado no caminho criativo normal (patch oficial Forge 1.12.x de PlayerInteractionManager). Proteção contra falha de escrita/snapshot e política advance são adaptações já contratadas, não garantias do original.
- Recursos conferidos: blockstates `crops_strawberry/tomato/eggplant.json` apontam a `tinted_crop`, com 4/5/4 texturas em `textures/blocks/crops`; produtos em `textures/items/crops`. `crops_seeds.json` usa neck/body e HFCrops.initClient colore ambas as camadas. Nomes en_us/pt_br: Strawberry/Morango, Tomato/Tomate, Eggplant/Berinjela. HFCrops.preInit registra cropStrawberry/cropTomato/cropEggplant e venda; tags modernas são adaptação do OreDictionary. Inspeção de arquivos não comprova aparência em jogo.
- `HFShops` registra sementes de custo positivo no supermercado; `PurchasableCropSeeds` exige estação e regra. `SpecialRulesYear` consulta aniversário da cidade mais próxima e CalendarHelper.getYearsPassed: diferença não negativa entre pisos dos totais de dias divididos pela duração do ano, exigindo 2/1/1. Não equivale simplesmente ao ano global nem a contagem de aniversários completos. Aquisição por comandos/abas não implementa estes serviços.
- `ItemCrop` também define alimentação original: morango 2/0,6, tomate 2/0,5, berinjela 1/0,5 (fome/modificador de saturação). Produtos HF atuais não implementam alimentação; inclusão ou adiamento explícito dessas propriedades integra a decisão de recorte, não deve ser presumido pela semelhança com pepino.

Fatias 10–15-A; atualização em 15/11/2026. Pacote `harvestfestival.crops` aprovado em produção e testes. Dependências: etapas 5/6 de `DEPENDENCIES.md`, [Ferramentas](ferramentas.md), [Calendário](calendario.md) e [Remessas](remessas.md). Contrato anterior integral em [histórico da fatia 12](../archive/fatia-12-cultivos-anterior.md).

## Decisões e comportamento

- O responsável aprovou o plano com a correção arquitetural de seguir o original: um bloco compartilhado `harvestfestival:crop`, com `CropBlockEntity`, em vez de `turnip_crop` por espécie. O estágio lógico não é propriedade de blockstate.
- Cultivos: nabo comum `harvestfestival:turnip` e pepino `harvestfestival:cucumber`, com sementes `<cultivo>_seeds` e produtos de mesmo identificador do cultivo; aquisição por comando, sem lojas, receitas, alimentação ou culinária. O desbloqueio comercial YEAR2 do pepino original permanece dependente de lojas/cidade.
- Clique superior com semente exige primeiro que o bloco clicado pertença à tag de solo da definição e permita edição; somente então tenta um quadrado 3×3 centrado acima dele. Cada posição exige tag de solo, dois espaços de ar e permissões no solo/planta/acima. Sucesso parcial consome um saco; nenhum sucesso não consome. O fluxo vanilla conserva quantidade em criativo. Grama/terra clicada não permite plantar vizinhos arados.
- Restrição do centro solicitada explicitamente pelo responsável em 14/11/2026: divergência intencional do original. `ItemHFSeeds.onItemUse` só verifica a face antes de percorrer o 3×3; `plantSeedAt` e `GrowthHandler.canPlantSeedAt` verificam o solo de cada destino, sem exigir farmland no centro original para nabo/pepino. Não atribuir a restrição nova ao código 1.12.2.
- Nasce no estágio 1 e contador de seca 1. Em primavera, rega permite avançar uma unidade por evento: 1→2→3→4→5. São quatro viradas regadas. Estágios visuais originais agrupados por limites 2/4/5.
- Não há verificação de estação no plantio. Na virada, fora da estação, morre mesmo com água. Sem água não cresce. Contador hidratado é zero; após atualização sobrevivida é incrementado. Morte por seca ocorre ao entrar no ciclo com contador maior que 2, habilitada no integrado e desabilitada no dedicado por dados, conforme padrões originais. Contador satura em limite+1, sem overflow após inatividade longa.
- Rega no solo ou na planta utiliza o contrato existente `ToolActions.apply`; tag de cultivos resolve o solo imediatamente abaixo. Permissões são verificadas tanto no alvo clicado quanto no solo resolvido. Regador tem prioridade sobre colheita e mantém enchimento/capacidade/consumo originais da fatia 9.
- Nabo: colheita madura por clique sem agachar remove a planta antes de emitir o produto; falha de remoção ou loot vazio não gera item nem remove a planta. Quebra madura mantém o caminho anterior de loot; imatura/murcha não fornece produto nem semente. Solo permanece preparado. Não exige foice.
- Produto tem preço 60 via `c:crops/turnip`, reutilizando `ShippingPrices` e `PlayerShipping` sem alterar sua lógica. Depósito fixa preço; pagamento ocorre na próxima virada conectado.

### Pepino e rebrota — fatia 12 aprovada

- Primavera, água obrigatória, estágio inicial 1, seca inicial 1 e contador `harvests=0`. Limites visuais originais 3/6/9/10; maturidade 10 após nove viradas regadas. Colheitas 1–7 conservam a entidade e retornam a 7; cada rebrota exige três viradas regadas. A oitava entrega um pepino e remove a planta, sem produzir semente ou estado murcho por esgotamento. Preço 40 via `c:crops/cucumber`, oito produtos somam 320.
- `CropData.harvest` usa o estágio anterior para resolver o produto e depois retorna ao estágio de rebrota; `CropRegistry.harvestCrop` chama `hasCompletedMaxHarvests`, que incrementa antes de comparar `>= maxHarvests`. Não são oito rebrotas além da primeira colheita. `DropHandler` fornece exatamente um produto maduro. Evidência por leitura do original, não por execução em 1.12.2.
- Pepino não declara `setBecomesDouble`, `setRequiresSickle` ou handlers laterais: é uma planta de um bloco. `Crop` fornece primavera, necessidade de água, handler sazonal e drop padrão; `HFCrops.CUCUMBER` define `setStages(3,6,9,10)`, `setRegrow(7)`, `setMaxHarvests(8)`, venda 40 e cor da semente `0x36B313`.
- Clique sem agachar colhe e conserva a planta antes do limite; clique imaturo/murcho não colhe. Regador continua prioritário. `BlockHFCrops.removedByPlayer` colhe quando possível e depois remove a posição: quebrar pepino maduro entrega um produto e destrói a planta em qualquer colheita; imaturo/murcho não dá produto. A preservação especial de foice exige `requiresSickle`, ausente no pepino. Sem parte superior ou drops duplicados. Ferramentas completas/corte em área e remoção especial pela enxada continuam fora do recorte.
- O hook original também é chamado em criativo pelo [patch Forge 1.12.x de PlayerInteractionManager](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/patches/minecraft/net/minecraft/server/management/PlayerInteractionManager.java.patch); HF não restringe o produto ao survival. O hook moderno do pepino resolve produto no servidor independentemente de `willHarvest`. O caminho anterior do nabo não foi redefinido.
- Colher conserva seca e água do solo; não hidrata, não zera seca e não cresce. Rebrota recebe a mesma morte sazonal/por seca e o mesmo padrão integrado/dedicado do nabo. Só umidade 7 cresce; ordem crescimento→secagem, idempotência, ausência de compensação e migração permanecem existentes. Após esgotamento o solo permanece no estado atual e pode voltar à terra na atualização posterior já seca sem planta.
- O ciclo completo requer 30 viradas regadas (9+7×3). Plantado na primavera 1, não completa oito colheitas antes do verão sob calendário normal. Validação do limite usa reposicionamento administrativo de dia dentro da primavera; não alterar estação, duração ou crescimento do cultivo para facilitar o teste. Morte sazonal é validada separadamente.
- Plano aprovado pelo responsável em 13/11/2026: política de loot vazio por dados, proteção contra falha de escrita e política de recarga abaixo. A aparência provisória foi substituída pela correção solicitada em 14/11/2026; decisão anterior preservada no [histórico de aparência/plantio](../archive/fatia-12-aparencia-anterior.md). Condição adicional obrigatória: todo item adicionado em uma fatia deve trazer textura original. Não autoriza alterar AGENTS.md.

## Recortes aprovados e referência

- Referência consultada somente nos caminhos relevantes: `HFCrops`, `Crop`, `StateHandlerBasic`, `ItemHFSeeds`, `GrowthHandler`, `DropHandler`, `CropData`, `CropRegistry`, `TileCrop`, `WateringHandler`, `WateringTickHandler`, `DailyTickHandler`, regador e busca dirigida de pisoteio.
- `HFCrops.TURNIP` declara `setStages(2,4,5)` e venda 60. `ItemHFSeeds` inicia estágio 1 e tenta 3×3. `CropData`/`GrowthHandler` confirmam quatro incrementos até 5; o tooltip antigo chama o total de estágios de dias, não sendo usado para adicionar um dia artificial.
- Original atualiza cultivo em PRE e solo em MAIN. O port faz duas passagens coordenadas: todas as plantas avaliam rega/crescimento, depois todos os solos participantes secam. Não depende da ordem de listeners independentes.
- Original admite chuva pelo calendário/clima HF. Port aprovado mantém regador como única fonte, sem chuva, hidratação por proximidade ou aspersores.
- Original pode substituir plantas mortas aleatoriamente por madeira/pedra/erva daninha. Recorte aprovado usa somente estado murcho removível, sem drops. Esses recursos não foram antecipados.
- Original desidrata farmland 7→3→0 em dias sem chuva; só 7 conta como regado. Atualização posterior com 0 pode retornar terra se não houver planta. A fatia 11, autorizada após o aceite, adota a farmland compartilhada com essa sequência e retorno à terra sem planta; queda usa condições equivalentes verificadas em Forge/NeoForge. Contrato anterior preservado em [histórico do solo](../archive/fatia-11-solo-anterior.md); detalhes e migração em [Ferramentas](ferramentas.md).

## Formato dos dados e política de recarga

Um recurso `data/<namespace>/crops/<caminho>.json` define `<namespace>:<caminho>`. A leitura aceita namespaces de datapack; não registra novos blocos/itens durante recarga.

```json
{
  "seed_tag": "harvestfestival:seeds/turnip",
  "soil_tag": "harvestfestival:crop_soils",
  "seasons": ["spring"],
  "stages": [2, 4, 5],
  "appearances": [4, 5, 6],
  "requires_water": true,
  "drought_limit": 2,
  "dies_without_water_integrated": true,
  "dies_without_water_dedicated": false,
  "planting_radius": 1,
  "harvest_loot": "harvestfestival:crops/turnip"
}
```

- Todos os campos mostrados, exceto `appearances`, são obrigatórios. Estações válidas: spring/summer/autumn/winter, lista não vazia. `stages` contém limites visuais estritamente crescentes entre 1 e 1.000.000; último valor é maturidade. Não está limitado a cinco. Limite de seca: 0..999.999. Raio: 0 ou 1. Booleanos não aceitam strings; inteiros não aceitam frações nem strings. Campos desconhecidos não têm efeito.
- Campos novos opcionais: `regrow_stage` (padrão 0; zero remove, positivo deve ser inferior à maturidade), `max_harvests` (padrão 1; inteiro 1..1.000.000) e `empty_harvest_policy` (`preserve` padrão ou `advance`). Sem rebrota, máximo deve ser 1. Null, tipos incorretos, política desconhecida ou limites incompatíveis invalidam a definição. Nabo mantém esses defaults; pepino usa 7, 8 e `advance`.
- `appearances` é opcional, inclusive no exemplo acima: ausente ou lista vazia conserva o mapeamento provisório anterior. Lista não vazia deve ter um inteiro por limite de `stages`, com valores 0..10, exceto 3 (reservado à morte); null, frações, strings, tamanho ou valores inválidos descartam a definição. Nabo usa [4,5,6], pepino [7,8,9,10]. Esses valores selecionam modelos empacotados, não registram modelos ou espécies dinamicamente. Recarga não altera estágio/contador; cache visual é atualizado na próxima atualização da entidade ou carga segura do chunk.
- Política `advance` reproduz o original quando uma colheita elegível retorna lista vazia: avança estágio/contador e remove ao atingir o limite mesmo sem emitir produto. `preserve` conserva o contrato anterior do nabo. Pilhas vazias não são produtos. A interação que efetiva uma transição vazia é consumida para impedir outra ação de item no mesmo clique; adaptação técnica coberta pela política aprovada.
- Proteção aprovada: confirmar alteração/remoção antes de emitir qualquer produto. Falha não gasta colheita nem entrega item; um snapshot antigo não pode confirmar outra transição. Original não verifica todos esses retornos; não atribuir paridade de falhas ao port. Não se promete transação contra crash ou exceções de terceiros após confirmação.
- Sementes são associadas por tags, não por ID de espécie codificado na classe Java. Tags ausentes/vazias não correspondem. Duas definições que casem com a mesma semente recusam plantio com aviso, sem escolha dependente da ordem.
- Item precisa oferecer a interação `CropSeedItem` para plantar. Registradas sementes/produtos de nabo e pepino; não cria sementes com componentes, registros dinâmicos de itens ou interação para sementes vanilla por datapack.
- JSON malformado, campos/limites inválidos ou recurso de loot inexistente descartam apenas a definição efetiva, com aviso de identidade/motivo. Pack superior inválido não recupera regra inferior; recarga substitui integralmente a tabela. IDs/tags não criam conteúdo novo por si mesmos.
- A validação de estrutura/referências internas de loot pertence ao carregador vanilla. Loot ausente da tabela carregada ou sem drops não entrega produto; a transição segue `empty_harvest_policy`. Um recurso de loot inexistente já invalida a definição no catálogo. Erros de loot do carregador são distintos dos avisos do catálogo de cultivos.
- Sem definição válida, plantas existentes ficam congeladas e não colhem; solo ainda seca. Restaurar definição retoma da situação salva, sem compensação. Alterar maturidade não converte/zera estágios existentes; a próxima operação usa a definição atual.
- Recarga preserva estágio, seca, morte e número colhido. Novo estágio de rebrota vale na próxima colheita; diminuir máximo até/abaixo do contador bloqueia novos produtos sem remover automaticamente a planta. Quebra continua possível sem produto; aumentar o máximo permite retomar se os demais requisitos forem atendidos. É política técnica aprovada para datapacks, não mecanismo existente no original. Alterar máximo não ressuscita plantas já removidas.
- `data/harvestfestival/loot_table/crops/turnip.json` fornece um nabo. O loot vazio do bloco genérico evita drops padrão sem definição. Preço separado em `shipping_prices/tags/c/crops/turnip.json`, formato existente `{"price":60,"priority":0}`. Tags comuns `c:crops`, `c:crops/turnip` e `c:seeds`; tags de plantio/solo/cultivos no namespace HF.
- Pepino usa os caminhos paralelos `crops/cucumber`, `seeds/cucumber` e `shipping_prices/tags/c/crops/cucumber.json`, com preço 40/prioridade 0. Tags comuns incluem `c:crops/cucumber`, `c:crops` e `c:seeds`; nenhuma mudança no algoritmo econômico.

## Persistência e apresentação

- `CropBlockEntity` salva `crop_state`: por exemplo `{"crop":"harvestfestival:cucumber","stage":7,"dry_days":1,"dead":false,"harvests":1}`. Estágio 1..1.000.000, seca e colheitas 0..1.000.000 integrais. `harvests` ausente significa zero para saves anteriores; zero pode ser omitido na escrita. Campo presente inválido não vira zero. Ausência/erro de payload deixa identidade sentinela murcha, inerte, sem produto; falhas seguem diagnóstico vanilla. O codec elimina resultados parciais antes de `ValueInput.read`, que aceita parciais, e diferencia null explícito de campo ausente. JSON booleano é inválido; NBT não distingue booleano de byte numérico.
- O blockstate guarda apenas `appearance=0..10`, cache calculado da entidade/definição pelo servidor, sem limitar crescimento. Valores antigos 0/1/2 conservam modelos provisórios e 3 continua murcho; 4/5/6 selecionam nabo_1/2/3, 7/8/9/10 pepino_1/2/3/4. Os identificadores de arquivo são `turnip` e `cucumber`. Novos estados aceitam os valores antigos dos saves. Na fila já existente de carga, após `getChunkNow` confirmar FULL, a entidade reaplica seu mesmo `CropState` para atualizar somente aparência; não cresce, não altera seca/colheitas nem identidade diária. Usa bloco/entidade genéricos existentes, sem registro por espécie.
- Estado do bloco e entidade são salvos/sincronizados pelo chunk. `getUpdateTag` fornece dados para entrada no chunk; `ClientboundBlockEntityDataPacket` para atualizações. Sem payload próprio, ticker de crescimento, renderizador personalizado ou consulta do catálogo de servidor pelo cliente.
- Plantas vivas usam as sete texturas originais de `textures/blocks/crops/turnip_1..3.png` e `cucumber_1..4.png`, copiadas byte a byte para `textures/block/crops/`. Limites conferidos em `StateHandlerBasic`: nabo 1–2/3–4/5+, pepino 1–3/4–6/7–9/10+; rebrota em 7 volta à terceira textura do pepino. Modelo `crop_original` conserva os quatro planos e coordenadas do `models/block/tinted_crop.json` original, inclusive altura -1..15; remove `tintindex` pois `IStateHandler.getColor` retorna -1 para planta viva e explicita `minecraft:cutout`. Estado murcho continua com a aparência anteriormente aprovada.
- Item nabo agora usa `textures/items/crops/turnip.png` original, copiada para `textures/item/crops/turnip.png`. Suas sementes usam as duas texturas originais de saco já empacotadas, sem tint adicional (cor branca `0xFFFFFF` do original). Mantidos nomes/traduções, propriedades dos itens e gameplay. Licença MIT/copyright existentes preservados. Inspeção dos arquivos e comparação de hashes não constituem validação visual em jogo.
- Proveniência dos itens novos: `textures/items/crops/cucumber.png`, `seed_bag_neck.png` e `seed_bag_body.png` do Snownee/Harvest-Festival, branch 1.12.2-0.6.X, copiados byte a byte para `textures/item/` (produto em `crops/`). Modelo original `models/item/crops_seeds.json` usa neck em layer0 e body em layer1; `HFCrops.initClient` aplica a cor do cultivo aos dois índices. Definição moderna aplica `minecraft:constant` 3584787 (`0x36B313`) às duas camadas. Licença MIT/copyright continuam em `META-INF/licenses/harvest-festival-original.txt`. Imagens inspecionadas e hashes iguais conferidos; aparência em jogo ainda não validada.

## Atualização diária e idempotência

- `CropDailyUpdates` acompanha chunks por eventos Load/Unload; na carga só registra referência, sem acessar mundo antes de FULL. Na virada verifica `getChunkNow`, sem gerar/carregar chunks, e examina apenas seções cuja paleta possa conter tags de solo/cultivo.
- Fatia 11: carga também agenda migração do solo legado para a primeira oportunidade segura no servidor, antes da atualização de cultivos. A conversão não cresce a planta, não altera seus contadores nem a identidade do último ciclo. Solo atual e critérios de perda do preparo em [Ferramentas](ferramentas.md).
- Primeira passagem avalia plantas usando água existente; segunda reduz umidade 7→3→0; em zero e sem planta da tag maintains_farmland retorna terra. Inclui solos vazios. Usa estação global do evento existente em todas as dimensões participantes; não porta climas/estações locais do original.
- Attachment de chunk `harvestfestival:crop_cycle`, campo `last_cycle`, armazena o último identificador concluído. Alteração marca chunk para salvar. Ciclo repetido/antigo não cresce nem apaga uma rega nova feita depois do primeiro processamento.
- Identificadores vêm de `DailyCycleData`, descrito em [Calendário](calendario.md). Recuar data não bloqueia futuras viradas; saltar muitos dias continua gerando no máximo uma atualização observada. Comandos HF de data não geram evento por si mesmos.
- Chunk descarregado preserva estado/água e não recebe atualizações. Carregar/reabrir não processa ciclos passados; na próxima virada recebe apenas um ciclo. A marca de água pode permanecer enquanto descarregado, como consequência deliberada da ausência de compensação.
- Marcador ausente em chunk preexistente começa em zero. Payload de marcador presente inválido ou sem campo usa Long.MAX_VALUE, impedindo replay; exige correção administrativa dos dados para retomar. Sequência global inválida/esgotada fornece ciclo zero: cultivos param, calendário/remessas continuam; diagnóstico no log. Não há recuperação automática que possa duplicar crescimento.
- Garantia cobre thread do servidor, repetição normal e salvar/reabrir. Não promete transação durável contra crash, falha de disco ou exceção intermediária de terceiros.

## API e verificação na versão alvo

Fontes locais exatas `minecraft-patched-26.2.0.93-sources.jar` e `neoforge-26.2.0.93-sources.jar`, com compilação aprovada:

- `BaseEntityBlock.codec/newBlockEntity`, `BlockEntityType` público, `ValueInput/ValueOutput`, `loadAdditional/saveAdditional`, `getUpdateTag/getUpdatePacket`, `ClientboundBlockEntityDataPacket.create`, `Level.sendBlockUpdated`.
- `BlockBehaviour.useItemOn/useWithoutItem/canSurvive/updateShape/getDrops`, `LootParams`, `LootContextParamSets.BLOCK`, `reloadableRegistries().getLootTable`, `Block.getDrops/popResource`. PASS deixa o regador executar sua interação; fonte de `ServerPlayerGameMode` conferida.
- `ChunkEvent.Load/Unload`, restrição de não interagir com mundo em Load antes de FULL, `ServerChunkCache.getChunkNow`, `LevelChunkSection.maybeHas/getBlockState`, `ChunkAccess.getSections/getSectionYFromSectionIndex/markUnsaved`, attachment serializável em chunk.
- APIs de calendário/SavedData documentadas em [Calendário](calendario.md); ferramentas, recarga básica e mixin restrito à umidade já documentados em [Ferramentas](ferramentas.md).
- Núcleo público: `CropLifecycle.newDay/canHarvest/afterHarvest`, `CropSeedItem.plant(Plot,...)`, `CropBlock.harvestOnce/breakOnce`, `CropBlockEntity.commitHarvest`, `CropDailyUpdates.process(ChunkWork,...)`, `CropDefinitions.get/forSeed`. `afterHarvest` retorna estado retido ou null para remoção; `harvestOnce` recebe confirmação por `Predicate<CropState>` (null remove). `commitHarvest` compara snapshot/remoção antes de alterar; estado retido usa `setCrop`, marca para salvar e sincroniza. Adaptação de mundo usa essas mesmas rotinas.
- Fatia 12: fontes locais exatas conferidas para `IBlockExtension.onDestroyedByPlayer`, `ServerPlayerGameMode.destroyBlock/removeBlock`, `Block.playerDestroy/getDrops/popResource`, `BlockEntity.setChanged`, `ItemTintSources`, `Constant.MAP_CODEC`, `CuboidItemModelWrapper.Unbaked` e `ItemModelGenerator` (tint index por camada). Hook de quebra no servidor apenas para definição com rebrota; confirma remoção e invalida o snapshot da entidade removida antes de emitir produto. A segunda consulta vanilla de loot encontra estado inerte e não duplica produto. Não há estado transitório salvo, ticker, mixin ou pacote cliente novo. Caminho sem rebrota conserva a quebra anterior do nabo.

## Validação e limites

Em 14/11/2026 o responsável informou «Tudo certo com o pepino», aceitando o comportamento entregue sem relato individual por cenário, e solicitou correções da aparência das plantas, nabo e restrição do clique de plantio. As correções passaram no build com **3 testes novos, 105 totais executados**, zero falhas/erros/ignorados: clique em grama/terra com vizinhos arados sem consumo (ambas as sementes), limites visuais/rebrota e codec dos estados, compatibilidade visual antiga e rejeição de mapeamentos inválidos. Regressões de crescimento/remessa/migração aprovadas. Nenhum teste automatizado de carga completa de mundo ou renderização é atribuído; `runClient` não executado pelo agente. Posteriormente, em 14/11/2026, o responsável confirmou «Confirmado a fatia»: aceite global da fatia 12 e dessas correções, sem atribuir execução individual de testes ou observação ao agente. Dedicado/multiplayer, cenário 8 e indicadores continuam pendentes.

Fontes alvo locais 26.2.0.93: `IntegerProperty`/`BlockState.CODEC`, sincronização de entidade e carregamento FULL já documentados; `CuboidModel`/`CuboidModelElement` conferidos para geometria, coordenadas, faces, shade e ambientocclusion. Modelos usam caminho cutout existente no port. Nenhuma API cliente própria, payload, ticker ou mixin novo.

Fatia 12, 13/11/2026: build aprovado; **15 testes novos, 102 totais efetivamente executados**, zero falhas/erros/ignorados. Seis novos testes de rebrota/quebra cobrem 30 viradas e oito colheitas, falhas/snapshot antigo, loot vazio, quebra sem retenção/segunda consulta real de drops, estados inelegíveis e seca/resgate/estação. Três de persistência cobrem NBT comprimido/payload, save antigo e corrupção sem resultado parcial; três de definições cobrem defaults/limites e recarga; um de plantio, um de identidade diária na rebrota e um de remessas a 320. Regressões anteriores de nabo, farmland, migração e remessas passaram.

Testes usam registros/tags/recursos reais e adaptadores de ações; teste de quebra chama a entrada real `getDrops` após remoção para verificar a segunda passagem vazia. Não simulam `ServerPlayerGameMode`, cancelamentos externos, cliques de cliente ou loot aleatório em mundo: o produto primário é fornecido pelo adaptador. Ausência de compensação e rebase são testes de núcleo, não descarga real de mundo. Texturas foram conferidas por hash e inspeção de arquivo, sem validação de renderização. Na entrega inicial, `runClient` não foi executado pelo agente e o aceite manual estava pendente; aceite posterior da fatia/correções registrado acima. Dedicado/multiplayer e cenário 8 continuam pendentes.

Build aprovado em 11/11/2026. 24 testes novos, suíte total 81, efetivamente executados; zero falhas/erros/ignorados. Testes usam registros, tags e recursos reais do mod, adaptadores de ações sem mundo e serialização vanilla/NeoForge. Não simulam cliques de cliente, servidor dedicado ou dois jogadores.

Fatia 11: regressões adaptadas para farmland compartilhada e aprovadas; suíte total 87, 6 novos testes de solo, todos executados. Cobertura e limites da migração/mixin em [Ferramentas](ferramentas.md). Após o aceite da fatia 10, o responsável também informou «Testado e aprovado» para a fatia 11 em 12/11/2026: aceite global single player, sem detalhamento por cenário nem ampliação às pendências anteriores.

- Plantio: nove alvos, repetição, sucesso parcial, face, solo por tag, espaço, permissões e falha de escrita.
- Regador: resolução por tag ao clicar na planta, mesmo solo, consumo, repetição e nova rega após secagem; mantém testes anteriores de permissões/reserva/ambiguidade.
- Crescimento: quatro incrementos, maturidade, seca/morte/resgate, estação e padrões integrado/dedicado; regra ausente; definição de 50 estágios sem outro bloco.
- Ordem diária: duas passagens, solo vazio, repetição que preserva água nova, ausência de chunk sem replay, definição ausente ainda secando, attachment preservado e marcador inválido.
- Colheita: madura/imatura, falha de remoção, loot vazio e emissão posterior à remoção; sem duplicação. Integração testa produto real com tag/preço empacotado e fila existente, pagamento único após NBT comprimido. Esse teste fornece o drop pelo adaptador, não executa sorteio de loot num mundo.
- Persistência: entidade registrada recriada por `BlockEntity.loadStatic`, solo e NBT comprimido; payload de atualização e dados inválidos. Teste de payload não comprova tráfego nem aparência em cliente.
- Recarga: recursos de packs reais, precedência, superior inválido sem fallback, remoção/restauração, ambiguidade e referência de loot ausente. Sequência: persistência, recuo administrativo e corrupção/esgotamento conservador.

runClient não foi executado pelo agente na implementação. Em 12/11/2026 o responsável informou «Teste finalizado e aprovado», encerrando o aceite do ciclo single player entregue, sem detalhar resultados adicionais por passo. Servidor dedicado e multiplayer permanecem pendentes. Cenário 8 e aparência do complemento visual do regador continuam pendentes no aceite daquele complemento; nenhum teste de cultivo ou codec encerra essas obrigações.

## Esclarecimentos após o aceite — 12/11/2026

- **Cultivo murcho sem drops:** corresponde ao caminho WITHERED do original. `BlockHFCrops.removedByPlayer` retorna `world.setBlockToAir(pos)` antes da colheita, e `getItemDropped` retorna `Items.AIR`. O arbusto é aparência do cultivo HF morto, não um `minecraft:dead_bush` com loot vanilla. Não adicionar gravetos ou sementes por inferência visual. Ausência de drop não comprova bloco fantasma; incapacidade de remover/persistência indevida após quebrar seria outro problema, não relatado de forma inequívoca.
- **Destino aleatório na morte:** `TileCrop` pode converter a posição em madeira/pedra/erva daninha; são blocos substitutos, não drops imediatos de toda planta morta. Esse comportamento ainda falta ao port, conforme recorte aprovado, e deve ser coberto nas fatias funcionais da Fase 3 junto dos recursos necessários.
- **Chunks descarregados:** `DailyTickHandler.processTickableBlocks` só chama `newDay` se `world.isBlockLoaded(pos)`. `TileHarvest.validate` reinscreve a posição; `CropData` persiste estágio/água, sem data de última atualização ou laço de compensação. No caminho diário padrão consultado, não crescer descarregado e não recuperar dias na volta é comportamento do original, não uma simplificação criada pelo port. A evidência é leitura do código de referência, não execução adicional de 1.12.2. Chunk mantido carregado por outro mecanismo não se enquadra como descarregado.
- O sistema agrícola completo continua obrigação da Fase 3. Aceite desta fatia não encerra catálogo, ferramentas completas, chuva ou demais regras vanilla; solo compartilhado passa a ser tratado pela fatia 11, ver [Ferramentas](ferramentas.md) e FEATURES. Integrações específicas e dependências de outros sistemas continuam explicitamente delimitadas.

## Fatia 13 — trigo e corte por ferramenta

Plano aprovado em 14/11/2026. Trigo HF reutiliza crop/entidade/ciclo: primavera, verão e outono, estágio inicial 1, limites 2/5/9/12/16/22/27/28, água obrigatória e 27 viradas regadas. Produto único minecraft:wheat; preço existente 100 por c:crops/wheat. Não altera sementes ou regras globais vanilla.

Evidência por leitura de HFCrops.WHEAT, Crop, CropData, CropRegistry, GrowthHandler, DropHandler e BlockHFCrops: requiresSickle impede interação; minimumCut=0 admite tentativa imatura, mas DropHandler só fornece produto maduro. Corte sempre remove; mão/ferramenta inadequada e planta imatura/murcha não dão produto. Criativo percorre remoção com produto quando maduro e foice. Não houve execução do original.

Definição recebe harvest_tool_tag opcional; ausência mantém os contratos anteriores. minimum_cut aceita somente zero; rebrota com ferramenta é rejeitada. Tags vazias não autorizam produto. Loot condicionado à ferramenta, remoção confirmada e invalidação da entidade evitam segunda emissão. Falha de remoção não fornece produto; loot vazio remove. Recarga conserva estado e identidade diária; definição inválida congela crescimento e impede produto.

Slots visuais 11–18 usam minecraft:block/wheat_stage0..7; 0–10 preservados. Saco usa texturas originais existentes com cor 0xEAC715 nas duas camadas. Modelos vanilla atuais foram autorizados, sem promessa de igualdade de pixels com 1.12.2. Nenhum novo campo persistido no cultivo.

A velocidade de quebra dos cultivos passa a seguir BlockHFCrops: dureza viva 0,1/murcha 0,5, divisor 10 com tag de foice e 100 sem; modificadores do jogador preservados. Fontes alvo locais 26.2.0.93: getDestroyProgress, Player.getDestroySpeed, ItemInstance no parâmetro TOOL e onDestroyedByPlayer. Ciclo, farmland, migração e pagamentos permanecem existentes.

Validação automatizada da fatia 13: build aprovado; 14 testes novos, 119 totais efetivamente executados, zero falhas/erros/ignorados. runClient não executado nesta entrega; aceite visual e manual pendente. Testes usam adaptadores, registros e serialização reais, sem simular cliente ou servidor dedicado.

Em 14/11/2026 o responsável declarou a fatia 13 completa. Aceite global, sem atribuir execução individual de cenários ou testes ao agente. Cenário 8, indicadores do regador e dedicado/multiplayer permanecem pendentes; fadiga, desmaio e recuperação seguem para a fatia 14.

## Correção obrigatória dos handlers

A proposta anterior dizia que a busca não havia localizado associações de DropHandlerGrass/DropHandlerPotato e que abóbora não associava GrowthHandlerPumpkin. Essa conclusão era incorreta: `RegistryHelper.registerCrop → addHandlersToCrop` instancia classes por reflexão/convenção usando prefixos de HFModInfo. A anotação unused não indica código abandonado.

- Grass: DropHandlerGrass e StateHandlerGrass associados; `setStages(11)` não troca state. Drop stage−5 desde 6. `maxHarvests=1` permanece. Registro/remoção precedem markSafe; não inferir rebrota ilimitada. Necessária validação real dos callbacks de topo/base na 18-B.
- Potato: DropHandlerPotato associado e não sobrescrito em HFCrops; `setStages(Blocks.POTATOES,...)` troca apenas visual. Madura gera uma entrada, com 1/20 de segunda; cada entrada tem 1/50 de venenosa. Condição do laço sorteada a cada avaliação.
- Pumpkin: GrowthHandlerPumpkin e StateHandlerPumpkin associados e preservados por `setStages(15)`. Herda GrowthHandlerSide; sem regrow, vira bloco na própria posição, orientação horizontal aleatória. É mecanismo ativo a portar, não restauração de funcionalidade abandonada.
- Watermelon: StateHandlerWatermelon via reflexão; GrowthHandlerSide explícito. Sem regrow, mesma transformação, estágio 11. O nome Side não demonstra produção lateral.
- Sobrescritas gerais: `setStages(int...)` instala StateHandlerBasic; `setStages(Block,int...)` instala StateHandlerBlock; inteiro único só muda total. Crescimento de nether wart substituído por SOUL_SAND. Plugins/scripts adiados não foram tratados como padrão.

Caminhos efetivos: TileCrop → CropData.newDay/grow → growthHandler; BlockHFCrops → CropHelper → CropRegistry.harvestCrop → CropData.harvest → dropHandler. Evidência estática; não houve execução do original. Árvores, nabo tutorial e nether wart exigem recortes próprios.

## Política vanilla — evidências do levantamento

HFCrops preserva itens vanilla. Defaults: DISABLE_VANILLA_SEEDS/GROWTH/DROPS/WHEAT_SEEDS/MOISTURE=true; DISABLE_VANILLA_HOE=false. DisableHandler aplica controles distintos; RegistryHelper também desliga ticks aleatórios de plantas quando growth está ativo. Blacklist de sementes é compartilhada, populada quando plantio ou drops da grama é bloqueado; isso não funde as duas opções.

SeedRecipeHandler recebe saco HF como ingrediente e gera sementes vanilla (nove para trigo/beterraba/melão/abóbora). Cenoura/batata não recebem essa receita, pois produto e semente são o mesmo item. Receitas adicionais de HFCrops: trigo→1 semente vanilla, duas beterrabas→duas sementes vanilla, bloco de melancia→nove fatias. ItemHFSeeds planta 3×3 usando identidade de cultivo; crop provider não converte inventário.

Aceite 15-V: testar cada chave isolada e combinações que compartilham blacklist/CROPS; mão principal/secundária, criativo/survival e itens vanilla existentes; negar plantio sem remover itens, preservar alimentação, distinguir crescimento aleatório/farinha de osso e colheita/drop, comprovar direção/quantidade de cada receita. Só então registrar relação vanilla concluída para os cultivos cobertos. Sem bloqueio global ou receita inversa na fatia 14.


## Fatia 14 — cebola e espinafre

Implementação aprovada pelo responsável em 14/11/2026; validação automatizada em 15/11/2026. IDs onion/onion_seeds e spinach/spinach_seeds, bloco genérico existente. Cebola: verão, limites 3/7/8, sete viradas regadas; espinafre: outono, 2/5/6, cinco viradas. Ambos iniciam em estágio 1/seca 1, água obrigatória, colheita única, sem foice/rebrota, um produto e preço 100 por c:crops/<espécie>. Compra original 350 documentada, sem loja implementada.

Evidência por leitura de HFCrops, Crop, StateHandlerBasic, GrowthHandler, DropHandler, CropData, CropRegistry, TileCrop, BlockHFCrops e ItemHFSeeds. Mesmos contratos de plantio 3×3/centro arado, morte sazonal/seca, resgate, loot vazio, falha de escrita e remoção antes do drop. Integrado morre quando entra com seca >2; dedicado mantém o padrão sem morte por seca. Chuva/aspersores e substituição aleatória na morte continuam adiados. Nenhum bloqueio ou conversão de sementes vanilla adicionado.

Slots 19–21 representam cebola em 1–3/4–7/8+; slots 22–24 representam espinafre em 1–2/3–5/6+. Faixa 0–18 preservada; nenhum campo novo em CropState. Mesma entidade/sincronização/ciclo, sem registro de bloco por espécie. Sementes entram após trigo na aba Sementes; produtos após nabo/pepino somente na busca, conforme abas aprovadas.

Oito PNGs originais copiados byte a byte de textures/items/crops/{onion,spinach}.png e textures/blocks/crops/{onion,spinach}_{1,2,3}.png para diretórios modernos item/block. Todos os hashes conferidos iguais. Sacos reutilizam neck/body originais, tintas 0xDCC307/0x90AE15 em ambas as camadas; modelos usam crop_original existente. Licença MIT já empacotada preservada. Hash e inspeção de arquivos não constituem teste visual no jogo.

Quatro testes novos de cultivos cobrem crescimento/limites visuais/estação/sementes, seca/resgate/dedicado, colheita/falhas/loot vazio/não duplicação e entidades em NBT comprimido. Um teste novo de remessas cobre nove unidades de cada produto, 900 por fila, rejeição de sementes e pagamento único após persistência. Testes existentes de abas/fixtures e limite inválido de aparência ajustados; regressões de nabo/pepino/trigo executadas. Produto de colheita é fornecido pelo adaptador; não simula sorteio de loot num mundo. Total da fatia: 20 testes novos, 139 totais executados e aprovados; sem execução manual atribuída.

Preferência do responsável em 14/11/2026: não acumular arquivos de plano; roteiro manual somente na resposta final após implementação. Plano da fatia e proposta agrícola anterior removidos com autorização, após incorporar evidências, correção rastreável e sequência em FEATURES. Indicadores do regador confirmados funcionais; cenário 8, dedicado/multiplayer e validação manual desta entrega continuam pendentes.

Aceite global da fatia 14 pelo responsável em 15/11/2026, sem atribuir execução individual ao agente. Conferência adicional da semente de cebola: HFCrops.ONION usa 0xDCC307, WHEAT usa 0xEAC715; initClient aplica a mesma cor às duas camadas do modelo crops_seeds. Port mantém esses valores distintos e neck/body com SHA256 idênticos aos originais. A semelhança do saco é original; nenhum recurso visual alterado. Conferência estática, sem nova sessão de cliente. Cenário 8 e dedicado/multiplayer continuam pendentes.

## Fatia 15-A — cenoura, beterraba e batata

Plano aprovado pelo responsável após a 15-F, com continuidade explícita em 15/11/2026. Implementação nos pacotes existentes, sem alterar ferramentas, fome, carga, durabilidade ou política vanilla 15-V. Aquisição das sementes por comandos/aba Sementes; preços de compra e desbloqueios abaixo são evidência, não lojas/receitas implementadas.

| Cultivo / semente HF | Estação | Limites / viradas regadas desde estágio 1 | Produto vanilla / venda | Compra original / cor do saco |
| --- | --- | --- | --- | --- |
| carrot / carrot_seeds | Outono | 1/2/3/4/5/6/7/8 / 7 | minecraft:carrot / 160 | 500 / 0xF8AC33 |
| beetroot / beetroot_seeds | Outono | 1/2/3/4 / 3 | minecraft:beetroot / 120 | 550, TOWN_PROGRESS (progress) / 0x690000 |
| potato / potato_seeds | Primavera | 1/2/3/4/5/6/7/8 / 7 | minecraft:potato / 80; minecraft:poisonous_potato / 1 | 350 / 0xBE8D2B |

Evidência por leitura de `crops/HFCrops`, `api/crops/Crop`, `StateHandlerBlock`, `GrowthHandler`, `DropHandler`, `crops/CropData`, `CropRegistry`, `TileCrop`, `BlockHFCrops`, `ItemHFSeeds`, `core/helpers/RegistryHelper` e `HFModInfo`. Não houve execução do original. Os três são cultivos de um bloco, água obrigatória, sem foice/rebrota, máximo de uma colheita. Cenoura/beterraba entregam uma unidade madura; batata usa o handler especial abaixo. Plantio 3×3 com centro arado (divergência autorizada), prioridade da rega, seca inicial 1, morte sazonal e entrada seca >2 no integrado seguem o ciclo existente. Dedicado mantém morte por seca desabilitada por padrão. Sem compensação em chunks descarregados, chuva/aspersores ou substitutos aleatórios na morte.

### Batata e transação de colheita

`RegistryHelper.registerCrop("potato") → addHandlersToCrop → Class.forName(HFModInfo.DROPHANDLERS + "Potato")` associa efetivamente `crops/handlers/drop/DropHandlerPotato`; `setStages(Blocks.POTATOES,...)` só substitui o state handler. O laço original reavalia `rand.nextInt(20)==0 ? 2 : 1` em **cada** condição, inclusive a primeira e a última. Sequência de limites: 20/50/20 ou 20/50/20/50/20. Uma unidade é garantida se elegível, segunda com probabilidade 1/20; cada unidade é substituída por venenosa com 1/50, podendo resultar em duas venenosas. Não sortear quantidade uma vez, não adicionar veneno como bônus, não aplicar quantidade vanilla/Fortune. Maturidade é verificada antes de acessar loot; imatura/murcha não sorteia.

Entrada de loot registrada `harvestfestival:potato_harvest`, referenciada pelo `harvest_loot` existente. Produtos e divisores ficam no JSON (`normal`, `poisonous`, `extra_divisor=20`, `poison_divisor=50`); divisores inteiros positivos, codecs normais de condições/funções/peso preservados. `PotatoHarvestLoot.createItemStack` usa o random do contexto e o mesmo laço testado. Não introduz reflexão em produção: registro moderno substitui descoberta antiga, preservando o comportamento.

Novas definições usam `empty_harvest_policy=advance` e booleano estrito opcional `harvest_on_player_break=true`. Ausência mantém false e preserva o caminho anterior de nabo/cebola/espinafre. Clique de colheita e quebra removem antes de emitir; loot vazio remove, falha de remoção não emite. Quebra madura também entrega em criativo, como `BlockHFCrops.removedByPlayer`/patch Forge 1.12.x já conferido acima, independentemente de `willHarvest`. Snapshot invalidado impede segunda emissão pelo fluxo vanilla. Não há campo persistido novo; `CropState`, entidade e identidade diária permanecem compatíveis.

### Recursos, APIs e cobertura

Slots 0–24 preservados; cenoura 25–28, beterraba 29–32, batata 33–36. `StateHandlerBlock` associa estágio lógico aos metadados em ordem. Modelos vanilla **26.2** autorizados, sem alegar igualdade de pixels com 1.12.2: cenoura/batata agrupam idades 0–1/2–3/4–6/7; beterraba 0/1/2/3. Mapeamentos conferidos no client.jar 26.2. Sementes reutilizam os dois PNGs originais já empacotados: neck SHA256 `4361F05DAF8B89559AB239DA937A56B7404DDCE86077D75CACFE946EBB429597`, body `9DADEA780A0C25CEA0AFD50106C446E558BFDEC2C8549516A13EA2ECDAB14E2E`; hashes reconferidos contra referência MIT, cores nas duas camadas. Produtos/ícones/traduções vanilla reutilizados; nomes das sementes em pt_br/en_us. Destinos em [Abas](abas-criativas.md), preços/tags em [Remessas](remessas.md).

APIs conferidas nas fontes locais exatas `minecraft-patched-26.2.0.93-sources.jar`: `LootPoolSingletonContainer`/`singletonFields`, `LootPoolEntryContainer.codec`, `LootPoolEntries.CODEC`, `Item.CODEC` (Holder), `LootContext.getRandom`, `LootTable.DIRECT_CODEC`, `Registries.LOOT_POOL_ENTRY_TYPE` (registro de MapCodec). `DeferredRegister` do NeoForge registra o codec; compilação e decodificação pelo registro real verificam a associação. Reutilizados `getDrops`, `onDestroyedByPlayer`, `commitHarvest`, blockstate/entidade e sincronização já documentados; sem mixin/API cliente nova.

Testes novos cobrem três ciclos completos, estações/água/seca/resgate/dedicado, limites visuais, recursos/tintas/traduções, remoção antes do produto, falha/loot vazio/snapshot antigo/segunda passagem de drops, NBT comprimido de estados plantado/intermediário/maduro/morto e recarga inválida/restaurada. Testes determinísticos decodificam a tabela e a entrada de batata pelo codec registrado, executam o laço real com sequência controlada e comprovam os sorteios finais e substituições individuais. Teste econômico verifica preços separados 160/120/80/1, sementes sem preço, fila persistida e pagamento único de 361 após descartar a tabela. Regressões existentes e teste de plantio/abas ampliados.

Limites: adaptadores não simulam cliques de cliente, `ServerPlayerGameMode`, servidor real, renderização ou loot de mundo; decodificação da tabela e execução do laço não equivalem a sorteio em jogo. runClient/aceite manual da 15-A, cenário 8 e dedicado/multiplayer permanecem pendentes. Indicadores do regador já confirmados funcionais; memória de camas por dimensão adiada. 15-V continua separada: sem conversão automática de sementes, bloqueio global, receitas ou mudança de alimentação vanilla.

Validação automatizada da 15-A: build aprovado, 9 testes novos (8 de cultivos/loot/recursos e 1 de remessas), 160 totais efetivamente executados, zero falhas/erros/ignorados. `compileJava`/recursos reutilizados no build aprovado após compilação nesta implementação; `compileTestJava` e `test` executados. Não atribuir resultado em cache à execução dos testes.

Aceite posterior: responsável informou «Testes aprovados» e solicitou seguir à 15-V. Encerra o aceite global da 15-A, sem atribuir resultados individuais ou execução de cliente ao agente. Cenário 8, dedicado/multiplayer e memória de camas por dimensão permanecem pendentes; a política 15-V exige recorte aprovado antes da implementação.

## Fatia 15-V — política dos quatro cultivos presentes

Plano aprovado explicitamente após o aceite da 15-A. Escopo padrão: blocos vanilla wheat, carrots, beetroots e potatoes; itens wheat_seeds, beetroot_seeds, carrot e potato. Melão/abóbora aguardam a 18-A; nether wart e outras plantas não foram incluídos. Não conclui toda a política vanilla. Nenhum item/bloco/aba/textura, pacote ou mixin novo; não altera ferramentas, esforço, solo diário ou preços.

### Regras e fontes

Fonte original, leitura estática: `core/handlers/DisableHandler` (VanillaSeeds, VanillaGrowth, VanillaDrops, SeedDrops), `core/helpers/RegistryHelper.addSeeds/registerVanillaCrop`, `crops/HFCrops.configure/preInit/init` e `crops/handlers/SeedRecipeHandler`. A blacklist é compartilhada, mas a inscrição dos handlers depende de opções independentes. `CROPS` é preenchido quando growth ou drops estão ativos; isso não implica bloquear ambos. Não houve execução do original.

`data/harvestfestival/crop_settings/vanilla.json` contém quatro booleanos estritos, todos true por padrão:

| Campo | Efeito |
| --- | --- |
| disable_planting | Nega somente uso do item no bloco se estiver em harvestfestival:vanilla_crop_seeds; não remove/converte inventário nem cancela genericamente interação do bloco/uso no ar |
| disable_growth | Nega avanço aleatório de blocos em harvestfestival:vanilla_crops e uso de itens sobre eles; desde a ampliação autorizada de 16/11/2026, regadores em harvestfestival:watering_cans são exceção para hidratar o solo. Farinha de osso continua bloqueada |
| disable_drops | Esvazia loot de blocos em harvestfestival:vanilla_crops, inclusive produtos e sementes |
| disable_grass_seeds | Em harvestfestival:vanilla_seed_drop_sources, remove apenas itens em vanilla_crop_seeds, preservando demais produtos/plantas de tesoura |

Tags padrão são recursos separados, não uso genérico de c:seeds/c:crops. Fontes de grama: short_grass, fern, tall_grass, large_fern, sunflower, lilac, rose_bush e peony, correspondentes aos grupos TALLGRASS/DOUBLE_PLANT do original. Bloco HF e sementes HF ficam fora das tags restritivas. Tags podem ser substituídas/ampliadas por datapack; não há cache de membros nem listas de IDs em Java. Sem filtro de modo de jogo ou mão; restrições da interação aplicam-se nos dois lados com servidor autoritativo. Comer no ar, receitas, inventários e remessas não recebem bloqueio novo. Farinha de osso por dispensador/chamada externa não recebe bloqueio global; recorte preserva o caminho de interação comprovado.

Recurso de maior precedência fornece o arquivo inteiro. Ausente/inválido: publica política com quatro false e aviso; não conserva versão anterior nem usa fallback do pack inferior. Restauração + recarga reaplica o recurso. Recarregar não destrói plantas existentes nem altera inventários. Política do servidor é limpa ao encerrar; tags seguem recarga/sincronização vanilla.

### Adaptações aprovadas e sincronização

`CropGrowEvent.Pre` impede avanço, sem modificar o registro vanilla ou prometer a mesma sequência de consumo de aleatoriedade de setTickRandomly(false). `RightClickBlock.setUseItem(FALSE)` nega somente o caminho de uso do item, como o DENY original. [Patch Forge 1.12.x de PlayerInteractionManager](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.12.x/patches/minecraft/net/minecraft/server/management/PlayerInteractionManager.java.patch) conferido no levantamento.

Loot passa por `VanillaCropLootModifier` com guarda de BLOCK_STATE/tag. `BlockDropsEvent` sozinho não cobre `BlockBehaviour.onExplosionHit` no alvo; por isso o modificador cobre loot de quebra, perda de suporte, explosões e consultas que forneçam o estado correspondente. **Efeito sobre /loot aprovado explicitamente no plano**; não atribuir equivalência de comando ao original. Contextos sem estado e blocos fora das tags são preservados. Não altera experiência, cancela explosões ou elimina entidades de item já existentes. O [patch Forge 1.12.x de Block](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.12.x/patches/minecraft/net/minecraft/block/Block.java.patch) confirma filtragem anterior à emissão; ordem de modificadores externos não foi validada.

Attachment `harvestfestival:vanilla_crop_policy`: quatro booleanos, somente sincronização ao titular, sem serialização em save e sem copyOnDeath. `OnDatapackSyncEvent` atualiza os jogadores relevantes na entrada/recarga; `PlayerRespawnEvent` e `PlayerChangedDimensionEvent` republicam regras vigentes na entidade atual. No servidor, interação lê a regra autoritativa; no cliente, a cópia transitória do jogador. Não há cache estático cliente atravessando mundos, payload manual ou gravação de configuração no jogador. Estado inicial sem sincronização é aberto; servidor continua decidindo. Esse espelho técnico não comprova nem encerra cenário 8 (ouro/aniversário).

### Receitas

| Recurso em recipe/ | Ingrediente por tag | Resultado vanilla | Forma |
| --- | --- | --- | --- |
| wheat_seeds_from_hf.json | harvestfestival:seeds/wheat | 9 wheat_seeds | Sem formato |
| beetroot_seeds_from_hf.json | harvestfestival:seeds/beetroot | 9 beetroot_seeds | Sem formato |
| wheat_seeds_from_crop.json | c:crops/wheat | 1 wheat_seeds | 1×1 |
| beetroot_seeds_from_crop.json | c:crops/beetroot, duas posições | 2 beetroot_seeds | Sem formato |

Receitas independem das opções de bloqueio, como no original; não alteram outras receitas vanilla. Não há conversão vanilla→HF, conversão automática, saco de cenoura/batata→produto, receita de melão/abóbora ou obtenção por lojas. Produtos e sementes existentes mantêm destinos criativos aprovados.

### APIs e verificação

Fontes locais exatas Minecraft/NeoForge 26.2.0.93: `RightClickBlock`/`TriState`, `CropGrowEvent.Pre.Result.DO_NOT_GROW`, chamadas em CropBlock/BeetrootBlock; `LootModifier.codecStart` com condições/prioridade, `IGlobalLootModifier`/registro de MapCodec, `LootContext`/BLOCK_STATE/EXPLOSION_RADIUS, `LootModifierManager`; `SimplePreparableReloadListener`, `ResourceManager.getResource`; attachment sync sem serialize, `OnDatapackSyncEvent.getRelevantPlayers`, eventos de respawn/dimensão; `Recipe.CODEC`, `ShapedRecipe`, `ShapelessRecipe`, `CraftingInput`, `ItemStackTemplate`.

**Particularidade da versão:** `LootModifierManager` carrega diretamente instâncias em loot_modifiers/ e ordena por prioridade decrescente. Não usa índice global_loot_modifiers.json de versões anteriores, apesar de comentário antigo na interface. Instância empacotada: harvestfestival:vanilla_crop_policy, priority=1000 em JSON. Teste usa o carregador real, não apenas o codec isolado.

Testes automatizados: 16 combinações das opções, escopo/exclusões, não consumo no predicado, preservação de produtos de tesoura, evento de crescimento, precedência/invalidez/ausência/restauração das regras, substituição real de tags, codec de sincronização e attachment não persistido, atualização de titulares novos/anteriores; carregador real do modificador com contextos de bloco/explosão/sem estado/HF, alteração das regras após carga; quatro receitas decodificadas com matches/assemble, ingredientes errados/adicionais, quantidades e duas posições de beterraba.

Limites: contextos de loot são construídos em teste com o construtor vanilla via reflexão, exclusivamente em src/test, sem ServerLevel; listas de produtos são fornecidas ao modificador. Isso não simula destruição, explosão, água, dispensador ou servidor real. Titulares de attachment não são jogadores em sessão; codec/refresh não comprovam tráfego, morte ou reconexão. Cliques nas duas mãos/modos, alimentação e ausência de previsão visual incorreta precisam do roteiro manual. runClient não executado pelo agente. Aceite manual da 15-V, cenário 8 e dedicado/multiplayer pendentes; indicadores do regador já confirmados funcionais e memória de camas por dimensão adiada.

Validação automatizada da 15-V: 11 testes novos (7 de política/recarga/tags/attachment, 2 de modificador e 2 de receitas), 171 totais efetivamente executados, zero falhas/erros/ignorados. Build aprovado; `test` executou, enquanto produção/recursos estavam UP-TO-DATE após compilação nesta implementação. Regressões de cultivos HF, ferramentas, calendário e remessas passaram. Nenhum resultado de cliente ou servidor dedicado atribuído ao build.

Em 16/11/2026 o responsável confirmou que, após corrigir a localização do vanilla.json para dentro do datapack, plantio/crescimento/drops/sementes da grama funcionaram. Não atribui execução individual adicional ao agente. Solicitou ampliação da rega: qualquer bloco pode iniciar a área existente e regadores ficam isentos somente da negação de interação por disable_growth. A implementação inicial negava qualquer item, fiel ao handler original; a exceção agora é divergência autorizada para compatibilidade. Detalhes e limites em [Ferramentas](ferramentas.md#ampliação-autorizada-da-rega--16112026). Cenário 8 e dedicado/multiplayer preservados.
