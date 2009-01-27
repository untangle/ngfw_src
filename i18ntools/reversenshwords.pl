#!/usr/bin/perl

use Getopt::Long;

my $PROGRAM = "reversepowords";
my $VERSION = "0.1";

# Input validation bits
# ./reversepowords -files
GetOptions("help"=>\$hflag,
            "version"=>\$version,
            "files=s"=>\$files,
            "dir=s"=>\$dir,
            "savedir=s"=>\$savedir,);
            
usage() if $hflag;
print "version $VERSION\n" if $version;
# directory is given, compile list of files.
print "Dir is <$dir> savedir is <$savedir>\n";
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
    print "Process files <$files>\n" if $files;
    @files2process = split / /, $files;
    $wordcount = 0;
    foreach $file (@files2process) {
        print "processing file <$file>\n";
        # open file
        open(DATA_FILE, "$dir/$file") || print("Could not open file $dir/$file !");
        @raw_data=<DATA_FILE>;
        close(DATA_FILE);
        # print "Raw data is <@raw_data>\n";
        @newfile = processDataFile(@raw_data);
        # if its a .pot file change to po
        $newfilename = $file;
        # $newfilename =~ s/\.pot$/\.po/;
        # add language to filename
        # $newfilename =~ s/\./-test\./;
        open(FILEWRITE, "> $savedir/$newfilename")|| 
            print("Could not open file $savedir/$newfilename for overwriting !");;
        print FILEWRITE @newfile;
        close(FILEWRITE);
    }
    print "WordCount is <$wordcount>\n";
}
 
exit;
        
sub processDataFile {
    local (@datafile) = @_;
    local @newdatafile =();
    $wording_loaded = 0;
    # print "processing <@datafile>\n";
    foreach $line (@datafile) {
        # we completed a word reversal lines, write them out
        if ($line  =~ /LANG_ENGLISH/) {
            push @newdatafile, $line;
            print "Oldline is <$line>";
            $line =~ /\"(.*)\"/;
            # print "Words <$1>\n";
            # print "Words found <$1>\n";
            @words = split / /, $1;
            $wordcount += $#words;
            $line =~ s/LANG_ENGLISH/LANG_IRISH/;
            foreach $word (@words) {
                if ($word !~/\{|\%|\$|\*/) {
                    # escape out periods
                    $word =~ s/\.//;
                    $word =~ s/\*//;
                    print "Word is <$word>\n";
                    $newword = reverse $word;
                    print "newword is <$newword>\n";
                    $line =~ s/$word/$newword/;
                }
            }
            push @newdatafile, $line;
            print "Newline is <$line>\n";
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



