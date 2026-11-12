# Calendário e atualização diária — contratos vigentes

Consultar para relógio, datas e consumidores de NewDayEvent.

Implementação aprovada: calendário derivado do relógio persistente do Overworld, com 30 dias por estação, quatro estações e início em segunda-feira, primavera, ano 1. Usa o padrão original de 24000 ticks por dia; configuração de duração, HUD, clima, festivais e notificações para sistemas futuros não fazem parte desta fatia.

APIs conferidas nas fontes locais de Minecraft/NeoForge 26.2.0.93: `WorldClocks.OVERWORLD`, `MinecraftServer.clockManager()`, `ServerClockManager.getTotalTicks/setTotalTicks`. O próprio `ServerClockManager` é `SavedData`, carregado pelo servidor via `computeIfAbsent(ServerClockManager.TYPE)`; não há segundo contador persistente do mod. Pausa, velocidade do relógio, sono e comandos vanilla afetam a data pela mesma fonte de tempo.

Comandos administrativos de nível GAMEMASTERS: `/hf day` consulta data; `/hf day <valor>` limita o dia a 1–30; `/hf season <spring|summer|autumn|winter>` aceita também maiúsculas; `/hf year <valor>` limita o mínimo a 1. Ajustes preservam a hora. `/hf newDay` move para o último tick do dia, como o original; o dia seguinte começa quando o relógio avança, portanto um relógio pausado precisa ser retomado.

Cálculos usam long para evitar overflow intermediário de int do original em anos elevados. Não há migração de saves 1.12.2. Fontes originais consultadas: CalendarDate, CalendarHelper, CalendarServer, CalendarSavedData, NewDayHandler, HFCalendar e comandos de dia/estação/ano/novo dia.

Build aprovado com cinco testes de calendário e três de ouro. Testes de calendário: data inicial, limites de dia/estação/ano, preservação da hora e anos elevados, ciclo de 1200 dias e gravação/releitura de ClockState em NBT comprimido. O teste de codec não substitui salvar/reabrir um mundo real. Validação manual realizada pelo usuário e confirmada em 02/11/2026: comandos, troca de estação, virada do ano, salvar/reabrir e passagem natural do tempo passaram. A fatia de calendário básico está concluída. Essa confirmação não amplia a cobertura para servidor dedicado ou multiplayer simultâneo.

## Fase 2 — atualização diária interna

Regra aprovada pelo usuário: passagem natural, sono e `/hf newDay` notificam uma atualização; `/hf day`, `/hf season` e `/hf year` reposicionam a referência sem simular dias intermediários. Reabertura não reproduz atualizações anteriores.

`DailyUpdates` compara a data entre `ServerTickEvent.Pre` e `Post` (APIs conferidas nas fontes NeoForge 26.2.0.93), publicando `NewDayEvent` no barramento NeoForge, no servidor, com datas anterior/nova e servidor proprietário. `WorldCalendar.set` reposiciona a referência após ajustes administrativos. `ServerStoppedEvent` remove o estado da sessão. O marcador é transitório: inicializar a referência a cada início de tick evita replay no carregamento, sem criar outro calendário salvo.

O evento é único por transição observada, não um laço de compensação por dias pulados. Consumidores podem assinar o evento; remessas já o consomem. Cultivos, animais, fases PRE/MAIN/POST de blocos e pausa sem jogadores continuam fora deste contrato. Alterações vanilla ou de terceiros executadas dentro de um tick podem gerar uma transição; a supressão administrativa explícita cobre os comandos HF. Não se promete transação durável ou execução exatamente uma vez em caso de crash de futuros consumidores.

Cinco testes novos cobrem transição natural sem repetição, salto de sono na virada do ano, ajuste administrativo, pausa/recuo/reabertura, independência de titulares e ausência de laço por dias pulados. São testes do detector; a verificação manual de sono e eventos reais foi posteriormente confirmada pelo usuário. Não foi repetida automação de interface, conforme preferência do usuário.

## Validação consolidada

Calendário: 5 testes novos, suíte total 8. Detector diário: 5 novos, suíte total 13. Roteiros manuais aceitos. Cobertura dedicada conforme [fatia 8](../archive/fatia-08-validacao-integrada.md), sem ampliar variantes observadas.
