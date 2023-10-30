import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class FileCompare {
    private final Scanner inputReader;
    private final Scanner comparisonReader;
    private final FileWriter output;
    private int lineNumber = 0;


    public FileCompare(String inputFileName, String comparisonFileName) throws IOException {
        inputReader = new Scanner(new File(inputFileName));
        comparisonReader = new Scanner(new File(comparisonFileName));
        output = new FileWriter(inputFileName.replace(".xml","Comparison.txt"));
    }

    public void compare() throws IOException {
        while (inputReader.hasNext() && comparisonReader.hasNext()){
            lineNumber++;
            String inputLine = inputReader.nextLine().strip();
            String comparisonLine = comparisonReader.nextLine().strip();
            if (!inputLine.equals(comparisonLine)){
                output.write("Mismatch on line " + lineNumber);
                return;
            }
        }
        output.write("Files are identical!");
    }

    public void close() throws IOException {
        inputReader.close();
        comparisonReader.close();
        output.close();
    }
}
