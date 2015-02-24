package edu.upenn.cis455.mapreduce.worker;

import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.upenn.cis455.storage.DBFunc;
import edu.upenn.cis455.xpathengine.HttpClientCrawler;

public class URLDistributor extends Thread {
	public static Logger log = Logger.getLogger(URLDistributor.class);
	int numWorkers;
	String[] workers;

	URLDistributor(int n, String[] wor) {
		numWorkers = n;
		workers = wor;

	}

	public void run() {

		while (CrawlerMaster.running) {

			ArrayList<String> urls_desc = new ArrayList<String>();
			urls_desc.addAll(CrawlerMaster.ExtractedLinks);

			synchronized (CrawlerMaster.ExtractedLinks) {
				CrawlerMaster.ExtractedLinks.clear();
			}
			
			String[] workersBody = new String[numWorkers];
			for (int i = 0; i < numWorkers; i++) {
				workersBody[i] = "";
			}
			// loop thru all links and find links for respective workers
			

			int total=urls_desc.size();
			for (int i=0;i<total;i++) {

				String url_d=urls_desc.get(i);
				String url=url_d;

				if (url_d.contains("\t")) {
					try {
						url = url_d.substring(0, url_d.indexOf('\t'));
						
						
					} catch (Exception e) {
						url = url_d;
					}
				}

				MessageDigest md = null;
				try {
					md = MessageDigest.getInstance("SHA1");
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String host[] = HttpClientCrawler.get_hostname_reqURL(url);

				byte[] digest = md.digest(host[0].getBytes());
				int target_worker = getWorkerNum(digest);
				System.out.println("target worker for "+host[0]+"&&&&&&&&&& "+target_worker);

				if (target_worker == WorkerServlet.me) {
					CrawlerMaster.URLFrontier.enqueue(url_d);
				} else {
					workersBody[target_worker - 1] = workersBody[target_worker - 1]
							+ url_d + "\n";
					
				}
			}

			sendRequest(workersBody);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}

	}

	public void sendRequest(String[] body) {
		HttpClientReq pushDataClient = new HttpClientReq();

		for (int i = 0; i < numWorkers; i++) {
			
			String[] worker_addr = workers[i].split(":");
			System.out.println("sending push data request to "+worker_addr[1]+"-- body:"+body[i]+"--------------------");
			pushDataClient.sendRequest(worker_addr[0], "/worker/pushdata",
					Integer.parseInt(worker_addr[1]), "POST", body[i]);
			

		}
	}

	public int getWorkerNum(byte[] digest) {

		int result = 0;
		String allfs = "";

		for (int i = 0; i < 40; i++) {
			allfs = allfs + "f";
		}
		BigInteger mask = new BigInteger(allfs, 16);

		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16)
					.substring(1));

		}
		BigInteger digest_b = new BigInteger(sb.toString(), 16);
		BigInteger mask_window = mask.divide(BigInteger.valueOf(numWorkers));

		for (int i = 1; i <= numWorkers; i++) {

			BigInteger window = mask_window.multiply(BigInteger.valueOf(i));

			if (digest_b.compareTo(window) < 0) {
				result = i;
				break;
			}
			window = window.add(window);
		}

		if (result == 0)
			result = numWorkers;

		return result;
	}
}
