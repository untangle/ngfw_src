# HTTP - HyperText Transfer Protocol (RFC 2616)
# Pattern quality: great
# Usually runs on port 80
#
# This pattern has been tested and is believed to work well.  If it does not
# work for you, or you believe it could be improved, please post to 
# l7-filter-developers@lists.sf.net .  This list may be subscribed to at
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# this intentionally catches the response from the server
# rather than the request so that other protocols which use
# http (like kazaa) can be caught based on specific http requests
# regardless of the ordering of filters...
# also matches posts

http
# Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF (rfc 2616)
# As specified in rfc 2616 a status code is preceeded and followed by a
# space. 
http/(0\.9|1\.0|1\.1) [1-5][0-9][0-9] [\x09-\x0d -~]*(connection:|content-type:|content-length:)|post [\x09-\x0d -~]* http/[01]\.[019]
# old patterns:
#(http[\x09-\x0d -~]*(200 ok|302 |304 )[\x09-\x0d -~]*(connection:|content-type:|content-length:))|^(post [\x09-\x0d -~]* http/)
