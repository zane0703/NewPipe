package org.zane.newpipe.page;

import java.awt.*;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
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

    public VideoPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        numberFormat = NumberFormat.getInstance();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        HyperlinkListener hyperlinkListener = e -> {
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
                                            CommonUtil.getQueryMap(
                                                url.getQuery()
                                            );
                                        String v = query.get("v");
                                        if (v != null) {
                                            if (v.equals(videoId)) {
                                                String t = query.get("t");
                                                long newTime = Long.parseLong(
                                                    t
                                                );
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
        };
        EmbeddedMediaPlayerComponent mediaPlayerComponent =
            new EmbeddedMediaPlayerComponent(
                new MediaPlayerFactory(
                    VideoUtil.nativeDiscovery,
                    "--avcodec-hw=auto",
                    "--ffmpeg-hw"
                ),
                null,
                new AdaptiveFullScreenStrategy(App.getInstance()),
                null,
                null
            );
        videoLenghtLabel = new JLabel("-:--:--");
        currentTimestampLabel = new JLabel("00:00");
        JPanel videoTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        videoModel = new DefaultComboBoxModel<>();
        videoComboBox = new JComboBox<VideoStream>(videoModel);
        audioModel = new DefaultComboBoxModel<>();
        audioComboBox = new JComboBox<AudioStream>(audioModel);
        subtitleModel = new DefaultComboBoxModel<SubtitlesStream>();
        subtitleComboBox = new JComboBox<SubtitlesStream>(subtitleModel);
        JButton speedBtm = new JButton("1x");
        JButton fullScreenBtn = new JButton(IconRes.FULLSCREEN_ICON);

        JPanel videoInfo = new JPanel(new GridLayout(1, 2));
        JPanel likeAndViewPanelPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );
        JPanel likeAndViewPanel = new JPanel();

        JPanel videoMenuBtnPanel = new JPanel(new GridLayout(1, 4));
        JButton openVLCBtn = new JButton(
            "Open in VLC Media Player",
            IconRes.VLC_ICON
        );
        JButton copyUrlBtn = new JButton("Copy URL", IconRes.COPY_ICON);
        JButton openBrowserBtn = new JButton(
            "Open in browser",
            IconRes.LANGUAGE_ICON
        );
        JPanel navigationBar = new JPanel(new GridLayout(1, 3));
        JButton relatedStreamsBtn = new JButton(IconRes.ART_TRACK_ICON);
        JButton commentBtn = new JButton(IconRes.COMMENT_ICON);
        JViewport viewport = new JViewport();
        JPanel videoDescriptionPanel = new JPanel();
        JPanel publishDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel videoDescriptionTextPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        JPanel descriptionMatadata = new JPanel(new GridLayout(3, 3, 10, 10));
        JLabel tmpLabel = new JLabel("Category:", SwingConstants.RIGHT);

        videoCommentPanel = new CommentPanel(mainViewPort, hyperlinkListener);

        mediaPlayer = mediaPlayerComponent.mediaPlayer();
        this.addComponentListener(
            new ComponentAdapter() {
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
            }
        );

        mediaPlayerComponent.setPreferredSize(new Dimension(500, 500));
        mediaPlayer
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());
        mediaPlayerComponent.setPreferredSize(new Dimension(500, 500));
        this.add(mediaPlayerComponent);
        // c.gridy = 1;

        playbackSliderRow.add(currentTimestampLabel, BorderLayout.LINE_START);

        playbackSlider.setForeground(IconRes.YOUTUBE_COLOUR);
        playbackSlider.addChangeListener(e -> {
            if (playbackSlider.getValueIsAdjusting() || isPositionChanged) {
                return;
            }
            System.out.println("change timestamp");
            mediaPlayer
                .controls()
                .setTime((long) (playbackSlider.getValue() * videoLengthDiff));
        });
        playbackSliderRow.add(playbackSlider, BorderLayout.CENTER);

        playbackSliderRow.add(videoLenghtLabel, BorderLayout.LINE_END);
        this.add(playbackSliderRow);

        // video Contol

        this.add(videoContol);

        playButton.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            } else {
                mediaPlayer.controls().play();
            }
        });
        videoContol.add(playButton);

        //ComboBox

        videoComboBox.setRenderer(new VideoComboBoxRenderer());
        videoComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof VideoStream videoStream) {
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    playVideo(
                        videoStream,
                        currentAudioStream,
                        currentSubtitlesStream,
                        currentTime
                    );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentVideoStream = videoStream;
                }
            }
        });
        videoContol.add(new JLabel("Video:"));
        videoContol.add(videoComboBox);

        audioComboBox.setRenderer(new AudioComboBoxRenderer());
        audioComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof AudioStream audioStream) {
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    playVideo(
                        currentVideoStream,
                        audioStream,
                        currentSubtitlesStream,
                        currentTime
                    );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentAudioStream = audioStream;
                }
            }
        });
        videoContol.add(new JLabel("Audio:"));
        videoContol.add(audioComboBox);

        subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());
        subtitleComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
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
        });
        videoContol.add(new JLabel("Subtitle:"));
        videoContol.add(subtitleComboBox);

        speedBtm.addActionListener(e -> {
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
                    options[2]
                ) ==
                1
            ) {
                currentSpeed = s.getSpeed();
                BigDecimal speedDec = new BigDecimal(currentSpeed).divide(div);
                speedBtm.setText(df.format(speedDec) + "x");
                mediaPlayer.controls().setRate(speedDec.floatValue());
            }
        });
        videoContol.add(speedBtm);

        videoContol.add(fullScreenBtn);

        Font boldFont = videoTitle.getFont();
        boldFont = boldFont.deriveFont(Font.BOLD, boldFont.getSize());
        videoTitle.setFont(boldFont);
        videoTitlePanel.add(videoTitle);
        this.add(videoTitlePanel);

        uploaderInfo = new ChannelInfoPanel(mainViewPort);
        videoInfo.add(uploaderInfo);

        likeAndViewPanel.setLayout(
            new BoxLayout(likeAndViewPanel, BoxLayout.Y_AXIS)
        );
        viewCountLabel = new JLabel("", SwingConstants.RIGHT);
        likeAndViewPanel.add(viewCountLabel);
        likeCountLabel = new JLabel(
            "",
            IconRes.THUMP_UP_ICON,
            SwingConstants.RIGHT
        );
        likeAndViewPanel.add(likeCountLabel);
        likeAndViewPanelPanel.add(likeAndViewPanel);
        videoInfo.add(likeAndViewPanelPanel);
        this.add(videoInfo);

        copyUrlBtn.addActionListener(e -> {
            try {
                clipboard.setContents(
                    new StringSelection(streamExtractor.getUrl()),
                    null
                );
            } catch (ParsingException pe) {
                pe.printStackTrace();
            }
        });
        copyUrlBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        copyUrlBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        videoMenuBtnPanel.add(copyUrlBtn);

        openBrowserBtn.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                mediaPlayer.controls().pause();
                try {
                    Desktop.getDesktop().browse(
                        URI.create(streamExtractor.getUrl())
                    );
                } catch (IOException | ParsingException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        openBrowserBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        openBrowserBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        videoMenuBtnPanel.add(openBrowserBtn);

        openVLCBtn.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            }
            new Thread(() -> {
                VideoUtil.openVLC(streamExtractor, mainViewPort);
            })
                .start();
        });
        openVLCBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        openVLCBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        videoMenuBtnPanel.add(openVLCBtn);
        downloadBtn = new JButton("Donwload", IconRes.DOWNLOAD_ICON);
        downloadBtn.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            }
            new Thread(() -> {
                VideoUtil.downloadVideo(streamExtractor, false);
            })
                .start();
        });
        downloadBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        downloadBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        videoMenuBtnPanel.add(downloadBtn);
        this.add(videoMenuBtnPanel);

        relatedStreamsPanel = new JPanel();
        relatedStreamsPanel.setLayout(
            new BoxLayout(relatedStreamsPanel, BoxLayout.Y_AXIS)
        );

        videoCommentPanel.setLayout(
            new BoxLayout(videoCommentPanel, BoxLayout.Y_AXIS)
        );
        videoCommentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        videoDescriptionPanel.setLayout(
            new BoxLayout(videoDescriptionPanel, BoxLayout.Y_AXIS)
        );

        publishDateLabel = new JLabel();
        publishDatePanel.add(publishDateLabel);
        videoDescriptionPanel.add(publishDatePanel);

        videoDescriptionText = new JHTMLPane();

        videoDescriptionText.addHyperlinkListener(hyperlinkListener);
        Dimension maxSize = new Dimension(
            getPreferredSize().width,
            Integer.MAX_VALUE
        );
        oldWidth = maxSize.width;
        videoDescriptionText.setMaximumSize(maxSize);
        videoDescriptionTextPanel.add(videoDescriptionText);
        videoDescriptionPanel.add(videoDescriptionTextPanel);

        tmpLabel.setFont(boldFont);
        descriptionMatadata.add(tmpLabel);

        videoCategoryLabel = new JLabel("", SwingConstants.LEFT);
        descriptionMatadata.add(videoCategoryLabel);
        tmpLabel = new JLabel("License:", SwingConstants.RIGHT);
        tmpLabel.setFont(boldFont);
        descriptionMatadata.add(tmpLabel);
        licenseLabel = new JLabel("", SwingConstants.LEFT);
        descriptionMatadata.add(licenseLabel);
        tmpLabel = new JLabel("Privacy:", SwingConstants.RIGHT);
        tmpLabel.setFont(boldFont);
        descriptionMatadata.add(tmpLabel);
        privacyLabel = new JLabel("", SwingConstants.LEFT);
        descriptionMatadata.add(privacyLabel);
        videoDescriptionPanel.add(descriptionMatadata);
        tagPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
        tagPanel.setMaximumSize(maxSize);
        videoDescriptionPanel.add(tagPanel);

        commentBtn.setToolTipText("View video comment");
        commentBtn.addActionListener(e -> viewport.setView(videoCommentPanel));
        navigationBar.add(commentBtn);

        relatedStreamsBtn.setToolTipText("View related video");
        relatedStreamsBtn.addActionListener(e ->
            viewport.setView(relatedStreamsPanel)
        );
        navigationBar.add(relatedStreamsBtn);
        JButton descriptionBtn = new JButton(IconRes.DESCRIPTION_ICON);
        descriptionBtn.setToolTipText("View video description");
        descriptionBtn.addActionListener(e ->
            viewport.setView(videoDescriptionPanel)
        );
        navigationBar.add(descriptionBtn);
        this.add(navigationBar);

        viewport.setView(relatedStreamsPanel);
        this.add(viewport);
        fullScreenBtn.addActionListener(e -> {
            FullScreenApi fullScreenApi = mediaPlayer.fullScreen();
            fullScreenApi.set(!fullScreenApi.isFullScreen());
            App app = App.getInstance();
            if (fullScreenApi.isFullScreen()) {
                fullScreenBtn.setIcon(IconRes.FULLSCREEN_EXIT_ICON);
                viewport.setVisible(false);
                videoInfo.setVisible(false);
                navigationBar.setVisible(false);
                videoMenuBtnPanel.setVisible(false);
                videoTitlePanel.setVisible(false);
                app.setSearchbarVisible(false);
            } else {
                fullScreenBtn.setIcon(IconRes.FULLSCREEN_ICON);
                viewport.setVisible(true);
                videoInfo.setVisible(true);
                navigationBar.setVisible(true);
                videoMenuBtnPanel.setVisible(true);
                videoTitlePanel.setVisible(true);
                app.setSearchbarVisible(true);
            }
        });
        this.setFocusable(true);
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
                    playButton.doClick();
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
                        fullScreenBtn.doClick();
                    }
                }
            }
        );
        actionMap.put(
            "fullscreen",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fullScreenBtn.doClick();
                }
            }
        );
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
                    mediaPlayer.media().play(streamExtractor.getHlsUrl());
                } else {
                    List<VideoStream> videoStreams =
                        streamExtractor.getVideoOnlyStreams();
                    List<AudioStream> audioStreams =
                        streamExtractor.getAudioStreams();
                    List<SubtitlesStream> subtitlesStreams =
                        streamExtractor.getSubtitlesDefault();
                    currentVideoStream = videoStreams.get(0);
                    currentAudioStream = null;
                    downloadBtn.setEnabled(true);
                    for (AudioStream audioStream : audioStreams) {
                        Locale audiosLocale = audioStream.getAudioLocale();
                        if (
                            audiosLocale != null &&
                            audiosLocale.getLanguage() == "en"
                        ) {
                            currentAudioStream = audioStream;
                            break;
                        }
                    }
                    if (currentAudioStream == null) {
                        currentAudioStream = audioStreams.get(0);
                    }

                    currentSubtitlesStream = null;

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
                    mediaPlayer.controls().play();
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
    }

    public void playVideo(
        VideoStream videoStream,
        AudioStream audioStream,
        SubtitlesStream subtitlesStream,
        long currentTime
    ) {
        this.requestFocus();
        MediaApi mediaApi = mediaPlayer.media();
        mediaApi.prepare(
            videoStream.getContent(),
            String.format(":start-time=%.3f", currentTime / 1000.0),
            ":input-slave=" + currentAudioStream.getContent()
        );

        // mediaApi
        //     .slaves()
        //     .add(
        //         MediaSlaveType.AUDIO,
        //         MediaSlavePriority.HIGH,
        //         currentAudioStream.getContent()
        //     );
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
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
        }

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICON);
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

        public SpeedSelectorPanel() {
            setLayout(new BorderLayout());
            speed = currentSpeed;
            JLabel speedLanel = new JLabel(
                df.format(new BigDecimal(currentSpeed).divide(div)) + "x",
                SwingConstants.CENTER
            );
            JPanel speedLabelPanel = new JPanel(new BorderLayout());
            speedLabelPanel.add(new JLabel("0.1x"), BorderLayout.LINE_START);
            speedLabelPanel.add(speedLanel, BorderLayout.CENTER);
            speedLabelPanel.add(new JLabel("10x"), BorderLayout.LINE_END);
            JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
            JButton subBtn = new JButton("-25%");
            slider = new JSlider(10, 1000, currentSpeed);
            JButton addBtn = new JButton("+25%");
            this.add(subBtn, BorderLayout.LINE_START);
            subPanel.add(speedLabelPanel);
            subPanel.add(slider);
            this.add(addBtn, BorderLayout.LINE_END);
            subBtn.addActionListener(e -> slider.setValue(speed - 25));
            addBtn.addActionListener(e -> slider.setValue(speed + 25));
            slider.addChangeListener(e -> {
                speed = slider.getValue();
                speedLanel.setText(
                    df.format(new BigDecimal(speed).divide(div)) + "x"
                );
            });

            this.add(subPanel);
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
