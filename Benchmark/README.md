#Why?
People have asked me to provide some benchmarking information because theyn were skeptical about the advantages of this codec.

So this project is simply a WordCount implementation that runs the same file with various settings for this codec and (as a reference) against the default GzipCodec.

#Input
I took the list of installed rpms on my machine, replaced the '.' and '-' with ' ' and concatenated ithe gzipped version of this 100000 times.
The result was a file that was 1101900000 bytes in size (i.e. just below 1.1GB).

#The cluster
The cluster I ran this on is a series of 8 old developer workstations. 
Master: 4 core CPU, single disk.
Workers: 7 systems (6x4 core, 1x2 core), all have a single disk.

Note that these systems do NOT have idential specifications. So in the below tables when running only a few mappers you see that 'it matters' which of these system was actually runnin that specific mapper.

Given the yarn config in place on this cluster can run at most 12 mappers simultaneously.

This cluster was fully idle during all benchmark runs.

All output files were checked and resulted in the same md5 of the output file.

#Results
##A 1.1 GB input file doing a wordcount.

Splitsize | Splits | Elapsed | Avg. Map Time | Total Map Time
 --:|--:|--:|--:|--:|
GzipCodec  |  1 | 1470 sec  | 1462 sec | 1462 sec |
1101900001 |  1 | 1430 sec  | 1421 sec | 1421 sec |
 550950001 |  2 |  715 sec  |  699 sec | 1398 sec |
 367300001 |  3 |  507 sec  |  483 sec | 1449 sec |
 275475001 |  4 |  394 sec  |  374 sec | 1496 sec |
 220380001 |  5 |  329 sec  |  298 sec | 1490 sec |
 183650001 |  6 |  301 sec  |  268 sec | 1608 sec |
 157414286 |  7 |  233 sec  |  219 sec | 1533 sec |
 137737501 |  8 |  236 sec  |  206 sec | 1648 sec |
 122433334 |  9 |  204 sec  |  179 sec | 1611 sec |
 110190001 | 10 |  197 sec  |  176 sec | 1760 sec |
 100172728 | 11 |  171 sec  |  152 sec | 1672 sec |
  91825001 | 12 |  175 sec  |  147 sec | 1764 sec |
  84761539 | 13 |  264 sec  |  136 sec | 1768 sec |
  78707143 | 14 |  248 sec  |  125 sec | 1750 sec |  
  73460001 | 15 |  246 sec  |  118 sec | 1770 sec |
  68868751 | 16 |  235 sec  |  111 sec | 1776 sec |
  64817648 | 17 |  253 sec  |  108 sec | 1836 sec |
  61216667 | 18 |  232 sec  |  105 sec | 1890 sec |
  57994737 | 19 |  229 sec  |   99 sec | 1881 sec |
  55095001 | 20 |  213 sec  |   96 sec | 1920 sec |
  52471429 | 21 |  215 sec  |   92 sec | 1932 sec |
  50086364 | 22 |  205 sec  |   88 sec | 1936 sec |
  47908696 | 23 |  205 sec  |   84 sec | 1932 sec |
  45912501 | 24 |  238 sec  |   82 sec | 1968 sec |
  44076001 | 25 |  238 sec  |   81 sec | 2025 sec |

##A 220 MB input file doing a wordcount.
Splitsize | Splits | Elapsed | Avg. Map Time | Total Map Time
 --:|--:|--:|--:|--:|
GzipCodec |  1 | 274 sec | 267 sec |  267 sec
220380001 |  1 | 282 sec | 275 sec |  275 sec
110190001 |  2 | 156 sec | 150 sec |  300 sec
 73460001 |  3 | 120 sec | 110 sec |  330 sec
 55095001 |  4 |  89 sec |  81 sec |  324 sec
 44076001 |  5 |  70 sec |  64 sec |  320 sec
 36730001 |  6 |  66 sec |  59 sec |  354 sec
 31482858 |  7 |  55 sec |  48 sec |  336 sec
 27547501 |  8 |  53 sec |  44 sec |  352 sec
 24486667 |  9 |  49 sec |  40 sec |  360 sec
 22038001 | 10 |  46 sec |  37 sec |  370 sec
 20034546 | 11 |  44 sec |  35 sec |  385 sec
 18365001 | 12 |  43 sec |  33 sec |  396 sec
 16952308 | 13 |  59 sec |  30 sec |  390 sec
 15741429 | 14 |  57 sec |  29 sec |  406 sec
 14692001 | 15 |  62 sec |  27 sec |  405 sec
 13773751 | 16 |  59 sec |  26 sec |  416 sec
 12963530 | 17 |  56 sec |  26 sec |  442 sec
 12243334 | 18 |  57 sec |  24 sec |  432 sec
 11598948 | 19 |  53 sec |  23 sec |  437 sec
 11019001 | 20 |  54 sec |  22 sec |  440 sec
 10494286 | 21 |  53 sec |  21 sec |  441 sec
 10017273 | 22 |  60 sec |  21 sec |  462 sec
  9581740 | 23 |  58 sec |  20 sec |  460 sec
  9182501 | 24 |  58 sec |  20 sec |  480 sec
  8815201 | 25 |  60 sec |  19 sec |  475 sec
  8476154 | 26 |  62 sec |  20 sec |  520 sec
  8162223 | 27 |  57 sec |  18 sec |  486 sec
  7870715 | 28 |  64 sec |  18 sec |  504 sec
  7599311 | 29 |  64 sec |  18 sec |  522 sec
  7346001 | 30 |  62 sec |  18 sec |  540 sec
  7109033 | 31 |  61 sec |  17 sec |  527 sec
  6886876 | 32 |  62 sec |  16 sec |  512 sec
  6678182 | 33 |  62 sec |  16 sec |  528 sec
  6481765 | 34 |  64 sec |  16 sec |  544 sec
  6296572 | 35 |  70 sec |  16 sec |  560 sec
  6121667 | 36 |  64 sec |  16 sec |  576 sec
  5956217 | 37 |  68 sec |  15 sec |  555 sec
  5799474 | 38 |  67 sec |  15 sec |  570 sec
  5650770 | 39 |  70 sec |  15 sec |  585 sec
  5509501 | 40 |  72 sec |  15 sec |  600 sec
  5375122 | 41 |  72 sec |  15 sec |  615 sec
  5247143 | 42 |  69 sec |  14 sec |  588 sec
  5125117 | 43 |  73 sec |  14 sec |  602 sec
  5008637 | 44 |  71 sec |  14 sec |  616 sec
  4897334 | 45 |  72 sec |  13 sec |  585 sec
  4790870 | 46 |  72 sec |  13 sec |  598 sec
  4688937 | 47 |  72 sec |  13 sec |  611 sec
  4591251 | 48 |  77 sec |  13 sec |  624 sec
  4497552 | 49 |  83 sec |  14 sec |  686 sec
  4407601 | 50 |  85 sec |  13 sec |  650 sec


##A 140 MB input file doing a wordcount.

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


#Conclusions
As long there is spare CPU capacity the solution allows for a very good scaling of the processing speed.
A file that would normally take about 24 minutes to 'wordcount' only takes about 3 minutes.

After the maximal paralelisation point (12) we see that the processing time jumps up and stays there. This can be explained by the fact that only after the first mapper of the 12 mappers is finished the 13th can start.
