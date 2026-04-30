package assembler;

import java.util.ArrayList;
import java.util.List;

public class AssemblyParser {
    public static class ParseResult {
        public boolean success;
        public String label;
        public String opcode;
        public String mnemonic; 
        public List<String> operands = new ArrayList<>();
        public List<String> steps = new ArrayList<>();
        public byte[] bytes = new byte[0];

    }

    public ParseResult parseLine(String line) {
        ParseResult result = new ParseResult();
        if (line == null) return result;

        String original = line.trim();
        result.mnemonic = original; 
        
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
        
        try {
            switch (result.opcode) {
                // NOP: single-byte instruction with fixed opcode 0x00.
                case "NOP": return new byte[]{0x00};
                // HLT: single-byte instruction with fixed opcode 0x76.
                case "HLT": return new byte[]{0x76};
                // LDA: 0x3A + 16-bit address low/high bytes.
                case "LDA": return encodeAddress16(result, (byte) 0x3A);
                // STA: 0x32 + 16-bit address low/high bytes.
                case "STA": return encodeAddress16(result, (byte) 0x32);
                // INR: 0x04 + register code in bits 5-3.
                case "INR": return encodeInr(result);
                // MOV: 0x40 + (dest code << 3) + src code.
                case "MOV": return encodeMov(result);
                // ADD: 0x80 + register code in bits 2-0.
                case "ADD": return encodeAdd(result);
                // MVI: 0x06 + register code << 3, plus immediate byte.
                case "MVI": return encodeMvi(result);
                // JMP: 0xC3 + 16-bit address low/high bytes.
                case "JMP": return encodeAddress16(result, (byte) 0xC3);
                default: return null;
            }
        } catch (Exception ex) { return null; }
    }

    private byte[] encodeAddress16(ParseResult result, byte opcode) {
        // Parse the operand address and emit opcode followed by low and high bytes.
        int address = Integer.parseInt(result.operands.get(0).replace("H", ""), 16);
        return new byte[]{opcode, (byte) (address & 0xFF), (byte) ((address >> 8) & 0xFF)};
    }

    private byte[] encodeInr(ParseResult result) {
        // Format INR as 0x04 plus register code in bits 5-3.
        String reg = result.operands.get(0);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x04 | (code << 3))};
    }

    private byte[] encodeMov(ParseResult result) {
        // Format MOV as 0x40 plus dest code shifted to bits 5-3 and source code in bits 2-0.
        String dest = result.operands.get(0);
        String src = result.operands.get(1);
        int destCode = "BCDEHLMA".indexOf(dest);
        int srcCode = "BCDEHLMA".indexOf(src);
        return new byte[]{(byte) (0x40 | (destCode << 3) | srcCode)};
    }

    private byte[] encodeAdd(ParseResult result) {
        // Format ADD as 0x80 plus register code in bits 2-0.
        String reg = result.operands.get(0);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x80 | code)};
    }

    private byte[] encodeMvi(ParseResult result) {
        // Format MVI as 0x06 plus register code in bits 5-3, then immediate data byte.
        String reg = result.operands.get(0);
        int data = Integer.parseInt(result.operands.get(1).replace("H", ""), 16);
        int code = "BCDEHLMA".indexOf(reg);
        return new byte[]{(byte) (0x06 | (code << 3)), (byte) data};
    }
}   