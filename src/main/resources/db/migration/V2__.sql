ALTER TABLE users
    ADD activity_loc_firsts_place VARCHAR(255) NULL;

ALTER TABLE users
    ADD activity_loc_seconds_place VARCHAR(255) NULL;

ALTER TABLE users
    ADD age INT NULL;

ALTER TABLE users
    ADD birthday datetime NULL;

ALTER TABLE users
    ADD mobile_carrier VARCHAR(255) NULL;

ALTER TABLE users
    ADD name VARCHAR(255) NULL;

ALTER TABLE users
    ADD national_info VARCHAR(255) NULL;

ALTER TABLE users
    ADD residence_first_place VARCHAR(255) NULL;

ALTER TABLE users
    ADD residence_second_place VARCHAR(255) NULL;

ALTER TABLE blocked_users
    ADD admin_id BIGINT NULL;

ALTER TABLE blocked_users
    ADD CONSTRAINT FK_BLOCKED_USERS_ON_ADMIN FOREIGN KEY (admin_id) REFERENCES users (id);

ALTER TABLE users
    MODIFY is_drinking BIT (1) NULL;

ALTER TABLE users
    MODIFY is_smoking BIT (1) NULL;