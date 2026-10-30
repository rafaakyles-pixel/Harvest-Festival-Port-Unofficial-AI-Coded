# Notas de portabilidade

## Escopo e verificação

Auditoria estática em 30/10/2026 da cópia `reference/original-1.12.2/`. Nenhum código novo em `src/`; referência mantida somente para leitura. Inventário em `FEATURES.md` e arquitetura em `DEPENDENCIES.md`, aprovado explicitamente pelo usuário em 30/10/2026. A fase permanece 0; a aprovação não inicia a implementação.

O alvo é **Minecraft 26.2 com NeoForge 26.2.0.93**, verificado e confirmado pelo usuário em 30/10/2026 e registrado em `AGENTS.md`. As verificações individuais de APIs e de comportamento na versão exata continuam sujeitas às limitações abaixo.

O escopo está limitado ao conteúdo do mod até **0.6.x**. Funcionalidades de versões posteriores ou apenas planejadas em comentários/TODOs não serão implementadas. Os vestígios continuam documentados para rastreabilidade, sem representar compromisso de portagem.

## Organização do inventário

Em 30/10/2026, as seções de catálogo foram movidas integralmente de `FEATURES.md`, sem reescrever nem remover linhas, para `catalogs/variants.md`, `catalogs/content-registries.md`, `catalogs/quests.md` e `catalogs/resources.md`. O índice dos quatro catálogos foi acrescentado no topo de `FEATURES.md`; sistemas, dados salvos, mixins, integrações e cobertura da árvore permaneceram nesse arquivo. A reconstrução dos trechos na ordem original confirmou preservação exata do conteúdo.

## Mixins do original — lista completa

Conferência cruzada de todos os `@Mixin`, arquivos `*Mixin*.java`, configuração `src/main/resources/harvestfestival.mixins.json` e `InitMixins.getMixinConfigs()`: **2 mixins**, ambos de cliente; lista comum `mixins` vazia. O bootstrap `InitMixins` é um carregador, não um terceiro mixin. O original requer MixinBooter em `HFModInfo` e usa MixinExtras na segunda injeção.

| Mixin original | Classe e método vanilla 1.12.2 | Ponto de alteração e efeito | Alternativa sugerida para NeoForge 26.2                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `joshie.harvest.mixin.client.EntityRendererMixin` | `net.minecraft.client.renderer.EntityRenderer#addRainParticles()` | `@Inject` em `HEAD`, cancelável. Consulta `HFApi.calendar.getRecentNonSunnyWeather(mc.world)` e cancela todo o método quando `!weather.isRain()`. Impede os efeitos vanilla desse método sob clima não classificado como chuva; não altera a previsão ou o tempo do servidor. | **API candidata verificada na branch oficial 26.2.x:** `CustomWeatherEffectRenderer#tickRain(ClientLevel, long, Camera)` permite impedir o tick vanilla retornando `true`; cadastro via `RegisterCustomEnvironmentEffectRendererEvent#registerWeatherEffectRenderer`, seleção por `NeoForgeEnvironmentAttributes.CUSTOM_WEATHER_EFFECTS`. Usar o clima HF para decidir a supressão. Paridade exata de partículas/sons e aplicação dinâmica ao Overworld: **não verificado** em execução e na versão 26.2.0.93. |
| `joshie.harvest.mixin.client.RenderFishHookMixin` | `net.minecraft.client.renderer.entity.RenderFish#doRender(EntityFishHook, double, double, double, float, float)`; descritor `doRender(Lnet/minecraft/entity/projectile/EntityFishHook;DDDFF)V` | `@WrapOperation` nas chamadas a `ItemStack#getItem()`. Executa a chamada original e, quando o item é `joshie.harvest.fishing.item.ItemFishingRod`, retorna `Items.FISHING_ROD` somente à lógica desse renderizador. Faz reconhecer as varas HF ao calcular/renderizar a linha, sem substituir o item real no inventário. | **Não verificado:** procurar primeiro a extensão moderna que reconhece ações/capacidades de vara no renderizador; alternativamente, registrar renderizador próprio para a entidade de anzol HF. Não foi confirmado um evento/API equivalente na fonte 26.2 consultada; a tentativa de obter o patch oficial de `FishingHookRenderer` não retornou conteúdo. Não assumir que um evento de captura de peixe substitui lógica visual. Nenhum novo mixin aprovado.                                                 |

