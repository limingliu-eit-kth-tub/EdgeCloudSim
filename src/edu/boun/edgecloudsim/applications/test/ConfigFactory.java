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
	private static String DefaultConfigPath="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\default_config.properties";
	private static String EdgeConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\edge_devices1.xml";
	
	private static String ApplicationConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications1.xml";
	private static String DdosApplicationConfigPath= "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications_ddos.xml";
	
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
	        	
	        	if(randomNum<percentageOfAttacker) {
	        		//create xml file for attacker app
	        		Element application = doc.createElement("application");
	                rootElement.appendChild(application);
	                Attr attr = doc.createAttribute("name");
	                attr.setValue("Attacker App "+ATTACKER_APP_COUNT++);
	                application.setAttributeNode(attr);
	                
	                Element usage_percentage = doc.createElement("usage_percentage");
	                application.appendChild(usage_percentage);
	                usage_percentage.appendChild(doc.createTextNode("100"));
	                
	                Element prob_cloud_selection = doc.createElement("prob_cloud_selection");
	                application.appendChild(prob_cloud_selection);
	                prob_cloud_selection.appendChild(doc.createTextNode("20"));
	                
	                Element poisson_interarrival = doc.createElement("poisson_interarrival");
	                application.appendChild(poisson_interarrival);
	                poisson_interarrival.appendChild(doc.createTextNode("20"));
	                
	                Element delay_sensitivity = doc.createElement("delay_sensitivity");
	                application.appendChild(delay_sensitivity);
	                delay_sensitivity.appendChild(doc.createTextNode("0"));
	                
	                Element active_period = doc.createElement("active_period");
	                application.appendChild(active_period);
	                active_period.appendChild(doc.createTextNode("45"));
	                
	                Element idle_period = doc.createElement("idle_period");
	                application.appendChild(idle_period);
	                idle_period.appendChild(doc.createTextNode("15"));
	                
	                Element finish_period = doc.createElement("finish_period");
	                application.appendChild(finish_period);
	                finish_period.appendChild(doc.createTextNode("6000"));
	                
	                Element data_upload = doc.createElement("data_upload");
	                application.appendChild(data_upload);
	                data_upload.appendChild(doc.createTextNode("1500"));
	                
	                Element data_download = doc.createElement("data_download");
	                application.appendChild(usage_percentage);
	                data_download.appendChild(doc.createTextNode("25"));
	                
	                Element task_length = doc.createElement("task_length");
	                application.appendChild(task_length);
	                task_length.appendChild(doc.createTextNode("2000"));
	                
	                Element required_core = doc.createElement("required_core");
	                application.appendChild(required_core);
	                required_core.appendChild(doc.createTextNode("2"));
	                
	                Element vm_utilization_on_edge = doc.createElement("vm_utilization_on_edge");
	                application.appendChild(vm_utilization_on_edge);
	                vm_utilization_on_edge.appendChild(doc.createTextNode("20"));
	                
	                Element vm_utilization_on_cloud = doc.createElement("vm_utilization_on_cloud");
	                application.appendChild(vm_utilization_on_cloud);
	                vm_utilization_on_cloud.appendChild(doc.createTextNode("2"));
	                
	                Element vm_utilization_on_mobile = doc.createElement("vm_utilization_on_mobile");
	                application.appendChild(vm_utilization_on_mobile);
	                vm_utilization_on_mobile.appendChild(doc.createTextNode("0"));
	                
	        	}else {
	        		//create xml file for attacker app
	        		Element application = doc.createElement("application");
	                rootElement.appendChild(application);
	                Attr attr = doc.createAttribute("name");
	                attr.setValue("Normal App "+NORMAL_APP_COUNT++);
	                application.setAttributeNode(attr);
	                
	                Element usage_percentage = doc.createElement("usage_percentage");
	                application.appendChild(usage_percentage);
	                usage_percentage.appendChild(doc.createTextNode("100"));
	                
	                Element prob_cloud_selection = doc.createElement("prob_cloud_selection");
	                application.appendChild(prob_cloud_selection);
	                prob_cloud_selection.appendChild(doc.createTextNode("20"));
	                
	                Element poisson_interarrival = doc.createElement("poisson_interarrival");
	                application.appendChild(poisson_interarrival);
	                poisson_interarrival.appendChild(doc.createTextNode("20"));
	                
	                Element delay_sensitivity = doc.createElement("delay_sensitivity");
	                application.appendChild(delay_sensitivity);
	                delay_sensitivity.appendChild(doc.createTextNode("0"));
	                
	                Element active_period = doc.createElement("active_period");
	                application.appendChild(active_period);
	                active_period.appendChild(doc.createTextNode("45"));
	                
	                Element idle_period = doc.createElement("idle_period");
	                application.appendChild(idle_period);
	                idle_period.appendChild(doc.createTextNode("15"));
	                
	                Element finish_period = doc.createElement("finish_period");
	                application.appendChild(finish_period);
	                finish_period.appendChild(doc.createTextNode("6000"));
	                
	                Element data_upload = doc.createElement("data_upload");
	                application.appendChild(data_upload);
	                data_upload.appendChild(doc.createTextNode("1500"));
	                
	                Element data_download = doc.createElement("data_download");
	                application.appendChild(usage_percentage);
	                data_download.appendChild(doc.createTextNode("25"));
	                
	                Element task_length = doc.createElement("task_length");
	                application.appendChild(task_length);
	                task_length.appendChild(doc.createTextNode("2000"));
	                
	                Element required_core = doc.createElement("required_core");
	                application.appendChild(required_core);
	                required_core.appendChild(doc.createTextNode("2"));
	                
	                Element vm_utilization_on_edge = doc.createElement("vm_utilization_on_edge");
	                application.appendChild(vm_utilization_on_edge);
	                vm_utilization_on_edge.appendChild(doc.createTextNode("20"));
	                
	                Element vm_utilization_on_cloud = doc.createElement("vm_utilization_on_cloud");
	                application.appendChild(vm_utilization_on_cloud);
	                vm_utilization_on_cloud.appendChild(doc.createTextNode("2"));
	                
	                Element vm_utilization_on_mobile = doc.createElement("vm_utilization_on_mobile");
	                application.appendChild(vm_utilization_on_mobile);
	                vm_utilization_on_mobile.appendChild(doc.createTextNode("0"));
	        	}

	        }
	        
	        
	        // write the content into xml file
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        DOMSource source = new DOMSource(doc);
	        StreamResult result = new StreamResult(new File(ApplicationConfigPath));

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
            wlan_id.appendChild(doc.createTextNode("0"));
            
            Element attractiveness = doc.createElement("attractiveness");
            location.appendChild(attractiveness);
            attractiveness.appendChild(doc.createTextNode("0"));
            
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
