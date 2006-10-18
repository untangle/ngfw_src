DELETE FROM tr_protofilter_evt WHERE time_stamp < (:cutoff)::timestamp;
