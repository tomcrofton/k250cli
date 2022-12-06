package org.alleypress.k250.serial;

public interface ProgressWatcher {
	public void begin();
	public void setProgress(double progress);
	public void end();
}
