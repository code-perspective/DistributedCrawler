package edu.upenn.cis455.mapreduce.worker;

import java.io.*;
import java.util.ArrayList;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.upenn.cis455.storage.BerkeleyDBAPI;

public class WorkerServlet extends HttpServlet {
	public static Logger log = Logger.getLogger(WorkerServlet.class);

	static final long serialVersionUID = 455555002;
	public static WorkerStatus status = WorkerStatus.IDLE;
	public static int port_listening_on;
	public static String storageDir;
	public static String BDBStore;
	public static String BDBStoreLinks;
	public static String[] workers;
	public static int numWorkers;
	public static int me;
	public static String log4jDir;

	public void init() {
		System.out
				.println("**************************************************");

		String master_ip_port = getInitParameter("master");

		String[] master_addr = master_ip_port.split(":");
		port_listening_on = Integer.parseInt(getInitParameter("port"));
		storageDir = getInitParameter("storagedir");
		log4jDir = storageDir + "log4j";
		BDBStore = getInitParameter("BDBStorage");
		BerkeleyDBAPI.environmentPath=BDBStore;
		BDBStoreLinks=BDBStore.substring(0,BDBStore.length()-1)+"Links";

		createBDBAndStorageDirectories();
		createRequiredDirectories();

		PropertyConfigurator.configure(log4jDir + "/log4j.properties");

		System.out.println("worker servlet on port: " + port_listening_on
				+ " with storage directory: " + storageDir);

		log.info("worker servlet on port: " + port_listening_on
				+ " with storage directory: " + storageDir + "BDB Store: "
				+ BDBStore + "Master addr: " + master_ip_port);

		StatusReporter sp = new StatusReporter(master_addr[0],
				Integer.parseInt(master_addr[1]));
		sp.start();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("text/html");

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.println("<html><head><title>Worker</title></head>");
			out.println("<body>");

			String path_info = request.getPathInfo();
			if (path_info.equals("/")) {
				sendDirectoryList(out, new File(log4jDir));
			} else {
				BufferedReader reader = null;

				System.out.println(log4jDir + path_info.substring(1));
				try {
					reader = new BufferedReader(new FileReader(new File(
							storageDir + path_info.substring(1))));

					String line;
					while ((line = reader.readLine()) != null) {
						out.println("<p>" + line + "</p>");
					}

					reader.close();

				} catch (IOException e) {

				}
			}
			out.println("</body></html>");
			out.flush();
		} catch (Exception e) {

		} finally {
			out.close();
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) {

		if (request.getPathInfo().startsWith("/runmap")) {
			System.out.println("received runmap");
			log.info("Received Start Crawl Command");
			status = WorkerStatus.CRAWLING;
			map(request);
		} else if (request.getPathInfo().startsWith("/runreduce")) {

			status = WorkerStatus.IDLE;
			System.out.println("received run reduce");
			log.info("Received Stop Crawl Command");
			reduce(request);

		} else if (request.getPathInfo().startsWith("/pushdata")) {
			saveData(request);
		}
	}

	public void sendDirectoryList(PrintWriter out, File f) {

		File[] files = f.listFiles();
		for (File fi : files) {

			out.println("<a href=\"" + f.getName() + "/" + fi.getName() + "\">"
					+ fi.getName() + "</a></br>");

		}
	}

	void map(HttpServletRequest request) {
		String crawlCmd = request.getParameter("crawlCmd");
		String numThreads = request.getParameter("numThreads");
		log.info("Crawl Command state = " + crawlCmd);
		int numWorks = Integer.parseInt(request.getParameter("numWorkers"));
		String[] wors = new String[numWorks];
		
		for (int i = 0; i < numWorks; i++) {
			wors[i] = request.getParameter("worker" + i);
			System.out.println(wors[i]);
			if (wors[i].contains(Integer.toString(port_listening_on)))
				me = i + 1;

		}
		System.out.println(me);

		workers = wors;
		numWorkers = numWorks;

		ArrayList<String> seeds = new ArrayList<String>();
		CrawlerMaster.running = true;

		if (crawlCmd.equals("start")) {
			String del_logs = request.getParameter("deleteLogs");
			refreshCrawlerState(del_logs);

			String seedUrls = request.getParameter("seedUrls");
			log.info("Seeds received as: \n" + seedUrls);

			if (seedUrls != null) {
				String[] str = seedUrls.split("\n");
				for (int j = 0; j < str.length; j++) {
					seeds.add(str[j]);
				}
			}
			log.info("Starting crawl..............");

		} else if( crawlCmd.equals("resume")) {

			seeds = resumeCrawlerState();
			log.info("Seeds Received as: ");
			for (String s : seeds)
				log.info(s);
		}

		CrawlerMaster crawler = new CrawlerMaster(seeds, numThreads);
		crawler.startCrawling();

	}

	void reduce(HttpServletRequest request) {

		// code to save state and stop crawler

		CrawlerMaster.running = false;

		log.info("Saving crawler state");

	}

	ArrayList<String> resumeCrawlerState() {
		ArrayList<String> seeds = new ArrayList<String>();
		File frontierFile = new File(storageDir
				+ "crawlerState/urlFroniter.txt");
		File extractedFile = new File(storageDir
				+ "crawlerState/extractedLinks.txt");
		File lastCrawledFile = new File(storageDir
				+ "crawlerState/lastCrawledTime.txt");
		File countFile = new File(storageDir
				+ "crawlerState/countCrawledDocuments.txt");
		File validCountFile = new File(storageDir
				+ "crawlerState/validCrawledDocuments.txt");
		
		
		File writeFileCountFile = new File(storageDir
				+ "crawlerState/writeFileCount.txt");
		File writeCountFile = new File(storageDir
				+ "crawlerState/writeCount.txt");
		File readFileCountFile = new File(storageDir
				+ "crawlerState/readFileCount.txt");
		seeds.addAll(getSeedsFromFile(extractedFile));
		setUrlFrontier(frontierFile);
		setLastCrawledTime(lastCrawledFile);
		
		CrawlerMaster.count = getCount(countFile);
		CrawlerMaster.valid_urls_count = getCount(validCountFile);
		BlockingQueue.writeCount = getCount(writeCountFile);
		BlockingQueue.writeFileCount = getCount(writeFileCountFile);
		BlockingQueue.readFileCount = getCount(readFileCountFile);
		return seeds;
	}

