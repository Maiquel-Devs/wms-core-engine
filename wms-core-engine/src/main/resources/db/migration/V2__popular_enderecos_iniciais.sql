-- Carga inicial de endereços físicos para simulação de armazém logístico
-- Estrutura: Rua (R) - Bloco (B) - Nível (N) - Posição (P)

-- RUA 01: Produtos Gerais / Carga Seca (Nível 1 = Chão com alta capacidade; Níveis 2 e 3 = Estantes)
-- Bloco 01
INSERT INTO endereco_estoque (codigo_endereco, rua, bloco, nivel, posicao, capacidade_peso_kg, capacidade_volume_m3, ocupado, version)
VALUES
    ('R01-B01-N01-P01', 'R01', 'B01', 1, 1, 1500.00, 3.5000, FALSE, 0),
    ('R01-B01-N01-P02', 'R01', 'B01', 1, 2, 1500.00, 3.5000, FALSE, 0),
    ('R01-B01-N02-P01', 'R01', 'B01', 2, 1, 450.00, 2.0000, FALSE, 0),
    ('R01-B01-N02-P02', 'R01', 'B01', 2, 2, 450.00, 2.0000, FALSE, 0),
    ('R01-B01-N03-P01', 'R01', 'B01', 3, 1, 300.00, 1.8000, FALSE, 0),
    ('R01-B01-N03-P02', 'R01', 'B01', 3, 2, 300.00, 1.8000, FALSE, 0);

-- Bloco 02
INSERT INTO endereco_estoque (codigo_endereco, rua, bloco, nivel, posicao, capacidade_peso_kg, capacidade_volume_m3, ocupado, version)
VALUES
    ('R01-B02-N01-P01', 'R01', 'B02', 1, 1, 1500.00, 3.5000, FALSE, 0),
    ('R01-B02-N01-P02', 'R01', 'B02', 1, 2, 1500.00, 3.5000, FALSE, 0),
    ('R01-B02-N02-P01', 'R01', 'B02', 2, 1, 450.00, 2.0000, FALSE, 0),
    ('R01-B02-N02-P02', 'R01', 'B02', 2, 2, 450.00, 2.0000, FALSE, 0),
    ('R01-B02-N03-P01', 'R01', 'B02', 3, 1, 300.00, 1.8000, FALSE, 0),
    ('R01-B02-N03-P02', 'R01', 'B02', 3, 2, 300.00, 1.8000, FALSE, 0);

-- RUA 02: Posições Adicionais para Cenários de Concorrência e Estresse
INSERT INTO endereco_estoque (codigo_endereco, rua, bloco, nivel, posicao, capacidade_peso_kg, capacidade_volume_m3, ocupado, version)
VALUES
    ('R02-B01-N01-P01', 'R02', 'B01', 1, 1, 2000.00, 4.0000, FALSE, 0),
    ('R02-B01-N01-P02', 'R02', 'B01', 1, 2, 2000.00, 4.0000, FALSE, 0),
    ('R02-B01-N02-P01', 'R02', 'B01', 2, 1, 500.00, 2.5000, FALSE, 0),
    ('R02-B01-N02-P02', 'R02', 'B01', 2, 2, 500.00, 2.5000, FALSE, 0);

-- Inserção de produtos mestre para testes operacionais
INSERT INTO produto (codigo_sku, nome, descricao, categoria_risco, recomendacao_armazenagem, peso_unitario_kg, volume_unitario_m3)
VALUES
    ('SKU-MOT-001', 'Motor Elétrico Industrial Trifásico', 'Equipamento de alta densidade de peso', 'PADRAO', 'Manter em piso nivelado. Não empilhar mais que 2 unidades.', 650.00, 1.2000),
    ('SKU-CXA-002', 'Lote de Caixas Plásticas Vazias', 'Carga volumosa de baixo peso', 'PADRAO', 'Posicionamento livre em níveis intermediários ou superiores.', 120.00, 2.4000),
    ('SKU-SOL-003', 'Solvente Industrial Grau Técnico', 'Produto químico de risco inflamável', 'INFLAMAVEL', 'Armazenar em área com ventilação natural. Longe de pontos de calor.', 280.00, 1.1000);