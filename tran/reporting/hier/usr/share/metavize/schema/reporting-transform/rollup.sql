DELETE FROM mvvm_login_evt WHERE time_stamp < (:cutoff)::timestamp;
