/*
 * Title:        EdgeCloudSim - Basic Edge Orchestrator implementation
 * 
 * Description: 
 * BasicEdgeOrchestrator implements basic algorithms which are
 * first/next/best/worst/random fit algorithms while assigning
 * requests to the edge devices.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ddos.edge_orchestrator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import com.opencsv.CSVWriter;

import ddos.MainApp;
import ddos.core.DdosSimSettings;
import ddos.util.DdosSimLogger;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;

/**
 * 
 * ddos_note: modified on top of BasicEdgeOrchestrator to perform data manipulation 
 * 			  and ddos detection
 *
 */
public class DdosDetectionOrchestrator extends EdgeOrchestrator {
	private int numberOfHost; //used by load balancer
	private int lastSelectedHostIndex; //used by load balancer
	private int[] lastSelectedVmIndexes; //used by each host individually
	
	//ddos_note: newly introduced fields for ddos
	private static final int DDOS_ATTACK = 0;
	private static final int OUTPUT_DATA = 1;
	private HashSet<Integer> MalicousApp=new HashSet<Integer>();// map to record malicious app id, used for blocking the malicious apps
	private HashMap<Integer, AppProfile> AppProfileMap=new HashMap<Integer, AppProfile>(); //map used to store application traffic informatin
	private HashMap<Location, ArrayList<Task>> taskLocationList= new HashMap<Location, ArrayList<Task>>(); // map used to identify location of traffic
	
	public DdosDetectionOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfHost=DdosSimSettings.getInstance().getNumOfEdgeHosts();
		
