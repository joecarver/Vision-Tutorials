package jwc;

import java.net.URL;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.BasicMatcher;
import org.openimaj.feature.local.matcher.BasicTwoWayMatcher;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.MatchingUtilities;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut5_Main
{
	static MBFImage query, target;
	LocalFeatureMatcher<Keypoint> matcher;
	
	DoGSIFTEngine engine;
	LocalFeatureList<Keypoint> queryKeyPoints, targetKeyPoints;
	
    public static void main( String[] args ) throws Exception
    {
    	 query = ImageUtilities.readMBF(new URL("https://dl.dropboxusercontent.com/u/8705593/query.jpg"));
    	 target = ImageUtilities.readMBF(new URL("https://dl.dropboxusercontent.com/u/8705593/target.jpg"));
    	 
    	 Tut5_Main m = new Tut5_Main();
    	 
    	 m.init();
    	// m.matchBasic();
    	m.matchConsistent();
    	// m.matchTwoWay();
    }
    
    public void init(){
    	 //Difference-of-Gaussian feature detector, described with a SIFT descriptor 
    	 DoGSIFTEngine engine = new DoGSIFTEngine();
    	 
    	 //Keypoint class contains a public field 'ivec' - in SIFT is a 128-dimensional description of a patch of pixels round a point
    	 //Can compare keypoints with eachother 
    	 queryKeyPoints = engine.findFeatures(query.flatten());
    	 targetKeyPoints = engine.findFeatures(target.flatten());
    }
    
    //Finds many matches, many of which are incorrect
    public void matchBasic()
    {
    	//Match most similar keypoints between each image
    	 matcher = new BasicMatcher<Keypoint>(80);
    	 matcher.setModelFeatures(queryKeyPoints);
    	 matcher.findMatches(targetKeyPoints);
    	 
    	 MBFImage basicMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
    	 DisplayUtilities.display(basicMatches);
    }
    
    //Better to filter matches based on a given geometric model
    public void matchConsistent()
    {
    	//RANSAC model fitter configured to find Affine Transforms
    	 RobustAffineTransformEstimator affineModelFitter = new RobustAffineTransformEstimator(5.0, 1500,
    			  new RANSAC.PercentageInliersStoppingCondition(0.5));
    	 
    	 //THIS WORKS BEST!
    	 //TODO - LMedS?
    	 RobustHomographyEstimator homographyModelFitter = new RobustHomographyEstimator(5.0, 1500, 
    			 new RANSAC.PercentageInliersStoppingCondition(0.5), HomographyRefinement.SYMMETRIC_TRANSFER);
    	 
    	 //ConsistentLocalFeatureMatcher takes a model and matcher, finds which matches are consistent to the model and most likely to be correct
    	 matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(new FastBasicKeypointMatcher<Keypoint>(8), homographyModelFitter);	 
    	 matcher.setModelFeatures(queryKeyPoints);
    	 matcher.findMatches(targetKeyPoints);
    	 
    	 MBFImage consistentMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
    	 DisplayUtilities.display(consistentMatches, "Matches");
    	 

    	 //The AffineTransformModel returned by getModel() contains the best transform matrix to go from the query to the target
    	 //Transform the bounding box of our query with the transform estimated in the AffineTransformModel
    	 target.drawShape(query.getBounds().transform(homographyModelFitter.getModel().getTransform().inverse()), 3, RGBColour.BLUE);
    	 DisplayUtilities.display(target, "Target Area");
    }
    
    //more matchers here http://www.openimaj.org/apidocs/org/openimaj/feature/local/matcher/LocalFeatureMatcher.html
    public void matchTwoWay(){
    	matcher = new BasicTwoWayMatcher<Keypoint>();
    	matcher.setModelFeatures(queryKeyPoints);
    	matcher.findMatches(targetKeyPoints);
    	
    	MBFImage twoWayMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.GREEN);
    	DisplayUtilities.display(twoWayMatches);
    }

}
