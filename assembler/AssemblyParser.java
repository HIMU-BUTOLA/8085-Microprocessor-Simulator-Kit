package assembler;

import java.util.ArrayList;
import java.util.List;

public class AssemblyParser {
    public static class ParseResult {
        public boolean success;
        public String label;
        public String opcode;
        public String mnemonic; // Stores original input for the trace
        public List<String> operands = new ArrayList<>();
        public List<String> steps = new ArrayList<>();
        public byte[] bytes = new byte[0];

        public void addStep(String step) {
            steps.add(step);
        }
    }

    public ParseResult parseLine(String line) {
        ParseResult result = new ParseResult();
        if (line == null) return result;

        String original = line.trim();
        result.mnemonic = original; // Capture exactly what the user typed
        
        int commentIndex = original.indexOf(';');
        if (commentIndex >= 0) {
            line = original.substring(0, commentIndex).trim();
        } else {
            line = original;
        }

        if (line.isEmpty()) return result;

        if (line.contains(":")) {
            String[] labelParts = line.split(":", 2);
            result.label = labelParts[0].trim();
            line = labelParts[1].trim();
        }

        String[] words = line.split("\\s+", 2);
        result.opcode = words[0].toUpperCase();

        if (words.length > 1) {
            result.operands = splitOperands(words[1].trim());
        }

        byte[] encoding = assembleInstruction(result);
        if (encoding != null) {
            result.bytes = encoding;
            result.success = true;
        }
        return result;
    }

    private List<String> splitOperands(String operandText) {
        List<String> operands = new ArrayList<>();
        for (String part : operandText.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) operands.add(part.toUpperCase());
        }
        return operands;
    }

    private byte[] assembleInstruction(ParseResult result) {
        // This switch handles mapping mnemonics to 8085 opcodes
        try {
            switch (result.opcode) {
                case "NOP": return new byte[]{0x00};
                case "HLT": return new byte[]{0x76};
                case "LDA": return encodeAddress16(result, (byte) 0x3A);
                case "STA": return encodeAddress16(result, (byte) 0x32);
                case "INR": return encodeInr(result);
                case "MOV": return encodeMov(result);
                case "ADD": return encodeAdd(result);
                case "MVI": return encodeMvi(result);
                case "JMP": return encodeAddress16(result, (byte) 0xC3);
                default: return null;
            }
        } catch (Exception ex) { return null; }
    }

    private byte[] encodeAddress16(ParseResult result, byte opcode) {
        int address = Integer.parseInt(result.operands.get(0).replace("H", ""), 16);
        return new byte[]{opcode, (byte) (address & 0xFF), (byte) ((address >> 8) & 0xFF)};
    }

    private byte[] encodeInr(ParseResult result) {
        String reg = result.operands.get(0);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x04 | (code << 3))};
    }

    private byte[] encodeMov(ParseResult result) {
        String dest = result.operands.get(0);
        String src = result.operands.get(1);
        int destCode = "BCDEHLMA".indexOf(dest);
        int srcCode = "BCDEHLMA".indexOf(src);
        return new byte[]{(byte) (0x40 | (destCode << 3) | srcCode)};
    }

    private byte[] encodeAdd(ParseResult result) {
        String reg = result.operands.get(0);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x80 | code)};
    }

    private byte[] encodeMvi(ParseResult result) {
        String reg = result.operands.get(0);
        int data = Integer.parseInt(result.operands.get(1).replace("H", ""), 16);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x06 | (code << 3)), (byte) data};
    }
}   