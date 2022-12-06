package org.alleypress.k250.serial;

public interface SerialAdapter {
	public String[] getPortNames();
	public void selectPort(String portName) throws SerialException;
	public String getAdapterInfo() throws SerialException;

	public void echoTest(int packetSize) throws SerialException;
	
	public String getConfig() throws SerialException;
	public String loopTest(int packets, int size) throws SerialException;
	public void sendReset() throws SerialException;
	
	public void setProgressWatcher(ProgressWatcher watcher);
		
}
