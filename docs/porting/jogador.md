# Ouro e aniversários — contratos vigentes

Consultar para dados do jogador, sincronização e aniversários. Cenário 8 não executado; mensagens administrativas não comprovam espelho cliente. Histórico em [arquivo](../archive/README.md).

### Comportamento e limites da fatia

- Referências consultadas: `player/stats/Stats`, `StatsServer`, `PlayerLoader` e `player/command/HFCommandGold`. Saldo inicial zero, armazenamento `long`, valores negativos permitidos e soma com overflow de 64 bits preservados, sem teto ou clamp adicional.
- O original mantém dados por UUID fora da entidade. No port, o attachment `harvestfestival:gold` é salvo pelo ciclo vanilla/NeoForge de dados do jogador. `copyOnDeath()` preserva o ouro na substituição da entidade ao renascer. Não há migração dos arquivos `HF/<UUID>.dat` de 1.12.2 nesta fatia.
- Atualizado na quarta fatia: `PlayerGold.get(Player)` permite leitura no cliente; `set/add` continuam recebendo `ServerPlayer`. Comandos executam no servidor. O attachment agora sincroniza o saldo somente ao dono; HUD/GUI permanecem adiados.
- Traduções dos comandos em JSON `en_us` e `pt_br`. Ouro não depende de lojas ou remessas.

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
Quarta fatia aprovada pelo usuário e implementada em 02/11/2026. O original `StatsServer.syncGold` envia `PacketSyncGold` apenas ao jogador proprietário, com um long de 64 bits; o cliente atualiza seus próprios dados. O port usa `AttachmentType.Builder.sync((holder, recipient) -> holder == recipient, ByteBufCodecs.LONG)`, mantendo o codec persistente e `copyOnDeath`. Não há pacote próprio nem mensagem cliente → servidor para alterar ouro.

APIs verificadas nas fontes locais de NeoForge 26.2.0.93 e Minecraft correspondente: `AttachmentType.Builder.sync`, `AttachmentSync.syncEntityUpdate`, `syncInitialPlayerAttachments` e `ByteBufCodecs.LONG`. O caminho de atualização inclui o próprio jogador entre os destinatários e aplica o filtro; o envio inicial também usa o filtro. `PlayerList` chama a sincronização inicial ao entrar e renascer; `ServerPlayer` a chama após troca de dimensão. Um attachment ainda ausente conserva o padrão zero em ambos os lados.

`PlayerGold.get(Player)` disponibiliza a leitura do espelho ao futuro HUD. Alterações pela API do mod continuam restritas a `ServerPlayer`. Ler o attachment de outro jogador no cliente não fornece seu saldo real. Nenhuma classe exclusiva do cliente foi importada pelo código comum.
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

**Sincronização com cliente verificada somente por teste de codificação de rede**, cobrindo data e ausência. O filtro de envio usa `holder == recipient`, como ouro. Não houve inspeção do espelho cliente nem tráfego real. A verificação real permanece pendente na validação integrada. Consultas administrativas verificam o estado no servidor.

## Validação consolidada

Ouro: 3 testes iniciais. Aniversários: 11 novos, suíte total 37; correção booleano/texto: 1 novo, suíte total 38. Single player aceito com limites históricos; dedicado conforme fatia 8, sem encerrar espelho cliente. A mensagem test usa Boolean.toString; retorno Brigadier continua 1/0. Escolha pelo jogador e efeitos adiados.

Comandos de ouro: /hf gold [player] get|set|add; set/add recebem valor. PlayerGold.addTo é a soma reutilizada por remessas.
