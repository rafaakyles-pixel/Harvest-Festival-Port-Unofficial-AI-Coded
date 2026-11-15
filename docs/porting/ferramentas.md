# Ferramentas e solo — contratos vigentes

Consultar para ferramentas, água, solo e cultivo (fatia 10). Pacote harvestfestival.tools aprovado em 09/11/2026.

## Contrato atual — 15-F: enxada e regador até prata

Plano aprovado pelo responsável em 15/11/2026, somente 15-F; 15-A e 15-V aguardam aprovação própria. Pacotes existentes, sem mixin novo. A base histórica abaixo registra os mínimos anteriores; esta seção substitui seus limites de nível único/ação unitária e ausência de durabilidade da enxada.

- IDs básicos `hoe`/`watering_can` preservados; novos `hoe_copper`, `hoe_silver`, `watering_can_copper`, `watering_can_silver`. Somente `/give` e aba Ferramentas e blocos; sem receitas, lojas, ferreiro, reparo ou promoção automática. Pilhas de uma unidade, regadores vazios, ferramentas novas com nível/carga zero.
- Enxada: básico 1×1; cobre até 1×2; prata até 1×3. Carga persistente 0/1/2, incremento por 20 ticks completos enquanto usa (duração 32000), somado à carga anterior e limitado pelo tier. Soltar aplica mesmo antes de um incremento; carga não é gasta. No máximo, próximo acionamento aplica imediatamente. Agachar ao iniciar cobre/prata apenas zera carga; básico continua aplicando. Agachar depois de iniciar não reduz a área na liberação. Nível não desbloqueia carga.
- Regador sem carga: básico 1×1, cobre 3×1, prata 3×3; agachar não reduz área, capacidade sempre 128. Geometria usa alvo como início da profundidade, não centro do quadrado. Quatro orientações horizontais: sul +Z, norte −Z, leste +X, oeste −X. Enxada percorre X/Z; regador Y/X/Z em alvo.y−1 e alvo.y, resolvendo cultivo HF para solo abaixo. Pode atingir solos em dois planos; repetição do mesmo solo já molhado não cobra de novo.
- Preservado o domínio aprovado: enxada em terra/grama/caminho por tags e ar acima; regador começa em `waterable` ou cultivo HF e aplica regras existentes. Cada candidato/solo exige permissão; não força chunks ausentes. Terra grossa, remoção de IPlantable, plantas duplas, reconhecimento geral de plantas externas, mineração com enxada, prévia gráfica e reparos permanecem fora do recorte. O original contempla vários desses caminhos; a limitação foi aprovada expressamente.
- `ToolArea.positions/perform` seleciona e executa em ordem; `ToolActions.apply` continua sendo a ação de um alvo. Apenas CHANGED chama `FarmToolTasks.complete`: aumenta nível até 100 por 0,39215682745098/0,196078431372549/0,130718954248366 (básico/cobre/prata). Taxa usa tier físico mesmo com carga baixa. Com consumo ligado e fora do criativo: esforço 4/actions_per_half_haunch × 3/2,5/2; enxada recebe um dano sujeito a encantamento. Regador nunca recebe dano.
- Enxadas têm durabilidade 128/256/768; em dano máximo permanecem no inventário, com nome de quebrada e sem ação/carga. Não usar `hurtAndBreak` moderno. Guarda é reavaliada a cada posição; água parcial e último ponto de dano respeitam a ordem. Criativo/consume_hunger=false mantêm nível, sem esforço/dano; água continua independente e criativo exige reserva positiva, mas não a reduz. Regra effort inválida desliga custo/dano, conservando alteração/água e nível; não restaura regras inferiores.
- Esforço usa `ToolEffort.consume`, preservando efeitos/desmaio/sono da fatia 14; não interrompe laço especificamente pelo desmaio (original só reavalia usabilidade/água). Não há custo por iniciar carga, descarregar, encher, repetir rega ou falhar. Ganho de nível e custos ocorrem exclusivamente no servidor.
- Adaptações já aprovadas mantidas: custo somente após escrita aceita, permissões por alvo, regador começa vazio e enchimento tem prioridade mesmo cheio. Enxada publica `BlockToolModificationEvent` com resultado proposto pela regra/tag; cancelamento/null/estado inalterado impedem custo. Não herda automaticamente terra enraizada ou outras transformações da enxada vanilla moderna. Integrações específicas continuam adiadas.

