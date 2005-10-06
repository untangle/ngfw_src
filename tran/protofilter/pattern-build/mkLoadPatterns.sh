#!/bin/sh

category_file=../categories

FILE=../LoadPatterns.java

BUILD_DIR=`dirname $0`"/"
BUILD_DIR=`cd $BUILD_DIR; echo $PWD`
cd $BUILD_DIR

if [ ! -d patterns/ ] ; then
    echo "No patterns"
    exit -1
fi 

cd patterns/
echo > $FILE

cat ../LoadPatterns_start.java >> $FILE

for i in `ls ./*.pat` ; do
    cat $i | sed -e '/^[ \t]*#/d' | sed -e '/^[ \t]*$/d' > $i.new
    STR="`cat $i | head -n 1 | sed -e 's/.*#//' -e 's/"//g'`"
    NAME="`echo $STR | perl -p -e 's/(.*?) +-.*/\1/g'`"
    DESC="`echo $STR | perl -p -e 's/.*?-(.*)/\1/g'`"
    QUAL="`cat $i | head -n 2 | tail -n 1 | sed -e 's/.*: //'`"
    DEF="`head -n 2 $i.new  | tail -n 1 | sed -e 's/\\\\/\\\\\\\\/g' | sed -e 's/\\\"/\\\\\"/g' `"
    CATEGORY="`grep \"^$NAME|:\" $category_file | sed -e 's/[^|]*|: *//g' -e 's/\([^ ].*[^ ]\) */\1/g'`"

    if [ "x$CATEGORY" == "x" ]; then
        echo "No category for $NAME"
        exit -2
    fi

    if [ "$NAME" == "Unknown" ] ; then 
        continue
    fi

    echo " ==> " $NAME

    echo -ne "\tpats.put(\"" >> $FILE
    echo -n "$NAME" >> $FILE
    echo -n "\",new ProtoFilterPattern(\"" >> $FILE
    echo -n "$NAME"  >> $FILE
    echo -n "\",\""  >> $FILE
    # XXX 
    echo -n "$CATEGORY" >> $FILE
    echo -n "\",\""  >> $FILE
    echo -n "$DESC" >> $FILE
    echo -n "\",\""  >> $FILE
    echo -n "$DEF" >> $FILE
    echo -n "\",\""  >> $FILE
    echo -n "$QUAL" >> $FILE
    echo -n "\","  >> $FILE
    
    echo    "false,false,false));" >> $FILE
done

rm *.new

cat ../LoadPatterns_end.java >> $FILE

cd ..

echo "Saving LoadPatterns.java"
cp LoadPatterns.java ../main/com/metavize/tran/protofilter/
rm LoadPatterns.java
