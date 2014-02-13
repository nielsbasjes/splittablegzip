#!/bin/bash

make wordcount-normal.txt

for I in `seq 1 25` ; 
do 
    make wordcount-${I}.txt
done
