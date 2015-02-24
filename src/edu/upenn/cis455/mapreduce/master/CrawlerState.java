package edu.upenn.cis455.mapreduce.master;

public class CrawlerState {
	public String ip;
	public String port;
	public String status;
	public int valid_count;
	public int total_url_count;
	public int file_count;
	long LastUpdatedTime = -1;

	public CrawlerState(String ip, String port, String status,int valid_url,int file_count,int total_url_count) {
		this.ip = ip;
		this.port = port;
		this.status = status;
		this.valid_count=valid_url;
		this.total_url_count=total_url_count;
		this.file_count=file_count;
		LastUpdatedTime = System.currentTimeMillis();

	}

}
