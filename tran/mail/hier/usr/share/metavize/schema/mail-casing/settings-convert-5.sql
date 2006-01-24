-- settings converter for release 3.2



-- com.metavize.tran.mail.papi.EmailAddressPair
CREATE TABLE settings.email_address_pair (
    pair_id int8 NOT NULL,
    address1 text,
    address2 text,
    position int4,
    settings_id int8,
    PRIMARY KEY (pair_id));

-- com.metavize.tran.mail.papi.EmailAddressWrapper
CREATE TABLE settings.email_address_wrapper (
    addr_id int8 NOT NULL,
    address text,
    position int4,
    settings_id int8,
    PRIMARY KEY (addr_id));       
    
