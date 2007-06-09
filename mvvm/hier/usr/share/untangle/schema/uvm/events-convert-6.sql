-- events conversion for release-3.2

-- com.untangle.mvvm.engine.TransformStateChange
CREATE TABLE events.transform_state_change (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    tid int8 NOT NULL,
    state text NOT NULL,
    PRIMARY KEY (event_id));
