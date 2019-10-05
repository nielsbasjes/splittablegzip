# Using the SplittableGZipCodec in Apache Hadoop MapReduce (Java)
To use this in a Hadoop MapReduce job written in Java you must make sure this library has been added as a dependency.

In Maven you would simply add this dependency

    <dependency>
      <groupId>nl.basjes.hadoop</groupId>
      <artifactId>splittablegzip</artifactId>
      <version>1.2</version>
    </dependency>

Then in Java you would create an instance of the Job that you are going to run

    Job job = ...

and then before actually running the job you set the configuration using something like this:

    job.getConfiguration().set("io.compression.codecs", "nl.basjes.hadoop.io.compress.SplittableGzipCodec");
    job.getConfiguration().setLong("mapreduce.input.fileinputformat.split.minsize", 5000000000);
    job.getConfiguration().setLong("mapreduce.input.fileinputformat.split.maxsize", 5000000000);


NOTE: The ORIGINAL GzipCodec may NOT be in the list of compression codec anymore !
