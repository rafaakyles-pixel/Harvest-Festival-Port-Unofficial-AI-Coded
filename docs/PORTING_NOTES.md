# Notas de portabilidade

Referência técnica de entrada. Leia a cada tarefa; consulte somente os contratos pertinentes abaixo.
Histórico não faz parte da leitura inicial. Regras de manutenção na seção 9 de `AGENTS.md`.

## Escopo e alvo

- Original: `reference/original-1.12.2/`, branch `1.12.2-0.6.X`, somente leitura; conteúdo até 0.6.x.
- TODOs e vestígios não constituem compromisso de implementação; ambiguidades exigem consulta ao responsável.
- Minecraft 26.2 / NeoForge 26.2.0.93; ModDevGradle 2.0.148, Gradle 9.2.1, toolchain Java 25.
- Identidade definida pelo responsável em 25/11/2026: port quase idêntico ao original, priorizando atualizações para o ambiente moderno. Na 21-C foram aprovadas altura completa da dimensão e classificação moderna de precipitação por bioma/posição; a classificação agrícola aprovada na 21-B permanece independente. Decisões e adaptações concretas em [Calendário](porting/calendario.md#inverno-físico-e-visual--fatia-21-c).
- Mod único `harvestfestival`, versão técnica `0.1.0-dev`. Fase e pacotes autorizados: [STATUS](STATUS.md).
- Não prometer migração de saves 1.12.2; as fatias implementadas não realizam essa migração.
- Grafo e prioridade executiva em [DEPENDENCIES](DEPENDENCIES.md): 20 aprovada; fechamento agrícola A–E e complementos de servidor vazio, substituição de plantas após morte e controle de umidade, além das correções de queda/caixa de seleção dos frutos registradas em [Árvores](porting/arvores-frutiferas.md), antes dos NPCs, adiados para a 22 ou 23. Escopo e critérios em [STATUS](STATUS.md#sequência-aprovada-pelo-responsável--fechamento-agrícola--npcs-na-22-ou-23). Complementos dependentes de NPCs ficam após os NPCs; integrações específicas continuam adiadas em [FEATURES](FEATURES.md). Decisão de 23/11/2026, sem implementação nesta revisão.

## Consulta por sistema

21-B e 21-C aceitas globalmente, sem ampliar cobertura dedicada/multiplayer. Suporte/seleção implementados na C.1, incluindo extensão solicitada de queda por decay das folhas frutíferas; remoção sazonal permanece sem drop por decisão do responsável. Validação real pendente; cópia pelo botão central permanece em C.2 (árvores) e D.1 (sementes dos cultivos), com regressão em E. Ordem e critérios em [STATUS](STATUS.md#distribuição-dos-ajustes-solicitados-em-25112026); fontes e APIs da C.1 em [Árvores](porting/arvores-frutiferas.md#suporte-e-seleção-dos-frutos--fatia-21-c1).

Leia o documento correspondente antes de alterar o sistema ou implementar um consumidor de seu contrato.
As APIs verificadas e os formatos persistidos ficam nesses documentos, junto às limitações da verificação.
Não há tabela global acumulativa nem diário de fatias neste arquivo.

| Quando consultar | Contrato vigente |
| --- | --- |
| Ouro, attachments, sincronização, aniversários do jogador/NPCs | [Jogador](porting/jogador.md) |
| Relógio, datas, clima diário/previsão, virada do dia, qualquer consumidor de `NewDayEvent` | [Calendário](porting/calendario.md) |
| Caixa, fila, preços por item/tag, venda e pagamento | [Remessas](porting/remessas.md) |
| Enxada/regador/foice até mystril, carga/área/nível/durabilidade, água, esforço e solo | [Ferramentas](porting/ferramentas.md) |
| Cultivos até a 19-A, estação nula no Nether/End para todos os HF; 19-B integralmente adiada com levantamento preservado; transformação em vanilla, grama dupla, loot e política delimitada | [Cultivos](porting/cultivos.md) |
| Árvores frutíferas: produtor persistente independente dos troncos, migração conservadora, templates, folhas/frutos e adaptações aprovadas na 20 | [Árvores frutíferas](porting/arvores-frutiferas.md) |
| Aspersores velho/ferro: ordem diária, tanque e transferência implementados; aquisição externa adiada | [Aspersores](porting/aspersores.md) |

Abas e destino criativo dos itens: [Abas criativas](porting/abas-criativas.md). Desde 14/11/2026, itens novos devem receber destino explícito nas abas aprovadas. Isso não encerra cenário 8.

Continuidade agrícola e intenção futura de milho em duas camadas, adiada até texturas/recorte próprios, em [FEATURES](FEATURES.md#sequência-agrícola-pendente); evidências corrigidas de reflexão/handlers em [Cultivos](porting/cultivos.md). Planos transitórios removidos após implementação por autorização do responsável; roteiros manuais somente na resposta final.

Aceites globais das fatias 15, 16, 17, 18 e 19-A registrados em [STATUS](STATUS.md), sem inferir resultados individuais de testes; na 19-A o responsável relatou plantio normal do Nabo Robusto. Evidências, contratos implementados e levantamento da continuidade agrícola nos contratos de [Cultivos](porting/cultivos.md) e [Ferramentas](porting/ferramentas.md). Ampliação futura da foice para vegetação vanilla permanece fora da 20; cenário 8 e dedicado/multiplayer permanecem pendentes.

### Contratos transversais e decisões obrigatórias

- Estrutura de alimentação de todos os alimentos será implementada conjuntamente em uma fatia posterior, por decisão explícita do responsável ao aprovar a fatia 16. Produtos HF desta expansão permanecem sem alimentação; isso não representa paridade alimentar com o original.
- Todo item novo adicionado em uma fatia deve incluir textura do original, conforme condição explícita do responsável em 13/11/2026. Registrar proveniência/licença e distinguir inspeção de arquivo de validação em jogo. Não alterar AGENTS.md sem autorização específica.
- Plantio de sementes exige solo arado válido no bloco clicado antes de tentar o 3×3, por solicitação explícita de 14/11/2026. É divergência autorizada do original, que valida cada destino sem essa condição central; contrato e evidência em [Cultivos](porting/cultivos.md).
- A Fase 3 deve completar o gameplay de cada sistema em fatias verificáveis; mínimos aprovados não equivalem a sistema completo. Pendências agrícolas, incluindo relação com ferramentas vanilla e solo compartilhado, pertencem à Fase 3, não ao polimento. Diretriz reafirmada em 12/11/2026; detalhes em [Ferramentas](porting/ferramentas.md).
- Servidor é autoridade dos dados; ouro/aniversário sincronizam apenas ao titular. Comandos administrativos consultam o servidor.
- O calendário deriva do relógio persistente do Overworld; não criar segundo calendário. A sequência técnica persistente de eventos aprovada na fatia 10 identifica atualizações, não mede dias ou tempo; contrato em [Calendário](porting/calendario.md).
- Consumidor agrícola resolve estação nula no Nether/End para todos os cultivos HF, por aprovação ampliada da 19-A. Demais dimensões mantêm estação global; plantas existentes nessas duas dimensões murcham na próxima virada processada. Não altera relógio nem plantas vanilla. 19-B adiada integralmente; suas decisões específicas serão reavaliadas se retomada.
- `NewDayEvent` notifica uma transição observada; não compensa dias pulados nem promete transação durável contra crash.
- Remessas fixam preço no depósito e pagam na próxima virada com o jogador conectado. Não pagam offline nem ao reconectar; comportamento aceito em 08/11/2026.
- A futura escolha de aniversário pelo jogador deve reutilizar API/attachment existentes, validando autorização; escolha e efeitos continuam adiados.
- Cultivos avaliam farmland com `moisture=7` ou precipitação agrícola antes da passagem diária de solo; sem precipitação, secagem 7→3→0 e retorno à terra sem planta seguem vigentes. Integração 21-B, tag de biomas aprovada e regra distinta de preparo/carga em [Cultivos](porting/cultivos.md#chuva-agrícola--fatia-21-b). Migração de prepared_soil e pisoteio da fatia 11 mantidos, sem compensação retroativa. Clima diário em [Calendário](porting/calendario.md#clima-diário--fatia-21-a); inverno físico/visual permanece na C.
- Rega ampliada por autorização de 16/11/2026: qualquer bloco pode iniciar a área existente, com alterações/custos somente em solos aceitos pelas regras; regadores são exceção à negação de uso da política vanilla. Geometria, permissões e limites em [Ferramentas](porting/ferramentas.md#ampliação-autorizada-da-rega--16112026).

## Validação e pendências obrigatórias

- Fase 2 encerrada pelo responsável em 08/11/2026 com ressalva do cenário 8; Fase 3 autorizada.
- Cenários 1–7 e 9 aceitos conforme cobertura e limitações registradas na [fatia 8](archive/fatia-08-validacao-integrada.md).
- Isso não atribui execução a variantes não observadas, incluindo variantes mutadoras de permissões e repetições adicionais de testes de datapack.
- **Cenário 8 NÃO EXECUTADO:** inspecionar ouro/aniversário no cliente do titular, isolamento entre jogadores, atualização, limpeza e reconexão.
- Essa obrigação **já integra o aceite do complemento visual do regador**. Barra/tooltip de água, mensagens administrativas e testes de codec não comprovam espelho de ouro/aniversário.
- Roteiro existente: seção “Roteiro integrado completo” da [fatia 8](archive/fatia-08-validacao-integrada.md). Não criar documento vazio nem adiar a obrigação para outra interface.
- A pendência não autoriza antecipar HUD de ouro/aniversário ou calendário visual.
- Fatia 9 aceita em single player em 09/11/2026, sem relato individual por passo; dedicado/multiplayer continuam pendentes.
- Indicadores do regador confirmados funcionais pelo responsável nesta solicitação em 14/11/2026. Substitui a pendência visual dos registros anteriores; não encerra cenário 8 nem dedicado/multiplayer.
- Fatia 11: farmland compartilhada aceita pelo responsável em 12/11/2026 («Testado e aprovado»), em single player, sem detalhamento individual dos cenários. Esse aceite não encerra cenário 8, indicadores, dedicado ou multiplayer; texturas tiveram confirmação separada. Evidência e cobertura em [Ferramentas](porting/ferramentas.md).
- Fatia 10: teste final aprovado pelo responsável em 12/11/2026, sem ampliar cobertura além do ciclo single player entregue. Build anterior aprovado, 24 testes novos/81 totais executados. Cobertura em [Cultivos](porting/cultivos.md); dedicado/multiplayer e cenário 8 continuam pendentes. Texturas da enxada/regador confirmadas separadamente pelo responsável em 12/11/2026 como correspondentes ao original; barra/tooltip e cenário 8 permanecem pendentes.

- Fatia 12 e correções aceitas globalmente pelo responsável em 14/11/2026 («Confirmado a fatia»), sem ampliar cobertura individual, dedicado/multiplayer, cenário 8 ou indicadores do regador. Contrato em [Cultivos](porting/cultivos.md).

## Ambiente consolidado

Na raiz `C:\dev\harvest-festival-port`, em PowerShell; variáveis somente do processo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:GRADLE_USER_HOME = 'C:/gradle-home'
New-Item -ItemType Directory -Path 'build/tmp' -Force | Out-Null
$env:TEMP = (Resolve-Path 'build/tmp').Path
$env:TMP = $env:TEMP
.\gradlew.bat build '-Dorg.gradle.jvmargs=-Xmx1G -Dfile.encoding=COMPAT' --max-workers=2 --console=plain
```

- Java 21 é o launcher Gradle deste ambiente; a toolchain de compilação/testes/jogo permanece Java 25.
- Para reexecutar testes: `build test --rerun` com os mesmos argumentos. `test UP-TO-DATE` não comprova nova execução.
- Para cliente: substituir `build` por `runClient`. Perfil atual em `run/client1`; saves/datapacks/logs ficam nessa pasta.
- Servidor + dois clientes: configuração e comandos reutilizáveis na [fatia 8](archive/fatia-08-validacao-integrada.md).
- Temporários locais resolveram a falha de loopback observada; caminho ASCII e `COMPAT` resolveram a falha observada do executor de testes.
- Quedas `EXCEPTION_ACCESS_VIOLATION` em `glfw.dll`: causa não determinada, sem bug do mod comprovado.
- Avisos OSHI/Perflib não impediram carregamento/menu observados; isso não demonstra inocuidade em qualquer situação.
- Nenhuma configuração global alterada. Incidentes e procedimentos anteriores permanecem no [histórico](archive/README.md).

## Equivalências e limites de verificação

| Original 1.12.2 | Direção de portagem |
| --- | --- |
| `OreDictionary`, metadados e IDs fixos | Tags, registros modernos, JSON; variantes nos catálogos |
| Receitas/preços/cultivos em Java | Dados e regras declarativas, definidos na fatia correspondente |
| `WorldSavedData`, NBT manual | Escolher mecanismo por escopo; attachments já verificados para jogador, sem presumir APIs dos demais sistemas |
| `HFTrackers` | Contratos básicos e composição gradual conforme dependências |
| Pacotes/proxies | Rede moderna e isolamento cliente; verificar APIs específicas por sistema |
| `WorldProvider`, dimensões numéricas | Reavaliar mineração/clima na versão alvo |
| GUIs, TESR, OpenGL | Reimplementar conforme a versão alvo |

Direções arquiteturais não são aprovação de APIs concretas. Verificações exatas e suas evidências estão nos contratos por sistema.

## Mixins e itens não verificados

- Clima diário: `harvestfestival.tools.mixin.DailyWeatherMixin` substitui `ServerLevel.advanceWeatherCycle` apenas em tipos `minecraft:overworld`; nenhuma API/evento equivalente nas fontes exatas 26.2.0.93. Previsão, rede, transições, comandos e limites em [Calendário](porting/calendario.md#clima-diário--fatia-21-a). Renderização sazonal específica continua fora da 21-A.

- Árvores, 23/11/2026: `TreeTrunkMixin` observa a mutação real em `LevelChunkSection.setBlockState` dentro de `LevelChunk.setBlockState` para encerrar vínculos sem depender de eventos parciais de quebra/colocação; `TreeRandomTickMixin` preserva sorteios nos dois pontos originais quando os troncos HF já não existem, usando `ServerLevel.tickChunk`. Subpacote existente `harvestfestival.tools.mixin`, sem pacote novo. Eventos disponíveis não cobrem essas operações integralmente; fontes exatas, persistência/migração, snapshots e testes em [Árvores frutíferas](porting/arvores-frutiferas.md).
- Frutos, C.1: `FruitSupportMixin` envolve `ServerPlayerGameMode.removeBlock` para observar remoção efetiva por jogador, inclusive criativo, após cancelamentos/permissões. Evento de quebra é anterior à remoção e loot não cobre criativo; por isso não substituem esse ponto. Reutiliza `TreeTrunkMixin` para invalidar confirmações por alterações/restauração, sem mudar vínculos. API exata 26.2/26.2.0.93, geometria, divergência autorizada e limites em [Árvores](porting/arvores-frutiferas.md#suporte-e-seleção-dos-frutos--fatia-21-c1).

- Abóbora vanilla lisa 26.2 e adaptações da grama aprovadas em 18/11/2026; fontes/APIs e diferenças em Cultivos. Alternativa de abóbora própria não adotada (suas APIs não foram verificadas). Ramo visual sazonal e callbacks originais da grama não observados em jogo; par coerente é adaptação aprovada. Sem mixin novo.

- Original tem exatamente 2 mixins de cliente; levantamento completo em [mixins originais](archive/mixins-originais.md).
- Inverno: `WinterPrecipitationMixin` adapta as consultas de formação em `ServerLevel.tickPrecipitation`; `WinterParticlesMixin`, exclusivo do cliente, cancela `ClientLevel.tickWeatherEffects` conforme o último clima não ensolarado. Eventos modernos atendem colunas e neblina, mas não essas duas interceptações sem substituir dados dimensionais. Fontes exatas, justificativas, alcance e validação em [Calendário](porting/calendario.md#inverno-físico-e-visual--fatia-21-c).
- Anzol: equivalente para reconhecer vara HF no renderizador **não verificado**. Investigar extensão moderna; renderizador próprio é alternativa, sem decisão aprovada.
- Access transformers, reflexão e provedores antigos devem ser reavaliados caso a caso, sem cópia automática.
- Árvores/aspersores da 20 implementados nos pacotes existentes, com exceção dimensional própria, copa integral protegida, autonomia sem dono e flags de espécie/sincronização integral aprovados. Ajuste de 23/11: folhas obtíveis por tesoura ou Silk Touch, metades maduras independentes e par juvenil mantido por decisão explícita; detalhes no contrato de árvores. APIs, saves, recursos e validações nos contratos; cliente sem inspeção visual autorizada, dedicado/multiplayer pendentes. Nenhuma migração/conversão vanilla→HF ou aquisição substituta.
- Fatia 11: `harvestfestival.tools.mixin.FarmlandBlockMixin`, subpacote aprovado explicitamente pelo responsável. Alvo `FarmlandBlock.randomTick`, injeção HEAD cancelável para solos participantes por tag; impede hidratação/secagem aleatória concorrendo com o ciclo diário. A fonte exata 26.2.0.93 não oferece evento cancelável nesse método. Queda e ticks de obstrução permanecem vanilla/NeoForge. Evidências, limites e migração em [Ferramentas](porting/ferramentas.md).

- Fatia 14: `harvestfestival.tools.mixin.PlayerSleepAccessor`, autorizado explicitamente pelo responsável, escreve somente `Player.sleepCounter` no desmaio com sono instantâneo. Fonte exata 26.2.0.93 não expõe setter/evento equivalente; decisão coletiva de sono continua no motor. Aplicação à classe transformada testada; aceite global da fatia 14 informado pelo responsável em 15/11/2026, sem detalhamento individual do sono. Contrato em [Ferramentas](porting/ferramentas.md).

## Histórico e manutenção

- [Índice dos registros preservados](archive/README.md): texto anterior integral, dividido por assunto/fatia; propostas antigas não prevalecem sobre decisões posteriores.
- Atualizar contratos por sistema no lugar, preservando decisões substituídas no histórico quando necessário.
- Acrescentar aqui apenas decisões transversais, pendências obrigatórias ou uma linha de índice para novo sistema.
- Não acrescentar resumos por fatia, listas globais de APIs ou logs de build. A meta é manter esta entrada estável durante a portagem.


Memória de camas por dimensão mantida pendente pelo responsável na fatia 14; o port usa somente RespawnConfig correspondente à dimensão destino e fallback quando ausente. Limites, dados de esforço/sono e APIs em [Ferramentas](porting/ferramentas.md).
