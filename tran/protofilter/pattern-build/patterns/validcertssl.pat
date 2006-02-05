# Valid certificate SSL 
# Pattern quality: good notsofast
# Protocol groups: ietf_proposed_standard secure

# This matches anything claiming to use a valid certificate from a well 
# known certificate authority.
#
# This is a subset of ssl, so it needs to come first to match.
#
# Note that opening a website that has a valid certificate will 
# open one connection that matches this and many ssl connections that
# only match the ssl pattern.  Thus, this pattern may not be very useful.
#
# This pattern is believed match only the above, but may not match all
# of it.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/SSL
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# the certificate authority info is sent in quasi plain text, if it matches 
# a well known certificate authority then we will assume it is a 
# web/imaps/etc server. Other ssl may be good too, but it should fall under 
# a different rule

validcertssl
^.?.?\x01\x03\x01?.*\x0b.*(thawte|equifax secure certificate authority|rsa data security, inc|verisign, inc|gte cybertrust root|entrust\.net limited)
