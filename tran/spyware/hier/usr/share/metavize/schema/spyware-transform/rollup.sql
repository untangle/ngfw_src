DELETE FROM tr_spyware_evt_activex WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spyware_evt_access WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spyware_evt_cookie WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_spyware_evt_blacklist WHERE time_stamp < (:cutoff)::timestamp;
