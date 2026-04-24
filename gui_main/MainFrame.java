package gui_main;

import assembler.AssemblyParser;
import cpu.CPU;
import memory.Memory;
import execution.Executor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final CPU cpu;
    private final Memory memory;
    private Executor executor;
    private final AssemblyParser parser;

    private final JTextField txtA, txtB, txtC, txtD, txtE, txtH, txtL, txtPC, txtSP;
    private final JCheckBox chkZ, chkS, chkP, chkCY, chkAC;
    private final JTextField asmInput;
    private final JTable memTable;
    private final DefaultTableModel memTableModel;
    private final JTextArea txtLog;
    private final JButton btnCompile, btnStep, btnRun, btnReset;
    private final JTextField memAddrInput, memDataInput;
    private final JButton btnStoreMem, btnExamine;
    private int nextAssemblyAddress = 0;

    public MainFrame() {
        super("8085 Simulator - Pro Trace Mode");
        cpu = new CPU();
        memory = new Memory();
        executor = new Executor(cpu, memory);
        parser = new AssemblyParser();

        // Initialize UI Components
        txtA = createRegField(); txtB = createRegField(); txtC = createRegField();
        txtD = createRegField(); txtE = createRegField(); txtH = createRegField();
        txtL = createRegField(); txtPC = createRegField(); txtSP = createRegField();
        chkS = createFlagCheckBox("S"); chkZ = createFlagCheckBox("Z");
        chkAC = createFlagCheckBox("AC"); chkP = createFlagCheckBox("P"); chkCY = createFlagCheckBox("CY");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLayout(new BorderLayout(15, 15));

        // --- Register Panel (Ordered) ---
        JPanel regs = new JPanel(new GridLayout(2, 5, 15, 10));
        regs.setBorder(BorderFactory.createTitledBorder("Registers (Hex)"));
        regs.add(createLabeledField("A", txtA)); regs.add(createLabeledField("B", txtB));
        regs.add(createLabeledField("D", txtD)); regs.add(createLabeledField("H", txtH));
        regs.add(createLabeledField("PC", txtPC));
        regs.add(createFlagPanel()); regs.add(createLabeledField("C", txtC));
        regs.add(createLabeledField("E", txtE)); regs.add(createLabeledField("L", txtL));
        regs.add(createLabeledField("SP", txtSP));

        // --- Assembly Input (Enter Key Enabled) ---
        JPanel asmPanel = new JPanel(new BorderLayout(8, 8));
        asmInput = new JTextField();
        asmInput.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        btnCompile = new JButton("Assemble");
        asmInput.addActionListener(e -> compileInstruction(null)); // Enter key
        asmPanel.add(asmInput, BorderLayout.CENTER);
        asmPanel.add(btnCompile, BorderLayout.EAST);

        // --- Memory Table (Hex Addressing) ---
        memTableModel = new DefaultTableModel(new String[]{"Addr (Hex)", "Val (Hex)", "Dec", "Char"}, 0);
        memTable = new JTable(memTableModel);
        JScrollPane memScroll = new JScrollPane(memTable);
        JPanel memContainer = new JPanel(new BorderLayout());
        memContainer.setBorder(BorderFactory.createTitledBorder("System Memory"));
        
        JPanel memSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memAddrInput = new JTextField("0000", 5);
        memDataInput = new JTextField("00", 3);
        btnStoreMem = new JButton("Write"); btnExamine = new JButton("Read");
        memSearch.add(new JLabel("Addr:")); memSearch.add(memAddrInput);
        memSearch.add(new JLabel("Data:")); memSearch.add(memDataInput);
        memSearch.add(btnStoreMem); memSearch.add(btnExamine);
        memContainer.add(memSearch, BorderLayout.NORTH);
        memContainer.add(memScroll, BorderLayout.CENTER);

        // --- Execution Log ---
        txtLog = new JTextArea();
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Execution Trace"));

        // --- Side-by-Side Layout ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(memContainer);
        centerPanel.add(logScroll);

        // --- Main Frame Assembly ---
        JPanel top = new JPanel(new BorderLayout());
        top.add(regs, BorderLayout.NORTH);
        top.add(asmPanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout());
        btnStep = new JButton("Step"); btnRun = new JButton("Run"); btnReset = new JButton("Reset");
        bottom.add(btnStep); bottom.add(btnRun); bottom.add(btnReset);
        add(bottom, BorderLayout.SOUTH);

        // Listeners
        btnCompile.addActionListener(this::compileInstruction);
        btnStoreMem.addActionListener(e -> {
            int addr = Integer.parseInt(memAddrInput.getText(), 16);
            int val = Integer.parseInt(memDataInput.getText(), 16);
            memory.writeByte(addr, (byte) val);
            updateMemoryTable();
        });
        btnExamine.addActionListener(e -> {
            int addr = Integer.parseInt(memAddrInput.getText(), 16);
            int val = memory.readByte(addr) & 0xFF;
            memDataInput.setText(String.format("%02X", val));
        });
        btnReset.addActionListener(e -> resetSim());
        btnStep.addActionListener(e -> step());
        btnRun.addActionListener(e -> runSim());

        updateMemoryTable();
        updateUI();
    }

    private void compileInstruction(ActionEvent e) {
        String code = asmInput.getText().trim();
        if (code.isEmpty()) return;
        AssemblyParser.ParseResult res = parser.parseLine(code);
        for (String step : res.steps) {
            log(step);
        }
        if (res.success) {
            int startAddr = nextAssemblyAddress; //
            for (byte b : res.bytes) {
                memory.writeByte(nextAssemblyAddress++, b);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : res.bytes) sb.append(String.format("%02X ", b & 0xFF));
            log("Opcode: " + sb.toString().trim());
            asmInput.setText("");
            updateMemoryTable();
        } else { 
            log("Fail: " + code); 
        }
    }

    private void updateMemoryTable() {
        memTableModel.setRowCount(0);
        for (int i = 0; i < 0x2100; i++) { // Wide range for LDA/STA 2050
            int val = memory.readByte(i) & 0xFF;
            memTableModel.addRow(new Object[]{String.format("%04X", i), String.format("%02X", val), val, (char)val});
        }
    }

    private void step() {
        executor.executeNext();
        log(executor.getLastMessage());
        updateUI();
        updateMemoryTable();
    }

    private void runSim() {
        new Thread(() -> {
            while (!executor.isHalted()) {
                executor.executeNext();
                SwingUtilities.invokeLater(() -> {
                    log(executor.getLastMessage());
                    updateUI();
                    updateMemoryTable();
                });
                try { Thread.sleep(50); } catch (Exception ex) {}
            }
        }).start();
    }

    private void resetSim() {
        cpu.reset(); memory.reset(); executor.reset();
        nextAssemblyAddress = 0;
        log("--- Reset ---");
        updateMemoryTable(); updateUI();
    }

    private JPanel createLabeledField(String l, JTextField f) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(l), BorderLayout.NORTH); p.add(f, BorderLayout.CENTER);
        return p;
    }

    private JPanel createFlagPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.add(chkS); p.add(chkZ); p.add(chkAC); p.add(chkP); p.add(chkCY);
        return p;
    }

    private JTextField createRegField() {
        JTextField f = new JTextField("00", 2);
        f.setEditable(false); f.setHorizontalAlignment(0);
        return f;
    }

    private JCheckBox createFlagCheckBox(String l) {
        JCheckBox c = new JCheckBox(l); c.setEnabled(false);
        return c;
    }

    private void log(String m) {
        txtLog.append(m + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void updateUI() {
        txtA.setText(String.format("%02X", cpu.A & 0xFF));
        txtB.setText(String.format("%02X", cpu.B & 0xFF));
        txtC.setText(String.format("%02X", cpu.C & 0xFF));
        txtD.setText(String.format("%02X", cpu.D & 0xFF));
        txtE.setText(String.format("%02X", cpu.E & 0xFF));
        txtH.setText(String.format("%02X", cpu.H & 0xFF));
        txtL.setText(String.format("%02X", cpu.L & 0xFF));
        txtPC.setText(String.format("%04X", cpu.PC & 0xFFFF));
        txtSP.setText(String.format("%04X", cpu.SP & 0xFFFF));
        chkS.setSelected(cpu.flagS);
        chkZ.setSelected(cpu.flagZ);
        chkAC.setSelected(cpu.flagAC);
        chkP.setSelected(cpu.flagP);
        chkCY.setSelected(cpu.flagCY);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
//java -cp . gui_main.MainFrame
//javac -cp . *.java gui_main/*.java assembler/*.java cpu/*.java execution/*.java memory/*.java
