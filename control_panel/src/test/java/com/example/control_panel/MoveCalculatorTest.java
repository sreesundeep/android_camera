package com.example.control_panel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoveCalculatorTest {
    @Test
    public void testScrollTable() {
        int[][] moveTable = new int[][]{
                {0, -100, -200, -300, -400}, // i = 0
                {100, 0, -100, -200, -300}, // i = 1
                {200, 100, -200, -100, -200}, // i = 2
                {300, 200, 100, 0, -100}, // i = 3
                {400, 300, 200, 100, 0} // i = 4
        };
        int[][] scrollTable = new int[][] {
                {0, -100, 0, 100, 200}, // i = 0
                {-200, 0, 0, 100, 200}, // i = 1
                {-200, -100, 0, 100, 200}, // i = 2
                {-200, -100, 0, 0, 200}, // i = 3
                {-200, -100, 0, 100, 200} // i = 4
        };
        for (int i=0; i<5; i++) {
            for (int j=0; j<5; j++) {
                if (i != j) {
                    ModeSwitchScrollView.MoveCalculator calculator = new ModeSwitchScrollView.MoveCalculator(i, j, 100);
                    calculator.invoke();
                    String message = String.format("i=%d j=%d", i, j);
                    assertEquals("Move = " + message, moveTable[i][j], calculator.getMoveSize());
                    assertEquals("Scroll = " + message,scrollTable[i][j], calculator.getScrollX());
                }
            }
        }
    }
}
