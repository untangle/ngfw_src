DELETE FROM tr_spyware_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;
