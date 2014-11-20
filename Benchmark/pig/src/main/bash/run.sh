#!/bin/sh

INPUT=words-100K.txt.gz

hdfs dfs -put ${INPUT}

echo DATE,SPLITS,FILESIZE,SPLITSIZE,TIME >> output.txt
for SPLITS in `seq 1 100`;
do
    hdfs dfs -rm -R wordcount-${SPLITS}.txt
    FILESIZE=$(stat -c%s "${INPUT}")
    SPLITSIZE=$(((FILESIZE/SPLITS)+1)) 

    exec 3>&1 4>&2
    TIME=$(TIMEFORMAT="%R"; { time pig -param file=${INPUT} -param splits=${SPLITS} -param splitsize=${SPLITSIZE} pig/wordcount.pig 1>&3 2>&4; } 2>&1)
    exec 3>&- 4>&-
    echo $(date),${SPLITS},${FILESIZE},${SPLITSIZE},${TIME} >> output.txt
done

cat output.txt