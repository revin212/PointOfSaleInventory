-- Shift / cash drawer support.

CREATE TABLE shifts (
    id            UUID PRIMARY KEY,
    opened_by     UUID NOT NULL,
    opened_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    opening_cash  NUMERIC(18,2) NOT NULL DEFAULT 0,
    closed_by     UUID,
    closed_at     TIMESTAMP,
    closing_cash  NUMERIC(18,2),
    status        VARCHAR(20) NOT NULL,
    note          VARCHAR(500),
    CONSTRAINT fk_shifts_opened_by FOREIGN KEY (opened_by) REFERENCES users(id),
    CONSTRAINT fk_shifts_closed_by FOREIGN KEY (closed_by) REFERENCES users(id),
    CONSTRAINT ck_shifts_status CHECK (status IN ('OPEN','CLOSED'))
);
CREATE INDEX idx_shifts_opened_by_opened_at ON shifts(opened_by, opened_at);
CREATE INDEX idx_shifts_status_opened_at ON shifts(status, opened_at);

CREATE TABLE cash_movements (
    id         UUID PRIMARY KEY,
    shift_id   UUID NOT NULL,
    type       VARCHAR(10) NOT NULL,
    amount     NUMERIC(18,2) NOT NULL,
    note       VARCHAR(500),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_movements_shift FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cash_movements_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_cash_movements_type CHECK (type IN ('IN','OUT')),
    CONSTRAINT ck_cash_movements_amount CHECK (amount > 0)
);
CREATE INDEX idx_cash_movements_shift_created_at ON cash_movements(shift_id, created_at);

