#!/bin/bash

make wordcount-normal.txt

for I in `seq 1 50` ;
do
    make wordcount-${I}.txt
done
