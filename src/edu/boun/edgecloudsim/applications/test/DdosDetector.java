package edu.boun.edgecloudsim.applications.test;

import java.io.File;
import java.io.IOException;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;
import net.sf.javaml.tools.DatasetTools;
import net.sf.javaml.tools.data.FileHandler;

public class DdosDetector{
	

	public enum algorithm{
		KMEANS,NN
	}
	
	private static String datsetNetworkCSV="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\data_all.csv";
	private static String datsetAppCSV="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log_APP\\data_all.csv";
	private static int KMEANS_CLUSTER=11;
	private static int KMEANS_ITERATION=100;
	private static int NETWORK_DATASET_LABEL_COLUMN=2;
	private static int APP_DATASET_LABEL_COLUMN=1;
	
	private static DistanceMeasure dm=new EuclideanDistance();
	
	public DdosDetector() {
		//do nothing
	}

	
	public static String KMeansNetwork(double failureRate, double delay) throws IOException{
		Dataset data = FileHandler.loadDataset(new File(datsetNetworkCSV), NETWORK_DATASET_LABEL_COLUMN, ",");
//		data.remove(0);//remove header
		/*
		 * Create a new instance of the KMeans algorithm, with no options
		 * specified. By default this will generate 4 clusters.
		 */
		Clusterer km = new KMeans(KMEANS_CLUSTER,KMEANS_ITERATION);
		/*
		 * Cluster the data, it will be returned as an array of data sets, with
		 * each dataset representing a cluster
		 */
		Dataset[] clusters = km.cluster(data);
		
		Instance[] centroids=new Instance[KMEANS_CLUSTER];
		
		int belongToCluster=-1;
		
		Instance currentMetic=new DenseInstance(new double[] {failureRate,delay},"NULL");
		
		double minDistance = Double.MAX_VALUE;
		for(int i=0;i<KMEANS_CLUSTER;i++) {
			centroids[i]=DatasetTools.average(clusters[i]);
			double dist = dm.measure(centroids[i], currentMetic);
            if (dm.compare(dist, minDistance)) {
                minDistance = dist;
                belongToCluster = i;
            }
		}
		String label=(String)clusters[belongToCluster].get(0).classValue();
		return label;
	}
	
	public static boolean KMeansApp(double freq) throws IOException{
		Dataset data = FileHandler.loadDataset(new File(datsetAppCSV), APP_DATASET_LABEL_COLUMN, ",");
		data.remove(0);//remove header
		/*
		 * Create a new instance of the KMeans algorithm, with no options
		 * specified. By default this will generate 4 clusters.
		 */
		Clusterer km = new KMeans(KMEANS_CLUSTER,KMEANS_ITERATION);
		/*
		 * Cluster the data, it will be returned as an array of data sets, with
		 * each dataset representing a cluster
		 */
		Dataset[] clusters = km.cluster(data);
		
		Instance[] centroids=new Instance[KMEANS_CLUSTER];
		
		int belongToCluster=-1;
		
		Instance currentMetic=new DenseInstance(new double[] {freq},"NULL");
		
		double minDistance = Double.MAX_VALUE;
		for(int i=0;i<KMEANS_CLUSTER;i++) {
			centroids[i]=DatasetTools.average(clusters[i]);
			double dist = dm.measure(centroids[i], currentMetic);
            if (dm.compare(dist, minDistance)) {
                minDistance = dist;
                belongToCluster = i;
            }
		}
		String label=(String)clusters[belongToCluster].get(0).classValue();
		return label.equals("TRUE")? true:false;
	}
	
	
	
	public static String detectDDoSAttack(double failureRate, double delay, algorithm algo) throws IOException {
		if(algo==algorithm.KMEANS) {
			return KMeansNetwork(failureRate,delay);
		}
	
		return null;
	}
	
	
	
	public static boolean detectMaliciousApp(double freq, algorithm algo) throws IOException{
		if(algo==algorithm.KMEANS) {
			return KMeansApp(freq);
		}
		return true;
	}

}
