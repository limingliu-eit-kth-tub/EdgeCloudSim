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
					for (Map.Entry<Integer,AppProfile> entry : AppProfileMap.entrySet()) {
						AppProfile profile=entry.getValue();
						boolean isAttacker=DdosDetector.detectMaliciousApp((double)profile.taskCounter/(profile.appEndTime-profile.appStartTime), 
								profile.totalBwUsage/(profile.appEndTime-profile.appStartTime),
								profile.totalProcessingTime/(profile.appEndTime-profile.appStartTime), 
								DdosDetector.algorithm.KMEANS);
						
						if(isAttacker) {
							System.out.println(profile.taskName+" is detected as ATTACKER");
						}else {
							System.out.println(profile.taskName+" is detected as NORMAL App");
						}
					}
					
					//reset the app profile map
					this.AppProfileMap=new HashMap<Integer, AppProfile>();
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
							if(profile.taskName.contains("Normal App")) {
								attackerType="NORMAL";
							}
							
							String [] record = new String[] {
									
									String.format("%.6f", (double)profile.taskCounter/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)profile.totalBwUsage/(profile.appEndTime-profile.appStartTime)), 
									String.format("%.6f", (double)profile.totalProcessingTime/(profile.appEndTime-profile.appStartTime)), 
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
		this.totalBwUsage = SimLogger.getInstance().getTotalBwUsage(type);
		this.totalServiceTime = SimLogger.getInstance().getTotalServiceTime(type);
		this.totalProcessingTime = SimLogger.getInstance().getTotalProcessingTime(type);
		
		if(appStartTime==0)appStartTime=CloudSim.clock();
		
		appEndTime=CloudSim.clock();
	}

	@Override
	public String toString() {
		return "AppProfile [type=" + type + ", taskCounter=" + taskCounter + ", totalBwUsage=" + totalBwUsage
				+ ", totalServiceTime=" + totalServiceTime + ", totalProcessingTime=" + totalProcessingTime + "]";
	}
	
	
	
	
}
