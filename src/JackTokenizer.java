import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JackTokenizer {


    private final StringBuilder currentToken = new StringBuilder();
    private int readerIntBuffer;
    private char currentChar;
    private char nextChar;
    private int lineNumber = 1;

    private final FileReader reader;
    private final FileWriter tokenTagWriter;
    private  VMWriter vmWriter;

    private final ArrayList<Character> symbols = new ArrayList<>(List.of(
            '{','}','(',')','[',']','.',',',';','+','-','*','/','&','|','<','>','=','~'));
    private final HashMap<String,String> symbolConversion = new HashMap<>();
    private final ArrayList<String> keywords = new ArrayList<>(List.of(
            "class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean",
            "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"));

    private TokenType type = TokenType.NONE;

    public JackTokenizer(String fileName, String inputDirectory, String outputDirectory) throws IOException {
        symbolConversion.put("<","&lt;");
        symbolConversion.put(">","&gt;");
        symbolConversion.put("\"","&quot;");
        symbolConversion.put("&","&amp;");

        reader = new FileReader(fileName);
        String output = fileName.replace(inputDirectory, outputDirectory);
        tokenTagWriter = new FileWriter(output.replace(".jack", "T.xml"));
        tokenTagWriter.write("<tokens>\n");

        currentChar = (char) reader.read();
        readerIntBuffer = reader.read();
        nextChar = (char) readerIntBuffer;
        skipWhiteSpace();

    }

    public void setVMWriter(VMWriter vmWriter){
        this.vmWriter = vmWriter;
    }

    /**
     * Loads the next token of the input file to the current token String
     * @throws IOException if reader encounters an error
     */
    public void advance() throws IOException {
        currentToken.delete(0, currentToken.length());
        boolean tokenFound = false;
        while (!tokenFound && readerIntBuffer != -1) {
            type = TokenType.NONE;
            if (currentChar == '/' && nextChar == '/') {
                // handle comments
                while (currentChar != '\n' && readerIntBuffer != -1) {
                    step();
                }
                step();
                skipWhiteSpace();
                //vmWriter.write(currentToken.toString());
                currentToken.delete(0, currentToken.length());
            } else if (currentChar == '/' && nextChar == '*') {
                while (currentChar != '*' || nextChar != '/') {
                    step();
                }
                step();
                step();
                skipWhiteSpace();
                vmWriter.write(currentToken.toString());
                currentToken.delete(0, currentToken.length());
            } else if (currentChar == '"') {
                // Handle string constants
                loadStringToken();
                tokenFound = true;
            } else if (symbols.contains(currentChar)) {
                // handle symbols
                type = TokenType.SYMBOL;
                tokenFound = true;
                step();
            } else {
                // load until whitespace or symbol
                loadText();
                searchToken(currentToken.toString());
                tokenFound = true;
            }

        }
        debugPrintTokenToConsole();
        skipWhiteSpace();
    }

    public String getTokenString(){
        return String.valueOf(currentToken);
    }

    public void appendToToken(String s){
        currentToken.append(s);
    }

    public TokenType getType() {
        return type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public char symbol(){
        if (type == TokenType.SYMBOL && readerIntBuffer != -1) {
            return currentToken.toString().charAt(0);
        } else if (readerIntBuffer == -1){
            return '}';
        } else {
            throw new IllegalArgumentException("Invalid token type on line: " + lineNumber + ", \n" +
                    "expected: SYMBOL received: " + currentToken + " (" + type + ")");
        }
    }

    public String identifier(){
        if (type == TokenType.IDENTIFIER) {
            return currentToken.toString();
        } else {
            throw new IllegalArgumentException("Invalid token type on line: " + lineNumber + ", \n" +
                    "expected: IDENTIFIER received: " + currentToken + " (" + type + ")");
        }
    }

    public String keyword(){
        if (type == TokenType.KEYWORD) {
            return currentToken.toString();
        } else {
            throw new IllegalArgumentException("Invalid token type on line: " + lineNumber + ", \n" +
                    "expected: KEYWORD received: " + currentToken + " (" + type + ")");
        }
    }

    public int intVal(){
        if (type == TokenType.INT_CONSTANT) {
            return Integer.parseInt(currentToken.toString());
        } else {
            throw new IllegalArgumentException("Invalid token type on line: " + lineNumber + ", \n" +
                    "expected: INT_CONSTANT received: " + currentToken + " (" + type + ")");
        }
    }

    public String stringVal(){
        if (type == TokenType.STR_CONSTANT){
            return currentToken.toString().replace("\"","");
        } else {
            throw new IllegalArgumentException("Invalid token type on line: " + lineNumber + ", \n" +
                    "expected: STR_CONSTANT received: " + type);
        }
    }

    private void loadStringToken() throws IOException {
        type = TokenType.STR_CONSTANT;
        step();
        while (currentChar != '"') {
            if (currentChar == '\n') {
                // Strings cannot contain new lines
                throw new IllegalArgumentException("Invalid String declaration on line " + lineNumber);
            }
            step();
        }
        step();
    }

    private void loadText() throws IOException {
        while (!Character.isWhitespace(currentChar) && !symbols.contains(currentChar) && readerIntBuffer != -1){
            step();
        }
    }

    private void searchToken(String tokenString){
        if (keywords.contains(tokenString)){
            type = TokenType.KEYWORD;
        } else if (Character.isDigit(tokenString.charAt(0))){
            try {
                Integer.parseInt(tokenString);
                type = TokenType.INT_CONSTANT;
            } catch (NumberFormatException e){
                throw new IllegalArgumentException("Invalid integer constant on line " + lineNumber);
            }
        } else if (validIdentifier(tokenString)){
            type = TokenType.IDENTIFIER;
        } else {
            throw new IllegalArgumentException("Unrecognized token on line " + lineNumber);
        }
    }

    private boolean validIdentifier(String s){
        for (int i = 0; i < s.length(); i++){
            char c  = s.charAt(i);
            if (!Character.isDigit(c) && !Character.isAlphabetic(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * Appends the current character to the token and reads the next character
     * @throws IOException if reader encounters an error
     */
    private void step() throws IOException {
        // loads next char into token and updates both current and next char
        currentToken.append(currentChar);

        readerIntBuffer = reader.read();
        char previousChar = currentChar;
        currentChar = nextChar;
        nextChar = (char) readerIntBuffer;
        if (previousChar == '\n'){
            lineNumber++;
        }
    }

    private void skipWhiteSpace() throws IOException {
        while (Character.isWhitespace(currentChar)){
            if (currentChar == '\n'){
                lineNumber++;
            }
            readerIntBuffer = reader.read();
            currentChar = nextChar;
            nextChar = (char) readerIntBuffer;
        }
    }

    public void writeTokenToFile() throws IOException {

        String tokenTypeString = null;
        String tokenString = String.valueOf(currentToken); // current token is a stringBuilder, need string

        if (tokenString.isBlank()){
            return;
        }

        switch (type){
            case KEYWORD -> tokenTypeString = "keyword";
            case IDENTIFIER -> tokenTypeString = "identifier";
            case STR_CONSTANT -> {
                tokenTypeString = "stringConstant";
                tokenString = tokenString.replace("\"","");
            }
            case INT_CONSTANT -> tokenTypeString = "integerConstant";
            case SYMBOL -> {
                tokenTypeString = "symbol";
                if (symbolConversion.containsKey(tokenString)) {
                    tokenString = symbolConversion.get(tokenString);
                }
            }
        }
        tokenTagWriter.write("<" + tokenTypeString + "> " + tokenString + " </" + tokenTypeString + ">\n");
    }

    public void close() throws IOException {
        reader.close();
        tokenTagWriter.write("</tokens>\n");
        tokenTagWriter.close();
    }

    public void debugPrintTokenToConsole(){
        if (!currentToken.isEmpty()) {
            System.out.println("<<" + currentToken + ">>" + " @L" + lineNumber + " type: " + type);
        }
    }
}

