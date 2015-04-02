package org.gfbio.terminologyServer.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.gfbio.terminologyServer.tools.util.SettingsLoader;

import au.com.bytecode.opencsv.CSVReader;

public class HierarchyGenerator {
	public static final String defaultSettingsFileLocation = "HierarchyGenerator.settings";
	public static final String defaultSeparatorCharacter = ",";
	public static final String defaultQuoteCharacter = "\"";
	public static final String defaultFilldown = "true";
	public static final String defaultOutputFileLocation = "output.xml";
	public static final String defaultIdType = "nameBased";
	public static final String defaultNumericStartId = "1";
	
	public static final String inputFileKey = "inputFile";
	public static final String separatorCharacterKey = "separatorCharacter";
	public static final String quoteCharacterKey = "quoteCharacter";
	public static final String filldownKey = "filldown";
	public static final String outputFileKey = "outputFile";
	public static final String hierarchyColumnsKey = "hierarchyColumns";
	public static final String propertyColumnsKey = "propertyColumns";
	public static final String idTypeKey = "idType";
	public static final String numericStartIdKey = "numericStartId";
	public static final String outputHeaderKey = "header";
	public static final String outputFooterKey = "footer";
	public static final String headerFileKey = "headerFile";
	public static final String footerFileKey = "footerFile";
	public static final String firstRowAsColumnNamesKey = "firstRowAsColumnNames";
	public static final String showBroaderReferenceKey = "showBroaderReference";
	public static final String showNarrowerReferenceKey = "showNarrowerReference";
	public static final String elementTemplateKey = "ElementTemplate";
	public static final String broaderReferenceTemplateKey = "BroaderReferenceTemplate";
	public static final String narrowerReferenceTemplateKey = "NarrowerReferenceTemplate";
	public static final String propertyTemplateKey = "PropertyTemplate";
	
	public static final String defaultPrefix = "default";
	public static final String columnPrefix = "column";
	
	public static final String rootId = "root";
	
	public String[] columnNames = null;
	public int nodeCount = 0;
	public int[] hierarchyColumns;
	public int[] propertyColumns;
	public Map<String, String> settingsMap;
	
	protected int numericIdCounter;
	
	public HierarchyGenerator(String settingsFileLocation) {
		
		settingsMap = SettingsLoader.loadSettings(settingsFileLocation,defaultSettingsFileLocation);
		
		if(!settingsMap.containsKey(inputFileKey)){
			System.out.println("Settings file does not contain the parameter '"+inputFileKey+"'. Stopping program now ...");
			System.exit(1);
		}
				
		File inputFile = new File(getSetting(inputFileKey));
		
		if(!inputFile.exists()){
			System.out.println("Input file '"+inputFile.getName()+"' does not exist. Stopping program now ...");
			System.exit(1);
		}
		
		char separatorCharacter = getSetting(separatorCharacterKey).charAt(0);
		char quoteCharacter = getSetting(quoteCharacterKey).charAt(0);
		
		//get columns to transform
		if(!settingsMap.containsKey(hierarchyColumnsKey)){
			System.out.println("Settings file does not contain the parameter '"+hierarchyColumnsKey+"'. Stopping program now ...");
			System.exit(1);
		}
		String hierarachyColumnsText = settingsMap.get(hierarchyColumnsKey);
		// check if value is only positive integers, separated by comma; 0 or values with a leading 0 are not allowed 
		if(!hierarachyColumnsText.matches("^([1-9]\\d*)(,[1-9]\\d*)*$")){
			System.out.println("Settings parameter '"+hierarchyColumnsKey+"' must be only comma separated integers larger than 0. Instead it is '"+hierarachyColumnsText+"'Stopping program now ...");
			System.exit(1);
		}
		
		//split the hierarachyColumns into single values and convert them to integers
		String[] separatedHierarchyColumnsText = hierarachyColumnsText.split(",");
		hierarchyColumns = new int[separatedHierarchyColumnsText.length];
		for(int hierarchyColumnIndex = 0; hierarchyColumnIndex < separatedHierarchyColumnsText.length; hierarchyColumnIndex++){
			hierarchyColumns[hierarchyColumnIndex] = Integer.parseInt(separatedHierarchyColumnsText[hierarchyColumnIndex]);
		}
		
		String propertyColumnsText = getSetting(propertyColumnsKey);
		propertyColumns = new int[0];
		// check if value is only positive integers, separated by comma; 0 or values with a leading 0 are not allowed 
		if(!propertyColumnsText.matches("^([1-9]\\d*)(,[1-9]\\d*)*$") && propertyColumnsText.length()>0){
			System.out.println("Settings parameter '"+propertyColumnsKey+"' must be only comma separated integers larger than 0. Instead it is '"+propertyColumnsText+"'. Property columns are ignored.");
			propertyColumnsText = "";
		}else if(propertyColumnsText.length()>0){
			//split the propertyColumns into single values and convert them to integers
			String[] separatedPropertyColumnsText = propertyColumnsText.split(",");
			propertyColumns = new int[separatedPropertyColumnsText.length];
			for(int propertyColumnIndex = 0; propertyColumnIndex < separatedPropertyColumnsText.length; propertyColumnIndex++){
				propertyColumns[propertyColumnIndex] = Integer.parseInt(separatedPropertyColumnsText[propertyColumnIndex]);
			}
		}
		
		//check for defaultElementTemplate
		String defaultElementTemplate = getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.elementTemplateKey);
		if(defaultElementTemplate.equals("")){
			System.out.println("Warning: There is no "+HierarchyGenerator.defaultPrefix+HierarchyGenerator.elementTemplateKey+" defined.");
		}
		
