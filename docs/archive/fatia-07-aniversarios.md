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

