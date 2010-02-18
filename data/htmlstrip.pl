#!/usr/bin/perl

$seenMfile = undef;

while(<>) {
	if (!$seenMfile) {
		next unless /MFile/;
		s/.*>//;
		$seenMfile = 1;
	}
	print unless /</;
}

