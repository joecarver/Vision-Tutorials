package jwc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.RangePartitioner;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut14_Main 
{
	
	
	//4 implementations to compute normalised averages of all images in a dataset
	//doing both loops in a parallel manner is the quickest method, but not by a lot - due to overheads in retrieving images etc
	//parallelising the inner loop gives a greater speed-up than the outer loop, suggesting it is more computationally intensive
    public static void main( String[] args ) throws Exception
    {
    	Tut14_Main m = new Tut14_Main();
    	m.init();
    	
    	//m.nonParallel(); //~17.4s
    	//m.innerParallel(); //~8.6s
    	//m.outerParallel(); //~10.3s
    	m.bothParallel(); //~7.4s
    }

	private VFSGroupDataset<MBFImage> allImages;
	private GroupedDataset<String, ListDataset<MBFImage>, MBFImage> images;
    
    void init() throws Exception
    {
    	//load all images directly and restrict our program to first 8
    	allImages = Caltech101.getImages(ImageUtilities.MBFIMAGE_READER);
    	images = GroupSampler.sample(allImages, 8, false);
    }
    
    //takes ~7.4ms
    void bothParallel()
    {
    	final List<MBFImage> output = new ArrayList<MBFImage>();
    	final ResizeProcessor resize = new ResizeProcessor(200);
    	
    	Timer t1 = Timer.timer();
    	
    	//Parallel.forEach takes a collection to iterate over and the operation to apply to each element of the collection
    	//actual functionality defined by perform function
    	Parallel.forEach(images.values(), new Operation<ListDataset<MBFImage>>(){
    		
			public void perform(ListDataset<MBFImage> object) {
				final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
				
				//Partitioned version feeds each thread a collection of images
	    		Parallel.forEachPartitioned(new RangePartitioner<MBFImage>(object), new Operation<Iterator<MBFImage>>() {
					public void perform(Iterator<MBFImage> it)
					{
						final MBFImage temp = new MBFImage(200,200, ColourSpace.RGB);
						final MBFImage tempAccum = new MBFImage(200,200,3);
						
						while(it.hasNext()){
							temp.fill(RGBColour.WHITE);
							MBFImage i = it.next();
							
							final MBFImage small = i.process(resize).normalise();
							final int x = (200 - small.getWidth()) / 2;
					        final int y = (200 - small.getHeight()) / 2;
					        temp.drawImage(small, x, y);
					        tempAccum.addInplace(temp);
						}
						
						//Stop multiple threads accessing the image at the same time
				        synchronized(current){
				        	current.addInplace(tempAccum);
				        }
					}
	    		});	
	    		
	    		current.divideInplace((float) object.size());
	    	    output.add(current);
			}
    		
    	});
    	DisplayUtilities.display("Images", output);
    	System.out.println("Time: " + t1.duration() + "ms");
    }
    
   //takes ~10.3ms
    void outerParallel()
    {
    	final List<MBFImage> output = new ArrayList<MBFImage>();
    	final ResizeProcessor resize = new ResizeProcessor(200);
    	
    	Timer t1 = Timer.timer();
    	
    	//Parallel.forEach takes a collection to iterate over and the operation to apply to each element of the collection
    	//actual functionality defined by perform function
    	Parallel.forEach(images.values(), new Operation<ListDataset<MBFImage>>(){
    		
			public void perform(ListDataset<MBFImage> object) {
				final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);
				
				for (MBFImage i : object) {
	    	        MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
	    	        tmp.fill(RGBColour.WHITE);
	    	        MBFImage small = i.process(resize).normalise();
	    	        int x = (200 - small.getWidth()) / 2;
	    	        int y = (200 - small.getHeight()) / 2;
	    	        tmp.drawImage(small, x, y);
	    	        current.addInplace(tmp);
	    		}
	    		
	    		current.divideInplace((float) object.size());
	    	    output.add(current);
			}
    		
    	});
    	DisplayUtilities.display("Images", output);
    	System.out.println("Time: " + t1.duration() + "ms");
    }
    
    //Takes ~8.6s on my machine
    void innerParallel(){
    	List<MBFImage> output = new ArrayList<MBFImage>();
    	final ResizeProcessor resize = new ResizeProcessor(200);
    	
    	Timer t1 = Timer.timer();
    	for(ListDataset<MBFImage> clzImages : images.values())
    	{
    		final MBFImage current = new MBFImage(200,200, ColourSpace.RGB);
    		
    		//Partitioned version feeds each thread a collection of images
    		Parallel.forEachPartitioned(new RangePartitioner<MBFImage>(clzImages), new Operation<Iterator<MBFImage>>() {
				public void perform(Iterator<MBFImage> it)
				{
					final MBFImage temp = new MBFImage(200,200, ColourSpace.RGB);
					final MBFImage tempAccum = new MBFImage(200,200,3);
					
					while(it.hasNext()){
						temp.fill(RGBColour.WHITE);
						MBFImage i = it.next();
						
						final MBFImage small = i.process(resize).normalise();
						final int x = (200 - small.getWidth()) / 2;
				        final int y = (200 - small.getHeight()) / 2;
				        temp.drawImage(small, x, y);
				        tempAccum.addInplace(temp);
					}
					
					//Stop multiple threads accessing the image at the same time
			        synchronized(current){
			        	current.addInplace(tempAccum);
			        }
				}
    		});	
    		current.divideInplace((float) clzImages.size());
    		output.add(current);
    	}
    	
    	DisplayUtilities.display("Images", output);
    	System.out.println("Time: " + t1.duration() + "ms");
    }
    
    //Takes roughly 17.4s on my machine
    void nonParallel() throws Exception
    {
    	List<MBFImage> output = new ArrayList<MBFImage>();
    	ResizeProcessor resize = new ResizeProcessor(200);
    	
    	Timer t1 = Timer.timer();
    	for(ListDataset<MBFImage> clzImages : images.values())
    	{
    		MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

    		for (MBFImage i : clzImages) {
    	        MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
    	        tmp.fill(RGBColour.WHITE);
    	        MBFImage small = i.process(resize).normalise();
    	        int x = (200 - small.getWidth()) / 2;
    	        int y = (200 - small.getHeight()) / 2;
    	        tmp.drawImage(small, x, y);
    	        current.addInplace(tmp);
    		}
    		
    		current.divideInplace((float) clzImages.size());
    	    output.add(current);
    	}
    	DisplayUtilities.display("Images", output);
    	System.out.println("Time: " + t1.duration() + "ms");
    	
    }
}
