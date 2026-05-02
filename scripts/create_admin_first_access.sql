-- Cria/atualiza usuário admin com senha BCrypt e força troca no primeiro acesso.
-- Uso:
--   mysql -u <usuario> -p <nome_do_banco> < scripts/create_admin_first_access.sql
--
-- Senha em texto puro deste script: Admin@123
-- Hash BCrypt gerado para essa senha: $2b$12$P5xiMCsGEJgE6ywTN1hW3uwTVxC1g2XdJplG4knkL9YBMRHt4avii

SET @ADMIN_NOME = 'Administrador';
SET @ADMIN_EMAIL = 'admin@demo.com.br';
SET @ADMIN_PERFIL = 'ADM';
SET @ADMIN_SENHA_HASH = '$2b$12$P5xiMCsGEJgE6ywTN1hW3uwTVxC1g2XdJplG4knkL9YBMRHt4avii';

INSERT INTO usuarios (
    email,
    senha_hash,
    nome,
    perfil,
    ativo,
    senha_temp_ativa,
    empresa_id,
    cliente_pessoa_fisica_id
)
VALUES (
    @ADMIN_EMAIL,
    @ADMIN_SENHA_HASH,
    @ADMIN_NOME,
    @ADMIN_PERFIL,
    1,
    1,
    NULL,
    NULL
)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    perfil = VALUES(perfil),
    senha_hash = VALUES(senha_hash),
    ativo = 1,
    senha_temp_ativa = 1,
    empresa_id = NULL,
    cliente_pessoa_fisica_id = NULL;
