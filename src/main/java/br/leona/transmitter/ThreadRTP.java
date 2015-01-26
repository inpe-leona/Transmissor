package br.leona.transmitter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.VideoFormat;
import javax.swing.SwingWorker;

public class ThreadRTP extends SwingWorker<Void, Void> {
    private boolean message = false;
    private String hostAddress = null;
    private Transmitter transmissor;
    private boolean connect = false;    
    private MediaLocator mediaLocator;
    private int port;

    public ThreadRTP(int port) {
        super();	
        this.port = port;             
        mediaLocator = new MediaLocator("vfw://0");  
    }

    @Override
    protected Void doInBackground() {
        
           
           
        while (!isCancelled()) {
            if (!message) {
                message = !message;
                System.err.println("Esperando conexão de um cliente...");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (hostAddress == null) {
                try {
                    ServerSocket hostServer = new ServerSocket(port);
                    Socket socket = hostServer.accept();
                    if (socket.getInetAddress() != null
                    && !socket.getInetAddress().equals("")) {
                        hostAddress = socket.getInetAddress().getHostAddress();
                        connect = true;
                    }
                } catch (IOException e) {
                }
            }
            if (connect) {
                connect = false;
                transmissor = new Transmitter(mediaLocator,
                                              hostAddress, 1235, 
                                              new Format(VideoFormat.JPEG));
            //1235 é a port de transmissao RTP, tem que ser diferente da TCP
            //aqui estamos especificando por qual port sera o envio 
            //das informacoes de video
                String result = transmissor.start();
                if (result != null) {
                    System.err.println("Error : " + result);
                }
                System.err.println("Transmissão RTP iniciada para " + hostAddress);
            }
        }
        if (transmissor != null) {
                transmissor.stop();
        }
        System.err.println("Servidor desligado.");
        return null;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String ipCliente) {
        this.hostAddress = ipCliente;
    }

}