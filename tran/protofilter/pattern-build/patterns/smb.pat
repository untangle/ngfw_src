# Samba/SMB - Server Message Block - Microsoft Windows filesharing
# Pattern quality: good fast
# Protocol groups: proprietary document_retrieval networking
#
# "This protocol is sometimes also referred to as the Common Internet File 
# System (CIFS), LanManager or NetBIOS protocol." -- "man samba"
#
# Actually, SMB is a higher level protocol than NetBIOS.  However, the 
# NetBIOS header is only 4 bytes: not much to match on.
#
# http://www.ubiqx.org/cifs/SMB.html
#
# This pattern is lightly tested.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/SMB
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

smb
# matches a NEGOTIATE PROTOCOL or TRANSACTION REQUEST command
\xffsmb[\x72\x25]
