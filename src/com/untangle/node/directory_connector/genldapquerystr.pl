# Quick and dirty utility program to generate the LDAP query string for getting list of users from AD server
#
# It adds all the combinations of the array @modifiers together and builds and LDAP query string of usertypes.
#

#!/usr/bin/perl

my @modifiers = (1, 8, 32, 64, 65536);
my $curval;
my $outstr = "(|(userAccountControl=512)";
my $iterations = 2 ** @modifiers;
my $parens = 2;

for (my $loop = 1; $loop < $iterations; $loop++) {
    $curcnt = $loop;
    $curval = 512;
    $element = 0;
    while ($curcnt > 0) {
#        print "While: $curcnt Mod: " . $curcnt % 2 . "\n";
        if ($curcnt % 2 == 1 ) {
            $curval += @modifiers[$element];
        }
       $curcnt = int ($curcnt / 2);
       $element++;
    }
    if ($loop + 1 == $iterations) {
        $outstr .= "(userAccountControl=";
    } else {
        $outstr .= "(|(userAccountControl=";
        $parens++;
    }
    $outstr .= "$curval)";
}

for (; $parens > 0; $parens--) {
    $outstr .= ")";
}
print "$outstr\n";

