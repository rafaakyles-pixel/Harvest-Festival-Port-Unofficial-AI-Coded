## Fase 2 — sexta fatia, preços de remessa por item e tag (06/11/2026)

Uma regra por recurso em `data/harvestfestival/shipping_prices/items/<namespace>/<caminho>.json` ou `tags/<namespace>/<caminho>.json` sob o mesmo diretório de preços. O datapack de maior precedência fornece o arquivo efetivo para cada caminho. A recarga recompõe a tabela do zero e a publica inteira. Se o arquivo efetivo for inválido, aquela regra é descartada com aviso e as demais continuam disponíveis. Um arquivo removido perde efeito na próxima recarga, respeitada a precedência dos packs.

`price` é inteiro positivo até o máximo de long. A regra de tag exige também `priority`, inteiro de 32 bits com sinal. Vale primeiro o item específico. Entre as tags vigentes do item vale a maior prioridade e, em empate nessa prioridade, o maior preço. O aviso identifica as tags empatadas e sai uma vez por item por recarga. Sem regra aplicável, usa-se a leitura existente de SellValue e exige-se valor positivo. O depósito grava o preço na fila e o pagamento usa o valor gravado.

No original, ShippingRegistry consulta SellValue antes do registro. A precedência desta fatia foi definida pelo responsável. HFCrops usa os preços de venda de Crop e registra os produtos. A tabela inicial aproveita somente os valores de cenoura (160 por item) e trigo (100 pela tag comum c:crops/wheat).

APIs conferidas nos artefatos locais de 26.2.0.93 e exercitadas pelo build: AddServerReloadListenersEvent, SortedReloadListenerEvent.addListener, SimplePreparableReloadListener.prepare/apply, ResourceManager.listResources, BuiltInRegistries.ITEM.getOptional e Holder.is(TagKey). A busca opcional permite rejeitar um identificador desconhecido antes da inclusão na tabela.

### Testes automatizados

O launcher unitário do NeoForge inicializa os registros sem abrir um mundo. Cada teste com tags cria um MappedRegistry de teste, registra o graveto vanilla em um holder próprio, vincula test:a e test:b e congela o registro. O ItemStack usa esse holder. As regras passam pelo leitor de JSON de produção.

| Teste de tag | Verificação com bootstrap, sem jogo |
| --- | --- |
| specificItemBeatsTagAndSellValue | Regra específica de 7 prevalece sobre tag de 50 e SellValue de 90. |
| higherPriorityWinsEvenWithLowerPriceAndSellValue | Entre prioridades negativas, −1 prevalece sobre −2, mesmo com preço menor. |
| tiedTagsWarnOnceAndPayHigherPrice | Duas tags na maior prioridade geram um aviso com ambas e pagam 50 em duas consultas. |
| nonMatchingTagFallsBackToSellValueOrRejects | A tag ausente do holder permite recusar o graveto sem consumo e aceitar o mesmo item após atribuir SellValue. |
| reloadBetweenDepositsPaysBothFrozenPrices | Dois conjuntos de recursos são lidos e aplicados em sequência pelo listener. Dois depósitos pela mesma tag pagam 100 + 140. |

Dois outros testes verificam arquivo malformado e preço negativo ao lado de uma regra válida, e ID de item desconhecido descartado com aviso. Build aprovado em 06/11/2026: 26 testes, zero falhas, erros ou ignorados. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

### Roteiro manual em single player

1. Execute `.\gradlew.bat runClient` com o ambiente Windows documentado. Abra um mundo de teste novo com comandos, execute `/hf gold set 0`, coloque a caixa obtida com `/give @s harvestfestival:shipping_box` e receba dois trigos com `/give @s minecraft:wheat 2`. Logo depois de uma virada de dia, deposite um trigo com um clique sem agachar. Um trigo deve restar e o saldo deve continuar zero.
2. Na pasta do projeto, abra `run/saves/<pasta do mundo>/datapacks/`. Crie a pasta `hf_precos_teste`. Na raiz dela, salve o arquivo UTF-8 `pack.mcmeta` (sem extensão .txt). O formato de dados 107.1 foi conferido no version.json do Minecraft alvo.

```json
{
  "pack": {
    "description": "Teste de preços de remessa",
    "min_format": [107, 1],
    "max_format": [107, 1]
  }
}
```

3. Dentro de `hf_precos_teste`, crie `data/harvestfestival/shipping_prices/tags/c/crops/wheat.json` com o conteúdo abaixo. A pasta data e o pack.mcmeta devem ficar lado a lado.

```json
{ "price": 140, "priority": 0 }
```

4. Execute `/reload` e aguarde. Confira `/datapack list enabled` e procure `file/hf_precos_teste`. Se aparecer apenas em `/datapack list available`, execute `/datapack enable "file/hf_precos_teste" last` e confira novamente. Use o mundo novo apenas com o mod e esse pack. Confira em `run/logs/latest.log` se houve avisos sobre o arquivo.
5. Antes da próxima virada, deposite o segundo trigo. Execute `/hf newDay`, aguarde o avanço do relógio e consulte `/hf gold get`. O total esperado é 240. Isso verifica o acionamento real por /reload, a associação do trigo vanilla à tag comum fornecida pelo NeoForge, a precedência do arquivo externo e o preço anterior preservado. Uma nova virada sem depósito deve manter 240.
6. Para conferir o carregamento tolerante a falhas no mundo, adicione `data/harvestfestival/shipping_prices/items/minecraft/inexistente.json` com `{"price":1}` e um segundo arquivo `items/minecraft/stick.json` no mesmo diretório de preços contendo apenas `{`. Execute `/reload`, confira os dois caminhos nos avisos de latest.log e verifique que um graveto comum é recusado e outro trigo rende 140.

### Validação pelo responsável — 06/11/2026

O responsável executou os passos 1 a 6 com `runClient` em mundo single player, sem erros. A caixa aceitou trigo pela tag `c:crops/wheat`. Os depósitos a 100 antes e a 140 depois de `/reload` renderam 240 após `/hf newDay`. O responsável conferiu em `latest.log` os avisos sobre o identificador de item inexistente e o JSON malformado.

Sexta fatia concluída. A Fase 2 permanece aberta e a próxima fatia é o aniversário do jogador. Servidor dedicado, multiplayer e reconexão continuam pendentes.

