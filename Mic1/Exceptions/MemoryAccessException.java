package Mic1.Exceptions;

import java.io.FileWriter;

public class MemoryAccessException extends Exception {
    public MemoryAccessException(String state, int address) {
        System.out.println("Attempted memory access to invalid location");
        System.out.println("ProgramMemory[" + address + "]");
        System.out.print("Segmentation fault ");

        try {
            FileWriter fw = new FileWriter("core.bin");

            fw.write(state);
            fw.flush();
            fw.close();

            System.out.println("(core dumped)");
        } catch (Exception e) {
            System.out.println("\nFailed to write core dump.");
        }

        System.exit(1);
    }
}
