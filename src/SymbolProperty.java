public class SymbolProperty {
    private final String type;
    private final int index;
    private final SymbolTable.kind kind;

    public SymbolProperty(String type, SymbolTable.kind kind, int index){
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public SymbolTable.kind getKind() {
        return kind;
    }

    public String formattedString(int longestType){
        StringBuilder symbolProperties = new StringBuilder();

        int padding = 4;

        switch (kind){
            case STATIC -> padding = 1;
            case FIELD -> padding = 2;
        }
        symbolProperties.append(kind).append(" ".repeat(padding));

        int typePadding = longestType - type.length() + 1;

        symbolProperties.append(type).append(" ".repeat(typePadding));

        return symbolProperties.toString();
    }

    public String toString(){
        return "kind: " + kind + " type: " + type + " index: " + index;
    }
}
