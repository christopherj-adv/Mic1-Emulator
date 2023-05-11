import java.util.LinkedList;
import java.util.Scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import Mic1.ControlStore;
import Mic1.ControlStore.Instruction;
import Mic1.Exceptions.*;

public class Mic1 {
    boolean halted = true;

    int MPC = 0;
    int pmlen = 4096;

    // Registers
    short programCounter = 0;
    short accumulator = 0;
    short stackPointer = 4095;
    short instructionRegister = 0;
    short temporaryInstructionRegister = 0;

    // Masks
    final short AMASK = 0b11111111; // 8-bit address mask
    final short SMASK = 0b00111111; // 6-bit stack mask

    // Memory Access Registers
    int MAR = 0;
    short MBR = 0;

    // Scratchpad Registers
    short A = 0, B = 0, C = 0, D = 0, E = 0, F = 0;

    // ALU Flags
    boolean ALUZ = false;
    boolean ALUN = false;

    // MPC Flag
    boolean Mmux = false;

    String[] programMemory;
    ControlStore controlStore;

    Mic1(String promPath) throws PromFileException {
        LinkedList<String> promData = new LinkedList<String>();

        if (!loadPromFile(promPath, promData))
            throw new PromFileException("Failed to load PROM file.");

        if (promData.size() > 256)
            throw new PromFileException("PROM file too large.");

        controlStore = new ControlStore(promData);
    }

    void execute() {
        halted = false;

        while (!halted) {
            ALUZ = ALUN = Mmux = false;

            Instruction currentInstruction = controlStore.getInstruction(MPC);
            System.out.println("Running instruction at MPC: " + MPC);
            System.out.println(currentInstruction.toFormattedString());

            MPC++;

            if (currentInstruction.getRD() && currentInstruction.getWR()) // Check for halt
            {
                halted = true;
            } else {
                // Get data to feed into the ALU.
                short AMUX;

                if (currentInstruction.getAMUX())
                    AMUX = MBR;
                else {
                    AMUX = fetchRegisterBus(currentInstruction.getA());
                }

                short b_latch = fetchRegisterBus(currentInstruction.getB());
                if (currentInstruction.getMAR()) // Write to MAR if need be.
                    MAR = b_latch;

                // Run ALU and shift logic.
                short output = simulateALU(currentInstruction.getALU(), AMUX, b_latch);

                switch (currentInstruction.getSHIFT()) {
                    case 1:
                        output >>= 1;
                        System.out.println("Shifted right: " + output);

                        break;
                    case 2:
                        output <<= 1;
                        System.out.println("Shifted left: " + output);
                        break;
                }

                // Figure out where to write the data.
                short data_out_loc = currentInstruction.getC();
                if (currentInstruction.getENC()) {
                    switch (data_out_loc) {
                        case 0:
                            programCounter = output;
                            break;
                        case 1:
                            accumulator = output;
                            break;
                        case 2:
                            stackPointer = output;
                            break;
                        case 3:
                            instructionRegister = output;
                            break;
                        case 4:
                            temporaryInstructionRegister = output;
                            break;
                        /*
                         * zr, po, no, amask, smask, uneditable
                         */

                        // Scratch pad registers
                        case 10:
                            A = output;
                            break;
                        case 11:
                            B = output;
                            break;
                        case 12:
                            C = output;
                            break;
                        case 13:
                            D = output;
                            break;
                        case 14:
                            E = output;
                            break;
                        case 15:
                            F = output;
                            break;
                    }
                }

                if (currentInstruction.getMBR()) // Write to MBR if need be.
                    MBR = output;

                // Branching logic.
                switch (currentInstruction.getCONDITION()) {
                    case 1: // Check ALUN
                        if (ALUN)
                            Mmux = true;
                        break;
                    case 2: // Check ALUZ
                        if (ALUZ)
                            Mmux = true;
                        break;
                    case 3: // Always branch
                        Mmux = true;
                        break;
                }

                if (Mmux)
                    MPC = currentInstruction.getADDR();

                // Now process reading / writing.
                if (currentInstruction.getRD()) {
                    if (MAR >= pmlen)
                        dumpProgram();
                    else {
                        System.out.println("Setting mbr to " + MAR + ": " + programMemory[MAR]);
                        MBR = Short.parseShort(programMemory[MAR], 2);
                        System.out.println("MBR: " + MBR);
                    }
                } else if (currentInstruction.getWR())
                    programMemory[MAR] = String.format("%16s", Integer.toBinaryString(MBR));// .replace(' ', '0');
            }
        }
    }

