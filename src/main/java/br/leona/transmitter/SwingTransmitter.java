package br.leona.transmitter;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SwingTransmitter extends JFrame {
    //public static Transmitter transmissor = null;
    private ThreadRTP threadRTP;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SwingTransmitter();
            }
        });
    }

    public SwingTransmitter() {
        setTitle("Transmissor");
        JPanel panelServer = new JPanel();
        panelServer.setLayout(new BoxLayout(panelServer, BoxLayout.LINE_AXIS));
        JLabel namePort = new JLabel("Porta para conex�o TCP: ");
        final JTextField port = new JTextField("1235");
        panelServer.add(namePort);
        panelServer.add(port);
        JPanel panelButton = new JPanel();
        panelButton.setLayout(new BoxLayout(panelButton, BoxLayout.LINE_AXIS));
        
        final JButton turnOn = new JButton("Ligar");
        turnOn.setEnabled(true);
        final JButton turnOff = new JButton("Desligar");
        turnOff.setEnabled(false);
        turnOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                turnOn.setEnabled(false);
                turnOff.setEnabled(true);
        // escolhida a port do socket para a conexao TCP, 
                (threadRTP = new ThreadRTP(Integer.parseInt(port.getText()))).execute();
        //pode ser qualquer uma, eu tinha escolhido 1234 ao acaso.
        // esse numero especifica em qual port o servidor vai estar escutando
        //pedidos de conexao tcp de clientes.
            }
        });
        turnOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                turnOn.setEnabled(true);
                turnOff.setEnabled(false);
                threadRTP.cancel(true);
                threadRTP = null;
            }
        });        
        
        panelButton.add(turnOn);
        panelButton.add(turnOff);
        getContentPane().add(panelServer, BorderLayout.PAGE_START);
        getContentPane().add(panelButton, BorderLayout.PAGE_END);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 300);
        setVisible(true);
    }

}
