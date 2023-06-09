package depixelate;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GeneratePix {

	
	/**
	 * These constants adjust the font settings in the gernerated images
	 * They all must be exactly correct in order to get a desired result
	 */
	private static final int FONT_SIZE = 48;
	private static final int FONT_STYLE = Font.ITALIC + Font.BOLD;
	private static final Color BACKGROUND = Color.black;
	private static final Color TEXT = Color.white;
	private static final String FONT_PATH = "fonts/stratford-serial-extrabold-regular.ttf"; //should be ttf but this can be adjusted when initialized 
	
	private final int WIDTH;
	private final int HEIGHT;
	
	private Graphics2D g;
	private BufferedImage buffIm;
	private int[] pixels;
	private Font f;
	private String fontName;
	
	/**
	 * number corresponding to the correct y offset in order for the text to just barely touch the top of the image
	 */
	private int top;

	public GeneratePix() {
		this(400,50);
	}
	
	public GeneratePix(int width, int height) {
		this.WIDTH = width;
		this.HEIGHT = height;
		buffIm = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		pixels = ((DataBufferInt) buffIm.getRaster().getDataBuffer()).getData();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			f = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH));
			ge.registerFont(f);
		} catch (FontFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fontName =  f.getName();
		findTop();
	}
	
	/**
	 * Finds the top of the text which will vary based on the font size
	 */
	private void findTop() {
		top = 0;
		for (int i = 1; i < HEIGHT; i++) {
			g = buffIm.createGraphics();
			g.setFont(new Font(fontName, FONT_STYLE, FONT_SIZE));
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setColor(TEXT);
			g.drawString("OOO||", 0, i);
			g.dispose();
			boolean foundText = false;
			for (int x = 0; x < WIDTH; x++) {
				//System.out.print((pixels[x] & 0xffffff) + ", ");
				if ((TEXT == Color.white && (pixels[x] & 0xffffff) != 0) || (TEXT == Color.black && (pixels[x] & 0xffffff) == 0 )) {
					foundText = true;
					break;
				}
			}
			//If there was no text seen in this pass it means that the text is not touching the top row
			if (!foundText) {
				top = i - 1;
				return;
			}
		}
	}
	
	/**
	 * Saves a reder with no offset and a blocksize of 12
	 * @param s
	 */
	public void render(String s) {
		renderTest(s, 0, 0, 12);
	}
	
	/**
	 * Saves a render with a default blockSize of 12 and other specified settings
	 * @param s
	 * @param offsetX
	 * @param offsetY
	 */
	public void renderTest(String s, int offsetX, int offsetY) {
		renderTest(s, offsetX, offsetY, 12);
	}
 	
	/**
	 * Renders and saves a string with the specified settings
	 * @param s
	 * @param offsetX
	 * @param offsetY
	 * @param blockSize
	 */
	public void renderTest(String s, int offsetX, int offsetY, int blockSize) {
		g = buffIm.createGraphics();
		g.setFont(new Font(fontName, FONT_STYLE, FONT_SIZE));
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(TEXT);
		g.drawString(s, offsetX, top + offsetY);
		g.dispose();
		String sFileName = s.replaceAll("[\\\\/:*?\"<>|]", "");

		File file = new File("generated\\" + sFileName + ".png");
		try {
			ImageIO.write(buffIm, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Saved at " + file.getAbsolutePath());
		pixilate(blockSize);
		file = new File("generated\\" + sFileName + "pix.png");
		try {
			ImageIO.write(buffIm, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Saved at " + file.getAbsolutePath());
	}
	
	/**
	 * Save the current buffered image
	 */
	public void saveRender() {
		File file = new File("generated\\" + "Saved" + ".png");
		try {
			ImageIO.write(buffIm, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Saved at " + file.getAbsolutePath());
	}
	
	public void render(String s, int offsetX, int offsetY, int blockSize) {
		g = buffIm.createGraphics();
		g.setFont(new Font(fontName, FONT_STYLE, FONT_SIZE));
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(TEXT);
		g.drawString(s, offsetX, top + offsetY);
		g.dispose();
	}
	
	public void pixilate(int blockSize) {
		int blocksX = WIDTH / blockSize + 1;
		int blocksY = HEIGHT / blockSize + 1;
		int pixel;
		
		
		for (int bx = 0; bx < blocksX; bx++) {
			for (int by = 0; by < blocksY; by++) {
				
				//probably use long for big blocksize
				int sumR = 0, sumG = 0, sumB = 0;
				
				for (int x = bx * blockSize; x < (bx + 1) *blockSize && x < WIDTH; x++) {
					for (int y = by * blockSize; y < (by + 1) *blockSize && y < HEIGHT; y++) {
						pixel = pixels[x + y * WIDTH];
						sumR += (pixel & 0xFF0000) >> 16;
						sumG += (pixel & 0x00ff00) >> 8;
						sumB += pixel & 0x0000ff;
					}
				}
				int aveR, aveG, aveB, aveColor;
				aveR = (int) (((double) sumR) / (blockSize * blockSize));
				aveG = (int) (((double) sumG) / (blockSize * blockSize));
				aveB = (int) (((double) sumB) / (blockSize * blockSize));
				aveColor = (aveR << 16) ^ (aveG << 8) ^ aveB;
				for (int x = bx * blockSize; x < (bx + 1) *blockSize && x < WIDTH; x++) {
					for (int y = by * blockSize; y < (by + 1) *blockSize && y < HEIGHT; y++) {
						pixels[x + y * WIDTH] = aveColor;
					}
				}
			}
		}
	}
	
	public int[] getPixels() {
		return pixels;
	}
	
	public int getTop() {
		return top;
	}
	
	public BufferedImage getBufferedImage() {
		return buffIm;
	}
}
