#Why?
People have asked me to provide some benchmarking information because theyn were skeptical about the advantages of this codec.

So this project is simply a WordCount implementation that runs the same file with various settings for this codec and (as a reference) against the default GzipCodec.


#Input
I took the list of installed rpms on my machine, replaced the '.' and '-' with ' ' and concatenated this file 100000 times before running it through gzip.
The result was a file that was 1397956862 bytes in size (i.e. just below 1.4GB).








-rw-rw-r--.  1 nbasjes nbasjes 1101900000 Feb 11 17:39 words.txt.gz

Splitsize | Splits | Elapsed | Avg. Map Time | Total Map Time
 --:|--:|--:|--:|--:|
1101900000 |  1 |   sec |   sec |   sec |
  70000000 |  2 |    sec |    sec |   sec |
 50000000 |  3 |    sec |    sec |   sec |
 35000000 |  4 |    sec |    sec |   sec |
 28000000 |  5 |    sec |    sec |   sec | 
 25000000 |  6 |    sec |    sec |   sec |
 20000000 |  7 |    sec |    sec |   sec |
 17500000 |  8 |    sec |    sec |   sec |
 15000000 | 10 |    sec |    sec |   sec |
 12500000 | 12 |    sec |    sec |   sec |
 11500000 | 13 |    sec |    sec |   sec |
 10000000 | 14 |    sec |    sec |   sec |
  5000000 | 28 |    sec |    sec |   sec |
  2500000 | 56 |    sec |     sec |   sec | 


Splitsize | Splits | Elapsed | Avg. Map Time | Total Map Time
 --:|--:|--:|--:|--:|
139796017 |  1 | 170 sec | 162 sec | 162 sec |
 70000000 |  2 |  90 sec |  83 sec | 166 sec |
 50000000 |  3 |  66 sec |  58 sec | 174 sec |
 35000000 |  4 |  55 sec |  48 sec | 192 sec |
 28000000 |  5 |  49 sec |  42 sec | 210 sec | 
 25000000 |  6 |  39 sec |  32 sec | 192 sec |
 20000000 |  7 |  38 sec |  30 sec | 210 sec |
 17500000 |  8 |  32 sec |  26 sec | 208 sec |
 15000000 | 10 |  32 sec |  23 sec | 230 sec |
 12500000 | 12 |  31 sec |  21 sec | 252 sec |
 11500000 | 13 |  44 sec |  19 sec | 247 sec |
 10000000 | 14 |  39 sec |  18 sec | 252 sec |
  5000000 | 28 |  42 sec |  11 sec | 308 sec |
  2500000 | 56 |  59 sec |   8 sec | 448 sec | 




Splitsize | Splits | Elapsed | Avg. Map Time | Total Map Time
 --:|--:|--:|--:|--:|
139796017 |  1 | 167 sec | 159 sec | 159 sec |
 70000000 |  2 |  90 sec |  82 sec | 164 sec |
 50000000 |  3 |  65 sec |  57 sec | 171 sec |
 35000000 |  4 |  57 sec |  48 sec | 192 sec |
 28000000 |  5 |  48 sec |  41 sec | 205 sec | 
 25000000 |  6 |  44 sec |  35 sec | 210 sec |
 20000000 |  7 |  38 sec |  29 sec | 203 sec |
 17500000 |  8 |  34 sec |  28 sec | 224 sec |
 15000000 | 10 |  31 sec |  22 sec | 220 sec |
 12500000 | 12 |  31 sec |  21 sec | 252 sec |
 11500000 | 13 |  43 sec |  19 sec | 247 sec |
 10000000 | 14 |  40 sec |  18 sec | 252 sec |
  5000000 | 28 |  43 sec |  12 sec | 336 sec |
  2500000 | 56 |  62 sec |   9 sec | 504 sec | 
