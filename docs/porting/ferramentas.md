# Ferramentas e solo — contratos vigentes

Consultar para ferramentas, água, solo e cultivo (fatia 10). Pacote harvestfestival.tools aprovado em 09/11/2026.

### Comportamento e contrato consumido pelos cultivos

- `harvestfestival:hoe` e `harvestfestival:watering_can`: nível único, pilha máxima 1, sem durabilidade, melhorias, receita ou aquisição por lojas. Disponíveis por `/give`. Desde 12/11/2026 usam texturas básicas originais, substituindo enxada de madeira/balde vanilla; há barra e tooltip de água conforme complemento abaixo; não há HUD de ouro/aniversário.
- Enxada: clique em um único bloco da tag `harvestfestival:tillable`, com ar acima e face diferente de baixo, transforma em `minecraft:farmland[moisture=0]`. A tag inicial inclui terra, bloco de grama e caminho de terra. Terra grossa e remoção de plantas do original não integram o mínimo aprovado. Enxadas vanilla continuam funcionando e produzem o mesmo solo, sem bloqueio novo.
- Solo compartilhado: farmland vanilla, com forma de 15/16 de bloco e propriedade `moisture`. A política diária usa 7→3→0; uma atualização iniciada em zero retorna terra se o bloco acima não pertencer a `minecraft:maintains_farmland`. A tag é ampliada com `#harvestfestival:crops`, incluindo plantas murchas, sem substituir entradas vanilla. Ticks aleatórios não hidratam/secam solos participantes; chuva/clima HF continuam pendentes. Pisoteio usa o caminho de queda vanilla/NeoForge, descrito abaixo.
- Regador: novo item começa vazio. Mirar água reconhecida por `minecraft:water` (inclui fluido corrente) e usar repõe até 128 sem remover o fluido. Uma interação com água tem prioridade sobre regar e é consumida mesmo quando já está cheio, sem aumentar a reserva. Para regar, clicar no solo da tag waterable; o servidor grava `moisture=7` e consome 1 unidade somente após a escrita aceita. Repetir em 7 retorna UNCHANGED sem consumo. Alvo inválido, bloqueio, ambiguidade, reserva vazia ou falha de escrita não consomem. Criativo exige reserva positiva para mudar o solo, mas não a reduz.
- `ToolActions.apply(ToolAction, Target, ItemStack, boolean creative)` é o contrato de servidor para um alvo explícito. `Target` fornece estado, associação a tags (implementação padrão usa `BlockState.is`), espaço acima, autorização e escrita que retorna se houve alteração aceita. Resultados: `CHANGED`, `NO_MATCH`, `BLOCKED`, `EMPTY`, `UNCHANGED`, `AMBIGUOUS` e `FAILED`. O adaptador de interação verifica jogador, face, `mayInteract` e `mayUseItemAt` antes de chamar o contrato; o núcleo permite testar a lógica sem construir um mundo.
- O chamador deve fornecer um alvo autorizado e executar no servidor. `ToolActions.resolveTarget` resolve a posição abaixo do bloco clicado quando a ação é WATER e o bloco pertence à tag `harvestfestival:crops`; valida permissões no clique e no solo resolvido. A fatia 10 usa esse caminho sem outra reserva de água. `CropDailyUpdates` avalia plantas com umidade 7 antes da secagem diária 7→3→0, inclusive em solo vazio carregado. Salvar/reabrir preserva a umidade; chunks descarregados não são compensados. O componente do item acompanha o inventário vanilla; não há pacote de rede personalizado nem attachment de jogador para água. Detalhes em [Cultivos](cultivos.md).

### Dados recarregáveis

Diretório `data/harvestfestival/tool_actions/`, um recurso JSON por regra. Somente esse namespace é lido, como nos catálogos anteriores. Um datapack pode substituir os arquivos ou fornecer novas regras nesse diretório. Exemplo da enxada:

```json
{
  "action": "hoe",
  "target_tag": "harvestfestival:tillable",
  "requires_air_above": true,
  "result": {
    "block": "minecraft:farmland",
    "properties": {"moisture": "0"}
  }
}
```

Regra de rega:

```json
{
  "action": "water",
  "target_tag": "harvestfestival:waterable",
  "result": {
    "block": "minecraft:farmland",
    "properties": {"moisture": "7"}
  }
}
```

