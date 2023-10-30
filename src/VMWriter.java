import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
    private final FileWriter vmWriter;
    private String padding = "    ";

    public VMWriter(String fileName) throws IOException {
        vmWriter = new FileWriter(fileName);
    }

    public void close() throws IOException {
        vmWriter.close();
    }

    public void writePush(Segment seg, int index) throws IOException {
        vmWriter.write(padding + "push " + seg.toString().toLowerCase() + " " + index + "\n");
    }

    public void writePop(Segment seg, int index) throws IOException {
        vmWriter.write(padding + "pop " + seg.toString().toLowerCase() + " " + index + "\n");
    }

    public void writeArithmetic(Ops operation) throws IOException {
        vmWriter.write(padding + operation.toString().toLowerCase() + "\n");
    }

    public void writeLabel(String label) throws IOException {
        vmWriter.write("label " + label + '\n');
    }

    public void writeGoTo(String label) throws IOException {
        vmWriter.write(padding + "goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        vmWriter.write(padding + "if-goto " + label + "\n");
    }

    public void writeCall(String name, int args) throws IOException {
        vmWriter.write(padding + "call " + name + " " + args + "\n");
    }

    public void writeFunction(String name, int nVars) throws IOException {
        vmWriter.write(padding + "\nfunction " + name + " " + nVars +  "\n");
    }

    public void writeReturn() throws IOException {
        vmWriter.write(padding + "return\n");
    }

    public void writeAsComment(String commentText) throws IOException {
        vmWriter.write("//" + commentText + "\n");
    }

    public void write(String string) throws IOException {
        vmWriter.write(string);
    }
}
