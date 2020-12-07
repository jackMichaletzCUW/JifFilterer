import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Settings {

	public static String outputPath;
	private static Map<String, String> settingDictionary;
	private static String[] filterFunctionList;
	
	public static void clearScreen() {
		for(int i = 0; i < 24; i++) {
			System.out.println();
		}
	}
	
	public static void loadSettings(String fileName) {
		settingDictionary = new HashMap<String, String>();
		
		ArrayList<String> settingList = new ArrayList<String>();
		
		try {
			settingList = new ArrayList<String>(Files.readAllLines(Paths.get(fileName)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(String setting : settingList) {
			String[] settingSplit = setting.split(":");
			
			if(settingSplit.length == 2) {
				settingDictionary.put(settingSplit[0], settingSplit[1].substring(1));
			}
		}
	}
	
	public static String get(String key) {
		return settingDictionary.get(key);
	}
	
	public static int getInteger(String key) {
		return Integer.parseInt(settingDictionary.get(key));
	}
	
	public static double getDouble(String key) {
		return Double.parseDouble(settingDictionary.get(key));
	}
	
	public static String[] getFilterFunctionList() {
		if(filterFunctionList == null) {
			filterFunctionList = get("filter-function-list").split(",");
		}
		
		return filterFunctionList;
	}
}
