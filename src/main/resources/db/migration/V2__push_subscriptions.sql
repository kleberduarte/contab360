-- Web Push (VAPID): subscriptions por usuário.
-- IF NOT EXISTS: compatível com dev onde Hibernate (ddl-auto=update) já criou a tabela.
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    p256dh VARCHAR(256) NOT NULL,
    auth VARCHAR(256) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_push_subscriptions_endpoint UNIQUE (endpoint),
    CONSTRAINT fk_push_subscriptions_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
