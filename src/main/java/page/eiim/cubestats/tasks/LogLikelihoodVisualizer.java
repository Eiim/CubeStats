package page.eiim.cubestats.tasks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import page.eiim.cubestats.tasks.TaskBayesEval.LogLikelihood;
import page.eiim.cubestats.tasks.TaskBayesEval.Parameters;

public class LogLikelihoodVisualizer extends JPanel {
	private static final long serialVersionUID = -6955647727114223183L;
	private ArrayList<Point2D.Double> dataPoints;
	private ArrayList<Point2D.Double> dataPoints2;
    private double minX, maxX, minY, maxY;
    
    public LogLikelihoodVisualizer(ArrayList<Point2D.Double> dataPoints) {
        this(dataPoints, null);
    }
    
    public LogLikelihoodVisualizer(ArrayList<Point2D.Double> dataPoints, ArrayList<Point2D.Double> dataPoints2) {
        this.dataPoints = dataPoints;
        this.dataPoints2 = dataPoints2;
        calculateBounds();
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
    }
    
    private void calculateBounds() {
        if (dataPoints.isEmpty()) return;
        
        minX = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
        
        for (Point2D.Double point : dataPoints) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        
        // Include second dataset in bounds if present
        if (dataPoints2 != null) {
            for (Point2D.Double point : dataPoints2) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
            }
        }
        
        // Add some padding
        double xPadding = (maxX - minX) * 0.05;
        double yPadding = (maxY - minY) * 0.05;
        minX -= xPadding;
        maxX += xPadding;
        minY -= yPadding;
        maxY += yPadding;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int padding = 60;
        
        // Draw axes
        g2.setColor(Color.BLACK);
        g2.drawLine(padding, height - padding, width - padding, height - padding); // X-axis
        g2.drawLine(padding, padding, padding, height - padding); // Y-axis
        
        // Draw axis labels
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Time (seconds)", width / 2 - 40, height - 20);
        g2.rotate(-Math.PI / 2);
        g2.drawString("Log-Likelihood (Time Part)", -height / 2 - 80, 20);
        g2.rotate(Math.PI / 2);
        
        // Draw tick marks and labels
        drawTicks(g2, width, height, padding);
        
        // Draw the line graph
        if (dataPoints.size() > 1) {
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            
            for (int i = 0; i < dataPoints.size() - 1; i++) {
                Point2D.Double p1 = dataPoints.get(i);
                Point2D.Double p2 = dataPoints.get(i + 1);
                
                int x1 = transformX(p1.x, width, padding);
                int y1 = transformY(p1.y, height, padding);
                int x2 = transformX(p2.x, width, padding);
                int y2 = transformY(p2.y, height, padding);
                
                g2.drawLine(x1, y1, x2, y2);
            }
            
            // Draw points
            g2.setColor(Color.RED);
            for (Point2D.Double point : dataPoints) {
                int x = transformX(point.x, width, padding);
                int y = transformY(point.y, height, padding);
                g2.fillOval(x - 3, y - 3, 6, 6);
            }
        }
        
