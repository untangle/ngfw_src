DELETE FROM tr_boxbackup_evt WHERE time_stamp < (:cutoff)::timestamp;
