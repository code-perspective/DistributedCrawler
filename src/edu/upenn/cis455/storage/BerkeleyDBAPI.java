package edu.upenn.cis455.storage;

import java.io.File;

import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;


public class BerkeleyDBAPI {

	/**
	 * @param args
	 */
	
	public static String environmentPath=null;
	
//	public BerkeleyDBAPI(String folder) {
//		environmentPath=folder;
//	}
	
	

	

	public Environment getEnvironment() {

		Environment dbEnv = null;
		try {
			// create a configuration for DB environment
			EnvironmentConfig envConf = new EnvironmentConfig();

			// environment will be created if not exists
			envConf.setAllowCreate(true);

			// open/create the DB environment using config
			dbEnv = new Environment(new File(environmentPath), envConf);

		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
			return dbEnv;
		}

		return dbEnv;

	}


	public Database getDatabaseHandle(Environment dbEnv, String databaseName) {
		// create a configuration for DB
		DatabaseConfig dbConf = new DatabaseConfig();

		// db will be created if not exits
		dbConf.setAllowCreate(true);

		// create/open testDB using config
		Database testDB = dbEnv.openDatabase(null, databaseName, dbConf);

		return testDB;
	}
	
	
	

	public void put(Database testDB, String key, String value) {
		// key
		DatabaseEntry dbkey = new DatabaseEntry();
		// data
		DatabaseEntry dbdata = new DatabaseEntry();

		StringBinding.stringToEntry(key, dbkey);

		StringBinding.stringToEntry(value, dbdata);

		// insert key/value pair to database
		testDB.put(null, dbkey, dbdata);
	}

	public String get(Database db_handle, String key) {
		DatabaseEntry dbkey = new DatabaseEntry();
		DatabaseEntry dbdata = new DatabaseEntry();

		String data = null;

		StringBinding.stringToEntry(key, dbkey);

		try {
			// read from database
			if ((db_handle.get(null, dbkey, dbdata, null) == OperationStatus.SUCCESS)) {

				data = StringBinding.entryToString(dbdata);

			}
		} catch (Exception e) {
			e.printStackTrace();
			return data;
		}

		return data;
	}
	
	
	

	public boolean delete(Database db_handle, String key) {
		DatabaseEntry dbkey = new DatabaseEntry();
		StringBinding.stringToEntry(key, dbkey);

		try {
			// delete from database
			if ((db_handle.delete(null, dbkey) == OperationStatus.SUCCESS))
				return true;
			else
				return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public void printDatabaseKeyValue(Database db_handle){
		Cursor cursor = db_handle.openCursor(null, null);
		DatabaseEntry dbkey = new DatabaseEntry();
		DatabaseEntry dbdata = new DatabaseEntry();
		int count = 0;
		
		while(cursor.getNext(dbkey, dbdata, null)!=OperationStatus.NOTFOUND){
			count++;
			System.out.println(StringBinding.entryToString(dbkey)+":"+StringBinding.entryToString(dbdata));
//			delete(db_handle, StringBinding.entryToString(dbkey));
		}
		System.out.println("--------------"+count);
		cursor.close();
		
	}
	
	
	
	public static void main(String[] args) {

		BerkeleyDBAPI.environmentPath ="/home/ubuntu/BerkeleyDB1";
		BerkeleyDBAPI db = new BerkeleyDBAPI();
		
		try {

			Environment dbEnv = db.getEnvironment();
			Database testDB = db.getDatabaseHandle(dbEnv, DBFunc.URLHash_URL);
			
			db.printDatabaseKeyValue(testDB);
			
			System.out.println("--------------------------------");
			
			
						
			System.out.println("Done");
			testDB.close();
			dbEnv.close();

		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
	}

}

