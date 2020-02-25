package com.demo.ingest.gcsbqingest.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.io.FilenameUtils;

/** Utility class for processing strings. */
public class StringUtil {

	private static final String GCS_URI_PREFIX = "gs://";

	public static String[] splitGcsUri(String gcsUri) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(gcsUri), "gcsUri cannot be null or empty.");
		Preconditions.checkArgument(gcsUri.startsWith(GCS_URI_PREFIX), "gcsUri has to start with gs://");
		String trimmedUri = gcsUri.replaceFirst(GCS_URI_PREFIX, "");
		String[] parts = trimmedUri.split("/", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid GCS URI, should contain both bucket and path.");
		}
		return parts;
	}

	public static String getGcsBaseName(String gcsUri) {
		String[] parts = splitGcsUri(gcsUri);
		return getGcsBaseNameByPath(parts[1]);
	}

	private static String getGcsBaseNameByPath(String path) {
		return FilenameUtils.getBaseName(path);
	}

	public static String getGcsDecompressUri(String bucket) {
		return String.format("%s/decompress", getGcsTempDir(bucket));
	}

	public static String getGcsTempDir(String bucket) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(bucket), "Bucket cannot be null or empty.");
		return String.format("%s%s/temp", GCS_URI_PREFIX, bucket);
	}

	public static String generateGcsUri(String bucket, String path) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(bucket), "Bucket cannot be null or empty");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Path cannot be null or empty");
		return String.format("%s%s/%s", GCS_URI_PREFIX, bucket, path);
	}
}