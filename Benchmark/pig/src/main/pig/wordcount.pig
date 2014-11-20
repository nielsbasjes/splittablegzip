REGISTER lib/splittablegzip-*.jar

-- Set the compression codecs so that the GZipCodec is removed and the splittable variant is added.
-- In this example we simply remove everything and only have the splittable codec.
SET io.compression.codecs nl.basjes.hadoop.io.compress.SplittableGzipCodec

-- Tune the settings how big the splits should be.
SET mapreduce.input.fileinputformat.split.minsize $splitsize
SET mapreduce.input.fileinputformat.split.maxsize $splitsize

-- Avoid PIG merging multiple splits back to a single mapper.
-- http://stackoverflow.com/q/17054880
SET pig.noSplitCombination true

-- Regular wordcount
A = load '$file';
B = foreach A generate flatten(TOKENIZE((chararray)$0)) as word;
C = group B by word;
D = foreach C generate COUNT(B), group;

store D into 'wordcount-$splits.txt';

