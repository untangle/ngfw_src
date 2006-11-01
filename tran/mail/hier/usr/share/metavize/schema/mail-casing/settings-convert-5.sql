-- settings converter for release 3.2

ALTER TABLE settings.tr_mail_quarantine_settings
    DROP COLUMN digest_from;



-- com.untangle.mvvm.tran.EmailAddressRule
CREATE TABLE settings.email_addr_rule (
    rule_id int8 NOT NULL,
    address text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,     
    PRIMARY KEY (rule_id));    

-- com.untangle.tran.mail.papi.EmailAddressPairRule
CREATE TABLE settings.email_addr_pair_rule (
    rule_id int8 NOT NULL,
    address1 text,
    address2 text,
    name text,
    category text,
    description text,
    live bool,
    alert bool,
    log bool,
    position int4,
    settings_id int8,    
    PRIMARY KEY (rule_id));      
    
