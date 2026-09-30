-- Portaria :: dados de contato do organizador
--
-- Quem opera o sistema é o organizador do evento, e o participante precisa de
-- um caminho para tirar dúvidas antes de se inscrever.
--
-- O endereço é texto livre de propósito: aqui ele serve como referência de
-- contato, não para entrega nem cobrança. Estruturar em CEP, logradouro,
-- número e UF exigiria validação e consulta de CEP para um dado que ninguém
-- vai processar. Se um dia houver emissão de documento fiscal, aí sim.
--
-- Todos opcionais: o sistema já funciona sem nenhum deles.

ALTER TABLE usuario ADD COLUMN telefone VARCHAR(30);
ALTER TABLE usuario ADD COLUMN endereco VARCHAR(200);
ALTER TABLE usuario ADD COLUMN site     VARCHAR(150);
