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
- Atualizado na quarta fatia: `PlayerGold.get(Player)` permite leitura no cliente; `set/add` continuam recebendo `ServerPlayer`. Comandos executam no servidor. O attachment agora sincroniza o saldo somente ao dono; HUD/GUI permanecem adiados.
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

Build aprovado com cinco testes de calendário e três de ouro. Testes de calendário: data inicial, limites de dia/estação/ano, preservação da hora e anos elevados, ciclo de 1200 dias e gravação/releitura de ClockState em NBT comprimido. O teste de codec não substitui salvar/reabrir um mundo real. Validação manual realizada pelo usuário e confirmada em 02/11/2026: comandos, troca de estação, virada do ano, salvar/reabrir e passagem natural do tempo passaram. A fatia de calendário básico está concluída. Essa confirmação não amplia a cobertura para servidor dedicado ou multiplayer simultâneo.

## Fase 2 — atualização diária interna

Regra aprovada pelo usuário: passagem natural, sono e `/hf newDay` notificam uma atualização; `/hf day`, `/hf season` e `/hf year` reposicionam a referência sem simular dias intermediários. Reabertura não reproduz atualizações anteriores.

`DailyUpdates` compara a data entre `ServerTickEvent.Pre` e `Post` (APIs conferidas nas fontes NeoForge 26.2.0.93), publicando `NewDayEvent` no barramento NeoForge, no servidor, com datas anterior/nova e servidor proprietário. `WorldCalendar.set` reposiciona a referência após ajustes administrativos. `ServerStoppedEvent` remove o estado da sessão. O marcador é transitório: inicializar a referência a cada início de tick evita replay no carregamento, sem criar outro calendário salvo.

O evento é único por transição observada, não um laço de compensação por dias pulados. Consumidores futuros podem assinar o evento; cultivos, animais, pagamentos, fases PRE/MAIN/POST de blocos e pausa sem jogadores não foram implementados nesta fatia. Alterações vanilla ou de terceiros executadas dentro de um tick podem gerar uma transição; a supressão administrativa explícita cobre os comandos HF. Não se promete transação durável ou execução exatamente uma vez em caso de crash de futuros consumidores.

Cinco testes novos cobrem transição natural sem repetição, salto de sono na virada do ano, ajuste administrativo, pausa/recuo/reabertura, independência de titulares e ausência de laço por dias pulados. São testes do detector; a verificação manual de sono e eventos reais foi posteriormente confirmada pelo usuário. Não foi repetida automação de interface, conforme preferência do usuário.

### Roteiro de verificação manual

Abrir um mundo de teste com comandos e acompanhar `run/logs/latest.log`, procurando `Harvest Festival daily update:`. A linha é diagnóstico interno, não mensagem no HUD.

1. Executar `/hf newDay`: esperar exatamente uma linha nova. Aguardar alguns segundos: não deve repetir.
2. Executar `/hf day 15`, `/hf season winter`, `/hf year 7`: não devem gerar linhas por si mesmos. Evitar executar justamente na virada natural do dia.
3. Executar `/hf day 30` e `/hf newDay`: uma linha nova; `/hf day` deve indicar primavera do ano seguinte.
4. Dormir numa cama durante a noite: uma linha nova pela passagem ao dia seguinte.
5. Salvar/sair e reabrir: nenhuma linha nova apenas pela entrada. Pausar o relógio também não deve gerar repetição.

Validação manual aprovada pelo usuário: todos os passos do roteiro funcionaram como descrito. O usuário também confirmou build bem-sucedido após executar runClient. Fatia encerrada; isso não amplia a cobertura para servidor dedicado ou multiplayer simultâneo.

## Fase 2 — sincronização individual do ouro

Quarta fatia aprovada pelo usuário e implementada em 02/11/2026. O original `StatsServer.syncGold` envia `PacketSyncGold` apenas ao jogador proprietário, com um long de 64 bits; o cliente atualiza seus próprios dados. O port usa `AttachmentType.Builder.sync((holder, recipient) -> holder == recipient, ByteBufCodecs.LONG)`, mantendo o codec persistente e `copyOnDeath`. Não há pacote próprio nem mensagem cliente → servidor para alterar ouro.

APIs verificadas nas fontes locais de NeoForge 26.2.0.93 e Minecraft correspondente: `AttachmentType.Builder.sync`, `AttachmentSync.syncEntityUpdate`, `syncInitialPlayerAttachments` e `ByteBufCodecs.LONG`. O caminho de atualização inclui o próprio jogador entre os destinatários e aplica o filtro; o envio inicial também usa o filtro. `PlayerList` chama a sincronização inicial ao entrar e renascer; `ServerPlayer` a chama após troca de dimensão. Um attachment ainda ausente conserva o padrão zero em ambos os lados.

`PlayerGold.get(Player)` disponibiliza a leitura do espelho ao futuro HUD. Alterações pela API do mod continuam restritas a `ServerPlayer`. Ler o attachment de outro jogador no cliente não fornece seu saldo real. Nenhuma classe exclusiva do cliente foi importada pelo código comum.

### Validação informada pelo responsável — 04/11/2026

Fatia concluída: sincronização de saldo de ouro com o jogador proprietário. Validação manual, no jogo, pelo responsável, com runClient em mundo single player.

Cenários validados: `/hf gold set` e `/hf gold add`, com valor consultado por `/hf gold get`; renascer após a morte; atravessar portal e voltar; sair e reabrir o mundo.

Cenários pendentes: servidor dedicado, dois jogadores e reconexão. A consulta administrativa verifica o saldo no servidor; o relato não inclui inspeção direta do espelho no cliente ou do tráfego de rede. A conclusão da fatia registra o aceite do responsável, sem ampliar a cobertura dos cenários relatados.

- Build (última linha de saída fornecida pelo responsável): **não informado**.
- Testes automatizados (quantidade informada pelo responsável): **não informado**.

### Planejamento anterior — remessas e pagamento diário (histórico)

Escopo definido: depósito de produtos, fila persistente e crédito sem duplicação. Critérios: a fila sobrevive a salvar e reabrir o mundo; cada item é creditado uma única vez. Fora do escopo: preços por tags e aniversário. Nenhuma implementação de remessas foi realizada nesta atualização documental.

Permanecem pendentes: aniversário (definição conforme o original, persistência e sincronização), preços (registros por dados/tags e consulta de valores) e validação integrada (servidor dedicado, dois jogadores, permissões, reconexão e persistência).

Naquele planejamento, a lista de leitura estava em STATUS.md. O evento diário está em `src/main/java/harvestfestival/calendar/`, junto de `DailyUpdates` e `DailyTransition`; `src/main/java/harvestfestival/daily/` não existe. A implementação abaixo consumiu o evento existente, sem mudança de pacote.

## Fase 2 — remessas e pagamento diário implementados (04/11/2026)

