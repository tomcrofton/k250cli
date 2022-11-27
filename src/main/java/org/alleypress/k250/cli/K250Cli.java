package org.alleypress.k250.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class K250Cli {

	private Options options = new Options();

	public K250Cli() {
		options.addOption("h", "help", false, "Print this help screen.");
		//.addOption("g", "gui", false, "Show GUI Application")
		//.addOption("n", true, "No. of copies to print");
	}
	
	public void process(String[] args) {
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
	}
	
	public void showHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("K250Cli", options);				
	}
	
	public static void main(String[] args) {
		K250Cli cli = new K250Cli();
		cli.process(args);
	}

}
