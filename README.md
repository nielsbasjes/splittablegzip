# Making gzip splittable for Hadoop

In many Hadoop production environments you get gzipped files as the raw input. 
Usually these are Apache HTTPD logfiles. 
When putting these gzipped files into Hadoop you are stuck with exactly 1 map 
task per input file. In many scenarios this is fine. 

However when doing a lot of work in this very first map task it may very well be
advantageous to dividing the work over multiple tasks, even if there is a 
penalty for this scaling out.

This addon for Hadoop makes this possible.

## Benchmark
I did benchmarking jobs to see how this solution scales and performs.
The software and the results can be found in the [Benchmark](Benchmark) folder.

In general we can say that you win as long as there is unused capacity in your cluster.

![Graph of the results](Benchmark/Benchmark55.png)

## Requirements
First of all this only works with Hadoop 0.21 and up because this depends on 
the presence of the SplittableCompressionCodec interface.
So Hadoop 1.x is not yet supported (waiting for [HADOOP-7823][HADOOP-7823]).

I tested it with Hortonworks 2.1.2 and Cloudera CDH 4.5.0.

## Downloads

### Sources
Currently it can only be downloaded via github.

[https://github.com/nielsbasjes/splittablegzip][github]

Running this in the codec directory automatically generates an RPM:

    mvn package -Prpm 

### Binary
For normal projects you can simply download the prebuilt version from maven central.
So when using maven you can simply add this to your project

    <dependency>
      <groupId>nl.basjes.hadoop</groupId>
      <artifactId>splittablegzip</artifactId>
      <version>1.2</version>
    </dependency>

## Building
On CDH the build fails if the native gzip could not be loaded for running the unit tests.
To fix this you need to install the native package and set the environment so it can find them for your platform.
I.e. Do something like this before starting the build or loading your IDE.

   export LD_LIBRARY_PATH=/usr/lib/hadoop-0.20/lib/native/Linux-amd64-64

## Installation

1. Place the jar file in the classpath of your hadoop installation.
2. Enable this codec and make sure the regular GzipCodec is NOT used. 
   This can be done by changing the **io.compression.codecs** property to 
   something like this:
    
   **org.apache.hadoop.io.compress.DefaultCodec, nl.basjes.hadoop.io.compress.SplittableGzipCodec, org.apache.hadoop.io.compress.BZip2Codec**
   
3. Set the split size to something that works in your situation. 
   This can be done by setting the appropriate values for 
   **mapreduce.input.fileinputformat.split.minsize** and/or
   **mapreduce.input.fileinputformat.split.maxsize**.

# Choosing the configuration settings
## How it works
For each "split" the gzipped input file is read from the beginning of the file 
till the point where the split starts, thus reading, decompressing and 
discarding (wasting!) everything that is before the start of the split.

**FACT: Files compressed with the Gzip codec are NOT SPLITTABLE. Never have been, never will be.**

This codec offers a trade off between "spent resources" and "scalability" when
reading Gzipped input files by simply always starting at the beginning of the 
file.

So in general this "splittable" Gzip codec will WASTE CPU time and 
FileSystem IO (HDFS) and probably other system resources (Network) too to
reduce the "wall clock" time in some real-life situations.

## When is this useful?
Assume you have a heavy map phase for which the input is a 1GiB Apache httpd logfile. 
Now assume this map takes 60 minutes of CPU time to run. 
Then this task will take 60 minutes to run because all of that CPU time must be 
spent on a single CPU core ... Gzip is not splittable!

This codec will waste CPU power by always starting from the start of the 
gzipped file and discard all the decompressed data until the start of the split 
has been reached.

Decompressing a 1GiB Gzip file usually takes only a few (2-4) minutes.

So if a "60 minutes" input file is split into 4 equal parts then:

1.  the 1st map task will
    *   process the 1st split (15 minutes)
2.  the 2nd map task will
    *   **discard** the 1st split ( 1 minute ).
    *   _process_ the 2nd split (15 minutes).
3.  the 3rd map task will
    *   **discard** the 1st split ( 1 minute ).
    *   **discard** the 2nd split ( 1 minute ).
    *   process the 3rd split (15 minutes).
4.  the 4th task will
    *   **discard** the 1st split ( 1 minute ).
    *   **discard** the 2nd split ( 1 minute ).
    *   **discard** the 3rd split ( 1 minute ).
    *   process the 4th split (15 minutes).

Because all tasks run in parallel the running time in this example would be 
18 minutes (i.e. the worst split time) instead of the normal 60 minutes. 
We have wasted about 6 minutes of CPU time and completed the job in about 30% 
of the original wall clock time.

## Tuning for optimal performance and scalability.
The overall advise is to **EXPERIMENT** with the settings and do benchmarks.

Remember that:

* Being able to split the input has a positive effect scalability IFF there 
  is room to scale out to.
* This codec is only useful if there are less Gzipped input file(s) than 
  available map task slots (i.e. some slots are idle during the input/map phase).
* There is a way of limiting the IO impact. Note that in the above example the 
  4th task will read and decompress the ENTIRE input file.
* Splitting increases the load on (all kinds of) system resources: 
  CPU and HDFS/Network. The additional load on the system resources has a 
  negative effect on the scalability. Splitting a file into 1000 splits will 
  really hammer the datanodes storing the first block of the file 1000 times.
* More splits also affect the number of reduce tasks that follow.
* If you create more splits than you have map task slots you will certainly have 
  a suboptimal setting and you should increase the split size to reduce the number 
  of splits. 

A possible optimum:

* Upload the input files into HDFS with a blocksize that is equal (or a few 
  bytes bigger) than the file size.
  
     `hadoop fs -Ddfs.block.size=1234567890 -put access.log.gz /logs`
     
  This has the effect that all nodes that have "a piece of the file" always have
  "the entire file". This ensures that no network IO is needed for a single node
  to read the file IFF it has it locally available.
* The replication of the HDFS determines on how many nodes the input file is 
  present. So to avoid needless network traffic the number of splits must be 
  limited to AT MOST the replication factor of the underlying HDFS.
* Try to make sure that all splits of an input file are roughly the same size. 
  Don't be surprised if the optimal setting for the split size turns out to be 
  500MiB or even 1GiB.

# Alternative approaches

Always remember that there are alternative approaches:

* Decompress the original gzipped file, split it into pieces and recompress the pieces before offering them to Hadoop.
  For example: [http://stackoverflow.com/questions/3960651](http://stackoverflow.com/questions/3960651)
* Decompress the original gzipped file and compress using a different splittable codec.
  For example by using bzip2 or not compressing at all.

# Implementation notes
There were two major hurdles that needed to be solved to make this work:

* **The reported position depends on the read blocksize.**

  If you read information in "records" the getBytesRead() will return a value 
  that jumps incrementally. Only after a new disk block has been read will the 
  getBytesRead return a new value. "Read" means: read from disk an loaded into 
  the decompressor but does NOT yet mean that the uncompressed information was read.

  The solution employed is that when we get close to the end of the split we switch 
  to a crawling mode. This simply means that the disk reads are reduced to 1 byte, 
  making the position reporting also 1 byte accurate.
  This was implemented in the ThrottleableDecompressorStream.
  
* **The input is compressed.**

  If you read 1 byte (uncompressed) you do not always get an increase in the 
  reported getBytesRead(). This happens because the value reported by getBytesRead
  is all about the filesize on disk (= compressed) and compressed files have less 
  bytes than the uncompressed data. This makes it impossible to make two splits 
  meet accurately.
  The solution is based around the concept that we try to report the position as 
  accurately as possible but when we get really close to the end we stop 
  reporting the truth and we start lying about the position.
  The lie we use to cross the split boundry is that 1 uncompressed byte read is 
  reported as 1 compressed byte increase in position. 
  This was implemented using a simple state machine with 3 different states on 
  what position is reported through the getPos(). 
  The state is essentially selected on the distance to the end.
  These states are:

  *  REPORT

     Normally read the bytes and report the actual disk position in the getPos().
  *  HOLD

     When very close to the end we no longer change the reported file position 
     for a while.
  *  SLOPE

     When we are at the end: start reporting 1 byte increase from the getPos 
     for every uncompressed byte that was read from the stream.

  The overall effect is that the position reporting near the end of the split no
  longer represents the actual position and this makes the position usable for 
  reliably splitting the input stream.
  The actual point where the file is split is shifted a bit to the back of the 
  file (we're talking bytes, not even KiB) where this shift actually depends on 
  the compression levels of the data in the stream. If we start too early the 
  split may happen a byte too early and in the end the last split may lose the 
  last record(s). So that's why we hold for a while and only start the slope at 
  the moment we are certain we are beyond the indicated "end".
  To ensure the split starts at exactly the same spot as the previous split would
  end: we find the start of a split by running over the "part that must be discarded" 
  as-if it is a split. 

# History
Originally this feature was submitted to be part of the core of Hadoop.

See: Gzip splittable ([HADOOP-7076][HADOOP-7076]).

# Created by
This idea was conceived and implemented by [Niels Basjes][nielsbasjes].

[github]: https://github.com/nielsbasjes/splittablegzip "Github"
[HADOOP-7076]: https://issues.apache.org/jira/browse/HADOOP-7076 "JIRA: Splittable Gzip" 
[HADOOP-7823]: https://issues.apache.org/jira/browse/HADOOP-7823 "JIRA: Splittable Bzip2"
[nielsbasjes]: http://niels.basjes.nl "The homepage of Niels Basjes"
