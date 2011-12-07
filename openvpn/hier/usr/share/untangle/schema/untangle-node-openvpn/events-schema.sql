-- events schema for openvpn

CREATE TABLE events.n_openvpn_connect_evt (
        event_id INT8 NOT NULL,
        remote_address INET,
        remote_port    INT4,
        client_name    TEXT,
        rx_bytes       INT8,
        tx_bytes       INT8,
        time_stamp     TIMESTAMP,
        start_time     TIMESTAMP,
        end_time       TIMESTAMP,
        PRIMARY KEY    (event_id));