### Dados, recursos e evidências da 15-F

- Componente `harvestfestival:farm_tool_state`: `{"level":42.5,"charge":2,"invalid":false}`. Ausente é novo, sem tocar água/dano existentes. Nível finito 0..100 e carga integral 0..2; payload inválido torna o item inerte, sem reset para novo. Carga acima do tier físico também impede ação. Cópias imutáveis, codec persistente e de rede. Regador usa carga zero normalmente; não a utiliza para área.
- `data/harvestfestival/tool_settings/tiers.json` é **definição empacotada dos itens**, lida do classpath na inicialização por ambos os lados, antes do registro; não é configuração recarregável por datapack. Define dano, incremento de nível, esforço e dimensões de cada tier. Alteração requer reconstruir/reiniciar; recurso ausente/inválido impede carregar. Regras de alvo e effort continuam recarregáveis pelos listeners existentes. Sem novo catálogo cliente ou rede própria.
- Fontes originais lidas: `core/base/item/ItemTool`, `ItemToolChargeable`, `tools/item/ItemHoe`, `ItemWateringCan`, `tools/ToolHelper`, `HFTools`, `api/core/ITiered`, `core/helpers/EntityHelper`, `crops/CropRegistry` e `api/crops/WateringHandler`. `getBlocks` da enxada é prévia e exclui o centro em tiers superiores; execução real está em `onFinishedCharging` e inclui o centro. `ItemWateringCan` não herda carga. Evidência estática, sem sessão do original.
- [Patch oficial ItemStack Forge 1.12.x](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.12.x/patches/minecraft/net/minecraft/item/ItemStack.java.patch): destruição somente acima do máximo; guarda HF impede a tarefa seguinte ao atingir o máximo. O port preserva item esgotado, como já contratado para foice.
- APIs adicionais verificadas nas fontes locais exatas Minecraft/NeoForge 26.2.0.93 e compilação: `Item.getUseDuration/getUseAnimation/onUseTick/releaseUsing`, `LivingEntity.startUsingItem/releaseUsingItem/getUsedItemHand`, `Player.sendOverlayMessage`, `Direction.fromYRot`, `ItemInstance/canPerformAction`, `ItemAbilities.HOE_TILL`, `BlockToolModificationEvent.setFinalState/getFinalState/isCanceled`, `EnchantmentHelper.processDurabilityChange`. Componentes, tooltip, registro e persistência reutilizam APIs já descritas abaixo. `Level.hasChunkAt` é depreciado no alvo, usado como guarda de disponibilidade sem carregar chunks.
- Quatro PNGs copiados byte a byte da referência: `hoe_copper`, `hoe_silver`, `wateringcan_copper`, `wateringcan_silver`, de `textures/items/tools` para `textures/item/tools`. SHA256 iguais e inspeção dos arquivos realizadas. Parents originais handheld/generated, traduções en_us/pt_br; licença MIT existente preservada. Tooltips de água estendidos aos tiers, área do regador e carga/instruções da enxada; mensagens de carga/descarga pelo servidor. Não constitui validação visual em jogo.
- Validação: 12 testes novos (7 de estado/tarefas e 5 de área), 151 totais executados, sem falhas/erros/ignorados. Cobrem fronteiras de carga, decisões de acionamento, quatro direções/ordem/planos, repetições/água parcial, custos/dano/nível, criativo/configuração/encantamento, estado inválido, persistência NBT comprimido e cópias. Testes existentes de abas/fixtures atualizados; regressões de cultivos, foice, esforço, solo e remessas executadas. Testes usam núcleo compartilhado, registros, tags e recursos reais; não simulam entrada de mouse, ServerPlayer completo, cancelamentos externos, tráfego ou mundo salvo completo. runClient/manual não executados pelo agente; aceite manual pendente.
- Cenário 8 e dedicado/multiplayer permanecem pendentes. Indicadores anteriores do regador já confirmados funcionais; novos tiers ainda requerem conferência manual. Abas não encerram espelho de ouro/aniversário. Memória de camas por dimensão permanece adiada.

### Base histórica — comportamento das fatias 9–14

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

## Fatia 13 — foice básica

