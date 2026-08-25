-- ============================================================
-- Pantry Manager Database Initialization
-- PostgreSQL
-- ============================================================

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    token_share VARCHAR(260) NOT NULL,
    fcm_token VARCHAR(512)
);

-- PANTRY
CREATE TABLE IF NOT EXISTS pantry (
    id SERIAL PRIMARY KEY,
    creator INTEGER NOT NULL,
    token_share VARCHAR(260) NOT NULL UNIQUE,
    kcal_threshold INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_pantry_creator
        FOREIGN KEY (creator)
        REFERENCES users(id)
);

-- PANTRY Share Requests
CREATE TABLE IF NOT EXISTS pantry_share_requests(
    id SERIAL PRIMARY KEY,
    pantry_id INTEGER NOT NULL,
    requesting_user_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_share_request_status
        CHECK (status IN ('pending', 'accepted', 'rejected')),

    CONSTRAINT fk_pantry_id
        FOREIGN KEY (pantry_id)
        REFERENCES pantry(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_requesting_user_id
        FOREIGN KEY (requesting_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
    name VARCHAR(100) NOT NULL,
    token_share VARCHAR(260) NOT NULL,

    PRIMARY KEY (name, token_share),

    CONSTRAINT fk_categories_pantry
        FOREIGN KEY (token_share)
        REFERENCES pantry(token_share)
);

-- PRODUCTS
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    "EAN" VARCHAR(15),
    unit VARCHAR(10) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL DEFAULT 0.0,
    category VARCHAR(50) NOT NULL,
    kcal INTEGER NOT NULL DEFAULT 0,
    token_share VARCHAR(260) NOT NULL,

    CONSTRAINT chk_unit
        CHECK (unit IN ('Kg', 'unit', 'L')),

    CONSTRAINT fk_products_pantry
        FOREIGN KEY (token_share)
        REFERENCES pantry(token_share),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category, token_share)
        REFERENCES categories(name, token_share)
);

-- EVENTS
CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    token_share VARCHAR(260) NOT NULL,
    product_id INTEGER NOT NULL,
    event_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kcal INTEGER NOT NULL DEFAULT 0,
    quantity NUMERIC(12,3) NOT NULL,
    unit VARCHAR(10) NOT NULL,

    CONSTRAINT fk_events_pantry
        FOREIGN KEY (token_share)
        REFERENCES pantry(token_share),

    CONSTRAINT fk_events_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),
        
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_users_username
    ON users(username);

CREATE INDEX IF NOT EXISTS idx_pantry_creator
    ON pantry(creator);

CREATE INDEX IF NOT EXISTS idx_share_requests_pantry
    ON pantry_share_requests(pantry_id);

CREATE INDEX IF NOT EXISTS idx_share_requests_user
    ON pantry_share_requests(requesting_user_id);

CREATE INDEX IF NOT EXISTS idx_share_requests_status
    ON pantry_share_requests(status);

CREATE INDEX IF NOT EXISTS idx_categories_token_share
    ON categories(token_share);

CREATE INDEX IF NOT EXISTS idx_products_ean
    ON products("EAN");

CREATE INDEX IF NOT EXISTS idx_products_token_share
    ON products(token_share);

CREATE INDEX IF NOT EXISTS idx_events_token_share
    ON events(token_share);

CREATE INDEX IF NOT EXISTS idx_events_product_id
    ON events(product_id);

CREATE INDEX IF NOT EXISTS idx_events_data
    ON events(event_date);