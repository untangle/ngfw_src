-- events converter for release 3.1.3

----------------
-- constraints |
----------------

-- foreign key constraints
ALTER TABLE events.tr_mail_message_info_addr
    DROP CONSTRAINT fk_trml_msginfoaddr_to_msginfo;
