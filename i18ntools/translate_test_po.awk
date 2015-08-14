# This awk script edits all the strings in a PO file and replaces it with "test" translations
# msgid "foo" will get set to
# msgstr "XfooX"
# 
{
    if ( $0 ~ /^msgid.*$/ ) {
	print $0;
	sub("msgid \"","", $0);
	sub("\"$","", $0);
	str = $0
    } else {
	if ( $0 ~ /^msgstr.*$/ ) {
	    print "msgstr \"X" str "X\""
	} else  {
	    print $0;
	}
    }
}