# Notas de portabilidade

## Escopo e verificação

Auditoria estática em 30/10/2026 da cópia `reference/original-1.12.2/`, mantida somente para leitura. Inventário em `FEATURES.md` e arquitetura em `DEPENDENCIES.md`, aprovado explicitamente pelo usuário em 30/10/2026. Fase 1 validada; primeira fatia da Fase 2 implementada: saldo individual persistente e comandos administrativos, com validação funcional em 02/11/2026.

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

## Fase 1 — esqueleto e ambiente

- Autorização de início recebida em 30/10/2026. A revisão de cobertura não encontrou sistemas/pacotes, missões anotadas ou mixins ausentes dos artefatos da Fase 0.
- Base técnica consultada: [MDK oficial de Minecraft 26.2 com ModDevGradle](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle/tree/a0340c5454c01e43ccf83977601a4d90afee6e1f). Wrapper e licença do template obtidos desse commit, sem executar comandos Git.
- Versões fixadas: Minecraft **26.2**, NeoForge **26.2.0.93**, ModDevGradle **2.0.148**, wrapper Gradle **9.2.1** e toolchain **Java 25**, conforme configuração do MDK. JDK local utilizado: Oracle **25.0.5.0.2**.
- Identificador do mod: `harvestfestival`; pacote mínimo: `harvestfestival`; versão técnica inicial do port: `0.1.0-dev` (independente do limite de conteúdo original 0.6.x).
- `HarvestFestival` usa `@Mod` e registra somente uma mensagem de carregamento. Não foram acrescentados itens, blocos, sistemas, mixins ou integrações.
- `src/main/templates/META-INF/neoforge.mods.toml` é expandido pelo build para `build/resources/main/META-INF/neoforge.mods.toml`. As dependências estão restritas às versões exatas confirmadas. Não há valores `${...}` não resolvidos no metadado gerado.
- Licença MIT do template preservada em `TEMPLATE_LICENSE.txt`. Nenhum código de gameplay da referência foi copiado nesta fase.

### Problemas de ambiente encontrados

1. O ambiente restrito bloqueou resolução de hosts e escrita no cache Gradle do usuário. Downloads/build foram executados com a autorização de acesso correspondente; a referência permaneceu somente leitura.
2. O Gradle iniciou, mas falhou com `Unable to establish loopback connection`, causado por `UnixDomainSockets.connect` / `Invalid argument: connect`. Direcionar `TEMP` e `TMP` para `build/tmp` no processo resolveu a falha; build e cliente funcionaram com esse ajuste. Nenhuma configuração global do Windows foi alterada.
3. O log do cliente registra avisos/erro do OSHI sobre contadores Windows `Perflib 009`, além de avisos vanilla de comandos/traduções. Não impediram o carregamento nem a abertura do menu; contadores do sistema não foram modificados.

### Como reproduzir no PowerShell (histórico da Fase 1)

