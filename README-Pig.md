# Using the SplittableGZipCodec in Apache Pig
To use this in an Apache Pig you must make sure this library has been added as a dependency.

Simply download the prebuilt library from Maven Central.
You can find the latest jar file here: [https://search.maven.org/search?q=a:splittablegzip](https://search.maven.org/search?q=a:splittablegzip)

Then in Pig you need to load this jat file into your job.

    REGISTER splittablegzip-*.jar

and then before actually running the job you set the configuration using something like this:

    -- Set the compression codecs so that the GZipCodec is removed and the splittable variant is added.
    -- In this example we simply remove everything and only have the splittable codec.
    SET io.compression.codecs nl.basjes.hadoop.io.compress.SplittableGzipCodec

    -- Tune the settings how big the splits should be.
    SET mapreduce.input.fileinputformat.split.minsize $splitsize
    SET mapreduce.input.fileinputformat.split.maxsize $splitsize

    -- Avoid PIG merging multiple splits back to a single mapper.
    -- http://stackoverflow.com/q/17054880
    SET pig.noSplitCombination true

And after this the actual job is done.
