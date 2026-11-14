# Aparência e plantio anteriores à correção de 14/11/2026

Trechos substituídos na correção solicitada após o responsável informar «Tudo certo com o pepino». O aceite informado não valida as correções posteriores nem cenários individuais não relatados. Contrato vigente: [Cultivos](../porting/cultivos.md).

## docs/porting/cultivos.md

- Clique superior com semente tenta um quadrado 3×3 centrado acima do solo clicado. Cada posição exige tag de solo, dois espaços de ar e permissões no solo/planta/acima. Sucesso parcial consome um saco; nenhum sucesso não consome. O fluxo vanilla conserva quantidade em criativo.
- Plano aprovado pelo responsável em 13/11/2026: política de loot vazio por dados, proteção contra falha de escrita, política de recarga abaixo e aparência compartilhada provisória da planta. Condição adicional obrigatória: todo item adicionado em uma fatia deve trazer textura original; pepino e sementes já a incluem. Não autoriza alterar AGENTS.md.
- O blockstate guarda apenas `appearance=0..3`: pequena, intermediária, madura e murcha. É cache de aparência provisória calculado da entidade/definição pelo servidor; não limita nem determina crescimento. Várias definições compartilham essas formas/texturas provisórias. Renderização específica por espécie fica para conteúdo posterior.
- Plantas mantêm texturas provisórias vanilla: trigo inicial/intermediário, beterraba madura e arbusto morto. Itens antigos do nabo permanecem como entregues na fatia 10; os dois novos itens de pepino usam texturas originais. Nomes em en_us e pt_br. Em 12/11/2026 o responsável relatou o visual de arbusto seco e aprovou o teste final do nabo; não atribuir inspeção individual dos demais modelos nem aceite visual do pepino.

## docs/STATUS.md

- Fatia 12 implementada em 13/11/2026 conforme plano aprovado: pepino, rebrota 7→10, oito colheitas, contador persistente, políticas explícitas de loot vazio/recarga e remessa a 40. Build aprovado: 15 testes novos/102 totais executados, zero falhas/erros/ignorados. Aceite manual do ciclo e dos itens pendente; planta mantém aparência compartilhada provisória aprovada. Contrato em [Cultivos](porting/cultivos.md), anterior preservado em [histórico da fatia 12](archive/fatia-12-cultivos-anterior.md). Não encerra cenário 8, indicadores, dedicado ou multiplayer.

## docs/FEATURES.md

- [x] Fatia 12 implementada em 13/11/2026: pepino com nove viradas iniciais, rebrota em três, oito colheitas, contador persistente e preço 40 por tag. Políticas de loot vazio, falhas e recarga aprovadas; nabo preservado. Texturas originais do produto e saco de sementes incluídas como condição do responsável. Build aprovado, 15 testes novos/102 totais executados. Aceite manual pendente; contrato em `porting/cultivos.md`.
- [ ] Recortes restantes da fatia 10: chuva/clima HF e substituição aleatória de plantas mortas por recursos continuam ausentes. Rebrota limitada incluída na fatia 12; perda do preparo e pisoteio na fatia 11. Aparência provisória compartilhada das plantas não representa renderização completa dos demais cultivos. Todo item novo de cada fatia deve incluir textura original, conforme decisão de 13/11/2026.
