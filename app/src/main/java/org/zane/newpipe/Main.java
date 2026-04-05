package org.zane.newpipe;

import com.formdev.flatlaf.IntelliJTheme;
import com.sun.jna.NativeLibrary;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.schabi.newpipe.extractor.NewPipe;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.util.Downloader;
import org.zane.newpipe.util.VideoUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.InitializationException;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

@Command(
    name = "NewPipe Desktop",
    mixinStandardHelpOptions = true,
    version = "NewPipe Desktop v0.3"
)
public class Main implements Runnable {

    public static enum LaunchMode {
        DEFAULT,
        DOWNLOAD,
        VLC,
    }

    @Option(
        names = { "-m", "--mode" },
        description = "Valid values: ${COMPLETION-CANDIDATES}",
        defaultValue = "DEFAULT",
        paramLabel = "Launch Mode"
    )
    LaunchMode launchMode;

    @Option(
        names = { "-v", "--vlc" },
        description = "custom VLC media player install location",
        paramLabel = "VLC Path"
    )
    String vlcPath;

    @Option(
        names = { "-t", "--theme" },
        description = "custom theme file JSON based on IntelliJ Theme",
        paramLabel = "theme.json"
    )
    File theme;

    @Option(
        names = { "-n", "--no-auto-play" },
        description = "stop auto playing"
    )
    boolean isNoAutoPlay;

    @Parameters(
        arity = "0..1",
        index = "0",
        paramLabel = "Query",
        description = "Search query or video URL"
    )
    String query;

    @Spec
    CommandSpec spec;

    public void run() {
        NewPipe.init(new Downloader());
        UIManager.put("TabbedPane.tabAreaAlignment", "fill");
        UIManager.put("TabbedPane.tabWidthMode", "equal");

        System.setProperty("flatlaf.useWindowDecorations", "false");
        if (theme == null) {
            IntelliJTheme.setup(
                App.class.getResourceAsStream("/Darcula_Pitch_Black.theme.json")
            );
        } else {
            try {
                IntelliJTheme.setup(new FileInputStream(theme));
            } catch (FileNotFoundException e) {
                ArgSpec querySpec = null;
                for (ArgSpec argSpec : spec.args()) {
                    if (argSpec.paramLabel().equals("theme.json")) {
                        querySpec = argSpec;
                        break;
                    }
                }
                throw new ParameterException(
                    spec.commandLine(),
                    "custom theme file not found",
                    e,
                    querySpec,
                    theme.toString()
                );
            }
        }

        AtomicReference<TrayIcon> trayIcon = new AtomicReference<>(null);

        if (SystemTray.isSupported()) {
            try {
                TrayIcon trayIcon2 = new TrayIcon(
                    IconRes.LAUNCHER_ICON,
                    "NewPipe"
                );
                SystemTray.getSystemTray().add(trayIcon2);
                trayIcon.set(trayIcon2);
            } catch (AWTException e) {
                trayIcon.set(null);
            }
        }

        if (vlcPath != null && !vlcPath.isBlank()) {
            vlcPath = vlcPath.trim();
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcLibraryName(),
                vlcPath
            );
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcCoreLibraryName(),
                vlcPath
            );
            try {
                NativeLibrary.getInstance(RuntimeUtil.getLibVlcLibraryName());
                VideoUtil.setVlcPath(vlcPath);
            } catch (UnsatisfiedLinkError e) {
                ArgSpec querySpec = null;
                for (ArgSpec argSpec : spec.args()) {
                    if (argSpec.paramLabel().equals("VLC Path")) {
                        querySpec = argSpec;
                        break;
                    }
                }
                throw new ParameterException(
                    spec.commandLine(),
                    "the VLC path provided is invalid.",
                    querySpec,
                    vlcPath
                );
            }
        } else {
            NativeDiscovery nd = new NativeDiscovery();
            if (nd.discover()) {
                VideoUtil.setVlcPath(nd.discoveredPath());
            } else {
                throw new InitializationException("VLC not found.");
            }
        }

        switch (launchMode) {
            case DEFAULT:
                SwingUtilities.invokeLater(() -> {
                    App app;
                    if (query == null || query.isBlank()) {
                        app = new App(!isNoAutoPlay, trayIcon.get());
                    } else {
                        app = new App(query, trayIcon.get());
                    }
                    app.setVisible(true);
                });
                break;
            case DOWNLOAD:
                if (query == null || query.isBlank()) {
                    ArgSpec querySpec = null;
                    for (ArgSpec argSpec : spec.args()) {
                        if (argSpec.paramLabel().equals("Query")) {
                            querySpec = argSpec;
                            break;
                        }
                    }
                    throw new MissingParameterException(
                        spec.commandLine(),
                        querySpec,
                        "Video URL are required for download mode."
                    );
                }
                VideoUtil.downloadVideo(query, true, trayIcon.get());
                break;
            case VLC:
                if (query == null || query.isBlank()) {
                    ArgSpec querySpec = null;
                    for (ArgSpec argSpec : spec.args()) {
                        if (argSpec.paramLabel().equals("Query")) {
                            querySpec = argSpec;
                            break;
                        }
                    }
                    throw new MissingParameterException(
                        spec.commandLine(),
                        querySpec,
                        "Video URL are required for open with VLC mode."
                    );
                }
                VideoUtil.openVLC(query, null);
                break;
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv6Addresses", "true");
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("picocli.ansi", "true");
        CommandLine cl = new CommandLine(new Main());
        cl.setCaseInsensitiveEnumValuesAllowed(true);
        int exitCode = cl.execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