        // Draw second line if present
        if (dataPoints2 != null && dataPoints2.size() > 1) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(2));
            
            for (int i = 0; i < dataPoints2.size() - 1; i++) {
                Point2D.Double p1 = dataPoints2.get(i);
                Point2D.Double p2 = dataPoints2.get(i + 1);
                
                int x1 = transformX(p1.x, width, padding);
                int y1 = transformY(p1.y, height, padding);
                int x2 = transformX(p2.x, width, padding);
                int y2 = transformY(p2.y, height, padding);
                
                g2.drawLine(x1, y1, x2, y2);
            }
            
            // Draw points
            g2.setColor(Color.ORANGE);
            for (Point2D.Double point : dataPoints2) {
                int x = transformX(point.x, width, padding);
                int y = transformY(point.y, height, padding);
                g2.fillOval(x - 3, y - 3, 6, 6);
            }
        }
    }
    
    private int transformX(double x, int width, int padding) {
        return (int) (padding + (x - minX) / (maxX - minX) * (width - 2 * padding));
    }
    
    private int transformY(double y, int height, int padding) {
        return (int) (height - padding - (y - minY) / (maxY - minY) * (height - 2 * padding));
    }
    
    private void drawTicks(Graphics2D g2, int width, int height, int padding) {
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        
        // X-axis ticks
        int numXTicks = 10;
        for (int i = 0; i <= numXTicks; i++) {
            double x = minX + (maxX - minX) * i / numXTicks;
            int screenX = transformX(x, width, padding);
            g2.drawLine(screenX, height - padding, screenX, height - padding + 5);
            g2.drawString(String.format("%.0f", x), screenX - 10, height - padding + 20);
        }
        
        // Y-axis ticks
        int numYTicks = 10;
        for (int i = 0; i <= numYTicks; i++) {
            double y = minY + (maxY - minY) * i / numYTicks;
            int screenY = transformY(y, height, padding);
            g2.drawLine(padding - 5, screenY, padding, screenY);
            g2.drawString(String.format("%.2f", y), padding - 45, screenY + 5);
        }
    }
    
    /**
     * Static method to calculate and visualize log-likelihoods
     * @param prior The Parameters object representing the prior distribution
     * @param times List of solve times
     * @param dates List of solve dates
     * @param dnfs Number of DNFs
     */
    public static void calculateAndVisualize(Parameters prior, List<Double> times, 
                                            List<LocalDate> dates, int dnfs) {
        calculateAndVisualize(prior, times, dates, dnfs, null, null, null, 0);
    }
    
    /**
     * Static method to calculate and visualize two sets of log-likelihoods
     * @param prior1 The first Parameters object
     * @param times1 First list of solve times
     * @param dates1 First list of solve dates
     * @param dnfs1 First DNF count
     * @param prior2 The second Parameters object (null for single line)
     * @param times2 Second list of solve times
     * @param dates2 Second list of solve dates
     * @param dnfs2 Second DNF count
     */
    public static void calculateAndVisualize(Parameters prior1, List<Double> times1, 
                                            List<LocalDate> dates1, int dnfs1,
                                            Parameters prior2, List<Double> times2,
                                            List<LocalDate> dates2, int dnfs2) {
        ArrayList<Point2D.Double> dataPoints1 = new ArrayList<>();
        
        // Calculate log-likelihoods for times from 200 to 12000 (2 to 120 seconds)
        for (int time = 200; time <= 12000; time += 100) {
            LogLikelihood ll = TaskBayesEval.getLL(prior1, times1, dates1, dnfs1, time);
            // X-axis: time divided by 100 (to get seconds)
            // Y-axis: time part of log-likelihood
            dataPoints1.add(new Point2D.Double(time / 100.0, ll.timePart()));
        }
        
        ArrayList<Point2D.Double> dataPoints2 = null;
        if (prior2 != null) {
            dataPoints2 = new ArrayList<>();
            for (int time = 200; time <= 12000; time += 100) {
                LogLikelihood ll = TaskBayesEval.getLL(prior2, times2, dates2, dnfs2, time);
                dataPoints2.add(new Point2D.Double(time / 100.0, ll.timePart()));
            }
        }
        
        // Create and display the visualization
        final ArrayList<Point2D.Double> finalDataPoints2 = dataPoints2;
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Log-Likelihood Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new LogLikelihoodVisualizer(dataPoints1, finalDataPoints2));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    // Example usage
    public static void main(String[] args) {
        // Example with empty data
        Parameters prior = new Parameters(8.017, 0.1511, 2.372, 0.06917, 1.1791, 72.79);
        List<Double> times = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        int dnfs = 0;
        
        //calculateAndVisualize(prior, new ArrayList<>(), new ArrayList<>(), dnfs);
        
        for(int i = 0; i < 100; i++) {
        	times.add(Math.log(3000.5));
            dates.add(LocalDate.now());
        }
        
        calculateAndVisualize(prior, new ArrayList<>(), new ArrayList<>(), dnfs,
        					  prior, times, dates, dnfs);
    }
}