Fonte oficial da alternativa climática: [CustomWeatherEffectRenderer, branch 26.2.x](https://github.com/neoforged/NeoForge/blob/26.2.x/src/client/java/net/neoforged/neoforge/client/CustomWeatherEffectRenderer.java), consultada em 30/10/2026. A interface distingue renderização de precipitação (`renderSnowAndRain`) do tick de chuva (`tickRain`); apenas trocar a renderização não demonstra equivalência com `addRainParticles`.

O [guia oficial de migração 26.2](https://docs.neoforged.net/primer/docs/26.2/) documenta mudanças extensas de renderização vanilla, mas não comprova por si só substituições de hooks NeoForge. A branch consultada é móvel; fixar fonte/versão e validar os efeitos na futura fatia de implementação.

### Mecanismos invasivos que não são mixins

`src/main/resources/META-INF/harvestfestival_at.cfg` contém access transformers para renderização de chuva, GUI/inventários, sono, entidades, pesca, renderização de itens e quebra de blocos. Eles não aumentam a contagem de mixins. O original também usa reflexão e provedores antigos. Reavaliar cada necessidade na implementação; não copiar access transformers ou assinaturas antigas automaticamente.

## Equivalências e decisões para a futura implementação

| Original 1.12.2 | Direção de portagem, sem aprovação de API concreta |
| --- | --- |
| `OreDictionary`, metadados de variantes e IDs fixos | Tags de itens/blocos, registros modernos e dados JSON. Preservar variantes explícitas em `catalogs/variants.md`, indexado por `FEATURES.md`. |
| Receitas, preços, cultivos e catálogos em inicializadores Java | Priorizar dados e regras declarativas; não inventar um formato/API antes da fatia correspondente. |
| `WorldSavedData`, NBT manual, arquivos de jogador | Separar dados globais, por mundo, por jogador, por entidade e por bloco; mecanismo/assinaturas da versão exata: **não verificado**. Não prometer migração de saves 1.12.2. |
| `HFTrackers` e ciclos entre sistemas | Contratos básicos e composição gradual; grafo e ordem propostos em `DEPENDENCIES.md`. |
| Pacotes de rede e proxies cliente/servidor antigos | Reimplementar sincronização e isolamento do cliente pelas APIs da versão alvo; assinaturas ainda **não verificadas**. |
| `WorldProvider`, IDs numéricos de dimensão e manipulação de chunks | Reavaliar dimensão de mineração e clima com mecanismos modernos; não transportar acesso direto aos arrays antigos. |
| GUIs, TESR, modelos baked e OpenGL | Reimplementar interfaces/renderização conforme a versão confirmada; não transportar pipeline 1.12.2. |

Estas são direções arquiteturais impostas pelas regras do projeto, não código pronto nem confirmação de compatibilidade binária.

## Conteúdo ativo, vestígios e integrações

- Aquaculture, Biomes O' Plenty, Blood Magic, CraftTweaker e Immersive Engineering têm carregadores condicionais no Java. Todos estão **adiados** para o port, conforme `FEATURES.md`.
- Buttons e os handlers Morpheus estão comentados. `HFTools` ainda tem detecção/configuração de Morpheus; não confundir isso com o handler de sono reativado.
- JEI aparece como dependência de build, mas a busca no Java não encontrou plugin/import correspondente. Integração permanece **adiada**, sem atribuir funcionalidades ausentes.
- MixinBooter/MixinExtras atendem à infraestrutura legada. Não implicam obrigação de carregá-los na nova plataforma; decisão adiada.
- Casamento/pena azul possuem vestígios; `PurchasableBlueFeather` está comentada com TODO de reintrodução. Não inventariar casamento completo como gameplay ativo.
- `HFCrops.NETHER_WART` é registrado apesar do TODO de reintrodução adequada; manter explícita essa diferença entre registro e disponibilidade normal.
- A troca de provedor do Overworld e a configuração variável de dias por estação em `HFCalendar` estão comentadas. Não tratá-las como opções ativas.
- Há oito festivais registrados em `HFFestivals`; `festivals/none.json` é cenário auxiliar, não festival adicional.
- `HFSavedData.writeToNBT` não persiste os rastreadores; contém caminho de migração de cidades. Calendário/cidades/minas/jogadores e entidades possuem mecanismos próprios.

Não foi necessário decidir comportamento novo nesta auditoria. Vestígios de funcionalidades planejadas para depois de 0.6.x permanecem fora do escopo confirmado. Ambiguidades de gameplay dentro de 0.6.x exigirão pergunta explícita antes de implementar.

## Validação e limitações

- Revisão documental: sistemas/pacotes confrontados com árvore Java, registros de conteúdo, recursos e configuração de mixins; catálogos complementares em `catalogs/`, indexados por `FEATURES.md`.
- Builds obrigatórios **a partir da Fase 1**, conforme confirmação do usuário e atualização de `AGENTS.md`. Na auditoria anterior, `.\gradlew.bat build` falhou pela ausência do wrapper; não houve nova tentativa nesta tarefa documental da Fase 0.
- Limitação atual de ambiente/estrutura: não existem `gradlew.bat`, `build.gradle` ou `build.gradle.kts` na raiz. A criação e validação do esqueleto ficam para a Fase 1; não executar o build dentro da referência somente leitura.
- `runClient` não executado: o esqueleto da Fase 1 ainda não existe. Não houve validação de gameplay/renderização no jogo.
- A reorganização documental foi validada por preservação do conteúdo e conferência de arquivos/linhas; não implica build aprovado. A ausência atual do esqueleto não bloqueia a documentação da Fase 0. Nenhuma fase foi avançada.
