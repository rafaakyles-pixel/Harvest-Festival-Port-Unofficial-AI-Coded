# Ferramentas e solo — contratos vigentes

Consultar para ferramentas, água, solo e cultivo (fatia 10). Pacote harvestfestival.tools aprovado em 09/11/2026.

### Comportamento e contrato para a fatia 10

- `harvestfestival:hoe` e `harvestfestival:watering_can`: nível único, pilha máxima 1, sem durabilidade, melhorias, receita ou aquisição por lojas. Disponíveis por `/give`. Texturas provisórias vanilla de enxada de madeira e balde; há barra e tooltip de água conforme complemento abaixo; não há HUD de ouro/aniversário.
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

Assets vanilla referenciados (enxada, balde, terra, farmland seco/molhado e modelo cube_bottom_top) conferidos no JAR local da versão alvo. Aceite posterior e limites de validação constam da seção final.

### Relato sobre o solo, registrado sem implementação

O responsável observou que o solo não seca com o tempo nem ao passar dos dias, e que pisar nele não o faz retornar à terra. São comportamentos ausentes do bloco passivo aprovado para a fatia 9. A secagem/consumo da marca de rega na atualização diária é pendência essencial para o ciclo de cultivo da fatia 10. Retorno à terra por pisoteio e eventuais outras condições de secagem devem ter comportamento e condições conferidos no original antes de implementar; não presumir equivalência com as regras vanilla ou que caminhar e saltar são a mesma ação. Este complemento somente registra essas pendências, sem alterar o solo.

### Indicadores de água implementados

Pedido adicional autorizado pelo responsável para facilitar os testes. O regador apresenta barra azul no ícone do inventário/hotbar e tooltip traduzido `Água: atual / 128` (`Water: current / 128`). A linha fica vermelha quando vazio e aqua quando possui água. A barra usa a faixa vanilla de 0 a 13 pixels; arredonda para cima para manter pelo menos um pixel enquanto existir uma unidade. O tooltip fornece a quantidade exata. Vazio mantém o fundo da barra, sem preenchimento azul; cheio ocupa os 13 pixels. Estado ausente ou inválido continua representando zero.

A apresentação lê exclusivamente WateringCanState do próprio ItemStack, já persistido e sincronizado pelo inventário. Não muda consumo, enchimento, codec, capacidade, regras por tags ou comportamento do solo. A apresentação não adiciona telas, rede ou armazenamento. Respeita ocultação do tooltip e ocultação do componente de água. A textura provisória do item permanece a existente; esta alteração é o indicador de quantidade, não uma arte definitiva do regador.

APIs verificadas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `Item.isBarVisible`, `getBarWidth`, `getBarColor`, `RegisterTooltipAppendersEvent.registerAppender`, `TooltipLocation.POST_CUSTOM`, `TooltipAppender`, `TooltipDisplay.hideTooltip/shows` e `Component.translatable`. Registro no barramento do mod em ToolContent. Foi usado o evento moderno de appenders em lugar do método depreciado Item.appendHoverText. Nenhum pacote ou mixin novo.

## Validação consolidada

Fatia 9: suíte total 57, 19 novos, execução aprovada; single player aceito em 09/11/2026 sem detalhar cada passo. Dedicado/multiplayer pendentes. Complemento visual: 57 testes existentes reexecutados, sem novos; validação visual pendente. Cenário 8 integra obrigatoriamente o aceite deste complemento e permanece não executado. A fatia 10 consumirá/limpará WATERED diariamente; pisoteio e outras condições de secagem dependem de conferir o original.
