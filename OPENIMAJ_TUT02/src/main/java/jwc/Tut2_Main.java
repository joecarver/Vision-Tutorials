package jwc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.pixel.ConnectedComponent.ConnectMode;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processor.connectedcomponent.render.BorderRenderer;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Ellipse;
import org.openimaj.image.pixel.ConnectedComponent.ConnectMode;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Tut2_Main {
    public static void main( String[] args ) throws MalformedURLException, IOException, InterruptedException {
    	
    	//collect image from web
    	MBFImage image = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/sinaface.jpg"));
    	
    	//imgFrame used throughout to update w new image
    	JFrame imgFrame = DisplayUtilities.createNamedWindow("Tut01");
    	DisplayUtilities.display(image, imgFrame);
    	
    	
    	MBFImage clone = image.clone();
    	
    	/* Loop over rows and columns,setting each G&B pixel to 0
    	for(int y=0; y<image.getHeight(); y++){
    		for(int x=0; x<image.getWidth(); x++){
    			clone.getBand(1).pixels[y][x] = 0;
    			clone.getBand(2).pixels[y][x] = 0;
    		}
    	}
    	*/
    	
    	//use openimaj built-in methods instead
    	clone.getBand(1).fill(0f);
    	clone.getBand(2).fill(0f);
    	
    	Thread.sleep(2000);
    	DisplayUtilities.display(clone, imgFrame);
    	
    	
    	image.processInplace(new CannyEdgeDetector());
    	Thread.sleep(2000);
    	//each pixel of each band is replaced with the edge response at that point
    	DisplayUtilities.display(image, imgFrame);
    	
    	
    	
    	//ellipses defined by their centre [x, y], axes [major, minor] and rotation
    	//wait and redraw in between each new shapes    	
    	Ellipse e1 = new Ellipse(700f, 450f, 20f, 10f, 0f);
    	image.drawShapeFilled(e1, RGBColour.WHITE);
    	Thread.sleep(1000);
    	DisplayUtilities.display(image, imgFrame);
    	Ellipse e2 = new Ellipse(650f, 425f, 25f, 12f, 0f);
    	image.drawShapeFilled(e2, RGBColour.WHITE);
    	Thread.sleep(500);
    	DisplayUtilities.display(image, imgFrame);
    	Ellipse e3 = new Ellipse(600f, 380f, 30f, 15f, 0f);
    	image.drawShapeFilled(e3, RGBColour.WHITE);
    	Thread.sleep(500);
    	DisplayUtilities.display(image, imgFrame);
    	Ellipse e4 = new Ellipse(500f, 300f, 100f, 70f, 0f);
    	image.drawShapeFilled(e4, RGBColour.WHITE);
    	Thread.sleep(500);
    	DisplayUtilities.display(image, imgFrame);
    	image.drawText("OpenIMAJ is", 425, 300, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
    	Thread.sleep(500);
    	DisplayUtilities.display(image, imgFrame);
    	image.drawText("Not Bad", 425, 330, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
    	Thread.sleep(1000);
    	DisplayUtilities.display(image, imgFrame);
    	
    	//used to draw borders between image and given component
    	BorderRenderer<Float[]> br = new BorderRenderer<Float[]>(image, RGBColour.MAGENTA, ConnectMode.CONNECT_8);
    	ConnectedComponent e1c = new ConnectedComponent(e1);
    	br.process(e1c);
    	ConnectedComponent e2c = new ConnectedComponent(e2);
    	br.process(e2c);
    	ConnectedComponent e3c = new ConnectedComponent(e3);
    	br.process(e3c);
    	ConnectedComponent e4c = new ConnectedComponent(e4);
    	br.process(e4c);
    	
    	Thread.sleep(1000);
    	DisplayUtilities.display(image, imgFrame);    	
    }
}
