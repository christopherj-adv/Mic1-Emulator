package Mic1;

import java.util.LinkedList;

public class ControlStore {
    private Instruction[] instructions;

    public ControlStore(LinkedList<String> prom) {
        instructions = new Instruction[prom.size()];

        for (int i = 0; i < prom.size(); i++) {
            instructions[i] = new Instruction(prom.get(i));
        }
    }

    public Instruction getInstruction(int idx) {
        return instructions[idx];
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < instructions.length; i++) {
            sb.append(i);

            if (i < 10)
                sb.append(":   ");
            else if (i < 100)
                sb.append(":  ");
            else
                sb.append(": ");

            sb.append(instructions[i].toFormattedString());
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < instructions.length - 1; i++)
            sb.append(instructions[i].toString() + "\n");
        sb.append(instructions[instructions.length - 1].toString());
        return sb.toString();
    }

    public class Instruction {
        String instruction;

        final Boolean AMUX;
        final short CONDITION, ALU, SHIFT;
        final Boolean MBR, MAR, RD, WR, ENC;
        final short C, B, A, ADDR;

        Instruction(String instruction) {
            this.instruction = instruction;

            if (instruction.length() != 32)
                throw new IllegalArgumentException("Instruction must be 32 bits long");

            for (int i = 0; i < instruction.length(); i++) {
                if (!(instruction.charAt(i) == '0' || instruction.charAt(i) == '1')) {
                    throw new IllegalArgumentException("Only binary characters permitted: " + instruction.charAt(i)
                            + "\nInstruction idx: " + i);
                }
            }

            AMUX = instruction.charAt(0) == '1';
            CONDITION = Short.parseShort(instruction.substring(1, 3), 2);

            if (CONDITION < 0 || CONDITION > 3)
                throw new IllegalArgumentException("CONDITION must be between 0 and 3");

            ALU = Short.parseShort(instruction.substring(3, 5), 2);

            if (ALU < 0 || ALU > 3)
                throw new IllegalArgumentException("ALU must be between 0 and 3");

            SHIFT = Short.parseShort(instruction.substring(5, 7), 2);

            if (SHIFT < 0 || SHIFT > 2)
                throw new IllegalArgumentException("SHIFT must be between 0 and 2");

            MBR = instruction.charAt(7) == '1';
            MAR = instruction.charAt(8) == '1';
            RD = instruction.charAt(9) == '1';
            WR = instruction.charAt(10) == '1';
            ENC = instruction.charAt(11) == '1';

            C = Short.parseShort(instruction.substring(12, 16), 2);
            B = Short.parseShort(instruction.substring(16, 20), 2);
            A = Short.parseShort(instruction.substring(20, 24), 2);
            ADDR = Short.parseShort(instruction.substring(24, 32), 2);
        }

        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();

            sb.append("| ");
            sb.append("AMUX: " + (AMUX ? 1 : 0));
            sb.append(" | ");
            sb.append("CONDITION: " + CONDITION);
            sb.append(" | ");
            sb.append("ALU: " + ALU);
            sb.append(" | ");
            sb.append("SHIFT: " + SHIFT);
            sb.append(" | ");

            sb.append("MBR: " + (MBR ? 1 : 0));
            sb.append(" | ");
            sb.append("MAR: " + (MAR ? 1 : 0));
            sb.append(" | ");
            sb.append("RD: " + (RD ? 1 : 0));
            sb.append(" | ");
            sb.append("WR: " + (WR ? 1 : 0));
            sb.append(" | ");
            sb.append("ENC: " + (ENC ? 1 : 0));
            sb.append(" | ");

            if (A < 10)
                sb.append("A:  " + A);
            else
                sb.append("A: " + A);
            sb.append(" | ");

            if (B < 10)
                sb.append("B:  " + B);
            else
                sb.append("B: " + B);
            sb.append(" | ");

            if (C < 10)
                sb.append("C:  " + C);
            else
                sb.append("C: " + C);

            sb.append(" | ");

            sb.append("ADDR: " + ADDR);

            if (ADDR > 99)
                sb.append(" |");
            else if (ADDR > 9)
                sb.append("  |");
            else
                sb.append("   |");

            sb.append("\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return instruction;
        }

        // Getters
        public Boolean getAMUX() {
            return AMUX;
        }

        public short getCONDITION() {
            return CONDITION;
        }

        public short getALU() {
            return ALU;
        }

        public short getSHIFT() {
            return SHIFT;
        }

        public Boolean getMBR() {
            return MBR;
        }

        public Boolean getMAR() {
            return MAR;
        }

        public Boolean getRD() {
            return RD;
        }

        public Boolean getWR() {
            return WR;
        }

        public Boolean getENC() {
            return ENC;
        }

        public short getA() {
            return A;
        }

        public short getB() {
            return B;
        }

        public short getC() {
            return C;
        }

        public short getADDR() {
            return ADDR;
        }
    }
}
