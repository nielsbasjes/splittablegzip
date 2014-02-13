#!/bin/bash

all: 
	@echo "Don't use the makefile directly. Run the ./run.sh instead."

clean:: 
	-rm -f wordcount-normal.txt

wordcount-normal.txt: words-100K.txt.gz target/wordcount-1.0-job.jar
	-hdfs dfs -rm -R wordcount-normal
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount $< wordcount-normal
	hdfs dfs -text wordcount-normal/part-r-* | sort > wordcount-normal.txt

wordcount-%.txt: words-100K.txt.gz target/wordcount-1.0-job.jar
	( \
	SPLITS=$$(echo $@| sed 's/wordcount-\(.*\).txt/\1/' ) ;\
	FILESIZE=$$(stat -c%s "$<"); \
	SPLITSIZE=$$(echo "$${FILESIZE}/$${SPLITS} +1" | bc) ; \
	hdfs dfs -rm -R wordcount-$${SPLITS} ; \
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount $< wordcount-$${SPLITS} -s $${SPLITSIZE} ; \
	hdfs dfs -text wordcount-$${SPLITS}/part-r-* | sort > wordcount-$${SPLITS}.txt ; \
	)

words-100K.txt.gz: words.txt.gz
	for I in `seq 1 100000` ; do cat $< ; done > $@
	-hdfs dfs -rm $@
	hdfs dfs -put $@

words.txt.gz:
	rpm -qa | sed 's/\./ /g;s/-/ /g' | gzip > $@

clean:: 
	-rm -f wordcount-*.txt

target/wordcount-1.0-job.jar:
	mvn package

clean::
	mvn clean
