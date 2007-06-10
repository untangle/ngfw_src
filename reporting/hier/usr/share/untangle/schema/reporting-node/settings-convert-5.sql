-- settings conversion for release-5.0

ALTER TABLE settings.tr_reporting_settings RENAME TO n_reporting_settings;
ALTER TABLE settings.tr_reporting_sched RENAME TO n_reporting_sched;
ALTER TABLE settings.tr_reporting_wk_sched_rule RENAME TO n_reporting_wk_sched_rule;
ALTER TABLE settings.tr_reporting_wk_sched RENAME TO n_reporting_wk_sched;
