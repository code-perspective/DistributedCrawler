package edu.upenn.cis455.xpathengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

public class HttpClientCrawler {

	public static Logger log = Logger.getLogger(HttpClientCrawler.class);
	public Properties response_properties = new Properties();
	public Properties response_header = new Properties();

	public String content_type = null;
	public String cookie_header = null;
	public static int content_limit = 1000000;
	public Date if_modified_since = null;
	public String method = "GET";
	public String url = "";
	public String body = "";
	public boolean https = false;
	public Socket socket = null;
	PrintWriter pr = null;
	int redirect_count = 0;

	/**
	 * program to get BODY of the docuemnt at the given url input: url from
	 * where document needs to be fetched
	 */

	public void makeRequest(String url, Long if_modified, String meth) {
		try {
			if (if_modified != null)
				if_modified_since = new Date(if_modified);
			method = meth;
			this.url = url;
			makeRequest();
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		}
	}

	public void makeRequest() {

		String[] url_split = get_hostname_reqURL(url);
		String hostName = url_split[0];
		String req_url = url_split[1];

		int portNumber = 80;

		try {
			System.out.println("\t url: " + url + "\thostname: " + hostName
					+ ", port: " + portNumber);
			log.info("making req to url: " + url + "\thostname: " + hostName
					+ ", port: " + portNumber);

			socket = new Socket(hostName, portNumber);

			if (https) {
				log.info("https request");
				if (!method.equals("HEAD")) {
					log.info("GET request https");
					getBODYHttps();
				} else {
					log.info("HEAD request https");
					sendHEADHttps();
				}
			} else {
				log.info("http request");
				sendRequestHeader(socket, req_url, hostName);
				log.info("request header sent");
				fetchDocument(socket);
				log.info("fetching document done");
			}

		} catch (IOException e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		}

	}

	void sendHEADHttps() {
		System.out.println("sending https head");
		HttpsURLConnection con = null;
		try {
			con = (HttpsURLConnection) new URL(url).openConnection();
			con.setRequestMethod("HEAD");
			con.addRequestProperty("User-Agent", "cis455crawler");
			con.setConnectTimeout(30000);
			con.setReadTimeout(30000);
			response_properties.put(NamesInHash.responseCode,
					String.valueOf(con.getResponseCode()));
			System.out.println("received response: " + con.getResponseCode());
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		} finally {
			con.disconnect();
		}

	}

