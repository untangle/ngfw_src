# TLS - Transport Layer Security - RFC 2246
# Pattern quality: good
# Usually runs on port 443
#
# This pattern has been tested and is believed to work well.  If it does not
# work for you, or you believe it could be improved, please post to
# l7-filter-developers@lists.sf.net .  This list may be subscribed to at
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

tls
# Client Hello
# All TLS is 1.0, which is known internally as 3.1
^\x16\x03\x01.?.?\x01.?.?.?\x03\x01

# Other messages:
# Change Cipher Spec
# ^\x14\x03\x01\x01\x01$
# Encrypted Alert (Length always 32? (\x20))
# ^\x15\x03\x01\x20
# Certificate (No longer than 4096)|
# ^\x16\x03\x01[\x01-\x10]?.?\x0b[\x01-\x10]?.?[\x01-\x10]?.?[\x01-\x10]?
# Client Key Exchange
# ^\x16\x03\x01[\x01-\x10]?.?\x10
# Encrypted Handshake Message (Length always 48?)
# ^\x16\x03\x01\x30
# Application Data (No longer than 4096)
# ^\x17\x03\x01

