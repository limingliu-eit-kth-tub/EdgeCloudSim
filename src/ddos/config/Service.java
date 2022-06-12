package ddos.config;
/*
 * ddos_note: template class created for auto-generation of application config xml file
 * IMPORTANT: be aware the different between service/applications, a service
 * 			  corresponds to a group of applications with different load/start time/end time
 */
public class Service {

	String name=null;
	
	int numApplications=0;
	int percentageNormal=100;
	int percentageDDoS=0;
	int percentagePeak=0;
	int percentageEventCrowd=0;
	
    int usagePercentage=50;
	
    int probCloudSelection=20;
	
    int delaySensitivity=0;
	
    int idlePeriod=15;
	
    int poissionIntervalRatio=5; // mutiplication ratio between normal app and attack app for poission distrition interval
    int poissionIntervalBase=10; //poission interarrival parameter for normal app
	
    int dataUploadRatio=2; 
    int dataUploadBase=1500; 
	
    int dataDownloadRatio=2; 
    int dataDownloadBase=25;
	
    int taskLengthRatio=2;
    int taskLengthBase=2000;
	
    int requiredCoreRatio=2; 
    int requiredCoreBase=2; 
	
    int vmUtilizationEdgeRatio=2; 
    int vmUtilizationEdgeBase=20; 
	
    int vmUtilizationCloudRatio=2; 
    int vmUtilizationCloudBase=2; 
	
    int vmUtilizationMobileRatio=0;
    int vmUtilizationMobileBase=0;
	
    //ddos_note: default constructor, using default app parameters
	public Service(String name,
			int numApplications, 
			int percentageNormal, 
			int percentageDDoS, 
			int percentagePeak,
			int percentageEventCrowd) {
		super();
		this.name = name;
		this.numApplications = numApplications;
		this.percentageNormal = percentageNormal;
		this.percentageDDoS = percentageDDoS;
		this.percentagePeak = percentagePeak;
		this.percentageEventCrowd = percentageEventCrowd;
	}
	
	//ddos_note: customized service constructor, using customized app parameters
	public Service(String name, int numApplications, int percentageNormal, int percentageDDoS, int percentagePeak,
			int percentageEventCrowd, int usagePercentage, int probCloudSelection, int delaySensitivity, int idlePeriod,
			int poissionIntervalRatio, int poissionIntervalBase, int dataUploadRatio, int dataUploadBase,
			int dataDownloadRatio, int dataDownloadBase, int taskLengthRatio, int taskLengthBase, int requiredCoreRatio,
			int requiredCoreBase, int vmUtilizationEdgeRatio, int vmUtilizationEdgeBase, int vmUtilizationCloudRatio,
			int vmUtilizationCloudBase, int vmUtilizationMobileRatio, int vmUtilizationMobileBase) {
		super();
		
		assert (percentageNormal+percentageDDoS+percentagePeak+percentageEventCrowd)==100:"percentages of apps does not sum to 100";
		this.name = name;
		this.numApplications = numApplications;
		this.percentageNormal = percentageNormal;
		this.percentageDDoS = percentageDDoS;
		this.percentagePeak = percentagePeak;
		this.percentageEventCrowd = percentageEventCrowd;
		this.usagePercentage = usagePercentage;
		this.probCloudSelection = probCloudSelection;
		this.delaySensitivity = delaySensitivity;
		this.idlePeriod = idlePeriod;
		this.poissionIntervalRatio = poissionIntervalRatio;
		this.poissionIntervalBase = poissionIntervalBase;
		this.dataUploadRatio = dataUploadRatio;
		this.dataUploadBase = dataUploadBase;
		this.dataDownloadRatio = dataDownloadRatio;
		this.dataDownloadBase = dataDownloadBase;
		this.taskLengthRatio = taskLengthRatio;
		this.taskLengthBase = taskLengthBase;
		this.requiredCoreRatio = requiredCoreRatio;
		this.requiredCoreBase = requiredCoreBase;
		this.vmUtilizationEdgeRatio = vmUtilizationEdgeRatio;
		this.vmUtilizationEdgeBase = vmUtilizationEdgeBase;
		this.vmUtilizationCloudRatio = vmUtilizationCloudRatio;
		this.vmUtilizationCloudBase = vmUtilizationCloudBase;
		this.vmUtilizationMobileRatio = vmUtilizationMobileRatio;
		this.vmUtilizationMobileBase = vmUtilizationMobileBase;
	}
	
	
	

}