Plano aprovado em 14/11/2026. harvestfestival:sickle atua em um cultivo HF por quebra, sem carga/área adicional. Dano 0–128, retém item em 128 e velocidade 0,05 quando esgotada. Continua reconhecida pela tag sickles, inclusive esgotada. Não confundir reconhecimento com canUse. Atributos de ataque +5 e velocidade -3.

Fontes originais: ItemSickle, ItemTool, ItemToolChargeable, ITiered, ToolHelper e HFTools; patches oficiais Forge 1.12.x de PlayerInteractionManager e ItemStack consultados no levantamento. Callback ocorre antes de remover; cada tarefa utilizável aumenta nível em 0,39215682745098 até 100. Survival consome um dano e exaustão 4/27×3. Criativo não percorre o callback normal. Opção consume_hunger=false também desabilita dano, mantendo nível. Não houve teste no original.

data/harvestfestival/tool_settings/sickle.json contém consume_hunger booleano e actions_per_half_haunch inteiro positivo. Recarga inválida/ausente desabilita tarefas de custo/nível, com aviso, sem fallback; reconhecimento para colheita permanece por tag. Componente sickle_state persiste nível; ausente significa novo, inválido fica inerte. Dano usa componentes vanilla. Cópias não compartilham estado mutável.

APIs verificadas nas fontes locais 26.2.0.93: Item.mineBlock/getName/getDestroySpeed/use, ItemAttributeModifiers, EnchantmentHelper.processDurabilityChange e ItemStack.setDamageValue. Não usar hurtAndBreak: removeria ao atingir o máximo moderno. Adapter conserva ferramenta em 128. Dano respeita modificadores de encantamento. Sem novo mixin.

Recorte explicitamente aprovado: atuação agrícola em cultivos HF; demais vegetais, reparo e progressão permanecem pendentes. Fadiga, efeitos de exaustão/desmaio e recuperação são pendências da próxima fatia, solicitadas pelo responsável; consumo básico de fome permanece. Não generalizar esta entrega como ferramenta completa.

Textura sickle_basic.png copiada byte a byte da referência (SHA256 492539E8EF4271D07F67ADA26245F6906E0EB76288405E80B71F35A6528E3367), modelo com handheld_rotated original. Licença MIT já empacotada preservada. Conferência de arquivos não comprova aparência no cliente.

Validação automatizada da fatia 13: build aprovado; 14 testes novos, 119 totais efetivamente executados, zero falhas/erros/ignorados. runClient não executado nesta entrega; aceite visual e manual pendente. Testes usam adaptadores, registros e serialização reais, sem simular cliente ou servidor dedicado.

Em 14/11/2026 o responsável declarou a fatia 13 completa. Aceite global, sem atribuir execução individual de cenários ou testes ao agente. Cenário 8, indicadores do regador e dedicado/multiplayer permanecem pendentes; fadiga, desmaio e recuperação seguem para a fatia 14.

## Fatia 14 — esforço, efeitos, desmaio e recuperação

Plano aprovado em 14/11/2026; implementação/validação em 15/11/2026. Fontes originais: tools/ToolHelper, ToolEvents, HFTools, HFPotion, core/handlers/SleepHandler, HFCore, ItemTool, ItemHoe, ItemWateringCan, ItemSickle, EntityHelper e CalendarAPI/CalendarHelper. Evidência por leitura, sem execução do original.

