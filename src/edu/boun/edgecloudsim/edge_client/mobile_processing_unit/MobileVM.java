/*
 * Title:        EdgeCloudSim - MobileVM
 * 
 * Description: 
 * MobileVM adds vm type information over CloudSim's VM class
 *
 * Please note that the mobile processing units are simulated via
 * CloudSim. It is assumed that the mobile devices operate Hosts
 * and VMs like a server. That is why the class names are similar
 * to other Cloud and Edge components (to provide consistency).
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_client.mobile_processing_unit;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

import ddos.core.DdosSimSettings;

public class MobileVM extends Vm {
	private DdosSimSettings.VM_TYPES type;
	
	public MobileVM(int id, int userId, double mips, int numberOfPes, int ram,
			long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);

		type = DdosSimSettings.VM_TYPES.MOBILE_VM;
	}

	public DdosSimSettings.VM_TYPES getVmType(){
		return type;
	}
}
