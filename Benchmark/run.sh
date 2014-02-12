#!/bin/bash

FILENAME=words-100K.txt.gz

make ${FILENAME}
FILESIZE=$(stat -c%s "$FILENAME")

make wordcount-normal.txt

for I in `seq 1 25` ; 
do 
    SPLITSIZE=$(echo "(${FILESIZE}/${I})+1" | bc)
    make wordcount-${SPLITSIZE}.txt
done