- ToolEffort.consume é a entrada de servidor. Núcleo Target/transition é compartilhado com os testes: lê comida antes do custo; 3–6 aplica fadiga 6000 ticks removendo exaustão; ≤2 sem exaustão aplica 2000 ticks removendo fadiga. Só o ramo posterior desmaia: comida zero/duração ≤1990, ou early_fainting/duração ≤1500. Não desmaia ao criar efeito nem automaticamente ao esperar. O ramo 3–6 tem precedência sobre o desmaio precoce.
- ToolEffects registra efeitos nocivos próprios: fadiga multiplica movimento por ~0,9; exaustão multiplica movimento e ataque por ~0,5. Não são efeitos vanilla de mineração/fome. Cegueira aleatória 1/256 em todo Post e também Pre quando gameTime%20=0, 300 ticks/amplificador 0/ambiente/sem partículas. Autoridade exclusivamente servidor aprovada no plano, sem sorteio duplicado cliente.
- Desmaio aplica cegueira 100 ticks/amplificador 7; depois teleporta/tenta dormir ou usa a alternativa letal explicitamente ligada. Sempre limpa efeitos HF e, se habilitado, restaura comida 20/saturação 5/exaustão alimentar 0/temporizador 0, inclusive com teleporte desligado/sono recusado. Não cura vida, não desconta ouro nem remove inventário/XP no padrão.
- Morte optativa usa DamageType exhausted, dano 1000, scaling never, exhaustion 0 e tags bypasses_armor/resistance/enchantments. Mantém imunidade criativa, cooldown, totem e eventos do motor; não é /kill nem morte garantida. Término de uso de qualquer item remove exaustão se comida >2 e fadiga se >6; não cria fadiga ao remover exaustão. Leite remove efeitos; expiração e persistência usam motor. A fusão de efeitos aplicada pelo HF reproduz PotionEffect.combine antigo, sem guardar efeito mais fraco oculto para reaparecer depois.
- Foice conserva seu domínio/callback, nível, dano e reconhecimento anteriores; custo existente passa por ToolEffort. Criativo, consume_hunger=false, ferramenta esgotada e regra da foice inválida conservam guardas anteriores. Enxada/regador cobram 4/27×3 somente após CHANGED na ação existente, uma vez ao resolver planta→solo. Encher/repetir rega/alvo inválido/água vazia não cobram. Água permanece independente do consumo de fome; enxada continua sem durabilidade/nível nesse recorte, regador original não tem dano. Plantio e colheita à mão não ganham custo.

### Dados e política de recarga

`data/harvestfestival/tool_settings/effort.json`: consume_hunger=true, actions_per_half_haunch=27, restore_hunger_on_sleep=true, restore_hunger_on_fainting=true, enable_fainting=true, early_fainting=false, death_fainting=false, instant_fainting_sleep=true, attack_fainting=false, block_fainting=false, sleep_anytime=true, sleep_only_at_night=false. Booleanos estritos/divisor inteiro positivo; recurso vencedor ausente/inválido desliga as regras novas com aviso, sem fallback. Restaurar e recarregar retoma. Registro dos listeners é estável; consultam regra atual, equivalente técnico moderno com recarga aprovada.

SickleRules continua sendo a única fonte de consume_hunger/divisor para a foice; os dois campos de effort valem para enxada/regador. Regra comum inválida não elimina o custo/nível/dano já contratado da foice. Os listeners opcionais de ataque e drops chamam consume com custo zero; não herdam guarda de criativo/consume_hunger de performTask. Morpheus, maldição, carga, reparo e outros vegetais continuam fora do recorte.

Efeitos, comida e contador de sono usam armazenamento do motor; não há stamina/attachment paralelo. Ícones são os recortes originais 18×18 em (0,198) e (18,198) de textures/gui/potions.png, em textures/mob_effect/{fatigue,exhaustion}.png; sem redesenho. Traduções en_us/pt_br incluídas, licença original preservada.

### Sono e destino

- CanPlayerSleepEvent adapta os checks de SleepHandler: jogador vivo/acordado, dimensão de superfície, distância X/Z≤3,Y≤2, nenhum Monster na caixa ±8/±5/±8, inclusive criativo. sleep_anytime permite tentativa diurna; restrição opcional usa (ticks+6000)%24000 e intervalo inclusivo 6000..sunset, sunsets 20500/21500/19000/16500 por estação. CanContinueSleepingEvent permite continuidade diurna somente com cama ainda presente; não mantém cama destruída.
- O original inicia tentativa mesmo sem cama, mas EntityPlayer.isInBed no tick pode acordá-lo. Port mantém tentativa e check de existência posterior; não promete sono durável ou avanço de dia sem cama. Falta de monstros/cama não é inferida do resultado do teleporte. Calendário/sono coletivo continuam decididos pelo motor; temporizador instantâneo não força os demais jogadores a dormir.
- Tags de dimension_type surface_sleep/fainting_respawn contêm minecraft:overworld por padrão: mapeamento das capacidades dos providers vanilla 1.12.2; dimensões de terceiros exigem dados/validação próprios. Desmaio permanece na dimensão habilitada para respawn, senão retorna Overworld. Usa coordenadas armazenadas somente se RespawnConfig pertence à dimensão destino; sem ponto correspondente, aplica o fallback aleatório legado. Memória de camas por dimensão foi explicitamente mantida pendente pelo responsável: não implementado attachment; um respawn moderno em outra dimensão pode ter substituído o anterior.
- Fallback usa spawn global, radius limitado pela borda, offset metade−nextInt(max(2,radius)), sem aleatorização em mundo Adventure/sem céu de provider equivalente. A posição desce até bloco que bloqueia movimento, ignorando folhas, em vez de aceitar superfície de água como chão. Mantém risco do destino original, sem busca de local seguro por respawn moderno. Mundo/mods com providers próprios não foram validados.
- SleepFinishedTimeEvent usa ClockAdjustment.Absolute para próxima fronteira de 24000 do Overworld; respeita decisão coletiva/ADVANCE_TIME do motor, sem emitir NewDayEvent extra. PlayerWakeUpEvent só limpa/recupera se relógio%24000==0 e opção habilitada. Cancelar sono antecipadamente não restaura por si só. LivingIncomingDamageEvent cancela apenas STARVE enquanto dormindo quando o handler de sono está habilitado.
- **Accessor autorizado explicitamente pelo responsável:** tools.mixin.PlayerSleepAccessor expõe somente escrita de Player.sleepCounter; chamado somente no desmaio com instant_fainting_sleep. Fonte exata não oferece setter/evento para marcar sono profundo imediato. Sem mudança no algoritmo de decisão coletiva, sem substituir desmaio por morte.

