-- fix up event tables if message info data is:
-- 1- invalid (events refer to null messages)
DELETE FROM tr_spam_evt_smtp
  WHERE msg_id IS null;
DELETE FROM tr_spam_evt
  WHERE msg_id IS null;
-- 2- missing (events refer to non-existent messages)
DELETE FROM tr_spam_evt_smtp
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_spam_evt_smtp
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
DELETE FROM tr_spam_evt
  WHERE msg_id IN
        (SELECT msg_id
           FROM tr_spam_evt
         EXCEPT
         SELECT id
           FROM tr_mail_message_info);
