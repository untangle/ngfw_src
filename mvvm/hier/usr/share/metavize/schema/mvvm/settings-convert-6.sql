-- settings conversion for release-3.2

UPDATE tid SET policy_id = NULL WHERE id IN
    (SELECT tid.id FROM tid
     LEFT JOIN transform_persistent_state ON tid.id = tid
     WHERE target_state IS NULL AND NOT policy_id IS NULL);

DELETE FROM user_policy_rule WHERE set_id IS NULL;