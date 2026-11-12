# Notas de portabilidade

Referência técnica de entrada. Leia a cada tarefa; consulte somente os contratos pertinentes abaixo.
Histórico não faz parte da leitura inicial. Regras de manutenção na seção 9 de `AGENTS.md`.

## Escopo e alvo

- Original: `reference/original-1.12.2/`, branch `1.12.2-0.6.X`, somente leitura; conteúdo até 0.6.x.
- TODOs e vestígios não constituem compromisso de implementação; ambiguidades exigem consulta ao responsável.
- Minecraft 26.2 / NeoForge 26.2.0.93; ModDevGradle 2.0.148, Gradle 9.2.1, toolchain Java 25.
- Mod único `harvestfestival`, versão técnica `0.1.0-dev`. Fase e pacotes autorizados: [STATUS](STATUS.md).
- Não prometer migração de saves 1.12.2; as fatias implementadas não realizam essa migração.
- Grafo e ordem aprovados em [DEPENDENCIES](DEPENDENCIES.md); integrações específicas adiadas em [FEATURES](FEATURES.md).

## Consulta por sistema

Leia o documento correspondente antes de alterar o sistema ou implementar um consumidor de seu contrato.
As APIs verificadas e os formatos persistidos ficam nesses documentos, junto às limitações da verificação.
Não há tabela global acumulativa nem diário de fatias neste arquivo.

| Quando consultar | Contrato vigente |
| --- | --- |
| Ouro, attachments, sincronização, aniversários do jogador/NPCs | [Jogador](porting/jogador.md) |
| Relógio, datas, virada do dia, qualquer consumidor de `NewDayEvent` | [Calendário](porting/calendario.md) |
| Caixa, fila, preços por item/tag, venda e pagamento | [Remessas](porting/remessas.md) |
| Enxada, regador, água no item, solo e futuro cultivo | [Ferramentas](porting/ferramentas.md) |
| Bloco genérico de cultivo, sementes, crescimento diário, morte e colheita | [Cultivos](porting/cultivos.md) |

### Contratos transversais e decisões obrigatórias

- A Fase 3 deve completar o gameplay de cada sistema em fatias verificáveis; mínimos aprovados não equivalem a sistema completo. Pendências agrícolas, incluindo relação com ferramentas vanilla e solo compartilhado, pertencem à Fase 3, não ao polimento. Diretriz reafirmada em 12/11/2026; detalhes em [Ferramentas](porting/ferramentas.md).
- Servidor é autoridade dos dados; ouro/aniversário sincronizam apenas ao titular. Comandos administrativos consultam o servidor.
- O calendário deriva do relógio persistente do Overworld; não criar segundo calendário. A sequência técnica persistente de eventos aprovada na fatia 10 identifica atualizações, não mede dias ou tempo; contrato em [Calendário](porting/calendario.md).
- `NewDayEvent` notifica uma transição observada; não compensa dias pulados nem promete transação durável contra crash.
- Remessas fixam preço no depósito e pagam na próxima virada com o jogador conectado. Não pagam offline nem ao reconectar; comportamento aceito em 08/11/2026.
- A futura escolha de aniversário pelo jogador deve reutilizar API/attachment existentes, validando autorização; escolha e efeitos continuam adiados.
- Cultivos avaliam farmland com `moisture=7` antes da secagem diária 7→3→0; solo já seco sem planta pode voltar à terra. Migração de prepared_soil e pisoteio por queda autorizados na fatia 11; sem compensação retroativa. Chuva/clima continuam pendentes. Contrato em [Ferramentas](porting/ferramentas.md).

## Validação e pendências obrigatórias

- Fase 2 encerrada pelo responsável em 08/11/2026 com ressalva do cenário 8; Fase 3 autorizada.
- Cenários 1–7 e 9 aceitos conforme cobertura e limitações registradas na [fatia 8](archive/fatia-08-validacao-integrada.md).
- Isso não atribui execução a variantes não observadas, incluindo variantes mutadoras de permissões e repetições adicionais de testes de datapack.
- **Cenário 8 NÃO EXECUTADO:** inspecionar ouro/aniversário no cliente do titular, isolamento entre jogadores, atualização, limpeza e reconexão.
- Essa obrigação **já integra o aceite do complemento visual do regador**. Barra/tooltip de água, mensagens administrativas e testes de codec não comprovam espelho de ouro/aniversário.
- Roteiro existente: seção “Roteiro integrado completo” da [fatia 8](archive/fatia-08-validacao-integrada.md). Não criar documento vazio nem adiar a obrigação para outra interface.
- A pendência não autoriza antecipar HUD de ouro/aniversário ou calendário visual.
- Fatia 9 aceita em single player em 09/11/2026, sem relato individual por passo; dedicado/multiplayer continuam pendentes.
- Complemento visual do regador implementado; aparência e passos novos ainda aguardam validação manual.
- Fatia 11: farmland compartilhada aceita pelo responsável em 12/11/2026 («Testado e aprovado»), em single player, sem detalhamento individual dos cenários. Esse aceite não encerra cenário 8, indicadores/texturas anteriores, dedicado ou multiplayer. Evidência e cobertura em [Ferramentas](porting/ferramentas.md).
- Fatia 10: teste final aprovado pelo responsável em 12/11/2026, sem ampliar cobertura além do ciclo single player entregue. Build anterior aprovado, 24 testes novos/81 totais executados. Cobertura em [Cultivos](porting/cultivos.md); dedicado/multiplayer e cenário 8 continuam pendentes. Texturas originais de enxada/regador aplicadas depois desse aceite ainda aguardam verificação visual.

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

- Original tem exatamente 2 mixins de cliente; levantamento completo em [mixins originais](archive/mixins-originais.md).
- Chuva: candidata `CustomWeatherEffectRenderer#tickRain`, consultada na branch móvel 26.2.x. Paridade de partículas/sons, aplicação dinâmica ao Overworld e execução na versão exata 26.2.0.93 **não verificadas**.
- Anzol: equivalente para reconhecer vara HF no renderizador **não verificado**. Investigar extensão moderna; renderizador próprio é alternativa, sem decisão aprovada.
- Access transformers, reflexão e provedores antigos devem ser reavaliados caso a caso, sem cópia automática.
- Fatia 11: `harvestfestival.tools.mixin.FarmlandBlockMixin`, subpacote aprovado explicitamente pelo responsável. Alvo `FarmlandBlock.randomTick`, injeção HEAD cancelável para solos participantes por tag; impede hidratação/secagem aleatória concorrendo com o ciclo diário. A fonte exata 26.2.0.93 não oferece evento cancelável nesse método. Queda e ticks de obstrução permanecem vanilla/NeoForge. Evidências, limites e migração em [Ferramentas](porting/ferramentas.md).

## Histórico e manutenção

- [Índice dos registros preservados](archive/README.md): texto anterior integral, dividido por assunto/fatia; propostas antigas não prevalecem sobre decisões posteriores.
- Atualizar contratos por sistema no lugar, preservando decisões substituídas no histórico quando necessário.
- Acrescentar aqui apenas decisões transversais, pendências obrigatórias ou uma linha de índice para novo sistema.
- Não acrescentar resumos por fatia, listas globais de APIs ou logs de build. A meta é manter esta entrada estável durante a portagem.
