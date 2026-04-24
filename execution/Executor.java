package execution;

import cpu.CPU;
import memory.Memory;

public class Executor {
    private final CPU cpu;
    private final Memory memory;
    private boolean halted;
    private String lastMessage = "Ready";

    public Executor(CPU cpu, Memory memory) {
        this.cpu = cpu;
        this.memory = memory;
    }

    public boolean isHalted() {
        return halted;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void reset() {
        halted = false;
        lastMessage = "Ready";
    }

    public void executeNext() {
        if (halted) {
            lastMessage = "CPU already halted.";
            return;
        }
        int pc = cpu.PC & 0xFFFF;
        int opcode = memory.readByte(pc) & 0xFF;
        cpu.PC = (cpu.PC + 1) & 0xFFFF;
        lastMessage = String.format("Executed %02X", opcode);

        try {
            switch (opcode) {
                case 0x00: // NOP
                    break;
                case 0x76: // HLT
                    halted = true;
                    lastMessage = "HLT encountered.";
                    break;
                case 0x37: // STC
                    cpu.flagCY = true;
                    break;
                case 0x3F: // CMC
                    cpu.flagCY = !cpu.flagCY;
                    break;
                case 0x2F: // CMA
                    cpu.A = (~cpu.A) & 0xFF;
                    break;
                case 0x27: // DAA
                    executeDAA();
                    break;
                case 0x07: // RLC
                    cpu.flagCY = (cpu.A & 0x80) != 0;
                    cpu.A = ((cpu.A << 1) | (cpu.flagCY ? 1 : 0)) & 0xFF;
                    break;
                case 0x0F: // RRC
                    cpu.flagCY = (cpu.A & 0x01) != 0;
                    cpu.A = ((cpu.A >> 1) | (cpu.flagCY ? 0x80 : 0)) & 0xFF;
                    break;
                case 0x17: // RAL
                    int carry = cpu.flagCY ? 1 : 0;
                    cpu.flagCY = (cpu.A & 0x80) != 0;
                    cpu.A = ((cpu.A << 1) | carry) & 0xFF;
                    break;
                case 0x1F: // RAR
                    carry = cpu.flagCY ? 1 : 0;
                    cpu.flagCY = (cpu.A & 0x01) != 0;
                    cpu.A = ((cpu.A >> 1) | (carry << 7)) & 0xFF;
                    break;
                case 0xE3: // XTHL
                    int tempL = cpu.L;
                    int tempH = cpu.H;
                    int low = popByte();
                    int high = popByte();
                    pushByte(tempH);
                    pushByte(tempL);
                    cpu.L = low;
                    cpu.H = high;
                    break;
                case 0xF9: // SPHL
                    cpu.SP = cpu.getRegisterPair("HL");
                    break;
                case 0xE5: // PUSH H
                    pushWord(cpu.getRegisterPair("HL"));
                    break;
                case 0xF5: // PUSH PSW
                    pushWord(cpu.getPSW());
                    break;
                case 0xE1: // POP H
                    cpu.setRegisterPair("HL", popWord());
                    break;
                case 0xF1: // POP PSW
                    cpu.setPSW(popWord());
                    break;
                case 0x09: // DAD B
                    addRegisterPair("BC");
                    break;
                case 0x19: // DAD D
                    addRegisterPair("DE");
                    break;
                case 0x29: // DAD H
                    addRegisterPair("HL");
                    break;
                case 0x39: // DAD SP
                    addRegisterPair("SP");
                    break;
                case 0x2A: // LHLD addr
                    executeLoadHLFromMemory(pc);
                    break;
                case 0x22: // SHLD addr
                    executeStoreHLToMemory(pc);
                    break;
                case 0x3A: // LDA addr
                    executeLoadAFromMemory(pc);
                    break;
                case 0x32: // STA addr
                    executeStoreAToMemory(pc);
                    break;
                case 0x02: // STAX B
                    memory.writeByte(cpu.getRegisterPair("BC"), (byte) (cpu.A & 0xFF));
                    break;
                case 0x12: // STAX D
                    memory.writeByte(cpu.getRegisterPair("DE"), (byte) (cpu.A & 0xFF));
                    break;
                case 0x0A: // LDAX B
                    cpu.A = memory.readByte(cpu.getRegisterPair("BC")) & 0xFF;
                    break;
                case 0x1A: // LDAX D
                    cpu.A = memory.readByte(cpu.getRegisterPair("DE")) & 0xFF;
                    break;
                case 0xDB: // IN
                    cpu.A = 0;
                    lastMessage = "IN instruction executed (no I/O emulation).";
                    break;
                case 0xD3: // OUT
                    lastMessage = "OUT instruction executed (no I/O emulation).";
                    break;
                case 0xC3: // JMP
                    cpu.PC = readAddress(pc + 1);
                    break;
                case 0xC2: // JNZ
                    conditionalJump(!cpu.flagZ, pc + 1);
                    break;
                case 0xCA: // JZ
                    conditionalJump(cpu.flagZ, pc + 1);
                    break;
                case 0xD2: // JNC
                    conditionalJump(!cpu.flagCY, pc + 1);
                    break;
                case 0xDA: // JC
                    conditionalJump(cpu.flagCY, pc + 1);
                    break;
                case 0xF2: // JP
                    conditionalJump(!cpu.flagS, pc + 1);
                    break;
                case 0xFA: // JM
                    conditionalJump(cpu.flagS, pc + 1);
                    break;
                case 0xEA: // JPE
                    conditionalJump(cpu.flagP, pc + 1);
                    break;
                case 0xE2: // JPO
                    conditionalJump(!cpu.flagP, pc + 1);
                    break;
                case 0xCD: // CALL
                    executeCall(pc + 1);
                    break;
                case 0xC4: // CNZ
                    conditionalCall(!cpu.flagZ, pc + 1);
                    break;
                case 0xCC: // CZ
                    conditionalCall(cpu.flagZ, pc + 1);
                    break;
                case 0xD4: // CNC
                    conditionalCall(!cpu.flagCY, pc + 1);
                    break;
                case 0xDC: // CC
                    conditionalCall(cpu.flagCY, pc + 1);
                    break;
                case 0xF4: // CP
                    conditionalCall(!cpu.flagS, pc + 1);
                    break;
                case 0xFC: // CM
                    conditionalCall(cpu.flagS, pc + 1);
                    break;
                case 0xEC: // CPE
                    conditionalCall(cpu.flagP, pc + 1);
                    break;
                case 0xE4: // CPO
                    conditionalCall(!cpu.flagP, pc + 1);
                    break;
                case 0xC9: // RET
                    cpu.PC = popWord();
                    break;
                case 0xC0: // RNZ
                    if (!cpu.flagZ) cpu.PC = popWord();
                    break;
                case 0xC8: // RZ
                    if (cpu.flagZ) cpu.PC = popWord();
                    break;
                case 0xD0: // RNC
                    if (!cpu.flagCY) cpu.PC = popWord();
                    break;
                case 0xD8: // RC
                    if (cpu.flagCY) cpu.PC = popWord();
                    break;
                case 0xE0: // RPO
                    if (!cpu.flagP) cpu.PC = popWord();
                    break;
                case 0xE8: // RPE
                    if (cpu.flagP) cpu.PC = popWord();
                    break;
                case 0xF0: // RP
                    if (!cpu.flagS) cpu.PC = popWord();
                    break;
                case 0xF8: // RM
                    if (cpu.flagS) cpu.PC = popWord();
                    break;
                default:
                    if (isMoveInstruction(opcode)) {
                        int dest = (opcode >> 3) & 0x07;
                        int src = opcode & 0x07;
                        int value = getRegisterValue(src);
                        setRegisterValue(dest, value);
                    } else if (isMVIInstruction(opcode)) {
                        int reg = (opcode >> 3) & 0x07;
                        int data = memory.readByte(cpu.PC) & 0xFF;
                        cpu.PC = (cpu.PC + 1) & 0xFFFF;
                        setRegisterValue(reg, data);
                    } else if (isArithmeticInstruction(opcode)) {
                        int reg = opcode & 0x07;
                        int value = getRegisterValue(reg);
                        switch (opcode & 0xF8) {
                            case 0x80:
                                executeAdd(value, false);
                                break;
                            case 0x88:
                                executeAdd(value, true);
                                break;
                            case 0x90:
                                executeSub(value, false);
                                break;
                            case 0x98:
                                executeSub(value, true);
                                break;
                            case 0xA0:
                                executeLogical(value, 'A');
                                break;
                            case 0xA8:
                                executeLogical(value, 'X');
                                break;
                            case 0xB0:
                                executeLogical(value, 'O');
                                break;
                            case 0xB8:
                                executeCompare(value);
                                break;
                        }
                    } else if (isImmediateArithmetic(opcode)) {
                        int data = memory.readByte(cpu.PC) & 0xFF;
                        cpu.PC = (cpu.PC + 1) & 0xFFFF;
                        switch (opcode) {
                            case 0xC6:
                                executeAdd(data, false);
                                break;
                            case 0xCE:
                                executeAdd(data, true);
                                break;
                            case 0xD6:
                                executeSub(data, false);
                                break;
                            case 0xDE:
                                executeSub(data, true);
                                break;
                            case 0xE6:
                                executeLogical(data, 'A');
                                break;
                            case 0xEE:
                                executeLogical(data, 'X');
                                break;
                            case 0xF6:
                                executeLogical(data, 'O');
                                break;
                            case 0xFE:
                                executeCompare(data);
                                break;
                        }
                    } else if (isRegisterIncrement(opcode)) {
                        int reg = (opcode >> 3) & 0x07;
                        int before = getRegisterValue(reg);
                        int value = (before + 1) & 0xFF;
                        setRegisterValue(reg, value);
                        cpu.flagAC = ((before & 0x0F) == 0x0F);
                        setZeroSignParity(value);
                    } else if (isRegisterDecrement(opcode)) {
                        int reg = (opcode >> 3) & 0x07;
                        int before = getRegisterValue(reg);
                        int value = (before - 1) & 0xFF;
                        setRegisterValue(reg, value);
                        cpu.flagAC = ((before & 0x0F) == 0);
                        setZeroSignParity(value);
                    } else if (isRegisterPairIncrement(opcode)) {
                        int pair = (opcode >> 4) & 0x03;
                        int value = getRegisterPair(pair);
                        value = (value + 1) & 0xFFFF;
                        setRegisterPair(pair, value);
                    } else if (isRegisterPairDecrement(opcode)) {
                        int pair = (opcode >> 4) & 0x03;
                        int value = getRegisterPair(pair);
                        value = (value - 1) & 0xFFFF;
                        setRegisterPair(pair, value);
                    } else if (isStaxOpcode(opcode)) {
                        int pair = (opcode >> 4) & 0x03;
                        int address = getRegisterPair(pair);
                        memory.writeByte(address, (byte) (cpu.A & 0xFF));
                    } else {
                        halted = true;
                        lastMessage = String.format("Unknown opcode %02X at %04X", opcode, pc);
                    }
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            halted = true;
            lastMessage = "Memory access error.";
        }
    }

    private void executeLoadHLFromMemory(int pc) {
        int address = readAddress(pc + 1);
        cpu.L = memory.readByte(address) & 0xFF;
        cpu.H = memory.readByte(address + 1) & 0xFF;
        cpu.PC = (cpu.PC + 2) & 0xFFFF;
    }

    private void executeStoreHLToMemory(int pc) {
        int address = readAddress(pc + 1);
        memory.writeByte(address, (byte) (cpu.L & 0xFF));
        memory.writeByte(address + 1, (byte) (cpu.H & 0xFF));
        cpu.PC = (cpu.PC + 2) & 0xFFFF;
    }

    private void executeLoadAFromMemory(int pc) {
        int address = readAddress(pc + 1);
        cpu.A = memory.readByte(address) & 0xFF;
        cpu.PC = (cpu.PC + 2) & 0xFFFF;
    }

    private void executeStoreAToMemory(int pc) {
        int address = readAddress(pc + 1);
        memory.writeByte(address, (byte) (cpu.A & 0xFF));
        cpu.PC = (cpu.PC + 2) & 0xFFFF;
    }

    private void executeCall(int offset) {
        int target = readAddress(offset);
        pushWord(cpu.PC & 0xFFFF);
        cpu.PC = target;
    }

    private void conditionalJump(boolean condition, int offset) {
        if (condition) {
            cpu.PC = readAddress(offset);
        } else {
            cpu.PC = (cpu.PC + 2) & 0xFFFF;
        }
    }

    private void conditionalCall(boolean condition, int offset) {
        int target = readAddress(offset);
        if (condition) {
            pushWord(cpu.PC & 0xFFFF);
            cpu.PC = target;
        } else {
            cpu.PC = (cpu.PC + 2) & 0xFFFF;
        }
    }

    private boolean isMoveInstruction(int opcode) {
        return opcode >= 0x40 && opcode <= 0x7F && opcode != 0x76;
    }

    private boolean isMVIInstruction(int opcode) {
        return opcode == 0x06 || opcode == 0x0E || opcode == 0x16 || opcode == 0x1E || opcode == 0x26 || opcode == 0x2E || opcode == 0x36 || opcode == 0x3E;
    }

    private boolean isArithmeticInstruction(int opcode) {
        return opcode >= 0x80 && opcode <= 0xBF;
    }

    private boolean isImmediateArithmetic(int opcode) {
        return opcode == 0xC6 || opcode == 0xCE || opcode == 0xD6 || opcode == 0xDE || opcode == 0xE6 || opcode == 0xEE || opcode == 0xF6 || opcode == 0xFE;
    }

    private boolean isRegisterIncrement(int opcode) {
        return opcode == 0x04 || opcode == 0x0C || opcode == 0x14 || opcode == 0x1C || opcode == 0x24 || opcode == 0x2C || opcode == 0x34 || opcode == 0x3C;
    }

    private boolean isRegisterDecrement(int opcode) {
        return opcode == 0x05 || opcode == 0x0D || opcode == 0x15 || opcode == 0x1D || opcode == 0x25 || opcode == 0x2D || opcode == 0x35 || opcode == 0x3D;
    }

    private boolean isRegisterPairIncrement(int opcode) {
        return opcode == 0x03 || opcode == 0x13 || opcode == 0x23 || opcode == 0x33;
    }

    private boolean isRegisterPairDecrement(int opcode) {
        return opcode == 0x0B || opcode == 0x1B || opcode == 0x2B || opcode == 0x3B;
    }

    private boolean isStaxOpcode(int opcode) {
        return opcode == 0x02 || opcode == 0x12;
    }

    private int getRegisterValue(int code) {
        switch (code) {
            case 0: return cpu.B;
            case 1: return cpu.C;
            case 2: return cpu.D;
            case 3: return cpu.E;
            case 4: return cpu.H;
            case 5: return cpu.L;
            case 6: return memory.readByte(cpu.getRegisterPair("HL")) & 0xFF;
            case 7: return cpu.A;
            default: return 0;
        }
    }

    private void setRegisterValue(int code, int value) {
        value &= 0xFF;
        switch (code) {
            case 0: cpu.B = value; break;
            case 1: cpu.C = value; break;
            case 2: cpu.D = value; break;
            case 3: cpu.E = value; break;
            case 4: cpu.H = value; break;
            case 5: cpu.L = value; break;
            case 6: memory.writeByte(cpu.getRegisterPair("HL"), (byte) value); break;
            case 7: cpu.A = value; break;
        }
    }

    private int getRegisterPair(int code) {
        switch (code) {
            case 0: return cpu.getRegisterPair("BC");
            case 1: return cpu.getRegisterPair("DE");
            case 2: return cpu.getRegisterPair("HL");
            case 3: return cpu.SP & 0xFFFF;
            default: return 0;
        }
    }

    private void setRegisterPair(int code, int value) {
        switch (code) {
            case 0: cpu.setRegisterPair("BC", value); break;
            case 1: cpu.setRegisterPair("DE", value); break;
            case 2: cpu.setRegisterPair("HL", value); break;
            case 3: cpu.SP = value & 0xFFFF; break;
        }
    }

    private void executeAdd(int value, boolean useCarry) {
        int initial = cpu.A;
        int carryIn = useCarry && cpu.flagCY ? 1 : 0;
        int result = initial + value + carryIn;
        cpu.flagAC = (((initial & 0x0F) + (value & 0x0F) + carryIn) & 0x10) != 0;
        cpu.flagCY = result > 0xFF;
        cpu.A = result & 0xFF;
        setZeroSignParity(cpu.A);
    }

    private void executeSub(int value, boolean useBorrow) {
        int initial = cpu.A;
        int borrow = useBorrow && cpu.flagCY ? 1 : 0;
        int result = initial - value - borrow;
        cpu.flagCY = result < 0;
        cpu.flagAC = ((initial & 0x0F) - (value & 0x0F) - borrow) < 0;
        cpu.A = result & 0xFF;
        setZeroSignParity(cpu.A);
    }

    private void executeLogical(int value, char type) {
        switch (type) {
            case 'A': cpu.A &= value; break;
            case 'X': cpu.A ^= value; break;
            case 'O': cpu.A |= value; break;
        }
        cpu.flagCY = false;
        cpu.flagAC = false;
        setZeroSignParity(cpu.A);
    }

    private void executeCompare(int value) {
        int result = cpu.A - value;
        cpu.flagCY = result < 0;
        cpu.flagAC = ((cpu.A & 0x0F) - (value & 0x0F)) < 0;
        setZeroSignParity(result);
    }

    private void setZeroSignParity(int value) {
        value &= 0xFF;
        cpu.flagZ = value == 0;
        cpu.flagS = (value & 0x80) != 0;
        cpu.flagP = Integer.bitCount(value) % 2 == 0;
    }

    private void addRegisterPair(String pair) {
        int result = cpu.getRegisterPair(pair) + cpu.getRegisterPair("HL");
        cpu.flagCY = result > 0xFFFF;
        cpu.setRegisterPair("HL", result & 0xFFFF);
    }

    private void pushWord(int value) {
        cpu.SP = (cpu.SP - 1) & 0xFFFF;
        memory.writeByte(cpu.SP, (byte) ((value >> 8) & 0xFF));
        cpu.SP = (cpu.SP - 1) & 0xFFFF;
        memory.writeByte(cpu.SP, (byte) (value & 0xFF));
    }

    private int popWord() {
        int low = memory.readByte(cpu.SP) & 0xFF;
        cpu.SP = (cpu.SP + 1) & 0xFFFF;
        int high = memory.readByte(cpu.SP) & 0xFF;
        cpu.SP = (cpu.SP + 1) & 0xFFFF;
        return (high << 8) | low;
    }

    private int popByte() {
        int value = memory.readByte(cpu.SP) & 0xFF;
        cpu.SP = (cpu.SP + 1) & 0xFFFF;
        return value;
    }

    private void pushByte(int value) {
        cpu.SP = (cpu.SP - 1) & 0xFFFF;
        memory.writeByte(cpu.SP, (byte) (value & 0xFF));
    }

    private void executeDAA() {
        int correction = 0;
        if ((cpu.A & 0x0F) > 9 || cpu.flagAC) {
            correction += 0x06;
        }
        if ((cpu.A >> 4) > 9 || cpu.flagCY || ((cpu.A & 0x0F) + (correction & 0x0F)) > 9) {
            correction += 0x60;
            cpu.flagCY = true;
        }
        int result = cpu.A + correction;
        cpu.flagAC = (((cpu.A & 0x0F) + (correction & 0x0F)) & 0x10) != 0;
        cpu.A = result & 0xFF;
        setZeroSignParity(cpu.A);
    }

    private int readAddress(int offset) {
        int low = memory.readByte(offset) & 0xFF;
        int high = memory.readByte(offset + 1) & 0xFF;
        return (high << 8) | low;
    }
}
