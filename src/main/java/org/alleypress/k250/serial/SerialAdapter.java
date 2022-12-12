package org.alleypress.k250.serial;

import java.io.File;
import java.io.IOException;

public interface SerialAdapter {
	public String[] getPortNames();
	public void selectPort(String portName) throws SerialException;
	public String getAdapterInfo() throws SerialException;
	public void sendReset() throws SerialException;
	public void setProgressWatcher(ProgressWatcher watcher);
	public void echoTest(int packetSize) throws SerialException;
	public byte[] getConfig() throws SerialException;
	public String loopTest(int packets, int size) throws SerialException;
	public void saveDigitizer(File f) throws SerialException, IOException;
	public void loadDigitizer(File f) throws SerialException, IOException;
		
}
