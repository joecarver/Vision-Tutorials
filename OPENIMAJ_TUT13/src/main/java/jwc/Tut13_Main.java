package jwc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.dataset.util.DatasetAdaptors;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.model.EigenImages;


/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut13_Main 
{
	private GroupedDataset<String, ListDataset<FImage>, FImage> training, testing;
	private int nTraining, nTesting;
	private EigenImages eigen;
	
	//Map <person, features> features=all features of all training instances of person
	private HashMap<String, DoubleFV[]> features = new HashMap<String, DoubleFV[]>();;
	
	
    public static void main( String[] args ) throws Exception
    {
    	Tut13_Main m = new Tut13_Main();
    	m.init();
    
    	m.estimateIdentities();
    	//m.reconstructRandomFace();
    }

    public void init() throws Exception
    {
    	VFSGroupDataset<FImage> dataset = new VFSGroupDataset<FImage>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);
    	
    	//reducing size of training set significantly reduces amount of correct results
    	//incorrect stays about the same as threshold eliminates unsure guesses
    	//n-1 -> ~20 correct
    	//n=2 -> ~40 correct
    	//n=3 -> ~70 correct
    	//n=4 -> ~115 correct
    	//n=5 -> ~170 correct
    	nTraining = 1;
    	nTesting = 5;
    	
    	GroupedRandomSplitter<String, FImage> splits = new GroupedRandomSplitter<String, FImage>(dataset, nTesting, 0, nTraining);
    	training = splits.getTrainingDataset();
    	testing = splits.getTestDataset();
    	
    	this.learnBasisVectors();
    	this.storeFeatures();
    }
    
    //learn the PCA basis which we can use to project the images into features used for recognition
    public void learnBasisVectors()
    {
    	System.out.println("Learning Basis Vectors");
    	List<FImage> basisImages = DatasetAdaptors.asList(training); //list of images from which to learn the basis
    	int nEigenVectors = 100; //how many dimensions for a feature
    	eigen = new EigenImages(nEigenVectors);
    	eigen.train(basisImages);
    	
    	List<FImage> eigenFaces = new ArrayList<FImage>();
    	for(int i=0; i<12; i++){
    		eigenFaces.add(eigen.visualisePC(i));
    	}
    	//DisplayUtilities.display("EigenFaces", eigenFaces);
    	System.out.println("Done");
    }
    
    public void storeFeatures(){
    	//Map <person, features> features=all features of all training instances of person
    	System.out.println("Storing all feature vectors");
    	for(final String person : training.getGroups())
    	{
    		final DoubleFV[] fvs = new DoubleFV[nTraining];
    		
    		for(int i=0; i<nTraining; i++){
    			final FImage face = training.get(person).get(i);
    			fvs[i] = eigen.extractFeature(face);
    		}
    		features.put(person, fvs);
    	}
    	System.out.println("Done");
    }
  
    public void reconstructRandomFace()
    {   
    	//holds names of all people
    	List<String> people = new ArrayList<String>(training.getGroups());
    	
    	//random int between 1:nPeople used to get a random person's id
    	String personName = people.get((int)Math.ceil(Math.random()*people.size()));
    	
    	//display a random photo of them alongside
    	FImage actual = training.getRandomInstance(personName);
    	ArrayList<FImage> toDisplay = new ArrayList<FImage>();
    	toDisplay.add(actual);
    	
    	System.out.println("Reconstructing " + personName);

    	//array of weight vectors that can be used to reconstruct
    	DoubleFV[] allFeatures = features.get(personName);
    	
    	FImage recons=null;
    	for(int i=0; i<allFeatures.length; i++)
    	{
    		recons = eigen.reconstruct(allFeatures[i]).normalise();
    	}
    	
    	toDisplay.add(recons);
    	DisplayUtilities.display(personName, toDisplay);
    	
    	System.out.println("Done");
    }
    
    //extract feature from tested image, find database feature with closest euclidean distance
    //Threshold of 10 usually eliminates most incorrect guesses
    public void estimateIdentities()
    {
    	int correct = 0, incorrect = 0;
    	double threshold = 10;
    	for(String truePerson : testing.getGroups()){
    		for(FImage face : testing.get(truePerson)){
    			DoubleFV testFeature = eigen.extractFeature(face);
    			
    			String bestPerson = null;
    			double minDistance = Double.MAX_VALUE;
    			
    			for(final String person : features.keySet()){
    				for(final DoubleFV f : features.get(person)){
    					
    					double distance = f.compare(testFeature, DoubleFVComparison.EUCLIDEAN);
    					
    					if(distance < minDistance){
    						minDistance = distance;
    						bestPerson = person;
    					}
    				}
    			}
    			if(minDistance > threshold){
    				bestPerson = "unknown";
    			} else { //ignore unknown results when calculating accuracy
    				if(truePerson.equals(bestPerson)){
	    				correct++;
	    			}
	    			else{
	    				incorrect++;
	    			}
    			}
				System.out.println("Actual: " + truePerson + "\tguess: " + bestPerson);
    			
    		}
    	}
    	System.out.println("Correct: " + correct + ", Incorrect: " + incorrect);
    }
}