### Fontes herdadas, APIs e validação

Além da referência HF, consultados os patches oficiais Forge 1.12.x de EntityPlayer/WorldProvider/WorldServer e código mapeado 1.12.2 de FoodStats, EntityPlayer, PotionEffect, ItemBucketMilk e World: [patches Forge](https://github.com/MinecraftForge/MinecraftForge/tree/1.12.x/patches/minecraft/net/minecraft), [fontes mapeadas](https://github.com/KealJones/mc-1.12.2-source_files/tree/master/src/minecraft/net/minecraft). Leitura, não observação em jogo. O fallback herdado e o despertar por cama ausente corrigem a incerteza do levantamento inicial.

Fontes locais exatas minecraft-patched/neoforge 26.2.0.93 e compilação: MobEffect/addAttributeModifier, MobEffectInstance, FoodData/readAdditionalSaveData, TagValueInput, CanPlayerSleepEvent, CanContinueSleepingEvent, PlayerWakeUpEvent, SleepFinishedTimeEvent/ClockAdjustment, PlayerTickEvent.Pre/Post, LivingEntityUseItemEvent.Finish, AttackEntityEvent, BlockDropsEvent, LivingIncomingDamageEvent (antes de acordar no dano), ServerPlayer.teleportTo/startSleepInBed/getRespawnConfig, Level.dimensionTypeRegistration, Heightmap, WorldBorder e GameRules.RESPAWN_RADIUS. blocksMotion é depreciado no alvo, usado de forma restrita para reproduzir o predicado herdado do fallback, não para lógica geral de colisão.

15 testes novos de esforço/configuração/sono: limiares e precedência, custos/efeitos/ordem de teleporte e recuperação via adaptador, desmaio desligado/alternativa letal/configuração inválida, restauração dos quatro campos, merge sem efeito oculto, codec de efeitos, atributos, padrões/tipos/recarga de recursos reais, horários sazonais/offset de spawn e aplicação do accessor à classe Player transformada. Com cinco testes de cultivos/remessas, são 20 novos/139 totais executados e aprovados. Não simulam ServerPlayer completo, teleporte, cama, cancelamentos externos ou sessão multiplayer; teste de accessor comprova aplicação da interface, não sono real. runClient/manual pendentes nesta entrega. Cenário 8 e dedicado/multiplayer continuam pendentes; indicadores do regador já confirmados funcionais pelo responsável.

Aceite global da fatia 14 pelo responsável em 15/11/2026, sem inferir cenários individuais. Memória de camas por dimensão, cenário 8 e dedicado/multiplayer permanecem pendentes. Diretriz seguinte: distribuir tiers para facilitar testes agrícolas. Leitura preliminar de ItemHoe/ItemWateringCan e limites getXMinus/getXPlus/getZMinus/getZPlus de ItemTool comprova regador SILVER com largura 3/profundidade 3 e enxada SILVER com largura 1/profundidade 3, dependente de carga em ItemToolChargeable. Não existe enxada 3×3 nesses tiers; custos, carga, progressão e APIs ainda exigem levantamento fechado antes da implementação.