### Comportamento desta fatia

- O usuário autorizou explicitamente preço gravado no próprio item para testes anteriores ao sistema por tags. Usa `minecraft:custom_data` com `SellValue` numérico positivo, equivalente ao override original. Itens vazios, sem preço, com zero ou valor negativo são recusados sem consumo. Não existe catálogo de produtos, ID fixo de produto vendável ou registro de preços nesta implementação.
- Referências consultadas: `BlockStorage.hasShippedItem/onBlockActivated`, `TileShipping`, `ShippingRegistry.getSellValue`, `TrackingServer.addForShipping/newDay`, `PlayerTrackerServer.newDay`, `NewDayHandler` e `EntityHelper.getPlayerFromUUID`. O depósito manual original envia uma unidade para quem interage, independentemente do dono da caixa; fixa o preço no depósito. O port preserva esse caminho, incluindo consumir uma unidade no criativo. Agachar não deposita.
- `harvestfestival:shipping_box` é um bloco simples, sem tela ou entidade de bloco. Não armazena a fila no chunk: o attachment `harvestfestival:shipping_queue` pertence ao jogador e usa `copyOnDeath`. Quebrar a caixa ou descarregar seu chunk não elimina as remessas. Modelo provisório usa textura vanilla de barril; tag `minecraft:mineable/axe` e loot table em JSON. Disponível por `/give`, sem receita nesta fatia.
- Cada entrada contém cópia de uma unidade do item, incluindo componentes, e valor `long` fixado no depósito. A fila usa snapshots imutáveis; alterar o item restante não muda o preço aceito. A fila é consumida no servidor pelo evento diário existente, e a soma usa `PlayerGold.addTo`, também utilizado por `PlayerGold.add`; não há segundo saldo nem segunda implementação da aritmética. Preservados os valores negativos e overflow de `long` do saldo original.
- O consumidor percorre jogadores conectados. Sem jogador conectado, sua fila permanece salva e aguarda um novo evento diário com ele presente; entrar ou reabrir o mundo não dispara pagamento por si só. Não foram adicionados pagamentos retroativos, manipulação de arquivos de jogadores offline nem mudança no detector diário.
- Após consumir a fila, eventos repetidos encontram-na vazia e não pagam novamente os mesmos itens. A garantia é por remessa consumida, não um bloqueio global por data: itens novos entram numa nova fila. Ouro e fila são serializados no mesmo NBT de jogador, e a atualização síncrona ocorre na thread do servidor sem salvamento intermediário. Isso cobre salvamento normal e reabertura; não é promessa de transação durável contra crash, falha de disco ou exceções de outros mods.
- Sem novos pacotes, mixins, integrações, HUD, telas, cultivos, animais, aniversário ou preços por tags. Outros caminhos originais (funis, itens lançados e cesta), proprietário da caixa para automação e histórico de remessas permanecem fora desta implementação manual.

### APIs verificadas e testes

Fontes locais exatas: `minecraft-patched-26.2.0.93-sources.jar` e `neoforge-26.2.0.93-sources.jar`. Conferidos `BlockBehaviour.useItemOn`, `DeferredRegister.Blocks.registerBlock`, `Items.registerSimpleBlockItem`, `ItemStack.CODEC/copyWithCount`, `DataComponents.CUSTOM_DATA`, `CustomData.copyTag`, `CompoundTag.getLongOr`, `ExtraCodecs.POSITIVE_LONG`, `AttachmentType.Builder.serialize/copyOnDeath`, `TagValueOutput.createWithContext` e `RegistryAccess.fromRegistryOfRegistries`. Compilação confirma as chamadas usadas. O codec inicialmente tentado `Codec.longRange` não existe nesta versão e foi substituído pelo `ExtraCodecs.POSITIVE_LONG` verificado.

`PlayerShippingTest` acrescenta seis testes: depósito unitário e isolamento da cópia/preço; rejeição sem consumo; persistência da fila e saldo em arquivo NBT comprimido; pagamentos repetidos antes/depois de nova leitura; isolamento entre titulares e fila ausente; reutilização da aritmética de ouro com overflow. Os testes exercitam as funções usadas pelo depósito e pelo consumidor diário, usando os attachments registrados. Não simulam interação de cliente nem publicam um evento com servidor real.

O launcher unitário carrega os registros, mas não vincula os componentes de itens de um mundo. A tentativa de inicializar todos os componentes via dados vanilla encontrou validação NeoForge em `HolderSet$1`; o teste final prepara somente o graveto de teste com `MAX_STACK_SIZE=64` por `Holder.Reference.bindComponents`. Isso fica exclusivamente em `src/test`, sem alteração da inicialização do jogo. O codec real de `ItemStack`, os componentes personalizados e a serialização dos attachments continuam exercitados.

Build da implementação aprovado com 19 testes, zero falhas e zero erros. Resultado: `BUILD SUCCESSFUL in 13s`. Última linha literal da saída: `Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html`. O cache Gradle desta sessão estava em `C:\gradle-home`, e seu acesso exigiu execução autorizada fora da sandbox; não foi alterada configuração global. Procedimento de Java/TEMP/TMP/COMPAT descrito acima continua válido. O agente não executou `runClient` na implementação; a validação manual posterior pelo responsável está registrada abaixo.

### Validação informada pelo responsável — 05/11/2026

Fatia de remessas e pagamento concluída. Validação manual no jogo pelo responsável, com `runClient` em mundo single player:

- `/hf gold set`, caixa colocada no chão, graveto validado e valor consultado por `/hf gold get`.
- Valor mantido após morte e destruição da caixa.
- Sair e reabrir o mundo.
- `/hf newDay` resultou em valores corretos e não duplicados, inclusive após sair e entrar no mundo.

O registro reproduz os cenários relatados, sem atribuir inspeção de rede ou testes multiplayer ao responsável. A saída de um novo build após essa validação não foi informada; o build e os 19 testes anteriores permanecem registrados como evidência da implementação.

Próxima fatia: **preços de cultivos**, apenas registrada, sem início de implementação. Permanecem pendentes: aniversário (definição conforme o original, persistência e sincronização) e validação integrada (servidor dedicado, dois jogadores, permissões, reconexão e persistência). Sair/reabrir single player não encerra a pendência de reconexão multiplayer.

### Roteiro manual de remessas

1. Executar `./gradlew.bat runClient` com o ambiente Windows documentado; abrir um mundo de teste com comandos.
2. Executar `/give @s harvestfestival:shipping_box` e colocar a caixa.
3. Executar `/hf gold set 100` e `/give @s minecraft:stick[minecraft:custom_data={SellValue:25L}] 3`.
4. Sem agachar, clicar uma vez na caixa com os gravetos. Devem restar dois; `/hf gold get` ainda deve mostrar 100. Um graveto comum sem preço não deve ser consumido.
5. Salvar/sair antes da virada e reabrir: o saldo ainda deve ser 100. Executar `/hf newDay` e aguardar o relógio avançar; consultar saldo 125.
6. Salvar/sair e reabrir novamente; executar outro `/hf newDay` sem depositar: saldo continua 125. A repetição direta da rotina de pagamento é coberta pelo teste automatizado.
7. Depositar os dois gravetos restantes em dois cliques e avançar o dia: saldo 175. Opcionalmente quebrar a caixa depois do depósito para verificar que a fila pertence ao jogador.

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

