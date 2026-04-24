package memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Memory {
    private final byte[] memorySpace = new byte[65536];

    public byte readByte(int address) {
        return memorySpace[address & 0xFFFF];
    }

    public void writeByte(int address, byte data) {
        memorySpace[address & 0xFFFF] = data;
    }

    public int readWord(int address) {
        int low = readByte(address) & 0xFF;
        int high = readByte(address + 1) & 0xFF;
        return (high << 8) | low;
    }

    public void writeWord(int address, int value) {
        writeByte(address, (byte) (value & 0xFF));
        writeByte(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    public void reset() {
        Arrays.fill(memorySpace, (byte) 0);
    }

    public void loadHexFile(String filePath) throws IOException {
        int extendedAddress = 0;
        for (String line : Files.readAllLines(Paths.get(filePath))) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.charAt(0) == ':') {
                int count = Integer.parseInt(trimmed.substring(1, 3), 16);
                int address = Integer.parseInt(trimmed.substring(3, 7), 16);
                int recordType = Integer.parseInt(trimmed.substring(7, 9), 16);
                if (recordType == 0x00) {
                    for (int i = 0; i < count; i++) {
                        int data = Integer.parseInt(trimmed.substring(9 + i * 2, 11 + i * 2), 16);
                        writeByte(extendedAddress + address + i, (byte) data);
                    }
                } else if (recordType == 0x01) {
                    break;
                } else if (recordType == 0x04) {
                    extendedAddress = Integer.parseInt(trimmed.substring(9, 13), 16) << 16;
                }
            } else {
                String[] tokens = trimmed.split("\\s+");
                int address = 0;
                for (String token : tokens) {
                    if (token.isEmpty()) continue;
                    int data = Integer.parseInt(token, 16);
                    writeByte(address++, (byte) data);
                }
            }
        }
    }
}
