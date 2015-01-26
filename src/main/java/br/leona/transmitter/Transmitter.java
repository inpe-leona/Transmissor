
package br.leona.transmitter;

import java.awt.Dimension;
import java.io.IOException;
import java.net.InetAddress;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;

public class Transmitter {
    private MediaLocator locator;
    private String hostAddress;
    private int port;
    private Processor processor;
    private RTPManager[] messageRTP;
    private DataSource dataSource;

    public Transmitter(MediaLocator locator, 
                       String hostAddress, int port, Format format) {
        this.locator = locator;
        this.hostAddress = hostAddress;
        this.port = port;
    }

    public synchronized String start() {
        String result;
        result = createProcessor();
        if (result != null)
            return result;
        result = createTransmitter();
        if (result != null) {
            processor.close();
            processor = null;
            return result;
        }
        processor.start();
        return null;
    }

    public void stop() {
        synchronized (this) {
            if (processor != null) {
                processor.stop();
                processor.close();
                processor = null;
                for (int i = 0; i < messageRTP.length; i++) {
                    messageRTP[i].removeTargets("Sessao terminada.");
                    messageRTP[i].dispose();
                }
            }
        }
    }

    private String createProcessor() {
        if (locator == null) {
            return "Locator null";
        }
        
        try {
            dataSource = javax.media.Manager.createDataSource(locator);
        } catch (Exception e) {
            return "Couldn't create DataSource";
        }
        
        try {
            processor = javax.media.Manager.createProcessor(dataSource);
        } catch (NoProcessorException npe) {
            return "Couldn't create processor";
        } catch (IOException ioe) {
            return "IOException creating processor";
        }
        boolean result = waitForState(processor, Processor.Configured);
        if (result == false)
            return "Couldn't configure processor";
        TrackControl[] tracks = processor.getTrackControls();
        if (tracks == null || tracks.length < 1)
            return "Couldn't find tracks in processor";
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);
        Format supported[];
        Format chosen;
        boolean atLeastOneTrack = false;
        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();
            if (tracks[i].isEnabled()) {
                supported = tracks[i].getSupportedFormats();
                if (supported.length > 0) {
                    if (supported[0] instanceof VideoFormat) {
                        chosen = checkForVideoSizes(tracks[i].getFormat(),
                                                    supported[0]);
                    } else
                        chosen = supported[0];
                    tracks[i].setFormat(chosen);
                    System.err.println("Track " + i + " is set to transmit as:");
                    System.err.println("  " + chosen);
                    atLeastOneTrack = true;
                } else
                    tracks[i].setEnabled(false);
            } else
                tracks[i].setEnabled(false);
        }
        if (!atLeastOneTrack)
                return "Couldn't set any of the tracks to a valid RTP format";
        result = waitForState(processor, Controller.Realized);
        if (result == false)
                return "Couldn't realize processor";
        setJPEGQuality(processor, 0.5f);
        
        dataSource = processor.getDataOutput();
        return null;
    }

    private String createTransmitter() {
        PushBufferDataSource pbds = (PushBufferDataSource) dataSource;
        PushBufferStream pbss[] = pbds.getStreams();
        messageRTP = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        InetAddress ipAddr;
        SendStream sendStream;
        int port;
        SourceDescription srcDesList[];
        for (int i = 0; i < pbss.length; i++) {
            try {
                messageRTP[i] = RTPManager.newInstance();
                port = this.port + 2 * i;
                ipAddr = InetAddress.getByName(hostAddress);
                localAddr = new SessionAddress(InetAddress.getLocalHost(), port);
                destAddr = new SessionAddress(ipAddr, port);
                messageRTP[i].initialize(localAddr);
                messageRTP[i].addTarget(destAddr);
                System.err.println("Created RTP session: " + hostAddress + " "
                                  + port);
                sendStream = messageRTP[i].createSendStream(dataSource, i);
                sendStream.start();
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return null;
    }

    Format checkForVideoSizes(Format original, Format supported) {
        int width, height;
        Dimension size = ((VideoFormat) original).getSize();
        Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
        Format h263Fmt = new Format(VideoFormat.H263_RTP);

        if (supported.matches(jpegFmt)) {
            width = (size.width % 8 == 0 ? size.width
                    : (int) (size.width / 8) * 8);
            height = (size.height % 8 == 0 ? size.height
                    : (int) (size.height / 8) * 8);
        } else if (supported.matches(h263Fmt)) {
            if (size.width < 128) {
                width = 128;
                height = 96;
            } else if (size.width < 176) {
                width = 176;
                height = 144;
            } else {
                width = 352;
                height = 288;
            }
        } else {
            return supported;
        }

        return (new VideoFormat(null, new Dimension(width, height),
                Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED)).intersects(supported);
    }

    void setJPEGQuality(Player p, float val) {
        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat formato = new VideoFormat(VideoFormat.JPEG);
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
                Object objeto = ((Owned) cs[i]).getOwner();
                if (objeto instanceof Codec) {
                    Format fmts[] = ((Codec) objeto).getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                        if (fmts[j].matches(formato)) {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
                            System.err.println("- Qualidade configurada para " + val
                                              + " em " + qc);
                            break;
                        }
                    }
                }
                if (qc != null)
                    break;
            }
        }
    }

    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }

    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        failed = false;
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }
        if (failed)
            return false;
        else
            return true;
    }

    class StateListener implements ControllerListener {
        public void controllerUpdate(ControllerEvent ce) {
            if (ce instanceof ControllerClosedEvent)
                setFailed();
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }
}