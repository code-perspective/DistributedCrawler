package edu.upenn.cis455.xpathengine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;




public class NamesInHash {

	public static String response_line = "ResponseLine";
	public static String requestMethod="RequestMethod";
	public static String requestUrl="RequestUrl";
	public static String requestBody="RequestBody";
	public static String httpVersion="HttpVersion";
	public static String responseCode="ResponseCode";
	public static String location="Location";
	public static String requestLine="RequestLine";
	public static String ifUnmodifiedSince="If-Unmodified-Since";
	public static String ifModifiedSince="If-Modified-Since";
	public static String contentType="Content-Type";
	public static String contentLength = "Content-Length";
	public static String contentLanguage="Content-language";
	public static String lastModified="Last-Modified";
	public static String jSessionId="JSESSIONID";
	public static String setCookie="Set-Cookie";
	public static String cookie="Cookie";
	public static String requestedSessionId= "RequestedSessionId";
	public static String characterEncoding="CharacterEncoding";
	public static String acceptLanguage="Accept-Language";
	public static String host="Host";
	public static String remotePort="RemotePort";
	public static String remoteAddress="RemoteAddress";
	public static String servletName ="ServletName";
	public static String servletPath ="ServletPath";
	public static String pathInfo ="PathInfo";
	
	
	public static String get_init_line(String response_code) {
		
		int resp=Integer.parseInt(response_code);
		switch (resp) {

		case 200:
			return "200 OK";
		case 400:
			return "400 Bad Request";
		case 403:
			return "403 Forbidden";
		case 404:
			return "404 Not Found";
		case 501:
			return "501 Not Implemented";
		case 505:
			return "505 Version Not Supported";
		case 412:
			return "412 Precondition Failed";
		case 304:
			return "304 Not Modified";
		case 500:
			return "Server Error";
		default:
			return "404 Not Found";
		}
	}
	
//	public static String content_type(String ext) {
//		if (ext == null)
//			return "text/html";
//
//		switch (ext) {
//
//		case "txt":
//			return "text/plain";
//		case "html":
//			return "text/html";
//		case "jsp":
//			return "text/html";
//		case "mp3":
//			return "audio/mpeg";
//		case "pdf":
//			return "application/pdf";
//		case "jpeg":
//			return "image/jpeg";
//		case "jpg":
//			return "image/jpeg";
//		default:
//			return "text/plain";
//		}
//	}
	
	
	
	
	public static String formatDate(Date df)
	{
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
				Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		return sdf.format(df);
	}
	
	
	
	public static Date convertStringToDate(String date_in_req)
	{
		Date dr=null;
		System.out.println(date_in_req);
		try {
			SimpleDateFormat ob = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss zzz");
			ob.setTimeZone(TimeZone.getTimeZone("GMT"));
			dr = ob.parse(date_in_req);

		} catch (ParseException e) {
			
		}

		try {
			SimpleDateFormat ob = new SimpleDateFormat(
					"EEEE, dd-MMM-yy HH:mm:ss zzz");
			ob.setTimeZone(TimeZone.getTimeZone("GMT"));
			dr = ob.parse(date_in_req);

		} catch (ParseException e) {
			
		}

		try {
			SimpleDateFormat ob = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss yyyy");
			ob.setTimeZone(TimeZone.getTimeZone("GMT"));
			dr = ob.parse(date_in_req);

		} catch (ParseException e) {
			
		}

		return dr;
	}
}
