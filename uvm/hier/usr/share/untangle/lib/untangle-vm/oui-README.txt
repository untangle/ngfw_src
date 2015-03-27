oui-formatted.txt is a formatted list of MAC prefixes owned by various vendors
It comes from IEEE.

To grab the newest version do the following

wget http://standards-oui.ieee.org/oui.txt
cat oui.txt | grep '(hex)' | sed 's/(hex)//' | sed 's/\s\+//' | sed 's/\s\+/ /g' | sed 's/-/:/' | sed 's/-/:/' | sed 's/[0-9A-Fa-f:]\+/\L&/' > oui-formatted.txt
