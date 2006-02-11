-- convert for release 3.2

-- Nuke the current settings, they'll be recreated.
delete from tr_ids_rule;
delete from tr_ids_immutable_variables;
delete from tr_ids_mutable_variables;
delete from tr_ids_variable;
delete from tr_ids_settings;
