package page.eiim.cubestats;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Grapher extends JPanel {
    private final double[] data;

    public Grapher(double[] data) {
        this.data = data;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.length < 2) return;

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        int padding = 40;

        double maxValue = Double.MIN_VALUE;
        for (double v : data) maxValue = Math.max(maxValue, v);

        double xScale = (double)(width - 2 * padding) / (data.length - 1);
        double yScale = (height - 2 * padding) / maxValue;

        // Draw axes
        g2.drawLine(padding, height - padding, width - padding, height - padding); // x-axis
        g2.drawLine(padding, padding, padding, height - padding); // y-axis

        // Draw lines between data points
        g2.setColor(Color.BLUE);
        for (int i = 0; i < data.length - 1; i++) {
            int x1 = (int)(padding + i * xScale);
            int y1 = (int)(height - padding - data[i] * yScale);
            int x2 = (int)(padding + (i + 1) * xScale);
            int y2 = (int)(height - padding - data[i + 1] * yScale);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    public static void setup(double[] data) {
        JFrame frame = new JFrame("Line Graph");
        Grapher graphPanel = new Grapher(data);
        graphPanel.setPreferredSize(new Dimension(600, 400));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(graphPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}