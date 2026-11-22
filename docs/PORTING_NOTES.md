# Notas de portabilidade

Referência técnica de entrada. Leia a cada tarefa; consulte somente os contratos pertinentes abaixo.
Histórico não faz parte da leitura inicial. Regras de manutenção na seção 9 de `AGENTS.md`.

## Escopo e alvo

- Original: `reference/original-1.12.2/`, branch `1.12.2-0.6.X`, somente leitura; conteúdo até 0.6.x.
- TODOs e vestígios não constituem compromisso de implementação; ambiguidades exigem consulta ao responsável.
- Minecraft 26.2 / NeoForge 26.2.0.93; ModDevGradle 2.0.148, Gradle 9.2.1, toolchain Java 25.
- Mod único `harvestfestival`, versão técnica `0.1.0-dev`. Fase e pacotes autorizados: [STATUS](STATUS.md).
- Não prometer migração de saves 1.12.2; as fatias implementadas não realizam essa migração.
- Grafo e prioridade executiva em [DEPENDENCIES](DEPENDENCIES.md): 19 cultivos especiais, 20 árvores/aspersores em subfatias, NPCs a partir da 21, conforme sequência aprovada pelo responsável. Integrações específicas adiadas em [FEATURES](FEATURES.md).

## Consulta por sistema

Leia o documento correspondente antes de alterar o sistema ou implementar um consumidor de seu contrato.
As APIs verificadas e os formatos persistidos ficam nesses documentos, junto às limitações da verificação.
Não há tabela global acumulativa nem diário de fatias neste arquivo.

| Quando consultar | Contrato vigente |
| --- | --- |
| Ouro, attachments, sincronização, aniversários do jogador/NPCs | [Jogador](porting/jogador.md) |
| Relógio, datas, virada do dia, qualquer consumidor de `NewDayEvent` | [Calendário](porting/calendario.md) |
| Caixa, fila, preços por item/tag, venda e pagamento | [Remessas](porting/remessas.md) |
| Enxada/regador/foice até mystril, carga/área/nível/durabilidade, água, esforço e solo | [Ferramentas](porting/ferramentas.md) |
| Cultivos até a 19-A, estação nula no Nether/End para todos os HF; 19-B integralmente adiada com levantamento preservado; transformação em vanilla, grama dupla, loot e política delimitada | [Cultivos](porting/cultivos.md) |

Abas e destino criativo dos itens: [Abas criativas](porting/abas-criativas.md). Desde 14/11/2026, itens novos devem receber destino explícito nas abas aprovadas. Isso não encerra cenário 8.

Continuidade agrícola e intenção futura de milho em duas camadas, adiada até texturas/recorte próprios, em [FEATURES](FEATURES.md#sequência-agrícola-pendente); evidências corrigidas de reflexão/handlers em [Cultivos](porting/cultivos.md). Planos transitórios removidos após implementação por autorização do responsável; roteiros manuais somente na resposta final.

Aceites globais das fatias 15, 16, 17 e 18 registrados em [STATUS](STATUS.md), sem inferir resultados individuais de testes. Evidências, contratos implementados e levantamento da continuidade agrícola nos contratos de [Cultivos](porting/cultivos.md) e [Ferramentas](porting/ferramentas.md). Ampliação futura da foice para vegetação vanilla é solicitação não prioritária, fora da 17; cenário 8 e dedicado/multiplayer permanecem pendentes.

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
- Cultivos avaliam farmland com `moisture=7` antes da secagem diária 7→3→0; solo já seco sem planta pode voltar à terra. Migração de prepared_soil e pisoteio por queda autorizados na fatia 11; sem compensação retroativa. Chuva/clima continuam pendentes. Contrato em [Ferramentas](porting/ferramentas.md).
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

- Abóbora vanilla lisa 26.2 e adaptações da grama aprovadas em 18/11/2026; fontes/APIs e diferenças em Cultivos. Alternativa de abóbora própria não adotada (suas APIs não foram verificadas). Ramo visual sazonal e callbacks originais da grama não observados em jogo; par coerente é adaptação aprovada. Sem mixin novo.

- Original tem exatamente 2 mixins de cliente; levantamento completo em [mixins originais](archive/mixins-originais.md).
- Chuva: candidata `CustomWeatherEffectRenderer#tickRain`, consultada na branch móvel 26.2.x. Paridade de partículas/sons, aplicação dinâmica ao Overworld e execução na versão exata 26.2.0.93 **não verificadas**.
- Anzol: equivalente para reconhecer vara HF no renderizador **não verificado**. Investigar extensão moderna; renderizador próprio é alternativa, sem decisão aprovada.
- Access transformers, reflexão e provedores antigos devem ser reavaliados caso a caso, sem cópia automática.
- Fatia 11: `harvestfestival.tools.mixin.FarmlandBlockMixin`, subpacote aprovado explicitamente pelo responsável. Alvo `FarmlandBlock.randomTick`, injeção HEAD cancelável para solos participantes por tag; impede hidratação/secagem aleatória concorrendo com o ciclo diário. A fonte exata 26.2.0.93 não oferece evento cancelável nesse método. Queda e ticks de obstrução permanecem vanilla/NeoForge. Evidências, limites e migração em [Ferramentas](porting/ferramentas.md).

- Fatia 14: `harvestfestival.tools.mixin.PlayerSleepAccessor`, autorizado explicitamente pelo responsável, escreve somente `Player.sleepCounter` no desmaio com sono instantâneo. Fonte exata 26.2.0.93 não expõe setter/evento equivalente; decisão coletiva de sono continua no motor. Aplicação à classe transformada testada; aceite global da fatia 14 informado pelo responsável em 15/11/2026, sem detalhamento individual do sono. Contrato em [Ferramentas](porting/ferramentas.md).

## Histórico e manutenção

- [Índice dos registros preservados](archive/README.md): texto anterior integral, dividido por assunto/fatia; propostas antigas não prevalecem sobre decisões posteriores.
- Atualizar contratos por sistema no lugar, preservando decisões substituídas no histórico quando necessário.
- Acrescentar aqui apenas decisões transversais, pendências obrigatórias ou uma linha de índice para novo sistema.
- Não acrescentar resumos por fatia, listas globais de APIs ou logs de build. A meta é manter esta entrada estável durante a portagem.


Memória de camas por dimensão mantida pendente pelo responsável na fatia 14; o port usa somente RespawnConfig correspondente à dimensão destino e fallback quando ausente. Limites, dados de esforço/sono e APIs em [Ferramentas](porting/ferramentas.md).
