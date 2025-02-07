package ddos.config;

import java.io.File;
import java.util.ArrayList;
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

import ddos.MainApp;

//ddos_note: the factory to generate configuration xml files
public class ConfigurationFileFactory {
	
	private static int ATTACKER_APP_COUNT=0;
	private static int NORMAL_APP_COUNT=0;
	private static int PEAK_APP_COUNT=0;
	private static int EVENT_CROWD_APP_COUNT=0;
	
	/*
	 * service registry used for auto-generation of application config file
	 *  
	 */
	public ArrayList<Service> serviceRegistry=new ArrayList<Service>();
	
	public static ConfigurationFileFactory instance=null;
	
	public static ConfigurationFileFactory getInstance() {
		if(instance==null) {
			instance= new ConfigurationFileFactory();
			return instance;
		}else {
			return instance;
		}
	}
	
	public static void generateGlobalPropertiesConfigFile() {
		//no need for now, can just modify the template
	}
	
	public void addNewService(Service s) {
		serviceRegistry.add(s);
	}
	
	public void generateApplicationConfigFile () throws Exception {
		
		//check if the number of applications is equal to number of devices
		int totalNumApplications=0;
		for(Service s: serviceRegistry) {
			totalNumApplications+=s.numApplications;
		}
		
		assert(totalNumApplications==MainApp.numMobileDevices):"Number of applications not equal to number of mobile devices! This prohibits 1-to-1 mapping between application and device, be careful!";
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("applications");
        doc.appendChild(rootElement);
        
        //generate applications profile for each service
        for(Service s: serviceRegistry) {        	 
	        for(int i=0;i<s.numApplications;i++) {
	        	AppType appType=null;
	        	int randomNum = ThreadLocalRandom.current().nextInt(0, 100 );
	        	
	        	if(randomNum<s.percentageNormal) {
	        		appType=AppType.Normal;
	        	}else {
	        		if(randomNum<s.percentageNormal+s.percentageDDoS) {
	        			appType=AppType.DDoS;
	        		}else {
	        			if(randomNum<s.percentageNormal+s.percentageDDoS+s.percentagePeak) {
	        				appType=AppType.Peak;
	        			}else {
	        				appType=AppType.EventCrowd;
	        			}
	        		}
	        	}
	        	if(appType==null) {
	        		throw new Exception("App Type not assigned!");
	        	}
	        	generateSingleApplicationConfig(doc, rootElement, appType, s);
	        }
	        
	        //reset the counters	        
	        NORMAL_APP_COUNT=0;
	        ATTACKER_APP_COUNT=0;
	        PEAK_APP_COUNT=0;
	        EVENT_CROWD_APP_COUNT=0;    	        
        }
        
        try {
	        // write the content into xml file
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        DOMSource source = new DOMSource(doc);
	        StreamResult result = null;
	        result = new StreamResult(new File(MainApp.DdosApplicationConfigPath));
	        	        
	        // Output to console for testing
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.transform(source, result);
	
	      
		} catch (TransformerException tfe) {
	        tfe.printStackTrace();
	      }
		}
	
