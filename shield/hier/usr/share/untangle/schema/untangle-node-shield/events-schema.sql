-- com.untangle.mvvm.shield.ShieldRejectionEvent
CREATE TABLE events.n_shield_rejection_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    client_intf int2,
    reputation float8,
    mode int4,
    limited int4,
    dropped int4,
    rejected int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.shield.ShieldStatisticEvent
CREATE TABLE events.n_shield_statistic_evt (
    event_id int8 NOT NULL,
    accepted int4,
    limited  int4,
    dropped  int4,
    rejected int4,
    relaxed  int4,
    lax      int4,
    tight    int4,
    closed   int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE INDEX n_shield_rejection_evt_time_stamp_idx ON n_shield_rejection_evt (time_stamp);
