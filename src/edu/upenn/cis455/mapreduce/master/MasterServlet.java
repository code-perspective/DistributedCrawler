package edu.upenn.cis455.mapreduce.master;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class MasterServlet extends HttpServlet {
	public static Logger log = Logger.getLogger(MasterServlet.class);
	// Logger.getLogger(Dispatcher.class);
	static final long serialVersionUID = 455555001;
	static ConcurrentHashMap<String, CrawlerState> workers = new ConcurrentHashMap<String, CrawlerState>();
	static HashMap<String, String> worker_number = new HashMap<String, String>();
	static MasterStatus status = MasterStatus.IDLE;
	static long started = 0;
	static long time_till_now = 0;

	public void init() {
		getTimePassedTillNow(new File("timeTillNow"));
		File f = new File("log4j");
		if (!f.exists()) {
			System.out.println("creating log directory");
			createDirectory("log4j");

			f = new File("log4j/log4j.properties");
			PrintWriter pw;
			try {
				pw = new PrintWriter(f);

				pw.write("log4j.rootLogger=INFO, A1\n");
				pw.write("log4j.appender.A1=org.apache.log4j.RollingFileAppender\n");
				pw.write("log4j.appender.A1.File=log4j/LogFile.log\n");

				pw.write("log4j.appender.A1.MaxFileSize=2MB\n");
				pw.write("log4j.appender.A1.MaxBackupIndex=20\n");
				pw.write("log4j.appender.A1.append=true\n");
				pw.write("log4j.appender.A1.layout=org.apache.log4j.PatternLayout\n");
				pw.write("log4j.appender.A1.layout.ConversionPattern=%d{dd HH:mm:ss} %-4r [%t] %-5p %c %x - %m%n\n");
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		PropertyConfigurator.configure("log4j/log4j.properties");
		log.info("Master initialized");
		System.out.println("******************* master started");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws java.io.IOException {
		String pathinfo = request.getPathInfo();

		if (pathinfo.startsWith("/workerstatus")) {
			updateWorkerStatus(request);

		} else if (pathinfo.startsWith("/status")) {
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();

			out.println("<html><head><title>Master</title></head>");
			out.println("<body><h2>Shruthi Gorantala</h2>");
			out.println("<h2>shruthig</h2>");
			
			out.println("<h3>Time elapsed:");
			if (started != 0){
				out.println(getTimeDifference(started, System.currentTimeMillis()));			
			}else{
				
				out.println(getTimeDifference(0L, 0L));
			}
			
			out.println("</h3>");

			out.println("<table>");
			out.println("<tr>");
			out.println("<th>IP Address</th>");
			out.println("<th>port</th>");
			out.println("<th>status</th>");
			out.println("<th>Total urls</th>");
			out.println("<th>Docs saved</th>");
			out.println("<th>valid urls</th>");
			out.println("<th>worker link</th>");
			out.println("</tr>");

			int docsCrawled = sendWorkerStatus(out);

			out.println("</table>");

			out.println("<h2>Total Docs Crawled: " + docsCrawled + "</h2>");

			out.print("<form action=\"master\" name=\"myform\" method=\"POST\">");

			out.print("<input type=\"text\" name = \"crawlerstatus\" value=\"resume\"/><br/>");

			out.print("<input type=\"text\" name = \"numThreads\" value=\"5\"/><br/>");
			out.print("<textarea rows=\"20\" cols=\"100\" name = \"seedurls\"></textarea>");

			if (status == MasterStatus.IDLE) {
				out.println("<h2>Start Crawler:</h2>");
				out.print("<input type = \"submit\" value=\"Start/Resume\"/>");

			} else {
				out.println("<h2>Stop Crawler:</h2>");
				out.print("<input type = \"submit\" value=\"Stop\"/>");
			}
			out.print("</form>");
			out.println("</body></html>");
			out.flush();
			out.close();

		}

	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws java.io.IOException {
		String crawlCmd = request.getParameter("crawlerstatus");
		String numthreads = request.getParameter("numThreads");

		// System.out.println("masters current status: " + status);
		log.info("Master status: " + status);

		ArrayList<String> active_workers = new ArrayList<String>();
		ArrayList<String> workers_num = new ArrayList<String>();
		CrawlerState ws = null;
		log.info("Active workers list: ");

		for (String s : workers.keySet()) {
			ws = workers.get(s);

			long currenttime = System.currentTimeMillis();
			long lastupdatedtime = ws.LastUpdatedTime;
			if (currenttime - lastupdatedtime <= 30000) {
				active_workers.add(s);
				workers_num.add(ws.port);
				log.info("Ip=" + ws.ip + " Port=" + ws.port);
			} else {
				workers.remove(s);
				log.info("Removing worker from list of active workers: Ip="
						+ ws.ip + " Port=" + ws.port);
			}
		}

		Collections.sort(workers_num);

		ArrayList<String> final_active_workers = new ArrayList<String>();

		for (int i = 0; i < active_workers.size(); i++) {
			String s = active_workers.get(i);
			String port = s.split(":")[1];
			int index = workers_num.indexOf(port);
			s = s + ":" + index;
			final_active_workers.add(s);
		}

		log.info(final_active_workers);
		
		if (status == MasterStatus.CRAWLING) {
			time_till_now = time_till_now + System.currentTimeMillis()
					- started;
			started = 0;
			writeToFile(Long.toString(time_till_now), "timeTillNow");
			log.info("Stopping crawl");
			
			CrawlStopper stopper = new CrawlStopper(final_active_workers);
			stopper.start();
			
			
			status = MasterStatus.IDLE;
			log.info("Master status changed to IDLE after stopping crawler");

		} else if (status == MasterStatus.IDLE) {

			String seedurls = null;
			if (crawlCmd.equals("start")) {
				time_till_now=0;
				seedurls = request.getParameter("seedurls");
				
				log.info("Crawler State = Starting New Crawl");
				log.info("List of seed urls: ");
				log.info(seedurls);
				
			} else {
				log.info("Crawler State = Resume Previous Crawl");
			}
			
			
			started = System.currentTimeMillis();
			CrawlStarter st = new CrawlStarter(final_active_workers, seedurls,
					crawlCmd, numthreads);
			
			st.start();
			
			log.info(crawlCmd+"ing crawl");
			status = MasterStatus.CRAWLING;
			log.info("Master Status changed to crawling after resuming/starting crawling");
		}

		PrintWriter out = response.getWriter();
		out.write("<html><body>");
		out.write("<p> Crawling Started  Press back to see status</p>");
		out.write("</body></html>");
		out.flush();

	}

	String getTimeDifference(long date1, long date2) {
		String result = "";
		long difference = date2 - date1 + time_till_now;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(tz);
		result = df.format(new Date(difference));
		return result;
	}

	public int sendWorkerStatus(PrintWriter out) {
		CrawlerState ws = null;
		int total_urls = 0;
		int file_urls = 0;
		int valid_urls = 0;
		log.info("Inside sendWorkerStatus method. Sending status to worker");
		for (String s : workers.keySet()) {
			ws = workers.get(s);
			long currenttime = System.currentTimeMillis();
			long lastupdatedtime = ws.LastUpdatedTime;
			if (currenttime - lastupdatedtime <= 30000) {
				out.println("<tr>");
				out.println("<td>" + ws.ip + "</td>");
				out.println("<td>" + ws.port + "</td>");
				out.println("<td>" + ws.status + "</td>");
				out.println("<td>" + ws.total_url_count + "</td>");
				out.println("<td>" + ws.file_count + "</td>");
				out.println("<td>" + ws.valid_count + "</td>");
				out.println("<td><a href=\"http://" + ws.ip + ":" + ws.port
						+ "/worker/\">link</a></td>");
				out.println("</tr>");
				total_urls = total_urls + ws.total_url_count;
				file_urls = file_urls + ws.file_count;
				valid_urls = valid_urls + ws.valid_count;
			} else {
				System.out.println("removing " + s);
				workers.remove(s);
			}
		}
		
		
		out.println("<tr>");
		out.println("<td></td>");
		out.println("<td></td>");
		out.println("<td></td>");
		out.println("<td><h3>" + total_urls + "</h3></td>");
		out.println("<td><h3>" + file_urls + "</h3></td>");
		out.println("<td><h3>" + valid_urls + "</h3></td>");
		out.println("<td></td>");
		out.println("</tr>");
		return file_urls;
	}

	public void updateWorkerStatus(HttpServletRequest request) {

		String ipAddress = request.getRemoteHost();

		String ip_port = ipAddress + ":" + request.getParameter("port");

		String all_url_count = request.getParameter("totalcount");
		int all_url_int = 0;
		if (all_url_count != null)
			all_url_int = Integer.parseInt(all_url_count);
		
		String valid_url_count = request.getParameter("validcount");
		int valid_url_int = 0;
		if (valid_url_count != null)
			valid_url_int = Integer.parseInt(valid_url_count);
		
		String file_count = request.getParameter("filecount");
		int file_int = 0;
		if (file_count != null)
			file_int = Integer.parseInt(file_count);

		CrawlerState work_stat = new CrawlerState(ipAddress,
				request.getParameter("port"), request.getParameter("status"),valid_url_int,file_int ,all_url_int);
		
		 log.info("Current Master status: " + status
		 + ",Worker Status received from " + ip_port + " as "
		 + work_stat.status+work_stat.valid_count);

		workers.put(ip_port, work_stat);
	}

	public void createDirectory(String directory) {
		File Dir = new File(directory);
		if (!Dir.exists()) {
			Dir.mkdir();
		}
	}
	
	
	public void writeToFile(String data, String fileName) {
		BufferedWriter bw=null;
		try {
			
			bw = new BufferedWriter(new FileWriter(new File(fileName)));
			bw.write(data);
			
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e);
		}
		finally{
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("Data written to file");
	}
	
	
	void getTimePassedTillNow(File countFile) {
		
		
		if(!countFile.exists()){
			time_till_now=0;
			return;
		}
		BufferedReader br = null;
		ArrayList<String> temp = new ArrayList<String>();
		try {
			String line;
			br = new BufferedReader(new FileReader(countFile));
			while ((line = br.readLine()) != null) {
				temp.add(line);
			}

			time_till_now = Long.parseLong(temp.get(0));
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
}
