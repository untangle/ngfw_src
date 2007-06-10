-- convert for release 3.1

-- nothing to do

ALTER TABLE settings.TR_IDS_RULE ADD COLUMN sid int4;
UPDATE settings.TR_IDS_RULE SET sid = -1;
