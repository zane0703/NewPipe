package org.zane.newpipe;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.MediaSlavePriority;
import uk.co.caprica.vlcj.media.MediaSlaveType;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPage extends JPanel {

    private final App app;
    private CallbackMediaPlayerComponent mediaPlayerComponent;
    private JPanel videoContol;
    private JComboBox videoQ;
    private DefaultComboBoxModel<VideoStream> videoQM;
    private JComboBox audioQ;
    private DefaultComboBoxModel<AudioStream> audioQM;
    private AudioStream currentAudioStream;
    private VideoStream currentVideoStream;
    private boolean isEnableComboxEvent;
    private JSlider playbackSlider;
    private boolean isPositionChanged = false;
    private JLabel videoTimestamp;
    private JLabel videoLenght;
    private JLabel videoTitle;
    private JLabel uplodoaderName;
    private String channelURL;
    private double videoLengthDiff;
    private FlatSVGIcon playIcom;
    private FlatSVGIcon pauseIcom;
    private JButton playButton;

    public VideoPage(App app) {
        this.app = app;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.addComponentListener(
            new ComponentListener() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    // Code to execute when the JPanel is hidden
                    //
                    mediaPlayerComponent.mediaPlayer().controls().stop();
                    mediaPlayerComponent.mediaPlayer().release();
                }

                @Override
                public void componentShown(ComponentEvent e) {}

                public void componentMoved(ComponentEvent e) {}

                public void componentResized(ComponentEvent e) {}
            }
        );
        mediaPlayerComponent = new CallbackMediaPlayerComponent();
        final MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();
        mediaPlayerComponent.setMaximumSize(new Dimension(1000, 500));
        mediaPlayer
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());
        this.add(mediaPlayerComponent);
        // c.gridy = 1;

        playbackSlider = new JSlider(
            SwingConstants.HORIZONTAL,
            0,
            Integer.MAX_VALUE,
            0
        );

        playbackSlider.addChangeListener(e -> {
            if (playbackSlider.getValueIsAdjusting() || isPositionChanged) {
                return;
            }
            System.out.println("change timestamp");
            mediaPlayer
                .controls()
                .setTime((long) (playbackSlider.getValue() * videoLengthDiff));
        });
        this.add(playbackSlider);
        //c.gridy = 2;
        videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.add(videoContol);
        try {
            playIcom = new FlatSVGIcon(
                getClass().getResourceAsStream("/icon/ic_play_arrow.svg")
            );
            pauseIcom = new FlatSVGIcon(
                getClass().getResourceAsStream("/icon/ic_pause.svg")
            );
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        playButton = new JButton(playIcom);
        playButton.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            } else {
                mediaPlayer.controls().play();
            }
        });
        videoContol.add(playButton);
        videoTimestamp = new JLabel("--:--:--");
        videoContol.add(videoTimestamp);
        videoLenght = new JLabel("/--:--:--");
        videoContol.add(videoLenght);
        videoQM = new DefaultComboBoxModel<>();
        videoQ = new JComboBox<VideoStream>(videoQM);
        videoQ.setRenderer(new VideoComboBoxRenderer());
        videoQ.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof VideoStream videoStream) {
                    MediaApi media = mediaPlayer.media();
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    media.prepare(
                        videoStream.getContent(),
                        String.format(":start-time=%.3f", currentTime / 1000.0)
                    );

                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            currentAudioStream.getContent()
                        );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentVideoStream = videoStream;
                }
            }
        });
        videoContol.add(videoQ);
        audioQM = new DefaultComboBoxModel<>();
        audioQ = new JComboBox<AudioStream>(audioQM);
        audioQ.setRenderer(new AudioComboBoxRenderer());
        audioQ.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof AudioStream audioStream) {
                    MediaApi media = mediaPlayer.media();
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    media.prepare(
                        currentVideoStream.getContent(),
                        String.format(":start-time=%.3f", currentTime / 1000.0)
                    );

                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            audioStream.getContent()
                        );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentAudioStream = audioStream;
                }
            }
        });
        videoContol.add(audioQ);
        JPanel videoTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        videoTitle = new JLabel("", SwingConstants.LEFT);
        videoTitlePanel.add(videoTitle);
        this.add(videoTitlePanel);
        JPanel uploaderInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        uplodoaderName = new JLabel("", SwingConstants.LEFT);
        uploaderInfo.addMouseListener(new PanelClickListener());
        uploaderInfo.add(uplodoaderName);
        this.add(uploaderInfo);
    }

    public void showVideo(String videoUrl) {
        new Thread(() -> {
            try {
                StreamExtractor streamExtractor =
                    ServiceList.YouTube.getStreamExtractor(videoUrl);
                streamExtractor.fetchPage();
                channelURL = streamExtractor.getUploaderUrl();
                videoTitle.setText(streamExtractor.getName());
                System.out.println("streamExtractor.getSubChannelName())");
                System.out.println(streamExtractor.getUploaderName());
                uplodoaderName.setText(streamExtractor.getUploaderName());
                List<org.schabi.newpipe.extractor.Image> avatars =
                    streamExtractor.getUploaderAvatars();
                if (!avatars.isEmpty()) {
                    try {
                        BufferedImage image = ImageIO.read(
                            new URI(avatars.get(0).getUrl()).toURL()
                        );
                        if (image != null) {
                            ImageIcon icon = new ImageIcon(image);
                            // Create a JLabel to hold the icon
                            uplodoaderName.setIcon(icon);
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                List<VideoStream> videoStreams =
                    streamExtractor.getVideoOnlyStreams();
                currentVideoStream = videoStreams.get(0);
                List<AudioStream> audioStreams =
                    streamExtractor.getAudioStreams();
                currentAudioStream = audioStreams.get(0);
                videoQM.addAll(videoStreams);
                audioQM.addAll(audioStreams);
                //System.out.println("audi size " + audioStreams.size());

                SwingUtilities.invokeLater(() -> {
                    isEnableComboxEvent = false;
                    videoQM.setSelectedItem(currentVideoStream);
                    audioQM.setSelectedItem(currentAudioStream);
                    isEnableComboxEvent = true;
                    MediaPlayer MediaPlayer =
                        mediaPlayerComponent.mediaPlayer();
                    MediaApi media = MediaPlayer.media();
                    media.prepare(currentVideoStream.getContent());
                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            currentAudioStream.getContent()
                        );
                    MediaPlayer.controls().play();
                });
            } catch (ExtractionException | IOException e) {
                e.printStackTrace();
            }
        })
            .start();
    }

    public void stop() {
        mediaPlayerComponent.mediaPlayer().controls().stop();
    }

    public class VideoComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );
            if (value instanceof VideoStream videoStream) {
                setText(
                    videoStream.getCodec() +
                        " " +
                        videoStream.getResolution() +
                        " " +
                        videoStream.getFps() +
                        "FPS"
                );
            }
            return this;
        }
    }

    public class AudioComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );
            if (value instanceof AudioStream audioStream) {
                setText(
                    audioStream.getCodec() + " " + audioStream.getBitrate()
                );
            }
            return this;
        }
    }

    public class MyMediaPlayerEventListener
        implements MediaPlayerEventListener
    {

        @Override
        public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {}

        @Override
        public void opening(MediaPlayer mediaPlayer) {}

        @Override
        public void buffering(MediaPlayer mediaPlayer, float newCache) {}

        @Override
        public void playing(MediaPlayer mediaPlayer) {
            playButton.setIcon(pauseIcom);
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
        }

        @Override
        public void forward(MediaPlayer mediaPlayer) {}

        @Override
        public void backward(MediaPlayer mediaPlayer) {}

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
        }

        @Override
        public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
            String newTimeString = getTimeString(newTime);
            int newTimeSlid = (int) (newTime / videoLengthDiff);
            SwingUtilities.invokeLater(() -> {
                videoTimestamp.setText(newTimeString);
                if (playbackSlider.getValueIsAdjusting()) {
                    return;
                }
                isPositionChanged = true;
                playbackSlider.setValue(newTimeSlid);
                isPositionChanged = false;
            });
        }

        @Override
        public void positionChanged(
            MediaPlayer mediaPlayer,
            float newPosition
        ) {
            // if (playbackSlider.getValueIsAdjusting()) {
            //     return;
            // }
            // isPositionChanged = true;
            // playbackSlider.setValue((int) (newPosition * 100));
            // SwingUtilities.invokeLater(() -> {
            //     isPositionChanged = false;
            // });
        }

        @Override
        public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {}

        @Override
        public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {}

        @Override
        public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {}

        @Override
        public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {}

        @Override
        public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {}

        @Override
        public void videoOutput(MediaPlayer mediaPlayer, int newCount) {}

        @Override
        public void scrambledChanged(
            MediaPlayer mediaPlayer,
            int newScrambled
        ) {}

        @Override
        public void elementaryStreamAdded(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void elementaryStreamDeleted(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void elementaryStreamSelected(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void corked(MediaPlayer mediaPlayer, boolean corked) {}

        @Override
        public void muted(MediaPlayer mediaPlayer, boolean muted) {}

        @Override
        public void volumeChanged(MediaPlayer mediaPlayer, float volume) {}

        @Override
        public void audioDeviceChanged(
            MediaPlayer mediaPlayer,
            String audioDevice
        ) {}

        @Override
        public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {}

        @Override
        public void error(MediaPlayer mediaPlayer) {}

        @Override
        public void mediaPlayerReady(MediaPlayer mediaPlayer) {
            long lenght = mediaPlayer.status().length();
            videoLenght.setText("/" + getTimeString(lenght));
            videoLengthDiff = (double) lenght / (double) Integer.MAX_VALUE;
        }

        private String getTimeString(long time) {
            Duration duration = Duration.ofMillis(time);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart(); // toMinutesPart() in Java 9+, use arithmetic for Java 8
            long seconds = duration.toSecondsPart(); // toSecondsPart() in Java 9+, use arithmetic for Java 8
            if (hours == 0) {
                return String.format("%02d:%02d", minutes, seconds);
            }
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    private class PanelClickListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            app.nevigate(App.Page.CHANNEL, channelURL);
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
