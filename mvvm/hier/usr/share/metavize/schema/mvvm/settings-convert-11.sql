-- settings conversion for release 5.0

UPDATE settings.user_policy_rule SET user_matcher = '[any]' WHERE user_matcher = 'all';
UPDATE settings.user_policy_rule SET user_matcher = '[any]' WHERE user_matcher = 'any';

-- com.untangle.mvvm.BrandingSettings
CREATE TABLE settings.mvvm_branding_settings (
    settings_id int8 NOT NULL,
    company_name text,
    company_url text,
    logo bytea,
    contact_name text,
    contact_email text,
    PRIMARY KEY (settings_id));
