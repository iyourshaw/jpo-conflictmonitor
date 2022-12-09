package us.dot.its.jpo.conflictmonitor.jmx;

import java.util.ArrayList;

public class BeanList extends ArrayList<BaseBean> {

	private String error;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	
}
