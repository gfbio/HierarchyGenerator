package org.gfbio.terminologyServer.tools.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

public class SettingsLoader {
	public static Map<String, String> loadSettings(String settingsFileLocation, String defaultSettingsFileLocation) {
		File settingsFile = null;
		//check if provided settings file location is valid and settings file exists
		if(settingsFileLocation != null && !settingsFileLocation.equals("")){

			settingsFile = new File(settingsFileLocation);
			if(!settingsFile.exists()){
				System.out.println("Provided settings file '"+settingsFileLocation+"' does not exist. Trying to load default settings file ('"+defaultSettingsFileLocation+"') ...");
			}
		}else{
			System.out.println("No settings file provided. Trying to load default settings file ('"+defaultSettingsFileLocation+"') ...");
		}
		//load default settings file
		if(settingsFile == null || !settingsFile.exists()){
			settingsFile = new File(defaultSettingsFileLocation);if(!settingsFile.exists()){
				System.out.println("Default settings file '"+defaultSettingsFileLocation+"' does not exist. Stopping program now ...");
				System.exit(1);
			}
		}
		
		//load settings
		String nextLine;
		int lineCount = 0;
		String previousSettingsKey = null;
		Map<String,String> settingsMap = new TreeMap<String,String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(settingsFile), "UTF-8"));
			while ((nextLine = reader.readLine()) != null) {
				lineCount++;
				//skip commented lines and empty lines
				if(nextLine.startsWith("#") || nextLine.equals("")){
					continue;
				}
				int tabPosition = nextLine.indexOf("\t");
				if(tabPosition==-1){
					//there is no tab in the current line
					System.out.println("Could not load setting in line "+lineCount+" in settings file '"+settingsFile.getName()+":\n\t The line doesn't contain a tab character: '"+nextLine+"'. The line is skipped.");
				}else if(tabPosition == 0){
					//the line starts with a tab
					//this line is another line for the previous setting
					if(previousSettingsKey!=null){
						String settingsValue =  nextLine.substring(tabPosition+1);
						String concattedSettingsValue = settingsMap.get(previousSettingsKey)+"\n"+settingsValue;
						settingsMap.put(previousSettingsKey, concattedSettingsValue);
					}else{
						System.out.println("Could not load setting in line "+lineCount+" in settings file '"+settingsFile.getName()+":\n\t The line starts with a tab character, but there is no previous setting to which this value can be added: '"+nextLine+"'. The line is skipped.");
						
					}
				}else{
					//a new setting: extract key and value and save it in the settings map
					String settingsKey = nextLine.substring(0, tabPosition);
					String settingsValue =  nextLine.substring(tabPosition+1);
					settingsMap.put(settingsKey, settingsValue);
					previousSettingsKey = settingsKey;
				}
			}
			reader.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return settingsMap;
	}
}
