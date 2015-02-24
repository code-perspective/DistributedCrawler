package edu.upenn.cis455.mapreduce.worker;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import edu.upenn.cis455.storage.*;
import edu.upenn.cis455.xpathengine.*;

public class CrawlerThread extends Thread {

	public static Logger log = Logger.getLogger(CrawlerThread.class);

	public void run() {

		System.out.println("Crawl Thread started");

		while (CrawlerMaster.running) {
			HttpClientCrawler client_req = null;
			try {
				DBFunc db = new DBFunc();

				int count = incrementCount();
				String url_desc = fetchURLfromURLFrontier();

				String url;

				int split_index = url_desc.indexOf('\t');
				if (split_index == -1) {
					url = url_desc;
				} else {
					url = url_desc.substring(0, split_index);

				}

				String url_hash = getHash(url);

				if (split_index != -1) {
					db.putUrlDesc(url_desc.substring(split_index + 1), url_hash);
				}

				log.info("[" + count + "] Started Crawling URL : " + url);

				if (url == null) {
					log.info("Shutting down crawler");
					break;
				}

				System.out.println("[" + count + "] Crawling: " + url);

				if (isURLAlreadyVisitedinCurrentCrawl(url_hash)) {
					log.info("URL already visited in the Crawl");
					continue;
				}

				db.putURL_URLHash(url_hash, url);

				if (!isAllowedToCrawl(url)) {
					System.out.println(url + " not allowed to crawl");
					log.info(url + " not allowed to crawl by robots.txt ");
					continue;
				}

				if (CrawlerMaster.webserver_last_crawled_time
						.containsKey(HttpClientCrawler.get_hostname_reqURL(url)[0])) {

					Long current_time = System.currentTimeMillis();
					Long last_time = CrawlerMaster.webserver_last_crawled_time
							.get(HttpClientCrawler.get_hostname_reqURL(url)[0]);

					int crawl_delay_expected = getCrawlDelay(url) * 1000;

					int duration_passed = (int) (current_time - last_time);

					if (duration_passed < crawl_delay_expected) {

						System.out
								.println("Respecting Crawl Delay.. Putting Thread to sleep");
						log.info("Respecting crawl delay");
						try {
							Thread.sleep(crawl_delay_expected - duration_passed);
						} catch (InterruptedException e) {

							CrawlerMaster.URLFrontier.enqueue(url);
							log.error(e.getLocalizedMessage(), e);
							continue;
						}
					}

				}

				client_req = new HttpClientCrawler();

				log.info("HEAD request sent");
				// client_req.makeRequest(url, last_crawled, "HEAD");

				client_req.makeRequest(url, null, "HEAD");

				if (isValidToSendGet(client_req)) {
					log.info("Sending get Request");

					CrawlerMaster.webserver_last_crawled_time.put(
							HttpClientCrawler.get_hostname_reqURL(url)[0],
							System.currentTimeMillis());

					if (!client_req.https)
						client_req.clearHeaders();
					client_req.method = "GET";
					client_req.makeRequest();
					processNewDocument(client_req, url_hash);
					log.info("Processing document done");
					int c = incrementValidUrlCount();
					log.info("valid url count incremeting done" + c);

				}

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("\tsome weird exception.. skipping crawl");
				log.error(e.getLocalizedMessage(), e);

			} finally {
				if (client_req != null && client_req.socket != null) {
					try {
						client_req.socket.close();
					} catch (IOException e) {
						log.error(e.getLocalizedMessage(), e);
						e.printStackTrace();
					}
				}
			}

		}

		log.info("************************************ "
				+ Thread.currentThread().getName()
				+ "Thread Ended***********************************");
		System.out.println("************************************ "
				+ Thread.currentThread().getName()
				+ "Thread Ended***********************************");
	}

	synchronized int incrementCount() {
		CrawlerMaster.count++;
		return CrawlerMaster.count;
	}

	synchronized int incrementValidUrlCount() {
		CrawlerMaster.valid_urls_count++;
		return CrawlerMaster.valid_urls_count;
	}

