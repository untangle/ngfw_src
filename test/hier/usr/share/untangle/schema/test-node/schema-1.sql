-- schema for release-2.5

-------------
-- settings |
-------------

-- com.untangle.tran.test.TestSettings
CREATE TABLE settings.n_test_settings (
    id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    mode varchar(255),
    buffered bool,
    normal bool,
    release bool,
    quiet bool,
    min_random_buffer_size int4,
    max_random_buffer_size int4,
    PRIMARY KEY (ID));

----------------
-- constraints |
----------------

-- foreign key constraints

ALTER TABLE settings.n_test_settings
    ADD CONSTRAINT fk_tr_test_settings
    FOREIGN KEY (tid) REFERENCES settings.u_tid;

