## Fatia 9 — levantamento e proposta aguardando aprovação (09/11/2026)

Fase 3 atual e fatia 9 em andamento, exclusivamente em preparação. O responsável confirmou o pagamento de remessas offline somente na próxima virada com o jogador conectado. Cenário 8 deve integrar os critérios de aceite da primeira fatia com HUD/interface. Nenhum pacote ou código novo autorizado nesta preparação.

Leitura da referência restrita a ItemHoe, ItemWateringCan, ItemToolChargeable e ItemTool. Etapas 5 e 6 e evidência de Ferramentas de DEPENDENCIES.md consultadas: separar o contrato mínimo de ação dos consumidores de cultivo, coleta e esforço. Etapa 4 parcialmente entregue; cesta, flores e demais apoios continuam pendentes, sem implementação nesta fatia.

Proposta para aprovação: pacote harvestfestival.tools; duas ferramentas de nível único; solo preparado mínimo com propriedade persistente watered, sem ticks de crescimento ou consumidor diário; regras de transformação de estado selecionadas por tags em JSON; reserva de 0 a 128 unidades no item, enchimento por água e consumo de uma unidade somente após rega efetiva em sobrevivência. O cultivo da fatia 10 consumirá e limpará a marca. O bloco de apoio e essas escolhas ainda não estão aprovados.

APIs conferidas por leitura das fontes locais exatas minecraft-patched-26.2.0.93-sources.jar e neoforge-26.2.0.93-sources.jar: Item.useOn(UseOnContext), Item.use(Level, Player, InteractionHand), Item.getPlayerPOVHitResult com ClipContext.Fluid.ANY, DeferredRegister.Items.registerItem, DeferredRegister.Blocks.registerBlock, DataComponentType.Builder.persistent/networkSynchronized, ItemStack.CODEC, BooleanProperty.create, Block.createBlockStateDefinition, Level.setBlock e AddServerReloadListenersEvent/addListener. Nenhuma compilação de implementação nova ou validação de gameplay foi realizada. Não há mixin proposto.

