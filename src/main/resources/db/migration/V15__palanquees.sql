-- V15: Organisation des palanquées par créneau
-- Un directeur de plongée peut créer des palanquées et y répartir les plongeurs inscrits.
-- Migrations rendues idempotentes (IF NOT EXISTS) car elles peuvent avoir été
-- exécutées sans être enregistrées dans flyway_schema_history (suite à un repair).

CREATE TABLE IF NOT EXISTS palanquees (
    id         BIGSERIAL PRIMARY KEY,
    slot_id    BIGINT       NOT NULL REFERENCES dive_slots(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    position   INT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_palanquees_slot_id ON palanquees(slot_id);

-- Lien optionnel : un plongeur peut être assigné à une palanquée (NULL = non assigné)
ALTER TABLE slot_divers
    ADD COLUMN IF NOT EXISTS palanquee_id BIGINT REFERENCES palanquees(id) ON DELETE SET NULL;
