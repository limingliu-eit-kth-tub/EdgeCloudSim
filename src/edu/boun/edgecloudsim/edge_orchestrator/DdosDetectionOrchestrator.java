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

package edu.boun.edgecloudsim.edge_orchestrator;

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

import edu.boun.edgecloudsim.applications.test.DdosDetector;
import edu.boun.edgecloudsim.applications.test.MainApp;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class DdosDetectionOrchestrator extends EdgeOrchestrator {
	private int numberOfHost; //used by load balancer
	private int lastSelectedHostIndex; //used by load balancer
	private int[] lastSelectedVmIndexes; //used by each host individually
	private static final int DDOS_ATTACK = 0;
	private static final int OUTPUT_DATA = 1;
	public static double DDOS_DETECTION_WINDOW=0;
	private HashSet<Integer> MalicousApp=new HashSet<Integer>();
	
	private static double FAILURE_RATE_RECORD=0;
	private static double AVERAGE_DELAY_RECORD=0;
	
	
	
	private HashMap<Integer, AppProfile> AppProfileMap=new HashMap<Integer, AppProfile>();
	
	private HashMap<Location, ArrayList<Task>> taskLocationList= new HashMap<Location, ArrayList<Task>>(); 
	
	public DdosDetectionOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfHost=SimSettings.getInstance().getNumOfEdgeHosts();
		
		lastSelectedHostIndex = -1;
		lastSelectedVmIndexes = new int[numberOfHost];
		for(int i=0; i<numberOfHost; i++)
			lastSelectedVmIndexes[i] = -1;
	}
	
	public void recordTask(Task task) {
		AppProfile profile=AppProfileMap.get(task.getTaskType());
		
		if(profile==null) {
			profile=new AppProfile(task);
			AppProfileMap.put(profile.type, profile);
		}
		
		profile.newTask();
		
		//record location info
		Location location=  SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
		
//		System.out.println("Task "+profile.taskName+" is at location "+location.getXPos()+" "+location.getYPos());
		
		if(taskLocationList.containsKey(location) && taskLocationList.get(location)!=null) {
			taskLocationList.get(location).add(task);
		}else {
			ArrayList<Task> newList=new ArrayList<Task>();
			newList.add(task);
			taskLocationList.put(location,newList );
		}
		
	}

	@Override
	public int getDeviceToOffload(Task task) {
		recordTask(task);
		
		int result = SimSettings.GENERIC_EDGE_DEVICE_ID;
		if(!simScenario.equals("SINGLE_TIER")){
			//decide to use cloud or Edge VM
			int CloudVmPicker = SimUtils.getRandomNumber(0, 100);
			
			if(CloudVmPicker <= SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][1])
				result = SimSettings.CLOUD_DATACENTER_ID;
			else
				result = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		
		return result;
	}
	
	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;
		//System.out.println("\n"+CloudSim.clock()+" on task");
		if(deviceId == SimSettings.CLOUD_DATACENTER_ID){
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

	public EdgeVM selectVmOnLoadBalancer(Task task){
		EdgeVM selectedVM = null;
		
		
		
//		Location l=SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
//		l.getXPos();
//		l.getYPos();
//		
		
		//if the app is malicious, ban it
		if(MalicousApp.contains(task.getTaskType())) {
			if(MainApp.blockMalicious) {
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

	@Override
	public void processEvent(SimEvent arg0) {
		// TODO Auto-generated method stub
		synchronized(this){
			switch (arg0.getTag()) {
			case DDOS_ATTACK:
				try {
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
									SimLogger.getInstance().getTotalBwUsage(profile.type)/(profile.appEndTime-profile.appStartTime),
									SimLogger.getInstance().getTotalServiceTime(profile.type)/(profile.appEndTime-profile.appStartTime), 
									SimLogger.getInstance().getTotalProcessingTime(profile.type)/(profile.appEndTime-profile.appStartTime), 
									DdosDetector.algorithm.SVM);
//							System.out.println(SimLogger.getInstance().getTotalServiceTime(profile.type));
							if(isAttacker) {
//								System.out.println(profile.taskName+" is detected as ATTACKER");
								if(profile.taskName.contains("Attacker"))correctDetection++;
							}else {
//								System.out.println(profile.taskName+" is detected as NORMAL App");
								if(!profile.taskName.contains("Attacker"))correctDetection++;
							}
						}
						System.out.println("At "+CloudSim.clock()+" system under attack.");
						System.out.println("At "+CloudSim.clock()+" Detection Accuracy is: "+(double)100*correctDetection/totalApp+"%");
					}else {
						System.out.println("At "+CloudSim.clock()+" system not under attack, do nothing");
					}
						
					
					
					//reset the app profile map
					//this.AppProfileMap=new HashMap<Integer, AppProfile>();
					//schedule the next detection
					schedule(getId(), DDOS_DETECTION_WINDOW, DDOS_ATTACK);
					
//					double currentFailureRate = SimLogger.getInstance().getCurrentFailureRateInPercentage();
//					
//					double currentAvgDelay = SimLogger.getInstance().getCurrentResponseDelay();
//					System.out.println("\n failure rate:" +currentFailureRate+"  delay:"+currentAvgDelay);
//					SimLogger.getInstance().updateWindowRecord();
//					String attackLevel=DdosDetector.detectDDoSAttack(currentFailureRate,currentAvgDelay,DdosDetector.algorithm.KMEANS);
//					
//					System.out.println("\n"+CloudSim.clock()+" Detect DDOS attack level: "+attackLevel );
					
//					if(underAttack) {
//						//make prediction on which apps are malicious
//						double[] appReqFreqList=SimLogger.getInstance().getCurrentAppMetric();
//						List<Integer> malicousApps=new ArrayList<Integer>();
//						for(int i=0;i<appReqFreqList.length;i++) {
//							boolean isAttackApp=DdosDetector.detectMaliciousApp(appReqFreqList[i], DdosDetector.algorithm.KMEANS);
//							if(isAttackApp) {
//								MalicousApp.add(i);
//								System.out.println("detect malicous app:" +i+1);
//							}
//						}
//						//add malicious apps into list
//					}
					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
				
			case OUTPUT_DATA:
				try {
					String csv = "..\\Log\\data_all.csv";
					
					
					
					try {
						CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
						for (Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
							AppProfile profile=entry.getValue();
							String attackerType="ATTACKER";
							if(!profile.taskName.contains("ATTACKER")) {
								attackerType="NORMAL";
							}
							
							String [] record = new String[] {
									
									String.format("%.6f", (double)profile.taskCounter/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)SimLogger.getInstance().getTotalBwUsage(profile.type)/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)SimLogger.getInstance().getTotalServiceTime(profile.type)/(profile.appEndTime-profile.appStartTime)),
									String.format("%.6f", (double)SimLogger.getInstance().getTotalProcessingTime(profile.type)/(profile.appEndTime-profile.appStartTime)),
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
				SimLogger.printLine(getName() + ": unknown event type");
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
		if(DDOS_DETECTION_WINDOW==0) {
			System.out.println("DDOS_DETECTION_WINDOW should not be 0!");
			System.exit(0);
		}
		
		if(MainApp.datasetTrainingMode==true) {
			schedule(getId(), DDOS_DETECTION_WINDOW, OUTPUT_DATA);	
		}else {
			schedule(getId(), DDOS_DETECTION_WINDOW, DDOS_ATTACK);
		}
		
	}
}

class AppProfile{
	int type;
	String taskName;
	int taskCounter;
	double totalBwUsage;
	double totalServiceTime;
	double totalProcessingTime;
	double appStartTime;
	double appEndTime;
	
	public AppProfile(Task task) {
		super();
		this.type = task.getTaskType();
		this.taskName=SimSettings.getInstance().getTaskName(type);
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
		return "AppProfile [type=" + type + ", taskName=" + taskName + ", taskCounter=" + taskCounter
				+ ", totalBwUsage=" + totalBwUsage + ", totalServiceTime=" + totalServiceTime + ", appStartTime="
				+ appStartTime + ", appEndTime=" + appEndTime + "]";
	}


	
}
