Idea & Observations dump
===

1) Call org.apache.flink.api.common.io.FileInputFormat registerInflaterInputStreamFactory
   to override the default for .gz

org.apache.flink.api.common.io.FileInputFormat 

	/**
	 * A mapping of file extensions to decompression algorithms based on DEFLATE. Such compressions lead to
	 * unsplittable files.
	 */
	protected static final Map<String, InflaterInputStreamFactory<?>> INFLATER_INPUT_STREAM_FACTORIES =
			new HashMap<String, InflaterInputStreamFactory<?>>();

YUCK: They hardcoded assume unsplittable. They insert Bzip2 in there too --> IS splittable.

Hardcoded 'seek'
FileInputFormat::open --> 		if (this.splitStart != 0) {
                          			this.stream.seek(this.splitStart);
                          		}

