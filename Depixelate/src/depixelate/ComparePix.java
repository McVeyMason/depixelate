package depixelate;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Stack;

import javax.imageio.ImageIO;

public class ComparePix {
	
	/**
	 * To get the best result, this value must be adusted to get the desired size of the linked list 'guesses'
	 * This list shouldn't be too big as to slow down the computation, although a bigger list will allow
	 * the program to adjust when an incorrect string does better at first compared to the correct string.
	 */
	private static final double PERCENT_ERROR = 0.1;
	
	private static final String TEST_LETTERS = " .ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	/**
	 * Try two characters together first?
	 */
	private static final boolean TRY_FIRST_TWO = true;
	

	//The Width of the | character in the current font
	private static final int VERTICAL_BAR_WIDTH = 15;
	
	private GeneratePix gen;
	private BufferedImage input;
	private final int[] inputPixels;
	private final int blockSize;
	private LinkedList<CompareAttempt> guesses; //Stores 'close' guesses
	
	public ComparePix(String file, int blockSize) {
		guesses = new LinkedList<>();
		
		this.blockSize = blockSize;
		input = null;
		try {
		    input = ImageIO.read(new File(file));
		} catch (IOException e) {
			
		}
		inputPixels  = input.getRGB(0, 0, input.getWidth(), input.getHeight(), null, 0, input.getWidth());
		gen = new GeneratePix(input.getWidth(), input.getHeight());
		System.out.println(input.getWidth());
		System.out.println(input.getHeight());
	}
	
	public void runCompare() {
		compareAll();
	}
	
	private static class CompareAttempt implements Comparable<CompareAttempt>{
		private String s;
		private double aveDiff;
		private int offsetX;
		private int offsetY;
		private int numBlocks;
		
		private CompareAttempt(String s, double aveDiff, int offsetX, int offsetY, int numBlocks) {
			this.s = s;
			this.aveDiff = aveDiff;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.numBlocks = numBlocks;
		}
		
		public String toString() {
			return "String = " + s + ", Ave Diff is " + aveDiff + ", offsetX = " + offsetX + ", offsetY = " + offsetY + ", num Blocks = " + numBlocks; 
		}

		public double getScore() {
			return ComparePix.getScore(numBlocks, aveDiff);
		}
		
		public int compareTo(CompareAttempt o) {
			return (int) (getScore() - o.getScore());
		}
		
		
	}
	
	private void compareAll() {
		guesses = new LinkedList<>();
		String s = ""; //enter a starting word or letters here
		CompareAttempt best = null;
		
		//first we do a wide search for the first two characters at once using various offsets. 
		//For the sample image I used the max offset as blocksize /4 but it can be anything
		double bestScore = 0.0;
		for (int offsetX = 0; offsetX < blockSize / 4; offsetX++) {
			for (int offsetY = 0; offsetY < blockSize / 4; offsetY++) {
				if (TRY_FIRST_TWO) {
					for (int i = 0; i < TEST_LETTERS.length(); i++) {
						for (int j = 0; j < TEST_LETTERS.length(); j++) {//try two letters first to ensure better stack
							CompareAttempt c = compare(s + TEST_LETTERS.charAt(i) + TEST_LETTERS.charAt(j), offsetX, offsetY);
							double score = getScore(c.numBlocks, c.aveDiff);
							if (score > bestScore) {
								bestScore = score;
								guesses.push(c);
								best = c;
							} else if ((bestScore - score) / bestScore < PERCENT_ERROR) {
								guesses.push(c);
							}
						}
					}
				} else {
					for (int j = 0; j < TEST_LETTERS.length(); j++) {//try two letters first to ensure better stack
						CompareAttempt c = compare(s + TEST_LETTERS.charAt(j), offsetX, offsetY);
						double score = getScore(c.numBlocks, c.aveDiff);
						if (score > bestScore) {
							bestScore = score;
							guesses.push(c);
							best = c;
						} else if ((bestScore - score) / bestScore < PERCENT_ERROR) {
							guesses.push(c);
						}
					}
				}
			}
		}
		final double b = bestScore;
		guesses.removeIf((c) -> ((b - getScore(c.numBlocks, c.aveDiff)) / b > PERCENT_ERROR)); //clean up linked list
		guesses.sort((o1, o2) -> o1.compareTo(o2));
		
		System.out.println(best);
		renderCompare(best);
		guesses.push(best); //ensure it is on the top
		
		System.out.println(guesses.size());
		
		//after the first pass finding the first two characters, we search one character at a time
		while((best.numBlocks + 4) * blockSize < input.getWidth()) {
			bestScore = 0.0;
			LinkedList<CompareAttempt> nextGuesses = new LinkedList<>();
			CompareAttempt current;
			
			while(!guesses.isEmpty()) {
				//Treat the linked list like a stack as
				current = guesses.pop();
				s = current.s;
				int offsetX = current.offsetX, offsetY =current.offsetY;
				for (int i = 0; i < TEST_LETTERS.length(); i++) {
					CompareAttempt c = compare(s + TEST_LETTERS.charAt(i), offsetX, offsetY);
					double score = getScore(c.numBlocks, c.aveDiff);
					if (score > bestScore) {
						bestScore = score;
						nextGuesses.push(c);
						best = c;
					} else if ((bestScore - score) / bestScore < PERCENT_ERROR) {
						nextGuesses.push(c);
					}
				}
			}
			final double currentBest = bestScore;
			guesses = nextGuesses; //update guesses
			
			guesses.removeIf((c) -> ((currentBest - getScore(c.numBlocks, c.aveDiff)) / currentBest > PERCENT_ERROR));
			if (best.s.length() > 6)
				guesses.removeIf((c) -> ((currentBest - getScore(c.numBlocks, c.aveDiff)) / currentBest > PERCENT_ERROR / 2));
			guesses.sort((o1, o2) -> o1.compareTo(o2));
			System.out.println(best);
			renderCompare(best);
			guesses.push(best); //ensure best is on the top
			System.out.println(guesses.size());
		}
		
	}
	
