CREATE TABLE tr_test_settings (
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

ALTER TABLE tr_test_settings ADD CONSTRAINT FKEB1F4D2F1446F FOREIGN KEY (tid) REFERENCES tid;

