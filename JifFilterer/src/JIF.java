import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class JIF {

	public int imageLength;
	public int imageHeight;
	
	public BufferedImage[] frames;
	private ImageFrame[] imageFrames;
	
	public int[] delayTime;
	public int averageDelayTime;
		
	public JIF(String fileName) {
		try {
			imageFrames = readGif(Files.newInputStream(Paths.get(fileName)));
			
			frames = new BufferedImage[imageFrames.length];
			delayTime = new int[imageFrames.length];
			for(int i = 0; i < frames.length; i++) {
				frames[i] = imageFrames[i].getImage();
				
			    delayTime[i] = imageFrames[i].getDelay();
				
			    //ImageIO.write(frames[i], "PNG", new File("/Users/jack/Desktop/debug/tjif" + i + ".PNG"));
			}
			
			for(int i : delayTime) {
				averageDelayTime += i;
			}
			
			averageDelayTime /= delayTime.length;
			
			
		} catch(Exception e) {
			e.printStackTrace();
			Settings.clearScreen();
		}
	}
	
	public Rectangle getBounds() {
		Rectangle bounds = new Rectangle();
		
		int startx = 0;
		int endx = frames[0].getWidth();
		
		int starty = 0;
		int endy = frames[0].getHeight();
		
		int thresholdColor = Integer.parseInt(Settings.get("crop-threshold"), 16);
		
		for(int x = 0; x < endx / 2; x++) {
			for(int y = 0; y < frames[0].getHeight(); y++) {				
				if((Filter.toGray(frames[0].getRGB(x, y)) & 0xFFFFFF) > thresholdColor) {
					startx = x;
					break;
				}
			}
			if(startx != 0) { break; }
		}
		
		for(int y = 0; y < endy / 2; y++) {
			for(int x = 0; x < frames[0].getWidth(); x++) {
				if((Filter.toGray(frames[0].getRGB(x, y)) & 0xFFFFFF) > thresholdColor) {
					starty = y;
					break;
				}
			}
			if(starty != 0) { break; }
		}
		
		for(int x = frames[0].getWidth() - 1; x >= endx / 2; x--) {
			for(int y = 0; y < frames[0].getHeight(); y++) {
				if((Filter.toGray(frames[0].getRGB(x, y)) & 0xFFFFFF) > thresholdColor) {
					endx = x;
					break;
				}	
			}
			if(endx != frames[0].getWidth()) { break; }
		}
	
		for(int y = frames[0].getHeight() - 1; y >= endy / 2; y--) {
			for(int x = 0; x < frames[0].getWidth(); x++) {
				if((Filter.toGray(frames[0].getRGB(x, y)) & 0xFFFFFF) > thresholdColor) {
					endy = y;
					break;
				}	
			}
			if(endy != frames[0].getHeight()) { break; }
		}
		
		bounds.x = startx;
		bounds.y = starty;
		bounds.width = (endx + 1) - startx;
		bounds.height = (endy + 1) - starty;
		
		//System.out.printf("%dx%d at %d,%d\n", bounds.width, bounds.height, bounds.x, bounds.y);
		
		return bounds;
	}
	
	// taken from stackoverflow user Alex Orzechowski
	private ImageFrame[] readGif(InputStream stream) throws IOException{
	    ArrayList<ImageFrame> frames = new ArrayList<ImageFrame>(2);

	    ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("gif").next();
	    reader.setInput(ImageIO.createImageInputStream(stream));

	    int lastx = 0;
	    int lasty = 0;

	    int width = -1;
	    int height = -1;

	    IIOMetadata metadata = reader.getStreamMetadata();

	    Color backgroundColor = null;

	    if(metadata != null) {
	        IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

	        NodeList globalColorTable = globalRoot.getElementsByTagName("GlobalColorTable");
	        NodeList globalScreeDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");

	        if (globalScreeDescriptor != null && globalScreeDescriptor.getLength() > 0){
	            IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreeDescriptor.item(0);

	            if (screenDescriptor != null){
	                width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
	                height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
	            }
	        }

	        if (globalColorTable != null && globalColorTable.getLength() > 0){
	            IIOMetadataNode colorTable = (IIOMetadataNode) globalColorTable.item(0);

	            if (colorTable != null) {
	                String bgIndex = colorTable.getAttribute("backgroundColorIndex");

	                IIOMetadataNode colorEntry = (IIOMetadataNode) colorTable.getFirstChild();
	                while (colorEntry != null) {
	                    if (colorEntry.getAttribute("index").equals(bgIndex)) {
	                        int red = Integer.parseInt(colorEntry.getAttribute("red"));
	                        int green = Integer.parseInt(colorEntry.getAttribute("green"));
	                        int blue = Integer.parseInt(colorEntry.getAttribute("blue"));

	                        backgroundColor = new Color(red, green, blue);
	                        break;
	                    }

	                    colorEntry = (IIOMetadataNode) colorEntry.getNextSibling();
	                }
	            }
	        }
	    }

	    BufferedImage master = null;
	    boolean hasBackround = false;

	    for (int frameIndex = 0;; frameIndex++) {
	        BufferedImage image;
	        try{
	            image = reader.read(frameIndex);
	        }catch (IndexOutOfBoundsException io){
	            break;
	        }

	        if (width == -1 || height == -1){
	            width = image.getWidth();
	            height = image.getHeight();
	        }

	        IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
	        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
	        NodeList children = root.getChildNodes();

	        int delay = Integer.valueOf(gce.getAttribute("delayTime"));

	        String disposal = gce.getAttribute("disposalMethod");

	        if (master == null){
	            master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	            master.createGraphics().setColor(backgroundColor);
	            master.createGraphics().fillRect(0, 0, master.getWidth(), master.getHeight());

	        hasBackround = image.getWidth() == width && image.getHeight() == height;

	            master.createGraphics().drawImage(image, 0, 0, null);
	        }else{
	            int x = 0;
	            int y = 0;

	            for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++){
	                Node nodeItem = children.item(nodeIndex);

	                if (nodeItem.getNodeName().equals("ImageDescriptor")){
	                    NamedNodeMap map = nodeItem.getAttributes();

	                    x = Integer.valueOf(map.getNamedItem("imageLeftPosition").getNodeValue());
	                    y = Integer.valueOf(map.getNamedItem("imageTopPosition").getNodeValue());
	                }
	            }

	            if (disposal.equals("restoreToPrevious")){
	                BufferedImage from = null;
	                for (int i = frameIndex - 1; i >= 0; i--){
	                    if (!frames.get(i).getDisposal().equals("restoreToPrevious") || frameIndex == 0){
	                        from = frames.get(i).getImage();
	                        break;
	                    }
	                }

	                {
	                    ColorModel model = from.getColorModel();
	                    boolean alpha = from.isAlphaPremultiplied();
	                    WritableRaster raster = from.copyData(null);
	                    master = new BufferedImage(model, raster, alpha, null);
	                }
	            }else if (disposal.equals("restoreToBackgroundColor") && backgroundColor != null){
	                if (!hasBackround || frameIndex > 1){
	                    master.createGraphics().fillRect(lastx, lasty, frames.get(frameIndex - 1).getWidth(), frames.get(frameIndex - 1).getHeight());
	                }
	            }
	            master.createGraphics().drawImage(image, x, y, null);

	            lastx = x;
	            lasty = y;
	        }

	        {
	            BufferedImage copy;

	            {
	                ColorModel model = master.getColorModel();
	                boolean alpha = master.isAlphaPremultiplied();
	                WritableRaster raster = master.copyData(null);
	                copy = new BufferedImage(model, raster, alpha, null);
	            }
	            frames.add(new ImageFrame(copy, delay, disposal, image.getWidth(), image.getHeight()));
	        }

	        master.flush();
	    }
	    reader.dispose();

	    return frames.toArray(new ImageFrame[frames.size()]);
	}
	
	public void writeOut(String fileName) {
		try {
			ImageOutputStream stream = new FileImageOutputStream(new File(fileName));
			GifSequenceWriter sequenceWriter = new GifSequenceWriter(stream, BufferedImage.TYPE_4BYTE_ABGR, (int)((double)averageDelayTime * (10.0 * Settings.getDouble("speed-percentage"))), true);
			
			for(BufferedImage frame : frames) {
				sequenceWriter.writeToSequence(frame);
			}
			
			sequenceWriter.close();
			stream.close();
		} catch(Exception e) {
			e.printStackTrace();
			Settings.clearScreen();
		}
	}
	
}
