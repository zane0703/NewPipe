package org.zane.newpipe.util;

import com.formdev.flatlaf.util.SystemFileChooser;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

public class VideoUtil {

    private static String vlcPath;

    public static void setVlcPath(String vlcPath) {
        VideoUtil.vlcPath = vlcPath;
    }

    public static void openVLC(String videoURL, Component component) {
        new Thread(() -> {
            try {
                StreamExtractor se = ServiceList.YouTube.getStreamExtractor(
                    videoURL
                );
                se.fetchPage();
                openVLC(se, component);
            } catch (ExtractionException | IOException ee) {
                ee.printStackTrace();
            }
        })
            .start();
    }

    public static void openVLC(StreamExtractor se, Component component) {
        try {
            if (VideoUtil.vlcPath == null) {
                return;
            }
            String videoTitle = se.getName();
            String uploaderName = se.getUploaderName();
            if (se.getStreamType() == StreamType.LIVE_STREAM) {
                String hlsUrl = se.getHlsUrl();
                new Thread(() -> {
                    ProcessBuilder builder = new ProcessBuilder(
                        VideoUtil.vlcPath + "/vlc",
                        "--meta-title=" + videoTitle,
                        "--meta-artist=" + uploaderName,
                        hlsUrl
                    );
                    try {
                        builder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                    .start();
                return;
            }
            List<VideoStream> videoStreams = se.getVideoOnlyStreams();
            List<AudioStream> audioStreams = se.getAudioStreams();
            List<SubtitlesStream> subtitlesStreams = se.getSubtitlesDefault();
            JPanel videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
            DefaultComboBoxModel<VideoStream> videoModel =
                new DefaultComboBoxModel<>();
            videoModel.addAll(videoStreams);
            videoModel.setSelectedItem(videoStreams.get(0));
            JComboBox<VideoStream> videoComboBox = new JComboBox<VideoStream>(
                videoModel
            );
            videoComboBox.setRenderer(new VideoComboBoxRenderer());
            videoContol.add(new JLabel("Video:"));
            videoContol.add(videoComboBox);
            DefaultComboBoxModel<AudioStream> audioModel =
                new DefaultComboBoxModel<>();
            audioModel.addAll(audioStreams);
            audioModel.setSelectedItem(audioStreams.get(0));
            JComboBox<AudioStream> audioComboBox = new JComboBox<AudioStream>(
                audioModel
            );
            audioComboBox.setRenderer(new AudioComboBoxRenderer());
            videoContol.add(new JLabel("Audio:"));
            videoContol.add(audioComboBox);
            JComboBox<SubtitlesStream> subtitleComboBox = null;
            if (!subtitlesStreams.isEmpty()) {
                DefaultComboBoxModel<SubtitlesStream> subtitleModel =
                    new DefaultComboBoxModel<SubtitlesStream>();
                subtitleModel.addElement(null);
                subtitleModel.addAll(subtitlesStreams);
                subtitleComboBox = new JComboBox<SubtitlesStream>(
                    subtitleModel
                );
                subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());
                videoContol.add(new JLabel("Subtitle:"));
                videoContol.add(subtitleComboBox);
            }
            JComboBox<SubtitlesStream> subtitleComboBox2 = subtitleComboBox;
            SwingUtilities.invokeLater(() -> {
                if (
                    JOptionPane.showConfirmDialog(
                        component,
                        videoContol,
                        "Open in VLC Media Player",
                        JOptionPane.OK_CANCEL_OPTION
                    ) ==
                    JOptionPane.OK_OPTION
                ) {
                    new Thread(() -> {
                        VideoStream vs =
                            (VideoStream) videoComboBox.getSelectedItem();
                        AudioStream as =
                            (AudioStream) audioComboBox.getSelectedItem();
                        SubtitlesStream ss = null;
                        if (subtitleComboBox2 != null) {
                            ss =
                                (SubtitlesStream) subtitleComboBox2.getSelectedItem();
                        }

                        ArrayList<String> commands = new ArrayList<>(6);
                        commands.add(VideoUtil.vlcPath + "/vlc");
                        commands.add("--meta-title=" + videoTitle);
                        commands.add("--meta-artist=" + uploaderName);
                        // commands.add(videoTitle);
                        if (ss != null) {
                            commands.add(
                                "--input-slave=" +
                                    as.getContent() +
                                    "#" +
                                    ss.getContent()
                            );
                            commands.add("--sub-track=1");
                        } else {
                            commands.add("--input-slave=" + as.getContent());
                        }
                        commands.add(vs.getContent());
                        ProcessBuilder processBuilder = new ProcessBuilder(
                            commands
                        );
                        try {
                            processBuilder.start();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    })
                        .start();
                }
            });
        } catch (ExtractionException | IOException err) {
            err.printStackTrace();
        }
    }

    public static void downloadVideo(String videoURL) {
        new Thread(() -> {
            new Thread(() -> {
                try {
                    StreamExtractor se = ServiceList.YouTube.getStreamExtractor(
                        videoURL
                    );
                    se.fetchPage();
                    downloadVideo(se);
                } catch (ExtractionException | IOException ee) {
                    ee.printStackTrace();
                }
            })
                .start();
        })
            .start();
    }

    public static void downloadVideo(StreamExtractor se) {
        try {
            List<VideoStream> videoStreams = se.getVideoOnlyStreams();
            List<AudioStream> audioStreams = se.getAudioStreams();
            String videoTitle = se.getName();
            List<SubtitlesStream> subtitlesStreams = se.getSubtitlesDefault();
            JPanel downloadPanel = new JPanel(new GridLayout(3, 1));
            JPanel videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
            DefaultComboBoxModel<VideoStream> videoModel =
                new DefaultComboBoxModel<>();
            videoModel.addAll(videoStreams);
            videoModel.setSelectedItem(videoStreams.get(0));
            JComboBox<VideoStream> videoComboBox = new JComboBox<VideoStream>(
                videoModel
            );
            videoComboBox.setRenderer(new VideoComboBoxRenderer());
            videoContol.add(new JLabel("Video:"));
            videoContol.add(videoComboBox);
            DefaultComboBoxModel<AudioStream> audioModel =
                new DefaultComboBoxModel<>();
            audioModel.addAll(audioStreams);
            audioModel.setSelectedItem(audioStreams.get(0));
            JComboBox<AudioStream> audioComboBox = new JComboBox<AudioStream>(
                audioModel
            );
            audioComboBox.setRenderer(new AudioComboBoxRenderer());
            videoContol.add(new JLabel("Audio:"));
            videoContol.add(audioComboBox);
            JComboBox<SubtitlesStream> subtitleComboBox = null;
            if (!subtitlesStreams.isEmpty()) {
                DefaultComboBoxModel<SubtitlesStream> subtitleModel =
                    new DefaultComboBoxModel<SubtitlesStream>();
                subtitleModel.addElement(null);
                subtitleModel.addAll(subtitlesStreams);
                subtitleComboBox = new JComboBox<SubtitlesStream>(
                    subtitleModel
                );
                subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());
                videoContol.add(new JLabel("Subtitle:"));
                videoContol.add(subtitleComboBox);
            }
            JComboBox<SubtitlesStream> subtitleComboBox2 = subtitleComboBox;
            downloadPanel.add(videoContol);
            JPanel saveFilepanel = new JPanel(new GridBagLayout());
            JTextField saveFileText = new JTextField();
            saveFileText.setEditable(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            saveFilepanel.add(saveFileText, gbc);
            JButton saveFileBtn = new JButton("Browser");
            JButton downloadBtn = new JButton("Download");
            downloadBtn.setEnabled(false);
            saveFileBtn.addActionListener(e -> {
                SystemFileChooser fileChooser = new SystemFileChooser();
                fileChooser.setDialogTitle("Specify a file to save");
                fileChooser.addChoosableFileFilter(
                    new SystemFileChooser.FileNameExtensionFilter(
                        "Video Files (*.mp4)",
                        "mp4"
                    )
                );
                fileChooser.addChoosableFileFilter(
                    new SystemFileChooser.FileNameExtensionFilter(
                        "Video Files (*.mkv)",
                        "mkv"
                    )
                );
                String savefilePath = saveFileText.getText();
                if (savefilePath.isBlank()) {
                    savefilePath = videoTitle + ".mp4";
                }

                fileChooser.setSelectedFile(new File(savefilePath));
                fileChooser.setAcceptAllFileFilterUsed(false);

                int userSelection = fileChooser.showSaveDialog(null);

                if (userSelection == SystemFileChooser.APPROVE_OPTION) {
                    SystemFileChooser.FileNameExtensionFilter filter =
                        (SystemFileChooser.FileNameExtensionFilter) fileChooser.getFileFilter();
                    File fileToSave = fileChooser.getSelectedFile();
                    String absolutePata = fileToSave.getAbsolutePath();
                    String extensions = filter.getExtensions()[0];
                    if (!absolutePata.toLowerCase().endsWith(extensions)) {
                        absolutePata += "." + extensions;
                    }
                    downloadBtn.setEnabled(true);
                    saveFileText.setText(absolutePata);
                    // Proceed with saving the file via FileOutputStream or similar
                }
            });
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            saveFilepanel.add(saveFileBtn, gbc);
            downloadPanel.add(saveFilepanel);
            JProgressBar progressBar = new JProgressBar(0, 100);

            downloadPanel.add(progressBar);

            Object[] options = new Object[] { downloadBtn, "Cancel" };
            JOptionPane optionPane = new JOptionPane(
                downloadPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options,
                options[0]
            );

            JDialog dialog = optionPane.createDialog("Download video");
            dialog.setModal(false);

            downloadBtn.addActionListener(e -> {
                downloadBtn.setEnabled(false);
                saveFileBtn.setEnabled(false);
                videoComboBox.setEnabled(false);
                audioComboBox.setEnabled(false);
                if (subtitleComboBox2 != null) {
                    subtitleComboBox2.setEnabled(false);
                }
                VideoStream vs = (VideoStream) videoComboBox.getSelectedItem();
                AudioStream as = (AudioStream) audioComboBox.getSelectedItem();
                SubtitlesStream ss = null;
                if (subtitleComboBox2 != null) {
                    ss = (SubtitlesStream) subtitleComboBox2.getSelectedItem();
                }
                String fileName = saveFileText.getText();
                String fileExt = fileName.substring(
                    fileName.lastIndexOf('.') + 1
                );
                String slaveInput = ":input-slave=" + as.getContent();
                if (ss != null) {
                    slaveInput += "#" + ss.getContent();
                }
                String sout =
                    "#std{access=file,mux=" +
                    fileExt +
                    ",dst=\"" +
                    fileName +
                    "\"}";
                MediaPlayerFactory factory = new MediaPlayerFactory();
                MediaPlayer mediaPlayer = factory
                    .mediaPlayers()
                    .newMediaPlayer();
                mediaPlayer
                    .events()
                    .addMediaPlayerEventListener(
                        new MediaPlayerEventAdapter() {
                            @Override
                            public void positionChanged(
                                MediaPlayer mediaPlayer,
                                float newPosition
                            ) {
                                SwingUtilities.invokeLater(() ->
                                    progressBar.setValue(
                                        (int) (newPosition * 100)
                                    )
                                );
                            }

                            @Override
                            public void finished(MediaPlayer mediaPlayer) {
                                dialog.setVisible(false);
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Download Complated"
                                );
                            }
                        }
                    );
                mediaPlayer
                    .media()
                    .play(vs.getContent(), slaveInput, ":sout=" + sout);
                dialog.addWindowListener(
                    new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (mediaPlayer.status().isPlaying()) {
                                mediaPlayer.controls().start();
                            }
                        }
                    }
                );
            });
            dialog.setVisible(true);
        } catch (ExtractionException | IOException e) {
            e.printStackTrace();
        }
    }