	ArrayList<String> getSeedsFromFile(File extractedFile) {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(extractedFile));
			while ((line = br.readLine()) != null) {
				result.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	void setUrlFrontier(File frontierFile) {
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(frontierFile));
			while ((line = br.readLine()) != null) {
				CrawlerMaster.URLFrontier.enqueue(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	void setLastCrawledTime(File lastCrawledFile) {
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(lastCrawledFile));
			while ((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				CrawlerMaster.webserver_last_crawled_time.put(str[0],
						Long.parseLong(str[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	int getCount(File countFile) {
		BufferedReader br = null;
		int result = -1;
		ArrayList<String> temp = new ArrayList<String>();
		try {
			String line;
			br = new BufferedReader(new FileReader(countFile));
			while ((line = br.readLine()) != null) {
				temp.add(line);
			}

			result = Integer.parseInt(temp.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	void createBDBAndStorageDirectories() {
		File f = new File(BDBStore);
		if (!f.exists()) {
			createDirectory(BDBStore);
		}

		f = new File(storageDir);
		if (!f.exists()) {
			createDirectory(storageDir);
		}
		
		File f_links = new File(BDBStoreLinks);
		if (!f_links.exists()) {
			createDirectory(BDBStoreLinks);
		}

		

	}

	void createRequiredDirectories() {

		File f = new File(log4jDir);
		if (!f.exists()) {
			createDirectory(log4jDir);

			f = new File(log4jDir + "/log4j.properties");
			PrintWriter pw;
			try {
				pw = new PrintWriter(f);

				pw.write("log4j.rootLogger=INFO, A1\n");
				pw.write("log4j.appender.A1=org.apache.log4j.RollingFileAppender\n");
				pw.write("log4j.appender.A1.File=" + log4jDir
						+ "/LogFile.log\n");

				pw.write("log4j.appender.A1.MaxFileSize=2MB\n");
				pw.write("log4j.appender.A1.MaxBackupIndex=20\n");
				pw.write("log4j.appender.A1.append=true\n");
				pw.write("log4j.appender.A1.layout=org.apache.log4j.PatternLayout\n");
				pw.write("log4j.appender.A1.layout.ConversionPattern=%d{dd HH:mm:ss} %-4r [%t] %-5p %c %x - %m%n\n");
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				log.error(e.getLocalizedMessage(),e);
				e.printStackTrace();
			}
		}

		File crawledDocumentsFile = new File(storageDir + "crawledDocuments");
		if (!crawledDocumentsFile.exists()) {
			createDirectory(storageDir + "crawledDocuments");
			log.info("Directory created: " + storageDir + "crawledDocuments");
		}

		File crawlerStateFile = new File(storageDir + "crawlerState");
		if (!crawlerStateFile.exists()) {
			createDirectory(storageDir + "crawlerState");
			createFile(storageDir + "crawlerState/urlFroniter.txt");
			createFile(storageDir + "crawlerState/extractedLinks.txt");
			createFile(storageDir + "crawlerState/lastCrawledTime.txt");
			createFile(storageDir + "crawlerState/countCrawledDocuments.txt");
		}

		File BDBSt = new File(BDBStore);
		if (!BDBSt.exists()) {
			createDirectory(BDBStore);
		}
		
		File BDBStLinks = new File(BDBStoreLinks);
		if (!BDBStLinks.exists()) {
			createDirectory(BDBStoreLinks);
		}

	}

	void refreshCrawlerState(String del_logs) {

		CrawlerMaster.count = 0;
		CrawlerMaster.valid_urls_count = 0;
		BlockingQueue.readFileCount=0;
		BlockingQueue.writeCount=0;
		BlockingQueue.writeFileCount=0;
		BlockingQueue.shudWriteToFile=false;
		File crawledDocumentsFile = new File(storageDir + "crawledDocuments");
		File crawlerStateFile = new File(storageDir + "crawlerState");
		deleteDirectory(crawledDocumentsFile);
		deleteDirectory(crawlerStateFile);
		deleteDirectory(new File(BDBStore));
		deleteDirectory(new File(BDBStoreLinks));
		createRequiredDirectories();
	}

	void saveData(HttpServletRequest request) {

		BufferedReader reader = null;

		try {
			reader = request.getReader();
			String line;
			
			while ((line = reader.readLine()) != null) {
				CrawlerMaster.URLFrontier.enqueue(line);
			}

			
		} catch (IOException e) {
			log.error(e.getLocalizedMessage(),e);
			System.out.println("Error in adding url to Blocking Queue");
		}
		finally{
			try {
				reader.close();
			} catch (IOException e) {
				log.error(e.getLocalizedMessage(),e);
				e.printStackTrace();
			}
		}

	}



	public void createDirectory(String directory) {
		File Dir = new File(directory);

		// if the directory does not exist, create it
		if (!Dir.exists()) {
			Dir.mkdir();
		}
	}

	public static void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				deleteDirectory(f);
			}
		}
		dir.delete();
	}

	public void createFile(String fileName) {
		File file = new File(fileName);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Error creating file " + fileName);
			}
		}
	}

}
