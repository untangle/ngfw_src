-- convert for release 4.2

-- Nuke the current settings, they'll be recreated.  This is the only way to refresh
-- rule list right now, mea culpa
delete from tr_ids_rule;
delete from tr_ids_immutable_variables;
delete from tr_ids_mutable_variables;
delete from tr_ids_variable;
delete from tr_ids_settings;
