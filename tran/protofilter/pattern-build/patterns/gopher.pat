# Gopher - A precursor to HTTP - RFC 1436
# Pattern quality: good notsofast undermatch
# Protocol groups: obsolete ietf_rfc_documented document_retrieval
#
# Gopher servers usually run on TCP port 70.
#
# This pattern is lightly tested using gopher.dna.affrc.go.jp .
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Gopher
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

gopher
# This matches the server's response, but naturally only if it is a
# directory listing, not if it is sending a file, because then the data 
# is totally arbitrary.

# Matches the client saying "list what you have", then the server
# response: one of the file type characters, any printable characters, a
# tab, any printable characters, a tab, something that looks like a
# domain name, a tab, and then a number which could be the start of a
# port number.

# "0About internet Gopher\tStuff:About us\trawBits.micro.umn.edu\t70"
# "\r7search by keywords on protein data using wais\twaissrc:/protein_all/protein\tgopher.dna.affrc.go.jp\t70"

^[\x09-\x0d]*[1-9,+tgi][\x09-\x0d -~]*\x09[\x09-\x0d -~]*\x09[a-z0-9.]*\.[a-z][a-z].?.?\x09[1-9]
