-- settings conversion for release 5.0

-- com.untangle.mvvm.BrandingSettings
CREATE TABLE settings.mvvm_branding_settings (
    settings_id int8 NOT NULL,
    company_name text,
    logo bytea,
    contact_name text,
    contact_email text,
    PRIMARY KEY (settings_id));
