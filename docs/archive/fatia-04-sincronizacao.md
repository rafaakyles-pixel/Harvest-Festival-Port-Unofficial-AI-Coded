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