`action` aceita hoe/water; `target_tag` identifica uma tag de blocos. `requires_air_above` é booleano opcional (padrão false); mapas de propriedades são opcionais, com valores textuais. O estado resultante parte do estado padrão do bloco de saída e recebe as propriedades declaradas; não copia propriedades do alvo. As propriedades exigidas devem existir no esquema do bloco resultante; um alvo sem a propriedade ou com valor diferente não corresponde à regra. ID de bloco desconhecido, campo obrigatório ausente, tipo incorreto, propriedade/valor inválido ou JSON malformado descarta somente aquela regra com aviso identificando o recurso.

A recarga prepara uma lista nova e a publica inteira. O arquivo efetivo segue a precedência normal dos packs; um superior inválido não recupera a regra inferior. Tags ausentes/vazias não casam com alvos. Se duas regras casarem com ação, tag e propriedades do alvo, a ação é recusada com diagnóstico de ambiguidade, sem escolher pela ordem de leitura. Requisitos de espaço e mudança efetiva são checados depois dessa resolução. As tags de itens `harvestfestival:hoes` e `harvestfestival:watering_cans` identificam as ferramentas para futuros consumidores; a interação atual é fornecida pelas classes dos itens, e os alvos são selecionados por tags de blocos.

### Água no item

Componente registrado `harvestfestival:watering_can_state`, payload `{"water":128}`, inteiro entre 0 e 128. Ausência significa vazio. O valor é imutável; cópias do item não compartilham estado mutável. Construção por Java fora dos limites é rejeitada. Decodificação tolerante trata payload inválido como vazio: número fracionário/fora dos limites, string, booleano, null, lista ou campo ausente não geram água e não invalidam o item inteiro. Não há conversão silenciosa de string ou truncamento de fração. Encher limita a 128; consumo nunca produz valor negativo.

`DataComponentType` tem codec persistente e codec de rede derivado do mesmo formato. Testes percorrem os 129 valores possíveis; também gravam o ItemStack real em NBT comprimido, reabrem e confirmam identidade, quantidade, água e independência da cópia. Payload inválido dentro do codec real do ItemStack mantém o regador vazio.

### APIs conferidas e limites de validação

Fontes exatas locais de Minecraft/NeoForge 26.2.0.93 e compilação confirmaram: `Item.useOn/use`, `UseOnContext`, `getPlayerPOVHitResult` (alcance do jogador e `ClipContext.Fluid.ANY`), `FluidState.is(FluidTags.WATER)`, `Level.mayInteract/setBlock`, `Player.mayUseItemAt/isCreative`, registros diferidos de itens/blocos/componentes, `DataComponentType.Builder.persistent/networkSynchronized`, `ByteBufCodecs.fromCodec`, `ItemStack.CODEC`, `BooleanProperty`, definição/codec de estados, listener de recarga e `ResourceManager.listResources`. O caminho normal `ItemStack.useOn`/`CommonHooks.onPlaceItemIntoWorld` captura alterações de bloco e estado do item para o evento de colocação e eventual cancelamento; nenhum evento legado ou mixin foi adicionado. Essa leitura não constitui teste manual de cancelamento por terceiros.

Assets vanilla referenciados (enxada, balde, terra, farmland seco/molhado e modelo cube_bottom_top) conferidos no JAR local da versão alvo. Aceite posterior e limites de validação constam da seção final.

### Relato sobre o solo e resolução parcial

O responsável observou ausência de secagem e retorno à terra ao pisar na fatia 9. A fatia 10 resolveu a secagem booleana; a fatia 11 autoriza umidade diária, perda do preparo sem planta e pisoteio por queda conforme original. Caminhar não é cair. Contrato substituído preservado no [histórico do solo](../archive/fatia-11-solo-anterior.md).

### Indicadores de água implementados

Pedido adicional autorizado pelo responsável para facilitar os testes. O regador apresenta barra azul no ícone do inventário/hotbar e tooltip traduzido `Água: atual / 128` (`Water: current / 128`). A linha fica vermelha quando vazio e aqua quando possui água. A barra usa a faixa vanilla de 0 a 13 pixels; arredonda para cima para manter pelo menos um pixel enquanto existir uma unidade. O tooltip fornece a quantidade exata. Vazio mantém o fundo da barra, sem preenchimento azul; cheio ocupa os 13 pixels. Estado ausente ou inválido continua representando zero.

