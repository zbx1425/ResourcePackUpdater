package cn.zbx1425.resourcepackupdater;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class IPCHostEntryPoint {

    private static final JFrame frame = new JFrame("Resource pack update progress");
    private static final JLabel[] labels = new JLabel[3];

    public static void main(String[] args) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Border emptyBorder = new EmptyBorder(10, 20, 20, 10);
        labels[0] = new JLabel("如Minecraft主界面已出本窗口仍未关闭，那么直接关掉即可");
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
            new TcpClient(Integer.parseInt(args[0])).start();
        } catch (Exception ex) {
            frame.dispose();
        }
    }

    public static class TcpClient extends Thread {

        public boolean running;
        private final Socket socket;

        public TcpClient(int port) throws IOException {
            socket = new Socket(InetAddress.getLocalHost(), port);
        }

        public void run() {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                running = true;

                int i = 0;

                while (running) {
                    String received = reader.readLine().trim();
                    // System.out.println(received);
                    if (received.equals("end")) {
                        running = false;
                        try {
                            Thread.sleep(3000);
                        } catch (Exception ignored) {

                        }
                        frame.dispose();
                    } else {
                        final int labelIndex = i;
                        SwingUtilities.invokeLater(() -> {
                            labels[labelIndex].setText(received);
                        });
                        ++i;
                        if (i >= 3) i = 0;
                    }
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    labels[0].setText(e.getMessage());
                    labels[1].setText(e.getMessage());
                    labels[2].setText(e.getMessage());
                });
                running = false;
                try {
                    Thread.sleep(3000);
                } catch (Exception ignored) {

                }
                frame.dispose();
            }
        }
    }
}