Executar na raiz do projeto. Ajustar `JAVA_HOME` se o JDK 25 estiver instalado em outro local. O ajuste temporário abaixo foi necessário neste ambiente; suas variáveis valem apenas para a sessão atual do terminal.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.5.0.2'
New-Item -ItemType Directory -Path 'build/tmp' -Force | Out-Null
$env:TEMP = (Resolve-Path 'build/tmp').Path
$env:TMP = $env:TEMP
.\gradlew.bat build
.\gradlew.bat runClient
```

## Validação e limitações

- Revisão documental: sistemas/pacotes, 123 missões anotadas e os 2 mixins conferidos; catálogos complementares em `catalogs/`, indexados por `FEATURES.md`.
- Build do esqueleto: **BUILD SUCCESSFUL**, 30/10/2026, com artefato `build/libs/harvestfestival-0.1.0-dev.jar`. Não há fontes de testes nesta fase; o build não representa teste de gameplay.
- Cliente: executado por `runClient`, janela `Minecraft NeoForge* 26.2` aberta e menu principal observado, exibindo Minecraft 26.2, NeoForge 26.2.0.93 e 3 mods carregados.
- `run/logs/latest.log` confirma `Harvest Festival 0.1.0-dev (harvestfestival)` e `Harvest Festival skeleton loaded; no gameplay content registered.`
- Servidor dedicado, mundo jogável, persistência e gameplay não foram validados nesta fatia. As equivalências de mixins acima continuam sujeitas à verificação quando seus sistemas forem implementados.
- Fase 1 validada também pelo usuário, que confirmou o funcionamento do cliente e autorizou a continuação em 30/10/2026. A primeira fatia da Fase 2 foi posteriormente aprovada: saldo persistente e comandos administrativos, conforme `STATUS.md`.

## Fase 2 — saldo persistente por jogador

### Comportamento e limites da fatia

- Referências consultadas: `player/stats/Stats`, `StatsServer`, `PlayerLoader` e `player/command/HFCommandGold`. Saldo inicial zero, armazenamento `long`, valores negativos permitidos e soma com overflow de 64 bits preservados, sem teto ou clamp adicional.
- O original mantém dados por UUID fora da entidade. No port, o attachment `harvestfestival:gold` é salvo pelo ciclo vanilla/NeoForge de dados do jogador. `copyOnDeath()` preserva o ouro na substituição da entidade ao renascer. Não há migração dos arquivos `HF/<UUID>.dat` de 1.12.2 nesta fatia.
- API de acesso `PlayerGold.get/set/add` recebe `ServerPlayer`; comandos executam no servidor. O saldo ainda não tem espelho de cliente: o resultado é enviado pelas mensagens dos comandos. Sincronização para HUD/GUI pertence à futura fatia de interface.
- Nenhum item, bloco, tag de conteúdo, mixin, loja, remessa ou integração externa foi acrescentado. Traduções dos comandos em JSON `en_us` e `pt_br`.

### APIs verificadas na versão exata

Conferidas nas fontes locais `neoforge-26.2.0.93-sources.jar` e `build/moddev/artifacts/minecraft-patched-26.2.0.93-sources.jar`, e confirmadas por compilação/execução:

| Necessidade | API usada |
| --- | --- |
| Registro do dado | `DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID)` e registro no `IEventBus` do mod |
| Valor inicial e persistência | `AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("gold"))`; nesta versão, `serialize` recebe `MapCodec` |
| Leitura/alteração | `ServerPlayer.getData/setData` com fornecedor do attachment |
| Preservação no renascimento | `AttachmentType.Builder.copyOnDeath()` |
| Comandos | `RegisterCommandsEvent` no `NeoForge.EVENT_BUS`, Brigadier e `EntityArgument.player()` |
| Permissão administrativa | `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`, equivalente ao nível 2 usado pelo original |
| Mensagens | `CommandSourceStack.sendSuccess` e `Component.translatable` |

O payload é um composto com campo `gold` de 64 bits dentro de `neoforge:attachments` / `harvestfestival:gold`. O salvamento acompanha autosave e saída normal do jogador/mundo; não é uma gravação síncrona de disco a cada comando.

### Comandos e verificação manual

```text
/hf gold get
/hf gold set <valor>
/hf gold add <valor>
/hf gold <jogador> get
/hf gold <jogador> set <valor>
/hf gold <jogador> add <valor>
```

Alvo omitido usa o executor jogador; console precisa informar um jogador conectado. Nome e seletor de jogador único são aceitos. `get` é a consulta aprovada nesta fatia. `set`/`add` preservam a ordem dos argumentos do original; o parser Brigadier rejeita verbos desconhecidos e números fora de `long` em vez de reproduzir o fallback antigo que tratava qualquer verbo diferente de `set` como adição. O retorno numérico do comando é sucesso (`1`); o saldo completo aparece na mensagem.

Teste realizado em 02/11/2026, por `runClient`, no mundo local `HF Gold Validation` com comandos habilitados:

1. `/hf gold get` retornou `0`.
2. `/hf gold set 3000000000` e `/hf gold Dev add -25` retornaram `2999999975`.
3. Após salvar/sair e reabrir o mundo, `/hf gold @s get` retornou `2999999975`.
4. Após `/kill @s` e renascimento, `/hf gold get` manteve `2999999975`.
5. Mundo salvo e cliente encerrado normalmente; mensagens e ciclos de salvamento registrados em `run/logs/latest.log`.

### Testes automatizados e ambiente Windows

`PlayerGoldPersistenceTest` usa o carregamento real do mod pela integração `unitTest` do ModDevGradle e JUnit Jupiter 5.13.4, sem incluir testes no JAR de distribuição. Os três testes cobrem saldo inicial/dado ausente, isolamento entre dois titulares e serialização do attachment registrado para arquivo NBT comprimido com releitura por um titular novo. Valores cobertos: `0`, `125`, `-50`, `2147483648`, `Long.MIN_VALUE` e `Long.MAX_VALUE`. Estes testes exercitam armazenamento, não simulam dois clientes conectados.

Além do problema de socket já registrado na Fase 1, surgiram duas falhas de ambiente na inicialização do JUnit:

- `GradleWorkerMain` não encontrado: arquivo de argumentos do Gradle escrito em UTF-8 no caminho de usuário acentuado e interpretado pelo launcher Windows com outra codificação. O override de processo `-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT` resolveu o lançamento.
- `AgentLoadException` / JAR de instrumentação não encontrado: caminho temporário com acento recebido incorretamente pelo self-attach do FML. Usar a forma curta ASCII do diretório `build/tmp` resolveu. Não houve alteração de configurações globais do Windows, JDK ou Gradle.
- `generateModMetadata.filteringCharset = 'UTF-8'` mantém a leitura/expansão dos metadados independente da codificação de execução do Gradle.

Procedimento histórico anterior à mudança para `C:\dev` (não reutilizar os caminhos antigos; procedimento atual ao final deste documento):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.5.0.2'
New-Item -ItemType Directory -Path 'build/tmp' -Force | Out-Null
# Consulta somente a forma curta do caminho, sem mover ou apagar arquivos.
cmd /c 'for %I in ("C:\Users\Usuário\Documents\harvest-festival-port\build\tmp") do @echo %~sI'
$env:TEMP = 'C:/Users/USURIO~2/DOCUME~1/HARVES~1/build/tmp'
$env:TMP = $env:TEMP
.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT' --console=plain
.\gradlew.bat runClient '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT' --console=plain
```

