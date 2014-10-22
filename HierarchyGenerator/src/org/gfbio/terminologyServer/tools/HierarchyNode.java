package org.gfbio.terminologyServer.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class HierarchyNode {
	protected String name;
	protected String id;
	protected int columnIndex;
	protected int internalId;
	protected LinkedList<String[]> lines;
	protected Map<String,HierarchyNode> childNodes;
	protected List<String> orderedChildNodes;
	protected final HierarchyGenerator hierarchyGenerator;
	protected boolean wasProcessed;
	
	public HierarchyNode(String name, String id, int columnIndex, HierarchyGenerator hierarchyGenerator) {
		this.name = name;
		this.id = id;
		this.columnIndex = columnIndex;
		lines = null;
		childNodes = null;
		this.hierarchyGenerator = hierarchyGenerator;
		internalId = this.hierarchyGenerator.nodeCount;
		hierarchyGenerator.nodeCount++;
		wasProcessed = false;
	}
	
	public void addLine(String[] line){
		if(lines == null){
			lines = new LinkedList<String[]>();
		}
		
		lines.addFirst(line);
	}

	public void processChildNodes(int[] columnOrder, int currentColumnIndex) {
		int currentColumn = 0;
		if(currentColumnIndex>=0){
			currentColumn = columnOrder[currentColumnIndex];
		}
		
		if(currentColumn == columnIndex || wasProcessed){
			//determine the next column to be processed
			//the numbers are as entered by the user with the first column having the index 1!
			int childColumn = columnOrder[currentColumnIndex+1];
			//go through lines in reverse order to avoid index problems when removing lines
			for(int lineIndex = lines.size()-1; lineIndex >= 0 ; lineIndex--){
				String[] line = lines.get(lineIndex);
				//check if index is not out of bound
				if(line.length > childColumn){
					
					//get the column to process from the current line
					String conceptName = line[childColumn];
					if(!conceptName.equals("")){
						if(childNodes == null){
							childNodes = new TreeMap<String,HierarchyNode>();
							orderedChildNodes = new Vector<String>();
						}
						if(!childNodes.containsKey(conceptName)){
							HierarchyNode childNode = new HierarchyNode(conceptName, hierarchyGenerator.generateNewID(conceptName,this), childColumn, hierarchyGenerator);
							childNodes.put(conceptName, childNode);
							orderedChildNodes.add(conceptName);
						}
						childNodes.get(conceptName).addLine(line);
						lines.remove(lineIndex);
					}else{
						//lines that have content for the currently processed columns remain with this node, to be processed as properties later on
					}
				}else{
					//show warning: can not read column
					String columnName = "";
					if(hierarchyGenerator.columnNames != null && hierarchyGenerator.columnNames.length > childColumn){
						columnName = "('"+hierarchyGenerator.columnNames[childColumn]+"') ";
					}
					System.out.println("can not find column "+childColumn+" "+columnName+"in line "+line[0]+".");
				}
			}
			//all the lines are processed now
			wasProcessed = true;
		
		}
		
		if(currentColumn != columnIndex){
			//if there are more column order elements, go through all child nodes and have them process their child nodes recursively
			if(childNodes != null){				
				//call the same method on the child nodes
				for(String childNodeName:orderedChildNodes){
					childNodes.get(childNodeName).processChildNodes(columnOrder,currentColumnIndex);
				}
			}
		}
	}
	
	public void printChildNodes(BufferedWriter out, int columnToPrint){
		if(childNodes == null){
			return;
		}
		if(columnIndex == columnToPrint){

			String showBroaderReference = hierarchyGenerator.getSetting(HierarchyGenerator.showBroaderReferenceKey);
			String showNarrowerReference = hierarchyGenerator.getSetting(HierarchyGenerator.showNarrowerReferenceKey);
			
			for(String childNodeName:orderedChildNodes){
				HierarchyNode childNode = childNodes.get(childNodeName);
				
				//check for column specific templates, if not found, use default templates
				String elementTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.columnPrefix+childNode.columnIndex+HierarchyGenerator.elementTemplateKey);
				if(elementTemplate.length() == 0){
					elementTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.elementTemplateKey);
					//check if still empty string: warning
					if(elementTemplate.length() == 0){
						System.out.println("Warning: There is no "+HierarchyGenerator.elementTemplateKey+" for column "+childNode.columnIndex+". The element "+childNode.id+" ('"+childNode.name+"') will be skipped.");
						continue;
					}
				}
				String broaderReferenceTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.columnPrefix+childNode.columnIndex+HierarchyGenerator.broaderReferenceTemplateKey);
				if(broaderReferenceTemplate.length() == 0){
					broaderReferenceTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.broaderReferenceTemplateKey);
				}
				String narrowerReferenceTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.columnPrefix+childNode.columnIndex+HierarchyGenerator.narrowerReferenceTemplateKey);
				if(narrowerReferenceTemplate.length() == 0){
					narrowerReferenceTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.narrowerReferenceTemplateKey);
				}
				
				String elementBlock = elementTemplate.replace("<id>", childNode.id);
				elementBlock = elementBlock.replace("<name>", childNode.name);
				
				String relations = "";
				if((showBroaderReference.equals("true")||showBroaderReference.equals("1")) && !id.equals(HierarchyGenerator.rootId)){
					if(broaderReferenceTemplate.length() > 0){
						relations = broaderReferenceTemplate.replaceAll("<parent>", id)+"\n";
					}else{
						System.out.println("Warning: There is no "+HierarchyGenerator.broaderReferenceTemplateKey+" for column "+childNode.columnIndex+". The references for node "+childNode.id+" ('"+childNode.name+"') will not be displayed.");
					}
				}
				if(showNarrowerReference.equals("true")|showNarrowerReference.equals("1")){
					if(childNode.childNodes != null){
						if(narrowerReferenceTemplate.length() > 0){
							for(HierarchyNode grandChildNode:childNode.childNodes.values()){
								relations = relations + narrowerReferenceTemplate.replace("<child>", grandChildNode.id)+"\n";
							}
						}else{
							System.out.println("Warning: There is no "+HierarchyGenerator.narrowerReferenceTemplateKey+" for column "+childNode.columnIndex+". The references for node "+childNode.id+" ('"+childNode.name+"') will not be displayed.");
						}
					}
				}
				elementBlock = elementBlock.replace("<relations>", relations);
				
				//print properties of node
				String properties = "";
				if(childNode.lines.size()>0){
					if(childNode.lines.size()>1){
						//warning: only using first line for element properties
						String lineNumbers = childNode.lines.getLast()[0];
						for(int lineIndex = childNode.lines.size()-2; lineIndex > 0;lineIndex--){
							lineNumbers = lineNumbers + ", " + childNode.lines.get(lineIndex)[0];
						}
						lineNumbers = lineNumbers + " and " + childNode.lines.get(0)[0];
						System.out.println("There are multiple lines for the object properties of element "+childNode.id+" ('"+childNode.name+"'); line numbers: "+lineNumbers+"\n\tOnly the properties of line "+childNode.lines.getLast()[0]+" will be used.");
					}
					//use last line, since the lines are in reversed order
					String[] line = childNode.lines.getLast();
					for(int propertyColumnIndex:hierarchyGenerator.propertyColumns){
												
						if(line.length > propertyColumnIndex){
							//try to load property template specific for this property
							String propertyTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.columnPrefix+propertyColumnIndex+HierarchyGenerator.propertyTemplateKey);
							if(propertyTemplate.length() == 0){
								//alternative 1: try to load property template specific for this element
								propertyTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.columnPrefix+childNode.columnIndex+HierarchyGenerator.propertyTemplateKey);
								//alternative 2: try to load default property template
								if(propertyTemplate.length() == 0){
									propertyTemplate = hierarchyGenerator.getSetting(HierarchyGenerator.defaultPrefix+HierarchyGenerator.propertyTemplateKey);
									if(propertyTemplate.length()==0){
										System.out.println("Warning: There is no "+HierarchyGenerator.propertyTemplateKey+" for column "+propertyColumnIndex+". This property will not be displayed for node "+childNode.id+" ('"+childNode.name+"').");
									}
								}
							}
							String propertyValue = line[propertyColumnIndex];
							if(!propertyValue.equals("")){
								String propertyOutput = propertyTemplate;
								//if columnNames exist, replace <property> place holder with column name
								if(hierarchyGenerator.columnNames != null && hierarchyGenerator.columnNames.length > propertyColumnIndex){
									String propertyKey = hierarchyGenerator.columnNames[propertyColumnIndex];
									propertyOutput = propertyOutput.replace("<property>", propertyKey);
								}
								properties = properties + propertyOutput.replace("<value>", propertyValue)+"\n";
							}
						}else{
							//warning: can not read column
							String columnName = "";
							if(hierarchyGenerator.columnNames != null && hierarchyGenerator.columnNames.length > propertyColumnIndex){
								columnName = "('"+hierarchyGenerator.columnNames[propertyColumnIndex]+"') ";
							}
							System.out.println("can not find column "+propertyColumnIndex+" "+columnName+"in line "+line[0]+".");
						}
					}
				}
				elementBlock = elementBlock.replace("<properties>", properties);
				
				try {
					out.write(elementBlock+"\n");
					out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			for(String childNodeName:orderedChildNodes){
				HierarchyNode childNode = childNodes.get(childNodeName);
				childNode.printChildNodes(out, columnToPrint);
			}
		}
	}
	
	public String getName() {
		return name;
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	public List<String[]> getLines() {
		return lines;
	}
	
	public void setLines(LinkedList<String[]> lines) {
		this.lines = lines;
		Collections.reverse(lines);
	}

	public Map<String, HierarchyNode> getChildNodes() {
		return childNodes;
	}

}

