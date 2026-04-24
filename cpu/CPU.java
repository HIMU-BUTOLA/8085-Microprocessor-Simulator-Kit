package cpu;

public class CPU {
    public int A, B, C, D, E, H, L;
    public int PC, SP;
    public boolean flagZ, flagS, flagP, flagCY, flagAC;

    public CPU() {
        reset();
    }

    public void reset() {
        A = B = C = D = E = H = L = 0;
        PC = 0;
        SP = 0xFFFF;
        flagZ = flagS = flagP = flagCY = flagAC = false;
    }

    public int getRegisterPair(String pair) {
        if (pair == null) return 0;
        switch (pair.trim().toUpperCase()) {
            case "HL":
                return ((H & 0xFF) << 8) | (L & 0xFF);
            case "BC":
                return ((B & 0xFF) << 8) | (C & 0xFF);
            case "DE":
                return ((D & 0xFF) << 8) | (E & 0xFF);
            case "SP":
                return SP & 0xFFFF;
            default:
                return 0;
        }
    }

    public void setRegisterPair(String pair, int value) {
        switch (pair.trim().toUpperCase()) {
            case "HL":
                H = (value >> 8) & 0xFF;
                L = value & 0xFF;
                break;
            case "BC":
                B = (value >> 8) & 0xFF;
                C = value & 0xFF;
                break;
            case "DE":
                D = (value >> 8) & 0xFF;
                E = value & 0xFF;
                break;
            case "SP":
                SP = value & 0xFFFF;
                break;
            default:
                break;
        }
    }

    public int getPSW() {
        int flags = 0;
        flags |= flagS ? 0x80 : 0;
        flags |= flagZ ? 0x40 : 0;
        flags |= flagAC ? 0x10 : 0;
        flags |= flagP ? 0x04 : 0;
        flags |= 0x02; // reserved bit always set
        flags |= flagCY ? 0x01 : 0;
        return ((A & 0xFF) << 8) | (flags & 0xFF);
    }

    public void setPSW(int value) {
        A = (value >> 8) & 0xFF;
        int flags = value & 0xFF;
        flagS = (flags & 0x80) != 0;
        flagZ = (flags & 0x40) != 0;
        flagAC = (flags & 0x10) != 0;
        flagP = (flags & 0x04) != 0;
        flagCY = (flags & 0x01) != 0;
    }
}
