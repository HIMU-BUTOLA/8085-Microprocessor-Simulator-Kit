8085 Microprocessor SimulatorA high-performance, modular emulator for the Intel 8085 Microprocessor, developed in Java using the Swing framework. This project provides a complete environment to write, assemble, and execute 8085 Assembly language, offering a deep look into internal CPU operations. 



 Key FeaturesIntegrated Assembly Parser: A custom AssemblyParser.java that converts standard mnemonics (like MOV, ADD, LDA) into executable machine code.  Live Register Tracking: Real-time monitoring of all primary registers: Accumulator (A), B, C, D, E, H, L, Program Counter (PC), and Stack Pointer (SP).  Status Flag Visualization: Detailed display of the condition code register, tracking Sign (S), Zero (Z), Auxiliary Carry (AC), Parity (P), and Carry (CY).  Comprehensive Memory Interface: A virtual 64KB memory space with tools for manual data entry, memory examination, and hex file loading.  Dual Execution Modes:Step Mode: Execute programs instruction-by-instruction for precise debugging.  Run Mode: Automated execution with a simulated internal clock.  Execution Trace Log: A dedicated log window that records every executed opcode and provides status messages. 

 
 Architecture OverviewThe project is built with a strictly decoupled architecture to ensure technical accuracy and modularity:assembler: Handles the lexical analysis and binary encoding of assembly instructions.  cpu: Defines the physical hardware state, including registers and flag-packing logic (PSW).  execution: The instruction engine that simulates the ALU and control unit logic.  memory: Simulates the system's RAM and provides byte-level access controls.  gui_main: A centralized dashboard that synchronizes the hardware state with the visual interface.  


 
 Technical DetailsRegister Pair LogicThe simulator accurately mimics the 8085's ability to use 8-bit registers in pairs (BC, DE, HL) to store 16-bit addresses using bitwise shifting:
((H & 0xFF) << 8) | (L & 0xFF);  Flag Register (PSW)Status flags are managed as individual booleans and packed into the Program Status Word (PSW) following the hardware-defined bit-mapping of the 8085.  
 
 
 Getting StartedPrerequisitesJava Development Kit (JDK) 8 or higher.Installation & RunCompile all modules:Bashjavac -cp . *.java gui_main/*.java assembler/*.java cpu/*.java execution/*.java memory/*.java
Launch the application:Bashjava -cp . gui_main.MainFrame
Programming ExampleLoad this into the simulator to test basic arithmetic and memory storage:Code snippetMVI A, 0AH   ; Load 10 (decimal) into Accumulator




MVI B, 05H   ; Load 5 into Register B

ADD B        ; A = A + B (Result: 0FH)

STA 2050H    ; Store result in memory address 2050H

HLT          ; Stop execution
