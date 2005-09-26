-- converter for release 3.0

ALTER TABLE settings.tr_protofilter_settings DROP COLUMN buffersize;

UPDATE settings.tr_protofilter_settings SET chunklimit = 10;
