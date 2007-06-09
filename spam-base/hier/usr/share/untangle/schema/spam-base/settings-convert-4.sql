-- settings convert for release 4.1

-- turn off throttle
UPDATE tr_spam_smtp_config SET throttle = false;

-- change spam strength definitions
UPDATE tr_spam_smtp_config SET strength = 33 WHERE strength = 35;
UPDATE tr_spam_smtp_config SET strength = 35 WHERE strength = 43;
UPDATE tr_spam_smtp_config SET strength = 43 WHERE strength = 50;
UPDATE tr_spam_smtp_config SET strength = 50 WHERE strength = 65;
UPDATE tr_spam_smtp_config SET strength = 50 WHERE strength = 80;
