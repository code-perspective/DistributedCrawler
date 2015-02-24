package edu.upenn.cis455.storage;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;


public class DBFunc {

	public static Logger log = Logger.getLogger(DBFunc.class);
	static String URLHash_URL = "URLHash_URL";
	static String URLHash_LinkText = "URLHash_LinkText";
	static String URLHash_Date_DataType = "URLHash_Date_DataType";
	public static String URLhash_ListOfLinkHashes = "URLhash_ListOfLinkHashes";

	static String DomainHash_Domain = "DomainHash_Domain";
	static String DomainHash_Robots = "DomainHash_Robot";

	static String checkSum_URL = "Checksum_num_HashURL";
	static String URLHash_Checksum = "URLHash_Checksum";


	

	

	public synchronized void putURL_Date_DataType(String url, long date,
			String datatype) {
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_Date_DataType);
		try{
		db.put(db_handle, url, Long.toString(date) + "#" + datatype);
		}catch(Exception e){
			log.error(e.getLocalizedMessage(), e);
		}
		db_handle.close();
		env.close();
	}

	public synchronized String[] getDate_DataType_from_URL(String url) {

		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_Date_DataType);

		String[] result=null;
		try {
			String value = db.get(db_handle, url);
			
			if (value == null)
				result = null;
			else {
				result = value.split("#");
			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}

		db_handle.close();
		env.close();

		return result;
	}

	public synchronized void putURL_URLHash(String hash, String url) {
		System.out.println("\tAdding Hash for Url : " + url);
		log.info("\tAdding Hash for Url : " + url);
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_URL);
		db.put(db_handle, hash, url);
		db_handle.close();
		env.close();
	}

	public synchronized String getURL_URLHash(String url) {

		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_URL);
		String body = db.get(db_handle, url);
		db_handle.close();
		env.close();
		return body;
	}
	
	
	public synchronized void putURLHash_Checksum(String hash, String checksum) {
		log.info("Saving url and checksum for "+hash);
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_Checksum);
		db.put(db_handle, hash, checksum);
		db_handle.close();
		env.close();
	}

	public synchronized String getURLHash_Checksum(String hash) {

		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_Checksum);
		String body = db.get(db_handle, hash);
		db_handle.close();
		env.close();
		return body;
	}

	public synchronized void putURL_Links(String url, ArrayList<String> links) {
		System.out.println("\tAdding URLs and links to DB : " + url);
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db
				.getDatabaseHandle(env, URLhash_ListOfLinkHashes);

		try {
			String content = "";
			for (String link : links)
				content = content + link + "|";
			db.put(db_handle, url, content.substring(0, content.length() - 1));
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}
		db_handle.close();
		env.close();
	}

	public synchronized String[] getURL_Links(String url) {

		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db
				.getDatabaseHandle(env, URLhash_ListOfLinkHashes);

		String[] result = null;
		try {
			String value = db.get(db_handle, url);

			if (value == null)
				result = null;
			else
				result = value.split("\\|");
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}

		db_handle.close();
		env.close();

		return result;
	}

	public synchronized void putDomain_DomainHash(String url, String hash) {
		System.out.println("\tAdding Hash for Url : " + url);
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, DomainHash_Domain);
		db.put(db_handle, url, hash);
		db_handle.close();
		env.close();
	}

	public synchronized String getDomain_DomainHash(String url) {
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();

		Database db_handle = db.getDatabaseHandle(env, DomainHash_Domain);
		String body = null;
		try {
			body = db.get(db_handle, url);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}
		db_handle.close();
		env.close();
		return body;
	}

//	public synchronized void putDomain_Robots(String url, String[] links) {
//		System.out.println("\tAdding robots to DB : " + url);
//		BerkeleyDBAPI db = new BerkeleyDBAPI();
//		Environment env = db.getEnvironment();
//		Database db_handle = db.getDatabaseHandle(env, DomainHash_Robots);
//		try {
//			String content = "";
//			for (String link : links)
//				content = content + link + "|";
//			db.put(db_handle, url, content.substring(0, content.length() - 1));
//		} catch (Exception e) {
//
//		}
//
//		db_handle.close();
//		env.close();
//	}
//
//	public synchronized String[] getDomain_Robots(String url) {
//
//		BerkeleyDBAPI db = new BerkeleyDBAPI();
//		Environment env = db.getEnvironment();
//		Database db_handle = db.getDatabaseHandle(env, DomainHash_Robots);
//		String[] result = null;
//		try {
//			String value = db.get(db_handle, url);
//
//			if (value == null)
//				result = null;
//			else
//				result = value.split("\\|");
//		} catch (Exception e) {
//
//		}
//
//		db_handle.close();
//		env.close();
//
//		return result;
//	}

	public synchronized boolean putchecksum(String checksum, String url_hash) {
		boolean result = false;
		System.out.println("\tPutting checksum for URL ");
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, checkSum_URL);

		try {
			int count = getchecksum_count(checksum,
					db.getDatabaseHandle(env, checkSum_URL), db);
			if (count != 0)
				result = true;
			count++;

			String content = Integer.toString(count) + "|" + url_hash;
			db.put(db_handle, checksum, content);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}
		db_handle.close();
		env.close();
		return result;
	}

	public synchronized int getchecksum_count(String checksum,
			Database db_handle, BerkeleyDBAPI db) {

		String value = db.get(db_handle, checksum);

		int result = 0;
		try {
			if (value == null)
				result = 0;
			else
				result = Integer.parseInt(value.split("\\|")[0]);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			result = 0;
		}

		db_handle.close();

		return result;
	}
	
	
	public synchronized void putUrlDesc(String link_desc, String url_hash) {

		
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		Environment env = db.getEnvironment();
		Database db_handle = db.getDatabaseHandle(env, URLHash_LinkText);

		try {
			String desc = getUrlDesc(url_hash,
					db.getDatabaseHandle(env, URLHash_LinkText), db);
			if(desc==null)
				desc=link_desc;
			else
				desc=desc+"\n"+link_desc;
			
			db.put(db_handle, url_hash, desc);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}
		db_handle.close();
		env.close();
	}

	public synchronized String getUrlDesc(String url_hash,
			Database db_handle, BerkeleyDBAPI db) {

		String value = db.get(db_handle, url_hash);

		String result = "";
		try {
			if (value == null)
				result = null;
			else
				result = value;
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			result = null;
		}

		db_handle.close();

		return result;
	}

}
