
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

