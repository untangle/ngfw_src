-- 9.2

CREATE INDEX n_webfilter_evt_time_stamp_idx ON events.n_webfilter_evt (time_stamp);
CREATE INDEX n_webfilter_evt_request_id_idx ON events.n_webfilter_evt (request_id);
