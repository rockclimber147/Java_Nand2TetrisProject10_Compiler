import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CompilationEngine {

    private enum subroutineType{
        CONSTRUCTOR,
        FUNCTION,
        METHOD
    }

    private enum varOp{
        PUSH,
        POP
    }

    private subroutineType currentSubroutineType = null;

    private FileWriter xmlWriter;
    private VMWriter vmWriter;
    private JackTokenizer tokenizer;


    private final SymbolTable classLevelSymbols = new SymbolTable();
    private final SymbolTable subroutineLevelSymbols = new SymbolTable();


    private TokenType currentTokenType;
    private String currentClass = null;
    private boolean generateTokenXML = false;
    private int indentCount;
    private final String indents = "    ";

    private final HashMap<Character,String> symbolRepresentationXML = new HashMap<>();
    private final ArrayList<String> classVarDecKeywords = new ArrayList<>(List.of("static","field"));
    private final ArrayList<String> typeKeywords = new ArrayList<>(List.of("int","char","boolean", "Array"));
    private final ArrayList<String> subroutineDecKeywords = new ArrayList<>(List.of("constructor","function","method"));

    private final ArrayList<Character> unaryOp = new ArrayList<>(List.of('-','~'));
    private final HashMap<Character, Ops> unaryOpsTable = new HashMap<>();

    private final ArrayList<Character> op = new ArrayList<>(List.of('+','-','*','/','&','|','<','>','='));
    private final HashMap<Character, Ops> opsTable = new HashMap<>();

    private final ArrayList<String> keywordConstant = new ArrayList<>(List.of("true","false","null","this"));

    public CompilationEngine() throws IOException {
        indentCount = 0;


        symbolRepresentationXML.put('<',"&lt;");
        symbolRepresentationXML.put('>',"&gt;");
        symbolRepresentationXML.put('\"',"&quot;");
        symbolRepresentationXML.put('&',"&amp;");

        opsTable.put('+', Ops.ADD);
        opsTable.put('-', Ops.SUB);
        opsTable.put('&', Ops.AND);
        opsTable.put('|', Ops.OR);
        opsTable.put('<', Ops.LT);
        opsTable.put('>', Ops.GT);
        opsTable.put('=', Ops.EQ);

        unaryOpsTable.put('-', Ops.NEG);
        unaryOpsTable.put('~', Ops.NOT);
    }

    /**
     * Starts the compilation engine for a single file
     * @param fileName the relative file path of the file in the main project directory
     * @param inputDirectory The directory containing the files to translate
     * @param outputDirectory The output directory for the translated files
     * @throws IOException if file not found
     */
    public void init(String fileName, String inputDirectory, String outputDirectory) throws IOException {
        tokenizer = new JackTokenizer(fileName, inputDirectory, outputDirectory);

        String output = fileName.replace(inputDirectory, outputDirectory);
        xmlWriter = new FileWriter(output.replace(".jack",".xml"));
        vmWriter = new VMWriter(output.replace(".jack",".vm"));
        tokenizer.setVMWriter(vmWriter);
    }
    public void setGenerateTokenXML(){
        generateTokenXML = true;
    }

    public void close() throws IOException {
        tokenizer.close();
        xmlWriter.close();
        vmWriter.close();
    }
    private void advance() throws IOException {
        tokenizer.advance();
        if (generateTokenXML){
            tokenizer.writeTokenToFile();
        }
        currentTokenType = tokenizer.getType();
    }

    private void eatTerminalSymbol(char c) throws IOException {
        char t = tokenizer.symbol();
        if (c != t){
            throw new IllegalArgumentException("Exception in class " + currentClass + " Unexpected token on line " + tokenizer.getLineNumber() + "\n" +
                    "expected: " + c + " received: " + t);
        }
        writeTerminalSymbolXML(t);
        advance();
    }
    private void writeTerminalSymbolXML(char symbol) throws IOException {
        String xmlString = indents.repeat(indentCount) + "<symbol> ";
        if (symbolRepresentationXML.containsKey(symbol)){
            xmlString += symbolRepresentationXML.get(symbol);
        } else {
            xmlString += symbol;
        }
        xmlWriter.write(xmlString + " </symbol>\n");
    }

    private void eatTerminalKeyword(String keyword) throws IOException {
        String s = tokenizer.keyword();
        if (!s.equals(keyword)){
            throw new IllegalArgumentException("Exception in class " + currentClass + "Unexpected token on line " + tokenizer.getLineNumber() + "\n" +
                    "expected: " + keyword + " received: " + s);
        }
        writeTerminalKeywordXML(keyword);
        advance();
    }
    private void writeTerminalKeywordXML(String keyword) throws IOException {
        xmlWriter.write(indents.repeat(indentCount) + "<keyword> " + keyword + " </keyword>\n");
    }

    private void eatTerminalIntegerConstant() throws IOException {
        int intConstant = tokenizer.intVal();
        writeTerminalIntConstantXML(intConstant);
        advance();
    }
    private void writeTerminalIntConstantXML(int intConstant) throws IOException {
        xmlWriter.write(indents.repeat(indentCount) + "<integerConstant> " + intConstant + " </integerConstant>\n");
    }

    private void eatTerminalStringConstant() throws IOException {
        String stringConstant = tokenizer.stringVal();
        writeTerminalStringConstantXML(stringConstant);
        advance();
    }
    private void writeTerminalStringConstantXML(String stringConstant) throws IOException {
        xmlWriter.write(indents.repeat(indentCount) + "<stringConstant> " + stringConstant + " </stringConstant>\n");
    }

    private void eatTerminalIdentifier() throws IOException {
        String identifier = tokenizer.identifier();
        writeTerminalIdentifierXML(identifier);
        advance();
    }
    private void writeTerminalIdentifierXML(String identifier) throws IOException {
        xmlWriter.write(indents.repeat(indentCount) + "<identifier> " + identifier + " </identifier>\n");
    }

    private void writeOpenTag(String tag) throws IOException {
        xmlWriter.write(indents.repeat(indentCount) + "<" + tag + ">\n");
        indentCount++;
    }
    private void writeCloseTag(String tag) throws IOException {
        indentCount--;
        xmlWriter.write(indents.repeat(indentCount) + "</" + tag + ">\n");
    }

    /**
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() throws IOException {

        classLevelSymbols.reset(); // reset SymbolTable at class level

        advance();
        String tag = "class";
        writeOpenTag(tag);
        eatTerminalKeyword("class");

        currentClass = tokenizer.getTokenString(); // store class name as it's a datatype that will be used later

        eatTerminalIdentifier();
        eatTerminalSymbol('{');
        while (currentTokenType == TokenType.KEYWORD && classVarDecKeywords.contains(tokenizer.keyword())){
            compileClassVarDec();
        }

        vmWriter.write(classLevelSymbols.getFormattedTable());

        while (currentTokenType == TokenType.KEYWORD && subroutineDecKeywords.contains(tokenizer.keyword())){
            compileSubroutineDec();
        }
        eatTerminalSymbol('}');
        writeCloseTag(tag);
    }
    /**
     * ('static'|'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() throws IOException {
        String tag = "classVarDec";
        writeOpenTag(tag);

        String classVarKeyword = tokenizer.getTokenString(); // store keyword (kind) for symbolTable
        // 'static' or 'field' keywords handled in calling function
        eatTerminalKeyword(tokenizer.keyword());

        String classVarType = tokenizer.getTokenString(); // store varType
        // type -> keyword('int' | 'char' | 'boolean') | identifier
        compileType();

        String classVarName = tokenizer.getTokenString();

        classLevelSymbols.define(classVarName, classVarType, SymbolTable.kind.valueOf(classVarKeyword.toUpperCase()));

        tokenizer.appendToToken(classLevelSymbols.getPropertiesOfVar(classVarName));

        // varName
        eatTerminalIdentifier();

        // add var to SymbolTable

        // (',' varName)*
        while (currentTokenType == TokenType.SYMBOL && tokenizer.symbol() == ','){
            eatTerminalSymbol(',');

            classVarName = tokenizer.getTokenString();
            classLevelSymbols.define(classVarName, classVarType,
                    SymbolTable.kind.valueOf(classVarKeyword.toUpperCase()));

            eatTerminalIdentifier();
        }

        // ';'
        eatTerminalSymbol(';');
        writeCloseTag(tag);
    }
    /**
     * ('constructor'|'function'|'method') ('void'|type) subroutineName '(' parameterList ')' subroutineBody
     */
    public void compileSubroutineDec() throws IOException {
        subroutineLevelSymbols.reset(); // reset subroutine SymbolTable upon entering new subroutine
        String tag = "subroutineDec";
        writeOpenTag(tag);
        // ('constructor' | 'function '|' 'method') handled in calling function

        String subroutineTypeString = tokenizer.getTokenString(); // Set subroutine type
        switch (subroutineTypeString){
            case "function" -> currentSubroutineType = subroutineType.FUNCTION;
            case "method" -> {
                currentSubroutineType = subroutineType.METHOD;
                subroutineLevelSymbols.define("this", currentClass, SymbolTable.kind.ARG);
            }
            case "constructor" -> currentSubroutineType = subroutineType.CONSTRUCTOR;
        }

        eatTerminalKeyword(tokenizer.keyword());

        //('void' | type) -> ('void' | 'int' | 'boolean' | 'char' | identifier)
        // 'void'
        if (currentTokenType == TokenType.KEYWORD && tokenizer.keyword().equals("void")){
            eatTerminalKeyword("void");
        }  else {
            // type
            compileType();
        }
        // store subroutineName
        subroutineLevelSymbols.setName(tokenizer.identifier());
        eatTerminalIdentifier();
        // '('
        eatTerminalSymbol('(');
        // parameterList OPTIONAL
        compileParameterList();
        // ')'
        eatTerminalSymbol(')');

        compileSubroutineBody();

        writeCloseTag(tag);
    }
    /**
     * ((type varName) (',' type varName)*)? (? handled in calling function)
     */
    public void compileParameterList() throws IOException {
        String tag = "parameterList";
        writeOpenTag(tag);
        if ((currentTokenType == TokenType.KEYWORD) || (currentTokenType == TokenType.IDENTIFIER)) { // Arrays are IDENTIFIERS
            // type
            String argType = tokenizer.getTokenString(); // store argument type
            compileType();
            // varName
            String argName = tokenizer.getTokenString(); // store argument name
            subroutineLevelSymbols.define(argName, argType, SymbolTable.kind.ARG); // store argument in symbol table
            tokenizer.appendToToken(subroutineLevelSymbols.getPropertiesOfVar(argName)); // print to xml
            eatTerminalIdentifier();
            // (',' type varName)*
            while (tokenizer.symbol() == ',') {
                // ','
                eatTerminalSymbol(',');
                // type
                argType = tokenizer.getTokenString(); // store argument type
                compileType();
                // varName
                argName = tokenizer.getTokenString(); // store argument name
                subroutineLevelSymbols.define(argName, argType, SymbolTable.kind.ARG);
                tokenizer.appendToToken(subroutineLevelSymbols.getPropertiesOfVar(argName));
                eatTerminalIdentifier();
            }
        }
        writeCloseTag(tag);
    }
    /**
     * '{' varDec* statements '}'
     */
    public void compileSubroutineBody() throws IOException {
        String tag = "subroutineBody";
        writeOpenTag(tag);
        // '{'
        eatTerminalSymbol('{');
        // varDec*
        while (currentTokenType == TokenType.KEYWORD && tokenizer.keyword().equals("var")){
            compileVarDec();
        }

        // Use class name and subroutine name for function calls
        vmWriter.writeFunction(currentClass + "." + subroutineLevelSymbols.getName(),
                subroutineLevelSymbols.varCount(SymbolTable.kind.VAR));

        vmWriter.write(subroutineLevelSymbols.getFormattedTable());

        if (currentSubroutineType == subroutineType.CONSTRUCTOR){ // if the subroutine is a constructor
            // make room in memory for every field variable
            vmWriter.writePush(Segment.CONSTANT, classLevelSymbols.varCount(SymbolTable.kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            // set THIS to the address in memory returned by alloc
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (currentSubroutineType == subroutineType.METHOD){
            vmWriter.writePush(Segment.ARGUMENT, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        }
        // statements
        if (currentTokenType == TokenType.KEYWORD) {
            compileStatements();
        }
        // '}'
        eatTerminalSymbol('}');
        writeCloseTag(tag);
    }
    /**
     *  'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() throws IOException {
        String tag = "varDec";
        writeOpenTag(tag);
        // 'var'
        eatTerminalKeyword("var");
        // type
        String subroutineVarType = tokenizer.getTokenString();
        compileType();
        // varName
        String subroutineVarName = tokenizer.getTokenString();
        subroutineLevelSymbols.define(subroutineVarName, subroutineVarType, SymbolTable.kind.VAR);
        tokenizer.appendToToken(subroutineLevelSymbols.getPropertiesOfVar(subroutineVarName));
        eatTerminalIdentifier();
        //(',' varName)*
        while (tokenizer.symbol() == ','){
            eatTerminalSymbol(',');
            subroutineVarName = tokenizer.getTokenString();
            subroutineLevelSymbols.define(subroutineVarName, subroutineVarType, SymbolTable.kind.VAR);
            tokenizer.appendToToken(subroutineLevelSymbols.getPropertiesOfVar(subroutineVarName));
            eatTerminalIdentifier();
        }
        // ';'
        eatTerminalSymbol(';');
        writeCloseTag(tag);
    }
    /**
     * statement* -> (letStatement | ifStatement | whileStatement | doStatement | returnStatement)*
     */
    public void compileStatements() throws IOException {
        String tag = "statements";
        writeOpenTag(tag);
        while (currentTokenType == TokenType.KEYWORD){
            if (tokenizer.keyword().equals("let")){
                compileLet();
            } else if (tokenizer.keyword().equals("if")){
                compileIf();
            } else if (tokenizer.keyword().equals("while")){
                compileWhile();
            } else if (tokenizer.keyword().equals("do")){
                compileDo();
            } else if (tokenizer.keyword().equals("return")){
                compileReturn();
            }
        }
        writeCloseTag(tag);
    }
    /**
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() throws IOException {
        String tag = "letStatement";
        writeOpenTag(tag);
        // 'let'
        eatTerminalKeyword("let");
        // varName ('[' expression ']')?

        String varName = tokenizer.identifier(); // store varName
        boolean varIsArray = false;

        eatTerminalIdentifier();

        if (tokenizer.symbol() == '['){ // Handle Arrays

            varIsArray = true;
            handleVariableInTables(varName, varOp.PUSH); // push array

            eatTerminalSymbol('[');
            compileExpression();                         // push [expression]
            eatTerminalSymbol(']');
            vmWriter.writeArithmetic(Ops.ADD);           // add
        }
        // '='
        eatTerminalSymbol('=');
        compileExpression();                             // expression is now at top of stack

        if (varIsArray){
            vmWriter.writePop(Segment.TEMP, 0);    // store expression 2 in temp
            vmWriter.writePop(Segment.POINTER, 1); // set THAT to array[expression]
            vmWriter.writePush(Segment.TEMP, 0);   // retrieve expression 2
            vmWriter.writePop(Segment.THAT, 0);    // THAT 0 (array[expression]) is now expression 2
        } else {
            handleVariableInTables(varName, varOp.POP);  // pop to desired address
        }

        // ';'
        eatTerminalSymbol(';');
        writeCloseTag(tag);
    }
    /**
     * 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements' '}')?
     */
    public void compileIf() throws IOException {
        String tag = "ifStatement";
        writeOpenTag(tag);
        eatTerminalKeyword("if");
        eatTerminalSymbol('(');
        compileExpression(); // Expression is pushed to stack
        eatTerminalSymbol(')');

        int ifElseCount = subroutineLevelSymbols.getIfElseCount(); // get if/else count for this scope
        subroutineLevelSymbols.incrementIfElseCount(); // increment for the next scope

        vmWriter.writeArithmetic(Ops.NOT); // negate
        // goto label if statement is false (true after negation)
        vmWriter.writeIf(currentClass + "." + subroutineLevelSymbols.getName() + "_FALSE_"
                + ifElseCount);

        eatTerminalSymbol('{');
        compileStatements();

        vmWriter.writeGoTo(currentClass + "." + subroutineLevelSymbols.getName() + "_TRUE_"
                + ifElseCount);
        vmWriter.writeLabel(currentClass + "." + subroutineLevelSymbols.getName() + "_FALSE_"
                + ifElseCount);

        eatTerminalSymbol('}');
        // ( 'else' '{' statements ']')?
        if (currentTokenType == TokenType.KEYWORD && tokenizer.keyword().equals("else")){
            eatTerminalKeyword("else");
            eatTerminalSymbol('{');
            compileStatements();
            eatTerminalSymbol('}');
        }
        vmWriter.writeLabel(currentClass + "." + subroutineLevelSymbols.getName() + "_TRUE_"
                + ifElseCount);
        writeCloseTag(tag);
    }
    /**
     *  whileStatement:  'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() throws IOException {
        String tag = "whileStatement";
        writeOpenTag(tag);
        eatTerminalKeyword("while");

        int loopCount = subroutineLevelSymbols.getLoopCount();
        subroutineLevelSymbols.incrementLoopCount();

        vmWriter.writeLabel(currentClass + "." + subroutineLevelSymbols.getName() + "_LOOP_START_"
                + loopCount);

        eatTerminalSymbol('(');
        compileExpression(); // Expression pushed to stack
        eatTerminalSymbol(')');

        vmWriter.writeArithmetic(Ops.NOT);
        vmWriter.writeIf(currentClass + "." + subroutineLevelSymbols.getName() + "_LOOP_END_"
                + loopCount);

        eatTerminalSymbol('{');
        compileStatements();

        vmWriter.writeGoTo(currentClass + "." + subroutineLevelSymbols.getName() + "_LOOP_START_"
                + loopCount);

        eatTerminalSymbol('}');

        vmWriter.writeLabel(currentClass + "." + subroutineLevelSymbols.getName() + "_LOOP_END_"
                + loopCount);

        writeCloseTag(tag);

        subroutineLevelSymbols.incrementLoopCount();
    }
    /**
     * doStatement:  'do' subroutineName '(' expressionList ')' |
     * (className|varName) '.' subroutineName '(' expressionList ')' ';'
     */
    public void compileDo() throws IOException {
        String tag = "doStatement";
        writeOpenTag(tag);
        eatTerminalKeyword("do");

        String identifier = tokenizer.identifier();

        eatTerminalIdentifier();
        if (tokenizer.symbol() == '(') {
            handleIndependentMethodCall(identifier);
        } else {
            int methodArgs = 0;

            // if it's in the symbol table it's an instantiated object calling a method
            if (classLevelSymbols.contains(identifier)){
                methodArgs = 1;
                vmWriter.writePush(Objects.requireNonNull(getSegment(identifier, classLevelSymbols)),
                        classLevelSymbols.indexOf(identifier));

            } else if (subroutineLevelSymbols.contains(identifier)){
                methodArgs = 1;
                vmWriter.writePush(Objects.requireNonNull(getSegment(identifier, subroutineLevelSymbols)),
                        subroutineLevelSymbols.indexOf(identifier));
            }

            handleSubroutineCall(identifier, methodArgs);
        }
        vmWriter.writePop(Segment.TEMP, 0);

        eatTerminalSymbol(';');
        writeCloseTag(tag);
    } // do subroutines are void
    /**
     * returnStatement:  'return' expression? ';'
     */
    public void compileReturn() throws IOException {
        String tag = "returnStatement";
        writeOpenTag(tag);
        eatTerminalKeyword("return");
        if (currentTokenType == TokenType.SYMBOL && tokenizer.symbol() == ';'){

            vmWriter.writePush(Segment.CONSTANT, 0); // push garbage value if void

        } else {
            compileExpression();
        }
        vmWriter.writeReturn();
        eatTerminalSymbol(';');
        writeCloseTag(tag);
    } // will push constant 0 if no return value
    /**
     *   expression:  term (op term)*
     */
    public void compileExpression() throws IOException {
        String tag = "expression";
        ArrayList<Character> ops = new ArrayList<>();

        writeOpenTag(tag);

        compileTerm();
        // (op term)*
        while (currentTokenType == TokenType.SYMBOL && op.contains(tokenizer.symbol())){
            ops.add(tokenizer.symbol());
            eatTerminalSymbol(tokenizer.symbol());
            compileTerm();
        }
        writeCloseTag(tag);

        // push ops in reverse order
        for (int i = ops.size() - 1; i >= 0; i--){
            char op = ops.get(i);
            switch (op){
                case '*' -> vmWriter.writeCall("Math.multiply", 2);
                case '/' -> vmWriter.writeCall("Math.divide", 2);
                default -> vmWriter.writeArithmetic(opsTable.get(op));
            }
        }
    }
    /**
     * ( expression (',' expression)*)?
     * @return expressionCount
     */
    public int compileExpressionList() throws IOException {
        int expressionCount = 0;
        String tag = "expressionList";
        writeOpenTag(tag);
        if (currentTokenType != TokenType.SYMBOL || tokenizer.symbol() != ')'){
            compileExpression();
            expressionCount++;
            while (currentTokenType == TokenType.SYMBOL && tokenizer.symbol() == ','){
                eatTerminalSymbol(',');
                compileExpression();
                expressionCount++;
            }

        }
        writeCloseTag(tag);
        return expressionCount;
    }
    /**
     * term:  integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
     *        subroutineCall | '(' expression ')' | unaryOp term
     */
    public void compileTerm() throws IOException {
        String tag = "term";
        writeOpenTag(tag);
        switch (currentTokenType){
            case INT_CONSTANT -> {                                   // integerConstant

                vmWriter.writePush(Segment.CONSTANT, tokenizer.intVal());

                eatTerminalIntegerConstant();
            }
            case STR_CONSTANT -> {                                   // stringConstant
                handleStringConstant(tokenizer.stringVal());
                eatTerminalStringConstant();
            }
            case KEYWORD -> {                                        // keywordConstant
                if (keywordConstant.contains(tokenizer.keyword())){

                    switch (tokenizer.keyword()){
                        case "true" ->{
                            vmWriter.writePush(Segment.CONSTANT, 1);
                            vmWriter.writeArithmetic(Ops.NEG);                    // True is -1
                        }
                        case "this" -> vmWriter.writePush(Segment.POINTER, 0);
                        default -> vmWriter.writePush(Segment.CONSTANT, 0); // False and null are 0
                    }

                    eatTerminalKeyword(tokenizer.keyword());
                }
            }
            case SYMBOL -> {
                if (unaryOp.contains(tokenizer.symbol())){// unaryOP

                    char unaryOp = tokenizer.symbol(); // Store unaryOp to be pushed later

                    eatTerminalSymbol(tokenizer.symbol());
                    compileTerm();

                    vmWriter.writeArithmetic(unaryOpsTable.get(unaryOp)); // write unaryOp after term

                } else if (tokenizer.symbol() == '('){              // '(' expression ')'
                    eatTerminalSymbol('(');
                    compileExpression();
                    eatTerminalSymbol(')');
                }
            }
            case IDENTIFIER -> {
                /* Identifiers can be any of the following:
                   varName                                  -> push variable
                   varName[expression]                      -> handle array
                   varName.methodName(parameterList)        -> method call (push varName as ARG 0)
                   methodName(parameterList)                -> independent method call (push THIS as ARG 0) same as
                                                               this.methodName(parameterList)
                   className.functionName                   -> static function
                   className.constructorName(parameterList) -> constructor

                   symbols not found in symbolTable are assumed to be class or subroutine (function/method/constructor)
                   names.
                 */


                String identifier = tokenizer.identifier(); // store identifier
                eatTerminalIdentifier();
                char nextSymbol = tokenizer.symbol();
                if (nextSymbol == '(') {
                    handleIndependentMethodCall(identifier);
                } else if (nextSymbol == '.'){
                    int methodArgs = 0;

                    // if it's in the symbol table it's an instantiated object calling a method
                    if (classLevelSymbols.contains(identifier)){
                        methodArgs = 1;
                        vmWriter.writePush(Objects.requireNonNull(getSegment(identifier, classLevelSymbols)),
                                classLevelSymbols.indexOf(identifier));

                    } else if (subroutineLevelSymbols.contains(identifier)){
                        methodArgs = 1;
                        vmWriter.writePush(Objects.requireNonNull(getSegment(identifier, subroutineLevelSymbols)),
                                subroutineLevelSymbols.indexOf(identifier));
                    }

                    handleSubroutineCall(identifier, methodArgs);

                } else if (nextSymbol == '['){  // get array[expression]
                    handleVariableInTables(identifier,varOp.PUSH); // push array address
                    eatTerminalSymbol('[');
                    compileExpression();                           // push value of expression
                    eatTerminalSymbol(']');
                    vmWriter.writeArithmetic(Ops.ADD);             // get array segment address
                    vmWriter.writePop(Segment.POINTER, 1);   // set THAT
                    vmWriter.writePush(Segment.THAT, 0 );    // get value of segment
                } else {
                    handleVariableInTables(identifier, varOp.PUSH);
                }
            }
        }
        writeCloseTag(tag);
    }

    private boolean typeKeywordCheck(){
        return currentTokenType == TokenType.KEYWORD && typeKeywords.contains(tokenizer.keyword());
    }
    private void compileType() throws IOException {
        if (typeKeywordCheck()){
            eatTerminalKeyword(tokenizer.keyword());
        } else if (currentTokenType == TokenType.IDENTIFIER){
            eatTerminalIdentifier();
        } else {
            throw new IllegalArgumentException("Unexpected token on line " + tokenizer.getLineNumber());
        }
    }

    private void handleVariableInTables(String varName, varOp op) throws IOException {
        Segment segment = null;
        int index;
        if (subroutineLevelSymbols.contains(varName)){
            segment = getSegment(varName, subroutineLevelSymbols);
            index = subroutineLevelSymbols.indexOf(varName);
        } else {
            segment = getSegment(varName, classLevelSymbols);
            index = classLevelSymbols.indexOf(varName);
        }

        assert segment != null;
        switch(op) {
            case PUSH -> vmWriter.writePush(segment, index);
            case POP -> vmWriter.writePop(segment, index); // write pop to requisite address
        }
    }

    private Segment getSegment(String varName, SymbolTable table){
        SymbolTable.kind varKind = table.kindOf(varName);
        switch (varKind){
            case VAR -> {
                return Segment.LOCAL;
            }
            case ARG -> {
                return Segment.ARGUMENT;
            }
            case STATIC -> {
                return Segment.STATIC;
            }
            case FIELD -> {
                return Segment.THIS;
            }
            default -> {
                throw new IllegalArgumentException(varName + " not found in table");
            }
        }
    }

    private void handleStringConstant(String string) throws IOException {
        int stringLength = string.length();

        // allocate space for String
        vmWriter.writePush(Segment.CONSTANT, stringLength);  // get length of string
        vmWriter.writeCall("String.new", 1);      // make string of that length, push to stack
        // populate String
        for (int i = 0; i < stringLength; i++){
            char currentChar = string.charAt(i);
            vmWriter.writePush(Segment.CONSTANT, currentChar);      // push character (as an integer)
            vmWriter.writeCall("String.appendChar", 2);  // append to String, returning to stack
        }                                                           // String is now on top of stack
    }

    private void handleIndependentMethodCall(String identifier) throws IOException {
        vmWriter.writePush(Segment.POINTER, 0);
        int nArgs = 1;                                // THIS has been pushed as ARG 0
        eatTerminalSymbol('(');
        nArgs += compileExpressionList();             // Add the rest of the args
        eatTerminalSymbol(')');
        vmWriter.writeCall(currentClass + "." + identifier, nArgs);
    }
    private void handleSubroutineCall(String identifier, int methodArgs) throws IOException {
        eatTerminalSymbol('.');
        if (methodArgs == 1){
            // Change the identifier to the type of the object
            if (classLevelSymbols.contains(identifier)){
                identifier = classLevelSymbols.typeOf(identifier);
            } else {
                identifier = subroutineLevelSymbols.typeOf(identifier);
            }
        }
        String subroutineName = identifier + "." + tokenizer.identifier();
        eatTerminalIdentifier();
        eatTerminalSymbol('(');
        int nArgs = methodArgs + compileExpressionList();
        eatTerminalSymbol(')');
        vmWriter.writeCall(subroutineName, nArgs);
    }

}
