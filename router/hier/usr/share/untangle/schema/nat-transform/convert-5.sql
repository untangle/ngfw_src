-- convert script for release 3.0

-- indeces for reporting

CREATE INDEX tr_nat_redirect_evt_sess_idx ON tr_nat_redirect_evt (session_id);

