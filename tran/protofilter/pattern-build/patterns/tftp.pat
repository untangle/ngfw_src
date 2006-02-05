# TFTP - Trivial File Transfer Protocol - used for bootstrapping - RFC 1350
# Pattern quality: marginal veryfast
# Protocol groups: ietf_internet_standard document_retrieval
#
# usually runs on port 69
# 
# This pattern is unconfirmed. 
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/TFTP
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

tftp
# The first packet from the initiating host should either be a Read Request 
# or a Write Request.  In the other direction, it should be data packet with 
# block number one or an ACK with block number zero.  We only attempt to match
# the initiating host's packets, because the only identifying features of 
# the responses to them are two byte sequences (which isn't specific enough).
# (\x01|\x02) = Read Request or Write Request
# [ -~]* = the file name
# the rest = netascii|octet|mail (case insensitivity done by the kernel)

^(\x01|\x02)[ -~]*(netascii|octet|mail)
