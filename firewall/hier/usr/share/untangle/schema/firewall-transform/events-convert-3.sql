-- events convert for release 3.1

----------------------
-- PipelineEndpoints |
----------------------

DROP TABLE events.tr_firewall_tmp;

CREATE TABLE events.tr_firewall_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, was_blocked,
          rule_id, rule_index, evt.time_stamp
    FROM events.tr_firewall_evt evt JOIN events.pl_endp endp USING (session_id);

DROP TABLE events.tr_firewall_evt;
ALTER TABLE events.tr_firewall_tmp RENAME TO tr_firewall_evt;
ALTER TABLE events.tr_firewall_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_firewall_evt ADD PRIMARY KEY (event_id);

DROP INDEX tr_firewall_evt_sid_idx;
CREATE INDEX tr_firewall_evt_plepid_idx ON events.tr_firewall_evt (pl_endp_id);
