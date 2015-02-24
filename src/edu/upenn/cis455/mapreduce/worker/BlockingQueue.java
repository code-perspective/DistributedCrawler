package edu.upenn.cis455.mapreduce.worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

class BlockingQueue {
	public static Logger log = Logger.getLogger(BlockingQueue.class);
	public static int readFileCount = 0;
	public static int writeCount = 0;
	public static int writeFileCount = 0;
	Queue<String> queue = null;
	int queueSize;
	public static boolean shudWriteToFile = false;
	ArrayList<String> backupQueue = new ArrayList<String>();
	int maxBackupQueueSize = 200;

	BlockingQueue(int queueSize) {
		queue = new LinkedList<String>();
		this.queueSize = queueSize;
	}

	// method to remove request from a queue
	public synchronized String dequeue() {
		String result = null;
		// thread waits until a request is put in the queue and the queue is
		// no longer empty
		
		log.info("###########################################################################################");
		
		log.info(readFileCount + "-----" + writeFileCount + "-------"
				+ shudWriteToFile + "-----" + queueSize + "----" + queue.size());
		log.info("###########################################################################################");
		
		
		
		System.out
				.println("###########################################################################################");
		System.out.println(readFileCount + "-----" + writeFileCount + "-------"
				+ shudWriteToFile + "-----" + queueSize + "----" + queue.size());
		System.out
				.println("###########################################################################################");
		
		
		
		if (shudWriteToFile) {
			
			while (queue.isEmpty()) {
				
				log.info("Queue Full-------------- Fetching from file, readcount="+readFileCount+" , writeFileCount="+writeFileCount);
				System.out.println("Queue Full-------------- Fetching from file");
				
				fetchUrlsFromDoc();
				
				log.info("incrementing readFileCount "+readFileCount+" while write file count is "+writeFileCount);
			}
		} else {
			while (queue.isEmpty()) {
				try {
					wait();
				} catch (InterruptedException e) {
					log.error(e.getLocalizedMessage(), e);
					return null;
				}
			}
		}
		// removes the first request from the queue
		result = queue.remove();
		
		// notifies the thread about the newly available request to handle
		notify();
		return result;
	}

	// method to push request into the queue
	public synchronized void enqueue(String input) {
		// thread waits till the queue has space available to take in more
		// requests
		
		// adds the request to queue
		
			if (queue.size() >= queueSize) {
				shudWriteToFile = true;
			}
			
			if (shudWriteToFile) {
				if (backupQueue.size() >= maxBackupQueueSize) {
					putUrlsIntoDoc();
					
				} else {
					backupQueue.add(input);
				}
			} else {
				queue.add(input);
			}
			// notifies the thread that request has been added to the queue
			notify();
		
	}

	public int size() {
		return queue.size();
	}

	void putUrlsIntoDoc() {
		
		BufferedWriter bw=null;
		ArrayList<String> secondBackupQueue = new ArrayList<String>();
		synchronized (backupQueue) {
			secondBackupQueue.addAll(backupQueue);
			backupQueue.clear();
		}
		try {
			if (writeCount == 5) {
				writeFileCount++;
				writeCount = 0;
			}
			
			File file = new File(WorkerServlet.storageDir
					+ "crawlerState/BackupForUrlFrontier" + writeFileCount
					+ ".txt");

			bw = new BufferedWriter(new FileWriter(file, true));
			for (int i = 0; i < secondBackupQueue.size(); i++)
				bw.write(secondBackupQueue.get(i) + "\n");
			
			
		} catch (IOException e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		}
		finally{
			
			try {
				bw.close();
			} catch (IOException e) {
				log.error(e.getLocalizedMessage(), e);
				e.printStackTrace();
			}
			secondBackupQueue.clear();
			writeCount++;
		}
	}

	void fetchUrlsFromDoc() {
		BufferedReader br = null;

		if (readFileCount <= writeFileCount) {

			File file = new File(WorkerServlet.storageDir
					+ "crawlerState/BackupForUrlFrontier" + readFileCount
					+ ".txt");
			try {

				br = new BufferedReader(new FileReader(file));

				String line;
				

				while ((line = br.readLine()) != null) {
					queue.add(line);
				}
				
				
			} catch (Exception e) {
				log.error(e.getLocalizedMessage(), e);
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage(), e);
					e.printStackTrace();
				}
				file.delete();
				readFileCount++;
			}

		}

	}
}