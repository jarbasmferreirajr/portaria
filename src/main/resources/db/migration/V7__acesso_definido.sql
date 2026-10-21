-- Portaria :: a conta já teve a senha definida pela própria pessoa?
--
-- O sistema decidia enviar convite perguntando "existe conta com este e-mail?".
-- A pergunta certa é outra: "esta pessoa consegue entrar?". As duas divergem
-- num caso real -- alguém convidado que nunca abriu o link, foi removido do
-- evento e depois reincluído: a conta existe, o acesso nunca foi definido, e
-- nenhum convite era enviado. A pessoa ficava presa.
--
-- Contas criadas por convite nascem com acesso NÃO definido: a senha delas é
-- um valor aleatório que ninguém conhece. As que já existem são marcadas como
-- definidas, porque entraram no sistema por outro caminho.

ALTER TABLE usuario ADD COLUMN acesso_definido BOOLEAN DEFAULT TRUE NOT NULL;
