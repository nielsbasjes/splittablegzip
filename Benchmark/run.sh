#!/bin/bash

FILENAME=words-100K.txt.gz

make ${FILENAME}

make wordcount-normal.txt

for I in `seq 1 25` ; 
do 
    make wordcount-${I}.txt
done