## Fase 2 — sétima fatia, base de aniversários (07/11/2026)

Plano aprovado pelo responsável, incluindo `/hf npcBirthday <id> get` e `/hf npcBirthday <id> test <season> <day>`. Nenhum pacote novo, entidade, registro de NPC, efeito de gameplay, mixin ou integração foi acrescentado. Não há consumidor de `NewDayEvent` para aniversários.

### Original e decisões do responsável

No original, `StatsServer.setBirthday` copia a data corrente quando o aniversário ainda é nulo; `SyncHandler.onPlayerLogin` chama essa rotina na entrada. A data inclui dia, estação e ano, persiste em `Birthday` e chega somente ao titular por `PacketSyncBirthday`/`StatsClient`. `PlayerTrackerServer` possui disparo `birthday` quando o ano corrente é maior que 1; `GreetingTime` consulta anos decorridos. Esses efeitos e a atribuição automática não foram portados.

NPCs já têm aniversários no original: `HFNPCs.register` recebe dia/estação e constrói `CalendarDate` com ano padrão 1; `NPC` guarda essa data. `CalendarHelper.isDateSame` compara somente dia/estação. `ContainerNPCGift` multiplica pontos por 10 no aniversário, sujeito à aceitação do presente e ao marcador de relacionamento `BIRTHDAY_GIFT`. A cidade também possui uma data de fundação chamada `birthday` em `TownDataServer`, utilizada no cálculo de anos. Relações, presentes, cidade e seus efeitos não integram esta fatia.

**Extensões/decisões do responsável:** tabela independente de aniversários de NPCs por identificador e datapack (sem entidade ou catálogo); jogador inicialmente sem data e sem atribuição na entrada; representação anual sem ano; definição/limpeza exclusivamente administrativas nesta fatia. A existência de aniversários de NPCs é original; a nova base orientada a JSON é a extensão.

### Formato persistido e API

`BirthdayDate(Season, int)` reutiliza `Season` e a validação de `CalendarDate.of`, com dias de 1 a `CalendarDate.DAYS_PER_SEASON` (30). O codec valida estação/dia; a comparação anual ignora o ano de `CalendarDate`.

Attachment `harvestfestival:birthday`, dentro dos attachments do jogador: composto com campo opcional `date`, cujo valor contém `season` (spring/summer/autumn/winter) e `day` inteiro. Ausência é representada por attachment inexistente ou composto sem `date`; não há data sentinela nem ano artificial persistido. `clear` grava `Optional.empty()` pelo `setData`, permitindo sincronizar a limpeza. `copyOnDeath()` conserva o estado no renascimento.

`PlayerBirthday.get(Player)` lê também o espelho cliente. `set(ServerPlayer, BirthdayDate)` e `clear(ServerPlayer)` alteram o dado no servidor. **A futura escolha pelo jogador deverá chamar essa mesma API, após validar sua própria autorização, e usar o mesmo attachment/formato; não deve criar outro armazenamento nem depender do comando administrativo.** Nenhum mecanismo de escolha foi incluído agora.

Os comandos seguem o estilo do ouro: `/hf birthday [player] get`, `set <season> <day>` e `clear`; alvo omitido usa o executor jogador, console informa jogador conectado, nomes e seletores de jogador único são aceitos. Todos os comandos desta fatia exigem `LEVEL_GAMEMASTERS`. Dias inválidos são recusados, sem clamp; estações no comando aceitam maiúsculas/minúsculas.

### Regras de NPC por datapack

Uma regra por `data/harvestfestival/birthdays/<namespace>/<caminho>.json`, com `{"season":"spring","day":15}`. Estação JSON usa os nomes minúsculos e dia deve ser inteiro de 1 a 30. O caminho identifica o titular, sem exigir registro de NPC. O único exemplo incluído é **de teste**: `harvestfestival:npc/test_example`, primavera 15, no arquivo `data/harvestfestival/birthdays/harvestfestival/npc/test_example.json`.

`NpcBirthdays.get(Identifier)` devolve data opcional; `isBirthday(Identifier, CalendarDate)` é consulta pura e devolve falso para ID ausente. A tabela é somente do servidor, sem sincronização de catálogo. A recarga prepara uma tabela nova e publica um mapa imutável inteiro. Vale o arquivo de maior precedência por caminho. Arquivo efetivo inválido é descartado com aviso `Ignoring NPC birthday <caminho>`; **não** recupera a regra inferior. Demais arquivos continuam; remoções e alterações aparecem na recarga seguinte.

### APIs conferidas e validação automatizada

Fontes locais `neoforge-26.2.0.93-sources.jar` e `minecraft-patched-26.2.0.93-sources.jar`: `AttachmentType.Builder.serialize(MapCodec)`, `copyOnDeath`, `sync(BiPredicate, StreamCodec)`, cópia em `AttachmentInternals`, `ByteBufCodecs.fromCodec/optional`, `IdentifierArgument.id/getId`, `Commands.hasPermission(LEVEL_GAMEMASTERS)`, `AddServerReloadListenersEvent`/`addListener`, `SimplePreparableReloadListener.prepare/apply` e `ResourceManager.listResources`. `FallbackResourceManager` e `MultiPackResourceManager` resolvem precedência. A compilação confirmou as chamadas utilizadas.

Onze testes novos: dois de data (limites/codec e comparação anual), cinco de jogador (ausência inicial, definir/limpar e reler NBT comprimido, isolamento, cópia na morte e codificação de rede), quatro de tabela (inválidos junto de válida, consultas, precedência/remoção entre recargas e arquivo superior inválido sem fallback). Os testes de precedência usam `PathPackResources` e `MultiPackResourceManager` reais sobre diretórios temporários. O teste de morte usa reflexão exclusivamente em `src/test` para executar a rotina privada de cópia de titulares do NeoForge com seu flag `copyOnDeath`, sem construir servidor ou simular morte no jogo.

**Sincronização com cliente verificada somente por teste de codificação de rede**, cobrindo data e ausência. O filtro de envio usa `holder == recipient`, como ouro. Não houve inspeção do espelho cliente nem tráfego real. A verificação real permanece pendente na validação integrada. Consultas administrativas abaixo verificam o estado no servidor.

Build em 07/11/2026: `BUILD SUCCESSFUL in 28s`; **37 testes**, zero falhas, erros ou ignorados, conferidos nos XML de `build/test-results/test`. Executado com Java/TEMP/TMP/COMPAT documentados e acesso ao cache `C:\gradle-home`. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

