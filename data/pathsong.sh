#!/bin/sh

SCRIPTROOT=`dirname $0`
SONG=`basename $1`
MID=$SONG.mid
DATAROOT=`dirname $1`
STATEROOT=~/.big/handrolled
STATEDIR=$STATEROOT/$SONG
DATAFILE=$DATAROOT/$SONG.frommid.txt
PATHFILE=$DATAROOT/$SONG.path
IMAGEFILE=~/3dub/$SONG.png

wget http://ajanata.com/~ajanata/rb_mids/$SONG.mid
curl -F "mid_upload=@$MID" -F "tt=0" http://staff.dasdeck.de/valentin/midi/mid2txt.php | $SCRIPTROOT/htmlstrip.pl | $SCRIPTROOT/trackrearrange.pl > $DATAFILE
rm $MID
mkdir -p $STATEDIR
java -server -verbose:gc -XX:+PrintGCTimeStamps com.scorehero.pathing.fullband.DIYStorageOptimizer $DATAFILE
java -server com.scorehero.pathing.fullband.PathWalker $DATAFILE | $SCRIPTROOT/replacepassword.pl $SONG | tee $PATHFILE
curl --data-binary "@$PATHFILE" "http://static.socialgamer.net/~ajanata/phpspopt/web/chartgenapi.php?game=rb&guitar=expert&vocals=expert&drums=expert&bass=expert&file=$SONG" > $IMAGEFILE
#rm -R $STATEDIR
