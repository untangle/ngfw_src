DELETE FROM tr_openvpn_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_openvpn_connect_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_openvpn_distr_evt WHERE time_stamp < (:cutoff)::timestamp;
