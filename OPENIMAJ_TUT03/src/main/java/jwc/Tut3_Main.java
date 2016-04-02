package jwc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.connectedcomponent.GreyscaleConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.segmentation.FelzenszwalbHuttenlocherSegmenter;
import org.openimaj.image.segmentation.SegmentationUtilities;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;



public class Tut3_Main 
{
	
    public static void main( String[] args ) throws MalformedURLException, IOException 
    {
    	MBFImage festival = ImageUtilities.readMBF(new URL("http://www.residentadvisor.net/images/features/2015/top10-novdec.jpg"));
    	DisplayUtilities.display(festival);
    	
    	//MBFImage clustered = clusterClassifyImage(festival);
    	//labelImage(clustered);
    	labelFHS(festival);
    }
    
    public static MBFImage clusterClassifyImage(MBFImage image)
    {
    	//represent image in LAB Colour Space as this more accurately models difference in human perception
    	image = ColourSpace.convert(image, ColourSpace.CIE_Lab);
    	
    	//construct K-Means algorithm, parameter is number of clusters/classes to create
    	FloatKMeans cluster = FloatKMeans.createExact(2);
    	
    	//flatten image into array of floating point vectors for algorithm
    	float[][] imgPts = image.getPixelVectorNative(new float[image.getWidth() * image.getHeight()][3]);
    	
    	//group pixels into requested number of classes
    	FloatCentroidsResult result = cluster.cluster(imgPts);
    	
    	//average position of all points belonging to the cluster
    	final float[][] centroids = result.centroids;
    	for(float[] fs : centroids){
    		System.out.println(Arrays.toString(fs));
    	}
    	
    	//assigns each pixel to its respective class using centroids
    	final HardAssigner<float[],?,?> assigner = result.defaultHardAssigner();
    	
    	//assign method takes vector value of pixel, returns an index used to find correct centroid 
    	/*
    	for(int x=0; x<festival.getWidth(); x++){
    		for(int y=0; y<festival.getHeight(); y++){
    			float[] pixel = festival.getPixelNative(x, y);
    			int centroid = assigner.assign(pixel);
    			festival.setPixelNative(x, y, centroids[centroid]);
    		}
    	}
    	*/
    	
    	image.processInplace(new PixelProcessor<Float[]>(){

    		//assign each pixel to its corresponding centroid
			public Float[] processPixel(Float[] pixel) 
			{
				//Convert Float[] to float[]
				float[] pxl = new float[pixel.length];
				for(int i=0; i<pixel.length; i++){
					pxl[i] = pixel[i];
				}
				
				int centroid = assigner.assign(pxl);
				float[] rslts = centroids[centroid];
				
				//Convert float[] to Float
				Float[] results = new Float[rslts.length];
				for(int i=0; i<rslts.length; i++){
					results[i] = rslts[i];
				}
				
				return results;
			}
    	});
    	
    	//to display properly
    	image = ColourSpace.convert(image, ColourSpace.RGB);
    	
    	DisplayUtilities.display(image);
    	return image;
    }
    
    public static void labelImage(MBFImage segmentedImg)
    {
    	//find ConnectedComponents (groups of touching pixels in same class)
    	//MBFImage.flatten merges colours into grey values by averaging their values
    	GreyscaleConnectedComponentLabeler labeler = new GreyscaleConnectedComponentLabeler();
    	List<ConnectedComponent> components = labeler.findComponents(segmentedImg.flatten());
    	
    	//Label each region as long as its area is greater than a specified size
    	int i = 0;
    	for(ConnectedComponent c : components){
    		if(c.calculateArea() < 500)
    			continue;
    		segmentedImg.drawText("Region " + i++, c.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
    	}
   
    	DisplayUtilities.display(segmentedImg);
    }
    
    //alternative segmentation algorithm
    public static void labelFHS(MBFImage image)
    {
    	FelzenszwalbHuttenlocherSegmenter<MBFImage> fhs = new FelzenszwalbHuttenlocherSegmenter<MBFImage>();
    	//generate connected components
    	List<ConnectedComponent> comps = fhs.segment(image);
    	
    	//display all connected components in new window with random colours
    	MBFImage segmented = SegmentationUtilities.renderSegments(image.getWidth(), image.getHeight(), comps);
    	DisplayUtilities.display(segmented, "FHS CLUSTERED");
    	
    	int i=0;
    	//label 
    	for(ConnectedComponent c : comps){
    		if(c.calculateArea() < 500)
    			continue;
    		segmented.drawText("Region " + i++, c.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 20);
    	}
    	
    	DisplayUtilities.display(segmented, "FHS LABELLED");
    }
}
