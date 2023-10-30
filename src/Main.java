import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        String projectNumber = "11\\";
        String inputDirectory = "Projects\\" + projectNumber;
        String outputDirectory = "Output\\Project" + projectNumber;
        File[] filesList; // for use with CompilationEngine
        File[] xmlFilesList; // for use with FileCompare
        File toTranslate = new File(inputDirectory + args[0]); // filename is args[0]
        if (!toTranslate.exists()){
            throw new FileNotFoundException("Not found");
        }
        if (toTranslate.isDirectory()){
            try {
                Files.createDirectory(Path.of(outputDirectory + args[0])); // create output directory
            } catch (FileAlreadyExistsException ignored){}

            FileFilter jackFiles = (file) -> (file.getName().endsWith(".jack"));
            filesList = toTranslate.listFiles(jackFiles);

            FileFilter xmlFiles = (file) -> (file.getName().endsWith(".xml"));
            xmlFilesList = toTranslate.listFiles(xmlFiles);

        } else {
            filesList = new File[]{toTranslate};
            xmlFilesList = new File[]{toTranslate};
        }

        if (filesList != null) {
            CompilationEngine engine = new CompilationEngine();
            for (File f:filesList){
                System.out.println(f.getPath());
                engine.init(f.getPath(), inputDirectory, outputDirectory);
                engine.setGenerateTokenXML();
                engine.compileClass();
                engine.close();
            }
        }

        if (xmlFilesList != null){
            for (File x:xmlFilesList){
                FileCompare compare = new FileCompare(x.getPath().replace(inputDirectory, outputDirectory), x.getPath());
                compare.compare();
                compare.close();
            }
        }
    }
}
