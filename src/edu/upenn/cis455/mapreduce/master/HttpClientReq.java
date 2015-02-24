package edu.upenn.cis455.mapreduce.master;

import java.io.IOException;

import java.io.PrintWriter;
import java.net.Socket;

import org.apache.log4j.Logger;

public class HttpClientReq {
	public static Logger log = Logger.getLogger(HttpClientReq.class);

	public void sendRequest(String hostName, String req_url, int portNumber,
			String method, String body) {
		log.info("Sending request to host : " + hostName +" request url: "+ req_url + " method : " + method);
		Socket socket = null;
		try {
//			System.out.println(hostName + "," + portNumber);
			socket = new Socket(hostName, portNumber);
			sendRequestHeader(socket, req_url, hostName, method, body);
			
		} catch (IOException e) {
			System.out.println("error creating socket");
			
			 e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * program to send HTTP GET request to a server Socket: socket indicating
	 * the server req_url: url to be used in GET method host: String host to be
	 * used in host header of the HTTP GET header
	 */
	private void sendRequestHeader(Socket socket, String req_url, String host,
			String method, String body) throws IOException {

		PrintWriter pr = new PrintWriter(socket.getOutputStream());

		String request = "";
		request = request + method + " " + req_url + " HTTP/1.1\r\n";
		request = request + "Host:" + host + "\r\n";
		request = request + "User-Agent: cis455crawler\r\n";
		if(body!=null){
			request=request+"Content-Length: "+body.length()+"\r\n";
			request=request+"Content-Type: "+"application/x-www-form-urlencoded \r\n";
		}
		
		request = request + "Connection:close \r\n\r\n";
		
		
		if (body != null)
			request = request + body;
		
//		System.out.println(request);
		pr.write(request);

		pr.flush();
		pr.close();

	}

}