	public String getHash(String text) {
		MessageDigest message = null;
		try {
			message = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);
			;
		}
		message.reset();
		try {
			message.update(text.getBytes("utf8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);
			;
		}
		byte[] databytes = message.digest();
		StringBuffer sb = new StringBuffer();

		// converting the Hash into hexadecimal number
		for (int i = 0; i < databytes.length; i++) {
			sb.append(Integer.toString((databytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		String hashValue = sb.toString();

		return hashValue;
	}

	public int getCrawlDelay(String url) {
		return CrawlerMaster.robots.getCrawlDelay(url);
	}

	public void processNewDocument(HttpClientCrawler client, String url_hash) {

		String url = client.url;

		DBFunc db = new DBFunc();
		// db.putURL_Content(url, client.body);

		db.putURL_Date_DataType(url_hash, System.currentTimeMillis(), "html");
		
		String checksum = getContentChecksum(client);
		db.putURLHash_Checksum(url_hash, checksum);
		
		boolean isContentseen = db.putchecksum(checksum, url_hash);

		Document doc = null;

		if (client.body.length() == 0) {
			return;
		}

		System.out.println("\twriting to file");

		if (!isContentseen) {
			try {
				FileUtils.writeStringToFile(new File(WorkerServlet.storageDir
						+ "crawledDocuments/" + url_hash), client.body);

			} catch (IOException e) {
				System.out.println("\tError writing document to file");
				log.error(e.getLocalizedMessage(), e);
				;
				// e.printStackTrace();
			}
		}
		
		
		try {
			doc = Jsoup.parse(client.body);
		} catch (Exception e) {
			System.out.println("\tException while parsing the document");
			log.error(e.getLocalizedMessage(), e);
		}

		ExtractLinks(doc, url);
	}
	
	

	public String getContentChecksum(HttpClientCrawler client) {
		return getHash(client.body);
	}

	public void ExtractLinks(Document doc, String url) {
		Elements links = doc.getElementsByTag("a");

		DBFunc db = new DBFunc();
		ArrayList<String> link_for_db = new ArrayList<String>();

		int valid_links = 0;
		for (int i = 0; i < links.size(); i++) {

			String relative_link = links.get(i).attr("href");

			if (relative_link.startsWith("#")
					|| relative_link.startsWith("javascript:")
					|| relative_link.startsWith("Javascript:"))
				continue;

			String absolute_url = getAbsoluteURL(url, links.get(i).attr("href"));

			if (absolute_url != null) {
				link_for_db.add(getHash(absolute_url));
				valid_links++;

				String link_text = links.get(i).text();
				link_text = link_text.replace("\n", "*");

				String url_and_text = absolute_url + "\t" + link_text;

				synchronized (CrawlerMaster.ExtractedLinks) {
					if (!CrawlerMaster.ExtractedLinks.contains(url_and_text))
						CrawlerMaster.ExtractedLinks.add(url_and_text);
				}
			}

		}

		System.out.println("\tlinks extracted: " + link_for_db.size()
				+ ", valid: " + valid_links);

		if (link_for_db.size() != 0)
			db.putURL_Links(getHash(url), link_for_db);
	}

	public String getAbsoluteURL(String url, String link) {
		URL base_url;
		try {
			base_url = new URL(url);

			URL now = new URL(base_url, link);
			return now.toString();
		} catch (MalformedURLException e) {
			System.out.println("\t Malformed url excepiton: " + url + ","
					+ link);
			log.error(e.getLocalizedMessage(), e);
			;
			return null;
		}
	}

	public boolean isValidToSendGet(HttpClientCrawler client) {
		try {
			if (client.response_properties
					.getProperty(NamesInHash.responseCode).equals("200")) {

				if (client.response_header
						.containsKey(NamesInHash.contentLength)) {
					int content_length = Integer
							.parseInt(client.response_header
									.getProperty(NamesInHash.contentLength));
					System.out.println("\t" + content_length);
					if (content_length > HttpClientCrawler.content_limit)
						return false;
				}

				if (client.response_header.containsKey(NamesInHash.contentType)) {
					String content_type = client.response_header
							.getProperty(NamesInHash.contentType);

					if (!content_type.contains("html"))
						return false;
				}

				if (client.response_header
						.containsKey(NamesInHash.contentLanguage)) {
					String content_lang = client.response_header
							.getProperty(NamesInHash.contentLanguage);

					if (!content_lang.contains("en"))
						return false;
				}

				return true;
			} else
				return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return false;
		}

	}

	public String fetchURLfromURLFrontier() {
		String url = CrawlerMaster.URLFrontier.dequeue();
		return url;
	}

	public boolean isAllowedToCrawl(String url) {
		try {
			return CrawlerMaster.robots.isAllowedtoCrawl(url);
		} catch (NullPointerException e) {
			log.error(e.getLocalizedMessage(), e);
			;
			return true;
		}
	}

	public boolean isURLAlreadyVisitedinCurrentCrawl(String url_hash) {

		DBFunc db = new DBFunc();
		if (db.getURL_URLHash(url_hash) == null)
			return false;
		else
			return true;

	}

	public Long getDateIfURLAlreadyCrawledInPreviousCrawl(String url) {
		DBFunc db = new DBFunc();
		String[] date_datatype = db.getDate_DataType_from_URL(url);
		Long d = null;
		if (date_datatype != null)
			d = Long.parseLong(date_datatype[0]);
		return d;
	}

}
