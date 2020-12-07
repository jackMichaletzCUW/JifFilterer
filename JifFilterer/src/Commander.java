import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Commander {

	File[] filesToConvert;
	FilterProcess[] threads;
	Runnable progressFunction;
	boolean isFirstUpdate = true; 
	
	public Commander(String inputDirectory, int requestedNumberOfThreads) {
		File directory = new File(inputDirectory);
		filesToConvert = directory.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".gif");
		    }
		});
		
		threads = new FilterProcess[requestedNumberOfThreads];
	}
	
	private String generateProgressBar()
	{	
		double averageGlobalProgress = 0.0;
		for(int i = 0; i < threads.length; i++) {
			averageGlobalProgress += threads[i].totalProgress;
		}
		
		averageGlobalProgress /= threads.length;
		
		int decile = (int)(60 * averageGlobalProgress);
		
		String bar = "";
		
		for(int i = 0; i < 60; i++)
		{
			if(i < decile) {
				bar += String.format("%c", (char )(9601 + (i / 8)));
			} else {
				bar += " ";
			}
		}
		
		return String.format(" %02d%% COMPLETE.... %s ", (int)(averageGlobalProgress * 100), bar);
	}
	
	public void convert() {
		
		class Progress implements Runnable{
		    private final Object lock = new Object();

		    public void run(){
		        synchronized(lock) {
		        	for(int b = 0; b < (threads.length * 40) + 80; b++) {
						System.out.printf("\b");
					}
					
					for(int i = 0; i < threads.length; i++) {
						System.out.print(threads[i].status);
					}
					
					System.out.print(generateProgressBar());
		        }
		    }
		};
		
		Progress progressFunction = new Progress();
	
		/*progressFunction = () -> {
			for(int b = 0; b < (threads.length * 40); b++) {
				System.out.printf("\b");
			}
			
			for(int i = 0; i < threads.length; i++) {
				System.out.print(threads[i].status);
			}
			
			//System.out.print(generateProgressBar());
		};*/
		
		int filesPerThread = filesToConvert.length / threads.length;
		
		for(int i = 0; i < threads.length - 1; i++) {
			threads[i] = new FilterProcess(Arrays.copyOfRange(filesToConvert, i * filesPerThread, ((i+1) * filesPerThread)), progressFunction);
		}
		
		threads[threads.length - 1] = new FilterProcess(Arrays.copyOfRange(filesToConvert, (threads.length - 1) * filesPerThread, filesToConvert.length), progressFunction);
		
		/*for(int i = 0; i < threads.length; i++) {
			threads[i].process();
		}*/
		Settings.clearScreen();
		
		ExecutorService exec = Executors.newFixedThreadPool(threads.length);
		try {
		    for (FilterProcess p : threads) {
		        exec.submit(new Runnable() {
		            @Override
		            public void run() {
		                p.process();
		            }
		        });
		    }
		} finally {
		    exec.shutdown();
		}
		
		Settings.clearScreen();
		progressFunction.run();
	}
	
}