Limitações: servidor dedicado, dois jogadores simultâneos e rejeição de comandos sem permissão não foram validados em execução. A permissão foi conferida na API usada; isolamento do attachment foi testado automaticamente. Os avisos OSHI/Perflib anteriores continuam presentes e não impediram a validação funcional.

Build final de 02/11/2026: **BUILD SUCCESSFUL**, com três testes executados, zero falhas e zero erros. Relatório em `build/reports/tests/test/index.html`; artefato em `build/libs/harvestfestival-0.1.0-dev.jar`. `runClient` também terminou com sucesso após saída normal. A Fase 2 não está integralmente concluída; somente esta fatia foi encerrada.

## Ambiente atual após mudança de pasta — 02/11/2026

- Raiz atual: `C:\dev\harvest-festival-port`. O atributo oculto de `C:\dev` não impediu leitura, build ou execução dos testes.
- Java do launcher/daemon Gradle: Eclipse Temurin **21.0.13+9**, em `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`, confirmado por `gradlew.bat --version` com `JAVA_HOME` definido no processo.
- Toolchain do projeto: **Java 25**, mantida em `build.gradle`. A instalação do Java 21 não altera o alvo de compilação nem substitui a toolchain usada pelos testes/jogo. O JDK 25 continua instalado.
- Nesta sessão do agente, `JAVA_HOME` e `GRADLE_USER_HOME` não estavam definidos; não presumir que a instalação mudou o Java escolhido por todos os terminais. O cache Gradle continua em `C:\Users\Usuário\.gradle`.
- A tentativa sem ajuste de temporários falhou com `Unable to establish loopback connection`. Com `TEMP`/`TMP` no novo projeto, o primeiro build passou reaproveitando os testes anteriores. Ao forçar a execução dos testes, o executor falhou; o override de codificação `COMPAT` resolveu. Portanto, o sucesso inicial com tarefas `UP-TO-DATE` não bastava para confirmar o novo ambiente.
- Validação final: `BUILD SUCCESSFUL`, com os três testes efetivamente reexecutados, zero falhas e zero erros. Não foi executado `runClient` após esta mudança; a validação funcional anterior continua registrada acima.
- Nenhuma configuração global de Java, Windows ou Gradle foi alterada pelo agente. Os procedimentos antigos foram preservados como histórico.

