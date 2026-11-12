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

