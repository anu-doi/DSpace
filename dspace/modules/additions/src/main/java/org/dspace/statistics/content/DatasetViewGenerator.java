package org.dspace.statistics.content;

/**
 * 
 * @author Genevieve Turner
 *
 */
public class DatasetViewGenerator extends DatasetTypeGenerator {
	public enum IpRange {
		ALL, EXTERNAL, INTERNAL
	}
	
	private boolean showFullView;
	private boolean showFileDownloads;
	private int orderColumn;
	private String handle;
	private IpRange ipRange = IpRange.ALL;
	private int filterType = -1;
	
	public boolean isShowFullView() {
		return showFullView;
	}

	public void setShowFullView(boolean showFullView, String handle) {
		if (handle == null || handle.length() == 0) {
			this.showFullView = false;
			return;
		}
		this.handle = handle;
		this.showFullView = showFullView;
	}

	public boolean isShowFileDownloads() {
		return showFileDownloads;
	}
	
	public void setShowFileDownloads(boolean showFileDownloads) {
		this.showFileDownloads = showFileDownloads;
	}

	public int getOrderColumn() {
		return orderColumn;
	}

	public void setOrderColumn(int orderColumn) {
		this.orderColumn = orderColumn;
	}
	
	public String getHandle() {
		return handle;
	}

	public IpRange getIpRange() {
		return ipRange;
	}

	public void setIpRange(IpRange ipRange) {
		this.ipRange = ipRange;
	}
	
	public void setIpRange(String range) {
		if ("external".equals(range)) {
			this.ipRange = IpRange.EXTERNAL;
		}
		else if ("internal".equals(range)) {
			this.ipRange = IpRange.INTERNAL;
		}
		else {
			this.ipRange = IpRange.ALL;
		}
	}

	public int getFilterType() {
		return filterType;
	}

	public void setFilterType(int filterType) {
		this.filterType = filterType;
	}
}
