-- Limpa todos os dados do schema atual e mantém apenas o usuário admin.
-- Uso recomendado:
--   mysql -u <usuario> -p <nome_do_banco> < scripts/cleanup_keep_admin.sql

SET @KEEP_EMAIL = 'admin@prd.com.br';
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- Gera e executa DELETE para todas as tabelas, exceto 'usuarios'.
SELECT GROUP_CONCAT(
         CONCAT('DELETE FROM `', table_name, '`')
         ORDER BY table_name
         SEPARATOR '; '
       )
INTO @DELETE_ALL_EXCEPT_USUARIOS
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name <> 'usuarios';

SET @DELETE_ALL_EXCEPT_USUARIOS = IFNULL(@DELETE_ALL_EXCEPT_USUARIOS, 'SELECT 1');
PREPARE stmt_delete_all_except_usuarios FROM @DELETE_ALL_EXCEPT_USUARIOS;
EXECUTE stmt_delete_all_except_usuarios;
DEALLOCATE PREPARE stmt_delete_all_except_usuarios;

-- Mantém apenas o usuário desejado.
DELETE FROM usuarios
WHERE email <> @KEEP_EMAIL;

COMMIT;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
