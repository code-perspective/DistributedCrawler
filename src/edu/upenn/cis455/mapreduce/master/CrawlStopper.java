package edu.upenn.cis455.mapreduce.master;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CrawlStopper extends Thread {

	public static Logger log = Logger.getLogger(CrawlStarter.class);
	ArrayList<String> active_workers;

	public CrawlStopper(ArrayList<String> aw) {
		active_workers = aw;
	}

	public void run() {
		System.out.println("Job execution started");
		String req_url = "/worker/runreduce";
		

		

		for (String w : active_workers) {
			try {
				System.out.println("Sending run reduce to " + w);
				log.info("Sending Stop Crawling Command to: " + w);
				HttpClientReq client = new HttpClientReq();

				String[] ip_port = w.split(":");
				
				log.info("Run reduce POST body for "+w+" : ");
				client.sendRequest(ip_port[0], req_url,
						Integer.parseInt(ip_port[1]), "POST", null);

			} catch (Exception ex) {
				ex.printStackTrace();
				log.info(ex);
			}
		}

	}

}
