# MSN Messenger - Microsoft Network chat client
# Pattern quality: good fast
# Protocol groups: proprietary chat
#
# Usually uses port 1863
# http://www.hypothetic.org/docs/msn/index.php
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/MSN_Messenger
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

msnmessenger
# ver: allow versions up to 99.
# I've never seen a cvr other than cvr0.  Maybe this will be trouble later?
# Can't anchor at the beginning because sometimes this is encapsulated in
# HTTP.  But either way, the first packet ends like this.
ver [0-9]+ msnp[1-9][0-9]? [\x09-\x0d -~]*cvr0\x0d\x0a$
