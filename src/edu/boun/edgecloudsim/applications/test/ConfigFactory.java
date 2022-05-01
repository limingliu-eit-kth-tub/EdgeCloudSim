package edu.boun.edgecloudsim.applications.test;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

//the factory to generate configuration xml files
public class ConfigFactory {

	private static int ATTACKER_APP_COUNT=0;
	private static int NORMAL_APP_COUNT=0;
	private static int WLAN_ID_COUNT=0;
	private static String DefaultConfigPath="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\default_config.properties";
	private static String EdgeConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\edge_devices.xml";
	
	private static String ApplicationConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications.xml";
	private static String DdosApplicationConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications_ddos.xml";
	
	private static int usagePercentage=50;
	
	private static int probCloudSelection=20;
	
	private static int delaySensitivity=0;
	
	private static int idlePeriod=15;
	
	private static int poissionIntervalRatio=5; // mutiplication ratio between normal app and attack app for poission distrition interval
	private static int poissionIntervalBase=10; //poission interarrival parameter for normal app
	
	private static int dataUploadRatio=2; 
	private static int dataUploadBase=1500; 
	
	private static int dataDownloadRatio=2; 
	private static int dataDownloadBase=25;
	
	private static int taskLengthRatio=2;
	private static int taskLengthBase=2000;
	
	private static int requiredCoreRatio=2; 
	private static int requiredCoreBase=2; 
	
	private static int vmUtilizationEdgeRatio=2; 
	private static int vmUtilizationEdgeBase=20; 
	
	private static int vmUtilizationCloudRatio=2; 
	private static int vmUtilizationCloudBase=2; 
	
	private static int vmUtilizationMobileRatio=0;
	private static int vmUtilizationMobileBase=0;
	
	public ConfigFactory() {
		// TODO Auto-generated constructor stub
	}
	
