package com.example.opencv_app;

public class Color_Identify {
    int[][] color_value = new int[6][3];
    int[] RED = new int[]{255, 0x20, 0x2F};
    int[] WHITE = new int[]{255, 255, 255};
    int[] BLUE = new int[]{0, 0x81, 255};
    int[] GREEN = new int[]{0, 0x9D, 0x54};
    int[] ORANGE = new int[]{0xE8, 0X70, 0x0f};
    int[] YELLOW = new int[]{0xfe, 0xee, 0x4e};

    String[] color_string = new String[]{"r", "w", "b", "g", "o", "y"};

    int R_;
    int G_;
    int B_;

    public Color_Identify(int r, int g, int b) {
        color_value[0] = RED;
        color_value[1] = WHITE;
        color_value[2] = BLUE;
        color_value[3] = GREEN;
        color_value[4] = ORANGE;
        color_value[5] = YELLOW;
        this.R_ = r;
        this.G_ = g;
        this.B_ = b;
    }

    public String getColor() {
        int index = 0;
        int result = 0;
        int distance = 0x0FFFFFF;
        for (int[] color : color_value) {
            int temp_value = (R_ - color[0]) * (R_ - color[0]) + (G_ - color[1]) * (G_ - color[1]) + (B_ - color[2]) * (B_ - color[1]);
            if (temp_value < distance) {
                distance = temp_value;
                result = index;
            }
            index++;
        }
        return color_string[result];
    }
}
