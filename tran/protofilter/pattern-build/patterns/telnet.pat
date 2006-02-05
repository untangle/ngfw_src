# Telnet - Insecure remote login - RFC 854
# Pattern quality: good veryfast
# Protocol groups: ietf_internet_standard remote_access
#
# Usually runs on port 23
#
# This pattern is lightly tested.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Telnet
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

telnet
# Matches at least three IAC (Do|Will|Don't|Won't) commands in a row.  
# My telnet client sends 9 when I connect, so this should be fine.
# This pattern could fail on a unchatty connection or it could be 
# matched by something non-telnet spewing a lot of stuff in the fb-ff range.
^\xff[\xfb-\xfe].\xff[\xfb-\xfe].\xff[\xfb-\xfe]