### Roteiro runClient — jogador

Roteiro reproduzível; resultados observados na intervenção posterior estão registrados ao final desta seção. Execute `.\gradlew.bat runClient` com o ambiente Windows documentado e abra um mundo single player de teste com comandos habilitados.

1. `/hf birthday get`: deve informar ausência, inclusive em jogador preexistente sem o attachment.
2. `/hf birthday set spring 15` e `/hf birthday @s get`: devem mostrar primavera 15. `/hf birthday @s set winter 30` deve mostrar inverno 30.
3. Tente `/hf birthday set winter 0`, `/hf birthday set winter 31` e `/hf birthday set unknown 10`: devem falhar. `/hf birthday get` deve continuar inverno 30.
4. Salve/saia, reabra o mundo e consulte: inverno 30. Execute `/kill @s`, renasça e consulte novamente: inverno 30.
5. `/hf birthday clear` e `/hf birthday get`: ausência. Salve/saia e reabra: continua ausente. Defina novamente primavera 15 para confirmar que o dado continua editável.

### Roteiro runClient — NPC (sem entidade)

Use o mesmo mundo de teste, apenas com o mod e o datapack abaixo.

1. `/hf npcBirthday harvestfestival:npc/test_example get`: primavera 15. `/hf npcBirthday harvestfestival:npc/test_example test spring 15`: `true`; repita com `spring 16`: `false`. Um ID ausente, como `harvestfestival:npc/missing`, deve informar ausência no `get`.
2. Crie `run/saves/<pasta do mundo>/datapacks/hf_aniversarios_teste/pack.mcmeta` em UTF-8, sem extensão `.txt`, com:

```json
{"pack":{"description":"Teste de aniversários","min_format":[107,1],"max_format":[107,1]}}
```

3. Na mesma pasta do pack, crie `data/harvestfestival/birthdays/harvestfestival/npc/test_example.json` com o conteúdo abaixo. O nome da pasta é **`data`, sem ponto inicial**; `.data` não é reconhecida como raiz dos recursos, mesmo se o pack aparecer habilitado.

```json
{"season":"winter","day":30}
```

4. Execute `/reload` e aguarde. Confira `/datapack list enabled`; se necessário, use `/datapack enable "file/hf_aniversarios_teste" last`. `/hf npcBirthday harvestfestival:npc/test_example get` deve mostrar inverno 30. `test winter 30` deve retornar `true`; `test spring 15`, `false`.
5. **Sobrescrita inválida:** substitua o conteúdo desse mesmo arquivo por `{"season":"winter","day":31}`. Execute `/reload`. O `get` do exemplo deve informar ausência; `test winter 30` e `test spring 15` devem retornar `false`. Confira `run/logs/latest.log`: aviso `Ignoring NPC birthday` contendo `harvestfestival:birthdays/harvestfestival/npc/test_example.json`. Isso confirma descarte da regra, sem retorno à primavera 15 do pack inferior. Repita com JSON malformado contendo apenas `{`: mesmo resultado e novo aviso após `/reload`.
6. Mantendo o exemplo inválido, crie no pack `data/harvestfestival/birthdays/harvestfestival/npc/test_sibling.json` com `{"season":"summer","day":2}`. `/reload`; consulte `/hf npcBirthday harvestfestival:npc/test_sibling get`: verão 2, enquanto o exemplo continua ausente.
7. Execute `/datapack disable "file/hf_aniversarios_teste"` e `/reload`: exemplo volta a primavera 15 e `test_sibling` fica ausente. Isso confirma reconstrução da tabela sem reter regras removidas.

Servidor dedicado, multiplayer, reconexão e inspeção real do espelho cliente continuam pendentes. Salvar/reabrir single player não encerra a pendência de reconexão multiplayer.

### Correção e validação real — 07 e 08/11/2026

O responsável relatou saída `1`/`0` no comando `test` e ausência de efeito do datapack. A intervenção do agente em 07/11/2026 confirmou duas causas distintas:

- **Código:** argumento booleano de `Component.translatable` era convertido em valor numérico na serialização de rede. `NpcBirthdayCommands.testResult` agora usa `Boolean.toString(matches)`. O retorno numérico Brigadier continua 1/0; a mensagem visível passa a usar `true`/`false`. `NpcBirthdayCommandsTest` exercita a mensagem de produção por `ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC` e verifica ambas as strings após decodificar. Build da correção: `BUILD SUCCESSFUL in 15s`.
- **Datapack local:** `hf_aniversarios_teste` estava habilitado, mas seus recursos estavam sob `.data`. A tentativa de renomear foi negada pelo sistema; criou-se `data` e copiou-se o JSON, preservando `.data`. Não houve mudança no listener para aceitar caminhos incorretos.

No mundo single player `Anniversary test`, com cliente reiniciado pelo agente via `runClient`, foram observados:

| Caso | Resultado |
| --- | --- |
| Pack válido com inverno 30 | `test winter 30` mostrou `true`; `test spring 15` mostrou `false`. |
| Arquivo superior com dia 31 e `/reload` | `get` informou ausência; aviso em `latest.log` identificou `harvestfestival:birthdays/harvestfestival/npc/test_example.json` e limite 1–30. |
| Regra válida ao lado da inválida | `test_sibling get` mostrou verão 2. |
| Arquivo superior contendo apenas `{` e `/reload` | Aviso de JSON malformado com caminho; `test spring 15` mostrou `false`, sem recuperar a regra inferior. |
| Desativação do pack | A recarga decorrente do comando restaurou primavera 15 no `get` do exemplo. |
| Jogador sem data | `get` informou ausência. |
| `/hf birthday @s set winter 30` | Data definida; tentativa de dia 31 foi recusada. |
| `/kill @s` e renascimento | `get` manteve inverno 30. |
| Salvar/sair e reabrir com data | `get` manteve inverno 30. |
| `/hf birthday clear` | Informou ausência; agente salvou e voltou ao menu principal. |
| Reabrir após limpeza | **Responsável confirmou em 08/11/2026**: `/hf birthday get` no mundo de aniversário manteve ausência. |

Não se atribui execução manual a variações não observadas (por exemplo, dia 0, estação desconhecida e consulta de `test_sibling` após desativação). A cobertura automatizada dos limites e remoção entre recargas permanece registrada acima. Os cenários observados encerram a validação single player desta fatia conforme confirmação do responsável, sem antecipar validação integrada.

Estado local deixado pela intervenção: pack de aniversário desativado; `data/harvestfestival/birthdays/harvestfestival/npc/test_example.json` restaurado para inverno 30 e `test_sibling.json` com verão 2; `.data` original preservada. Esses arquivos e o mundo ficam em `run/saves/Anniversary test/`, fora dos entregáveis de código. O aniversário do jogador terminou ausente. Nenhum comando Git foi executado.