A apresentação lê exclusivamente WateringCanState do próprio ItemStack, já persistido e sincronizado pelo inventário. Não muda consumo, enchimento, codec, capacidade, regras por tags ou comportamento do solo. A apresentação não adiciona telas, rede ou armazenamento. Respeita ocultação do tooltip e ocultação do componente de água. O complemento de indicadores manteve a textura então existente; sua substituição separada em 12/11/2026 é descrita abaixo.

APIs verificadas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `Item.isBarVisible`, `getBarWidth`, `getBarColor`, `RegisterTooltipAppendersEvent.registerAppender`, `TooltipLocation.POST_CUSTOM`, `TooltipAppender`, `TooltipDisplay.hideTooltip/shows` e `Component.translatable`. Registro no barramento do mod em ToolContent. Foi usado o evento moderno de appenders em lugar do método depreciado Item.appendHoverText. Nenhum pacote ou mixin novo.

## Validação consolidada

Fatia 9: suíte total 57, 19 novos, execução aprovada; single player aceito em 09/11/2026 sem detalhar cada passo. Dedicado/multiplayer pendentes. Complemento visual: 57 testes existentes reexecutados, sem novos; validação visual pendente. Cenário 8 integra obrigatoriamente o aceite deste complemento e permanece não executado. Fatia 10: teste novo de resolução do alvo/regador integra os 24 novos/81 totais executados; secagem diária também testada. Nenhuma nova validação manual atribuída às ferramentas.

## Direção da Fase 3 e relação com vanilla

O responsável reafirmou em 12/11/2026 que o sistema agrícola completo, incluindo seus itens, deve ser entregue na Fase 3. As fatias 9/10 encerram seus ciclos aprovados, não o sistema inteiro. Escopo pequeno por fatia continua necessário; regras funcionais ausentes não ficam para polimento. Melhorias dependentes de lojas/NPCs seguem a ordem de DEPENDENCIES; integrações específicas permanecem adiadas.

Conferência dirigida da referência em 12/11/2026:

- `tools/item/ItemHoe.java` prepara `Blocks.FARMLAND`, o mesmo bloco vanilla. `api/crops/WateringHandler` reconhece `BlockFarmland` e lê/escreve `MOISTURE`; `WateringTickHandler` acompanha preparo e carga do solo.
- `HFCrops.configure`: `Disable vanilla hoes` tem padrão false. `DisableHandler.VanillaHoes` só registra/aplica cancelamento de `UseHoeEvent` com a opção habilitada. Portanto não presumir que toda enxada vanilla deve ser bloqueada para reproduzir o original. Sementes/crescimento/drops vanilla têm outras opções, várias true por padrão; são contratos distintos.
- A fatia 11 substitui a separação por farmland vanilla com umidade diária, após autorização explícita. Compartilhar bloco melhora a base de compatibilidade, mas não valida integrações específicas; mods que dependam de hidratação aleatória ainda podem exigir adaptação futura.
- `harvestfestival:prepared_soil` permanece registrado apenas para migração. Na carga, o chunk é enfileirado; em ServerTickEvent.Pre, após getChunkNow confirmar FULL, converte false→0 e true→7 com setBlock/UPDATE_ALL. A virada também verifica migração pendente antes de crescer. Não força carga, não altera CropBlockEntity nem o marcador diário, não avança dias; o estado convertido é salvo pelo chunk. Recarregar repete a varredura de forma idempotente. Blocos antigos criados administrativamente depois dessa varredura só migram ao recarregar o chunk.
- Tags crop_soils incluem farmland e o registro legado para sustentar plantas durante a conversão. waterable contém somente farmland. Datapacks antigos que substituam regras/tags precisam atualizar os resultados para moisture; não há reescrita silenciosa dos packs. Propriedades/valores desconhecidos continuam rejeitados pelo carregador existente.

## Umidade, queda e APIs da fatia 11

