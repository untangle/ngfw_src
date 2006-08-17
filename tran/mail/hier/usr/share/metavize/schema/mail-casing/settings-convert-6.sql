-- settings converter for release 4.0.1

-- Adjust the maximum quarantine size to 10 GBs
UPDATE settings.tr_mail_quarantine_settings SET max_quarantine_sz=10000000000;