O recebimento da mensagem administrativa correta **não é inspeção do espelho cliente do attachment**. A sincronização desse dado continua verificada somente por teste de codificação de rede; servidor dedicado, multiplayer, reconexão, permissões em execução e espelho cliente seguem pendentes.

Build do encerramento documental em 08/11/2026: `BUILD SUCCESSFUL in 7s`. Relatórios XML: **38 testes**, zero falhas, erros ou ignorados, incluindo a regressão da mensagem. A tarefa `test` estava `UP-TO-DATE`, sem nova execução dos testes neste build. Comando: `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=COMPAT' --console=plain`, com o ambiente Windows documentado. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

## Fase 2 — oitava fatia: validação dedicada e multiplayer (08/11/2026)

Plano e EULA local aprovados pelo responsável. Apenas configuração de execução e documentação alteradas; nenhuma feature ou correção de gameplay implementada. Nenhum comando Git executado. Fase 2 permanece aberta até decisão explícita do responsável.

### Configuração conferida

- `runServer`: `run/server`, sem GUI, heap inicial 512 MiB e máximo 2 GiB. `runClient`: `run/client1`, HFTester1, 512 MiB–3 GiB. `runClient2`: `run/client2`, HFTester2, 512 MiB–3 GiB. Mundos anteriores em `run/saves` preservados.
- `RunModel.gameDirectory`, `programArgument`, `jvmArgument` e `RunGameTask extends JavaExec` conferidos nas fontes locais de ModDevGradle 2.0.148. O padrão de JavaExec não encaminhava a entrada do console: `tasks.named('runServer', JavaExec) { standardInput = System.in }` habilita os comandos. É configuração de run, sem alteração do mod.
- Servidor local: `server-ip=127.0.0.1`, porta 25565, `online-mode=false`, dois jogadores, `op-permission-level=2`, mundo `hf_multiplayer_validation`, distância de visão 6, simulação 4, proteção de spawn 0 para permitir o teste de depósito sem operador. Sem RCON. EULA aceita explicitamente pelo responsável.
- Permissão na fonte exata `minecraft-patched-26.2.0.93-sources.jar`: `DedicatedServerProperties.deserializePermission("2")` usa `PermissionLevel.byId(2)` = `GAMEMASTERS`; `LevelBasedPermissionSet.hasPermission` compara nível igual ou superior; `Commands.LEVEL_GAMEMASTERS` exige `Permissions.COMMANDS_GAMEMASTER`. `PlayerList.op` usa por padrão `server.operatorUserPermissions()`. Portanto nível 2 satisfaz os três comandos HF. Isso não substitui a validação de rejeição/aceitação em execução.
- Formato dos packs comparado diretamente aos arquivos locais das fatias 6 e 7 (`Tags test` e `Anniversary test`): ambos usam `min_format:[107,1]` e `max_format:[107,1]`. Mesmo formato neste teste, sem divergência; não se usa número decimal `pack_format:107.1`.
- Máquina: cerca de 31,2 GiB físicos, 15,5 GiB livres antes dos jogos. Máximos dos três heaps de jogo: 8 GiB; mais até 3 GiB dos daemons Gradle de uso único, além de memória nativa/launchers. Após a primeira abertura dos três, cerca de 10,9 GiB livres. Gradle limitado a dois workers e 1 GiB por execução; `gradle.properties` não foi modificado.
- Inícios sequenciais: servidor em `Done` antes do primeiro cliente; primeiro cliente observado pelo agente no menu antes de `runClient2`. O segundo falhou nativamente em `glfw.dll+0x10fa1`, `glfwPollEvents`, `EXCEPTION_ACCESS_VIOLATION`, às 11:34:55. Relatório preservado em `run/client2/hs_err_pid15572.log`, primeira saída Gradle em `run/validation/client2-first-attempt.log`. Reiniciado sem mudança de código; responsável relatou que aparentemente chegou ao menu. Não atribuir a causa ao mod nem à automação sem investigação adicional.
- Não há depurador preparado para os processos atuais: configurações locais de IDE ainda apontam ao caminho antigo, e as runs usadas não ativam JDWP. Cenário 8 fica obrigatoriamente pendente até o primeiro consumidor cliente (HUD/interface), sem implementar esse consumidor nesta fatia. Mensagens administrativas e testes de codec não comprovam o espelho nem o isolamento real no cliente.

### Comandos de inicialização

Em cada terminal PowerShell, a partir da raiz:

```powershell
Set-Location 'C:\dev\harvest-festival-port'
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:GRADLE_USER_HOME = 'C:/gradle-home'
New-Item -ItemType Directory -Path build/tmp -Force | Out-Null
$env:TEMP = (Resolve-Path build/tmp).Path
$env:TMP = $env:TEMP
```

Primeiro terminal: aguardar `Done` antes de iniciar o próximo. Não executar as três tarefas no mesmo comando Gradle.

```powershell
.\gradlew.bat runServer '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain
```

Segundo terminal: aguardar o menu principal (na primeira abertura, botão Continue da tela de acessibilidade).

```powershell
.\gradlew.bat runClient '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain
```

Terceiro terminal, somente depois do menu do primeiro:

```powershell
.\gradlew.bat runClient2 '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain
```

Nos dois clientes, Multiplayer → Direct Connection → `127.0.0.1:25565`. Manter nomes HFTester1/HFTester2 nas reconexões (identidades offline). O console do servidor recebe comandos sem `/`; clientes usam `/`.

### Roteiro integrado completo

1. **Subida:** conferir `Harvest Festival loaded with persistent player gold and the world calendar.`, `Done`, ausência de ERROR/FATAL; executar `list`, `hf day`, `hf npcBirthday harvestfestival:npc/test_example get`. Avisos vanilla sobre ambiguidades e aviso do modo offline devem ser registrados separadamente.
2. **Conexões:** ambos conectam; `list` precisa mostrar os dois nomes simultâneos. Registrar entradas e UUIDs efetivos do log. No console: `op HFTester1`; manter HFTester2 sem operador até terminar o passo 4.
3. **Isolamento:** pausar relógio com `time pause`. No console:

```text
hf gold HFTester1 set 100
hf gold HFTester2 set 200
hf birthday HFTester1 set spring 5
hf birthday HFTester2 set winter 20
hf gold HFTester1 get
hf gold HFTester2 get
hf birthday HFTester1 get
hf birthday HFTester2 get
give HFTester1 harvestfestival:shipping_box
give HFTester1 minecraft:stick[minecraft:custom_data={SellValue:25L}] 2
give HFTester2 minecraft:stick[minecraft:custom_data={SellValue:40L}] 2
```

