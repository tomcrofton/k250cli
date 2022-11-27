package org.alleypress.k250.serial;

public interface SerialAdapter {
	public String[] getPortNames();
	public void selectPort(String portName) throws SerialException;
	public String getAdapterInfo() throws SerialException;

	public String getConfig() throws SerialException;
	public void sendReset() throws SerialException;
}
