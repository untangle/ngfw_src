-- settings conversion for release 4.2.3

UPDATE settings.user_policy_rule SET user_matcher = '[any]' WHERE user_matcher = 'all';
UPDATE settings.user_policy_rule SET user_matcher = '[any]' WHERE user_matcher = 'any';

