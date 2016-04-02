package jwc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut4_Main
{
	static URL[] imageURLs;
	 //stores images indexed by their url
    private HashMap<Integer, MBFImage> images;
    
	private ArrayList<double[]> distances;
	private ArrayList<MultidimensionalHistogram> histograms;
	
    public static void main( String[] args ) throws Exception
    {
    	Tut4_Main m = new Tut4_Main();
    	
		imageURLs = new URL[]{
			new URL( "http://users.ecs.soton.ac.uk/dpd/projects/openimaj/tutorial/hist1.jpg" ),
		    new URL( "http://users.ecs.soton.ac.uk/dpd/projects/openimaj/tutorial/hist2.jpg" ),
		    new  URL( "http://users.ecs.soton.ac.uk/dpd/projects/openimaj/tutorial/hist3.jpg" )
		};
		
		m.createHistograms();
		m.calculateDistances();
		m.sortBySimilarities();
		
	}

   
    
    public void createHistograms() throws Exception
    {
    	histograms = new ArrayList<MultidimensionalHistogram>();
		images = new HashMap<Integer, MBFImage>();
		//HistogramModel class provides a means for creating a MultidimensionalHistogram from an image
		HistogramModel model = new HistogramModel(4,4,4);
		
		//Load in 3 images and generate their histograms
		//remember to clone as model will be reused
		int imgID = 0;
		for(URL u : imageURLs)
		{
			MBFImage img = ImageUtilities.readMBF(u);
			images.put(imgID, img);
			imgID++;
			DisplayUtilities.display(img);
			
			model.estimateModel(img);
			histograms.add(model.histogram.clone());
		}
		
    }
    
    public void calculateDistances()
    {
    	distances = new ArrayList<double[]>();
		
		//Calculate comparison for each pair of histograms, store in arraylist
		//euclid - low value indicates high similarity
    	//intersection - high value indicates high similarity (sortBysimilarities must be swapped round to use this)
		for(int i=0; i<histograms.size(); i++){
			for(int j=0; j<histograms.size(); j++){
				
				double[] data = new double[3];
				double distance = histograms.get(i).compare(histograms.get(j), DoubleFVComparison.EUCLIDEAN);
				data[0] = i;
				data[1] = j;
				data[2] = distance;
				//System.out.println("Image " + i + " ~ Image " + j + " = " + distance);
				distances.add(data);
			}
		}
    }
    
    public void sortBySimilarities()
    {
    	//sort arraylist by last value of each entry (i.e. distance)
		Collections.sort(distances, new Comparator<double[]>(){
			public int compare(double[] o1, double[] o2) {
				return (Integer) (Double.valueOf(o1[2]).compareTo(o2[2]));
			}		
		});
		
		//Display lowest non-zero distance in list (i.e. most similar pair of images)
		//ignoring 0-values, put most similar images in array list
		ArrayList<MBFImage> similars = new ArrayList<MBFImage>();
		for(double[] data : distances){
			double distance = data[2];
			if(distance==0)
				continue;
			else
				System.out.println(data[0] + ", " + data[1] + " = " + distance);
				similars.add(images.get((int)data[0]));
				similars.add(images.get((int)data[1]));
		}
		
		//display first two entries (most similar two entries)
		DisplayUtilities.display(similars.get(0), "MOST SIM 1");
		DisplayUtilities.display(similars.get(1), "MOST SIM 2");
    }
    
}

