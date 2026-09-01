-- Garante a criação da tabela de Parâmetros Globais do Sistema
CREATE TABLE IF NOT EXISTS parametro_sistema (
    id BIGSERIAL PRIMARY KEY,
    chave VARCHAR(100) NOT NULL UNIQUE,
    valor TEXT NOT NULL,
    descricao VARCHAR(255),
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Se a tabela já existia sem a coluna descricao, adiciona a coluna com segurança
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'parametro_sistema' AND column_name = 'descricao'
    ) THEN
        ALTER TABLE parametro_sistema ADD COLUMN descricao VARCHAR(255);
    END IF;
END $$;

-- Criação da tabela de Usuários para autenticação e RBAC
CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserção de usuários padrão com hash BCrypt (Senha: 123456)
INSERT INTO usuario (nome, username, senha, perfil, ativo)
VALUES
('Operador de Doca', 'operador', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ROLE_OPERADOR', true),
('Administrador Geral', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ROLE_ADMIN', true)
ON CONFLICT (username) DO NOTHING;

-- Parâmetro inicial para Mistral AI
INSERT INTO parametro_sistema (chave, valor, descricao)
VALUES ('MISTRAL_API_KEY', '', 'Chave de API do provedor Mistral AI cadastrada via painel')
ON CONFLICT (chave) DO NOTHING;