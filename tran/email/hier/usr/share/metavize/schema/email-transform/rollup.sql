DELETE
 FROM tr_email_handler_info
 WHERE id
 IN
  (SELECT distinct hdl_id
   FROM tr_email_message_info
   WHERE time_stamp < :cutoff
  )
;
