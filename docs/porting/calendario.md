# Calendário e atualização diária — contratos vigentes

Consultar para relógio, datas e consumidores de NewDayEvent.

Implementação aprovada: calendário derivado do relógio persistente do Overworld, com 30 dias por estação, quatro estações e início em segunda-feira, primavera, ano 1. Usa o padrão original de 24000 ticks por dia; configuração de duração, HUD, clima, festivais e notificações para sistemas futuros não fazem parte desta fatia.

APIs conferidas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `WorldClocks.OVERWORLD`, `MinecraftServer.clockManager()`, `ServerClockManager.getTotalTicks/setTotalTicks`. O próprio `ServerClockManager` é `SavedData`, carregado pelo servidor via `computeIfAbsent(ServerClockManager.TYPE)`; não há segundo calendário persistente do mod. Pausa, velocidade do relógio, sono e comandos vanilla afetam a data pela mesma fonte de tempo. A sequência técnica de eventos da fatia 10, abaixo, não mede tempo.

Comandos administrativos de nível GAMEMASTERS: `/hf day` consulta data; `/hf day <valor>` limita o dia a 1–30; `/hf season <spring|summer|autumn|winter>` aceita também maiúsculas; `/hf year <valor>` limita o mínimo a 1. Ajustes preservam a hora. `/hf newDay` move para o último tick do dia, como o original; o dia seguinte começa quando o relógio avança, portanto um relógio pausado precisa ser retomado.

Cálculos usam long para evitar overflow intermediário de int do original em anos elevados. Não há migração de saves 1.12.2. Fontes originais consultadas: CalendarDate, CalendarHelper, CalendarServer, CalendarSavedData, NewDayHandler, HFCalendar e comandos de dia/estação/ano/novo dia.

Build aprovado com cinco testes de calendário e três de ouro. Testes de calendário: data inicial, limites de dia/estação/ano, preservação da hora e anos elevados, ciclo de 1200 dias e gravação/releitura de ClockState em NBT comprimido. O teste de codec não substitui salvar/reabrir um mundo real. Validação manual realizada pelo usuário e confirmada em 02/11/2026: comandos, troca de estação, virada do ano, salvar/reabrir e passagem natural do tempo passaram. A fatia de calendário básico está concluída. Essa confirmação não amplia a cobertura para servidor dedicado ou multiplayer simultâneo.

## Fase 2 — atualização diária interna

Regra aprovada pelo usuário: passagem natural, sono e `/hf newDay` notificam uma atualização; `/hf day`, `/hf season` e `/hf year` reposicionam a referência sem simular dias intermediários. Reabertura não reproduz atualizações anteriores.

`DailyUpdates` compara a data entre `ServerTickEvent.Pre` e `Post` (APIs conferidas nas fontes NeoForge 26.2.0.93), publicando `NewDayEvent` no barramento NeoForge, no servidor, com datas anterior/nova e servidor proprietário. `WorldCalendar.set` reposiciona a referência após ajustes administrativos. `ServerStoppedEvent` remove o estado da sessão. O marcador é transitório: inicializar a referência a cada início de tick evita replay no carregamento, sem criar outro calendário salvo.

O evento é único por transição observada, não um laço de compensação por dias pulados. Remessas e cultivos o consomem; cultivos coordenam crescimento e secagem internamente, sem fases gerais PRE/MAIN/POST. Animais e pausa sem jogadores continuam fora deste contrato. Alterações vanilla ou de terceiros executadas dentro de um tick podem gerar uma transição; a supressão administrativa explícita cobre os comandos HF. Não se promete transação durável ou execução exatamente uma vez em caso de crash de consumidores.

## Identidade persistente de atualização — fatia 10

- Aprovada pelo responsável para idempotência após salvar/reabrir e recuos administrativos: `DailyCycleData`, SavedData `harvestfestival:daily_cycles` do Overworld, payload `{"sequence":0}` inicial. É sequência de eventos efetivamente emitidos, não de dias transcorridos; não modifica o relógio nem gera atualizações na carga.
- `DailyUpdates.afterTick` incrementa uma vez quando `DailyTransition` detecta avanço e publica `NewDayEvent(server, previous, current, cycle)`. Consumidores leem `getCycle()`. Reentregar o mesmo evento conserva a identidade; não alocar outra sequência ao repetir um consumidor.
- Novo dia natural/sono/comando gera identidade nova; ajustes administrativos HF apenas rebaseiam e não incrementam. Salto de vários dias continua um evento. Recuar a data e atravessar novamente a fronteira produz identidade distinta, permitindo crescimento sem ficar preso a uma data máxima antiga.
- `DailyCycleData` marca SavedData como alterado; salvar/reabrir conserva sequência. Cultivos persistem último ciclo por chunk; remessas mantêm sua proteção por consumo de fila, sem alteração econômica.
- Sequência presente inválida, negativa, fracionária, não numérica ou sem campo fica em Long.MAX_VALUE, assim como sequência esgotada. `next()` retorna zero e registra erro, sem wrap/reset. Cultivos ignoram ciclo zero; calendário e pagamentos continuam. Não há recuperação automática de corrupção que possa reaplicar crescimento.
- APIs verificadas nas fontes locais exatas 26.2.0.93 e compiladas: `SavedDataType(Identifier, Supplier, Codec)`, `ServerLevel.getDataStorage`, `SavedDataStorage.computeIfAbsent`, `SavedData.setDirty`. Sem segundo relógio ou mudança no detector de fronteiras.
- Dois testes novos exercitam codec NBT/sequência e transição após recuo, além de payloads inválidos/esgotamento; fazem parte dos 24 novos/81 totais da fatia 10. Não constituem teste de crash ou de servidor dedicado.

Cinco testes novos cobrem transição natural sem repetição, salto de sono na virada do ano, ajuste administrativo, pausa/recuo/reabertura, independência de titulares e ausência de laço por dias pulados. São testes do detector; a verificação manual de sono e eventos reais foi posteriormente confirmada pelo usuário. Não foi repetida automação de interface, conforme preferência do usuário.

## Validação consolidada

Calendário: 5 testes novos, suíte total 8. Detector diário: 5 novos, suíte total 13. Roteiros manuais aceitos. Cobertura dedicada conforme [fatia 8](../archive/fatia-08-validacao-integrada.md), sem ampliar variantes observadas.
