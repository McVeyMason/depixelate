package depixelate;

public class Main {

	private static final String IMAGE_LOCATION = "images\\large.png";
	private static final int BLOCK_SIZE = 12;
	
	public static void main(String[] args) {
		GeneratePix g = new GeneratePix();
		g.render("Hello World"); //test rendering a string
		
		//run the main program
		ComparePix comp = new ComparePix(IMAGE_LOCATION, BLOCK_SIZE);
		comp.runCompare();
	}

}
