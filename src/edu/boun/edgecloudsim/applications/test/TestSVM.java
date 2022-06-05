package edu.boun.edgecloudsim.applications.test;

import java.io.File;

import libsvm.LibSVM;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;

public class TestSVM {

	public TestSVM() {
		// TODO Auto-generated constructor stub
	}
	private static String datsetAppCSV="D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\data_all.csv";

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		Dataset data = FileHandler.loadDataset(new File(datsetAppCSV), 4, ",");
        /*
         * Contruct a LibSVM classifier with default settings.
         */
        Classifier svm = new LibSVM();
        svm.buildClassifier(data);

        /*
         * Load a data set, this can be a different one, but we will use the
         * same one.
         */
        Dataset dataForClassification = FileHandler.loadDataset(new File(datsetAppCSV), 4, ",");
        
       // Object predictedClassValue = svm.classify(new DenseInstance(new double[] {
        		//0.099913	,0.089711	,1.464518	,1.450432},"NULL"));
        /* Counters for correct and wrong predictions. */
        int correct = 0, wrong = 0;
        /* Classify all instances and check with the correct class values */
        for (Instance inst : dataForClassification) {
            Object predictedClassValue = svm.classify(inst);
            Object realClassValue = inst.classValue();
            if (predictedClassValue.equals(realClassValue))
                correct++;
            else
               wrong++;
       }
       System.out.println("Correct predictions  " + correct);
       System.out.println("Wrong predictions " + wrong);
        
//       System.out.println(predictedClassValue);
	}

}