	public static void generateDefaultConfigFile() {
		//no need for now, can just modify the template
	}
	
	
	public static void generateApplicationConfigFile(int numOfIoTDevices, int percentageOfAttacker) {
		if(percentageOfAttacker<0 || percentageOfAttacker>100) {
			System.out.println("Percentage of Attacker wrong!");
			System.exit(0);
		}
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	        // root elements
	        Document doc = docBuilder.newDocument();
	        Element rootElement = doc.createElement("applications");
	        doc.appendChild(rootElement);
	        
	        for(int i=0;i<numOfIoTDevices;i++) {
	        	int randomNum = ThreadLocalRandom.current().nextInt(0, 100 + 1);
	        	boolean isAttackerApp=randomNum<percentageOfAttacker?true:false;
	        	
	        	//create xml file for attacker app
        		Element application = doc.createElement("application");
                rootElement.appendChild(application);
                
                //set name
                Attr attr = doc.createAttribute("name");
                if(isAttackerApp) {
                	attr.setValue("Attacker App "+ATTACKER_APP_COUNT++);
                }else {
                	attr.setValue("Normal App "+NORMAL_APP_COUNT++);
                }
                application.setAttributeNode(attr);
                
                //set usage percentage
                Element usage_percentage = doc.createElement("usage_percentage");
                application.appendChild(usage_percentage);
                usage_percentage.appendChild(doc.createTextNode(Integer.toString(usagePercentage)));
                
                //set prob of selecting cloud as destination
                Element prob_cloud_selection = doc.createElement("prob_cloud_selection");
                application.appendChild(prob_cloud_selection);
                prob_cloud_selection.appendChild(doc.createTextNode(Integer.toString(probCloudSelection)));
                
                //set poission interarrival parameter
                Element poisson_interarrival = doc.createElement("poisson_interarrival");
                application.appendChild(poisson_interarrival);
                if(isAttackerApp) {
                	poisson_interarrival.appendChild(doc.createTextNode(Integer.toString(poissionIntervalBase/poissionIntervalRatio)));
                }else {
                	poisson_interarrival.appendChild(doc.createTextNode(Integer.toString(poissionIntervalBase)));
                }
                
                
                //set delay sensitivity
                Element delay_sensitivity = doc.createElement("delay_sensitivity");
                application.appendChild(delay_sensitivity);
                delay_sensitivity.appendChild(doc.createTextNode(Integer.toString(delaySensitivity)));
                
                //set starting time of the application
                Element active_period = doc.createElement("active_period");
                application.appendChild(active_period);
                int randStart=ThreadLocalRandom.current().nextInt(0, MainApp.simTime + 1);
                active_period.appendChild(doc.createTextNode(Integer.toString(randStart)));
                
                //set waiting time of application before sending next task
                Element idle_period = doc.createElement("idle_period");
                application.appendChild(idle_period);
                idle_period.appendChild(doc.createTextNode(Integer.toString(idlePeriod)));
                
                //set finishing time of the app
                Element finish_period = doc.createElement("finish_period");
                application.appendChild(finish_period);
                int randFinish=ThreadLocalRandom.current().nextInt(randStart, MainApp.simTime + 1);
                finish_period.appendChild(doc.createTextNode(Integer.toString(randFinish)));
                
                //set data upload parameter of the app
                Element data_upload = doc.createElement("data_upload");
                application.appendChild(data_upload);
                if(isAttackerApp) {
                	data_upload.appendChild(doc.createTextNode(Integer.toString(dataUploadBase*dataUploadRatio)));
                }else {
                	data_upload.appendChild(doc.createTextNode(Integer.toString(dataUploadBase)));
                }
                
                //set data download parameter of the app
                Element data_download = doc.createElement("data_download");
                application.appendChild(data_download);
                if(isAttackerApp) {
                	data_download.appendChild(doc.createTextNode(Integer.toString(dataDownloadBase*dataDownloadRatio)));
                }else {
                	data_download.appendChild(doc.createTextNode(Integer.toString(dataDownloadBase)));
                }
                
                //set task length parameter of the app
                Element task_length = doc.createElement("task_length");
                application.appendChild(task_length);
                if(isAttackerApp) {
                	task_length.appendChild(doc.createTextNode(Integer.toString(taskLengthBase*taskLengthRatio)));
                }else {
                	task_length.appendChild(doc.createTextNode(Integer.toString(taskLengthBase)));
                }
                
                //set required core parameter of the app
                Element required_core = doc.createElement("required_core");
                application.appendChild(required_core);
                if(isAttackerApp) {
                	required_core.appendChild(doc.createTextNode(Integer.toString(requiredCoreBase*requiredCoreRatio)));
                }else {
                	required_core.appendChild(doc.createTextNode(Integer.toString(requiredCoreBase)));
                }
               
                //set vm utilization on edge node parameter of the app
                Element vm_utilization_on_edge = doc.createElement("vm_utilization_on_edge");
                application.appendChild(vm_utilization_on_edge);
                if(isAttackerApp) {
                	vm_utilization_on_edge.appendChild(doc.createTextNode(Integer.toString(vmUtilizationEdgeBase*vmUtilizationEdgeRatio)));
                }else {
                	vm_utilization_on_edge.appendChild(doc.createTextNode(Integer.toString(vmUtilizationEdgeBase)));
                }
                
                //set vm utilization on cloud node of the app
                Element vm_utilization_on_cloud = doc.createElement("vm_utilization_on_cloud");
                application.appendChild(vm_utilization_on_cloud);
                if(isAttackerApp) {
                	vm_utilization_on_cloud.appendChild(doc.createTextNode(Integer.toString(vmUtilizationCloudBase*vmUtilizationCloudRatio)));
                }else {
                	vm_utilization_on_cloud.appendChild(doc.createTextNode(Integer.toString(vmUtilizationCloudBase)));
                }
                
                //set vm utilization on mobile parameter of the appf
                Element vm_utilization_on_mobile = doc.createElement("vm_utilization_on_mobile");
                application.appendChild(vm_utilization_on_mobile);
                if(isAttackerApp) {
                	vm_utilization_on_mobile.appendChild(doc.createTextNode(Integer.toString(vmUtilizationMobileBase*vmUtilizationMobileRatio)));
                }else {
                	vm_utilization_on_mobile.appendChild(doc.createTextNode(Integer.toString(vmUtilizationMobileBase)));
                }
	        }
	        
	        
	        // write the content into xml file
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        DOMSource source = new DOMSource(doc);
	        StreamResult result = null;
	        result = new StreamResult(new File(DdosApplicationConfigPath));
	        
	        
	        // Output to console for testing
	        //StreamResult result = new StreamResult(System.out);
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.transform(source, result);

	        NORMAL_APP_COUNT=0;
	        ATTACKER_APP_COUNT=0;
	        System.out.println("File saved!");
		} catch (ParserConfigurationException pce) {
	        pce.printStackTrace();
	      } catch (TransformerException tfe) {
	        tfe.printStackTrace();
	      }
	}
		
	
	public static void generateEdgeConfigFile(int numEdgeServer) {
		try {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("edge_devices");
        doc.appendChild(rootElement);
        
        for(int i=0;i<numEdgeServer;i++) {
        	 // datacenter elements
            Element datacenter = doc.createElement("datacenter");
            rootElement.appendChild(datacenter);
            Attr attr = doc.createAttribute("arch");
            attr.setValue("x86");
            datacenter.setAttributeNode(attr);
            Attr attr1 = doc.createAttribute("os");
            attr1.setValue("Linux");
            datacenter.setAttributeNode(attr1);
            Attr attr2 = doc.createAttribute("vmm");
            attr2.setValue("Xen");
            datacenter.setAttributeNode(attr2);

            Element costPerBw = doc.createElement("costPerBw");
            datacenter.appendChild(costPerBw);
            costPerBw.appendChild(doc.createTextNode("0.1"));
            
            Element costPerSec = doc.createElement("costPerSec");
            datacenter.appendChild(costPerSec);
            costPerSec.appendChild(doc.createTextNode("3.0"));
            
            Element costPerMem = doc.createElement("costPerMem");
            datacenter.appendChild(costPerMem);
            costPerMem.appendChild(doc.createTextNode("0.05"));
            
            Element costPerStorage = doc.createElement("costPerStorage");
            datacenter.appendChild(costPerStorage);
            costPerStorage.appendChild(doc.createTextNode("0.05"));
            
            Element location = doc.createElement("location");
            datacenter.appendChild(location);
            
            Element x_pos = doc.createElement("x_pos");
            location.appendChild(x_pos);
            x_pos.appendChild(doc.createTextNode(Integer.toString(i)));
            
            Element y_pos = doc.createElement("y_pos");
            location.appendChild(y_pos);
            y_pos.appendChild(doc.createTextNode(Integer.toString(i)));
            
            Element wlan_id = doc.createElement("wlan_id");
            location.appendChild(wlan_id);
            wlan_id.appendChild(doc.createTextNode(Integer.toString(i)));
            
            Element attractiveness = doc.createElement("attractiveness");
            location.appendChild(attractiveness);
            attractiveness.appendChild(doc.createTextNode("1"));
            
            Element hosts = doc.createElement("hosts");
            datacenter.appendChild(hosts);
            
            
            Element host = doc.createElement("host");
            hosts.appendChild(host);
            
            Element core = doc.createElement("core");
            host.appendChild(core);
            core.appendChild(doc.createTextNode("8"));
            
            Element mips = doc.createElement("mips");
            host.appendChild(mips);
            mips.appendChild(doc.createTextNode("4000"));
            
            Element ram = doc.createElement("ram");
            host.appendChild(ram);
            ram.appendChild(doc.createTextNode("8000"));
            
            Element storage = doc.createElement("storage");
            host.appendChild(storage);
            storage.appendChild(doc.createTextNode("200000"));
            
            Element VMs = doc.createElement("VMs");
            host.appendChild(VMs);
            
            Element VM = doc.createElement("VM");
            VMs.appendChild(VM);
            Attr attr3 = doc.createAttribute("vmm");
            attr3.setValue("Xen");
            VM.setAttributeNode(attr3);
            
            Element core_vm = doc.createElement("core");
            VM.appendChild(core_vm);
            core_vm.appendChild(doc.createTextNode("8"));
            
            Element mips_vm = doc.createElement("mips");
            VM.appendChild(mips_vm);
            mips_vm.appendChild(doc.createTextNode("4000"));
            
            Element ram_vm = doc.createElement("ram");
            VM.appendChild(ram_vm);
            ram_vm.appendChild(doc.createTextNode("8000"));
            
            Element storage_vm = doc.createElement("storage");
            VM.appendChild(storage_vm);
            storage_vm.appendChild(doc.createTextNode("200000"));
        }
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        
       
        StreamResult result = new StreamResult(new File(EdgeConfigPath));

        // Output to console for testing
        //StreamResult result = new StreamResult(System.out);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        System.out.println("File saved!");

      } catch (ParserConfigurationException pce) {
        pce.printStackTrace();
      } catch (TransformerException tfe) {
        tfe.printStackTrace();
      }
		
	}

}