    public static class VideoComboBoxRenderer extends DefaultListCellRenderer {

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
                String codec = videoStream.getCodec();
                int dotIndex = codec.indexOf('.');
                if (dotIndex > -1) {
                    codec = codec.subSequence(0, dotIndex).toString();
                }
                setText(
                    codec +
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

    public static class AudioComboBoxRenderer extends DefaultListCellRenderer {

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
                String codec = audioStream.getCodec();
                int dotIndex = codec.indexOf('.');
                if (dotIndex > -1) {
                    codec = codec.subSequence(0, dotIndex).toString();
                }
                String text =
                    codec +
                    " " +
                    CommonUtil.numberToStringUnit(audioStream.getBitrate()) +
                    "bps";

                Locale locale = audioStream.getAudioLocale();
                if (locale != null) {
                    text += " " + locale.getDisplayLanguage();
                    String countryString = locale.getCountry();
                    if (countryString != null && !countryString.isBlank()) {
                        text += "(" + locale.getCountry() + ")";
                    }
                }
                setText(text);
            }
            return this;
        }
    }

    public static class SubTitleComboBoxRenderer
        extends DefaultListCellRenderer
    {

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
            if (value instanceof SubtitlesStream subtitlesStream) {
                setText(subtitlesStream.getDisplayLanguageName());
            } else if (value == null) {
                setText("None");
            }
            return this;
        }
    }
}
