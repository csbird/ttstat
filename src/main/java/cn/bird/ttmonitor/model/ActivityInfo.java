package cn.bird.ttmonitor.model;

public class ActivityInfo {
	private int id;
	private String name;
	private String startTime;
	private String endTime;
	private int chengTuanCount;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public int getChengTuanCount() {
		return chengTuanCount;
	}
	public void setChengTuanCount(int chengTuanCount) {
		this.chengTuanCount = chengTuanCount;
	}
	
}