Validação desta preparação documental (09/11/2026): BUILD SUCCESSFUL in 15s; 38 testes nos XML, zero falhas, erros ou ignorados. Tarefa test UP-TO-DATE, sem reexecução nem testes das ferramentas ainda. Última linha literal: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`. Build executado com TEMP/TMP locais e COMPAT; acesso ao cache Gradle exigiu execução autorizada após bloqueio inicial de escrita no arquivo de lock. runClient não executado; servidor dedicado e multiplayer pendentes para a fatia 9.

## Fatia 9 — ferramentas mínimas implementadas (09/11/2026)

Plano e pacote `harvestfestival.tools` aprovados explicitamente pelo responsável em 09/11/2026. Esta seção sucede a proposta anterior; o restante das ferramentas por nível, esforço, cultivos e coleta permanece fora da fatia. `DEPENDENCIES.md` já estava aprovado; etapas 5 e 6 são atendidas somente pelo contrato mínimo autorizado. Cesta, flores e demais apoios da etapa 4 continuam pendentes para fatias próprias.

### Comportamento e contrato para a fatia 10

- `harvestfestival:hoe` e `harvestfestival:watering_can`: nível único, pilha máxima 1, sem durabilidade, melhorias, receita ou aquisição por lojas. Disponíveis por `/give`. Texturas provisórias vanilla de enxada de madeira e balde; não há HUD, tooltip ou barra de água.
- Enxada: clique em um único bloco da tag `harvestfestival:tillable`, com ar acima e face diferente de baixo, transforma em `harvestfestival:prepared_soil[watered=false]`. A tag inicial inclui terra, bloco de grama e caminho de terra. Terra grossa e remoção de plantas do original não integram o mínimo aprovado.
- Solo preparado: bloco passivo de apoio em cubo completo, topo de farmland seco/molhado, propriedade booleana `watered`, tag vanilla `minecraft:mineable/shovel` e loot de terra. Não é farmland vanilla, não hidrata com água próxima/chuva, não perde umidade por tick, não cresce nem dispara novo dia. Não tem item próprio. Pode ser colocado com `/setblock` para testes.
- Regador: novo item começa vazio. Mirar água reconhecida por `minecraft:water` (inclui fluido corrente) e usar repõe até 128 sem remover o fluido. Uma interação com água tem prioridade sobre regar e é consumida mesmo quando já está cheio, sem aumentar a reserva. Para regar, clicar no solo com `watered=false`; o servidor grava `true` e consome 1 unidade somente após a escrita aceita. Alvo inválido, repetição, bloqueio, ambiguidade, reserva vazia ou falha de escrita não consomem. Criativo exige reserva positiva, mas não a reduz.
- `ToolActions.apply(ToolAction, Target, ItemStack, boolean creative)` é o contrato de servidor para um alvo explícito. `Target` fornece estado, associação a tags (implementação padrão usa `BlockState.is`), espaço acima, autorização e escrita que retorna se houve alteração aceita. Resultados: `CHANGED`, `NO_MATCH`, `BLOCKED`, `EMPTY`, `UNCHANGED`, `AMBIGUOUS` e `FAILED`. O adaptador de interação verifica jogador, face, `mayInteract` e `mayUseItemAt` antes de chamar o contrato; o núcleo permite testar a lógica sem construir um mundo.
- O chamador futuro deve fornecer um alvo autorizado e executar no servidor. A fatia 10 localizará o solo sob a planta e consumirá/limpará `PreparedSoilBlock.WATERED` no ciclo diário. Nesta fatia, a marca permanece inclusive após novo dia e salvar/reabrir; não existe contador paralelo ou consumidor de `NewDayEvent`. O modelo de blocos/chunks já persiste e sincroniza o estado. O componente do item acompanha o inventário vanilla; não há pacote de rede personalizado nem attachment de jogador para água.

### Dados recarregáveis

Diretório `data/harvestfestival/tool_actions/`, um recurso JSON por regra. Somente esse namespace é lido, como nos catálogos anteriores. Um datapack pode substituir os arquivos ou fornecer novas regras nesse diretório. Exemplo da enxada:

```json
{
  "action": "hoe",
  "target_tag": "harvestfestival:tillable",
  "requires_air_above": true,
  "result": {
    "block": "harvestfestival:prepared_soil",
    "properties": {"watered": "false"}
  }
}
```

Regra de rega:

```json
{
  "action": "water",
  "target_tag": "harvestfestival:waterable",
  "requires_properties": {"watered": "false"},
  "result": {
    "block": "harvestfestival:prepared_soil",
    "properties": {"watered": "true"}
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

Assets vanilla referenciados (enxada, balde, terra, farmland seco/molhado e modelo cube_bottom_top) conferidos no JAR local da versão alvo. Aparência em jogo ainda não observada nesta implementação.

Build de implementação: `BUILD SUCCESSFUL in 13s`; **57 testes**, 19 novos, zero falhas, erros ou ignorados. Tarefa `test` executada, sem apenas reutilizar relatórios. Comando com ambiente TEMP/TMP local: `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

Cobertura nova: 7 testes de ação/estado do solo, 7 de água/persistência/rede e 5 de regras/recarga. Há prova com tags reais aplicadas ao registro e JSONs obtidos dos recursos empacotados, além de `MultiPackResourceManager`/`PathPackResources` reais para sobreposição e regra superior inválida. Fixtures de ação não simulam cliente/servidor ou cliques reais. O teste de recursos foi ajustado para usar o classpath porque o launcher não usa necessariamente a raiz do projeto; tags globais alteradas pelo teste são restauradas ao final.

`runClient` não executado nesta implementação; roteiro abaixo pendente de execução e aceite. Servidor dedicado e multiplayer permanecem pendentes para esta fatia. O teste de codec de rede não encerra esses cenários nem o cenário 8 de ouro/aniversário. Nenhum comando Git executado.

### Roteiro manual — single player

Usar um mundo **novo de teste**, com comandos habilitados. O perfil atual `runClient` usa `run/client1`, portanto saves, datapacks e logs ficam sob essa pasta (não sob `run/saves`). Iniciar na raiz com o ambiente Windows já documentado:

```powershell
$env:TEMP = (Resolve-Path 'build/tmp').Path
$env:TMP = $env:TEMP
.\gradlew.bat runClient '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain
```

1. Preparar bancada e ferramentas no mundo de teste:

```text
/gamemode creative
/fill 99 79 99 104 80 104 minecraft:stone
/fill 100 80 100 102 80 100 minecraft:dirt
/setblock 102 81 100 minecraft:stone
/setblock 100 80 102 minecraft:water
/tp @s 101 81 103
/give @s harvestfestival:hoe
/give @s harvestfestival:watering_can
/gamemode survival
```

2. Selecionar a enxada e clicar no topo da terra em `100 80 100` e `101 80 100`: ambos viram solo preparado seco. Clicar na pedra `103 80 100` e na terra obstruída `102 80 100` (pela lateral): não mudam. Confirmar o primeiro com `/execute if block 100 80 100 harvestfestival:prepared_soil[watered=false] run say solo_seco`.
3. Selecionar o regador vazio e clicar no solo: continua seco. `/data get entity @s Inventory` permite inspecionar o componente do regador; ausência de componente equivale a zero. Não há barra/HUD.
4. Mirar diretamente a água `100 80 102` e clicar: componente passa a `{water:128}`; fonte permanece. Repetir: continua 128. Clicar no solo `100 80 100`: topo escurece e água passa a 127. Confirmar com `/execute if block 100 80 100 harvestfestival:prepared_soil[watered=true] run say solo_regado`. Repetir no solo molhado e na pedra: mantém 127.
5. Salvar/sair e reabrir: conferir solo molhado e regador com 127. Executar `/hf newDay` e aguardar avanço do relógio (usar `/time resume` se estiver pausado): solo continua molhado nesta fatia, sem consumo adicional.
6. Criativo: `/gamemode creative`, regar o segundo solo seco em `101 80 100`: muda para molhado, regador continua 127. Voltar a survival. Para provar a última unidade, guardar o regador anterior num baú ou removê-lo com `/clear @s harvestfestival:watering_can`, depois `/give @s harvestfestival:watering_can[harvestfestival:watering_can_state={water:1}]`. Repor os dois solos secos com `/setblock 100 80 100 harvestfestival:prepared_soil[watered=false]` e equivalente em `101 80 100`. Regar o primeiro consome a última unidade; o segundo permanece seco. Encher novamente na água permite regar o segundo.
7. Dados inválidos no item: remover o regador e usar `/give @s harvestfestival:watering_can[harvestfestival:watering_can_state={water:129}]`. Deve existir como item vazio e não regar; encher na fonte deve recuperá-lo normalmente. Repetir com `{water:-1}` ou `{water:1.5}` se desejado. Conferir nomes em inglês/português e modelos simples; a troca de idioma não altera o estado.
8. Datapack: criar `run/client1/saves/<mundo>/datapacks/hf_tools_validation/pack.mcmeta` com `{"pack":{"description":"Validação das ferramentas","min_format":[107,1],"max_format":[107,1]}}`. Criar `data/harvestfestival/tags/block/tillable.json` dentro do pack com `{"replace":true,"values":["minecraft:stone"]}`. `/reload`; se necessário `/datapack enable "file/hf_tools_validation" last`. Enxada agora deve preparar pedra livre e recusar terra. Restaurar a tag para terra ao terminar este passo ou desativar o pack.
9. No mesmo pack, criar `data/harvestfestival/tool_actions/water.json` contendo apenas `{`. `/reload`: aviso `Ignoring tool action ...water.json` em `run/client1/logs/latest.log`; rega indisponível, sem consumo, e enxada válida continua funcionando. Substituir pelo JSON válido da seção acima e recarregar: rega retorna. Acrescentar uma cópia válida como `duplicate.json`: tentar regar solo seco deve gerar `Ambiguous tool action`, sem mudança ou consumo. Remover a cópia e recarregar: volta a funcionar. Desativar o pack e recarregar: regras/tags originais retornam.
10. Salvar/sair e encerrar o cliente. Registrar resultados por passo e executar o build novamente. Sair/reabrir single player não encerra a pendência multiplayer.

### Aceite da entrega da fatia 9 — 09/11/2026

O responsável aprovou a entrega implementada. Não informou execução ou resultados do roteiro manual; portanto o aceite não é evidência de validação single player, servidor dedicado ou multiplayer. Essas pendências permanecem explícitas. Nenhum código alterado e nenhuma implementação da fatia 10 iniciada por este aceite.

## Aceite manual da fatia 9 e complemento visual do regador — 09/11/2026

O responsável informou «Teste validado, fatia aprovada». Isso encerra a fatia 9 quanto ao aceite single player, sucedendo o registro anterior que aguardava confirmação. O relato não detalhou resultados individuais por passo e não amplia a cobertura para servidor dedicado ou multiplayer.

### Relato sobre o solo, registrado sem implementação

O responsável observou que o solo não seca com o tempo nem ao passar dos dias, e que pisar nele não o faz retornar à terra. São comportamentos ausentes do bloco passivo aprovado para a fatia 9. A secagem/consumo da marca de rega na atualização diária é pendência essencial para o ciclo de cultivo da fatia 10. Retorno à terra por pisoteio e eventuais outras condições de secagem devem ter comportamento e condições conferidos no original antes de implementar; não presumir equivalência com as regras vanilla ou que caminhar e saltar são a mesma ação. Este complemento somente registra essas pendências, sem alterar o solo.

### Indicadores de água implementados

Pedido adicional autorizado pelo responsável para facilitar os testes. O regador apresenta barra azul no ícone do inventário/hotbar e tooltip traduzido `Água: atual / 128` (`Water: current / 128`). A linha fica vermelha quando vazio e aqua quando possui água. A barra usa a faixa vanilla de 0 a 13 pixels; arredonda para cima para manter pelo menos um pixel enquanto existir uma unidade. O tooltip fornece a quantidade exata. Vazio mantém o fundo da barra, sem preenchimento azul; cheio ocupa os 13 pixels. Estado ausente ou inválido continua representando zero.

A apresentação lê exclusivamente WateringCanState do próprio ItemStack, já persistido e sincronizado pelo inventário. Não muda consumo, enchimento, codec, capacidade, regras por tags ou comportamento do solo. A apresentação não adiciona telas, rede ou armazenamento. Respeita ocultação do tooltip e ocultação do componente de água. A textura provisória do item permanece a existente; esta alteração é o indicador de quantidade, não uma arte definitiva do regador.

APIs verificadas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `Item.isBarVisible`, `getBarWidth`, `getBarColor`, `RegisterTooltipAppendersEvent.registerAppender`, `TooltipLocation.POST_CUSTOM`, `TooltipAppender`, `TooltipDisplay.hideTooltip/shows` e `Component.translatable`. Registro no barramento do mod em ToolContent. Foi usado o evento moderno de appenders em lugar do método depreciado Item.appendHoverText. Nenhum pacote ou mixin novo.

### Critérios de aceite e verificação

1. Em `runClient`, passar o cursor sobre um regador vazio: `Água: 0 / 128` em vermelho, barra sem azul. Encher mirando água: `128 / 128` e barra azul cheia.
2. Em sobrevivência, regar um solo seco: `127 / 128`. Repetir em solo molhado não reduz. Para inspecionar faixas rapidamente, `/give @s harvestfestival:watering_can[harvestfestival:watering_can_state={water:64}]` deve mostrar `64 / 128` e 7 pixels; com `water:1`, um pixel; gastar a última unidade deixa vazio.
3. Salvar/reabrir e conferir que os indicadores refletem o mesmo valor. Trocar idioma entre português e inglês e conferir a linha traduzida. Esses passos verificam o complemento visual, não repetem a alegação de aceite da fatia 9 anterior.
4. **Cenário 8 obrigatório incluído no aceite deste complemento**, pois agora existe interface visual: inspecionar espelho cliente de ouro/aniversário do titular, atualização, limpeza, reconexão e isolamento entre jogadores. Continua não executado. Barra/tooltip de água, comandos administrativos e testes de codec não comprovam ouro/aniversário; não declarar encerrado esse critério com base no teste do regador. Nenhum HUD de ouro ou aniversário foi antecipado.

Build: `BUILD SUCCESSFUL in 15s`; 57 testes existentes reexecutados, zero falhas, erros ou ignorados. Sem testes novos para este ajuste de apresentação. Comando com TEMP/TMP locais: `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

O agente não executou runClient neste complemento; aparência e passos novos permanecem pendentes de validação manual. Servidor dedicado, multiplayer e cenário 8 continuam pendentes. Nenhum comando Git executado.
