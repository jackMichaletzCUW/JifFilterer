import java.io.File;
import java.io.IOException;

public class FilterProcess {

	File[] fileList;
	Runnable updateFunction;
	String status = "-----------WAITING ON COMMAND-----------";
	                
	String previousStatus = "";
	public double totalProgress = 0.0;
	public double localProgress = 0.0;
	double previousProgress = 0.0;
	String[] filterFunctionList;
	int filterComplexity = 0;
	
	public FilterProcess(File[] fileList, Runnable updateFunction) {
		 this.fileList = fileList;
		 this.updateFunction = updateFunction;
	}
	
	private String generateStatusString(int index) {
		String full = fileList[index].getName();
		
		if(full.length() < 10) {
			for(int i = 0; i < 13 - full.length(); i++) {
				full += "-";
			}
		} else if(full.length() > 10) {
			full = full.substring(0, 6) + ".." + full.substring(full.length() - 6, full.length() - 4);
		}
		
		return full;
	}
	
	private String generateProgressBar()
	{	
		int decile = (int)(20 * localProgress);
		
		String bar = "";
		
		for(int i = 0; i < 20; i++)
		{
			if(i < decile) {
				bar += String.format("%c", (char )(9601 + (i / 3)));
			} else {
				bar += " ";
			}
		}
		
		return bar;
	}
	
	private void updateStatus(int index) {
		status = String.format("%03d/%03d:%s %s ", index, fileList.length, generateStatusString(index), generateProgressBar());
	
		if(!status.equals(previousStatus)) {
			updateFunction.run();
		}
	}

	private void processImage(int index) {
		try {
			//localProgress = 0.0;
			//updateStatus(index);
			JIF j = new JIF(fileList[index].getCanonicalPath());
			localProgress = 0.1;
			updateStatus(index);
						
			for(String filterFunction : filterFunctionList) {
				previousProgress = localProgress;
				int functionValue = Filter.getFilterValue(filterFunction);
				
				switch(filterFunction) {
					case "resize":
						Filter.resize(j,
								Settings.getInteger("resize-width"),
								Settings.getInteger("resize-height"),
								(progress) -> {
									localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75 * progress);
									updateStatus(index);
								});
						break;
					case "curve":
						Filter.curve(j,
								Settings.getDouble("curve-horizontal-coefficient"),
								Settings.getDouble("curve-vertical-coefficient"),
								Settings.getInteger("curve-bulge"),
								(progress) -> {
									localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75 * progress);
									updateStatus(index);
								});
					case "crop":
						Filter.crop(j, 
								(progress) -> {
									localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75 * progress);
									updateStatus(index);
								});
					case "wash-out":
						Filter.washOut(j, Settings.getDouble("wash-out-factor-red"),
								Settings.getDouble("wash-out-factor-green"),
								Settings.getDouble("wash-out-factor-blue"));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
					case "lighten":
						Filter.lighten(j, Settings.getDouble("lighten-factor"));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
					case "normalize":
						Filter.normalize(j, Settings.getDouble("normalize-factor"), Settings.getDouble("normalize-threshold"));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
					case "color-shift":
						Filter.colorShift(j, Settings.getDouble("color-shift-factor-red"),
								Settings.getDouble("color-shift-factor-green"),
								Settings.getDouble("color-shift-factor-blue"),
								(progress) -> {
									localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75 * progress);
									updateStatus(index);
								});
						break;
					case "color-bleed":
						Filter.colorBleed(j, Settings.getDouble("color-bleed-factor-red"),
								Settings.getDouble("color-bleed-factor-green"),
								Settings.getDouble("color-bleed-factor-blue"),
								Settings.getDouble("color-bleed-blur-factor"),
								(progress) -> {
									localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75 * progress);
									updateStatus(index);
								});
						break;	
					case "interlace":
						Filter.interlace(j, Settings.getInteger("interlace-line-width"),
								Settings.getDouble("interlace-factor"));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
					case "add-scan-lines":
						Filter.addScanLines(j, Integer.parseInt(Settings.get("scan-line-color"), 16));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
					case "split":
						Filter.addSplit(j, Settings.getInteger("split-threshold"), Settings.getDouble("split-intensity"));
						localProgress = previousProgress + (((double)functionValue / (double)filterComplexity) * 0.75);
						updateStatus(index);
						break;
				}
			}
			
			j.writeOut(Settings.outputPath + "/c_" + fileList[index].getName());
			localProgress = 1.0;
			updateStatus(index);
			
		} catch (Exception e) {
			e.printStackTrace();
			Settings.clearScreen();
			updateFunction.run();
		}
	}
	
	public void process() {
		Runnable task = () -> {
			filterFunctionList = Settings.getFilterFunctionList();
			for(String filterFunction : filterFunctionList) {
				filterComplexity += Filter.getFilterValue(filterFunction);
			}
			
			for (int i = 0; i < fileList.length; i++) {
				processImage(i);
				totalProgress = (double)i / (double)fileList.length;
			}

			status = " ------FINISHED CONVERSION PROCESS----- ";
			updateFunction.run();
		};
		
		Thread thread = new Thread(task);
		thread.start();
	}
}
