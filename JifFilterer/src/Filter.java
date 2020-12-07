import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public class Filter {

	public static int getFilterValue(String filterName) {
		switch (filterName) {
			case "resize":
				return 35;
			case "curve":
				return 50;
			case "crop":
				return 23;
			case "wash-out":
				return 7;
			case "color-shift":
				return 22;
			case "color-bleed":
				return 35;
			case "interlace":
				return 8;
			case "lighten":
				return 5;
			case "normalize":
				return 10;
			case "add-scan-lines":
				return 1;
			case "split":
				return 3;
			default:
				return 10;
		}
	}
	
	private static BufferedImage cropFrame(BufferedImage frame, Rectangle bounds) {
		BufferedImage cropped = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 0; x < bounds.width; x++) {
			for(int y = 0; y < bounds.height; y++) {
				cropped.setRGB(x, y, frame.getRGB(x + bounds.x, y + bounds.y));
			}
		}
		
		return cropped;
	}
	
	private static BufferedImage curveFrame(BufferedImage frame, double xCoefficient, double yCoefficient, int bulge) {
		BufferedImage curved = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		double j = xCoefficient;
		double i = yCoefficient;
		
		double w = frame.getWidth();
		double h = frame.getHeight();
		
		for(int x = 0; x < w; x++) {
			// function that determines how high each column should be
			double f = (-1 * ( Math.pow(x - (w / 2), 2) / (j * w))) + h;
			// how much black padding on top and bottom
			double o = (h - f) / 2;
			
			for(int y = 0; y < h; y++) {
				if(y < (int)o || (h - y) < (int)o) {
					// draw black
					curved.setRGB((int)x, y, 0x000000);
				} else {
					double realy = (double)y - o;
					realy = (realy < 0 ? 0 : (realy > h - 1 ? h - 1 : realy));
					
					double u = ((h / f) * realy) + ((f / (bulge*j)) * (Math.sin((Math.PI * realy * 2) / f)));
					u = (u < 0 ? 0 : (u > h - 1 ? h - 1 : u));
					
					int belowColor = frame.getRGB((int)x, (int)u);
					int aboveColor = belowColor;
					
					if(((int)u + 1) < frame.getHeight()) {
						aboveColor = frame.getRGB((int)x, (int)u + 1);
					}
					
					int mixedColor = 0;
					int cy = (u - (int)u > 0.5 ? (int)u + 1 : (int)u);
					
					if(cy > 1 && cy < (h - 2) && x > 1 && x < (w - 2)) {
						int red = (int)(0.0375 * (double)(red(frame, x, cy - 1) + red(frame, x, cy + 1) + red(frame, x + 1, cy) + red(frame, x - 1, cy)))
								+ (int)(0.025 * (double)(red(frame, x - 1, cy - 1) + red(frame, x - 1, cy + 1) + red(frame, x + 1, cy - 1) + red(frame, x + 1, cy + 1)))
								+ (int)(0.025 * (double)(red(frame, x - 2, cy) + red(frame, x + 2, cy) + red(frame, x, cy - 2) + red(frame, x, cy + 2)))
								+ (int)(0.65 * red(frame, x, cy));
						
						int green = (int)(0.0375 * (double)(green(frame, x, cy - 1) + green(frame, x, cy + 1) + green(frame, x + 1, cy) + green(frame, x - 1, cy)))
								+ (int)(0.025 * (double)(green(frame, x - 1, cy - 1) + green(frame, x - 1, cy + 1) + green(frame, x + 1, cy - 1) + green(frame, x + 1, cy + 1)))
								+ (int)(0.025 * (double)(green(frame, x - 2, cy) + green(frame, x + 2, cy) + green(frame, x, cy - 2) + green(frame, x, cy + 2)))
								+ (int)(0.65 * green(frame, x, cy));
						
						int blue = (int)(0.0375 * (double)(blue(frame, x, cy - 1) + blue(frame, x, cy + 1) + blue(frame, x + 1, cy) + blue(frame, x - 1, cy)))
								+ (int)(0.025 * (double)(blue(frame, x - 1, cy - 1) + blue(frame, x - 1, cy + 1) + blue(frame, x + 1, cy - 1) + blue(frame, x + 1, cy + 1)))
								+ (int)(0.025 * (double)(blue(frame, x - 2, cy) + blue(frame, x + 2, cy) + blue(frame, x, cy - 2) + blue(frame, x, cy + 2)))
								+ (int)(0.65 * blue(frame, x, cy));
						
						mixedColor = ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF); 
					} else {
						mixedColor = mixColors(belowColor, aboveColor, u - (int)u);
					}

					if(o - y > 0) {
						mixedColor = mixColors(0x000000, mixedColor, o - y);
					} else if(o - (h - y) > 0) {
						mixedColor = mixColors(0x000000, mixedColor, o - (h - y));
					}
					
					curved.setRGB((int)x, y, mixedColor);
				}
			}
		}
		
		BufferedImage curved2 = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int y = 0; y < h; y++) {
			// function that determines how long each row should be
			double f = (-1 * ( Math.pow(y - (h / 2), 2) / (i * h))) + w;
			// how much black padding on right and left
			double o = (w - f) / 2;
			
			for(int x = 0; x < w; x++) {
				if(x < (int)o || (w - x) < (int)o) {
					// draw black
					curved2.setRGB(x, (int)y, 0x000000);
				} else {
					double realx = (double)x - o;
					
					realx = (realx < 0 ? 0 : (realx > w - 1 ? w - 1 : realx));
					
					double u = ((w / f) * realx) + ((f / (bulge*i)) * (Math.sin((Math.PI * realx * 2) / f)));
					//System.out.printf("%.2f\n", u);
					
					u = (u < 0 ? 0 : (u > w - 1 ? w - 1 : u));
					
					int leftColor = curved.getRGB((int)u, (int)y);
					
					int rightColor = leftColor;
					
					if(((int)u + 1) < frame.getWidth()) {
						rightColor = curved.getRGB((int)u + 1, (int)y);
					}
					
					int mixedColor = 0;
					
					int cx = (u - (int)u > 0.5 ? (int)u + 1 : (int)u);
					if(y > 1 && y < (h - 2) && cx > 1 && cx < (w - 2)) {
						int red = (int)(0.0375 * (double)(red(curved, cx, y - 1) + red(curved, cx, y + 1) + red(curved, cx + 1, y) + red(curved, cx - 1, y)))
								+ (int)(0.025 * (double)(red(curved, cx - 1, y - 1) + red(curved, cx - 1, y + 1) + red(curved, cx + 1, y - 1) + red(curved, cx + 1, y + 1)))
								+ (int)(0.025 * (double)(red(curved, cx - 2, y) + red(curved, cx + 2, y) + red(curved, cx, y - 2) + red(curved, cx, y + 2)))
								+ (int)(0.65 * red(curved, cx, y));
						
						int green = (int)(0.0375 * (double)(green(curved, cx, y - 1) + green(curved, cx, y + 1) + green(curved, cx + 1, y) + green(curved, cx - 1, y)))
								+ (int)(0.025 * (double)(green(curved, cx - 1, y - 1) + green(curved, cx - 1, y + 1) + green(curved, cx + 1, y - 1) + green(curved, cx + 1, y + 1)))
								+ (int)(0.025 * (double)(green(curved, cx - 2, y) + green(curved, cx + 2, y) + green(curved, cx, y - 2) + green(curved, cx, y + 2)))
								+ (int)(0.65 * green(curved, cx, y));
						
						int blue = (int)(0.0375 * (double)(blue(curved, cx, y - 1) + blue(curved, cx, y + 1) + blue(curved, cx + 1, y) + blue(curved, cx - 1, y)))
								+ (int)(0.025 * (double)(blue(curved, cx - 1, y - 1) + blue(curved, cx - 1, y + 1) + blue(curved, cx + 1, y - 1) + blue(curved, cx + 1, y + 1)))
								+ (int)(0.025 * (double)(blue(curved, cx - 2, y) + blue(curved, cx + 2, y) + blue(curved, cx, y - 2) + blue(curved, cx, y + 2)))
								+ (int)(0.65 * blue(curved, cx, y));
						
						mixedColor = ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF); 
					} else {
						mixedColor = mixColors(leftColor, rightColor, u - (int)u);
					}
					
					//int mixedColor = mixColors(leftColor, rightColor, u - (int)u);
					
					if(o - x > 0) {
						mixedColor = mixColors(0x000000, mixedColor, o - x);
					} else if(o - (w - x) > 0) {
						mixedColor = mixColors(0x000000, mixedColor, o - (w - x));
					}
					
					curved2.setRGB(x, (int)y, mixedColor);
				}
			}
		}
		
		return curved2;
	} 
	
	public static void curve(JIF target, double xCoefficient, double yCoefficient, int bulge, Consumer<Double> progressFunction) {
			for(int i = 0; i < target.frames.length; i++) {
				target.frames[i] = curveFrame(target.frames[i], xCoefficient, yCoefficient, bulge);
				progressFunction.accept((double)i / (double)target.frames.length);
			}
	}
	
	public static void crop(JIF target, Consumer<Double> progressFunction) {
		Rectangle bounds = target.getBounds();
		if(bounds.x != 0 || bounds.y != 0 || bounds.width != target.frames[0].getWidth() || bounds.height != target.frames[0].getHeight()) {
			for(int i = 0; i < target.frames.length; i++) {
				target.frames[i] = cropFrame(target.frames[i], bounds);
				progressFunction.accept((double)i / (double)target.frames.length);
			}
		}
	}
	
	private static BufferedImage generateNoiseMap(BufferedImage target) {
		BufferedImage noiseMap = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

		Random r = new Random();
		
		for(int x = 0; x < noiseMap.getWidth(); x++) {
			for(int y = 0; y < noiseMap.getHeight(); y++) {
				noiseMap.setRGB(x, y, (r.nextInt(2) == 0 ? 0x000000 : 0xFFFFFF));
			}
		}
		
		return noiseMap;
	}
	
	private static BufferedImage generateInterlaceMap(BufferedImage target, int lineHeight) {
		BufferedImage noiseMap = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 0; x < noiseMap.getWidth(); x++) {
			for(int y = 0; y < noiseMap.getHeight(); y++) {
				noiseMap.setRGB(x, y, (y % (lineHeight * 2) > lineHeight - 1 ? 0x000000 : 0xFFFFFF));
			}
		}

		return noiseMap;
	}
	
	public static void interlace(JIF target, int lineHeight, double intensity) {
		for(int i = 0; i < target.frames.length; i++) {
			BufferedImage noiseMap = mixFrames(generateInterlaceMap(target.frames[i], lineHeight), generateNoiseMap(target.frames[i]), 0.8);
			
			for(int x = 0; x < target.frames[i].getWidth(); x++) {
				for(int y = 0; y < target.frames[i].getHeight(); y++) {
					target.frames[i].setRGB(x, y, (0xFF << 24) | mixColors(target.frames[i].getRGB(x, y), noiseMap.getRGB(x, y), intensity));
				}
			}
		}
	}
	
	
	private static BufferedImage copy(BufferedImage target) {
		BufferedImage duplicate = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 0; x < duplicate.getWidth(); x++) {
			for(int y = 0; y < duplicate.getHeight(); y++) {
				duplicate.setRGB(x, y, target.getRGB(x, y));
			}
		}
		
		return duplicate;
	}
	
	private static int changeValue(int color, double mod) {
		int red = r(color);
		int green = g(color);
		int blue = b(color);
		
		red = (int)((double)red + ((double)red * mod));
		green = (int)((double)green + ((double)green * mod));
		blue = (int)((double)blue + ((double)blue * mod));
		
		red = (red > 0xFF ? 0xFF : red);
		green = (green > 0xFF ? 0xFF : green);
		blue = (blue > 0xFF ? 0xFF : blue);
		
		red = (red < 0x00 ? 0x00 : red);
		green = (green < 0x00 ? 0x00 : green);
		blue = (blue < 0x00 ? 0x00 : blue);
		
		return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}
	
	private static int saturatePixel(int color, double redIntensity, double greenIntensity, double blueIntensity) {
		int gray = toGray(color);
		
		return mixColorsPrecise(color, gray, redIntensity, greenIntensity, blueIntensity);
	}
	
	public static int toGray(int color) {
		int red = r(color);
		int green = g(color);
		int blue = b(color);
		
		int average = (red + green + blue) / 3;
				
		return ((average & 0xFF) << 16) | ((average & 0xFF) << 8) | (average & 0xFF);
	}
	
	private static int mixColorsPrecise(int firstColor, int secondColor, double mixRed, double mixGreen, double mixBlue) {
		int firstRed = r(firstColor);
		int firstGreen = g(firstColor);
		int firstBlue = b(firstColor);
		
		int secondRed = r(secondColor);
		int secondGreen = g(secondColor);
		int secondBlue = b(secondColor);

		int red = (int)(firstRed * mixRed) + (int)(secondRed * (1.0 - mixRed));
		int green = (int)(firstGreen * mixGreen) + (int)(secondGreen * (1.0 - mixGreen));
		int blue = (int)(firstBlue * mixBlue) + (int)(secondBlue * (1.0 - mixBlue));
		
		red = (red > 0xFF ? 0xFF : red);
		green = (green > 0xFF ? 0xFF : green);
		blue = (blue > 0xFF ? 0xFF : blue);
		
		red = (red < 0x00 ? 0x00 : red);
		green = (green < 0x00 ? 0x00 : green);
		blue = (blue < 0x00 ? 0x00 : blue);
		
		return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}
	
	public static int mixColors(int firstColor, int secondColor, double mix) {
		int firstRed = r(firstColor);
		int firstGreen = g(firstColor);
		int firstBlue = b(firstColor);
		
		int secondRed = r(secondColor);
		int secondGreen = g(secondColor);
		int secondBlue = b(secondColor);

		int red = (int)(firstRed * mix) + (int)(secondRed * (1.0 - mix));
		int green = (int)(firstGreen * mix) + (int)(secondGreen * (1.0 - mix));
		int blue = (int)(firstBlue * mix) + (int)(secondBlue * (1.0 - mix));
		
		red = (red > 0xFF ? 0xFF : red);
		green = (green > 0xFF ? 0xFF : green);
		blue = (blue > 0xFF ? 0xFF : blue);
		
		red = (red < 0x00 ? 0x00 : red);
		green = (green < 0x00 ? 0x00 : green);
		blue = (blue < 0x00 ? 0x00 : blue);
		
		return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}
	
	private static BufferedImage mixFrames(BufferedImage firstFrame, BufferedImage secondFrame, double mix) {
		BufferedImage mixed = new BufferedImage(firstFrame.getWidth(), firstFrame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 0; x < firstFrame.getWidth(); x++) {
			for(int y = 0; y < firstFrame.getHeight(); y++) {
				mixed.setRGB(x, y, (0xFF << 24) | mixColors(firstFrame.getRGB(x, y), secondFrame.getRGB(x, y), mix));
				//target.frames[i].setRGB(x, y, (0xFF << 24) | mixColors(target.frames[i].getRGB(x, y), noiseMap.getRGB(x, y), intensity));
			}
		}
		
		return mixed;
	}
	
	private static void shiftAllHorizontal(BufferedImage frame, int shiftAmount) {
		if(shiftAmount < 0) {
			for(int x = 0; x < frame.getWidth() + shiftAmount; x++) {
				for(int y = 0; y < frame.getHeight(); y++) {
					int targetColor = frame.getRGB(x - shiftAmount, y);
																			
					frame.setRGB(x, y, (0xFF << 24) | targetColor);
				}
			}
		} else {
			for(int x = frame.getWidth() - 1; x >= shiftAmount; x--) {
				for(int y = 0; y < frame.getHeight(); y++) {
					int targetColor = frame.getRGB(x - shiftAmount, y);
									
					frame.setRGB(x, y, (0xFF << 24) | targetColor);
				}
			}
		}
	}
	
	private static void shiftAllVertical(BufferedImage frame, int shiftAmount) {
		if(shiftAmount < 0) {
			for(int y = 0; y < frame.getHeight() + shiftAmount; y++) {
				for(int x = 0; x < frame.getWidth(); x++) {
					int targetColor = frame.getRGB(x, y - shiftAmount);
																			
					frame.setRGB(x, y, (0xFF << 24) | targetColor);
				}
			}
		} else {
			for(int y = frame.getHeight() - 1; y >= shiftAmount; y--) {
				for(int x = 0; x < frame.getWidth(); x++) {
					int targetColor = frame.getRGB(x, y - shiftAmount);
									
					frame.setRGB(x, y, (0xFF << 24) | targetColor);
				}
			}
		}
	}
	
	public static void shake(JIF target) {
		for(int i = 0; i < target.frames.length; i++) {
			double randomx = Math.random();
			
			if(randomx < 0.1) {
				shiftAllHorizontal(target.frames[i], 1);
			} else if(randomx < 0.2) {
				shiftAllHorizontal(target.frames[i], -1);
			}
			
			double randomy = Math.random();
			
			if(randomy < 0.1) {
				shiftAllVertical(target.frames[i], 1);
			} else if(randomy < 0.2) {
				shiftAllVertical(target.frames[i], -1);
			}
		}
	}
	
	private static void shift(BufferedImage frame, char rgb, int shiftAmount) {
		if(shiftAmount < 0) {
			for(int x = 0; x < frame.getWidth() + shiftAmount; x++) {
				for(int y = 0; y < frame.getHeight(); y++) {
					int targetColor = frame.getRGB(x - shiftAmount, y);
					int targetComponent = (targetColor >> (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)) & 0xFF;
				
					targetColor &= (~(0xFF << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)));
					
					frame.setRGB(x - shiftAmount, y, targetColor);
					
					int destinationColor = frame.getRGB(x, y);
					destinationColor &= (~(0xFF << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)));
					destinationColor |= ((targetComponent & 0xFF) << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0));
					
					frame.setRGB(x, y, (0xFF << 24) | destinationColor);
				}
			}
		} else {
			for(int x = frame.getWidth() - 1; x >= shiftAmount; x--) {
				for(int y = 0; y < frame.getHeight(); y++) {
					int targetColor = frame.getRGB(x - shiftAmount, y);
					int targetComponent = (targetColor >> (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)) & 0xFF;
				
					targetColor &= (~(0xFF << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)));
					frame.setRGB(x - shiftAmount, y, targetColor);
					
					int destinationColor = frame.getRGB(x, y);
					destinationColor &= (~(0xFF << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0)));
					destinationColor |= ((targetComponent & 0xFF) << (rgb == 'r' ? 16 : rgb == 'g' ? 8 : 0));
					
					frame.setRGB(x, y, (0xFF << 24) | destinationColor);
				}
			}
		}
	}
	
	public static void colorShift(JIF target, double redShiftPercentage, double greenShiftPercentage, double blueShiftPercentage, Consumer<Double> progressFunction) {
		int redShift = (int)((double)target.frames[0].getWidth() * redShiftPercentage);
		int greenShift = (int)((double)target.frames[0].getWidth() * greenShiftPercentage);
		int blueShift = (int)((double)target.frames[0].getWidth() * blueShiftPercentage);
		
		for(int i = 0; i < target.frames.length; i++) {
			BufferedImage copy = copy(target.frames[i]);
			
			if(redShift != 0)
				shift(copy, 'r', redShift + (Math.random() > 0.8 ? 1 : (Math.random() > 0.8 ? -1 : 0)));
			
			if(greenShift != 0)
				shift(copy, 'g', greenShift + (Math.random() > 0.8 ? 1 : (Math.random() > 0.8 ? -1 : 0)));
			
			if(blueShift != 0)
				shift(copy, 'b', blueShift + (Math.random() > 0.8 ? 1 : (Math.random() > 0.8 ? -1 : 0)));
			
			target.frames[i] = mixFrames(target.frames[i], copy, 0.5);
			
			progressFunction.accept((double)i / (double)target.frames.length);
		}
	}
	
	private static BufferedImage blurFrame(BufferedImage target, double amount) {
		BufferedImage blurred = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 1; x < target.getWidth() - 1; x++) {
			for(int y = 1; y < target.getHeight() - 1; y++) {
				double r = (amount * red(target, x, y))
						+ (((1.0 - amount) / 6.0) * (red(target, x - 1, y) + red(target, x + 1, y) + red(target, x, y + 1) + red(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (red(target, x - 1, y - 1) + red(target, x - 1, y + 1) + red(target, x + 1, y - 1) + red(target, x + 1, y + 1)));
				
				double g = (amount * green(target, x, y))
						+ (((1.0 - amount) / 6.0) * (green(target, x - 1, y) + green(target, x + 1, y) + green(target, x, y + 1) + green(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (green(target, x - 1, y - 1) + green(target, x - 1, y + 1) + green(target, x + 1, y - 1) + green(target, x + 1, y + 1)));
				
				double b = (amount * blue(target, x, y))
						+ (((1.0 - amount) / 6.0) * (blue(target, x - 1, y) + blue(target, x + 1, y) + blue(target, x, y + 1) + blue(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (blue(target, x - 1, y - 1) + blue(target, x - 1, y + 1) + blue(target, x + 1, y - 1) + blue(target, x + 1, y + 1)));
				
				int ri = (int)r;
				int gi = (int)g;
				int bi = (int)b;
				
				ri = (ri < 0 ? 0 : (ri > 255 ? 255 : ri));
				gi = (gi < 0 ? 0 : (gi > 255 ? 255 : gi));
				bi = (bi < 0 ? 0 : (bi > 255 ? 255 : bi));
				
				int color = ((ri & 0xFF) << 16) | ((gi & 0xFF) << 8) | (bi & 0xFF);
				blurred.setRGB(x, y, color);
			}
		}
		
		return blurred;
	}
	
	private static BufferedImage boxBlur(BufferedImage target, int radius) {
		BufferedImage blurred = new BufferedImage(target.getWidth(), target.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		for(int x = 0; x < target.getWidth(); x++) {
			for(int y = 0; y < target.getHeight(); y++) { 
				// if box includes out of bounds pixels, just use original rgb
				if(x < radius || x > (target.getWidth() - (radius + 1)) || y < radius || y > (target.getHeight() - (radius + 1))) {
					blurred.setRGB(x, y, target.getRGB(x, y));
				} else {
					double r = 0;
					double g = 0;
					double b = 0;
					
					double denominator = Math.pow(1 + (2 * radius), 2);
					
					for(int boxX = x - radius; boxX < x + radius + 1; boxX++) {
						for(int boxY = y - radius; boxY < y + radius + 1; boxY++) {
							r += ( (1.0 / denominator) * (double)red(target, boxX, boxY) );
							g += ( (1.0 / denominator) * (double)green(target, boxX, boxY) );
							b += ( (1.0 / denominator) * (double)blue(target, boxX, boxY) );
						}
					}
					
					int ri = (int)r;
					int gi = (int)g;
					int bi = (int)b;
					
					ri = (ri < 0 ? 0 : (ri > 255 ? 255 : ri));
					gi = (gi < 0 ? 0 : (gi > 255 ? 255 : gi));
					bi = (bi < 0 ? 0 : (bi > 255 ? 255 : bi));
					
					int color = ((ri & 0xFF) << 16) | ((gi & 0xFF) << 8) | (bi & 0xFF);
					blurred.setRGB(x, y, color);
				}
				
				/*double r = (amount * red(target, x, y))
						+ (((1.0 - amount) / 6.0) * (red(target, x - 1, y) + red(target, x + 1, y) + red(target, x, y + 1) + red(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (red(target, x - 1, y - 1) + red(target, x - 1, y + 1) + red(target, x + 1, y - 1) + red(target, x + 1, y + 1)));
				
				double g = (amount * green(target, x, y))
						+ (((1.0 - amount) / 6.0) * (green(target, x - 1, y) + green(target, x + 1, y) + green(target, x, y + 1) + green(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (green(target, x - 1, y - 1) + green(target, x - 1, y + 1) + green(target, x + 1, y - 1) + green(target, x + 1, y + 1)));
				
				double b = (amount * blue(target, x, y))
						+ (((1.0 - amount) / 6.0) * (blue(target, x - 1, y) + blue(target, x + 1, y) + blue(target, x, y + 1) + blue(target, x, y - 1)))
						+ (((1.0 - amount) / 12.0) * (blue(target, x - 1, y - 1) + blue(target, x - 1, y + 1) + blue(target, x + 1, y - 1) + blue(target, x + 1, y + 1)));
				
				int ri = (int)r;
				int gi = (int)g;
				int bi = (int)b;
				
				ri = (ri < 0 ? 0 : (ri > 255 ? 255 : ri));
				gi = (gi < 0 ? 0 : (gi > 255 ? 255 : gi));
				bi = (bi < 0 ? 0 : (bi > 255 ? 255 : bi));
				
				int color = ((ri & 0xFF) << 16) | ((gi & 0xFF) << 8) | (bi & 0xFF);
				blurred.setRGB(x, y, color);*/
			}
		}
		
		return blurred;
	}
	
	public static void colorBleed(JIF target, double redShiftPercentage, double greenShiftPercentage, double blueShiftPercentage, double blurFactor, Consumer<Double> progressFunction) {
		int redShift = (int)((double)target.frames[0].getWidth() * redShiftPercentage);
		int greenShift = (int)((double)target.frames[0].getWidth() * greenShiftPercentage);
		int blueShift = (int)((double)target.frames[0].getWidth() * blueShiftPercentage);
		
		for(int i = 0; i < target.frames.length; i++) {
			BufferedImage copy = copy(target.frames[i]);
			
			if(redShift != 0)
				shift(copy, 'r', redShift);
			
			if(greenShift != 0)
				shift(copy, 'g', greenShift);
			
			if(blueShift != 0)
				shift(copy, 'b', blueShift);
			
			//copy = blurFrame(copy, blurFactor);
			target.frames[i] = mixFrames(target.frames[i], boxBlur(copy, 4), 0.5);
			//target.frames[i] = blurFrame(target.frames[i], blurFactor);//mixFrames(target.frames[i], copy, 0.5);
			
			progressFunction.accept((double)i / (double)target.frames.length);
		}
	}
	
	public static void addScanLines(JIF target, int lineColor) {
		Random r = new Random();
		for(int i = 0; i < target.frames.length; i++) {
			double scanLinePercentage = Math.random();
			
			int linePos = (scanLinePercentage > 0.8 ? r.nextInt(target.frames[i].getHeight()) : -1);
			
			for(int y = 0; y < target.frames[i].getHeight(); y++) {
				if(y == linePos) {

					for(int x = 0; x < target.frames[i].getWidth(); x++) {
						
						int offset = r.nextInt(target.frames[i].getWidth());
						int lineLength = (int)(60 + (40 * Math.random()));
						if((x + offset) % 100 < lineLength)
							target.frames[i].setRGB(x, y, (0xFF << 24) | lineColor);
					}
				}
			}
		}
	}
	
	private static double getFrameBrightness(BufferedImage frame) {
		double frameBrightness = 0;
		
		for(int y = 0; y < frame.getHeight(); y++) {
			for(int x = 0; x < frame.getWidth(); x++) {
				frameBrightness += ((double)(toGray(frame.getRGB(x, y)) & 0xFFFFFF) / (double)0xFFFFFF);
			}
		}
		
		frameBrightness /= (frame.getHeight() * frame.getWidth());
		
		return frameBrightness;
	}
	
	public static void normalize(JIF target, double intensity, double threshold) {
		if(getFrameBrightness(target.frames[0]) > threshold) {
			// image is very bright
			lighten(target, intensity * -0.18);
		} else {
			lighten(target, intensity);
		}
	}
	
	private static BufferedImage splitFrame(BufferedImage frame, double splitLocation) {
		int split = (int)(frame.getHeight() * splitLocation);
		BufferedImage destination = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

		for(int y = split; y < frame.getHeight(); y++) {
			for(int x = 0; x < frame.getWidth(); x++) {
				destination.setRGB(x, y - split, frame.getRGB(x, y));
			}
		}
		
		for(int y = 0; y < split; y++) {
			for(int x = 0; x < frame.getWidth(); x++) {
				destination.setRGB(x, y + (destination.getHeight() - split), frame.getRGB(x, y));
			}
		}
		
		return destination;
	}
	
	public static void addSplit(JIF target, int threshold, double intensity) {
		if(target.frames.length > threshold) {
			int targetFrameIndex = (int)(Math.random() * target.frames.length);
			
			target.frames[targetFrameIndex] = splitFrame(target.frames[targetFrameIndex], Math.random() * intensity);
		}
	}
	
	public static void addNoise(JIF target, double intensity) {
		for(int i = 0; i < target.frames.length; i++) {
			BufferedImage noiseMap = generateNoiseMap(target.frames[i]);
			
			for(int x = 0; x < target.frames[i].getWidth(); x++) {
				for(int y = 0; y < target.frames[i].getHeight(); y++) {
					//target.frames[i].setRGB(x, y, (0xFF << 24) | changeValue(target.frames[i].getRGB(x, y), (noiseMap.getRGB(x, y) == 0x000000 ? -1.0 * intensity : 1.0 * intensity)));
					target.frames[i].setRGB(x, y, (0xFF << 24) | mixColors(target.frames[i].getRGB(x, y), noiseMap.getRGB(x, y), intensity));
				}
			}
		}
	}
	
	public static void lighten(JIF target, double intensity) {
		for(int i = 0; i < target.frames.length; i++) {			
			for(int x = 0; x < target.frames[i].getWidth(); x++) {
				for(int y = 0; y < target.frames[i].getHeight(); y++) {
					target.frames[i].setRGB(x, y, (0xFF << 24) | changeValue(target.frames[i].getRGB(x, y), intensity));
				}
			}
		}
	}
	
	public static void washOut(JIF target, double redIntensity, double greenIntensity, double blueIntensity) {
		for(int i = 0; i < target.frames.length; i++) {			
			for(int x = 0; x < target.frames[i].getWidth(); x++) {
				for(int y = 0; y < target.frames[i].getHeight(); y++) {
					target.frames[i].setRGB(x, y, (0xFF << 24) | saturatePixel(target.frames[i].getRGB(x, y), redIntensity, greenIntensity, blueIntensity));
				}
			}
		}
	}
	
	private static int r(int c) {
		return (c >> 16) & 0xFF;
	}
	
	private static int g(int c) {
		return (c >> 8) & 0xFF;
	}
	
	private static int b(int c) {
		return c & 0xFF;
	}
	
	private static int red(BufferedImage frame, int x, int y) {
		return r(frame.getRGB(x, y));
	}
	
	private static int green(BufferedImage frame, int x, int y) {
		return g(frame.getRGB(x, y));
	}
	
	private static int blue(BufferedImage frame, int x, int y) {
		return b(frame.getRGB(x, y));
	}
	
	private static BufferedImage resizeFrame(BufferedImage frame, int length, int height) {
		BufferedImage newFrame = new BufferedImage(length, height, BufferedImage.TYPE_4BYTE_ABGR);
		
		double stepx = (double)(frame.getWidth() - 1) / (double)length;
		double stepy = (double)(frame.getHeight() - 1) / (double)height;
		
		double xdiff, ydiff; xdiff = ydiff = 0;
		
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < length; j++) {
				int x = (int)(stepx * j);
				int y = (int)(stepy * i);
				
				xdiff = ((stepx * j) - x);
				ydiff = ((stepy * i) - y);
				
				int red = (int)((red(frame, x, y) * (1 - xdiff) * (1 - ydiff))
						+ (red(frame, x+1, y) * (1-ydiff) * xdiff)
						+ (red(frame, x, y+1) * ydiff * (1-xdiff))
						+ (red(frame, x+1, y+1) * ydiff * xdiff));
				
				int green = (int)((green(frame, x, y) * (1 - xdiff) * (1 - ydiff))
						+ (green(frame, x+1, y) * (1-ydiff) * xdiff)
						+ (green(frame, x, y+1) * ydiff * (1-xdiff))
						+ (green(frame, x+1, y+1) * ydiff * xdiff));
				
				int blue = (int)((blue(frame, x, y) * (1 - xdiff) * (1 - ydiff))
						+ (blue(frame, x+1, y) * (1-ydiff) * xdiff)
						+ (blue(frame, x, y+1) * ydiff * (1-xdiff))
						+ (blue(frame, x+1, y+1) * ydiff * xdiff));
				
				newFrame.setRGB(j, i, (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
			}
		}
		
		return newFrame;
	}
	
	public static void resize(JIF target, int targetLength, int targetHeight, Consumer<Double> progressFunction) {
		for(int i = 0; i < target.frames.length; i++) {
			target.frames[i] = resizeFrame(target.frames[i], targetLength, targetHeight);
			progressFunction.accept((double)i / (double)target.frames.length);
		}
	}
	
}
