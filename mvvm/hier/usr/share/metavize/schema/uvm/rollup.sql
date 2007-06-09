DELETE FROM shield_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;
