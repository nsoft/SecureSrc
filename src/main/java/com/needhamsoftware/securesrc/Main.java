package com.needhamsoftware.securesrc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import com.needhamsoftware.securesrc.ui.TopFrame;

public class Main {
  public static void main(String[] args) {
    String logConfig = ".level=" + Level.OFF + '\n';
    logConfig += "handlers=java.util.logging.ConsoleHandler\n";
    logConfig += "java.util.logging.ConsoleHandler" + ".level=" + Level.OFF + '\n';
    logConfig += "org.apache.lucene" + ".level=" + Level.OFF + "\n";
    try {
      java.util.logging.LogManager.getLogManager().readConfiguration(new java.io.ByteArrayInputStream(logConfig.getBytes(StandardCharsets.UTF_8)));
    } catch (IOException ioe) {
      System.err.println("cannot fully configure logging");
      ioe.printStackTrace();
    }
    SwingUtilities.invokeLater(() -> {
      TopFrame topFrame = new TopFrame("SecureSrc");
      topFrame.pack();
      topFrame.setLocationRelativeTo(null);
      topFrame.setVisible(true);
    });
  }
}