package edu.upenn.cis455.mapreduce.worker;

import java.util.ArrayList;

public class RobotsRules {

	ArrayList<String> star_rules= new ArrayList<String>();
	ArrayList<String> cis455_rules= new ArrayList<String>();
	int crawldelay=0;
	
	
	public void addStarRule(String s){
		star_rules.add(s);
	}
	
	public void addcis455Rule(String s){
		cis455_rules.add(s);
	}
	
	public void setCrawlDelay(int delay){
		crawldelay=delay;
	}

}