	public void generateSingleApplicationConfig(Document doc, Element rootElement, AppType appType, Service s ) throws Exception{

    	//create xml file for attacker app
		Element application = doc.createElement("application");
        rootElement.appendChild(application);
        
        //set name
        Attr attr = doc.createAttribute("name");
        if(appType==AppType.DDoS) {
        	attr.setValue(s.name+" Attacker App "+ATTACKER_APP_COUNT++);
        }
        else if (appType==AppType.Normal) {
        	attr.setValue(s.name+" Normal App "+NORMAL_APP_COUNT++);
        }
        else if (appType==AppType.Peak) {
        	attr.setValue(s.name+" Peak App "+PEAK_APP_COUNT++);
        }
        else if (appType==AppType.EventCrowd) {
        	attr.setValue(s.name+" Event Crowd App "+EVENT_CROWD_APP_COUNT++);
        }else {
        	throw new Exception("Unknown app type");
        }
        application.setAttributeNode(attr);
        
        //set usage percentage
        Element usage_percentage = doc.createElement("usage_percentage");
        application.appendChild(usage_percentage);
        usage_percentage.appendChild(doc.createTextNode(Integer.toString(s.usagePercentage)));
        
        //set prob of selecting cloud as destination
        Element prob_cloud_selection = doc.createElement("prob_cloud_selection");
        application.appendChild(prob_cloud_selection);
        prob_cloud_selection.appendChild(doc.createTextNode(Integer.toString(s.probCloudSelection)));
        
        //set poission interarrival parameter
        Element poisson_interarrival = doc.createElement("poisson_interarrival");
        application.appendChild(poisson_interarrival);
        if(appType==AppType.DDoS) {
        	poisson_interarrival.appendChild(doc.createTextNode(Integer.toString(s.poissionIntervalBase/s.poissionIntervalRatio)));
        }else {
        	poisson_interarrival.appendChild(doc.createTextNode(Integer.toString(s.poissionIntervalBase)));
        }
        
        //set delay sensitivity
        Element delay_sensitivity = doc.createElement("delay_sensitivity");
        application.appendChild(delay_sensitivity);
        delay_sensitivity.appendChild(doc.createTextNode(Integer.toString(s.delaySensitivity)));
        
        //set starting time of the application
        Element active_period = doc.createElement("active_period");
        application.appendChild(active_period);
        int randStart=0;
        if(appType==AppType.Peak) {
        	randStart=ThreadLocalRandom.current().nextInt((int)MainApp.PeakTimeStart + 1, (int)MainApp.PeakTimeEnd + 1);
            active_period.appendChild(doc.createTextNode(Integer.toString(randStart)));
        }else {
        	randStart=ThreadLocalRandom.current().nextInt(0, MainApp.simTime + 1);
            active_period.appendChild(doc.createTextNode(Integer.toString(randStart)));
        }
        
        //set waiting time of application before sending next task
        Element idle_period = doc.createElement("idle_period");
        application.appendChild(idle_period);
        idle_period.appendChild(doc.createTextNode(Integer.toString(s.idlePeriod)));
        
        //set finishing time of the app
        Element finish_period = doc.createElement("finish_period");
        application.appendChild(finish_period);
        if(appType==AppType.Peak) {
        	int randFinish=ThreadLocalRandom.current().nextInt(randStart, (int) MainApp.PeakTimeEnd + 1);
            finish_period.appendChild(doc.createTextNode(Integer.toString(randFinish)));
        }else {
        	int randFinish=ThreadLocalRandom.current().nextInt(randStart, MainApp.simTime + 1);
            finish_period.appendChild(doc.createTextNode(Integer.toString(randFinish)));
        }    
    
        //set data upload parameter of the app
        Element data_upload = doc.createElement("data_upload");
        application.appendChild(data_upload);
        if(appType==AppType.DDoS) {
        	data_upload.appendChild(doc.createTextNode(Integer.toString(s.dataUploadBase*s.dataUploadRatio)));
        }else {
        	data_upload.appendChild(doc.createTextNode(Integer.toString(s.dataUploadBase)));
        }
        
        //set data download parameter of the app
        Element data_download = doc.createElement("data_download");
        application.appendChild(data_download);
        if(appType==AppType.DDoS) {
        	data_download.appendChild(doc.createTextNode(Integer.toString(s.dataDownloadBase*s.dataDownloadRatio)));
        }else {
        	data_download.appendChild(doc.createTextNode(Integer.toString(s.dataDownloadBase)));
        }
        
        //set task length parameter of the app
        Element task_length = doc.createElement("task_length");
        application.appendChild(task_length);
        if(appType==AppType.DDoS) {
        	task_length.appendChild(doc.createTextNode(Integer.toString(s.taskLengthBase*s.taskLengthRatio)));
        }else {
        	task_length.appendChild(doc.createTextNode(Integer.toString(s.taskLengthBase)));
        }
        
        //set required core parameter of the app
        Element required_core = doc.createElement("required_core");
        application.appendChild(required_core);
        if(appType==AppType.DDoS) {
        	required_core.appendChild(doc.createTextNode(Integer.toString(s.requiredCoreBase*s.requiredCoreRatio)));
        }else {
        	required_core.appendChild(doc.createTextNode(Integer.toString(s.requiredCoreBase)));
        }
       
        //set vm utilization on edge node parameter of the app
        Element vm_utilization_on_edge = doc.createElement("vm_utilization_on_edge");
        application.appendChild(vm_utilization_on_edge);
        if(appType==AppType.DDoS) {
        	vm_utilization_on_edge.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationEdgeBase*s.vmUtilizationEdgeRatio)));
        }else {
        	vm_utilization_on_edge.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationEdgeBase)));
        }
        
        //set vm utilization on cloud node of the app
        Element vm_utilization_on_cloud = doc.createElement("vm_utilization_on_cloud");
        application.appendChild(vm_utilization_on_cloud);
        if(appType==AppType.DDoS) {
        	vm_utilization_on_cloud.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationCloudBase*s.vmUtilizationCloudRatio)));
        }else {
        	vm_utilization_on_cloud.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationCloudBase)));
        }
        
        //set vm utilization on mobile parameter of the appf
        Element vm_utilization_on_mobile = doc.createElement("vm_utilization_on_mobile");
        application.appendChild(vm_utilization_on_mobile);
        if(appType==AppType.DDoS) {
        	vm_utilization_on_mobile.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationMobileBase*s.vmUtilizationMobileRatio)));
        }else {
        	vm_utilization_on_mobile.appendChild(doc.createTextNode(Integer.toString(s.vmUtilizationMobileBase)));
        }
	}
		
	public void generateEdgeConfigFile(int numEdgeServer) {
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
        
        StreamResult result = new StreamResult(new File(MainApp.EdgeConfigPath));
        // Output to console for testing
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);

      } catch (ParserConfigurationException pce) {
        pce.printStackTrace();
      } catch (TransformerException tfe) {
        tfe.printStackTrace();
      }
		
	}
	
	
}