HFTester1 coloca a caixa; ambos a usam, um clique cada, sem agachar. Deve restar um graveto para cada um, com saldo ainda 100/200. O depósito é do jogador que interage, mesmo em caixa compartilhada. Se necessário, usar `tp HFTester2 HFTester1` no console. Verificação de fila online somente leitura: `data get entity HFTester1 "neoforge:attachments"` e equivalente para HFTester2; confirmar a exposição dessa chave em execução antes de alegar inspeção. Se indisponível, registrar a limitação e comprovar persistência/isolamento pelo pagamento individual dos passos 5 e 6, sem criar comando novo.

4. **Permissões:** no cliente HFTester2, sem operador, tentar:

```text
/hf gold get
/hf gold set 999
/hf gold add 1
/hf gold HFTester1 get
/hf birthday get
/hf birthday set summer 2
/hf birthday clear
/hf birthday HFTester1 get
/hf npcBirthday harvestfestival:npc/test_example get
/hf npcBirthday harvestfestival:npc/test_example test spring 15
```

Todos devem ser negados (a árvore pode aparecer como comando desconhecido/incompleto). Registrar mensagens e conferir no console que ouro/datas não mudaram. HFTester1 deve conseguir os mesmos verbos; usar `add 0`, redefinir os valores esperados e, após testar `clear`, restaurar primavera 5. Conferir `ops.json` efetivo no servidor. Só depois promover HFTester2 com `op HFTester2` para consultas dos passos seguintes. Nunca tratar sucesso do console como prova da permissão do jogador.

5. **Reconexão:** com relógio pausado e filas pendentes, cada jogador sai e entra novamente. Ouro 100/200, datas primavera 5/inverno 20; filas preservadas (inspeção se disponível e pagamento do passo 6). Registrar desconexões/entradas no log. Após pagamento, repetir reconexão para conferir que o ouro não muda e a fila não reaparece.
6. **Calendário/pagamento:** os dois jogadores consultam `/hf day` com relógio pausado: mesma data. No console, `hf newDay`, depois `time resume`; aguardar uma linha `Harvest Festival daily update:` e executar `time pause`. Esperado: ouro 125/240, mesma nova data nos dois clientes e apenas uma atualização para essa virada. Avançar outro dia sem depósitos: saldos permanecem 125/240. Consultas administrativas mostram a data do servidor, não constituem inspeção de espelho cliente. Não usar `hf day/season/year` como se disparassem pagamento: esses ajustes reposicionam a referência sem simular dias.
7. **Offline:** HFTester2 deposita seu segundo graveto (40), sai; HFTester1 permanece. Antes de ler qualquer arquivo do jogador offline, executar `save-all` e aguardar `Saved the game`. Guardar leitura somente leitura de `players/data/<UUID>.dat`, identificando UUID pelo log real. Avançar um dia com HFTester2 ausente, repetir `save-all` antes da nova leitura. Reconectar HFTester2 com relógio pausado, consultar saldo/fila; depois avançar novo dia com ele presente. Registrar pagamento na virada offline, reconexão, virada posterior ou ausência de pagamento durante o intervalo observado. A leitura estática prevê 240 ao reconectar e 280 após virada conectado; isso NÃO é resultado observado. Não corrigir: o responsável decidirá se aceita o comportamento ou autoriza correção, mesmo havendo diferença da referência.
8. **Espelho:** não executado nesta preparação, pendência obrigatória para primeiro consumidor cliente. Nenhum HUD, interface, comando cliente ou instrumentação nova será acrescentado. Quando houver consumidor, verificar no cliente titular ouro e aniversário, atualização/limpeza/reconexão e ausência dos valores reais no outro cliente; resultados administrativos e codec não substituem essa verificação.
9. **Dados por datapack:** roteiro abaixo. Ao terminar, salvar, desconectar clientes e usar `stop` no console; conferir salvamento e término normal. Manter as evidências locais.

### Datapack dedicado e roteiro de recarga

Raiz: `run/server/hf_multiplayer_validation/datapacks/hf_integrated_validation/`. `pack.mcmeta`:

```json
{"pack":{"description":"Validação integrada da Fase 2","min_format":[107,1],"max_format":[107,1]}}
```

Arquivos relativos à raiz (pasta `data`, sem ponto):

| Caminho | JSON válido |
| --- | --- |
| `data/harvestfestival/shipping_prices/tags/c/crops/wheat.json` | `{"price":140,"priority":0}` |
| `data/harvestfestival/birthdays/harvestfestival/npc/test_example.json` | `{"season":"winter","day":30}` |
| `data/harvestfestival/birthdays/harvestfestival/npc/test_sibling.json` | `{"season":"summer","day":2}` |

Preparação deixada com pack desativado e os três arquivos válidos. Os arquivos inválidos usados na prova de console foram retirados da raiz de recursos e preservados em `run/validation/inexistente.json.invalid` e `stick.json.invalid`.

1. Confirmar `datapack list enabled` sem o pack, relógio pausado e filas anteriores vazias. Zerar ouro de HFTester1, entregar dois trigos (`give HFTester1 minecraft:wheat 2`). Ele deposita um trigo, pelo valor base 100.
2. `datapack enable "file/hf_integrated_validation" last`, `reload`, aguardar conclusão; conferir `datapack list enabled`.
3. `hf npcBirthday harvestfestival:npc/test_example get`: inverno 30; `test winter 30`: true; `test spring 15`: false; sibling: verão 2.
4. Depositar segundo trigo; avançar dia com o procedimento do passo 6: ouro 240. Nova virada sem depósito: continua 240. Confirma preço congelado no depósito anterior à recarga.
5. Criar `data/harvestfestival/shipping_prices/items/minecraft/inexistente.json` com `{"price":1}` e `stick.json` com apenas `{`. Alterar aniversário do exemplo para `{"season":"winter","day":31}`. Recarregar: registrar caminhos nos avisos, exemplo ausente e ambos os testes de data false; sibling continua verão 2. Graveto comum recusado; um novo trigo paga 140.
6. Exemplo de aniversário com apenas `{`: recarregar, confirmar ausência e aviso; sibling permanece válido.
7. Desativar pack e recarregar: exemplo primavera 15, sibling ausente. Novo trigo paga 100. Restaurar os arquivos válidos para futura repetição e retirar os dois recursos inválidos da raiz de dados.

### Evidências obtidas pelo agente, sem jogadores conectados

