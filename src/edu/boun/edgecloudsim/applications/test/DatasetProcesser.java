package edu.boun.edgecloudsim.applications.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatasetProcesser {
	public static String datasetFolder="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\DDoS_50_Iter_1\\";
	public static String networkProfileName="network_profile.csv";
	public static String nodeProfileName="node_profile.csv";
	public static String profileFolder="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Profile\\";
	//important: the dataset should use consistent timewindow for data output (in MainApp/SimLogger setting), otherwise is meaningless
	public static void main(String[] args) throws Exception {
		DatasetProcesser dp=new DatasetProcesser();
		File dir = new File(datasetFolder);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				dp.processFile(child);
			}
		} else {
			throw new Exception("directory path is not correct");
		}
		
	}
	
	public void processFile(File data) {
		//read the csv file into string matrix
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(data.getAbsolutePath()))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		        records.add(Arrays.asList(values));
		    }
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		String featureName=data.getName().split(".")[0];
		
		//take the sum value of each time window
		for(int i=1;i<records.size();i++) {
			double sum=0;
			for(int j=1;j<records.get(0).size();j++) {
				sum+=Double.parseDouble(records.get(i).get(j));
			}
			
		}
		
		
	}

}
