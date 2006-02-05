# rlogin - remote login - RFC 1282
# Pattern quality: ok veryfast
# Protocol groups: ietf_rfc_documented remote_access
#
# usually runs on port 443
#
# This pattern is untested.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Rlogin
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

rlogin
# At least three characters (user name, user name, terminal type), 
# the first of which could be the first character of a user name, a
# slash, then a terminal speed.  (Assumes that usernames and terminal
# types are alphanumeric only.  I'm sure there are usernames like
# "straitm-47" out there, but it's not common.) All terminal speeds 
# I know of end in two zeros and are between 3 and 6 digits long.
# This pattern is uncomfortably general.
^[a-z][a-z0-9][a-z0-9]+/[1-9][0-9]?[0-9]?[0-9]?00
