-- events convert for release 3.1

-------------------------------
-- refer to PipelineEndpoints |
-------------------------------

DROP TABLE events.tr_nat_tmp;

CREATE TABLE events.tr_nat_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, rule_id,
           rule_index, is_dmz, evt.time_stamp
    FROM events.tr_nat_redirect_evt evt JOIN pl_endp endp USING (session_id);

DROP TABLE events.tr_nat_redirect_evt;
ALTER TABLE events.tr_nat_tmp RENAME TO tr_nat_redirect_evt;
ALTER TABLE events.tr_nat_redirect_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_nat_redirect_evt ADD PRIMARY KEY (event_id);

-- indices for reporting

DROP INDEX tr_nat_redirect_evt_sess_idx;
CREATE INDEX tr_nat_redirect_evt_plepid_idx ON events.tr_nat_redirect_evt (pl_endp_id);

-- SET NOT NULL

ALTER TABLE tr_nat_evt_dhcp_abs_leases ALTER COLUMN lease_id SET NOT NULL;

-- rename constraints

ALTER TABLE tr_nat_evt_dhcp_abs_leases DROP CONSTRAINT tr_nat_evt_dhcp_abs_leasespkey;
ALTER TABLE tr_nat_evt_dhcp_abs_leases ADD CONSTRAINT tr_nat_evt_dhcp_abs_leases_pkey PRIMARY KEY (event_id, "position");
