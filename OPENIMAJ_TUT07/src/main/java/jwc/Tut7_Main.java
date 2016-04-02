package jwc;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FFastGaussianConvolve;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.video.*;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut7_Main {
    public static void main( String[] args ) {
    	
    	//holds coloured frames (MB)
    	Video<MBFImage> video;
    	
    	//XuggleVideo internally uses ffmpeg
    	video = new XuggleVideo(new File("keyboardcat.flv"));
    	
    	//get video from webcam 
    	//video = new VideoCapture(320,240);
    	
    	//starts and plays video in window
    	VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(video);
    	
    	//Videos are iterable - process like this
    	//for(MBFImage i : video){
    	//	DisplayUtilities.displayName(i.process(new CannyEdgeDetector()), "videoFrames");
    	//}
    	
    	//alternatively, event-driven technique ties processing to image display automatically
    	display.addVideoListener(
    			//VideoDisplayListener is given video frames before rendering and recieves video display after rendering
    			//functionality like looping/stopping/pausing provided by VideoDisplay class
    			new VideoDisplayListener<MBFImage>(){

    				@Override
    				public void afterUpdate(VideoDisplay<MBFImage> display) {
    				}

    				@Override
    				public void beforeUpdate(MBFImage frame) {
    					//frame.processInplace(new CannyEdgeDetector());
    					//GaussianConvolve applies averaging to achieve blur effect
    					frame.processInplace(new FFastGaussianConvolve(10, 4));
    				}
    		
    	});
    	
    }
}
