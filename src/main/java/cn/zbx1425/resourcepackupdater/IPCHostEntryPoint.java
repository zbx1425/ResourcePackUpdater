package cn.zbx1425.resourcepackupdater;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class IPCHostEntryPoint {

    private static final JFrame frame = new JFrame("Resource pack update progress");
    private static final JLabel[] labels = new JLabel[3];

    public static void main(String[] args) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Border emptyBorder = new EmptyBorder(10, 20, 20, 10);
        labels[0] = new JLabel("等待进程间通讯UDP连接");
        labels[1] = new JLabel("Awaiting for IPC UDP connection");
        labels[2] = new JLabel("……");
        for (JLabel label : labels) label.setBorder(emptyBorder);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.getContentPane().add(labels[0]);
        frame.getContentPane().add(labels[1]);
        frame.getContentPane().add(labels[2]);
        frame.setMinimumSize(new Dimension(400, 180));
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);

        try {
            new UdpServer(Integer.parseInt(args[0])).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class UdpServer extends Thread {

        public boolean running;
        private final DatagramSocket socket;
        private final byte[] buf = new byte[1024];

        public UdpServer(int port) throws SocketException {
            socket = new DatagramSocket(new InetSocketAddress("127.0.0.1", port));
        }

        public void run() {
            running = true;

            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    Arrays.fill(buf, (byte) 0);
                    socket.receive(packet);
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    final String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                    System.out.println(received);
                    if (received.equals("end")) {
                        running = false;
                        try {
                            Thread.sleep(3000);
                        } catch (Exception ignored) {

                        }
                        frame.dispose();
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            String[] tokens = received.split("\n");
                            for (int i = 0; i < 3; ++i) {
                                labels[i].setText(tokens[i]);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            socket.close();
        }
    }
}
