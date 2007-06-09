-- settings conversion for release-5.0

ALTER TABLE settings.tr_spyware_settings RENAME TO n_spyware_settings;
ALTER TABLE settings.tr_spyware_cr RENAME TO n_spyware_cr;
ALTER TABLE settings.tr_spyware_ar RENAME TO n_spyware_ar,;
ALTER TABLE settings.tr_spyware_sr RENAME TO n_spyware_sr;
ALTER TABLE settings.tr_spyware_wl RENAME TO n_spyware_wl;
