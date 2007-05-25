-- settings convert for release 4.1

UPDATE settings.tr_httpblk_template SET header = 'Web Filter' WHERE header LIKE '%Metavize%';
