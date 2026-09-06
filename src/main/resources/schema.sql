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

CREATE TABLE IF NOT EXISTS message_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    requester_id INTEGER NOT NULL,
    recipient_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    CHECK (requester_id <> recipient_id),
    FOREIGN KEY (requester_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_message_requests_pending_context_pair
    ON message_requests (MIN(requester_id, recipient_id), MAX(requester_id, recipient_id), project_id)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(32) NOT NULL CHECK (type IN ('PRIVATE', 'PROJECT_TEAM')),
    project_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_conversations_project_team
    ON conversations (project_id) WHERE type = 'PROJECT_TEAM';

CREATE TABLE IF NOT EXISTS conversation_participants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    UNIQUE (conversation_id, user_id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    sender_id INTEGER NOT NULL,
    body VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP,
    removed_at TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS message_attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id INTEGER NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size INTEGER NOT NULL CHECK (file_size >= 0),
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    uploader_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (message_id) REFERENCES messages(id),
    FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    homeowner_id INTEGER NOT NULL,
    tradesperson_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    task_id INTEGER,
    level VARCHAR(16) NOT NULL CHECK (level IN ('TASK', 'PROJECT')),
    overall_rating INTEGER NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    quality_rating INTEGER CHECK (quality_rating BETWEEN 1 AND 5),
    communication_rating INTEGER CHECK (communication_rating BETWEEN 1 AND 5),
    reliability_rating INTEGER CHECK (reliability_rating BETWEEN 1 AND 5),
    professionalism_rating INTEGER CHECK (professionalism_rating BETWEEN 1 AND 5),
    body VARCHAR(10000),
    created_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    moderation_hidden_at TIMESTAMP,
    CHECK ((level = 'TASK' AND task_id IS NOT NULL) OR (level = 'PROJECT' AND task_id IS NULL)),
    FOREIGN KEY (homeowner_id) REFERENCES homeowners(id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_task_context
    ON reviews(homeowner_id, tradesperson_id, task_id) WHERE level = 'TASK';
CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_project_context
    ON reviews(homeowner_id, tradesperson_id, project_id) WHERE level = 'PROJECT';

CREATE TABLE IF NOT EXISTS review_responses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    review_id INTEGER NOT NULL UNIQUE,
    tradesperson_id INTEGER NOT NULL,
    body VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP,
    moderation_hidden_at TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id)
);

CREATE TABLE IF NOT EXISTS moderation_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_id INTEGER NOT NULL,
    target_type VARCHAR(32) NOT NULL CHECK (target_type IN ('REVIEW', 'REVIEW_RESPONSE')),
    review_id INTEGER,
    response_id INTEGER,
    reason VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    CHECK ((target_type = 'REVIEW' AND review_id IS NOT NULL AND response_id IS NULL)
        OR (target_type = 'REVIEW_RESPONSE' AND response_id IS NOT NULL AND review_id IS NULL)),
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    FOREIGN KEY (review_id) REFERENCES reviews(id),
    FOREIGN KEY (response_id) REFERENCES review_responses(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_report_review
    ON moderation_reports(reporter_id, review_id) WHERE review_id IS NOT NULL AND status IN ('OPEN','UNDER_REVIEW');
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_report_response
    ON moderation_reports(reporter_id, response_id) WHERE response_id IS NOT NULL AND status IN ('OPEN','UNDER_REVIEW');

CREATE TABLE IF NOT EXISTS portfolio_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tradesperson_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(5000),
    provenance VARCHAR(32) NOT NULL CHECK (provenance IN ('RHP_VERIFIED','EXTERNALLY_VERIFIED','SELF_REPORTED')),
    project_id INTEGER,
    task_id INTEGER,
    completion_date DATE,
    created_at TIMESTAMP NOT NULL,
    CHECK (task_id IS NULL OR project_id IS NOT NULL),
    CHECK (provenance <> 'RHP_VERIFIED' OR project_id IS NOT NULL),
    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);
CREATE INDEX IF NOT EXISTS idx_portfolio_items_owner_provenance ON portfolio_items(tradesperson_id, provenance);

CREATE TABLE IF NOT EXISTS portfolio_publication_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    portfolio_item_id INTEGER NOT NULL,
    attachment_id INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','APPROVED','DECLINED','CANCELLED')),
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    decided_by_homeowner_id INTEGER,
    UNIQUE (portfolio_item_id, attachment_id),
    FOREIGN KEY (portfolio_item_id) REFERENCES portfolio_items(id),
    FOREIGN KEY (attachment_id) REFERENCES message_attachments(id),
    FOREIGN KEY (decided_by_homeowner_id) REFERENCES homeowners(id)
);
CREATE INDEX IF NOT EXISTS idx_portfolio_publication_status ON portfolio_publication_requests(status);
