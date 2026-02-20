package com.needhamsoftware.securesrc.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import com.needhamsoftware.securesrc.model.Login;

public class LoginTreeCellRenderer extends DefaultTreeCellRenderer {
  Font original = null;

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
    super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
    if (original == null) {
      original = getFont();
    }
    setFont(original);

    Object node = ((DefaultMutableTreeNode) value).getUserObject();

    if (node instanceof Login login) {
      Font origFont = getFont();
      if (!login.isActive()) {
        setForeground(new Color(116, 105, 161));
        setFont(origFont.deriveFont(Font.ITALIC));
      } else {
        setFont(origFont.deriveFont(Font.BOLD));
      }
    }

    return this;
  }
}