#!/usr/bin/perl

use Getopt::Long;

my $PROGRAM = "markpowords";
my $VERSION = "0.1";
# ./markpowords.pl -dir=/home/jcoffin/translations081110/es 
#                     -savedir=/home/jcoffin/new 

# Input validation bits
# ./reversepowords -files
GetOptions("help"=>\$hflag,
            "version"=>\$version,
            "files=s"=>\$files,
            "dir=s"=>\$dir,
            "savedir=s"=>\$savedir,
            );
            
usage() if $hflag;
print "version $VERSION\n" if $version;
# directory is given, compile list of files.
print "Dir is <$dir> savedir is <$savedir>\n";
if (!(-e $savedir)) {
    # create the directory if it doesn't exist
    mkdir ($savedir, 0777);
}
if (!($files)) {
    $files = "";
    opendir(DIR, "$dir");
    $files = join " ", grep(/[\.pot|\.po]$/,readdir(DIR));
    # remove . and .. files
    $files =~ s/ \. / /;
    $files =~ s/ \.\.//;
    closedir(DIR);
    # print "files are <$files>\n";
}
if ($files) {
    # print "Process files <$files>\n" if $files;
    @files2process = split / /, $files;
    foreach $file (@files2process) {
        print "processing file <$file>\n";
        # open file
        open(DATA_FILE, "$dir/$file") || print("Could not open file $dir/$file !");
        @raw_data=<DATA_FILE>;
        close(DATA_FILE);     
        @newfile = processDataFile(@raw_data);
        # if its a .pot file change to po
        $newfilename = $file;
        $newfilename =~ s/\.pot$/\.po/;
        # add language to filename
        # $newfilename =~ s/\./-test\./;
        open(FILEWRITE, "> $savedir/$newfilename")|| 
            print("Could not open file $savedir/$newfilename for overwriting !");;
        print FILEWRITE @newfile;
        close(FILEWRITE);
    }
}
 
exit;
        
sub processDataFile {
    my (@datafile) = @_;
    my @newdatafile =();
    $wording_loaded = 0;
    $newline = "msgstr ";
    foreach $line (@datafile) {
        # we completed a word reversal lines, write them out
        if ($line  =~ /^msgstr/) {
            $wording_loaded = 0;
            # $newline =~ s/\"$//;
            # print "Write out newline <$newline>\n";
            if ($line =~ /^msgstr \"\.\"/) {
                push @newdatafile, $line;
            } elsif ($line =~ /^msgstr \"\,\"/) {
                push @newdatafile, $line;
            } elsif ($line =~ /^msgstr \"m\/d\/y\"/) {
                push @newdatafile, $line;
            } elsif ($line =~ /^msgstr \"Y\-m\-d g\:i\:s a\"/) {
                push @newdatafile, $line;
            } else {
                push @newdatafile, $newline;
            }
            $newline = "msgstr ";
        } elsif ($line  =~ /^\"Plural-Forms/) {
            push @newdatafile, "\"Plural-Forms: nplurals=0; plural=0;\\n\""; 
        } elsif ($line  =~ /^\"Content-Type/) {
            push @newdatafile, "\"Content-Type: text/plain; charset=UTF-8\\n\""; 
        } elsif (($line  =~ /^msgid/) || ($wording_loaded)) {
            # ignore lines until #: appears
            # print "Processing line <$line>\n";
            # write next line
            # push @newdatafile, $line;
            # msgid "%s Login"
            $wording_loaded = 1;
            # write out english line
            push @newdatafile, $line;
            # print "Oldline is <$line>";
            $line =~ s/msgid //; # remove line header
            $line =~ s/\"/\"JCC/;
            $newline .= $line;
        } else {
            push @newdatafile, $line;
        }
    }
    return @newdatafile;
}

sub usage {
    print "$PROGRAM [option argument][option argument]...\n";
    print "$PROGRAM [-files filename1 filename2 ...]\n";
    print "$PROGRAM [-help]\n";
    print "$PROGRAM [-version]\n";
    print "Options:\n";
    print "    -files    Files to convert\n";
    print "    -help     This text\n";
    print "    -version  version of the convertor\n";
    exit;
}