- Primeira subida às 11:31:59: `Done (2.118s)!`; log preservado em `run/validation/server-first-start.log`. A entrada de comandos não era encaminhada. Encerramento por Ctrl+C, sem alegação de `stop`/salvamento normal nessa primeira tentativa. Configuração de entrada ajustada; servidor reiniciado às 11:33:55: `Done (0.244s)!`.
- Às 11:33:57: `There are 0 of a max of 2 players online`, `Day 1 of Spring, year 1 (Monday).`, exemplo primavera 15, `Paused clock minecraft:overworld`. Nenhum ERROR/FATAL no log dedicado inspecionado.
- Às 11:34:25: `reload`; às 11:34:38: pack listado habilitado. Às 11:34:54: exemplo inverno 30, testes true/false e sibling verão 2.
- Às 11:40:19: recarga com dia 31, item inexistente e JSON de preço malformado gerou os três avisos esperados. Às 11:40:31: exemplo ausente, testes false/false, sibling verão 2.
- Às 11:40:42: recarga com JSON de aniversário malformado gerou aviso; às 11:40:59: exemplo ausente, sibling verão 2; pack desativado. Às 11:41:17: exemplo primavera 15, sibling ausente; `save-all` respondeu `Saved the game`.
- Isso confirma recarga e consultas de NPC no dedicado, além do descarte de regras de preço inválidas. NÃO comprova o preço pago em jogo, dois clientes conectados, permissão de jogador, reconexão, remessas offline ou espelho cliente.

### Build

Build inicial desta fatia: `BUILD SUCCESSFUL in 16s`, 38 testes efetivamente reexecutados, zero falhas, erros ou ignorados. Comando: `.\gradlew.bat build test --rerun '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`. Log `run/validation/build.log`; XML em `build/test-results/test`. A configuração posterior de encaminhamento do console exige build final, registrado no STATUS após execução.

### Atualização após conexão dos clientes e build final

Responsável conectou HFTester2 às 11:43:35 e HFTester1 às 11:43:49. Agente confirmou `list` às 11:44:17 com os dois nomes. Às 11:44:42, agente promoveu HFTester1; `ops.json` confirma somente esse nome, nível 2. Ouro 100/200 e datas primavera 5/inverno 20 definidos e lidos via comandos e `data get entity <nome> "neoforge:attachments"` (caminho confirmado em execução). Caixas/gravetos entregues; interações ainda aguardam relato. UUIDs efetivos em `run/server/usercache.json`: HFTester1 `e16bf07a-6192-33cf-9425-4ba7ef199f2c`; HFTester2 `f36f2709-9f98-3281-b688-378060965a89`.

Build final após ajuste de entrada do servidor: `BUILD SUCCESSFUL in 8s`, 38 testes nos XML, zero falhas/erros/ignorados; tarefa `test UP-TO-DATE`, reutilizando a execução de 16 segundos desta sessão. Comando `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`, saída em `run/validation/build-final.log`. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

Arquivos de configuração editados/criados manualmente nesta preparação: `build.gradle`, `docs/STATUS.md`, `docs/PORTING_NOTES.md`, `run/server/eula.txt`, `run/server/server.properties`, os quatro arquivos do pack (pack.mcmeta e três JSONs válidos), mais os dois recursos inválidos preservados fora do pack em `run/validation`. Logs, mundo, `ops.json`, cache de usuários, opções dos clientes e relatórios são saídas geradas pelas ferramentas/jogo, não código do mod. O servidor e os clientes permanecem abertos para a validação do responsável; relógio pausado, pack desativado. Não se declara encerramento normal enquanto a sessão continua.

Às 11:45:36, após os depósitos executados pelo responsável, `data get entity` confirmou filas individuais: HFTester1 uma entrada de graveto com `SellValue:25L` e `value:25L`, ouro 100, primavera 5; HFTester2 uma entrada com `SellValue:40L` e `value:40L`, ouro 200, inverno 20. Cenário 3 passou pela inspeção do estado real do servidor; persistência/reconexão e pagamento continuam cenários separados.

Inspeção adicional às 11:45:51: `data get entity <nome> Inventory` mostrou um graveto restante em cada jogador, confirmando consumo unitário. Nenhuma virada foi executada enquanto se aguardam os testes de permissão.

### Continuação autorizada ao agente — permissões e reconexão (08/11/2026)

O responsável autorizou operar os clientes e executar os testes diretamente, pedindo instruções apenas quando a automação não fosse viável. Nenhum comando Git ou alteração de gameplay.

- HFTester2 sem operador: log cliente registra rejeição de `hf gold get` às 11:45:56 (ação anterior do responsável), `hf birthday get` às 11:48:26 e `hf npcBirthday harvestfestival:npc/test_example get` antes de 11:49:06 (agente). Resposta: `Incorrect argument for command`.
- Após `op HFTester2` às 11:49:06, o agente executou essas três consultas no chat do cliente: NPC primavera 15, ouro 200 e aniversário inverno 20. Isso comprova rejeição/aceitação das três raízes com nível 2; não atribui execução às variações adicionais `set`, `add`, `clear`, `test` e alvo explícito do roteiro, ainda não exercitadas nesta comparação.
- Agente desconectou HFTester2 pelo menu às 11:50:38 e reconectou às 11:50:51. Console às 11:50:59 confirmou ouro 200, aniversário inverno 20 e fila com uma remessa de 40, sem pagamento pela entrada. Consulta `/hf day` no cliente mostrou primavera 1, ano 1, segunda-feira.
- Ao tentar ativar a janela do primeiro cliente, ele encerrou às 11:51:25 com a mesma falha nativa `glfw.dll+0x10fa1`, agora em `run/client1/hs_err_pid6548.log`. O servidor permaneceu ativo; houve ERROR de conexão `java.net.SocketException: Connection reset` no mesmo horário. Logo, a ausência de ERROR registrada no cenário 1 se refere à subida/intervalo inicial, não ao log completo após essa queda. Não há diagnóstico que atribua isso ao mod. Repetição temporal com ativação de janela é observação, não causa comprovada.
- Cliente 1 reiniciado sem mudanças; reconexão e consulta de data solicitadas ao responsável para evitar repetir a ativação problemática. `save-all` executado às 11:52:02 respondeu `Saved the game`.

HFTester1 reconectou pelo responsável às 11:52:29; leitura do agente às 11:52:41 confirmou ouro 100, primavera 5 e fila 25. O responsável informou primavera 1/ano 1 e o log cliente confirma às 11:52:35, igual à consulta do agente no segundo cliente às 11:51:18. Primeira virada pelo agente às 11:52:52 gerou uma atualização; às 11:53:07 ouro 125/240 e filas vazias. Segunda virada às 11:53:07, sem depósitos, gerou uma atualização; às 11:53:17 saldos ainda 125/240, primavera 3/ano 1. Relógio novamente pausado. Pagamento sem duplicação observado em execução; novas consultas nos dois clientes solicitadas antes do teste offline.

Consultas após duas viradas confirmadas em ambos os logs: HFTester1 às 11:53:36 e HFTester2 às 11:53:43 mostraram `Day 3 of Spring, year 1 (Wednesday)`. Cenário 6 passou.

### Retomada às 16:32 — cenário offline