Procedimento atual validado no PowerShell:

```powershell
Set-Location 'C:\dev\harvest-festival-port'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
New-Item -ItemType Directory -Path 'build/tmp' -Force | Out-Null
$env:TEMP = (Resolve-Path 'build/tmp').Path
$env:TMP = $env:TEMP
.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT' --console=plain
# Para exigir nova execução dos testes mesmo com resultados em cache:
.\gradlew.bat build test --rerun '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT' --console=plain
```

O diretório padrão e as raízes de escrita informados pelo aplicativo para este chat ainda apontavam para as pastas antigas/OneDrive nesta conferência. As operações foram direcionadas explicitamente a `C:\dev\harvest-festival-port`, com autorização de execução fora das raízes padrão. Vincular essa pasta como raiz de trabalho no aplicativo evita depender desse acesso adicional em futuras tarefas.

## Fase 2 — calendário básico

Implementação aprovada: calendário derivado do relógio persistente do Overworld, com 30 dias por estação, quatro estações e início em segunda-feira, primavera, ano 1. Usa o padrão original de 24000 ticks por dia; configuração de duração, HUD, clima, festivais e notificações para sistemas futuros não fazem parte desta fatia.

APIs conferidas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `WorldClocks.OVERWORLD`, `MinecraftServer.clockManager()`, `ServerClockManager.getTotalTicks/setTotalTicks`. O próprio `ServerClockManager` é `SavedData`, carregado pelo servidor via `computeIfAbsent(ServerClockManager.TYPE)`; não há segundo contador persistente do mod. Pausa, velocidade do relógio, sono e comandos vanilla afetam a data pela mesma fonte de tempo.

Comandos administrativos de nível GAMEMASTERS: `/hf day` consulta data; `/hf day <valor>` limita o dia a 1–30; `/hf season <spring|summer|autumn|winter>` aceita também maiúsculas; `/hf year <valor>` limita o mínimo a 1. Ajustes preservam a hora. `/hf newDay` move para o último tick do dia, como o original; o dia seguinte começa quando o relógio avança, portanto um relógio pausado precisa ser retomado.

Cálculos usam long para evitar overflow intermediário de int do original em anos elevados. Não há migração de saves 1.12.2. Fontes originais consultadas: CalendarDate, CalendarHelper, CalendarServer, CalendarSavedData, NewDayHandler, HFCalendar e comandos de dia/estação/ano/novo dia.

Build aprovado com cinco testes de calendário e três de ouro. Testes de calendário: data inicial, limites de dia/estação/ano, preservação da hora e anos elevados, ciclo de 1200 dias e gravação/releitura de ClockState em NBT comprimido. O teste de codec não substitui salvar/reabrir um mundo real. Validação manual do calendário e fechamento da fatia ainda pendentes; runClient retomado após interrupção da revisão automática de permissões por limite de uso.
