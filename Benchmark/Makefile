#!/bin/bash

all:: wordcount-normal.txt
#               1397956862 Feb 12 14:09 words-100K.txt.gz
all:: wordcount-1400000000.txt
all:: wordcount-700000000.txt
all:: wordcount-500000000.txt
all:: wordcount-350000000.txt
all:: wordcount-280000000.txt
all:: wordcount-250000000.txt
all:: wordcount-200000000.txt
all:: wordcount-175000000.txt
all:: wordcount-150000000.txt
all:: wordcount-125000000.txt
all:: wordcount-115000000.txt
all:: wordcount-100000000.txt
all:: wordcount-50000000.txt
all:: wordcount-25000000.txt
all:: wordcount-10000000.txt


clean:: 
	-rm -f wordcount-normal.txt

wordcount-normal.txt:
	-hdfs dfs -rm -R wordcount-normal
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount words-100K.txt.gz wordcount-normal
	hdfs dfs -text wordcount-normal/part-r-* | sort > wordcount-normal.txt

wordcount-%.txt:
	( \
	SIZE=$$(echo $@| sed 's/wordcount-\(.*\).txt/\1/' ) ;\
	-hdfs dfs -rm -R wordcount-$${SIZE} ; \
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount words-100K.txt.gz wordcount-$${SIZE} -s $${SIZE} ; \
	hdfs dfs -text wordcount-$${SIZE}/part-r-* | sort > wordcount-$${SIZE}.txt \
	)

clean:: 
	-rm -f wordcount-*000.txt
