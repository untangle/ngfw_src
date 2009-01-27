#!/usr/bin/perl

use Getopt::Long;

my $PROGRAM = "reversepowords";
my $VERSION = "0.1";
# ./reversepowords.pl -dir=/home/jcoffin/translations081110/es 
#                     -savedir=/home/jcoffin/new 
#                     -countonly=1
# Input validation bits
# ./reversepowords -files
GetOptions("help"=>\$hflag,
            "version"=>\$version,
            "files=s"=>\$files,
            "dir=s"=>\$dir,
            "savedir=s"=>\$savedir,
            "countonly=s"=>\$countonly,
            );
            
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
    # print "Process files <$files>\n" if $files;
    @files2process = split / /, $files;
    $wordcount = 0;
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
        if (!($countonly)) {
            open(FILEWRITE, "> $savedir/$newfilename")|| 
                print("Could not open file $savedir/$newfilename for overwriting !");;
            print FILEWRITE @newfile;
            close(FILEWRITE);
        }
    }
    print "WordCount is <$wordcount>\n";
}
 
exit;
        
sub processDataFile {
    local (@datafile) = @_;
    local @newdatafile =();
    $wording_loaded = 0;
    $newline = "msgstr \"";
    foreach $line (@datafile) {
        # we completed a word reversal lines, write them out
        if ($line  =~ /^msgstr/) {
            $wording_loaded = 0;
            $newline =~ s/\"$//;
            # print "Write out newline <$newline>\n";
            push @newdatafile, $newline;
            $newline = "msgstr \"";
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
            $line =~ /\"(.*)\"/;
            # print "Words <$1>\n";
            # print "Words found <$1>\n";
            @words = split / /, $1;
            $wordcount += $#words;
            foreach $word (@words) {
                if ($word =~/\{|\%|\"/) {
                    $newline .= $word;
                } else {
                    $newline .= reverse $word;
                }
                $newline .= " ";
            }
            $newline =~ s/\s+$//;
            $newline .= "\"\n\"";
            # print "Newline is <$newline>\n";
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



