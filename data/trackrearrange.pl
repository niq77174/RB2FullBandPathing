#!/usr/bin/perl -w
use strict;

my $tracks = {};

my $trackText = "";
my $trackName = "";
while (<>) {
	if (/^MTrk/) {
		$trackText = "";
	}

	$trackText .= $_;

	if (/^TrkEnd/) {
		$tracks->{$trackName} = $trackText if ($trackName);
		$trackText = "";
		$trackName = undef;
		next;
	}

	if (/TimeSig/) {
		$trackName = 'TEMPO';
	}

	if (/BEAT/) {
		$trackName = 'BEAT';
	}

	if (/PART VOCALS/) {
		$trackName = 'VOCALS';
	}

	if (/PART GUITAR/) {
		$trackName = 'GUITAR';
	}

	if (/PART BASS/) {
		$trackName = 'BASS';
	}

	if (/PART DRUMS/) {
		$trackName = 'DRUMS';
	}

}


print $tracks->{'BEAT'};
print $tracks->{'TEMPO'};
print $tracks->{'GUITAR'};
print $tracks->{'BASS'};
print $tracks->{'DRUMS'};
print $tracks->{'VOCALS'};
