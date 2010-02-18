#!/usr/bin/perl -w
use strict;
my $SCRIPTROOT="/home/niq/src/RB2FullBandPathing/data";
my $songTitle = shift @ARGV;
my $password = "medevac" . $songTitle;
#print "$password\n";
#my $md5 = `echo -n $password | md5sum`;
my $md5 = `$SCRIPTROOT/getpassword.sh $password`;
$md5 =~ s/ .*$//;
chop $md5;
#print "$md5\ntest\n";

while (<>) {
	s/PASSWORD/$md5/;
	print;
}