A tentativa anterior de avançar o dia não foi executada: a revisão automática da ferramenta ficou indisponível por limite de uso, sem determinação de ação insegura. O último estado confirmado era save-all às 11:54:40. HFTester1 também saiu às 11:55:01 e o servidor pausou vazio às 11:56:01. Na retomada, `list` às 16:32:08 confirmou zero jogadores; solicitado conectar somente HFTester1, sem controle de interface pelo agente.

A leitura anterior falhou porque o caminho presumido `playerdata` não existe neste mundo. O caminho real, conferido em disco, é `players/data/<UUID>.dat`; roteiro corrigido. Após novo `save-all` às 16:32:23 e `Saved the game`, leitura somente leitura do NBT comprimido de HFTester2 confirmou ouro 240, inverno 20 e fila de uma remessa de 40. Evidência extraída em `run/validation/offline-before.json`. Nenhum arquivo de jogador foi modificado pelo leitor.

Virada offline executada às 16:34:38 com somente HFTester1 conectado (`list` confirmado). `DailyUpdates` registrou transição de elapsedDays 2 para 3. Às 16:34:51, relógio pausado e `save-all` concluído; nova leitura de HFTester2 mostrou os mesmos 240 de ouro, inverno 20 e remessa 40. Evidência `run/validation/offline-after.json`. Portanto não pagou durante essa virada offline. Reconexão e próxima virada conectado ainda necessárias para concluir a classificação.

Conclusão da observação offline: HFTester2 reconectou às 16:35:58, pelo responsável durante a preparação da automação. Consulta às 16:36:45 mostrou ouro 240 e fila 40; portanto a reconexão não pagou. Nova virada com ele conectado às 16:36:45; consulta às 16:36:59 confirmou ouro 280 e fila vazia. Cenário 7 registrado como passou quanto à observação solicitada, sem aceitar paridade ou alterar comportamento. Decisão de aceitar ou corrigir permanece exclusivamente com o responsável.

Às 16:37:30, HFTester1 caiu novamente durante tentativa de ativar sua janela; servidor registrou Connection reset e continuou funcionando. Nenhuma correção de código feita. Na continuação às 16:44, apenas HFTester2 conectado; teste final de preços será nele, preservando o saldo-base 280. Valores esperados: 520 após depósitos de 100+140 e 620 após novo depósito de 100 com pack desativado. Isso evita nova abertura/automação do cliente que caiu.

### Resultado consolidado — 08/11/2026, 16:46

Cenários 1–7 e 9 com observações concluídas; cenário 8 não executado e obrigatório no primeiro consumidor cliente. O resultado do cenário 7 não representa aceite da regra offline. Nenhuma correção de gameplay implementada; nenhum comando Git executado.

Teste final de preços no HFTester2, preservando saldo-base 280 e com fila inicialmente vazia:

| Horário | Ação e evidência |
| --- | --- |
| 16:44:52 | Pack desativado, saldo 280, fila vazia; três trigos entregues. |
| 16:45:24 | Após primeiro clique do responsável: fila de um trigo a 100; pack habilitado e reload concluído às 16:45:25. |
| 16:45:41 | NPC exemplo inverno 30; test winter 30 true. |
| 16:45:58 | Responsável informou dois cliques após recarga, em vez de um. NBT confirmou três entradas: 100, 140, 140. Expectativa adaptada de forma explícita: crédito 380, saldo 660; não registrar execução fictícia de total 240. Virada executada. |
| 16:46:09 | Saldo 660, fila vazia. Outra virada sem depósitos. |
| 16:46:16 | Saldo ainda 660; pack desativado, reload concluído às 16:46:17, um trigo adicional entregue. |
| 16:46:42 | Após clique do responsável: fila de um trigo a 100. NPC exemplo primavera 15, sibling ausente. Virada executada. |
| 16:46:49 | Saldo 760 e fila vazia. save-all concluído; stop encerrou normalmente, salvando jogadores e todas as dimensões. runServer terminou com BUILD SUCCESSFUL in 5h 13m 9s. |

A variação de quantidade mantém a verificação do preço congelado antes da recarga e do novo preço após recarga. O descarte de regras de preço inválidas foi observado pelos avisos reais; o depósito/pagamento de trigo enquanto regras inválidas coexistiam não foi repetido nessa sessão, nem a recusa manual do graveto comum. A tabela não atribui essas variações adicionais como executadas. O núcleo solicitado de preços e aniversários com reload dedicado passou.

Build desta continuação: `BUILD SUCCESSFUL in 6s`, comando `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`. XML conferidos: 38 testes, zero falhas, erros ou ignorados; `test UP-TO-DATE`, reutilizando a reexecução desta sessão já registrada. Saída em `run/validation/build-conclusion.log`. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```

Estado final: servidor encerrado normalmente; HFTester2 desconectado pelo encerramento, saldo 760, inverno 20, fila vazia. Pack válido preservado no disco e desativado. HFTester1 teve saldo zerado na preparação interrompida do teste de preços e recebeu quatro trigos; não foi usado no pagamento final. Arquivos de evidência de console preservados em `run/validation/server-final.log`; leituras offline before/after também preservadas. Cliente gráfico pode permanecer aberto na tela de desconexão; nenhuma automação adicional de janela foi feita nesta conclusão.

Pendências obrigatórias: espelho de ouro/aniversário no primeiro consumidor cliente; decisão do responsável sobre comportamento offline; revisão e fechamento explícito da Fase 2 pelo responsável. Falhas nativas de GLFW durante ativação de janelas continuam sem causa comprovada e sem mudança no mod para contorná-las.

## Aceite e transição — 08/11/2026

O responsável deu a Fase 2 e a oitava fatia como concluídas, exceto o cenário 8, e autorizou seguir à Fase 3 antes dessa verificação. Esta decisão sucede os registros históricos que mantinham a fase aberta ou aguardavam aceite do comportamento offline. Mantida a regra observada de pagamento na próxima virada com o jogador conectado, sem correção e sem alegar prova adicional de paridade.

O cenário 8 continua não executado e obrigatório no primeiro consumidor cliente (HUD/interface): ouro/aniversário do titular, isolamento entre jogadores, atualização, limpeza e reconexão. Mensagens administrativas e testes de codec não comprovam o espelho. Avançar não elimina a pendência nem autoriza implementar HUD agora.

Somente STATUS.md e PORTING_NOTES.md atualizados. Nenhum código, pacote ou sistema novo; primeira fatia da Fase 3 ainda precisa de escopo e critérios verificáveis conforme DEPENDENCIES.md. Limitações dos testes e incidentes nativos de GLFW preservados. Nenhum comando Git ou controle de interface realizado.

Validação documental da transição: `BUILD SUCCESSFUL in 6s`; 38 testes nos XML, zero falhas, erros ou ignorados. `test UP-TO-DATE`, sem nova execução de testes. Comando: `.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain`; saída em `run/validation/build-phase3-transition.log`. Nenhum cliente aberto nesta atualização. Última linha literal:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.2.1/userguide/configuration_cache_enabling.html
```
