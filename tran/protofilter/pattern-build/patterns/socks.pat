# SOCKS Version 5 - Firewall traversal protocol - RFC 1928
# Pattern quality: ok fast
# Protocol groups: ietf_proposed_standard networking
#
# Usually runs on port 1080
# Also useful: http://www.iana.org/assignments/socks-methods
#
# We have had one report that this pattern works.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/SOCKS
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# method request, no private methods	\x05[\x01-\x08]*
# method reply, assumes sucess		\x05[\x01-\x08]?
# method dependent sub-negotiation	.*
# request, ipv4 only			\x05[\x01-\x03][\x01\x03].*
# reply					\x05[\x01-\x08]?[\x01\x03].*

# username/password method
# u/p request, assuming reasonable usernames and passwords
# \x05[\x02-\x10][a-z][a-z0-9\-]*[\x05-\x20][!-~]*
# server reply
# \x05

# GSSAPI method
# client initial token 		\x01\x01\x02.*
# server reply			\x01\x01\x02.*

# any other method  .* (all methods boil down to this until we have information
# about all the commonly used ones)

socks
\x05[\x01-\x08]*\x05[\x01-\x08]?.*\x05[\x01-\x03][\x01\x03].*\x05[\x01-\x08]?[\x01\x03]
