# Solo separado — decisão substituída na fatia 11

Registro do contrato das fatias 9/10, preservado após autorização de farmland compartilhada. Não é o comportamento vigente; consultar [Ferramentas](../porting/ferramentas.md).

- Enxada HF produzia `harvestfestival:prepared_soil[watered=false]`, cubo completo passivo, sem item, com loot de terra. Vanilla produzia outro bloco, `minecraft:farmland`.
- Rega exigia `watered=false` e gravava true. JSON da enxada/regador apontava para prepared_soil; tags crop_soils/waterable continham esse bloco.
- Cultivos avaliavam WATERED antes de limpar true→false diariamente. Não havia hidratação por proximidade/chuva, ticks de umidade, perda do preparo nem pisoteio.
- Os recortes foram aprovados nas fatias 9/10. Aceite single player da fatia 9 em 09/11/2026 e da fatia 10 em 12/11/2026; não abrangem a migração posterior.
- Após relato do responsável em 12/11/2026, conferiu-se que o original usa farmland vanilla, permite enxadas vanilla por padrão e tem bloqueio opcional. A separação ficou como pendência funcional da Fase 3, sem incompatibilidade real demonstrada.
- O responsável autorizou substituir por farmland compartilhada, migrar saves do port, adotar desidratação 7→3→0 e retorno à terra sem planta; autorizou também pisoteio por queda conforme original. Isso sucede o adiamento desses dois comportamentos. Chuva/clima continuam fora desta alteração.
- Cenário 8, indicadores/texturas do regador, dedicado e multiplayer não ganham cobertura a partir desta decisão.
