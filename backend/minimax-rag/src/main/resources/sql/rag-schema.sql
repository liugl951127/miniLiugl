-- RAG 模块 H2 沙箱数据库初始化
-- V7.1: 预计算 MockEmbedding (64维, seed=0xC0FFEE) 并存储, 让 RAG 检索可工作

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT,
    tenant_id BIGINT,
    name VARCHAR(255),
    description TEXT,
    visibility VARCHAR(16) DEFAULT 'private',
    doc_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    tags VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    owner_id BIGINT,
    title VARCHAR(255),
    source_type VARCHAR(32),
    source_url VARCHAR(512),
    file_size BIGINT,
    chunk_count INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'pending',
    error_message TEXT,
    tags VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    owner_id BIGINT,
    content TEXT,
    source VARCHAR(255),
    chunk_index INT,
    token_count INT DEFAULT 0,
    embedding BINARY(2048),
    dim INT DEFAULT 0,
    char_count INT DEFAULT 0,
    start_pos INT DEFAULT 0,
    end_pos INT DEFAULT 0,
    access_count INT DEFAULT 0,
    last_access_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- 种子测试数据 (embedding = MockEmbeddingClient 预计算值, dim=64)
INSERT INTO knowledge_base (id, owner_id, name, description, visibility, doc_count, chunk_count)
VALUES (1, 0, '测试知识库', '用于 Flow① RAG 测试', 'public', 1, 2);

INSERT INTO document (id, kb_id, owner_id, title, source_type, status, chunk_count)
VALUES (1, 1, 0, 'AI入门指南', 'manual', 'completed', 2);

INSERT INTO document_chunk (id, doc_id, kb_id, owner_id, content, source, chunk_index, dim, char_count, access_count, embedding)
VALUES (1, 1, 1, 0,
    '机器学习是人工智能的一个分支，它使用统计学方法让计算机系统能够从数据中学习并改进性能。常见的机器学习算法包括监督学习、无监督学习和强化学习。',
    'AI入门指南', 0, 64, 120, 0,
    CAST(X'3e3bf2f4bd4896ae3e4db1743ca95bd73daec41c3e5f769dbe1088b03cd9c19cbc9db580be492a913ecaa7a0be357af03e2a9a90bdccbb2d3e394fc8bdc9ee02bd1c97a63d6384213d71fcdf3dada0953ca4691fbd8fcdc3bdf357d2be4ffb93be0e434cbd3e83f43dfcb8293e9663603d53c1eabdce1e6f3d5124f1bbf7f1a3bc0329183e0dc2243c03d5eebd829c79bdad4f243e0e5ee63e3a48c4bd8b32163dc56bb93d9c9f153d7b3ac93e6f55233d6c70663d3377d4bd03078b3d7b59613dcb008fbd284bad3c8814ef3d7b1292be4fe0843d4a477b3df61f0b3c3b6740be1d46cd3d7f73713c6be4d03dd6d201bdd539d5bce3bca3bd838315be27294e' AS BINARY));

INSERT INTO document_chunk (id, doc_id, kb_id, owner_id, content, source, chunk_index, dim, char_count, access_count, embedding)
VALUES (2, 1, 1, 0,
    '深度学习是机器学习的一个子领域，使用多层神经网络来学习数据的表征。著名的深度学习模型包括卷积神经网络（CNN）和循环神经网络（RNN），广泛应用于图像识别和自然语言处理。',
    'AI入门指南', 1, 64, 130, 0,
    CAST(X'3e34271abd40449b3ea5b4523ca255663da784403e5631a2be0a89dd3cd0b93bbc972ac5be40d25c3ec23f9cbe2df3c73e2386e7bdc43d1d3e319fefbdc18db2bd1618c73d5a141c3d67f32d3da66cd13c9d973abd89d6b1bde93fbcbe475afcbe085c97bd369cd93df23c813e9026633d4af93cbdc591a23d487803bbeda8b2bbfb70923e07e0cb3bfcbbe7bd7a62febda61ec03e08770c3e328e97bd856bf43dbd3b4a3d961fe73d70cef33e6567a33d62a1a03d2c060abcfb30413d70ec463dc294dbbd2150863c826fde3d70a867be47410c3d41e3753debe9743c33a131be16c0aa3d74dac8bdf03b743dcde8cdbdcc6190bcda4a46bd7c1d16be203a33' AS BINARY));
