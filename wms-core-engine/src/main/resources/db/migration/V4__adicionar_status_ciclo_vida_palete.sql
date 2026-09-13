-- Adiciona colunas para controle de ciclo de vida e auditoria de expedição
ALTER TABLE palete ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'RECEBIDO_DOCA';
ALTER TABLE palete ADD COLUMN expedido_em TIMESTAMP WITHOUT TIME ZONE;

-- Atualiza os registros que já estão alocados atualmente
UPDATE palete
SET status = 'ARMAZENADO'
WHERE endereco_id IS NOT NULL;