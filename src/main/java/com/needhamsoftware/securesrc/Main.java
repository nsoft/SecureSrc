package com.needhamsoftware.securesrc;

import javax.swing.SwingUtilities;
import com.needhamsoftware.securesrc.ui.TopFrame;

public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        TopFrame topFrame = new TopFrame("SecureSrc");
        topFrame.pack();
        topFrame.setVisible(true);

      }
    });

  }
}