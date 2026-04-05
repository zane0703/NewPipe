package org.zane.newpipe.page;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.zane.newpipe.App;
import org.zane.newpipe.page.MainViewPort.NavigateOption;
import org.zane.newpipe.ui.ChannelInfoPanel;
import org.zane.newpipe.ui.CommentPanel;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.JHTMLPane;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;
import org.zane.newpipe.util.VideoUtil;
import org.zane.newpipe.util.VideoUtil.AudioComboBoxRenderer;
import org.zane.newpipe.util.VideoUtil.SubTitleComboBoxRenderer;
import org.zane.newpipe.util.VideoUtil.VideoComboBoxRenderer;
import org.zane.newpipe.util.WrapLayout;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.VideoApi;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenApi;
import uk.co.caprica.vlcj.player.embedded.fullscreen.adaptive.AdaptiveFullScreenStrategy;

public class VideoPage extends JPanel {

    private JPanel videoContol;
    private JComboBox videoComboBox;
    private DefaultComboBoxModel<VideoStream> videoModel;
    private JComboBox audioComboBox;
    private DefaultComboBoxModel<AudioStream> audioModel;
    private JComboBox subtitleComboBox;
    private DefaultComboBoxModel<SubtitlesStream> subtitleModel;
    private AudioStream currentAudioStream;
    private VideoStream currentVideoStream;
    private SubtitlesStream currentSubtitlesStream;
    private boolean isEnableComboxEvent;
    private JSlider playbackSlider;
    private boolean isPositionChanged = false;
    private JLabel currentTimestampLabel;
    private JLabel videoLenghtLabel;
    private JLabel videoTitle;
    private long currentTimestamp = 0;
    private double videoLengthDiff;
    private JButton playButton;
    private EmbeddedMediaPlayer mediaPlayer;
    private JLabel publishDateLabel;
    private JEditorPane videoDescriptionText;
    private CommentPanel videoCommentPanel;
    private JLabel viewCountLabel;
    private JLabel likeCountLabel;
    private NumberFormat numberFormat;
    private int currentSpeed = 100;
    private DecimalFormat df = new DecimalFormat("0.##");
    private BigDecimal div = new BigDecimal(100);
    private JPanel relatedStreamsPanel;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern(
        "dd MMMM yyyy HH:mm:ss"
    );
    private ChannelInfoPanel uploaderInfo;
    private String videoId;
    private MainViewPort mainViewPort;
    private JLabel videoCategoryLabel;
    private JLabel licenseLabel;
    private JLabel privacyLabel;
    private JPanel tagPanel;
    private StreamExtractor streamExtractor;
    private boolean isLive;
    private int oldWidth;
    private JButton downloadBtn;
    private JTabbedPane viewport;
    private int currentSpeedStep = 25;
    private boolean isAutoPlay;
    private JButton speedBtn;
    private JButton fullScreenBtn;
    private JPanel videoInfo;
    private JPanel videoMenuBtnPanel;
    private JPanel videoTitlePanel;
    private Clipboard clipboard =
        Toolkit.getDefaultToolkit().getSystemClipboard();
    private TrayIcon trayIcon;

    public VideoPage(MainViewPort mainViewPort, boolean isAutoPlay) {
        this.mainViewPort = mainViewPort;
        this.isAutoPlay = isAutoPlay;
        this.trayIcon = mainViewPort.getApp().getTrayIcon();

        numberFormat = NumberFormat.getInstance();

        EmbeddedMediaPlayerComponent mediaPlayerComponent =
            new EmbeddedMediaPlayerComponent(
                new MediaPlayerFactory(
                    VideoUtil.nativeDiscovery,
                    "--avcodec-hw=auto"
                ),
                null,
                new AdaptiveFullScreenStrategy(mainViewPort.getApp()),
                null,
                null
            ) {
                private int screenHeight =
                    Toolkit.getDefaultToolkit().getScreenSize().height - 20;

                @Override
                public Dimension getPreferredSize() {
                    Dimension videoDimension = this.mediaPlayer()
                        .video()
                        .videoDimension();
                    int width = VideoPage.this.getWidth();
                    int height;
                    if (videoDimension == null) {
                        height = 0;
                    } else {
                        height = (int) (width *
                            ((double) videoDimension.height /
                                (double) videoDimension.width));
                        height = Math.min(height, screenHeight);
                    }
                    return new Dimension(width, height);
                }
            };
        videoLenghtLabel = new JLabel("-:--:--");
        currentTimestampLabel = new JLabel("00:00");
        videoTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        videoTitle = new JLabel("", SwingConstants.LEFT);
        JPanel playbackSliderRow = new JPanel(new BorderLayout());
        playbackSlider = new JSlider(
            SwingConstants.HORIZONTAL,
            0,
            Integer.MAX_VALUE,
            0
        );
        videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playButton = new JButton(IconRes.PLAY_ARROW_ICON);
        videoModel = new DefaultComboBoxModel<VideoStream>();
        audioModel = new DefaultComboBoxModel<AudioStream>();
        subtitleModel = new DefaultComboBoxModel<SubtitlesStream>();
        videoComboBox = new JComboBox<VideoStream>(videoModel);
        audioComboBox = new JComboBox<AudioStream>(audioModel);
        subtitleComboBox = new JComboBox<SubtitlesStream>(subtitleModel);
        speedBtn = new JButton("1x");
        fullScreenBtn = new JButton(IconRes.FULLSCREEN_ICON);
        videoInfo = new JPanel(new GridLayout(1, 2));
        uploaderInfo = new ChannelInfoPanel(mainViewPort);
        viewCountLabel = new JLabel("", SwingConstants.RIGHT);
        likeCountLabel = new JLabel(
            "",
            IconRes.THUMP_UP_ICON,
            SwingConstants.RIGHT
        );
        JPanel likeAndViewPanelPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );
        JPanel likeAndViewPanel = new JPanel();

