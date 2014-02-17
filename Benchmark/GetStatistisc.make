# This is a quick and dirty makefile to screenscrape the jobhistory pages to get the stats I was looking for.

ClusterJobhistoryBaseUrl = "http://node1:19888/jobhistory/job/job_1390217611646_"
FirstJob = 0250
LastJob  = 0267

JobTargets = $(shell for I in `seq -w $(FirstJob) $(LastJob)` ; do echo Results-$$I.txt ; done)

Results.txt:: $(JobTargets) 
	@echo "Merge results"
	@cat Results-*.txt > $@

clean::
	rm -f Results.txt

Results-%.txt: Extract-%.txt
	@( ID=$$(echo $@ | cut -d'-' -f 2 | cut -d'.' -f1) ; echo "Calculate run $${ID}" )
	@cat $< | sed 's@\([0-9]*\)hrs, @(\1*3600)+@g;s@\([0-9]*\)mins, @(\1*60)+@g;s@\([0-9]*\)sec@(\1)@g;s@ @@g;s@|@ @g' |\
	while read name splits elapsed avgmap ; do echo "$${name} | $${splits} | $$((elapsed)) | $$((avgmap)) | $$(( (avgmap) * (splits) )) " ; done >$@
	
clean::
	rm -f $(JobTargets)

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
	curl -s "$(ClusterJobhistoryBaseUrl)$${ID}" | fgrep -v "Not Found"> $@ )

clean::
	rm -f Output-*.txt
