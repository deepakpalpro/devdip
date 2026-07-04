-- Configurable form-import providers. Which engine extracts a given source type — and whether an
-- external OCR/LLM provider is used at all — is data-driven and managed from the admin Settings UI,
-- not hard-coded. `code` matches a FormExtractor bean; `config_json` holds non-secret settings
-- (endpoint/model/secretRef) — secrets themselves are never stored here.
CREATE TABLE form_import_provider (
    id            BINARY(16) PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    name          VARCHAR(128) NOT NULL,
    source_type   VARCHAR(32) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    priority      INT NOT NULL DEFAULT 100,
    config_json   CLOB,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_form_import_provider_type ON form_import_provider (source_type, enabled, priority);

-- Deterministic, in-JVM providers: enabled by default.
INSERT INTO form_import_provider (id, code, name, source_type, enabled, priority, config_json) VALUES
    (X'a0000000000000000000000000000001', 'pdfbox',         'PDFBox (PDF AcroForm + text)',   'PDF',         TRUE,  10, NULL),
    (X'a0000000000000000000000000000002', 'csv',            'CSV header parser',              'CSV',         TRUE,  10, NULL),
    (X'a0000000000000000000000000000003', 'poi-spreadsheet','Apache POI (XLS/XLSX)',          'SPREADSHEET', TRUE,  10, NULL),
    (X'a0000000000000000000000000000004', 'jsoup-html',     'jsoup HTML form parser',         'HTML',        TRUE,  10, NULL);

-- External / AI IMAGE providers: present but DISABLED until an admin configures + enables them.
-- ollama-vision has a working implementation (module-service-integration) and is preferred (lower
-- priority number); llm-vision is a generic hosted seam kept for reference.
INSERT INTO form_import_provider (id, code, name, source_type, enabled, priority, config_json) VALUES
    (X'a0000000000000000000000000000005', 'ollama-vision',  'Ollama Vision (llava)',          'IMAGE',       FALSE,  5,
     '{"endpoint":"http://localhost:11434","model":"llava","timeoutSeconds":120}'),
    (X'a0000000000000000000000000000006', 'llm-vision',     'LLM Vision (hosted)',            'IMAGE',       FALSE, 20,
     '{"endpoint":"","model":"","secretRef":"FORM_IMPORT_LLM_API_KEY"}');