    short simulateALU(short ALUCODE, short a, short b) {
        int retVal = 0; // Int, not short, because Java is weird.

        System.out.println("a: " + a + "\nb: " + b + "\n");

        // TODO: Implement Proper ALU logic.
        switch (ALUCODE) {
            case 0:
                System.out.println("Computing " + a + " + " + b);
                retVal = a + b;
                break;
            case 1:
                System.out.println("Computing " + a + " & " + b);
                retVal = a & b;
                break;
            case 2:
                System.out.println("Passing through a: " + a);
                retVal = a;
                break;
            case 3:
                System.out.println("Inverting a: " + a);
                retVal = ~a & 0x7FFF;
                break;
        }

        if (retVal < 0)
            retVal += 65536;

        ALUZ = retVal == 0;
        ALUN = retVal < 0;

        System.out.println("ALU Output: " + retVal);
        System.out.println();

        return (short) (retVal % 65536);
    }

    private short fetchRegisterBus(short busVal) {
        short retVal = 0;

        switch (busVal) {
            case 0:
                retVal = programCounter;
                break;
            case 1:
                retVal = accumulator;
                break;
            case 2:
                retVal = stackPointer;
                break;
            case 3:
                retVal = instructionRegister;
                break;
            case 4:
                retVal = temporaryInstructionRegister;
                break;
            case 5:
                retVal = 0;
                break;
            case 6:
                retVal = 1;
                break;
            case 7:
                retVal = -1;
                break;
            case 8:
                retVal = AMASK;
                break;
            case 9:
                retVal = SMASK;
                break;
            case 10:
                retVal = A;
                break;
            case 11:
                retVal = B;
                break;
            case 12:
                retVal = C;
                break;
            case 13:
                retVal = D;
                break;
            case 14:
                retVal = E;
                break;
            case 15:
                retVal = F;
                break;
        }

        return retVal;
    }

    private short twosComplement(short val) {
        return (short) ((~val + 1) & 0xFFFF);
    }

    private Boolean loadInstructionSet(String path) {
        int programMemoryIdx = 0;
        File f = new File(path);
        Scanner s;

        try {
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            return false;
        }

        // TODO: Write transmit / receive logic.
        programMemory = new String[pmlen];

        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (line.length() != 16)
                throw new IllegalArgumentException("Instruction must be 16 bits long");

            programMemory[programMemoryIdx++] = line;
        }

        for (int i = programMemoryIdx; i < pmlen; i++)
            programMemory[i] = "1111111111111111"; // Halt Equivalent

        s.close();
        return true;
    }

    private Boolean loadPromFile(String path, LinkedList<String> promData) {
        File f = new File(path);
        Scanner s;

        try {
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            return false;
        }

        while (s.hasNextLine())
            promData.add(s.nextLine());

        s.close();
        return true;
    }

    private void dumpProgram() {
        System.out.print("Segmentation fault ");

        try {
            FileWriter fw = new FileWriter("core.bin");

            fw.write("Mic1 State:");
            fw.write("PC: " + programCounter + "\n");
            fw.write("AC: " + accumulator + "\n");
            fw.write("SP: " + stackPointer + "\n");
            fw.write("IR: " + instructionRegister + "\n");
            fw.write("TIR: " + temporaryInstructionRegister + "\n");
            fw.write("MAR: " + MAR + "\n");
            fw.write("MBR: " + MBR + "\n");
            fw.write("A: " + A + "\n");
            fw.write("B: " + B + "\n");
            fw.write("C: " + C + "\n");
            fw.write("D: " + D + "\n");
            fw.write("E: " + E + "\n");
            fw.write("F: " + F + "\n");
            fw.write("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n");

            fw.flush();

            fw.write("Program Memory:\n");

            for (int i = 0; i < pmlen; i++)
                fw.write(programMemory[i] + "\n");

            fw.flush();
            fw.close();

            System.out.println("(core dumped)");
        } catch (Exception e) {
            System.out.println("Failed to write core dump.");
        }

        System.exit(1);
    }

    // Getters
    public boolean isHalted() {
        return halted;
    }

    private void printState() {
        System.out.println("PC: " + programCounter);
        System.out.println("AC: " + accumulator);
        System.out.println("SP: " + stackPointer);
        System.out.println("A: " + A);
        System.out.println("B: " + B);
        System.out.println("C: " + C);
        System.out.println("D: " + D);
        System.out.println("E: " + E);
        System.out.println("F: " + F);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Mic1 <prom file> <input file>.");
            System.exit(1);
        }

        Mic1 mic1Emulator;

        try {
            mic1Emulator = new Mic1(args[0]);
        } catch (PromFileException e) {
            System.out.println(e.getMessage());
            System.exit(1);
            return;
        }

        mic1Emulator.loadInstructionSet(args[1]);

        // while (true) {
        // mic1Emulator.execute();
        // mic1Emulator.printState();
        // }
    }
}