	/**
	 * Saves a compare attempt to the generated folder
	 * @param attempt
	 */
	private void renderCompare(CompareAttempt attempt) {
		gen.renderTest(attempt.s, attempt.offsetX, attempt.offsetY, blockSize);
	}
	
	
	/**
	 * Compares a string to the pixilated image
	 * @param s
	 * @param offsetX
	 * @param offsetY
	 * @return
	 */
	private CompareAttempt compare(String s, int offsetX, int offsetY) {
		
		/**
		 * Generates a non pixilated image with a veritcal bar at the end in order to see
		 * how many blocks to analyze.
		 */
		gen.render(s+ "|", offsetX, offsetY, blockSize);
		int[] pixels = gen.getPixels();
		int numBlocksCompare = 1;
		for (int i = input.getWidth() - 1; i >= 0; i--) {
			if ((pixels[offsetY* input.getWidth() + i] & 0xffffff) != 0) {
				numBlocksCompare = (i - VERTICAL_BAR_WIDTH) / blockSize;
				break;
			}
		}
		
		//Now we pixilate the image, no need to re render without the bar because it won't be included
		gen.pixilate(blockSize);
		pixels = gen.getPixels();
		
		//sum the square difference between the two 
		double sumDiff = 0;
		for (int x = 0; x < numBlocksCompare * blockSize; x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				int diffR, diffG, diffB;
				diffR = (0xff & (pixels[y * input.getWidth() + x] >> 16)) - (0xff & (inputPixels[y * input.getWidth() + x] >> 16)); 
				diffR *= diffR;
				diffG = (0xff & (pixels[y * input.getWidth() + x] >> 8)) - (0xff & (inputPixels[y * input.getWidth() + x] >> 8)); 
				diffG *= diffG;
				diffB = (0xff & pixels[y * input.getWidth() + x]) - (0xff & inputPixels[y * input.getWidth() + x]); 
				diffB *= diffB;
				sumDiff += (double) (diffR + diffG + diffB) / (3.0 * 0xff * 0xff);
			}
		}
		
		double aveDiff = Math.sqrt(sumDiff) / (double)((numBlocksCompare) * blockSize * input.getHeight());
		return new CompareAttempt(s, aveDiff, offsetX, offsetY, numBlocksCompare);
	}
	
	/**
	 * Calculated the 'score' of a render based on the number of blocks and the average difference.
	 * This value is maximized so it must be inversely proportional to the average difference.
	 * @param numBlocks
	 * @param aveDiff
	 * @return
	 */
	private static double getScore(int numBlocks, double aveDiff) {
		return ((double) aveDiff) / Math.pow(aveDiff, 5); //inverse difference to get the score, weighted by the number of blocks. Changing the power changes the behavior
	}
}
