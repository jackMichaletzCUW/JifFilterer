
public class JifFilterer {

	public static void main(String[] args) {
		String inputDirectory = args[0];
		String outputDirectory = args[1];
		String settingsPath = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		
		Settings.outputPath = outputDirectory;
		Settings.loadSettings(settingsPath);
				
		//JIF j = new JIF(inputDirectory + "/singeranimegif15.gif");
		//JIF j = new JIF("/Users/jack/Desktop/animbars.gif");

		//Filter.interlace(j, 2, 0.9);
		//Filter.curve(j, 6, 7, 400, (d) -> {});
		//j.writeOut(outputDirectory + "/test.gif");
		Commander commander = new Commander(inputDirectory, numberOfThreads);
		commander.convert();
		
		System.out.println();
	}
	
}
