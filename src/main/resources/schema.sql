CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS homeowners (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS tradespeople (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    base_zip VARCHAR(20) NOT NULL,
    service_radius INTEGER NOT NULL,
    availability_status VARCHAR(32) NOT NULL CHECK (availability_status IN ('AVAILABLE_NOW', 'AVAILABLE_SOON', 'BUSY', 'NOT_ACCEPTING_WORK')),
    available_start_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS trades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS specialties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    trade_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    UNIQUE (trade_id, name),
    FOREIGN KEY (trade_id) REFERENCES trades(id)
);

CREATE TABLE IF NOT EXISTS person_trades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tradesperson_id INTEGER NOT NULL,
    trade_id INTEGER NOT NULL,
    UNIQUE (tradesperson_id, trade_id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id),
    FOREIGN KEY (trade_id) REFERENCES trades(id)
);

CREATE TABLE IF NOT EXISTS person_specialties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tradesperson_id INTEGER NOT NULL,
    specialty_id INTEGER NOT NULL,
    UNIQUE (tradesperson_id, specialty_id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id),
    FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);

CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    job_zip VARCHAR(20) NOT NULL,
    homeowner_id INTEGER NOT NULL,
    FOREIGN KEY (homeowner_id) REFERENCES homeowners(id)
);

CREATE TABLE IF NOT EXISTS project_teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    tradesperson_id INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('INVITED', 'PENDING', 'ACTIVE', 'SUSPENDED')),
    UNIQUE (project_id, tradesperson_id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id)
);

CREATE TABLE IF NOT EXISTS project_team_trades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_team_id INTEGER NOT NULL,
    trade_id INTEGER NOT NULL,
    UNIQUE (project_team_id, trade_id),
    FOREIGN KEY (project_team_id) REFERENCES project_teams(id),
    FOREIGN KEY (trade_id) REFERENCES trades(id)
);

CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    project_id INTEGER NOT NULL,
    parent_task_id INTEGER,
    CHECK (parent_task_id IS NULL OR parent_task_id <> id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (parent_task_id) REFERENCES tasks(id)
);

CREATE TABLE IF NOT EXISTS task_trades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    trade_id INTEGER NOT NULL,
    UNIQUE (task_id, trade_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (trade_id) REFERENCES trades(id)
);

CREATE TABLE IF NOT EXISTS task_assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    tradesperson_id INTEGER NOT NULL,
    UNIQUE (task_id, tradesperson_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    task_trade_id INTEGER NOT NULL,
    tradesperson_id INTEGER NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    message VARCHAR(2000),
    status VARCHAR(32) NOT NULL CHECK (status IN ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (task_trade_id) REFERENCES task_trades(id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_bids_submitted_task_trade_tradesperson
    ON bids (task_trade_id, tradesperson_id)
    WHERE status = 'SUBMITTED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_bids_accepted_task_trade
    ON bids (task_trade_id)
    WHERE status = 'ACCEPTED';