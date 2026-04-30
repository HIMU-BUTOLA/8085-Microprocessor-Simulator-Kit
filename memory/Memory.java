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
}
