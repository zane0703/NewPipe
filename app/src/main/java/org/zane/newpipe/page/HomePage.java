package org.zane.newpipe.page;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.zane.newpipe.ui.ItemListPanel;
import org.zane.newpipe.util.CommonUtil;

import java.awt.Component;

public class HomePage extends JTabbedPane {

    private final MainViewPort mainViewPort;

    public HomePage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        Thread.startVirtualThread(this::fetchPage);

    }

    private void fetchPage() {
        try {
            KioskList kioskList = ServiceList.YouTube.getKioskList();
            for (String kioskId : kioskList.getAvailableKiosks()) {
                KioskExtractor ke = kioskList.getExtractorById(kioskId, null);
                ke.fetchPage();
                ItemListPanel itemListPanel = new ItemListPanel<InfoItem>(mainViewPort, ke);
                try {

                    String tabName = ke.getName();
                    SwingUtilities.invokeLater(() -> {
                        this.addTab(tabName, itemListPanel);
                    });
                } catch (ParsingException parE) {
                    parE.printStackTrace();
                }
            }
            // setListExtractor(ke);
        } catch (ExtractionException | IOException | NullPointerException e) {
            e.printStackTrace();
            if (CommonUtil.retryPrompt(mainViewPort, "video")) {
                SwingUtilities.invokeLater(() -> {
                    this.removeAll();
                });
                fetchPage();
            }
        }
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Component c = getSelectedComponent();
        if (c != null) {
            Dimension d2 = c.getPreferredSize();
            return new Dimension(Math.min(d.width, mainViewPort.getWidth()), d2.height + 40);
        }
        return d;
    }
}
