package org.alleypress.k250.serial;

public class ConsoleProgressWatcher implements ProgressWatcher {

	@Override
	public void setProgress(double progress) {
		System.out.print(".");
	}

	@Override
	public void begin() {
		System.out.print(" [");
	}

	@Override
	public void end() {
		System.out.println("]");
	}

}
