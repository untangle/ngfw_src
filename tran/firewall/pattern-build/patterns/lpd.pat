# LPD - Line Printer Daemon Protocol - RFC 1179 - old-style UNIX printing
# Pattern quality: ok
#
# This pattern is untested.  If it does not work for you, or you believe
# it could be improved, please post to l7-filter-developers@lists.sf.net
# This list may be subscribed to at
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

lpd
# print waiting jobs:  ^\x01[!-~]+\x0a$
# receive a print job: ^\x02[!-~]+\x0a.[\x01\x02\x03][\x01-\x0a -~]*\x0a$
# Send queue state:    ^[\x03\x04][!-~]+[\x09-\x0d]+[a-z][\x09-\x0d -~]*\x0a$
# Remove jobs:         ^\x05[!-~]+[\x09-\x0d]+([a-z][!-~]*[\x09-\x0d]+[1-9][0-9]?[0-9]?|root[\x09-\x0d]+[!-~]+).*\x0a$

# This pattern could possibly match random data once in a while, but not
# too often, I think.

^(\x01[!-~]+|\x02[!-~]+\x0a.[\x01\x02\x03][\x01-\x0a -~]*|[\x03\x04][!-~]+[\x09-\x0d]+[a-z][\x09-\x0d -~]*|\x05[!-~]+[\x09-\x0d]+([a-z][!-~]*[\x09-\x0d]+[1-9][0-9]?[0-9]?|root[\x09-\x0d]+[!-~]+).*)\x0a$
