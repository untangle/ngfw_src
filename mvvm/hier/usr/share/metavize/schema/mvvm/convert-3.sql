-- convert script for release 1.5

-- indexes for reporting

CREATE INDEX pipeline_info_sid on pipeline_info (session_id);

-- Table for shield events
create table shield_evt (
        event_id int8 not null,
        ip inet,
        reputation float8,
        mode int4,
        limited int4,
        rejected int4,
        dropped int4,
        time_stamp timestamp,
        PRIMARY KEY (event_id));