		//check for defaultPropertyTemplate, 
		String defaultPropertyTemplate = getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.propertyTemplateKey);
		if(defaultPropertyTemplate.equals("")){
			System.out.println("Warning: There is no "+HierarchyGenerator.defaultPrefix+HierarchyGenerator.propertyTemplateKey+" defined.");
		}
		
		String showBroaderReference = getSetting(HierarchyGenerator.showBroaderReferenceKey);
		String showNarrowerReference = getSetting(HierarchyGenerator.showNarrowerReferenceKey);
		//check for defaultBroaderReferenceTemplate
		if(showBroaderReference.equals("true")|showBroaderReference.equals("1")){
			String broaderReferenceTemplate = getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.broaderReferenceTemplateKey);
			if(broaderReferenceTemplate.equals("")){
				System.out.println("Warning: There is no "+HierarchyGenerator.defaultPrefix+HierarchyGenerator.broaderReferenceTemplateKey+" defined, even though "+HierarchyGenerator.showBroaderReferenceKey+" is set to true.");
			}
		}
		//check for defaultNarrowerReferenceTemplate
		if(showNarrowerReference.equals("true")|showNarrowerReference.equals("1")){
			String narrowerReferenceTemplate = getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.narrowerReferenceTemplateKey);
			if(narrowerReferenceTemplate.equals("")){
				System.out.println("Warning: There is no "+HierarchyGenerator.defaultPrefix+HierarchyGenerator.narrowerReferenceTemplateKey+" defined, even though "+HierarchyGenerator.showNarrowerReferenceKey+" is set to true.");
			}
		}
		
		String filldown = getSetting(filldownKey);
		
		String idType = getSetting(idTypeKey);
		if(idType.equals("numeric")){
			String numericStartIdString = getSetting(numericStartIdKey);

			if(numericStartIdString.matches("^\\d+$")){
				numericIdCounter = Integer.parseInt(numericStartIdString);
			}else{
				numericIdCounter = 0;
				System.out.println("Settings parameter '"+numericStartIdKey+"' must be a positive integer. Instead it is '"+numericStartIdString+"'. It was now set to 0.");
				
			}
			
		}
		
		HierarchyNode rootNode = null;
		try {
			CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"),separatorCharacter,quoteCharacter);
			LinkedList<String[]> lines = new LinkedList<String[]>();
			int lineNumber = 0;
			//read file line by line
			String[] line = csvReader.readNext();
			while(line != null){
				//line number for error output: human readable counting, i.e. first line number is #1
				lineNumber ++;
				//line number is first column
				lines.add(ArrayUtils.addAll(new String[]{Integer.toString(lineNumber)},line));
				line = csvReader.readNext();
			}
			csvReader.close();
			
			//determine number of lines and number of columns
			int lineCount = lines.size();
			int maxColumnCount = 0;
			for(int lineIndex = 0; lineIndex < lineCount; lineIndex++){
				if(lines.get(lineIndex).length>maxColumnCount){
					maxColumnCount = lines.get(lineIndex).length;
				}
			}
			
			//save first row as column names, if requested
			String firstRowAsNames = settingsMap.get(firstRowAsColumnNamesKey);
			if(firstRowAsNames != null && (firstRowAsNames.equals("true")||firstRowAsNames.equals("1"))){
				columnNames = lines.get(0);
				lines.remove(0);
				lineCount--;
			}
			
			//fill down of missing values
			if(filldown.equals("true")||filldown.equals("1")){
				for(int fillDownRowIndex = 1; fillDownRowIndex < lineCount; fillDownRowIndex++){
					int hierarchyColumnIndex = 0;
					String[] previousLine = lines.get(fillDownRowIndex-1);
					String[] currentLine = lines.get(fillDownRowIndex);
					while(currentLine[hierarchyColumns[hierarchyColumnIndex]].length()==0){
						if(previousLine[hierarchyColumns[hierarchyColumnIndex]].length()>0){
							currentLine[hierarchyColumns[hierarchyColumnIndex]] = previousLine[hierarchyColumns[hierarchyColumnIndex]];
						}else{
							//warning: previous line is empty
							String columnName = "";
							if(columnNames != null && columnNames.length > hierarchyColumns[hierarchyColumnIndex]){
								columnName = " ("+columnNames[hierarchyColumns[hierarchyColumnIndex]]+")";
							}
							System.out.println("Error in line "+(currentLine[0])+": could not fill down property in column "+hierarchyColumns[hierarchyColumnIndex]+columnName);
						}
						hierarchyColumnIndex++;
					}
				}
			}
			
			rootNode = new HierarchyNode(rootId, rootId, 0, this);
			rootNode.setLines(lines);
			for(int columnIndex = -1; columnIndex < hierarchyColumns.length-1; columnIndex++){
				rootNode.processChildNodes(hierarchyColumns, columnIndex);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			String outputFilePath = getSetting(outputFileKey);
			File outputFile = new File(outputFilePath);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8"));
			
			//write the header from file
			String headerFileLink = getSetting(headerFileKey);
			if(headerFileLink.length()>0){
				File headerFile = new File(headerFileLink);
				if(headerFile.exists()){
					if(headerFile.isFile()){
						BufferedReader headerReader = new BufferedReader(new InputStreamReader(new FileInputStream(headerFile), "UTF8"));
						String headerLine = headerReader.readLine();
						while(headerLine != null){
							out.write(headerLine+"\n");
							headerLine = headerReader.readLine();
						}
						headerReader.close();
					}else{
						System.out.println("Header file: '"+headerFileLink+"' is not a file.");
					}
				}else{
					System.out.println("Header file: '"+headerFileLink+"' does not exist.");
				}
			}
			out.flush();
			
			//print the children of the root node first
			if(rootNode != null){
				rootNode.printChildNodes(out,0);
				//print according to the hierarchy
				for(int columnIndexToPrint:hierarchyColumns){
					rootNode.printChildNodes(out,columnIndexToPrint);
				}
			}
			
			//write the footer from file
			String footerFileLink = getSetting(footerFileKey);
			if(footerFileLink.length()>0){
				File footerFile = new File(footerFileLink);
				if(footerFile.exists()){
					if(footerFile.isFile()){
						BufferedReader footerReader = new BufferedReader(new InputStreamReader(new FileInputStream(footerFile), "UTF8"));
						String footerLine = footerReader.readLine();
						while(footerLine != null){
							out.write(footerLine+"\n");
							footerLine = footerReader.readLine();
						}
						footerReader.close();
					}else{
						System.out.println("Footer file: '"+footerFileLink+"' is not a file.");
					}
				}else{
					System.out.println("Footer file: '"+footerFileLink+"' does not exist.");
				}
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public String getSetting(String settingsKey){
		if(settingsKey == null){
			return "";
		}
		String settingsValue = settingsMap.get(settingsKey);
		if(settingsValue == null || settingsValue.length() == 0){
			if(settingsKey.equals(outputFileKey)){
				return defaultOutputFileLocation;
			}else if(settingsKey.equals(separatorCharacterKey)){
				return defaultSeparatorCharacter;
			}else if(settingsKey.equals(quoteCharacterKey)){
				return defaultQuoteCharacter;
			}else if(settingsKey.equals(filldownKey)){
				return defaultFilldown;
			}else if(settingsKey.equals(idTypeKey)){
				return defaultIdType;
			}else if(settingsKey.equals(numericStartIdKey)){
				return defaultNumericStartId;
			}
			
			return "";
		}
		return settingsValue;
	}

	public static void main(String[] args) {
		String settingsFileLocation = null;
		if(args.length>0){
			settingsFileLocation = args[0];
		}

		new HierarchyGenerator(settingsFileLocation);

	}

	protected TreeSet<String> idSet = new TreeSet<String>();
	public String generateNewID(String conceptName, HierarchyNode parent) {
		String idType = getSetting(idTypeKey);
		if(idType.equals("numeric")){
			String returnId = Integer.toString(numericIdCounter);
			numericIdCounter++;
			return returnId;
		}else if(idType.equals("ordered_numeric")){
			String parentNodeName = "";
			if(parent.getColumnIndex() != 0){
				parentNodeName = parent.id+".";
			}
			int siblingCount = parent.getChildNodes().size()+1;
			return parentNodeName+siblingCount;
		}else{
			if(idSet.contains(conceptName)){
				int count=2;
				String replacementId = conceptName+"_"+count;
				while(idSet.contains(replacementId)){
					count++;
					replacementId = conceptName+"_"+count;
				}
				idSet.add(replacementId);
				return replacementId;
			}else{
				idSet.add(conceptName);
				return conceptName;
			}
		}
	}

}
