package redempt.redlib.json;

interface JSONStorage {
	
	JSONStorage getParent();
	void setParent(JSONStorage obj);
	void add(String key, Object value);
	String getTempKey();
	void setTempKey(String value);
	
}
