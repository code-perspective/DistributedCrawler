package edu.upenn.cis455.mapreduce.master;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CrawlStarter extends Thread {
	public static Logger log = Logger.getLogger(CrawlStarter.class);
	ArrayList<String> active_workers;
	String seedUrls;
	String crawlCmd;
	String numThreads;

	
	public CrawlStarter(ArrayList<String> aw, String seedUrls, String crawlCmd,String numThreads) {
		active_workers = aw;
		this.seedUrls = seedUrls;
		this.crawlCmd = crawlCmd;
		this.numThreads=numThreads;

	}

	public void run() {
		
		String req_url = "/worker/runmap";
		String body = "numWorkers=" + active_workers.size() + "&crawlCmd="
				+ crawlCmd+"&numThreads="+numThreads;

		for (int i = 0; i < active_workers.size(); i++){
			String s=active_workers.get(i);
			body = body + "&worker" + s.split(":")[2] + "=" + s;
		}

		int i = 0;
		for (String w : active_workers) {
			
			String req_body_to_send=body;
			try {
				 System.out.println("Sending run map to " + w);
				log.info("Sending Crawl Command to: " + w);
				HttpClientReq client = new HttpClientReq();

				String[] ip_port = w.split(":");
				if (i == 0) {
					if (seedUrls != null && !seedUrls.isEmpty()) {
						req_body_to_send = req_body_to_send + "&seedUrls=" + seedUrls;
						log.info("Sending seed urls to: " + w);
					}
					i++;
				}
				log.info("Run map POST body for "+w+" : ");
				log.info(req_body_to_send);
				
				System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
				System.out.println("worker "+w);
				System.out.println(req_body_to_send);
				client.sendRequest(ip_port[0], req_url,
						Integer.parseInt(ip_port[1]), "POST", req_body_to_send);

			} catch (Exception ex) {
				ex.printStackTrace();
				log.error(ex.getLocalizedMessage(),ex);
			}
		}

	}

}
