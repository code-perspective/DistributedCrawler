package edu.upenn.cis455.mapreduce.job;

import edu.upenn.cis455.mapreduce.Context;
import edu.upenn.cis455.mapreduce.Job;

public class WordCount implements Job {

	public void map(String key, String value, Context context) {
		String[] words = value.split(" ");
		for (String word : words) {
			try {
				context.write(word, Integer.toString(1));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void reduce(String key, String[] values, Context context) {
		int count = 0;
		for (int i = 0; i < values.length; i++) {
			try {
				count = count + Integer.parseInt(values[i]);
			} catch (NumberFormatException e) {

			}
		}
		context.write(key, Integer.toString(count));

	}

}
