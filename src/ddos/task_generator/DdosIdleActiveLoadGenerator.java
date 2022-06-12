/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ddos.task_generator;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import ddos.core.DdosSimSettings;
import ddos.util.DdosSimLogger;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class DdosIdleActiveLoadGenerator extends LoadGeneratorModel{
	int taskTypeOfDevices[];
	public DdosIdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();
		
		//exponential number generator for file input size, file output size and task length
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[DdosSimSettings.getInstance().getTaskLookUpTable().length][3];
		
		//create random number generator for each place
		for(int i=0; i<DdosSimSettings.getInstance().getTaskLookUpTable().length; i++) {
			if(DdosSimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			
			expRngList[i][0] = new ExponentialDistribution(DdosSimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][1] = new ExponentialDistribution(DdosSimSettings.getInstance().getTaskLookUpTable()[i][7]);
			expRngList[i][2] = new ExponentialDistribution(DdosSimSettings.getInstance().getTaskLookUpTable()[i][8]);
		}
		
		//Each mobile device utilizes an app type (task type)
		taskTypeOfDevices = new int[numberOfMobileDevices];
		for(int i=0; i<numberOfMobileDevices; i++) {
			
			int randomTaskType=i%DdosSimSettings.getInstance().getTaskLookUpTable().length;
			double poissonMean = DdosSimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
			double activePeriod = DdosSimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
			double idlePeriod = DdosSimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
			double finishPeriod = DdosSimSettings.getInstance().getTaskLookUpTable()[randomTaskType][14];
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					DdosSimSettings.CLIENT_ACTIVITY_START_TIME, 
					DdosSimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;
			
			
			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			
			//ddos_note: generate tasks by poission distribution between starting period and finish period of the application
			while(virtualTime < finishPeriod) {
				double interval = rng.sample();

				if(interval <= 0){
					DdosSimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
					continue;
				}
				//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
				virtualTime += interval;
				
				if(virtualTime > activePeriodStartTime + activePeriod){
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					
					continue;
				}
				taskList.add(new TaskProperty(i,randomTaskType, virtualTime, expRngList));
			}
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}
