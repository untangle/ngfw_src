CREATE TABLE transform_preferences (
    id int8 NOT NULL,
    tid int8,
    red int4,
    green int4,
    blue int4,
    alpha int4,
    PRIMARY KEY (id));

CREATE TABLE transform_persistent_state (
    id int8 NOT NULL,
    tid int8,
    name varchar(64) NOT NULL,
    public_key bytea NOT NULL,
    target_state varchar(255) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE mvvm_login_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    login varchar(255),
    local bool,
    succeeded bool,
    reason char(1),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

INSERT INTO transform_preferences
    (SELECT id, tid, red, green, blue, alpha FROM transform_desc);
INSERT INTO transform_persistent_state
    (SELECT id, tid, name, public_key, target_state FROM transform_desc);

UPDATE transform_preferences SET id = nextval('hibernate_sequence');

ALTER TABLE transform_args DROP CONSTRAINT FK1C0835F0A8A3B796;
ALTER TABLE transform_desc DROP CONSTRAINT FK1C0963A41446F;
DROP TABLE transform_desc;

ALTER TABLE transform_args RENAME COLUMN transform_desc_id TO tps_id;

ALTER TABLE transform_args ADD CONSTRAINT FK1C0835F0A8A3B796
    FOREIGN KEY (tps_id) REFERENCES transform_persistent_state;

ALTER TABLE transform_preferences ADD CONSTRAINT FKE8B6BA651446F
    FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE transform_persistent_state ADD CONSTRAINT FKA67B855C1446F
    FOREIGN KEY (tid) REFERENCES tid;

