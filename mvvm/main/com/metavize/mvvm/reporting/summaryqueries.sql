-- How many files scanned
select count(*)
from tr_virus_evt;

-- How many files clean
select count(*)
from tr_virus_evt
where clean = 't';

-- How many files blocked
select count(*)
from tr_virus_evt
where clean != 't'
and virus_cleaned != 't'

-- How many files cleaned
select count(*)
from tr_virus_evt
where clean != 't'
and virus_cleaned = 't'

-- Passed
 = clean + cleaned

same for tr_virus_http_evt

same for tr_email_virus_evt

and add.

-- number of species of viruses found

---------

Summary:

  total traffic for period: incoming, outgoing, total

   total number of incoming sessions, outgoing sessions for period
   
   Average bytes/day

*** IP Summary Info

    box IP address
    range (IPMaddr) of inside addresses observed

   average session length in bytes, total and for each protocol.

virus above

Web Control
        total hits, traffic downloaded, uploaded
        total passed, blocked, allowed
        distinct hosts browsed

Protofilter
        number of sessions of logged
        number of sessions of blocked

Spyware
        number of spyware accesses logged
        number of spyware accesses blocked
        number of suspicous client cookies removed
        number of suspicous server cookies removed
        number of active-X installs blocked
        
email
        number of email messages sent
        number of email messages received
        average spam score for sent messages
        average spam score for received messages
        number of messages marked spam (sent)
        number of messages marked spam (received)
        number of messages scanned for viruses
        number of messages detected
        number of messages removed
        average email size
        
airgap
        has none
