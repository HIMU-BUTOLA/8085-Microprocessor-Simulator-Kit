import javax.swing.*;
import java.awt.*;

public class EightZeroEightFiveSimulatorC extends JFrame {

    
    private int[] memory = new int[65536]; 
    private int A = 0, B = 0, H = 0, L = 0, PC = 0, nextLoadAddr = 0;
    private boolean flagZ = false;

    
    private JTextField txtA, txtB, txtH, txtL, cmdInput;
    private JTextField memAddrInput, memDataInput; 
    private JTextArea txtLog;

    public EightZeroEightFiveSimulatorC() { 
        setupUI();
        updateUI();
    }

    private void setupUI() {
        setTitle("8085 Pro Assembler & Memory Kit");
        setSize(750, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        
        JPanel regPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        regPanel.setBorder(BorderFactory.createTitledBorder("CPU Registers"));
        txtA = createRegField(); txtB = createRegField(); 
        txtH = createRegField(); txtL = createRegField();
        regPanel.add(new JLabel(" A:")); regPanel.add(txtA);
        regPanel.add(new JLabel(" B:")); regPanel.add(txtB);
        regPanel.add(new JLabel(" H:")); regPanel.add(txtH);
        regPanel.add(new JLabel(" L:")); regPanel.add(txtL);

        
        JPanel memPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        memPanel.setBorder(BorderFactory.createTitledBorder("Manual Memory Access"));
        memAddrInput = new JTextField("2050");
        memDataInput = new JTextField("00");
        JButton btnStoreMem = new JButton("STORE IN RAM");
        JButton btnCheckMem = new JButton("EXAMINE ADDR");

        memPanel.add(new JLabel("Address (Hex):")); memPanel.add(memAddrInput);
        memPanel.add(new JLabel("Data (Hex):")); memPanel.add(memDataInput);
        memPanel.add(btnStoreMem);
        memPanel.add(btnCheckMem);

        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Assembler (Enter Commands)"));
        cmdInput = new JTextField();
        cmdInput.setFont(new Font("Monospaced", Font.BOLD, 18));
        JButton btnEnter = new JButton("ASSEMBLE");
        inputPanel.add(cmdInput, BorderLayout.CENTER);
        inputPanel.add(btnEnter, BorderLayout.SOUTH);

        
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(new Color(0, 255, 0));
        JScrollPane scroll = new JScrollPane(txtLog);

        
        JButton btnRun = new JButton("RUN PROGRAM FROM 0000H");
        btnRun.setFont(new Font("Arial", Font.BOLD, 16));
        btnRun.setBackground(new Color(34, 139, 34));
        btnRun.setForeground(Color.WHITE);

        

        
        btnStoreMem.addActionListener(e -> {
            try {
                int addr = Integer.parseInt(memAddrInput.getText(), 16);
                int data = Integer.parseInt(memDataInput.getText(), 16);
                memory[addr] = data & 0xFF;
                log("[MEM] Stored " + String.format("%02X", data) + " at " + String.format("%04X", addr));
            } catch (Exception ex) { log("! Invalid Hex for Memory"); }
        });

        
        btnEnter.addActionListener(e -> processCommand());
        cmdInput.addActionListener(e -> processCommand());

        
        btnRun.addActionListener(e -> runProgram());

        
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.add(memPanel, BorderLayout.NORTH);
        
        add(regPanel, BorderLayout.NORTH);
        add(westPanel, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.NORTH);
        southPanel.add(btnRun, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    private JTextField createRegField() {
        JTextField f = new JTextField("00");
        f.setEditable(false);
        f.setHorizontalAlignment(JTextField.CENTER);
        return f;
    }

    private void processCommand() {
        String cmd = cmdInput.getText().toUpperCase().trim();
        if (cmd.isEmpty()) return;
        try {
            if (cmd.startsWith("MVI A,")) {
                int val = Integer.parseInt(cmd.split(",")[1].trim(), 16);
                memory[nextLoadAddr++] = 0x3E; memory[nextLoadAddr++] = val & 0xFF;
                log("ASM: " + cmd);
            } else if (cmd.startsWith("LDA ")) { 
                int addr = Integer.parseInt(cmd.split(" ")[1].trim(), 16);
                memory[nextLoadAddr++] = 0x3A;
                memory[nextLoadAddr++] = addr & 0xFF;
                memory[nextLoadAddr++] = (addr >> 8) & 0xFF;
                log("ASM: " + cmd);
            } else if (cmd.equals("HLT")) {
                memory[nextLoadAddr++] = 0x76;
                log("ASM: HLT (Program Ready)");
            }
            else if (cmd.equals("MOV B,A")) {
                memory[nextLoadAddr++] = 0x47; 
                log("ASM: MOV B,A (Stored 47)");
            }
            else if (cmd.equals("MOV A,B")) {
                memory[nextLoadAddr++] = 0x78; 
                log("ASM: MOV A,B (Stored 78)");
            }
            else if (cmd.equals("ADD B")) {
                memory[nextLoadAddr++] = 0x80; 
                log("ASM: ADD B (Stored 80)");
            }
            else { log("! Unknown Command"); }
        } catch (Exception ex) { log("! Format Error"); }
        cmdInput.setText("");
    }

    private void runProgram() {
        PC = 0; boolean running = true;
        log("--- Executing ---");
        while (running) {
            int opcode = memory[PC] & 0xFF;
            switch (opcode) {
                case 0x3E: A = memory[PC+1]; PC += 2; break;
                case 0x3A: 
                    int target = (memory[PC+2] << 8) | memory[PC+1];
                    A = memory[target]; PC += 3; break;
                case 0x76: running = false; log("Done."); break;
                case 0x47: B = A; 
                    PC++; 
                    break;
                
                case 0x78: 
                    A = B; 
                    PC++; 
                    break;
                case 0x80: 
                    A = (A + B) & 0xFF; 
                    flagZ = (A == 0);   
                    PC++; 
                    break;
                default: running = false; break;
            }
        }
        updateUI();
    }

    private void updateUI() {
        txtA.setText(String.format("%02X", A));
        txtB.setText(String.format("%02X", B));
        txtH.setText(String.format("%02X", H));
        txtL.setText(String.format("%02X", L));
    }

    private void log(String m) { txtLog.append(m + "\n"); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EightZeroEightFiveSimulatorC().setVisible(true));
    }
}