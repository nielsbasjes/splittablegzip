/**
 * Making GZip Splittable for Apache Hadoop
 * Copyright (C) 2011-2019 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.hadoop.examples;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.LongSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class WordCount extends Configured implements Tool {

    public static class WordSplittingMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

        private static final LongWritable ONE  = new LongWritable(1);
        private static final Text         WORD = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] words = value.toString().split(" ");
            for (String word : words) {
                WordSplittingMapper.WORD.set(word);
                context.write(WordSplittingMapper.WORD, ONE);
            }
        }
    }

    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(getConf());
        job.setJobName("Wordcount");

        job.setJarByClass(getClass());
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        if (args.length > 2 && "-s".equals(args[2])) {
            if (args.length < 4) {
                System.out.println("Invalid parameters. Usage: -s <split.maxsize>");
                return -1;
            }
            job.getConfiguration().set("io.compression.codecs", "nl.basjes.hadoop.io.compress.SplittableGzipCodec");
            job.getConfiguration().setLong("mapreduce.input.fileinputformat.split.minsize", Long.parseLong(args[3])-10000);
            job.getConfiguration().setLong("mapreduce.input.fileinputformat.split.maxsize", Long.parseLong(args[3]));
            job.setJobName("Wordcount-"+args[3]);
        }

        job.setInputFormatClass(TextInputFormat.class);

        job.setMapperClass(WordSplittingMapper.class);
        job.setCombinerClass(LongSumReducer.class);
        job.setReducerClass(LongSumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setOutputFormatClass(TextOutputFormat.class);

        return (job.waitForCompletion(true) ? 1 : 0);

    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new WordCount(), args);
        System.exit(res);
    }
}
