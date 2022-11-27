package org.alleypress.k250.cli;

import org.alleypress.k250.serial.JSSerialAdapter;
import org.alleypress.k250.serial.SerialAdapter;
import org.alleypress.k250.serial.SerialException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class K250Cli {

	private Options options = new Options();

	public K250Cli() {
		options.addOption("h", "help", false, "Print this help screen.")
		.addOption("l", "list", false, "List serial ports")
		.addOption("p", "port", true, "Serial port to use")
		.addOption("i","info", false,"Show connected interface adapter info");
	}
	
	public void process(String[] args) throws SerialException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try { 
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			return;
		}
		if (cmd.hasOption('h') || (cmd.getOptions().length==0)) {
			showHelp();
			return;
		}
		SerialAdapter sa = new JSSerialAdapter();
		if (cmd.hasOption('l')) {
			String[] names = sa.getPortNames();
			if (names.length==0) {
				System.out.println("No ports found");
			} else {
				for (String s:names) {
					System.out.println(s);
				}
			}
			return;
		}
		
		if (cmd.hasOption('p')) {
			sa.selectPort(cmd.getOptionValue('p'));
		} else {
			String[] names = sa.getPortNames();
			if (names.length==0) {
				System.out.println("No ports found");
				return;
			}
			if (names.length>1) {
				System.out.println("Need to specify serial port");
				return;
			}
			sa.selectPort(names[0]);
		}
		
		if (cmd.hasOption('i')) {
			System.out.println(sa.getAdapterInfo());
			return;
		}		
	}
	
	public void showHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("K250Cli", options);				
	}
	
	public static void main(String[] args) {
		K250Cli cli = new K250Cli();
		try {
			cli.process(args);
		} catch (SerialException e) {
			System.err.println(e.getMessage());
		}
	}

}
