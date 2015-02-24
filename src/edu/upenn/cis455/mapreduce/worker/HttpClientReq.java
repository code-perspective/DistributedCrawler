package edu.upenn.cis455.mapreduce.worker;

import java.io.IOException;

import java.io.PrintWriter;
import java.net.Socket;

public class HttpClientReq {

	public void sendRequest(String hostName, String req_url, int portNumber,
			String method, String body) {
//		System.out.println("sending request to: "+portNumber+"--------------------------------------------------");
		Socket socket = null;
		try {
			socket = new Socket(hostName, portNumber);
			sendRequestHeader(socket, req_url, hostName, method, body);

		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendGETRequest(String hostName, String req_url, int portNumber,
			String method) {
		sendRequest(hostName, req_url, portNumber, method, null);
	}

	/**
	 * program to send HTTP GET request to a server Socket: socket indicating
	 * the server req_url: url to be used in GET method host: String host to be
	 * used in host header of the HTTP GET header
	 */
	private void sendRequestHeader(Socket socket, String req_url, String host,
			String method, String body) {

		PrintWriter pr=null;
		try {
			pr = new PrintWriter(socket.getOutputStream());

			String request = "";
			request = request + method + " " + req_url + " HTTP/1.1\r\n";
			request = request + "Host:" + host + "\r\n";
			request = request + "User-Agent: cis455crawler\r\n";
			if (method.equals("POST")) {
				request = request + "Content-Length: " + body.length() + "\r\n";
				request = request + "Content-Type: " + "text/plain \r\n";
			}

			request = request + "Connection:close \r\n\r\n";

			request = request + body;

//			System.out.println(request);
			pr.write(request);

			pr.flush();
			
		} catch (IOException e) {
			 e.printStackTrace();
		}finally{
			pr.close();
		}

	}

}
