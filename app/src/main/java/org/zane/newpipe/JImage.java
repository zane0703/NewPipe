package org.zane.newpipe;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class JImage extends JPanel {

    private BufferedImage image;
    private Dimension maxSize;

    public JImage() {
        maxSize = getMaximumSize();
        if (maxSize == null) {
            maxSize = new Dimension(0, 0);
        }
    }

    public JImage(BufferedImage image) {
        //this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.image = image;
        this();
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.repaint();
    }

    @Override
    public void setMaximumSize(Dimension maxSize) {
        this.maxSize = maxSize;
        super.setMaximumSize(maxSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Call the superclass method to ensure proper painting of the panel's background and borders
        super.paintComponent(g);

        // Draw the image at coordinates (0, 0)
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return super.getPreferredSize();
        }
        int maxW = maxSize.width; // Your hard maximum width
        int maxH = maxSize.height; // Your hard maximum height
        double aspectRatio = (double) image.getWidth() / image.getHeight();

        // Calculate width/height based on current parent size or max bounds
        int targetW = Math.min(getParent().getWidth(), maxW);
        int targetH = (int) (targetW / aspectRatio);

        if (targetH > Math.min(getParent().getHeight(), maxH)) {
            targetH = Math.min(getParent().getHeight(), maxH);
            targetW = (int) (targetH * aspectRatio);
        }
        return new Dimension(targetW, targetH);
    }
}
