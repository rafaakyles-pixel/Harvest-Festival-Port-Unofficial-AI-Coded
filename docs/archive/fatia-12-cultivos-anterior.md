# Contrato de cultivos anterior à fatia 12

Preservação integral do contrato das fatias 10/11 antes da atualização de 13/11/2026. O contrato vigente está em [Cultivos](../porting/cultivos.md). A fatia 12 foi autorizada com texturas originais obrigatórias para todos os itens novos; não há alteração de AGENTS.md.

---

# Cultivos — contrato vigente

Fatia 10, 11/11/2026. Pacote `harvestfestival.crops` aprovado em produção e testes. Dependências: etapas 5/6 de `DEPENDENCIES.md`, [Ferramentas](ferramentas.md), [Calendário](calendario.md) e [Remessas](remessas.md).

## Decisões e comportamento

- O responsável aprovou o plano com a correção arquitetural de seguir o original: um bloco compartilhado `harvestfestival:crop`, com `CropBlockEntity`, em vez de `turnip_crop` por espécie. O estágio lógico não é propriedade de blockstate.
- Primeiro cultivo: nabo comum `harvestfestival:turnip`. Saco `harvestfestival:turnip_seeds`, produto `harvestfestival:turnip`; aquisição por comando, sem lojas, receitas, alimentação ou culinária.
- Clique superior com semente tenta um quadrado 3×3 centrado acima do solo clicado. Cada posição exige tag de solo, dois espaços de ar e permissões no solo/planta/acima. Sucesso parcial consome um saco; nenhum sucesso não consome. O fluxo vanilla conserva quantidade em criativo.
- Nasce no estágio 1 e contador de seca 1. Em primavera, rega permite avançar uma unidade por evento: 1→2→3→4→5. São quatro viradas regadas. Estágios visuais originais agrupados por limites 2/4/5.
- Não há verificação de estação no plantio. Na virada, fora da estação, morre mesmo com água. Sem água não cresce. Contador hidratado é zero; após atualização sobrevivida é incrementado. Morte por seca ocorre ao entrar no ciclo com contador maior que 2, habilitada no integrado e desabilitada no dedicado por dados, conforme padrões originais. Contador satura em limite+1, sem overflow após inatividade longa.
- Rega no solo ou na planta utiliza o contrato existente `ToolActions.apply`; tag de cultivos resolve o solo imediatamente abaixo. Permissões são verificadas tanto no alvo clicado quanto no solo resolvido. Regador tem prioridade sobre colheita e mantém enchimento/capacidade/consumo originais da fatia 9.
- Colheita madura por clique sem agachar remove a planta antes de emitir o produto; falha de remoção ou loot vazio não gera item nem remove a planta. Quebra madura usa a mesma resolução de loot; imatura/murcha não fornece produto nem semente. Solo permanece preparado. Sem rebrota ou foice.
- Produto tem preço 60 via `c:crops/turnip`, reutilizando `ShippingPrices` e `PlayerShipping` sem alterar sua lógica. Depósito fixa preço; pagamento ocorre na próxima virada conectado.

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
  "requires_water": true,
  "drought_limit": 2,
  "dies_without_water_integrated": true,
  "dies_without_water_dedicated": false,
  "planting_radius": 1,
  "harvest_loot": "harvestfestival:crops/turnip"
}
```

- Todos os campos mostrados são obrigatórios. Estações válidas: spring/summer/autumn/winter, lista não vazia. `stages` contém limites visuais estritamente crescentes entre 1 e 1.000.000; último valor é maturidade. Não está limitado a cinco. Limite de seca: 0..999.999. Raio: 0 ou 1. Booleanos não aceitam strings; inteiros não aceitam frações nem strings. Campos adicionais não têm efeito.
- Sementes são associadas por tags, não por ID de espécie codificado na classe Java. Tags ausentes/vazias não correspondem. Duas definições que casem com a mesma semente recusam plantio com aviso, sem escolha dependente da ordem.
- Item precisa oferecer a interação `CropSeedItem` para plantar. Esta fatia registra somente a semente/produto do nabo; não cria sementes com componentes, registros dinâmicos de itens ou interação para sementes vanilla por datapack.
- JSON malformado, campos/limites inválidos ou recurso de loot inexistente descartam apenas a definição efetiva, com aviso de identidade/motivo. Pack superior inválido não recupera regra inferior; recarga substitui integralmente a tabela. IDs/tags não criam conteúdo novo por si mesmos.
- A validação de estrutura/referências internas de loot pertence ao carregador vanilla. Loot ausente da tabela carregada ou sem drops não entrega produto; clique mantém a planta. Erros de loot do carregador são distintos dos avisos do catálogo de cultivos.
- Sem definição válida, plantas existentes ficam congeladas e não colhem; solo ainda seca. Restaurar definição retoma da situação salva, sem compensação. Alterar maturidade não converte/zera estágios existentes; a próxima operação usa a definição atual.
- `data/harvestfestival/loot_table/crops/turnip.json` fornece um nabo. O loot vazio do bloco genérico evita drops padrão sem definição. Preço separado em `shipping_prices/tags/c/crops/turnip.json`, formato existente `{"price":60,"priority":0}`. Tags comuns `c:crops`, `c:crops/turnip` e `c:seeds`; tags de plantio/solo/cultivos no namespace HF.

## Persistência e apresentação

- `CropBlockEntity` salva `crop_state`: `{"crop":"harvestfestival:turnip","stage":1,"dry_days":1,"dead":false}`. Codec exige estágio 1..1.000.000 e seca 0..1.000.000 integrais. Ausência/erro de payload deixa identidade sentinela murcha, inerte, sem produto; falhas de codec seguem diagnóstico de leitura vanilla.
- O blockstate guarda apenas `appearance=0..3`: pequena, intermediária, madura e murcha. É cache de aparência provisória calculado da entidade/definição pelo servidor; não limita nem determina crescimento. Várias definições compartilham essas formas/texturas provisórias. Renderização específica por espécie fica para conteúdo posterior.
- Estado do bloco e entidade são salvos/sincronizados pelo chunk. `getUpdateTag` fornece dados para entrada no chunk; `ClientboundBlockEntityDataPacket` para atualizações. Sem payload próprio, ticker de crescimento, renderizador personalizado ou consulta do catálogo de servidor pelo cliente.
- Modelos referenciam texturas vanilla conferidas no JAR alvo: trigo inicial/intermediário, beterraba madura, arbusto morto, sementes/beterraba para itens. Nomes em en_us e pt_br. Em 12/11/2026 o responsável relatou o visual de arbusto seco e aprovou o teste final; não atribuir inspeção individual dos demais modelos.

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
- Núcleo público: `CropLifecycle.newDay/canHarvest`, `CropSeedItem.plant(Plot,...)`, `CropBlock.harvestOnce`, `CropDailyUpdates.process(ChunkWork,...)`, `CropDefinitions.get/forSeed`. Adaptação de mundo usa essas mesmas rotinas.

## Validação e limites

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
