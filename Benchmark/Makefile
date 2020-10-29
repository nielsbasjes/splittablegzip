#!/bin/bash

IoBytesPerChecksum = 512
InputFile = words-20K.txt.gz

all:
	@echo "Don't use the makefile directly. Run the ./run.sh instead."

clean::
	-rm -f wordcount-normal.txt

wordcount-normal.txt: $(InputFile) target/wordcount-1.0-job.jar
	-hdfs dfs -rm -R wordcount-normal
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount $< wordcount-normal
	hdfs dfs -text wordcount-normal/part-r-* | sort > wordcount-normal.txt

wordcount-%.txt: $(InputFile) target/wordcount-1.0-job.jar
	( \
	SPLITS=$$(echo $@| sed 's/wordcount-\(.*\).txt/\1/' ) ;\
	FILESIZE=$$(stat -c%s "$<"); \
	SPLITSIZE=$$(((FILESIZE/SPLITS)+1)) ; \
	hdfs dfs -rm -R wordcount-$${SPLITS} ; \
	yarn jar target/wordcount-1.0-job.jar nl.basjes.hadoop.examples.WordCount $< wordcount-$${SPLITS} -s $${SPLITSIZE} ; \
	hdfs dfs -text wordcount-$${SPLITS}/part-r-* | sort > wordcount-$${SPLITS}.txt ; \
	)

clean::
	-rm -f wordcount-*.txt

words.txt.gz:
	rpm -qa | sed 's/\./ /g;s/-/ /g' | gzip > $@

words-4K.txt.gz: words.txt.gz
	for I in `seq 1 4000` ; do cat $< ; done > $@
	( \
	    hdfs dfs -rm $@ ;\
	    FILESIZE=$$(stat -c%s "$@"); \
	    BLOCKSIZE=$$((FILESIZE-(FILESIZE%$(IoBytesPerChecksum))+$(IoBytesPerChecksum)));\
	    hdfs dfs -Ddfs.block.size=$${BLOCKSIZE} -put $@ ;\
	)

words-20K.txt.gz: words-4K.txt.gz
	cat $< $< $< $< $< > $@
	( \
	    hdfs dfs -rm $@ ; \
	    FILESIZE=$$(stat -c%s "$@"); \
	    BLOCKSIZE=$$((FILESIZE-(FILESIZE%$(IoBytesPerChecksum))+$(IoBytesPerChecksum)));\
	    hdfs dfs -Ddfs.block.size=$${BLOCKSIZE} -put $@ ;\
	)

words-100K.txt.gz: words-20K.txt.gz
	cat $< $< $< $< $< > $@
	( \
	    hdfs dfs -rm $@ ;\
	    FILESIZE=$$(stat -c%s "$@"); \
	    BLOCKSIZE=$$((FILESIZE-(FILESIZE%$(IoBytesPerChecksum))+$(IoBytesPerChecksum)));\
	    hdfs dfs -Ddfs.block.size=$${BLOCKSIZE} -put $@ ;\
	)

words-500K.txt.gz: words-100K.txt.gz
	cat $< $< $< $< $< > $@
	( \
	    hdfs dfs -rm $@ ;\
	    FILESIZE=$$(stat -c%s "$@"); \
	    BLOCKSIZE=$$((FILESIZE-(FILESIZE%$(IoBytesPerChecksum))+$(IoBytesPerChecksum)));\
	    hdfs dfs -Ddfs.block.size=$${BLOCKSIZE} -put $@ ;\
	)


target/wordcount-1.0-job.jar:
	mvn package

clean::
	mvn clean
