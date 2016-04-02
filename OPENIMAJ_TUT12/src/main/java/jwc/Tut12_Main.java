package jwc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.experiment.dataset.sampling.GroupedUniformRandomisedSampler;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.io.IOUtils;
import org.openimaj.image.*;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator.Mode;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.kernel.HomogeneousKernelMap;
import org.openimaj.ml.kernel.HomogeneousKernelMap.KernelType;
import org.openimaj.ml.kernel.HomogeneousKernelMap.WindowType;
import org.openimaj.util.pair.IntFloatPair;

import de.bwaldvogel.liblinear.SolverType;

/**
 * RESULTS
 * 
 * Sample size: 	10			10			10
 * Words: 			600			300			300
 * Dense step: 		5			5			3
 * Pyramid Scale: 	7			7			4
 * Aggregator: 		block		block		block
 * 
 * Accuracy: 		0.693		0.727		0.627
 *	
 *
 */
public class Tut12_Main {
	
	static File assignerLoc = new File("/Users/josephcarver/assigner");
	static File featureCache = new File("Users/josephcarver/featureCache");
	
    public static void main( String[] args ) throws Exception
    {
    	 GroupedDataset<String, VFSListDataset<Record<FImage>>, Record<FImage>> allData = Caltech101.getData(ImageUtilities.FIMAGE_READER);
    	 GroupedDataset<String, ListDataset<Record<FImage>>, Record<FImage>> data = GroupSampler.sample(allData, 10, false);
    	 
    	 //create training and test sets each with 15 items per group in data
    	 GroupedRandomSplitter<String, Record<FImage>> splits = new GroupedRandomSplitter<String, Record<FImage>>(data, 15, 0, 15);
    	 
    	 //PyramidDenseSIFT class takes a normal DenseSIFT instance and applies it to different sized windows on the regular sampling grid
    	 DenseSIFT ds = new DenseSIFT(3, 7);
    	 PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(ds, 6f, 4);
    	 
    	 
    	 //train the quantiser with a random selection of 30 images over the training set
    	 HardAssigner<byte[], float[], IntFloatPair> assigner = trainQuantiser(GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);
    	 
    	 //IOUtils.writeToFile(assigner, assignerLoc);
    	 //HardAssigner<byte[], float[], IntFloatPair> assigner = IOUtils.readFromFile(assignerLoc);
    			 
    	 //transforms data into a compact linear representation
    	 //Improves overall accuracy across classes
    	 HomogeneousKernelMap hkm = new HomogeneousKernelMap(KernelType.Chi2, WindowType.Rectangular);
    	 
    	 FeatureExtractor<DoubleFV, Record<FImage>> pe = hkm.createWrappedExtractor(new PHOWExtractor(pdsift, assigner));
    	 
    	 DiskCachingFeatureExtractor<DoubleFV, Record<FImage>> toDisk = new DiskCachingFeatureExtractor<DoubleFV, Record<FImage>>(featureCache, pe);
    	 
    	//Construct and train a classifier
    	 System.out.println("Constructing & Training classifier");
    	 LiblinearAnnotator<Record<FImage>, String> ann = new LiblinearAnnotator<Record<FImage>, String>(pe, Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);
    	 ann.train(splits.getTrainingDataset());
    	 System.out.println("Done");
    	 
    	 //Use OPENIMAJ evaluation framework to perform an automated evaluation of our classifier so far
    	 System.out.println("Evaluating Classifier");
    	 ClassificationEvaluator<CMResult<String>, String, Record<FImage>> eval = new ClassificationEvaluator<CMResult<String>, String, Record<FImage>>(
    			    ann, splits.getTestDataset(), new CMAnalyser<Record<FImage>, String>(CMAnalyser.Strategy.SINGLE));
    	 
    	 Map<Record<FImage>, ClassificationResult<String>> guesses = eval.evaluate();
    	 CMResult<String> result = eval.analyse(guesses);
    	 System.out.print(result.getDetailReport());
    }
    
    //extracts the first 10000 dense SIFT features from the images in the dataset, and then clusters them into 300 separate classes
    //returns a hard-assigner that can assign SIFT features to identifiers
    static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(Dataset<Record<FImage>> sample, PyramidDenseSIFT<FImage> pdsift)
    {
    	System.out.println("Training Quantiser");
    	List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<LocalFeatureList<ByteDSIFTKeypoint>>();
    	
    	//extract dense sift features from images in datasete
    	for (Record<FImage> rec : sample) {
            FImage img = rec.getImage();
            pdsift.analyseImage(img);
            allkeys.add(pdsift.getByteKeypoints(0.005f));
        }
    	
    	//keep only first 10,000 features
    	if (allkeys.size() > 10000)
            allkeys = allkeys.subList(0, 10000);

    	//cluster features into separate classes
    	//each of these represents a visual word
    	ByteKMeans km = ByteKMeans.createKDTreeEnsemble(300);
        DataSource<byte[]> datasource = new LocalFeatureListDataSource<ByteDSIFTKeypoint, byte[]>(allkeys);
        ByteCentroidsResult result = km.cluster(datasource);
        System.out.println("DONE");
        return result.defaultHardAssigner();    
    }
    
    static class PHOWExtractor implements FeatureExtractor<DoubleFV, Record<FImage>> {
        PyramidDenseSIFT<FImage> pdsift;
        HardAssigner<byte[], float[], IntFloatPair> assigner;
        public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift, HardAssigner<byte[], float[], IntFloatPair> assigner){
            this.pdsift = pdsift;
            this.assigner = assigner;
        }
        public DoubleFV extractFeature(Record<FImage> object) {
            FImage image = object.getImage();
            pdsift.analyseImage(image);
            
            //uses the HardAssigner to assign each Dense SIFT feature to a visual word and the compute the histogram
            BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);
            //Compute 4 histograms across the image, 2 vertically, 2 horizontally
            BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(
                    bovw, 2, 2);
            //Spatial histograms are appended together and normalised before being returned
            return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
        }
  }
}
