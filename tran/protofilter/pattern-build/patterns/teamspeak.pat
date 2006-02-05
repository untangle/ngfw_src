# TeamSpeak - VoIP application - http://goteamspeak.com
# Pattern quality: good veryfast
# Protocol groups: proprietary voip
#
# This pattern has been tested by Matthew Strait and verified by packet
# traces by at least two other people.  The meaning of f4b303 is not
# known, but it seems to appear in all first packets.  This pattern only
# matches the actual UDP voice traffic, not the TeamSpeak web interface
# or "TCP query".
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/TeamSpeak
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

teamspeak
^\xf4\xbe\x03.*teamspeak

