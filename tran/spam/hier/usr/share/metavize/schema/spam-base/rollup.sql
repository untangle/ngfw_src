DELETE FROM tr_spam_evt_smtp WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spam_evt WHERE time_stamp < (:cutoff)::timestamp;
