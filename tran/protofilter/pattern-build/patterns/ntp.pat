# (S)NTP - (Simple) Network Time Protocol - RFCs 1305 and 2030
# Pattern quality: good veryfast overmatch 
# Protocol groups: ietf_draft_standard time_synchronization
#
# This pattern is tested and is believed to work.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/NTP
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# client|server
# Requires the server's timestamp to be in the present or future (of 2005).
# Tested with ntpdate on Linux.
# Assumes version 2, 3 or 4.

# Note that ntp packets are always 48 bytes, so you should match on that too.

ntp
^([\x13\x1b\x23\xd3\xdb\xe3]|[\x14\x1c$].......?.?.?.?.?.?.?.?.?[\xc6-\xff])
