CREATE TABLE tr_email_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    control int8 NOT NULL,
    spam_inbound int8 NOT NULL,
    spam_outbound int8 NOT NULL,
    virus_inbound int8 NOT NULL,
    virus_outbound int8 NOT NULL,
    PRIMARY KEY (settings_id));

CREATE TABLE tr_email_ml_definition (
    def_id int8 NOT NULL,
    action char(1) NOT NULL,
    field_type char(1) NOT NULL,
    value varchar(255) NOT NULL,
    exch_value varchar(255),
    copy_on_block bool NOT NULL,
    notes varchar(255),
    settings_id int8,
    position int4,
    PRIMARY KEY (def_id));

CREATE TABLE tr_email_ssctl_definition (
    def_id int8 NOT NULL,
    scan_strength varchar(255),
    action_on_detect char(1) NOT NULL,
    scan bool,
    copy_on_block bool,
    notes varchar(255),
    copy_on_block_details varchar(255),
    PRIMARY KEY (def_id));

CREATE TABLE tr_email_vsctl_definition (
    def_id int8 NOT NULL,
    action_on_detect char(1) NOT NULL,
    scan bool,
    copy_on_block bool,
    notes varchar(255),
    copy_on_block_details varchar(255),
    PRIMARY KEY (def_id));

CREATE TABLE tr_email_ctl_definition (
    def_id int8 NOT NULL,
    spam_scanner char(1) NOT NULL,
    virus_scanner char(1) NOT NULL,
    pop3_postmaster varchar(255),
    imap4_postmaster varchar(255),
    copy_on_exception bool,
    msg_sz_limit int4,
    spam_msg_sz_limit int4,
    virus_msg_sz_limit int4,
    return_err_on_smtp_block bool,
    return_err_on_pop3_block bool,
    return_err_on_imap4_block bool,
    details_imap4_postmaster varchar(255),
    details_pop3_postmaster varchar(255),
    details_msg_sz_limit varchar(255),
    details_spam_sz_limit varchar(255),
    details_virus_sz_limit varchar(255),
    details_alerts varchar(255),
    details_log varchar(255),
    PRIMARY KEY (def_id));

CREATE TABLE tr_email_handler_info (
    id int8 NOT NULL,
    s_id int4,
    svr_type char(1) NOT NULL,
    user_name varchar(255),
    srv_greeting varchar(1024) NOT NULL,
    PRIMARY KEY (id));

CREATE TABLE tr_email_message_info (
    id int8 NOT NULL,
    hdl_id int8,
    size int4 NOT NULL,
    smtp_sender varchar(1024),
    smtp_recipient varchar(1024),
    rfc822_from varchar(1024),
    rfc822_to_list varchar(1024),
    rfc822_cc_list varchar(1024),
    rfc822_bcc_list varchar(1024),
    rfc822_subject varchar(1024),
    time_stamp timestamp,
    PRIMARY KEY (id));

CREATE TABLE tr_email_szlimit_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_email_spam_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    action char(1) NOT NULL,
    score varchar(255) NOT NULL,
    tests varchar(1024),
    is_spam bool NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_email_virus_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    action char(1) NOT NULL,
    name varchar(255) NOT NULL,
    is_clean bool NOT NULL,
    disinfected bool NOT NULL,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_email_custom_evt (
    event_id int8 NOT NULL,
    msg_id int8,
    action char(1) NOT NULL,
    field_type char(1) NOT NULL,
    pattern varchar(255) NOT NULL,
    exch_value varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D671446F
 FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_email_ml_definition
 ADD CONSTRAINT FK245E4D2F79192AB7
 FOREIGN KEY (settings_id)
 REFERENCES tr_email_settings;

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D67D0636463
 FOREIGN KEY (spam_inbound)
 REFERENCES tr_email_ssctl_definition;

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D673B993F26
 FOREIGN KEY (spam_outbound)
 REFERENCES tr_email_ssctl_definition;

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D675C97F3F7
 FOREIGN KEY (virus_inbound)
 REFERENCES tr_email_vsctl_definition;

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D6735F6A212
 FOREIGN KEY (virus_outbound)
 REFERENCES tr_email_vsctl_definition;

ALTER TABLE tr_email_settings
 ADD CONSTRAINT FK27C00D676382F13D
 FOREIGN KEY (control)
 REFERENCES tr_email_ctl_definition;

ALTER TABLE tr_email_message_info
 ADD CONSTRAINT FK99F2014A7EBF24EA
 FOREIGN KEY (hdl_id)
 REFERENCES tr_email_handler_info
 ON DELETE CASCADE;

ALTER TABLE tr_email_szlimit_evt
 ADD CONSTRAINT FK835BECD488187AB9
 FOREIGN KEY (msg_id)
 REFERENCES tr_email_message_info
 ON DELETE CASCADE;

ALTER TABLE tr_email_spam_evt
 ADD CONSTRAINT FK4CDFC11188187AB9
 FOREIGN KEY (msg_id)
 REFERENCES tr_email_message_info
 ON DELETE CASCADE;

ALTER TABLE tr_email_virus_evt
 ADD CONSTRAINT FKC510A09D88187AB9
 FOREIGN KEY (msg_id)
 REFERENCES tr_email_message_info
 ON DELETE CASCADE;

ALTER TABLE tr_email_custom_evt
 ADD CONSTRAINT FKD9FABC3988187AB9
 FOREIGN KEY (msg_id)
 REFERENCES tr_email_message_info
 ON DELETE CASCADE;
