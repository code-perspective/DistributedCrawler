package edu.upenn.cis455.mapreduce.worker;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import edu.upenn.cis455.xpathengine.HttpClientCrawler;

public class Robots {

	public static Logger log = Logger.getLogger(CrawlerThread.class);

	ConcurrentMap<String, RobotsRules> robots = new ConcurrentLinkedHashMap.Builder<String, RobotsRules>()
		    .maximumWeightedCapacity(2)
		    .build();

	
//	public static void main(String args[]){
//		Robots r = new Robots();
//		r.getCrawlDelay("http://crawltest.cis.upenn.edu/cnn/");
//		r.getCrawlDelay("http://en.wikipedia.org/wiki/Keanu_Reeves");
//		r.getCrawlDelay("http://www.dmoz.org");
//		r.getCrawlDelay("http://www.cis.upenn.edu/cnn/");
//		
//	}

	public boolean isAllowedtoCrawl(String url) {

		log.info("getting crawl delay");
		String[] host_req = HttpClientCrawler.get_hostname_reqURL(url);
		String hostname = host_req[0];
		String req_url = host_req[1];

		if (!robots.containsKey(hostname))
			getRobotsFile(url);

		return isAllowedToCrawl(req_url, robots.get(hostname));

	}

	public int getCrawlDelay(String url) {
		String[] host_req = HttpClientCrawler.get_hostname_reqURL(url);
		String hostname = host_req[0];

		if (!robots.containsKey(hostname))
			getRobotsFile(url);

		return robots.get(hostname).crawldelay;
	}

	public void getRobotsFile(String url) {

		try {

			String[] host_req = HttpClientCrawler.get_hostname_reqURL(url);

			String hostname = host_req[0];
			String crawl_delay = null;

			HttpClientCrawler robot_client = new HttpClientCrawler();
			Long l = null;

			System.out.println("\t sending get request for robots.txt :" + url);
			robot_client.makeRequest("http://" + hostname + "/robots.txt", l,
					"GET");

			String currentUserAgent = "";

			if (robot_client.body == null) {
				RobotsRules allow_all_rules = new RobotsRules();
				allow_all_rules.addStarRule("Allow:/");
				allow_all_rules.addcis455Rule("Allow:/");
				robots.put(hostname, allow_all_rules);
				
				return;
			}

			
			RobotsRules all_rules = new RobotsRules();

			for (String line : robot_client.body.split("\n")) {
				
				if (line.startsWith("#") || line.length() == 0)
					continue;
				
				
				else if (line.startsWith("User-agent"))
					currentUserAgent = line.split(":")[1].trim();
				
				
				else if (line.startsWith("Crawl-delay")) {
					
					if (currentUserAgent.equals("cis455crawler"))
						crawl_delay = line.split(":")[1].trim();
					else if (currentUserAgent.equals("*")) {
						if (crawl_delay == null)
							crawl_delay = line.split(":")[1].trim();
					}
					
					
				} else {
					
					if (currentUserAgent.equals("*")) {
						all_rules.addStarRule(line);
					} else if (currentUserAgent.equals("cis455crawler")) {
						all_rules.addcis455Rule(line);
					}
				}

			}

			int delay = 0;
			try {
				delay = Integer.parseInt(crawl_delay);
			} catch (Exception e) {
				delay = 0;
			}

			all_rules.setCrawlDelay(delay);
			robots.put(hostname, all_rules);
			

		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}

	}

	@SuppressWarnings("deprecation")
	public boolean isAllowedToCrawl(String req, RobotsRules rules) {

		boolean result = true;

		if (rules.cis455_rules.size() != 0) {
			for (String s : rules.cis455_rules) {
				try {
					s = URLDecoder.decode(s);
					if (s.startsWith("Disallow")) {
						String disallowed_url = s.split(":")[1].trim();
						if (req.startsWith(disallowed_url))
							result = false;
					} else if (s.startsWith("Allow")) {
						result = true;
						break;
					}
				} catch (Exception e) {
					log.error(e.getLocalizedMessage(), e);
					continue;
				}
			}
		}else{
			if (rules.star_rules.size() != 0) {
				for (String s : rules.star_rules) {

					try {
						s = URLDecoder.decode(s);
						if (s.startsWith("Disallow")) {
							String disallowed_url = s.split(":")[1].trim();
							if (req.startsWith(disallowed_url))
								result = false;
						} else if (s.startsWith("Allow")) {
							result = true;
							break;
						}
					} catch (Exception e) {
						log.error(e.getLocalizedMessage(), e);
						continue;
					}
				}
			}
		}

		return result;

	}

}
