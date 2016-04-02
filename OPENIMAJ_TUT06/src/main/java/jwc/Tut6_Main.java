package jwc;

import java.util.Set;

import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.dataset.BingImageDataset;
import org.openimaj.image.dataset.FlickrImageDataset;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut6_Main {
	

	
	
    public static void main( String[] args ) throws Exception
    {
    	Tut6_Main m = new Tut6_Main();
    	//m.displayRandomImages();
    	m.getWebImages();
    }

	
    

    
    //display one image at random for each person
    public void displayRandomImages() throws Exception
    {
    	VFSGroupDataset<FImage> groupedFaces = new VFSGroupDataset<FImage>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);
    	Set<Entry<String, VFSListDataset<FImage>>> entryset = groupedFaces.entrySet();
    	ArrayList<FImage> randomImages = new ArrayList<FImage>();
    	for (Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet())
    	{
    		VFSListDataset<FImage> images = entry.getValue();
    		randomImages.add(entry.getValue().getRandomInstance());
    	}
    	DisplayUtilities.display("Random" , randomImages);
    }
    
    public void getWebImages() throws Exception
    {
    	BingAPIToken bingToken = DefaultTokenFactory.get(BingAPIToken.class);
    	//BingImageDataset<FImage> images = BingImageDataset.create(ImageUtilities.FIMAGE_READER, bingToken, "cat", 10);
    	//DisplayUtilities.display("Bing..." ,images);
    	
    	
    	String[] names = {"George Osbourne", "Carl Cox", "Roy Keane"};
    	
    	List<BingImageDataset<MBFImage>> all_celebs = new ArrayList<BingImageDataset<MBFImage>>();
    	
    	for(int i=0; i<names.length; i++){
    		String name = names[i];
    		BingImageDataset<MBFImage> images = BingImageDataset.create(ImageUtilities.MBFIMAGE_READER, bingToken, name, 10);
    		all_celebs.add(images);
    	}
    	
    	
    	MapBackedDataset<String, BingImageDataset<MBFImage>, MBFImage> celeb_map = MapBackedDataset.of(all_celebs);
    	
    	for( Entry<String, BingImageDataset<MBFImage>> entry : celeb_map.entrySet()){
    		DisplayUtilities.display(entry.getKey(), entry.getValue());
    	}
    	
    }
    
    
}
