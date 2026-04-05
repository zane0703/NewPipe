package org.zane.newpipe.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NavigateOption;
import org.zane.newpipe.util.CommonUtil;

public class ChannelInfoPanel extends JPanel {

    private JImage channelAvatar;
    private JLabel channelNameLabel;
    private JLabel channelSubCountLabel;
    private String channelURL;

    public ChannelInfoPanel() {
        this(null);
    }

    public ChannelInfoPanel(MainViewPort mainViewPort) {
        this("", null, null, 0, mainViewPort);
    }

    public ChannelInfoPanel(
        String channelName,
        String channelAvatarURL,
        long channelSubcraberCount
    ) {
        this(channelName, channelAvatarURL, null, channelSubcraberCount, null);
    }

    public ChannelInfoPanel(
        String channelName,
        String channelAvatarURL,
        String channelURL,
        long channelSubcraberCount,
        MainViewPort mainViewPort
    ) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.channelURL = channelURL;
        JPanel uploaderSubInfo = new JPanel(new GridLayout(2, 1));
        channelAvatar = new JImage();
        channelNameLabel = new JLabel(channelName, SwingConstants.LEFT);
        channelSubCountLabel = new JLabel(
            CommonUtil.numberToStringUnit(channelSubcraberCount) +
                " subscribers",
            SwingConstants.LEFT
        );

        if (mainViewPort != null) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.addMouseListener(new PanelClickListener(mainViewPort));
        }

        uploaderSubInfo.setOpaque(false);

        channelAvatar.setMaximumSize(new Dimension(50, 50));
        if (channelAvatarURL != null) {
            setChanelAvatar(channelAvatarURL);
        }

        Font currentFont = channelNameLabel.getFont();
        channelNameLabel.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );

        channelSubCountLabel.setForeground(Color.LIGHT_GRAY);

        this.add(channelAvatar);
        uploaderSubInfo.add(channelNameLabel);
        uploaderSubInfo.add(channelSubCountLabel);
        this.add(uploaderSubInfo);
    }

    public void setChanelAvatar(String channelAvatarURL) {
        try {
            BufferedImage image = ImageIO.read(
                new URI(channelAvatarURL).toURL()
            );
            SwingUtilities.invokeLater(() -> {
                channelAvatar.setImage(image);
            });
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setInfo(String channelName, long channelSubcraberCount) {
        setInfo(channelName, channelSubcraberCount, null);
    }

    public void setInfo(
        String channelName,
        long channelSubcraberCount,
        String channelURL
    ) {
        SwingUtilities.invokeLater(() -> {
            channelNameLabel.setText(channelName);
            channelSubCountLabel.setText(
                CommonUtil.numberToStringUnit(channelSubcraberCount) +
                    " subscribers"
            );
        });
        this.channelURL = channelURL;
    }

    private class PanelClickListener implements MouseListener {

        private MainViewPort mainViewPort;

        public PanelClickListener(MainViewPort mainViewPort) {
            this.mainViewPort = mainViewPort;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            mainViewPort.navigate(
                new NavigateOption(MainViewPort.Page.CHANNEL, channelURL)
            );
        }

        // Other MouseListener methods (must be implemented, even if empty)
        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {
            e.getComponent().setBackground(Color.DARK_GRAY);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            e.getComponent().setBackground(Color.BLACK);
        }
    }
}