        videoMenuBtnPanel = new JPanel(new GridLayout(1, 4));
        JButton openVLCBtn = new JButton(
            "Open in VLC Media Player",
            IconRes.VLC_ICON
        );
        JButton copyUrlBtn = new JButton("Copy URL", IconRes.COPY_ICON);
        JButton openBrowserBtn = new JButton(
            "Open in browser",
            IconRes.LANGUAGE_ICON
        );
        downloadBtn = new JButton("Download", IconRes.DOWNLOAD_ICON);
        viewport = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Component c = getSelectedComponent();
                if (c != null) {
                    Dimension d2 = c.getPreferredSize();
                    return new Dimension(d.width, d2.height + 40);
                }
                return d;
            }
        };
        JPanel videoDescriptionPanel = new JPanel();
        JPanel publishDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel videoDescriptionTextPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );
        publishDateLabel = new JLabel();
        videoDescriptionText = new JHTMLPane();
        JPanel descriptionMetadata = new JPanel(new GridLayout(3, 3, 10, 10));
        licenseLabel = new JLabel("", SwingConstants.LEFT);
        privacyLabel = new JLabel("", SwingConstants.LEFT);
        videoCategoryLabel = new JLabel("", SwingConstants.LEFT);
        tagPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));

        videoCommentPanel = new CommentPanel(
            mainViewPort,
            this::onHyperlinkPressed
        );
        relatedStreamsPanel = new JPanel();
        mediaPlayer = mediaPlayerComponent.mediaPlayer();

        //set layout
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        likeAndViewPanel.setLayout(
            new BoxLayout(likeAndViewPanel, BoxLayout.Y_AXIS)
        );
        relatedStreamsPanel.setLayout(
            new BoxLayout(relatedStreamsPanel, BoxLayout.Y_AXIS)
        );
        videoCommentPanel.setLayout(
            new BoxLayout(videoCommentPanel, BoxLayout.Y_AXIS)
        );
        videoDescriptionPanel.setLayout(
            new BoxLayout(videoDescriptionPanel, BoxLayout.Y_AXIS)
        );

        videoCommentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension maxSize = new Dimension(
            getPreferredSize().width,
            Integer.MAX_VALUE
        );
        oldWidth = maxSize.width;
        videoDescriptionText.setMaximumSize(maxSize);
        tagPanel.setMaximumSize(maxSize);
        //mediaPlayerComponent.setPreferredSize(new Dimension(500, 500));

        //set cursor
        copyUrlBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openVLCBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openBrowserBtn.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        downloadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fullScreenBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        speedBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playbackSlider.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        viewport.addMouseMotionListener(CommonUtil.TABBED_CURSOR);

        //combox Renderer
        videoComboBox.setRenderer(new VideoComboBoxRenderer());
        audioComboBox.setRenderer(new AudioComboBoxRenderer());
        subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());

        //set too; tip
        playButton.setToolTipText("Play");
        fullScreenBtn.setToolTipText("Enter FullScreen");
        speedBtn.setToolTipText("Set Playback Speed");

        //set colour
        playbackSlider.setForeground(IconRes.YOUTUBE_COLOUR);

        //btn icon Position
        copyUrlBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        openBrowserBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        openVLCBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        downloadBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        copyUrlBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        openBrowserBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        openVLCBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        downloadBtn.setHorizontalTextPosition(SwingConstants.CENTER);

        //set font
        Font boldFont = videoTitle.getFont();
        boldFont = boldFont.deriveFont(Font.BOLD, boldFont.getSize());
        videoTitle.setFont(boldFont);
        //set event
        this.addComponentListener(componentAdapter);
        playbackSlider.addChangeListener(this::onPlaybackSliderChanged);
        playButton.addActionListener(this::onPlayBtnPressed);
        videoComboBox.addItemListener(this::onVideoComboBoxChanged);
        audioComboBox.addItemListener(this::onAudioComboBoxChanged);
        subtitleComboBox.addItemListener(this::onSubtitleComboBoxChanged);
        speedBtn.addActionListener(this::onSpeedBtnPressed);
        copyUrlBtn.addActionListener(this::onCopyBtnPressed);
        openBrowserBtn.addActionListener(this::onOpenBrowserBtnPressed);
        openVLCBtn.addActionListener(this::onOpenVLCBtnPressed);
        downloadBtn.addActionListener(this::onDownloadBtnPressed);
        fullScreenBtn.addActionListener(this::onFullScreenBtnPressed);
        videoDescriptionText.addHyperlinkListener(this::onHyperlinkPressed);
        viewport.addChangeListener(e -> viewport.revalidate());
        mediaPlayer
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());

        //add component
        this.add(mediaPlayerComponent);

        playbackSliderRow.add(currentTimestampLabel, BorderLayout.LINE_START);
        playbackSliderRow.add(playbackSlider, BorderLayout.CENTER);
        playbackSliderRow.add(videoLenghtLabel, BorderLayout.LINE_END);
        this.add(playbackSliderRow);

        videoContol.add(playButton);
        videoContol.add(new JLabel("Video:"));
        videoContol.add(videoComboBox);
        videoContol.add(new JLabel("Audio:"));
        videoContol.add(audioComboBox);
        videoContol.add(new JLabel("Subtitle:"));
        videoContol.add(subtitleComboBox);
        videoContol.add(speedBtn);
        videoContol.add(fullScreenBtn);
        this.add(videoContol);

        videoTitlePanel.add(videoTitle);
        this.add(videoTitlePanel);

        videoInfo.add(uploaderInfo);

        likeAndViewPanel.add(viewCountLabel);

        likeAndViewPanel.add(likeCountLabel);
        likeAndViewPanelPanel.add(likeAndViewPanel);
        videoInfo.add(likeAndViewPanelPanel);
        this.add(videoInfo);

        videoMenuBtnPanel.add(copyUrlBtn);
        videoMenuBtnPanel.add(openBrowserBtn);
        videoMenuBtnPanel.add(openVLCBtn);
        videoMenuBtnPanel.add(downloadBtn);
        this.add(videoMenuBtnPanel);

        publishDatePanel.add(publishDateLabel);
        videoDescriptionPanel.add(publishDatePanel);

        videoDescriptionTextPanel.add(videoDescriptionText);
        videoDescriptionPanel.add(videoDescriptionTextPanel);

        JLabel tmpLabel = new JLabel("Category:", SwingConstants.RIGHT);
        tmpLabel.setFont(boldFont);
        descriptionMetadata.add(tmpLabel);
        descriptionMetadata.add(videoCategoryLabel);

        tmpLabel = new JLabel("License:", SwingConstants.RIGHT);
        tmpLabel.setFont(boldFont);
        descriptionMetadata.add(tmpLabel);
        descriptionMetadata.add(licenseLabel);

        tmpLabel = new JLabel("Privacy:", SwingConstants.RIGHT);
        tmpLabel.setFont(boldFont);
        descriptionMetadata.add(tmpLabel);
        descriptionMetadata.add(privacyLabel);

        videoDescriptionPanel.add(descriptionMetadata);
        videoDescriptionPanel.add(tagPanel);

        viewport.addTab(null, IconRes.COMMENT_ICON, videoCommentPanel);
        viewport.addTab(null, IconRes.ART_TRACK_ICON, relatedStreamsPanel);
        viewport.addTab(null, IconRes.DESCRIPTION_ICON, videoDescriptionPanel);

        this.add(viewport);

        this.setFocusable(true);
        //keymap
        InputMap inputMap = this.getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "forward");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "forward");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "backward");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "backward");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "pause");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pause");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fullscreen");
        inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "fullscreen_exit"
        );
        ActionMap actionMap = this.getActionMap();
        actionMap.put(
            "forward",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mediaPlayer.controls().skipTime(10000);
                }
            }
        );
        actionMap.put(
            "backward",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mediaPlayer.controls().skipTime(-10000);
                }
            }
        );
        actionMap.put(
            "pause",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onPlayBtnPressed(null);
                }
            }
        );
        actionMap.put(
            "fullscreen_exit",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    FullScreenApi fullScreenApi = mediaPlayer.fullScreen();
                    if (fullScreenApi.isFullScreen()) {
                        onFullScreenBtnPressed(null);
                    }
                }
            }
        );
        actionMap.put(
            "fullscreen",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onFullScreenBtnPressed(null);
                }
            }
        );
    }

    private ComponentAdapter componentAdapter = new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {
            // Code to execute when the JPanel is hidden
            //
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            Dimension maxSize = new Dimension(
                getPreferredSize().width,
                Integer.MAX_VALUE
            );
            videoDescriptionText.setMaximumSize(maxSize);
            tagPanel.setMaximumSize(maxSize);
            if (oldWidth > maxSize.width) {
                videoDescriptionText.updateUI();
            }
            oldWidth = maxSize.width;
        }
    };

    private void onHyperlinkPressed(HyperlinkEvent e) {
        try {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL url = e.getURL();
                switch (url.getHost().toLowerCase()) {
                    case "www.youtube.com":
                    case "youtube.com":
                    case "m.youtube.com":
                        String[] paths = url.getPath().split("/");
                        switch (paths.length) {
                            case 2:
                                if (paths[1].equalsIgnoreCase("watch")) {
                                    Map<String, String> query =
                                        CommonUtil.getQueryMap(url.getQuery());
                                    String v = query.get("v");
                                    if (v != null) {
                                        if (v.equals(videoId)) {
                                            String t = query.get("t");
                                            long newTime = Long.parseLong(t);
                                            mediaPlayer
                                                .controls()
                                                .setTime(newTime * 1000);
                                        } else {
                                            mainViewPort.navigate(
                                                new NavigateOption(
                                                    MainViewPort.Page.VIDEO,
                                                    url.toString()
                                                )
                                            );
                                        }
                                    } else if (paths[1].charAt(0) == '@') {
                                        mainViewPort.navigate(
                                            new NavigateOption(
                                                MainViewPort.Page.CHANNEL,
                                                url.toString()
                                            )
                                        );
                                    }
                                }
                                break;
                            case 3:
                                switch (paths[1].toLowerCase()) {
                                    case "hashtag":
                                        mainViewPort.navigate(
                                            new NavigateOption(
                                                MainViewPort.Page.SEARCH,
                                                "#" + paths[2]
                                            )
                                        );
                                        break;
                                    case "channel":
                                        mainViewPort.navigate(
                                            new NavigateOption(
                                                MainViewPort.Page.CHANNEL,
                                                url.toString()
                                            )
                                        );
                                        break;
                                }

                                break;
                        }
                        break;
                    case "youtu.be":
                        mainViewPort.navigate(
                            new NavigateOption(
                                MainViewPort.Page.VIDEO,
                                url.toString()
                            )
                        );
                        break;
                    default:
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(url.toURI());
                        }
                }
            }
        } catch (ParseException | IOException | URISyntaxException pe) {
            pe.printStackTrace();
        }
    }

    private void onPlaybackSliderChanged(ChangeEvent e) {
        if (playbackSlider.getValueIsAdjusting() || isPositionChanged) {
            return;
        }
        mediaPlayer
            .controls()
            .setTime((long) (playbackSlider.getValue() * videoLengthDiff));
    }

    private void onSpeedBtnPressed(ActionEvent e) {
        SpeedSelectorPanel s = new SpeedSelectorPanel();
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e2 -> s.reset());
        Object[] options = new Object[] { resetBtn, "Ok", "Cancel" };
        if (
            JOptionPane.showOptionDialog(
                mainViewPort,
                s,
                "Playback speed option",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]
            ) ==
            1
        ) {
            currentSpeed = s.getSpeed();
            BigDecimal speedDec = new BigDecimal(currentSpeed).divide(div);
            speedBtn.setText(df.format(speedDec) + "x");
            mediaPlayer.controls().setRate(speedDec.floatValue());
        }
    }

    private void onFullScreenBtnPressed(ActionEvent e) {
        FullScreenApi fullScreenApi = mediaPlayer.fullScreen();
        fullScreenApi.set(!fullScreenApi.isFullScreen());
        App app = mainViewPort.getApp();
        if (fullScreenApi.isFullScreen()) {
            fullScreenBtn.setIcon(IconRes.FULLSCREEN_EXIT_ICON);
            fullScreenBtn.setToolTipText("Exit FullScreen");
            viewport.setVisible(false);
            videoInfo.setVisible(false);
            videoMenuBtnPanel.setVisible(false);
            videoTitlePanel.setVisible(false);
            app.setSearchBarVisible(false);
        } else {
            fullScreenBtn.setIcon(IconRes.FULLSCREEN_ICON);
            fullScreenBtn.setToolTipText("Enter FullScreen");
            viewport.setVisible(true);
            videoInfo.setVisible(true);
            videoMenuBtnPanel.setVisible(true);
            videoTitlePanel.setVisible(true);
            app.setSearchBarVisible(true);
        }
    }

    private void onPlayBtnPressed(ActionEvent e) {
        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().pause();
        } else {
            mediaPlayer.controls().play();
        }
    }

    private void onVideoComboBoxChanged(ItemEvent e) {
        if (isEnableComboxEvent) {
            boolean isPlaying;
            long currentTime;
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    if (e.getItem() instanceof VideoStream videoStream) {
                        isPlaying = mediaPlayer.status().isPlaying();
                        currentTime = mediaPlayer.status().time();
                        playVideo(
                            videoStream,
                            currentAudioStream,
                            currentSubtitlesStream,
                            currentTime
                        );

                        currentVideoStream = videoStream;
                    } else {
                        return;
                    }
                    break;
                case ItemEvent.DESELECTED:
                    if (currentAudioStream == null) {
                        if (trayIcon == null) {
                            JOptionPane.showMessageDialog(
                                mainViewPort,
                                "Must have either video and Audio",
                                "Changing Video",
                                JOptionPane.ERROR
                            );
                        } else {
                            trayIcon.displayMessage(
                                "Must have either video and Audio",
                                "",
                                MessageType.ERROR
                            );
                        }
                        isEnableComboxEvent = false;
                        videoModel.setSelectedItem(currentVideoStream);
                        isEnableComboxEvent = true;
                        return;
                    }
                    isPlaying = mediaPlayer.status().isPlaying();
                    currentTime = mediaPlayer.status().time();
                    playVideo(
                        null,
                        currentAudioStream,
                        currentSubtitlesStream,
                        currentTime
                    );
                    currentVideoStream = null;
                    break;
                default:
                    return;
            }
            if (isPlaying) {
                mediaPlayer.controls().play();
            }
        }
    }

    private void onAudioComboBoxChanged(ItemEvent e) {
        if (isEnableComboxEvent) {
            boolean isPlaying;
            long currentTime;
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    if (e.getItem() instanceof AudioStream audioStream) {
                        isPlaying = mediaPlayer.status().isPlaying();
                        currentTime = mediaPlayer.status().time();
                        playVideo(
                            currentVideoStream,
                            audioStream,
                            currentSubtitlesStream,
                            currentTime
                        );

                        currentAudioStream = audioStream;
                    } else {
                        return;
                    }
                    break;
                case ItemEvent.DESELECTED:
                    if (currentVideoStream == null) {
                        if (trayIcon == null) {
                            JOptionPane.showMessageDialog(
                                mainViewPort,
                                "Must have either video and Audio",
                                "Changing Audio",
                                JOptionPane.ERROR
                            );
                        } else {
                            trayIcon.displayMessage(
                                "Must have either video and Audio",
                                "",
                                MessageType.ERROR
                            );
                        }
                        isEnableComboxEvent = false;
                        audioModel.setSelectedItem(currentAudioStream);
                        isEnableComboxEvent = true;
                        return;
                    }
                    isPlaying = mediaPlayer.status().isPlaying();
                    currentTime = mediaPlayer.status().time();
                    playVideo(
                        currentVideoStream,
                        null,
                        currentSubtitlesStream,
                        currentTime
                    );
                    currentAudioStream = null;
                    break;
                default:
                    return;
            }
            if (isPlaying) {
                mediaPlayer.controls().play();
            }
        }
    }

    private void onSubtitleComboBoxChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent) {
            boolean isPlaying = mediaPlayer.status().isPlaying();
            final long currentTime = mediaPlayer.status().time();
            if (e.getItem() instanceof SubtitlesStream subtitlesStream) {
                playVideo(
                    currentVideoStream,
                    currentAudioStream,
                    subtitlesStream,
                    currentTime
                );

                currentSubtitlesStream = subtitlesStream;
            } else {
                playVideo(
                    currentVideoStream,
                    currentAudioStream,
                    null,
                    currentTime
                );
                currentSubtitlesStream = null;
            }
            if (isPlaying) {
                mediaPlayer.controls().play();
            }
        }
    }

    private void onCopyBtnPressed(ActionEvent e) {
        try {
            clipboard.setContents(
                new StringSelection(streamExtractor.getUrl()),
                null
            );
        } catch (ParsingException pe) {
            pe.printStackTrace();
        }
    }

    private void onOpenBrowserBtnPressed(ActionEvent e) {
        if (Desktop.isDesktopSupported()) mediaPlayer.controls().pause();
        try {
            Desktop.getDesktop().browse(URI.create(streamExtractor.getUrl()));
        } catch (IOException | ParsingException ioe) {
            ioe.printStackTrace();
        }
    }

    private void onOpenVLCBtnPressed(ActionEvent e) {
        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().pause();
        }
        new Thread(() ->
            VideoUtil.openVLC(streamExtractor, mainViewPort)
        ).start();
    }

    private void onDownloadBtnPressed(ActionEvent e) {
        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().pause();
        }
        new Thread(() ->
            VideoUtil.downloadVideo(streamExtractor, false, this.trayIcon)
        ).start();
    }

    public void showVideo(String videoUrl) {
        new Thread(() -> {
            try {
                long[] startTime = { 0 };
                streamExtractor = ServiceList.YouTube.getStreamExtractor(
                    videoUrl
                );
                streamExtractor.fetchPage();

                try {
                    URI videoURI = new URI(videoUrl);
                    String videoQ = videoURI.getQuery();

                    if (videoQ != null) {
                        Map<String, String> videoQMap = CommonUtil.getQueryMap(
                            videoQ
                        );
                        String t = videoQMap.get("t");
                        if (t != null) {
                            startTime[0] = Long.parseLong(t) * 1000l;
                        }
                    }
                } catch (
                    URISyntaxException
                    | ParseException
                    | NumberFormatException err
                ) {}
                videoId = streamExtractor.getId();
                videoTitle.setText(streamExtractor.getName());
                List<org.schabi.newpipe.extractor.Image> avatars =
                    streamExtractor.getUploaderAvatars();
                if (!avatars.isEmpty()) {
                    uploaderInfo.setChanelAvatar(avatars.get(0).getUrl());
                }
                uploaderInfo.setInfo(
                    streamExtractor.getUploaderName(),
                    streamExtractor.getUploaderSubscriberCount(),
                    streamExtractor.getUploaderUrl()
                );

                videoModel.removeAllElements();
                audioModel.removeAllElements();
                subtitleModel.removeAllElements();
                isLive =
                    streamExtractor.getStreamType() == StreamType.LIVE_STREAM;

                if (isLive) {
                    playbackSlider.setEnabled(false);
                    videoComboBox.setEnabled(false);
                    audioComboBox.setEnabled(false);
                    subtitleComboBox.setEnabled(false);
                    isPositionChanged = true;
                    playbackSlider.setValue(0);
                    isPositionChanged = false;
                    downloadBtn.setEnabled(false);
                    mediaPlayer.media().prepare(streamExtractor.getHlsUrl());
                } else {
                    List<VideoStream> videoStreams =
                        streamExtractor.getVideoOnlyStreams();
                    List<AudioStream> audioStreams =
                        streamExtractor.getAudioStreams();
                    System.out.println(videoStreams.getClass());
                    audioStreams.sort(VideoUtil.SORT_AUDIO);
                    List<SubtitlesStream> subtitlesStreams =
                        streamExtractor.getSubtitlesDefault();
                    currentVideoStream = videoStreams.get(0);
                    currentAudioStream = null;
                    downloadBtn.setEnabled(true);
                    currentAudioStream = audioStreams.get(0);

                    currentSubtitlesStream = null;
                    videoModel.addElement(null);
                    audioModel.addElement(null);
                    videoModel.addAll(videoStreams);
                    audioModel.addAll(audioStreams);
                    videoComboBox.setEnabled(true);
                    audioComboBox.setEnabled(true);
                    videoComboBox.setEnabled(true);
                    if (audioStreams.isEmpty()) {
                        subtitleComboBox.setEnabled(false);
                    } else {
                        subtitlesStreams.addFirst(null);
                        subtitleModel.addAll(subtitlesStreams);
                        subtitleComboBox.setEnabled(true);
                    }

                    isEnableComboxEvent = false;
                    videoModel.setSelectedItem(currentVideoStream);
                    audioModel.setSelectedItem(currentAudioStream);
                    subtitleModel.setSelectedItem(null);
                    isEnableComboxEvent = true;
                }

                String timeString = CommonUtil.getTimeString(
                    streamExtractor.getLength()
                );
                String viewCountString =
                    numberFormat.format(streamExtractor.getViewCount()) +
                    " views";
                String likeCountString = CommonUtil.numberToStringUnit(
                    streamExtractor.getLikeCount()
                );
                String UploadDateString =
                    "Published on " +
                    dtf.format(
                        streamExtractor.getUploadDate().getLocalDateTime()
                    );

                String videoDescriptionString = streamExtractor
                    .getDescription()
                    .getContent();
                String videoCategoryString = streamExtractor.getCategory();
                String privacyString = streamExtractor.getPrivacy().name();
                String licenceString = streamExtractor.getLicence();
                List<String> tagsString = streamExtractor.getTags();
                SwingUtilities.invokeLater(() -> {
                    videoLenghtLabel.setText(timeString);
                    publishDateLabel.setText(UploadDateString);
                    try {
                        videoDescriptionText.setText(videoDescriptionString);
                        relatedStreamsPanel.removeAll();
                        for (InfoItem item : streamExtractor
                            .getRelatedItems()
                            .getItems()) {
                            relatedStreamsPanel.add(
                                new SearchItemPanel(mainViewPort, item)
                            );
                        }
                        tagPanel.removeAll();
                        for (String tagString : tagsString) {
                            JButton tagBtn = new JButton(tagString);
                            tagBtn.setCursor(
                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            );
                            tagBtn.addActionListener(e ->
                                mainViewPort.navigate(
                                    new NavigateOption(
                                        MainViewPort.Page.SEARCH,
                                        "#" + tagString
                                    )
                                )
                            );
                            tagPanel.add(tagBtn);
                        }
                    } catch (
                        ExtractionException
                        | IOException
                        | URISyntaxException
                        | NullPointerException
                        | IndexOutOfBoundsException e
                    ) {
                        e.printStackTrace();
                        if (CommonUtil.retryPrompt(mainViewPort, "video")) {
                            showVideo(videoUrl);
                        }
                    }

                    viewCountLabel.setText(viewCountString);
                    likeCountLabel.setText(likeCountString);
                    videoCategoryLabel.setText(videoCategoryString);
                    privacyLabel.setText(privacyString);
                    licenseLabel.setText(licenceString);
                    if (!isLive) {
                        playVideo(
                            currentVideoStream,
                            currentAudioStream,
                            null,
                            startTime[0]
                        );
                    }
                    if (isAutoPlay) {
                        mediaPlayer.controls().play();
                    }
                });

                videoCommentPanel.fetchComment(videoUrl);
            } catch (
                ExtractionException
                | IOException
                | IndexOutOfBoundsException e
            ) {
                e.printStackTrace();
                if (CommonUtil.retryPrompt(mainViewPort, "video")) {
                    showVideo(videoUrl);
                }
            }
        })
            .start();
    }

    public void stop() {
        mediaPlayer.controls().stop();
        mediaPlayer.media().reset();
        videoCommentPanel.clear();
        relatedStreamsPanel.removeAll();
        System.gc();
    }

    public void playVideo(
        VideoStream videoStream,
        AudioStream audioStream,
        SubtitlesStream subtitlesStream,
        long currentTime
    ) {
        this.requestFocus();
        MediaApi mediaApi = mediaPlayer.media();
        String mrl;
        String[] options = new String[] {
            String.format(":start-time=%.3f", currentTime / 1000.0),
        };

        if (videoStream == null) {
            mrl = audioStream.getContent();
        } else {
            mrl = videoStream.getContent();
            if (audioStream != null) {
                options = new String[] {
                    options[0],
                    ":input-slave=" + currentAudioStream.getContent(),
                };
            }
        }
        mediaApi.prepare(mrl, options);
        if (subtitlesStream != null) {
            mediaPlayer
                .subpictures()
                .setSubTitleUri(subtitlesStream.getContent());
        }
    }

    private class MyMediaPlayerEventListener extends MediaPlayerEventAdapter {

        @Override
        public void buffering(MediaPlayer mediaPlayer, float newCache) {}

        @Override
        public void playing(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PAUSE_ICON);
            playButton.setToolTipText("Pause");
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
            playButton.setToolTipText("Play");
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
            playButton.setToolTipText("Play");
        }

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
            playButton.setToolTipText("Play");
        }

        @Override
        public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
            if (isLive) {
                return;
            }
            long newTimeSec = newTime / 1000;
            if (newTimeSec != currentTimestamp) {
                String newTimeString = CommonUtil.getTimeString(newTime / 1000);
                SwingUtilities.invokeLater(() ->
                    currentTimestampLabel.setText(newTimeString)
                );
            }

            int newTimeSlid = (int) (newTime / videoLengthDiff);
            SwingUtilities.invokeLater(() -> {
                if (playbackSlider.getValueIsAdjusting()) {
                    return;
                }
                isPositionChanged = true;
                playbackSlider.setValue(newTimeSlid);
                isPositionChanged = false;
            });
        }

        @Override
        public void mediaPlayerReady(MediaPlayer mediaPlayer) {
            if (isLive) {
                return;
            }
            videoLengthDiff =
                ((double) mediaPlayer.status().length()) /
                ((double) Integer.MAX_VALUE);
        }
    }

    private class SpeedSelectorPanel extends JPanel {

        private int speed;
        private JSlider slider;
        private JToggleButton step1;
        private JToggleButton step5;
        private JToggleButton step10;
        private JToggleButton step25;
        private JToggleButton step100;

        public SpeedSelectorPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JPanel mainSpeedPanel = new JPanel(new BorderLayout());
            speed = currentSpeed;
            JLabel speedLabel = new JLabel(
                df.format(new BigDecimal(currentSpeed).divide(div)) + "x",
                SwingConstants.CENTER
            );
            JPanel speedLabelPanel = new JPanel(new BorderLayout());
            speedLabelPanel.add(new JLabel("0.1x"), BorderLayout.WEST);
            speedLabelPanel.add(speedLabel, BorderLayout.CENTER);
            speedLabelPanel.add(new JLabel("10x"), BorderLayout.EAST);
            JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
            JButton subBtn = new JButton("+" + currentSpeedStep + "%");
            slider = new JSlider(10, 1000, currentSpeed);

            JButton addBtn = new JButton("+" + currentSpeedStep + "%");

            mainSpeedPanel.add(subBtn, BorderLayout.WEST);
            subPanel.add(speedLabelPanel);
            subPanel.add(slider);
            mainSpeedPanel.add(addBtn, BorderLayout.EAST);
            slider.setForeground(IconRes.YOUTUBE_COLOUR);
            slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            subBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            subBtn.addActionListener(e ->
                slider.setValue(speed - currentSpeedStep)
            );
            addBtn.addActionListener(e ->
                slider.setValue(speed + currentSpeedStep)
            );
            slider.addChangeListener(e -> {
                speed = slider.getValue();
                speedLabel.setText(
                    df.format(new BigDecimal(speed).divide(div)) + "x"
                );
            });

            mainSpeedPanel.add(subPanel, BorderLayout.CENTER);
            this.add(mainSpeedPanel);

            JPanel speedStepPanel = new JPanel(new GridLayout(1, 6));
            ButtonGroup stepGroup = new ButtonGroup();
            step1 = new JToggleButton("1%");
            step5 = new JToggleButton("5%");
            step10 = new JToggleButton("10%");
            step25 = new JToggleButton("25%");
            step100 = new JToggleButton("100%");
            step1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            step5.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            step10.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            step25.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            step100.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleStep(currentSpeedStep, true);
            step1.addChangeListener(e -> {
                if (step1.isSelected()) {
                    currentSpeedStep = 1;
                    subBtn.setText("-1%");
                    addBtn.setText("+1%");
                }
            });
            step5.addChangeListener(e -> {
                if (step5.isSelected()) {
                    currentSpeedStep = 5;
                    subBtn.setText("-5%");
                    addBtn.setText("+5%");
                }
            });
            step10.addChangeListener(e -> {
                if (step10.isSelected()) {
                    currentSpeedStep = 10;
                    subBtn.setText("-10%");
                    addBtn.setText("+10%");
                }
            });
            step25.addChangeListener(e -> {
                if (step25.isSelected()) {
                    currentSpeedStep = 25;
                    subBtn.setText("-25%");
                    addBtn.setText("+25%");
                }
            });
            step100.addChangeListener(e -> {
                if (step100.isSelected()) {
                    currentSpeedStep = 100;
                    subBtn.setText("-100%");
                    addBtn.setText("+100%");
                }
            });
            toggleStep(currentSpeedStep, true);
            speedStepPanel.add(new JLabel("Step:"));
            speedStepPanel.add(step1);
            speedStepPanel.add(step5);
            speedStepPanel.add(step10);
            speedStepPanel.add(step25);
            speedStepPanel.add(step100);
            stepGroup.add(step1);
            stepGroup.add(step5);
            stepGroup.add(step10);
            stepGroup.add(step25);
            stepGroup.add(step100);
            this.add(speedStepPanel);
        }

        private void toggleStep(int step, boolean selected) {
            switch (step) {
                case 1:
                    step1.setSelected(selected);
                    break;
                case 5:
                    step5.setSelected(selected);
                    break;
                case 10:
                    step10.setSelected(selected);
                    break;
                case 25:
                    step25.setSelected(selected);
                    break;
                case 100:
                    step100.setSelected(selected);
                    break;
            }
        }

        public void reset() {
            slider.setValue(100);
        }

        public int getSpeed() {
            return speed;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int maxSize = Math.min(size.width, mainViewPort.getWidth());
        return new Dimension(maxSize, size.height);
    }
}