		lastSelectedHostIndex = -1;
		lastSelectedVmIndexes = new int[numberOfHost];
		for(int i=0; i<numberOfHost; i++)
			lastSelectedVmIndexes[i] = -1;
	}
	
	//ddos_note: new method to record task information
	public void recordTask(Task task) {
		AppProfile profile=AppProfileMap.get(task.getTaskType());
		
		if(profile==null) {
			profile=new AppProfile(task);
			AppProfileMap.put(profile.id, profile);
		}
		
		profile.newTask();
		
		//record location info
		Location location=  SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
				
		if(taskLocationList.containsKey(location) && taskLocationList.get(location)!=null) {
			taskLocationList.get(location).add(task);
		}else {
			ArrayList<Task> newList=new ArrayList<Task>();
			newList.add(task);
			taskLocationList.put(location,newList );
		}
		
	}

	//ddos_note: same as BasicEdgeOrchestrator
	@Override
	public int getDeviceToOffload(Task task) {
		recordTask(task);
		
		int result = DdosSimSettings.GENERIC_EDGE_DEVICE_ID;
		if(!simScenario.equals("SINGLE_TIER")){
			//decide to use cloud or Edge VM
			int CloudVmPicker = SimUtils.getRandomNumber(0, 100);
			
			if(CloudVmPicker <= DdosSimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][1])
				result = DdosSimSettings.CLOUD_DATACENTER_ID;
			else
				result = DdosSimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		
		return result;
	}
	//ddos_note: same as BasicEdgeOrchestrator
	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;
		//System.out.println("\n"+CloudSim.clock()+" on task");
		if(deviceId == DdosSimSettings.CLOUD_DATACENTER_ID){
			//Select VM on cloud devices via Least Loaded algorithm!
			double selectedVmCapacity = 0; //start with min value
			List<Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
			for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
				List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
	            }
			}
		}
		else if(simScenario.equals("TWO_TIER_WITH_EO"))
			selectedVM = selectVmOnLoadBalancer(task);
		else
			selectedVM = selectVmOnHost(task);
		
		//record the task as well as the required resources
		
				
		return selectedVM;
	}
	//ddos_note: same as BasicEdgeOrchestrator
	public EdgeVM selectVmOnHost(Task task){
		EdgeVM selectedVM = null;
		
		Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
		//in our scenasrio, serving wlan ID is equal to the host id
		//because there is only one host in one place
		int relatedHostId=deviceLocation.getServingWlanId();
		
		//System.out.println("MD:"+task.getMobileDeviceId()+" Host:"+relatedHostId);
		
		
		List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
		
		if(policy.equalsIgnoreCase("RANDOM_FIT")){
			int randomIndex = SimUtils.getRandomNumber(0, vmArray.size()-1);
			double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(randomIndex).getVmType());
			double targetVmCapacity = (double)100 - vmArray.get(randomIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
			if(requiredCapacity <= targetVmCapacity)
				selectedVM = vmArray.get(randomIndex);
		}
		else if(policy.equalsIgnoreCase("WORST_FIT")){
			double selectedVmCapacity = 0; //start with min value
			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
				double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
				double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
				if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
					selectedVM = vmArray.get(vmIndex);
					selectedVmCapacity = targetVmCapacity;
				}
			}
		}
		else if(policy.equalsIgnoreCase("BEST_FIT")){
			double selectedVmCapacity = 101; //start with max value
			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
				double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
				double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
				if(requiredCapacity <= targetVmCapacity && targetVmCapacity < selectedVmCapacity){
					selectedVM = vmArray.get(vmIndex);
					selectedVmCapacity = targetVmCapacity;
				}
			}
		}
		else if(policy.equalsIgnoreCase("FIRST_FIT")){
			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
				double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
				double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
				if(requiredCapacity <= targetVmCapacity){
					selectedVM = vmArray.get(vmIndex);
					break;
				}
			}
		}
		else if(policy.equalsIgnoreCase("NEXT_FIT")){
			int tries = 0;
			while(tries < vmArray.size()){
				lastSelectedVmIndexes[relatedHostId] = (lastSelectedVmIndexes[relatedHostId]+1) % vmArray.size();
				double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(lastSelectedVmIndexes[relatedHostId]).getVmType());
				double targetVmCapacity = (double)100 - vmArray.get(lastSelectedVmIndexes[relatedHostId]).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
				if(requiredCapacity <= targetVmCapacity){
					selectedVM = vmArray.get(lastSelectedVmIndexes[relatedHostId]);
					break;
				}
				tries++;
			}
		}
		
		return selectedVM;
	}

	//ddos_note: modified for blocking malicious apps
	public EdgeVM selectVmOnLoadBalancer(Task task){
		EdgeVM selectedVM = null;
		
		//if the app is malicious, block it
		if(MalicousApp.contains(task.getTaskType())) {
			if(MainApp.blockMaliciousApps) {
				return null;
			}
		}
		
		if(policy.equalsIgnoreCase("RANDOM_FIT")){
			int randomHostIndex = SimUtils.getRandomNumber(0, numberOfHost-1);
			List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(randomHostIndex);
			int randomIndex = SimUtils.getRandomNumber(0, vmArray.size()-1);
			
			double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(randomIndex).getVmType());
			double targetVmCapacity = (double)100 - vmArray.get(randomIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
			if(requiredCapacity <= targetVmCapacity)
				selectedVM = vmArray.get(randomIndex);
		}
		else if(policy.equalsIgnoreCase("WORST_FIT")){
			double selectedVmCapacity = 0; //start with min value
			for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
				}
			}
		}
		else if(policy.equalsIgnoreCase("BEST_FIT")){
			double selectedVmCapacity = 101; //start with max value
			for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity < selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
				}
			}
		}
		else if(policy.equalsIgnoreCase("FIRST_FIT")){
			for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						break;
					}
					
					task.getUtilizationModelBw();

					
					
				}
			}
		}
		else if(policy.equalsIgnoreCase("NEXT_FIT")){
			int hostCheckCounter = 0;	
			while(selectedVM == null && hostCheckCounter < numberOfHost){
				int tries = 0;
				lastSelectedHostIndex = (lastSelectedHostIndex+1) % numberOfHost;

				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(lastSelectedHostIndex);
				while(tries < vmArray.size()){
					lastSelectedVmIndexes[lastSelectedHostIndex] = (lastSelectedVmIndexes[lastSelectedHostIndex]+1) % vmArray.size();
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(lastSelectedVmIndexes[lastSelectedHostIndex]).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(lastSelectedVmIndexes[lastSelectedHostIndex]).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity){
						selectedVM = vmArray.get(lastSelectedVmIndexes[lastSelectedHostIndex]);
						break;
					}
					tries++;
				}

				hostCheckCounter++;
			}
		}
		
		return selectedVM;
	}
	
	//ddos_note: function to calculate service similarity score.
	//			 re-write for research purpose
	private Map<String, Double> calculateServiceSimilarityScore() {
		//example similarity score calculation, you should implement your own solution here
		
		Map<String, Double> scoreMap=new HashMap<String,Double>();
		
		int totalNumTasks=0;
		Map<String, Integer> taskCounterMap=new HashMap<String,Integer>();
		
		for(Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
			String serviceName=entry.getValue().taskName.split(" ")[0];
			if(taskCounterMap.containsKey(serviceName)) {
				taskCounterMap.replace(serviceName, taskCounterMap.get(serviceName)+entry.getValue().taskCounter);
			}else {
				taskCounterMap.put(serviceName, entry.getValue().taskCounter);
			}
			totalNumTasks+=entry.getValue().taskCounter;
		}
		
		//calculate similarity score
		for(Map.Entry<String, Integer> entry : taskCounterMap.entrySet()) {
			scoreMap.put(entry.getKey(), (double)entry.getValue()/totalNumTasks);
		}
		
		return scoreMap;
	}

	@Override
	public void processEvent(SimEvent arg0) {
		synchronized(this){
			switch (arg0.getTag()) {
			case DDOS_ATTACK:
				try {
					
					//calculate service similarity score
					Map<String,Double> similarityScoreMap=calculateServiceSimilarityScore();
					System.out.println(" \n");
					System.out.println("Print similarity score");
					System.out.println(similarityScoreMap);
					
					
					//detect unusal traffic flow
					boolean highTraffic=false;
					boolean underAttack=false;
					
					int trafficSum=0;
					//for testing
					for (Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
						trafficSum+=entry.getValue().taskCounter;
					}
					
					if(trafficSum>MainApp.threshholdHighLoadPerEdgeDev*MainApp.numEdgeDevices) {
						System.out.println("At:"+CloudSim.clock()+" traffic is :"+trafficSum);
						System.out.println("System is undergoing high traffic");
						highTraffic=true;
					}else {
						System.out.println("Low traffic");
					}
					
					//check event calendar
					if(highTraffic) {
						if(CloudSim.clock()>=MainApp.PeakTimeStart && CloudSim.clock()<=MainApp.PeakTimeEnd) {
							//during traffic peak, do nothing
							System.out.println("During traffic peak time, do nothing");
						}else {
							//check traffic location
							Location busyLocation=null;
							int sizeComparator=0;
							
							for(Location it: taskLocationList.keySet()) {
								if(taskLocationList.get(it).size()>sizeComparator) {
									sizeComparator=taskLocationList.get(it).size();
									busyLocation=it;
								}
								
							}
							if(busyLocation.getXPos()!=MainApp.eventCrowdLocation.getXPos() 
									|| busyLocation.getYPos()!=MainApp.eventCrowdLocation.getYPos()) {
								System.out.println("Busiest location generates traffic: "+sizeComparator);
								System.out.println("Busiest location is: x:"+busyLocation.getXPos()+" y:"+busyLocation.getYPos());
								System.out.println("Not crowd event, judge as system under attack!");
								underAttack=true;
							}else {
								System.out.println("Busiest location generates traffic: "+sizeComparator);
								System.out.println("Busiest location is: x:"+busyLocation.getXPos()+" y:"+busyLocation.getYPos());
								System.out.println("Is crowd event, judge as system not under attack.");
							}
						}
					}
					
					if(underAttack) {
						//detect malicious applicaiton
						int totalApp=AppProfileMap.size();
						int correctDetection=0;
						for (Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
							AppProfile profile=entry.getValue();
							boolean isAttacker=DdosDetector.detectMaliciousApp((double)profile.taskCounter/(profile.appEndTime-profile.appStartTime), 
									DdosSimLogger.getInstance().getTotalBwUsage(profile.id)/(profile.appEndTime-profile.appStartTime),
									DdosSimLogger.getInstance().getTotalServiceTime(profile.id)/(profile.appEndTime-profile.appStartTime), 
									DdosSimLogger.getInstance().getTotalProcessingTime(profile.id)/(profile.appEndTime-profile.appStartTime), 
									DdosDetector.algorithm.SVM);
							if(isAttacker) {
								if(profile.taskName.contains("Attacker"))correctDetection++;
							}else {
								if(!profile.taskName.contains("Attacker"))correctDetection++;
							}
						}
						System.out.println("At "+CloudSim.clock()+" system under attack.");
						System.out.println("At "+CloudSim.clock()+" Detection Accuracy is: "+(double)100*correctDetection/totalApp+"%");
						
						
					}else {
						System.out.println("At "+CloudSim.clock()+" system not under attack, do nothing");
					}
						
				
					//schedule the next detection
					schedule(getId(), MainApp.ddosPeriodicDetectionWindow, DDOS_ATTACK);
					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
				
			case OUTPUT_DATA:
				try {
					
					//At detection window, output application performance metrics to csv vile
					try {
						CSVWriter writer = new CSVWriter(new FileWriter(MainApp.DatsetCSVPath, true));
						for (Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
							AppProfile profile=entry.getValue();
							String attackerType="ATTACKER";
							if(!profile.taskName.contains("ATTACKER")) {
								attackerType="NORMAL";
							}
							
							String [] record = new String[] {
									
									String.format("%.6f", (double)profile.taskCounter/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)DdosSimLogger.getInstance().getTotalBwUsage(profile.id)/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)DdosSimLogger.getInstance().getTotalServiceTime(profile.id)/(profile.appEndTime-profile.appStartTime)),
									String.format("%.6f", (double)DdosSimLogger.getInstance().getTotalProcessingTime(profile.id)/(profile.appEndTime-profile.appStartTime)),
									attackerType,
							};
							
							writer.writeNext(record);
						}
						writer.close();
					}catch(IOException e) {
						e.printStackTrace();
					}					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
			default:
				DdosSimLogger.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
	}

	@Override
	public void startEntity() {
		// TODO Auto-generated method stub
		if(MainApp.ddosPeriodicDetectionWindow==0) {
			System.out.println("DDOS_DETECTION_WINDOW should not be 0!");
			System.exit(0);
		}
		
		if(MainApp.datasetTrainingMode==true) {
			schedule(getId(), MainApp.ddosPeriodicDetectionWindow, OUTPUT_DATA);	
		}else {
			schedule(getId(), MainApp.ddosPeriodicDetectionWindow, DDOS_ATTACK);
		}
		
	}
}

/*
 * ddos_note: newly defined class to record simple application-related info, have similar 
 * function as SimLogger but here we only record information for duration of 
 * current detection window, while SimLogger records all historical data
 */
			 
class AppProfile{
	int id;
	String taskName;
	int taskCounter;
	double totalBwUsage;
	double totalServiceTime;
	double totalProcessingTime;
	double appStartTime;
	double appEndTime;
	
	public AppProfile(Task task) {
		super();
		this.id = task.getTaskType();
		this.taskName=DdosSimSettings.getInstance().getTaskName(id);
		taskCounter=0;
		appStartTime=0;
	}

	public void newTask() {
		this.taskCounter++;
		if(appStartTime==0)appStartTime=CloudSim.clock();
		
		appEndTime=CloudSim.clock();
	}

	@Override
	public String toString() {
		return "AppProfile [type=" + id + ", taskName=" + taskName + ", taskCounter=" + taskCounter
				+ ", totalBwUsage=" + totalBwUsage + ", totalServiceTime=" + totalServiceTime + ", appStartTime="
				+ appStartTime + ", appEndTime=" + appEndTime + "]";
	}


	
}