	void getBODYHttps() {
		log.info("Fetching body through https from " + url);
		String result = "";
		URL httpsUrl;
		BufferedReader br = null;
		HttpsURLConnection con = null;
		try {
			httpsUrl = new URL(url);
			con = (HttpsURLConnection) httpsUrl.openConnection();
			con.setConnectTimeout(30000);
			con.setReadTimeout(30000);
			if (con != null) {
				br = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String input;
				while ((input = br.readLine()) != null) {
					result = result + input + "\n";
				}

			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		} finally {
			try {
				br.close();
				con.disconnect();
			} catch (IOException e) {
				log.error(e.getLocalizedMessage(), e);
				e.printStackTrace();
			}
		}

		body = result;
	}

	/**
	 * program to send HTTP GET request to a server Socket: socket indicating
	 * the server req_url: url to be used in GET method host: String host to be
	 * used in host header of the HTTP GET header
	 */
	private void sendRequestHeader(Socket socket, String req_url, String host) {

		try {
			pr = new PrintWriter(socket.getOutputStream());

			String request = "";
			request = request + method + " " + req_url + " HTTP/1.1\r\n";
			log.info(request);
			request = request + "Host:" + host + "\r\n";
			request = request + "User-Agent: cis455crawler\r\n";
			if (if_modified_since != null) {

				request = request + NamesInHash.ifModifiedSince + ": "
						+ NamesInHash.formatDate(if_modified_since) + "\r\n";
			}

			if (cookie_header != null) {
				request = request + cookie_header;
			}

			request = request + "Connection:close \r\n\r\n";
			pr.write(request);

			// System.out.println(request);

			pr.flush();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		}

		// System.out.println(request);
	}

	/**
	 * Program to fetch document from a Reader Sets the headers and returns the
	 * BODY of the request. Also sets the content-type whether it is html or xml
	 * 
	 * @throws IOException
	 */
	public void fetchDocument(Socket socket) {

		log.info("getting response from " + url);

		try {
			socket.setSoTimeout(30000);

			BufferedReader socketReader = null;
			try {
				socketReader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				String line;
				Boolean flag = true;
				String header_in_req = "";
				while (true) {

					line = socketReader.readLine();
					header_in_req = header_in_req + "\n" + line;
					if (line == null)
						break;

					if (flag) {
						log.info(line);
						response_properties.put(NamesInHash.responseCode,
								line.split(" ")[1].trim());
						response_properties
								.put(NamesInHash.response_line, line);
						flag = false;
					} else {
						if (line != "\r" || line != "\r\n" || line != "\n") {

							String[] head_str = line.split(":");
							if (response_header.containsKey(head_str[0].trim())) {
								String s = (String) response_header
										.get(head_str[0].trim());
								s = s
										+ "\n"
										+ line.substring(line.indexOf(':') + 1)
												.trim();
								response_header.put(head_str[0].trim(), s);

							} else {
								response_header.put(head_str[0].trim(), line
										.substring(line.indexOf(':') + 1)
										.trim());
							}

							if (line.length() == 0) {
								break;
							}
						}
					}
				}

			} catch (IOException e) {
				log.error(e.getLocalizedMessage(), e);
				e.printStackTrace();
			}

			String responseCode = response_properties
					.getProperty(NamesInHash.responseCode);
			if (responseCode == null) {
				response_properties.put(NamesInHash.responseCode, "404");
				return;
			}

			if (responseCode.equals("301") || responseCode.equals("302")) {

				log.info("redirection 301/302 " + url);
				redirect_count++;
				if (redirect_count > 3) {
					response_properties.put(NamesInHash.responseCode, "404");
					System.out
							.println("round url&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&777");
					return;
				}

				if (response_header.containsKey(NamesInHash.location)) {
					String redirect_url = response_header
							.getProperty(NamesInHash.location);

					String redir_host = get_hostname_reqURL(redirect_url)[0];

					if (redir_host == null)
						redirect_url = getAbsoluteURL("http://"
								+ get_hostname_reqURL(url)[0], redirect_url);

					cookie_header = setCookieHeader();

					if (redirect_url.startsWith("https")) {
						https = true;
						clearHeaders();
						url = redirect_url;
						makeRequest();
					} else if (cookie_header != null
							|| (cookie_header == null && !redirect_url
									.equals(url))) {
						url = redirect_url;
						clearHeaders();
						makeRequest();
					}

				}
			}

			if (!method.equals("HEAD")) {
				log.info("fetching body for: " + url);
				fetchBody(socketReader);
			} else {
				try {
					pr.close();
					socketReader.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage(), e);
				}
			}

		} catch (SocketException e1) {
			log.error(e1.getLocalizedMessage(),e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public String setCookieHeader() {

		// Using "%" as seperator .. "|" didnt work may be some strange reason
		String[] cookies = null;
		if (response_header.containsKey("Set-Cookie")) {

			// log.info("setting cookie " +url);
			String list = response_header.getProperty(("Set-Cookie"));
			if (list.contains("\n")) {
				String[] temp = list.split("\n");
				cookies = new String[temp.length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = temp[i].trim();
					cookies[i] = temp[i].substring(0, temp[i].indexOf(";"));
				}

			} else {
				cookies = new String[1];
				cookies[0] = list.substring(0, list.indexOf(";"));
			}
		} else {
			return null;
		}
		String result = "Cookie: ";
		for (int i = 0; i < cookies.length; i++) {
			result = result + cookies[i] + "; ";
		}
		result = result.trim();
		return result.substring(0, result.length() - 1) + "\r\n";

	}

	public void clearHeaders() {
		response_properties.clear();
		response_header.clear();
	}

	public void fetchBody(BufferedReader socketReader) {

		String body_res = "";

		if (response_properties.getProperty(NamesInHash.responseCode).equals(
				"200")) {

			String b = "";
			try {
				while ((b = socketReader.readLine()) != null) {
					body_res = body_res + "\n" + b;
				}
			} catch (IOException e) {
				log.error(e.getLocalizedMessage(), e);
			} finally {
				try {
					pr.close();
					socketReader.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage(), e);
				}
			}

		}
		body = body_res;

	}

	public static String[] get_hostname_reqURL(String url) {
		URL u;
		String[] result = new String[2];
		result[0] = null;
		result[1] = null;
		try {
			u = new URL(url);

			String hostName = u.getHost();
			String req_url = u.getPath();
			result[0] = hostName;
			result[1] = req_url;

		} catch (MalformedURLException e) {
			log.error(e.getLocalizedMessage(), e);
		}

		return result;

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

			return null;
		}
	}
}
