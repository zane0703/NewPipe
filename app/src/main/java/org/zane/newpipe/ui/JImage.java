package org.zane.newpipe.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class JImage extends JPanel {

    private BufferedImage image;
    private Dimension maxSize;
    private Container parent;

    public JImage(Container parent) {
        this.parent = parent;
        this();
    }

    public JImage() {
        maxSize = getMaximumSize();
        if (maxSize == null) {
            maxSize = new Dimension(0, 0);
        }
    }

    public JImage(BufferedImage image, Container parent) {
        this.parent = parent;
        this(image);
    }

    public JImage(BufferedImage image) {
        //this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.image = image;
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
        Window topLevelWindow = SwingUtilities.getWindowAncestor(this);

        // Calculate width/height based on current parent size or max bounds
        if (parent == null) {
            parent = getParent();
        }
        int parentW;
        int parentH;
        if (parent == null) {
            parentH = Integer.MAX_VALUE;
            parentW = Integer.MAX_VALUE;
        } else {
            parentH = parent.getHeight();
            parentW = parent.getWidth();
        }
        int targetW = Math.min(parentW, maxW);
        if (topLevelWindow != null) {
            targetW = Math.min(targetW, topLevelWindow.getWidth());
        }
        int targetH = (int) (targetW / aspectRatio);

        if (targetH > Math.min(parentH, maxH)) {
            targetH = Math.min(parentH, maxH);
            targetW = (int) (targetH * aspectRatio);
        }
        return new Dimension(targetW, targetH);
    }

    // @Override
    // public Rectangle getBounds() {
    //     Dimension size = getPreferredSize();
    //     System.out.println(size);
    //     return new Rectangle(0, 0, size.width, size.height);
    // }
}
