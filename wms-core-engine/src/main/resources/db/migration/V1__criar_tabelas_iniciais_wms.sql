-- 1. Tabela de Produtos (Cadastro mestre e triagem)
CREATE TABLE produto (
    id BIGSERIAL PRIMARY KEY,
    codigo_sku VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    categoria_risco VARCHAR(30) NOT NULL DEFAULT 'PADRAO', -- PADRAO, INFLAMAVEL, PERECIVEL, QUIMICO
    recomendacao_armazenagem TEXT,                          -- Preenchido via triagem ou IA consultiva
    peso_unitario_kg NUMERIC(10, 2) NOT NULL,
    volume_unitario_m3 NUMERIC(10, 4) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Tabela de Endereços Físicos do Armazém (Rua-Bloco-Nível-Posição)
CREATE TABLE endereco_estoque (
    id BIGSERIAL PRIMARY KEY,
    codigo_endereco VARCHAR(30) NOT NULL UNIQUE, -- Ex: R01-B01-N01-P01
    rua VARCHAR(10) NOT NULL,
    bloco VARCHAR(10) NOT NULL,
    nivel INT NOT NULL,                          -- Nível 1 = Chão/Piso (cargas pesadas)
    posicao INT NOT NULL,
    capacidade_peso_kg NUMERIC(10, 2) NOT NULL,
    capacidade_volume_m3 NUMERIC(10, 4) NOT NULL,
    ocupado BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0            -- Controle de concorrência otimista (@Version)
);

-- 3. Tabela de Paletes (Unidade de movimentação física)
CREATE TABLE palete (
    id BIGSERIAL PRIMARY KEY,
    codigo_lote VARCHAR(50) NOT NULL UNIQUE,
    produto_id BIGINT NOT NULL,
    quantidade_itens INT NOT NULL,
    peso_total_kg NUMERIC(10, 2) NOT NULL,
    volume_total_m3 NUMERIC(10, 4) NOT NULL,
    endereco_id BIGINT UNIQUE,                   -- Um palete ocupa no máximo 1 endereço
    alocado_em TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_palete_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT fk_palete_endereco FOREIGN KEY (endereco_id) REFERENCES endereco_estoque (id)
);

-- 4. Tabela de Auditoria e Rastreabilidade de Movimentação
CREATE TABLE movimentacao_auditoria (
    id BIGSERIAL PRIMARY KEY,
    palete_id BIGINT NOT NULL,
    endereco_origem VARCHAR(30),
    endereco_destino VARCHAR(30) NOT NULL,
    tipo_operacao VARCHAR(30) NOT NULL,          -- ENTRADA, ALOCACAO, REMOCAO, EXPEDICAO
    usuario_operador VARCHAR(50) NOT NULL DEFAULT 'SISTEMA',
    data_hora TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_auditoria_palete FOREIGN KEY (palete_id) REFERENCES palete (id)
);

-- Índices de consulta rápida
CREATE INDEX idx_endereco_ocupado ON endereco_estoque (ocupado);
CREATE INDEX idx_endereco_nivel ON endereco_estoque (nivel);
CREATE INDEX idx_palete_produto ON palete (produto_id);