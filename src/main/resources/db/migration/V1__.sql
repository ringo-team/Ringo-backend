CREATE TABLE answered_surveys
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    servery_num_id BIGINT NULL,
    answer         VARCHAR(255) NULL,
    CONSTRAINT pk_answered_surveys PRIMARY KEY (id)
);

CREATE TABLE blocked_friends
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    user_id      BIGINT NULL,
    phone_number VARCHAR(255) NULL,
    CONSTRAINT pk_blocked_friends PRIMARY KEY (id)
);

CREATE TABLE blocked_users
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    phone_number VARCHAR(255) NULL,
    CONSTRAINT pk_blocked_users PRIMARY KEY (id)
);

CREATE TABLE chatroom_participants
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    chatting_room_id    BIGINT NULL,
    participant_user_id BIGINT NULL,
    CONSTRAINT pk_chatroom_participants PRIMARY KEY (id)
);

CREATE TABLE chatrooms
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    chatroom_name VARCHAR(255) NULL,
    created_date  datetime NOT NULL,
    type          SMALLINT NULL,
    CONSTRAINT pk_chatrooms PRIMARY KEY (id)
);

CREATE TABLE dormant_accounts
(
    id      BIGINT AUTO_INCREMENT NOT NULL,
    user_id BIGINT NULL,
    CONSTRAINT pk_dormant_accounts PRIMARY KEY (id)
);

CREATE TABLE jwt_refresh_tokens
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    user_id       BIGINT NULL,
    rand          INT          NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    CONSTRAINT pk_jwt_refresh_tokens PRIMARY KEY (id)
);

CREATE TABLE matchings
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    request_user    BIGINT NULL,
    requested_user  BIGINT NULL,
    matching_date   datetime     NOT NULL,
    matching_status VARCHAR(255) NOT NULL,
    CONSTRAINT pk_matchings PRIMARY KEY (id)
);

CREATE TABLE messages
(
    id          VARCHAR(255) NOT NULL,
    created_at  datetime NULL,
    updated_at  datetime NULL,
    chatroom_id BIGINT NULL,
    user_id     BIGINT NULL,
    message     VARCHAR(255) NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT NULL,
    type        SMALLINT NULL,
    is_read     BIT(1) NOT NULL,
    is_finished BIT(1) NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE TABLE participation_logs
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        datetime NULL,
    updated_at        datetime NULL,
    user_id           BIGINT NULL,
    num_of_access     INT DEFAULT 0 NOT NULL,
    continuous_access INT           NOT NULL,
    CONSTRAINT pk_participationlogs PRIMARY KEY (id)
);

CREATE TABLE payment_transactions
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    amount         INT          NOT NULL,
    payment_status SMALLINT NULL,
    payment_method SMALLINT NULL,
    payment_date   datetime     NOT NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id)
);

CREATE TABLE photographer_user_mappings
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    photographer_id BIGINT NULL,
    user_id         BIGINT NULL,
    CONSTRAINT pk_photographer_user_mappings PRIMARY KEY (id)
);

CREATE TABLE profiles
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime NULL,
    updated_at    datetime NULL,
    user_id       BIGINT NULL,
    image_url     VARCHAR(255) NULL,
    `description` VARCHAR(255) NULL,
    sort_order    INT NULL,
    status        VARCHAR(255) NULL,
    CONSTRAINT pk_profiles PRIMARY KEY (id)
);

CREATE TABLE reports
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    report_user_id   BIGINT NULL,
    reported_user_id BIGINT NULL,
    CONSTRAINT pk_reports PRIMARY KEY (id)
);

CREATE TABLE surveys
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    survey_num INT NOT NULL,
    content    VARCHAR(255) NULL,
    CONSTRAINT pk_surveys PRIMARY KEY (id)
);

CREATE TABLE user_activity_logs
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      datetime NULL,
    updated_at      datetime NULL,
    user_id         BIGINT NULL,
    connect         BIGINT DEFAULT 0 NULL,
    matching_num    BIGINT NULL,
    match_req_num   BIGINT NULL,
    match_taken_num BIGINT NULL,
    CONSTRAINT pk_useractivitylogs PRIMARY KEY (id)
);