- `FarmlandSoil.managed/watered/dry/migrate` centraliza reconhecimento por tag, rega exclusivamente em 7, secagem e conversão. Umidades 1/2/4/5/6 são preservadas pela atualização diária, como no WateringHandler original; regador pode levá-las a 7. Estado vanilla inválido segue o codec vanilla, sem armazenamento duplicado.
- Original HFCrops desabilita ticks aleatórios de farmland com `Disable vanilla moisture` true por padrão. Na versão 26.2.0.93, FarmlandBlock.randomTick não publica evento cancelável; o mixin cancela exclusivamente esse método para solos na tag crop_soils com MOISTURE. Não altera fallOn, tick de obstrução, shape ou registros vanilla. Não há substituição do bloco nem reflexão/access transformer.
- Fontes locais exatas: `minecraft-patched-26.2.0.93-sources.jar`: FarmlandBlock.MOISTURE/randomTick/fallOn/turnToDirt, Entity.canTrample, BlockTags.MAINTAINS_FARMLAND; `neoforge-26.2.0.93-sources.jar`: CommonHooks.onFarmlandTrample, BlockEvent.FarmlandTrampleEvent. Migração usa APIs de chunks já documentadas em Cultivos e ServerTickEvent.Pre existente.
- Conferência do Forge 1.12.x: [patch BlockFarmland](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/patches/minecraft/net/minecraft/block/BlockFarmland.java.patch), [Entity.canTrample](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/patches/minecraft/net/minecraft/entity/Entity.java.patch) e [ForgeHooks](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/common/ForgeHooks.java). Condições equivalentes na versão alvo: sorteio menor que distância de queda menos 0,5, entidade viva, jogador ou permissão de griefing e largura²×altura maior que 0,512. Evento pode cancelar. Nenhum handler próprio duplica essa lógica; leitura da referência HF não encontrou substituição dessas regras.

### Validação da farmland compartilhada

Build aprovado em 12/11/2026: 6 testes novos, 87 totais executados, zero falhas/erros/ignorados. Os novos testes cobrem sequência até terra, preservação por tag incluindo cultivo murcho, umidades intermediárias, migração idempotente/codec dos dois estados antigos e rega em todos os valores com consumo único. O sexto chama BlockState.randomTick com mundo nulo nos estados 0/3/7: passar comprova que o mixin aplicado interceptou o método antes do acesso vanilla ao mundo. Não simula chuva/água num mundo real.

Testes existentes de plantio, resolução do regador, ordem diária, repetição, chunks ausentes e persistência passaram a usar farmland; regressões de colheita/remessa continuam aprovadas. Testes de regras genéricas ainda usam o registro legado em fixtures para verificar parsing de propriedades booleanas; as regras empacotadas são testadas com farmland real. A migração automatizada verifica conversão/codec, não a carga de um save completo nem notificações entre blocos num mundo.

runClient não foi executado pelo agente nesta alteração. Em 12/11/2026 o responsável informou «Testado e aprovado»: aceite global da fatia 11 em single player, sem detalhamento individual de migração, interação, queda ou salvar/reabrir. Não atribuir execução separada desses cenários nem testes de entidades ao agente. Dedicado/multiplayer, cenário 8 e indicadores continuam pendentes; texturas tiveram confirmação separada abaixo.

## Texturas básicas originais

A pedido do responsável, os itens atuais usam cópias byte a byte de `textures/items/tools/hoe_basic.png` e `wateringcan_basic.png` da referência. Destino moderno: `assets/harvestfestival/textures/item/tools/`. Os modelos mantêm os parents equivalentes aos originais: `minecraft:item/handheld` para enxada e `minecraft:item/generated` para regador. As definições modernas de item permanecem existentes; nenhum Java, nível, carga ou componente mudou.

Proveniência: Snownee/Harvest-Festival, branch 1.12.2-0.6.X; licença MIT e aviso Copyright (c) 2014, 2015, 2016 joshiejack preservados no recurso empacotado `META-INF/licenses/harvest-festival-original.txt`, copiado da referência. Referência permanece somente leitura. Inspeção das duas imagens e conferência de hashes realizadas; revisão artística pode ocorrer no polimento.

Em 12/11/2026 o responsável confirmou que os visuais da enxada e do regador estão como no original, encerrando a pendência das texturas. Confirmação do responsável, sem atribuir inspeção de perspectivas específicas nem execução ao agente. Cenário 8 e aceite visual da barra/tooltip continuam pendentes. Registro documental sem testes novos ou mudanças de gameplay.
