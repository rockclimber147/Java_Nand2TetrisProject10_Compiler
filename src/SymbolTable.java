import java.util.HashMap;

public class SymbolTable {

    enum kind {
        STATIC,
        FIELD,
        ARG,
        VAR,
        NONE
    }

    private String name;
    private int loopCount = 0;
    private int ifElseCount = 0;

    private int longestName = 0;
    private int longestType = 0;

    private final HashMap<String, SymbolProperty> table = new HashMap<>();
    private final HashMap<kind, Integer> varIndexes = new HashMap<>(); // Tally of each kind

    public SymbolTable(){
        varIndexes.put(kind.STATIC, 0);
        varIndexes.put(kind.FIELD, 0);
        varIndexes.put(kind.ARG, 0);
        varIndexes.put(kind.VAR, 0);
    }


    public void reset(){
        table.clear();
        varIndexes.replaceAll((k, v) -> 0);

        loopCount = 0;
        ifElseCount = 0;
        longestName = 0;
        longestType = 0;
    }

    public void define(String name, String type, kind kind){

        if (name.length() > longestName) longestName = name.length();
        if (type.length() > longestType) longestType = type.length();

        table.put(name, new SymbolProperty(type, kind, varIndexes.get(kind)));
        varIndexes.put(kind, varIndexes.get(kind) + 1); // increment tally of defined kind
    }

    public int varCount(kind kind){
        return varIndexes.get(kind);
    }

    public kind kindOf(String name){
        if (!table.containsKey(name)){
            return kind.NONE;
        }
        return table.get(name).getKind();
    }

    public String getFormattedTable(){
        StringBuilder formattedTable = new StringBuilder();

        formattedTable.append("/*\n");

        for (kind k:kind.values()){
            if (k != kind.NONE && varIndexes.get(k) > 0){ // only look if there's at least one declared variable
                formattedTable.append(getPropertiesOfKind(k, varCount(k)));
            }
        }

        formattedTable.append("*/\n");

        return formattedTable.toString();
    }

    public boolean contains(String varName){
        return table.containsKey(varName);
    }

    /**
     *
     * @return THE NAME OF THE CURRENT SUBROUTINE
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String typeOf(String name){
        return table.get(name).getType();
    }

    public int indexOf(String name){
        return table.get(name).getIndex();
    }




    public String getPropertiesOfVar(String var){
        return " (" + table.get(var) + ")";
    }

    private String getPropertiesOfKind(kind varKind, int varCount){
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < varCount; i++){
            for (String varName: table.keySet()){
                SymbolProperty varProperties = table.get(varName);
                if ((varProperties.getKind() == varKind) && (varProperties.getIndex() == i)){
                    s.append(varProperties.formattedString(longestType));

                    int namePadding = longestName - varName.length() + 1;

                    s.append(varName)
                            .append(" ".repeat(namePadding))
                            .append("-> ").append(kindToSegment(varKind))
                            .append(i)
                            .append("\n");
                }
            }
        }

        return s.toString();
    }

    private String kindToSegment(kind k){
        String segment = null;
        switch (k) {
            case FIELD -> segment =  "this     ";
            case STATIC -> segment = "static   ";
            case VAR -> segment =    "local    ";
            case ARG -> segment =    "argument ";
        }
        return segment;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public int getIfElseCount() {
        return ifElseCount;
    }

    public void incrementLoopCount(){
        loopCount++;
    }

    public void incrementIfElseCount(){
        ifElseCount++;
    }
}