CREATE TABLE user_payment_logs
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT NULL,
    method     SMALLINT NULL,
    amount     INT      NOT NULL,
    pg         SMALLINT NULL,
    created_at datetime NOT NULL,
    CONSTRAINT pk_userpaymentlogs PRIMARY KEY (id)
);

CREATE TABLE user_points
(
    id      BIGINT AUTO_INCREMENT NOT NULL,
    user_id BIGINT NULL,
    point   INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_user_points PRIMARY KEY (id)
);

CREATE TABLE users
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime NULL,
    updated_at   datetime NULL,
    nickname     VARCHAR(255) NULL,
    email        VARCHAR(255) NULL,
    password     VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    device_token VARCHAR(255) NULL,
    gender       VARCHAR(255) NULL,
    height       VARCHAR(255) NULL,
    is_smoking   BIT(1) NOT NULL,
    is_drinking  BIT(1) NOT NULL,
    `role`       VARCHAR(255) NULL,
    religion     VARCHAR(255) NULL,
    job          VARCHAR(255) NULL,
    status       SMALLINT NULL,
    is_active    BIT(1) NOT NULL,
    etc          VARCHAR(255) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE withdrawers
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    join_period VARCHAR(255) NULL,
    reason      VARCHAR(255) NULL,
    CONSTRAINT pk_withdrawers PRIMARY KEY (id)
);

ALTER TABLE jwt_refresh_tokens
    ADD CONSTRAINT uc_jwt_refresh_tokens_user UNIQUE (user_id);

ALTER TABLE participation_logs
    ADD CONSTRAINT uc_participationlogs_user UNIQUE (user_id);

ALTER TABLE user_points
    ADD CONSTRAINT uc_user_points_user UNIQUE (user_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_devicetoken UNIQUE (device_token);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_phonenumber UNIQUE (phone_number);

ALTER TABLE answered_surveys
    ADD CONSTRAINT FK_ANSWERED_SURVEYS_ON_SERVERYNUM FOREIGN KEY (servery_num_id) REFERENCES surveys (id);

ALTER TABLE blocked_friends
    ADD CONSTRAINT FK_BLOCKED_FRIENDS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE chatroom_participants
    ADD CONSTRAINT FK_CHATROOM_PARTICIPANTS_ON_CHATTING_ROOM FOREIGN KEY (chatting_room_id) REFERENCES chatrooms (id);

ALTER TABLE chatroom_participants
    ADD CONSTRAINT FK_CHATROOM_PARTICIPANTS_ON_PARTICIPANT_USER FOREIGN KEY (participant_user_id) REFERENCES users (id);

ALTER TABLE dormant_accounts
    ADD CONSTRAINT FK_DORMANT_ACCOUNTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE jwt_refresh_tokens
    ADD CONSTRAINT FK_JWT_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE matchings
    ADD CONSTRAINT FK_MATCHINGS_ON_REQUESTED_USER FOREIGN KEY (requested_user) REFERENCES users (id);

ALTER TABLE matchings
    ADD CONSTRAINT FK_MATCHINGS_ON_REQUEST_USER FOREIGN KEY (request_user) REFERENCES users (id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGES_ON_CHATROOMID FOREIGN KEY (chatroom_id) REFERENCES chatrooms (id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGES_ON_USERID FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE participation_logs
    ADD CONSTRAINT FK_PARTICIPATIONLOGS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE photographer_user_mappings
    ADD CONSTRAINT FK_PHOTOGRAPHER_USER_MAPPINGS_ON_PHOTOGRAPHER FOREIGN KEY (photographer_id) REFERENCES users (id);

ALTER TABLE photographer_user_mappings
    ADD CONSTRAINT FK_PHOTOGRAPHER_USER_MAPPINGS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE profiles
    ADD CONSTRAINT FK_PROFILES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORTS_ON_REPORTED_USER FOREIGN KEY (reported_user_id) REFERENCES users (id);

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORTS_ON_REPORT_USER FOREIGN KEY (report_user_id) REFERENCES users (id);

ALTER TABLE user_activity_logs
    ADD CONSTRAINT FK_USERACTIVITYLOGS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_payment_logs
    ADD CONSTRAINT FK_USERPAYMENTLOGS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_points
    ADD CONSTRAINT FK_USER_POINTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);