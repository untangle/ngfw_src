# Jabber (XMPP) - open instant messenger protocol - RFC 3920 - http://jabber.org
# Pattern quality: good fast
# Protocol groups: ietf_proposed_standard chat
#
# This pattern has been tested with Gaim and Gabber.  It is only tested 
# with non-SSL mode Jabber with no proxies.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Jabber
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# Thanks to Jan Hudec for some improvements.

# Jabber seems to take a long time to set up a connection.  I'm
# connecting with Gabber 0.8.8 to 12jabber.org and the first 8 packets
# is this:
# <stream:stream to='12jabber.com' xmlns='jabber:client'
# xmlns:stream='http://etherx.jabber.org/streams'><?xml
# version='1.0'?><stream:stream
# xmlns:stream='http://etherx.jabber.org/streams' id='3f73e951'
# xmlns='jabber:client' from='12jabber.com'>
#
# No mention of my username or password yet, you'll note.

jabber
<stream:stream[\x09-\x0d ][ -~]*[\x09-\x0d ]xmlns=['"]jabber
