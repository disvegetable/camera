package com.example.opencv_app;

public class MyCube {
    String[][] F = new String[3][3];
    String[][] U = new String[3][3];
    String[][] D = new String[3][3];
    String[][] L = new String[3][3];
    String[][] R = new String[3][3];
    String[][] B = new String[3][3];

    public MyCube() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                F[i][j] = "g";
                U[i][j] = "w";
                D[i][j] = "y";
                L[i][j] = "o";
                R[i][j] = "r";
                B[i][j] = "b";
            }
        }
    }

    public String resultMoving(String[][] Status) {
        if (is_U_inv(Status)) {
            updateCube("U'");
            return "U'";
        } else if (is_U(Status)) {
            updateCube("U");
            return "U";
        } else if (is_L(Status)) {
            updateCube("L");
            return "L";
        } else if (is_L_inv(Status)) {
            updateCube("L'");
            return "L'";
        } else if (is_R(Status)) {
            updateCube("R");
            return "R";
        } else if (is_R_inv(Status)) {
            updateCube("R'");
            return "R'";
        } else if (is_F(Status)) {
            updateCube("F");
            return "F";
        } else if (is_F_inv(Status)) {
            updateCube("F'");
            return "F'";
        } else if (is_B(Status)) {
            updateCube("B");
            return "B";
        } else if (is_B_inv(Status)) {
            updateCube("B'");
            return "B'";
        } else if (is_D(Status)) {
            updateCube("D");
            return "D";
        } else if (is_D_inv(Status)) {
            updateCube("D'");
            return "D'";
        } else {
            return "unstable";
        }
    }

    private void updateCube(String Order) {
        //U
        if (Order.equals("U")) {
            rotate(U);
            //update other lines
            String[] tempLines = new String[3];
            for (int i = 0; i < 3; i++) {
                tempLines[i] = B[2][i];
            }

            for (int i = 0; i < 3; i++) {
                B[2][i] = L[2 - i][2];
                L[2 - i][2] = F[0][2 - i];
                F[0][2 - i] = R[i][0];
                R[i][0] = tempLines[i];
            }
        }
        //U'
        else if (Order.equals("U'")) {
            rotate_inv(U);
            //update other lines
            String[] tempLines = new String[3];
            for (int i = 0; i < 3; i++) {
                tempLines[i] = B[2][i];
            }

            for (int i = 0; i < 3; i++) {
                B[2][i] = R[i][0];
                R[i][0] = F[0][2 - i];
                F[0][2 - i] = L[2 - i][2];
                L[2 - i][2] = tempLines[i];
            }
        }
        //L
        else if (Order.equals("L")) {
            rotate(L);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = D[i][0];
            }
            for (int i = 0; i < 3; i++) {
                D[i][0] = F[i][0];
                F[i][0] = U[i][0];
                U[i][0] = B[i][0];
                B[i][0] = temp[i];
            }
        }
        //L'
        else if (Order.equals("L'")) {
            rotate_inv(L);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = B[i][0];
            }
            for (int i = 0; i < 3; i++) {
                B[i][0] = U[i][0];
                U[i][0] = F[i][0];
                F[i][0] = D[i][0];
                D[i][0] = temp[i];
            }
        }
        //R
        else if (Order.equals("R")) {
            rotate(R);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = B[i][2];
            }
            for (int i = 0; i < 3; i++) {
                B[i][2] = U[i][2];
                U[i][2] = F[i][2];
                F[i][2] = D[i][2];
                D[i][2] = temp[i];
            }
        }
        //R'
        else if (Order.equals("R'")) {
            rotate_inv(R);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = D[i][2];
            }
            for (int i = 0; i < 3; i++) {
                D[i][2] = F[i][2];
                F[i][2] = U[i][2];
                U[i][2] = B[i][2];
                B[i][2] = temp[i];
            }
        }
        //F
        else if (Order.equals("F")) {
            rotate(F);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = L[2][i];
            }
            for (int i = 0; i < 3; i++) {
                L[2][i] = D[0][2 - i];
                D[0][2 - i] = R[2][i];
                R[2][i] = U[2][i];
                U[2][i] = temp[i];
            }
        }
        //F'
        else if (Order.equals("F'")) {
            rotate_inv(F);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = L[2][i];
            }
            for (int i = 0; i < 3; i++) {
                L[2][i] = U[2][i];
                U[2][i] = R[2][i];
                R[2][i] = D[0][2 - i];
                D[0][2 - i] = temp[i];
            }
        }
        //D
        else if (Order.equals("D")) {
            rotate(D);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = F[2][i];
            }
            for (int i = 0; i < 3; i++) {
                F[2][i] = L[i][0];
                L[i][0] = B[0][2 - i];
                B[0][2 - i] = R[2 - i][2];
                R[2 - i][2] = temp[i];
            }
        }
        //D'
        else if (Order.equals("D'")) {
            rotate_inv(D);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = F[2][i];
            }
            for (int i = 0; i < 3; i++) {
                F[2][i] = R[2 - i][2];
                R[2 - i][2] = B[0][2 - i];
                B[0][2 - i] = L[i][0];
                L[i][0] = temp[i];
            }
        }
        //B
        else if (Order.equals("B")) {
            rotate(B);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = D[2][i];
            }
            for (int i = 0; i < 3; i++) {
                D[2][i] = L[0][2 - i];
                L[0][2 - i] = U[0][2 - i];
                U[0][2 - i] = R[0][2 - i];
                R[0][2 - i] = temp[i];
            }
        }
        //B'
        else {
            rotate_inv(B);
            String[] temp = new String[3];
            for (int i = 0; i < 3; i++) {
                temp[i] = D[2][i];
            }
            for (int i = 0; i < 3; i++) {
                D[2][i] = R[0][2 - i];
                R[0][2 - i] = U[0][2 - i];
                U[0][2 - i] = L[0][2 - i];
                L[0][2 - i] = temp[i];
            }
        }
    }


    //judge moving
    private boolean is_U_inv(String[][] Status) {
        boolean upper = true;
        boolean line = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (!U[i][j].equals(Status[2 - j][i])) {
                    upper = false;
                    break;
                }
            }
        }
        if (Status[3][0].equals(L[0][2]) && Status[3][1].equals(L[1][2]) && Status[3][2].equals(L[2][2])) {
            line = true;
        }
        return upper && line;
    }

    private boolean is_U(String[][] Status) {
        boolean upper = true;
        boolean line = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (!U[i][j].equals(Status[j][2 - i])) {
                    upper = false;
                    break;
                }
            }
        }
        if (Status[3][0].equals(R[2][0]) && Status[3][1].equals(R[1][0]) && Status[3][2].equals(R[0][0])) {
            line = true;
        }
        return upper && line;
    }

    private boolean is_L(String[][] Status) {
        boolean upLine = true;
        boolean frontLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[i + 3][0].equals(U[i][0])) {
                frontLine = false;
                break;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!Status[i][0].equals(B[i][0])) {
                upLine = false;
                break;
            }
        }
        return upLine && frontLine;
    }

    private boolean is_L_inv(String[][] Status) {
        boolean upLine = true;
        boolean frontLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[i + 3][0].equals(D[i][0])) {
                frontLine = false;
                break;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!Status[i][0].equals(F[i][0])) {
                upLine = false;
                break;
            }
        }
        return upLine && frontLine;
    }

    private boolean is_R(String[][] Status) {
        boolean upLine = true;
        boolean frontLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[i + 3][2].equals(D[i][2])) {
                frontLine = false;
                break;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!Status[i][2].equals(F[i][2])) {
                upLine = false;
                break;
            }
        }
        return upLine && frontLine;
    }

    private boolean is_R_inv(String[][] Status) {
        boolean upLine = true;
        boolean frontLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[i + 3][2].equals(U[i][2])) {
                frontLine = false;
                break;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!Status[i][2].equals(B[i][2])) {
                upLine = false;
                break;
            }
        }
        return upLine && frontLine;
    }

    private boolean is_F(String[][] Status) {
        boolean front = true;
        boolean line = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (!F[i][j].equals(Status[3 + j][2 - i])) {
                    front = false;
                    break;
                }
            }
        }
        if (Status[2][0].equals(L[2][0]) && Status[2][1].equals(L[2][1]) && Status[2][2].equals(L[2][2])) {
            line = true;
        }
        return front && line;
    }

    private boolean is_F_inv(String[][] Status) {
        boolean front = true;
        boolean line = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (!F[i][j].equals(Status[5 - j][i])) {
                    front = false;
                    break;
                }
            }
        }
        if (Status[2][0].equals(R[2][0]) && Status[2][1].equals(R[2][1]) && Status[2][2].equals(R[2][2])) {
            line = true;
        }
        return front && line;
    }

    private boolean is_B_inv(String[][] Status) {
        boolean leftLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[0][i].equals(L[0][i])) {
                leftLine = false;
                break;
            }
        }
        return leftLine;
    }

    private boolean is_B(String[][] Status) {
        boolean rightLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[0][i].equals(R[0][i])) {
                rightLine = false;
                break;
            }
        }
        return rightLine;
    }

    private boolean is_D(String[][] Status) {
        boolean leftLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[5][i].equals(L[i][0])) {
                leftLine = false;
                break;
            }
        }
        return leftLine;
    }

    private boolean is_D_inv(String[][] Status) {
        boolean rightLine = true;
        for (int i = 0; i < 3; i++) {
            if (!Status[5][i].equals(R[2 - i][2])) {
                rightLine = false;
                break;
            }
        }
        return rightLine;
    }

    private void rotate(String[][] face) {
        String[][] temp = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[j][2 - i] = face[i][j];
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                face[i][j] = temp[i][j];
            }
        }
    }

    private void rotate_inv(String[][] face) {
        String[][] temp = new String[3][3];
        //update up side
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[2 - j][i] = face[i][j];
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                face[i][j] = temp[i][j];
            }
        }
    }
}