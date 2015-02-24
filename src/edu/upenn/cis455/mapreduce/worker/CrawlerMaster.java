package edu.upenn.cis455.mapreduce.worker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class CrawlerMaster {

	public static Logger log = Logger.getLogger(CrawlerMaster.class);
	
	
	public static boolean running = true;
	public static BlockingQueue URLFrontier = new BlockingQueue(1000);
	public static ArrayList<String> ExtractedLinks = new ArrayList<String>();
	
	public static Robots robots;

	public static HashMap<String, Long> webserver_last_crawled_time = new HashMap<String, Long>();
	static int count = 0;
	static int valid_urls_count = 0;
	int numThreads = 5;
	URLDistributor u;

	public CrawlerMaster(ArrayList<String> seeds, String n) {
		log.info("Crawler master object created");
		int a = 5;
		try {
			a = Integer.parseInt(n);
		} catch (Exception e) {

		}
		if (a != 5)
			numThreads = a;

		for (String s : seeds) {
			ExtractedLinks.add(s);
		}
		robots = new Robots();

		u = new URLDistributor(WorkerServlet.numWorkers,
				WorkerServlet.workers);
		u.start();
		

	}

	public void startCrawling() {
		
		
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < numThreads; i++) {
			CrawlerThread t = new CrawlerThread();
			log.info("Thread created " + t.getName());
			threads.add(t);
			t.start();
		}
		

		for (Thread s : threads) {
			try {
				s.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getLocalizedMessage(), e);;
			}
		}
		
	
		
		System.out.println("))))))))))))))))))))))))))))))))))))))))))))))))))))))");
		
		try {
			u.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		saveCrawlerState();

		
		
		CrawlerMaster.ExtractedLinks.clear();
		CrawlerMaster.webserver_last_crawled_time.clear();
	}

	void saveCrawlerState() {
		log.info("Saving crawler state");
		System.out
				.println("**************************************saved crawler state");
		String urlFrontiersBody = "";
		String visitedUrlsBody = "";
		String lastCrawledTimeBody = "";
		urlFrontiersBody = getUrlFrontiers();
//		System.out.println(urlFrontiersBody.length());

		// visitedUrlsBody = getVisitedUrls();
		// System.out.println(visitedUrlsBody.length());


		lastCrawledTimeBody = getLastCrawledTime();
//		System.out.println(lastCrawledTimeBody.length());

		URLFrontier.putUrlsIntoDoc();

		writeToFile(urlFrontiersBody, WorkerServlet.storageDir
				+ "crawlerState/urlFroniter.txt");
		writeToFile(visitedUrlsBody, WorkerServlet.storageDir
				+ "crawlerState/visitedLinks.txt");
		writeExtractedLinksToFileLineByLine(WorkerServlet.storageDir
				+ "crawlerState/extractedLinks.txt");
		writeToFile(lastCrawledTimeBody, WorkerServlet.storageDir
				+ "crawlerState/lastCrawledTime.txt");

//		System.out.println(CrawlerMaster.count);
		writeToFile(Integer.toString(CrawlerMaster.count),
				WorkerServlet.storageDir
						+ "crawlerState/countCrawledDocuments.txt");
		writeToFile(Integer.toString(valid_urls_count), WorkerServlet.storageDir
				+ "crawlerState/validCrawledDocuments.txt");
		writeToFile(Integer.toString(BlockingQueue.readFileCount), WorkerServlet.storageDir+"crawlerState/readFileCount.txt");
		writeToFile(Integer.toString(BlockingQueue.writeFileCount), WorkerServlet.storageDir+"crawlerState/writeFileCount.txt");
		writeToFile(Integer.toString(BlockingQueue.writeCount), WorkerServlet.storageDir+"crawlerState/writeCount.txt");
		System.out
				.println("********************************************Finished saving***********************************");
		log.info("Crawler state saved");
	}

	public void writeToFile(String data, String fileName) {
		log.info("Data being written to file");
		BufferedWriter bw =null;
		try {
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			bw= new BufferedWriter(new FileWriter(file));
			bw.write(data);
			
		} catch (IOException e) {
			System.out.println("Error: writing to file: " + fileName);
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);;
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("Data written to file");
	}

	public void writeExtractedLinksToFileLineByLine(String fileName) {
		log.info("Data being written to file");
		try {
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			for (String s : ExtractedLinks)
				bw.write(s);
			bw.close();
		} catch (IOException e) {
			System.out.println("Error: writing to file: " + fileName);
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);;
		}
		log.info("Data written to file");
	}

	String getUrlFrontiers() {
		String result = "";
		while (URLFrontier.size() != 0)
			result = result + URLFrontier.dequeue() + "\n";
		return result;
	}

	// String getVisitedUrls() {
	// String result = "";
	// for (int i = 0; i < CrawlerMaster.visitedURLs.size(); i++) {
	// result = result + CrawlerMaster.visitedURLs.get(i) + "\n";
	// }
	// return result;
	// }

//	String getExtractedLinks() {
//		String result = "";
//		for (int i = 0; i < ExtractedLinks.size(); i++) {
//			result = result + ExtractedLinks.get(i) + "\n";
//		}
//		return result;
//	}

	String getLastCrawledTime() {
		String result = "";
		Iterator<String> it = webserver_last_crawled_time
				.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next().toString();
			String value = webserver_last_crawled_time.get(key)
					.toString();
			result = result + key + "\t" + value + "\n";
		}
		return result;
	}

}
