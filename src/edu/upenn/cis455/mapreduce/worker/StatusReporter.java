package edu.upenn.cis455.mapreduce.worker;

import java.io.File;

import org.apache.log4j.Logger;

public class StatusReporter extends Thread {

	public static Logger log = Logger.getLogger(StatusReporter.class);
	String masterIP;
	int masterPort;

	public StatusReporter(String ip, int port) {
		
		masterIP = ip;
		masterPort = port;
	}

	public void run() {
		
		log.info("status reporter started");

		while (true) {
			try {
				File f = new File(WorkerServlet.storageDir+"crawledDocuments");
				int file_count = f.list().length;
				HttpClientReq client = new HttpClientReq();
				String req_url = "/master/workerstatus?" 
						+ "port="+ WorkerServlet.port_listening_on 
						+ "&status="+ WorkerServlet.status
						+ "&validcount="+ Integer.toString(CrawlerMaster.valid_urls_count)
						+ "&totalcount="+ Integer.toString(CrawlerMaster.count)
						+ "&filecount="+ Integer.toString(file_count);
				log.info(req_url);

				client.sendGETRequest(masterIP, req_url, masterPort, "GET");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
}
