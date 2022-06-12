/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ddos;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import ddos.config.ConfigurationFileFactory;
import ddos.config.Service;
import ddos.core.DdosSimSettings;
import ddos.util.DdosSimLogger;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MainApp {
	
	//switches for different running mode
	public static boolean datasetTrainingMode=false;
	
	//global parameters
	public static int simTime = 60000;//in seconds
	public static int numMobileDevices=200;
	public static int numEdgeDevices=10;
	public static int numExperiment=1;
	
	//application settings/parameters
	public static double PeakTimeStart=0.5*simTime;
	public static double PeakTimeEnd=0.6*simTime;
	public static int eventCrowdDatacenterId=0;
	public static Location eventCrowdLocation= new Location (0,0,eventCrowdDatacenterId,eventCrowdDatacenterId);
	
	
	//ddos detection parameters
	public static boolean blockMaliciousApps=true;
	public static double ddosPeriodicDetectionWindow=6000;
	public static int threshholdHighLoadPerEdgeDev=15000; //threshold for high load (trigger ddos detection), this is a emperical number, you may also use ML to discover the threshold given fixed network scenario

	//external file paths
	public static String DatsetCSVPath="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\data_all.csv";
	public static String EdgeConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\edge_devices.xml";
	public static String DdosApplicationConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications_ddos.xml";
	public static String PropertiesConfigPath = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\default_config.properties";
	public static String SimloggerOutputFolder = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\Test\\";
	public static void simulate()  {
		
		//generate configuration files
		ConfigurationFileFactory.getInstance().generateEdgeConfigFile(numEdgeDevices);
	    
		
		try {
	    	Service s1= new Service("Parking",100, 25,25,25,25);//register a service with default parameters
			Service s2= new Service("Map",100,25,25,25,25,50,20,0,15,5,10,2,1500,2,25,2,2000,2,2,2,20,2,2,0,0); //register a customized service
			ConfigurationFileFactory.getInstance().addNewService(s1);
			ConfigurationFileFactory.getInstance().addNewService(s2);
			ConfigurationFileFactory.getInstance().generateApplicationConfigFile();
		} catch (Exception e1) {
			
			e1.printStackTrace();
		}
		
		//load settings from configuration file
	    DdosSimSettings.refresh();
		DdosSimSettings SS = DdosSimSettings.getInstance();
		if(SS.initialize(PropertiesConfigPath, EdgeConfigPath, DdosApplicationConfigPath) == false){
			DdosSimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		
		SS.setSIMULATION_TIME(simTime);
	
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		DdosSimLogger.printLine("Simulation started at " + now);
		DdosSimLogger.printLine("----------------------------------------------------------------------");
	
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
    					
    					DdosSimLogger.printLine("Scenario started at " + now);
    					DdosSimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy );
    					DdosSimLogger.printLine("Duration: " + SS.getSimulationTime() + " second(s) - Poisson: " + SS.getTaskLookUpTable()[0][2] + " - #devices: " + j);
    					DdosSimLogger.getInstance().simStarted(SimloggerOutputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");
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
    						// Start simulation
    						manager.startSimulation();
    						
    					}
    					catch (Exception e)
    					{
    						DdosSimLogger.printLine("The simulation has been terminated due to an unexpected error");
    						e.printStackTrace();
    						System.exit(0);
    					}
    					
    					Date ScenarioEndDate = Calendar.getInstance().getTime();
    					now = df.format(ScenarioEndDate);
    					DdosSimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
    					DdosSimLogger.printLine("----------------------------------------------------------------------");
    					
    				}//End of orchestrators loop
    			}//End of scenarios loop
    		}//End of mobile devices loop
        }      
	}
	
	public static void main(String[] args) throws IOException{
		//disalbe console output of cloudsim library
		Log.disable();
		DdosSimLogger.enablePrintLog();
		if(datasetTrainingMode) {
			simTime=6001;
			for(int i=0;i<100;i++) {
				simulate();
			}
		}else {
			simulate();
		}
	}
}
