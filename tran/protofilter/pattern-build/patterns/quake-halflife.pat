# Quake 2, Quake 3, Quake World, Half Life - popular computer games.
# Pattern quality: good
#
# This pattern has been tested with QuakeWorld (2.30), Quake 2 (3.20), 
# Quake 3 (1.32), and Half-life (1.1.1.0). But may also work on other
# games based on the Quake engine.
#
# Contributed by Laurens Blankers <laurens AT blankersfamily.com>
#
# Please post to l7-filter-developers@lists.sf.net as to whether it works
# for you or not.  If you believe it could be improved please post your
# suggestions to that list as well. You may subscribe to this list at
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

quake-halflife
# All quake (like) protocols start with 4x 0xFF.  Then the client either
# issues getinfo or getchallenge.
^\xff\xff\xff\xffget(info|challenge)

# A previous quake pattern allowed the connection to start with only 2 bytes
# of 0xFF.  This doesn't seem to ever happen, but we should keep an eye out
# for it.
