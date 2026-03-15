package org.zane.newpipe;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class JHTMLPane extends JEditorPane {

    private int maxWidth;

    public JHTMLPane() {
        super("text/html", "");
        maxWidth = getMaximumSize().width;
        setOpaque(false);
        setEditable(false);
        setFont(UIManager.getFont("Label.font"));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int PreferredMaxWidth = Math.min(size.width, maxWidth);
        // if (
        //     SwingUtilities.getAncestorOfClass(
        //             JScrollPane.class,
        //             this
        //         ) instanceof
        //         JScrollPane scrollPane
        // ) {
        //     JViewport viewport = scrollPane.getViewport();
        //     PreferredMaxWidth = Math.min(
        //         PreferredMaxWidth,
        //         viewport.getWidth()
        //     );
        // }
        return new Dimension(PreferredMaxWidth, size.height);
    }

    @Override
    public void setMaximumSize(Dimension maximumSize) {
        super.setMaximumSize(maximumSize);
        maxWidth = maximumSize.width;
    }
}
