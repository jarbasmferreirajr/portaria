-- Portaria :: encerramento de evento
--
-- Encerrar não é apagar. Um evento encerrado para de aceitar inscrições, mas
-- continua existindo com toda a sua lista de presença -- que é justamente o
-- registro que dá valor ao sistema depois que o evento acabou.
--
-- Coluna nula = evento aberto. Preenchida = momento em que foi encerrado,
-- o que guarda o "quando" além do "se".

ALTER TABLE evento ADD COLUMN encerrado_em TIMESTAMP;
