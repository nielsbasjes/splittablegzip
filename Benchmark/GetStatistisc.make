# This is a quick and dirty makefile to screenscrape the jobhistory pages to get the stats I was looking for.

Results.txt:: Results-200.txt
Results.txt:: Results-201.txt
Results.txt:: Results-202.txt
Results.txt:: Results-203.txt
Results.txt:: Results-204.txt
Results.txt:: Results-205.txt
Results.txt:: Results-206.txt
Results.txt:: Results-207.txt
Results.txt:: Results-208.txt
Results.txt:: Results-209.txt
Results.txt:: Results-210.txt
Results.txt:: Results-211.txt
Results.txt:: Results-212.txt
Results.txt:: Results-213.txt
Results.txt:: Results-214.txt
Results.txt:: Results-215.txt
Results.txt:: Results-216.txt
Results.txt:: Results-217.txt
Results.txt:: Results-218.txt
Results.txt:: Results-219.txt
Results.txt:: Results-220.txt
Results.txt:: Results-221.txt
Results.txt:: Results-222.txt
Results.txt:: Results-223.txt
Results.txt:: Results-224.txt
Results.txt:: Results-225.txt
Results.txt:: Results-226.txt
Results.txt:: Results-227.txt
Results.txt:: Results-228.txt
Results.txt:: Results-229.txt
Results.txt:: Results-230.txt
Results.txt:: Results-231.txt
Results.txt:: Results-232.txt
Results.txt:: Results-233.txt
Results.txt:: Results-234.txt
Results.txt:: Results-235.txt
Results.txt:: Results-236.txt
Results.txt:: Results-237.txt
Results.txt:: Results-238.txt
Results.txt:: Results-239.txt
Results.txt:: Results-240.txt
Results.txt:: Results-241.txt
Results.txt:: Results-242.txt
Results.txt:: Results-243.txt
Results.txt:: Results-244.txt
Results.txt:: Results-245.txt
Results.txt:: Results-246.txt
Results.txt:: Results-247.txt
Results.txt:: Results-248.txt
Results.txt:: Results-249.txt

Results.txt::
	@echo "Merge results"
	@cat Results-*.txt > $@

clean::
	rm -f Results.txt

Results-%.txt: Extract-%.txt
	@( ID=$$(echo $@ | cut -d'-' -f 2 | cut -d'.' -f1) ; echo "Calculate run $${ID}" )
	@cat $< | sed 's@\([0-9]*\)hrs, @(\1*3600)+@g;s@\([0-9]*\)mins, @(\1*60)+@g;s@\([0-9]*\)sec@(\1)@g;s@ @@g;s@|@ @g' |\
	while read name splits elapsed avgmap ; do echo "$${name} | $${splits} | $$((elapsed)) | $$((avgmap)) | $$(( (avgmap) * (splits) )) " ; done >$@
	
clean::
	rm -f Results-*.txt

#.PRECIOUS: Extract-%.txt
Extract-%.txt: Output-%.txt
	@cat $< | tr -d '\n' | sed 's@ *<@<@g;s@> *@>@g;s@.*>Job Name:<td>\([^<]*\).*>Elapsed:<td>\([^<]*\).*>Average Map Time<td>\([^<]*\).*>Map</a><td>\([^<]*\).*@\1 | \4 |  \2 |  \3 @;s@Wordcount-@@g;s@Wordcount@GzipCodec@g;' > $@ ; 
	@echo >> $@ 

clean::
	rm -f Extract-*.txt
	

.PRECIOUS: Output-%.txt
Output-%.txt:
	@( ID=$$(echo $@ | cut -d'-' -f 2 | cut -d'.' -f1) ; \
	echo "Download run $${ID}" ; \
	curl -s "http://node1:19888/jobhistory/job/job_1390217611646_0$${ID}" | fgrep -v "Not Found"> $@ )

clean::
	rm -f Output-*.txt
