/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.DdosDetectionOrchestrator;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MainApp {
	public static boolean datasetTrainingMode=false;
	public static boolean blockMalicious=true;
	public static int simTime = 60000;//in seconds
	public static double ddosPeriodicDetectionWindow=6000;
	public static int normalAppPercentage=0;
	public static int ddosPercentage=10;
	public static int peakAppPercentage=0;
	public static int eventCrowdPercentage=90;
	public static int numExperiment=1;
	public static int iterationNumber=1;
	public static String trainingDatasetBaseFolder="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\";
	public static int numMobileDevices=200;
	public static int numEdgeDevices=10;
	public static double PeakTimeStart=0.5*simTime;
	public static double PeakTimeEnd=0.6*simTime;
	public static int eventCrowdDatacenterId=0;
	public static Location eventCrowdLocation= new Location (0,0,eventCrowdDatacenterId,eventCrowdDatacenterId);
	public static int threshholdHighLoadPerEdgeDev=15000;
	
	public static int numTypeOfService=2;
	
	
	/**
	 * Creates main() to run this example
	 */
	
	public static void simulate()  {
		
		
		
		
		SmartParkingConfigFactory.getInstance().generateEdgeConfigFile(numEdgeDevices);
	    try {
	    	Service s1= new Service("Parking",100, 25,25,25,25);//register a service with default parameters
			Service s2= new Service("Map",100,25,25,25,25,50,20,0,15,5,10,2,1500,2,25,2,2000,2,2,2,20,2,2,0,0);
			
			SmartParkingConfigFactory.getInstance().addNewService(s1);
			SmartParkingConfigFactory.getInstance().addNewService(s2);
			SmartParkingConfigFactory.getInstance().generateApplicationConfigFile();
		} catch (Exception e1) {
			
			e1.printStackTrace();
		}
		
		
	    String configFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\default_config.properties";
	    String applicationsFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications_DDoS.xml";
	    String edgeDevicesFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\edge_devices.xml";
	    String outputFolder = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\Test\\" + iterationNumber;

		//load settings from configuration file
	    SimSettings.refresh();
		SimSettings SS = SimSettings.getInstance();
		if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
			SimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		
		//change some settings from control board
		SS.setSIMULATION_TIME(simTime);
		
		if(SS.getFileLoggingEnabled()){
			SimLogger.enableFileLog();
			SimUtils.cleanOutputFolder(outputFolder);
		}
		
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");
	
		SS.setMIN_NUM_OF_MOBILE_DEVICES(numMobileDevices);
		SS.setMAX_NUM_OF_MOBILE_DEVICES(numMobileDevices);
		for(int itr=0;itr<numExperiment;itr++) {
        	for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
    		{
    			for(int k=0; k<SS.getSimulationScenarios().length; k++)
    			{
    				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
    				{
    					String simScenario = SS.getSimulationScenarios()[k];
    					String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
    					Date ScenarioStartDate = Calendar.getInstance().getTime();
    					now = df.format(ScenarioStartDate);
    					
    					SimLogger.printLine("Scenario started at " + now);
    					SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
    					SimLogger.printLine("Duration: " + SS.getSimulationTime() + " second(s) - Poisson: " + SS.getTaskLookUpTable()[0][2] + " - #devices: " + j);
    					SimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");
    					try
    					{
    						// First step: Initialize the CloudSim package. It should be called
    						// before creating any entities.
    						int num_user = 2;   // number of grid users
    						Calendar calendar = Calendar.getInstance();
    						boolean trace_flag = false;  // mean trace events
    				
    						// Initialize the CloudSim library
    						CloudSim.init(num_user, calendar, trace_flag, 0.01);
    						// Generate EdgeCloudsim Scenario Factory
    						ScenarioFactory sampleFactory = new SampleScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);
    						// Generate EdgeCloudSim Simulation Manager
    						SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy);
    						
    						DdosDetectionOrchestrator.DDOS_DETECTION_WINDOW=ddosPeriodicDetectionWindow;
    						// Start simulation
    						manager.startSimulation();
    						
    					}
    					catch (Exception e)
    					{
    						SimLogger.printLine("The simulation has been terminated due to an unexpected error");
    						e.printStackTrace();
    						System.exit(0);
    					}
    					
    					Date ScenarioEndDate = Calendar.getInstance().getTime();
    					now = df.format(ScenarioEndDate);
    					SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
    					SimLogger.printLine("----------------------------------------------------------------------");
    					
    				}//End of orchestrators loop
    			}//End of scenarios loop
    		}//End of mobile devices loop
        }      
	}
	
	
	public static void main(String[] args) throws IOException{
		//disable console output of cloudsim library
		Log.disable();
		//enable console output and file output of this application
		
		if(datasetTrainingMode) {
			SimLogger.enablePrintLog();
			//numMobileDevices=10;
			//ddosPeriodicDetectionWindow=6000;
			simTime=6001;
			for(int i=0;i<10;i++) {
				simulate();
			}
		}else {
			SimLogger.enablePrintLog();
			for(int i=1;i<2;i++) {
				numMobileDevices=200;
				System.out.println("Iteration:"+i);
				simulate();
				System.out.println("\n");
			}
			
		}
		
		
	}
}
