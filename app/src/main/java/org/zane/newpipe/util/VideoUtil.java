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
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.zane.newpipe.ui.IconRes;
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
            Component optionComponent = switch (component) {
                case null -> {
                    JFrame frame = new JFrame();
                    frame.setIconImage(IconRes.LAUNCHER_ICON);
                    frame.setUndecorated(true);
                    frame.setVisible(true);
                    yield frame;
                }
                default -> component;
            };
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
                        optionComponent,
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
                        } finally {
                            if (component == null) {
                                optionComponent.setVisible(false);
                                System.exit(0);
                            }
                        }
                    })
                        .start();
                } else {
                    if (component == null) {
                        optionComponent.setVisible(false);
                        System.exit(0);
                    }
                }
            });
        } catch (ExtractionException | IOException err) {
            err.printStackTrace();
            if (component == null) {
                throw new RuntimeException(err);
            }
        }
    }

    public static void downloadVideo(
        String videoURL,
        boolean isCloseAfterDone
    ) {
        new Thread(() -> {
            new Thread(() -> {
                try {
                    StreamExtractor se = ServiceList.YouTube.getStreamExtractor(
                        videoURL
                    );
                    se.fetchPage();
                    downloadVideo(se, isCloseAfterDone);
                } catch (ExtractionException | IOException ee) {
                    ee.printStackTrace();
                }
            })
                .start();
        })
            .start();
    }

    public static void downloadVideo(
        StreamExtractor se,
        boolean isCloseAfterDone
    ) {
        try {
            List<VideoStream> videoStreams = se.getVideoOnlyStreams();
            List<AudioStream> audioStreams = se.getAudioStreams();
            String videoTitle = se.getName();
            List<SubtitlesStream> subtitlesStreams = se.getSubtitlesDefault();
            JPanel downloadPanel = new JPanel(new GridLayout(3, 1));
            JPanel videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
            DefaultComboBoxModel<VideoStream> videoModel =
                new DefaultComboBoxModel<>();
            videoModel.addElement(null);
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
            audioModel.addElement(null);
            audioModel.addAll(audioStreams);
            audioModel.setSelectedItem(audioStreams.get(0));
            JComboBox<AudioStream> audioComboBox = new JComboBox<AudioStream>(
                audioModel
            );
            audioComboBox.setRenderer(new AudioComboBoxRenderer());
            videoContol.add(new JLabel("Audio:"));
            videoContol.add(audioComboBox);
            DefaultComboBoxModel<SubtitlesStream> subtitleModel =
                new DefaultComboBoxModel<SubtitlesStream>();
            subtitleModel.addElement(null);
            JComboBox<SubtitlesStream> subtitleComboBox = new JComboBox<
                SubtitlesStream
            >(subtitleModel);
            subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());
            videoContol.add(new JLabel("Subtitle:"));
            videoContol.add(subtitleComboBox);
            if (!subtitlesStreams.isEmpty()) {
                subtitleModel.addAll(subtitlesStreams);
            } else {
                subtitleComboBox.setEnabled(false);
            }
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
            videoComboBox.addItemListener(e -> {
                downloadBtn.setEnabled(false);
                saveFileText.setText("");
            });
            audioComboBox.addItemListener(e -> {
                downloadBtn.setEnabled(false);
                saveFileText.setText("");
            });
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
            dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (isCloseAfterDone) {
                            System.exit(0);
                        }
                    }
                }
            );
            saveFileBtn.addActionListener(e -> {
                SystemFileChooser fileChooser = new SystemFileChooser();
                fileChooser.setDialogTitle("Specify a file to save");
                VideoStream vs = (VideoStream) videoComboBox.getSelectedItem();
                AudioStream as = (AudioStream) audioComboBox.getSelectedItem();
                String vCodec = null;
                if (vs != null) {
                    vCodec = vs.getCodec();
                    int dotindex = vCodec.indexOf('.');
                    if (dotindex > -1) {
                        vCodec = vCodec.substring(0, dotindex);
                    }
                }
                String aCodec = null;
                if (as != null) {
                    aCodec = as.getCodec();
                    int dotIndex = aCodec.indexOf('.');
                    if (dotIndex > -1) {
                        aCodec = aCodec.substring(0, dotIndex);
                    }
                }
                switch (vCodec) {
                    case "avc1":
                        switch (aCodec) {
                            case null:
                            case "mp4a":
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Video Files (*.mp4)",
                                        "mp4"
                                    )
                                );
                            default:
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Video Files (*.mkv)",
                                        "mkv"
                                    )
                                );
                                break;
                        }
                        break;
                    case "vp9":
                    case "av01":
                        switch (aCodec) {
                            case null:
                            case "opus":
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Video Files (*.webm)",
                                        "webm"
                                    )
                                );
                            default:
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Video Files (*.mkv)",
                                        "mkv"
                                    )
                                );
                        }
                        break;
                    case null:
                        switch (aCodec) {
                            case "mp4a":
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Audio Files (*.m4a)",
                                        "m4a"
                                    )
                                );
                                break;
                            case "opus":
                                fileChooser.addChoosableFileFilter(
                                    new SystemFileChooser.FileNameExtensionFilter(
                                        "Audio Files (*.weba)",
                                        "weba"
                                    )
                                );
                                break;
                            case null:
                                JOptionPane.showMessageDialog(
                                    null,
                                    "Must have eiter video or audio",
                                    "No codex seleted",
                                    JOptionPane.ERROR_MESSAGE
                                );
                                return;
                            default:
                                break;
                        }
                        fileChooser.addChoosableFileFilter(
                            new SystemFileChooser.FileNameExtensionFilter(
                                "Audio Files (*.ogg)",
                                "ogg"
                            )
                        );
                        break;
                    default:
                        fileChooser.addChoosableFileFilter(
                            new SystemFileChooser.FileNameExtensionFilter(
                                "Video Files (*.mkv)",
                                "mkv"
                            )
                        );
                        break;
                }

                String saveFilePath = saveFileText.getText();
                if (saveFilePath.isBlank()) {
                    saveFilePath =
                        videoTitle.replaceAll("[\\\\/:*?\"<>|]", "") +
                        "." +
                        (
                            (SystemFileChooser.FileNameExtensionFilter) fileChooser.getChoosableFileFilters()[0]
                        ).getExtensions()[0];
                }

                fileChooser.setSelectedFile(new File(saveFilePath));
                fileChooser.setAcceptAllFileFilterUsed(false);

                int userSelection = fileChooser.showSaveDialog(optionPane);

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
            progressBar.setForeground(IconRes.YOUTUBE_COLOUR);
            downloadPanel.add(progressBar);

            dialog.setIconImage(IconRes.LAUNCHER_ICON);

            downloadBtn.addActionListener(e -> {
                downloadBtn.setEnabled(false);
                saveFileBtn.setEnabled(false);
                videoComboBox.setEnabled(false);
                audioComboBox.setEnabled(false);
                subtitleComboBox.setEnabled(false);
                VideoStream vs = (VideoStream) videoComboBox.getSelectedItem();
                AudioStream as = (AudioStream) audioComboBox.getSelectedItem();
                SubtitlesStream ss =
                    (SubtitlesStream) subtitleComboBox.getSelectedItem();
                String fileName = saveFileText.getText();
                String fileExt = fileName.substring(
                    fileName.lastIndexOf('.') + 1
                );
                fileExt = switch (fileExt) {
                    case "m4a" -> "mp4";
                    case "weba" -> "webm";
                    default -> fileExt;
                };
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
                                JOptionPane.showMessageDialog(
                                    dialog,
                                    "Download Complated"
                                );
                                dialog.setVisible(false);
                            }
                        }
                    );

                if (vs == null) {
                    mediaPlayer
                        .media()
                        .prepare(as.getContent(), ":sout=" + sout);
                } else {
                    if (as == null) {
                        mediaPlayer
                            .media()
                            .prepare(vs.getContent(), ":sout=" + sout);
                    } else {
                        mediaPlayer
                            .media()
                            .prepare(
                                vs.getContent(),
                                ":input-slave=" + as.getContent(),
                                ":sout=" + sout
                            );
                    }
                }

                if (ss != null) {
                    mediaPlayer.subpictures().setSubTitleUri(ss.getContent());
                }
                dialog.addWindowListener(
                    new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (mediaPlayer.status().isPlaying()) {
                                mediaPlayer.controls().stop();
                            }
                            mediaPlayer.media().reset();
                        }
                    }
                );
                mediaPlayer.controls().play();
            });
            dialog.setVisible(true);
        } catch (ExtractionException | IOException e) {
            e.printStackTrace();
            if (isCloseAfterDone) {
                throw new RuntimeException(e);
            }
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
            } else if (value == null) {
                setText("None");
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
            } else if (value == null) {
                setText("None");
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
                Locale locale = subtitlesStream.getLocale();
                String text;
                if (locale == null) {
                    text = subtitlesStream.getDisplayLanguageName();
                } else {
                    text = locale.getDisplayLanguage();
                    String countryName = locale.getCountry();
                    if (countryName != null && !countryName.isBlank()) {
                        text += "(" + countryName + ")";
                    }
                }
                setText(text);
            } else if (value == null) {
                setText("None");
            }
            return this;
        }
    }
}
