package com.needhamsoftware.securesrc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.crypto.NoSuchPaddingException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import com.needhamsoftware.securesrc.EncryptionException;
import com.needhamsoftware.securesrc.Persistor;
import com.needhamsoftware.securesrc.encrypt.Encryption;
import com.needhamsoftware.securesrc.encrypt.KeyWithSalt;
import com.needhamsoftware.securesrc.model.Application;
import com.needhamsoftware.securesrc.model.Context;
import com.needhamsoftware.securesrc.model.Login;

@SuppressWarnings("CallToPrintStackTrace")
public class TopFrame extends JFrame {
  private static final String DEFAULT_CIPHER_SPEC = "AES/GCM/NoPadding";
  private static final String USER_HOME = System.getProperty("user.home");
  private static final File USER_HOME_DIR;
  public static final String CIPHER_PROP = "com.needhamsoftware.securesrc.cipher";
  public static final int KEY_SIZE = 128;

  static {
    try {
      USER_HOME_DIR = new File(USER_HOME).getCanonicalFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  private static final File DEFAULT_SAVE_LOCATION = new File(USER_HOME, "ssim.dat");
  private File location = DEFAULT_SAVE_LOCATION;

  private final JMenuItem newApplicationPopupItem;
  private final JMenuItem newContextPopupItem;
  private JPanel topPanel;
  private JTree contextTree;
  private JScrollPane treeScroll;
  private JPanel loginDisplay;
  private JTextField loginName;
  private JTextArea loginDescription;
  private JTextField identity;
  private JTextField secret;
  private JButton addNewButton;
  private JButton updateButton;
  private JTextField contextName;
  private JTextArea contextDescription;
  private JTextField applicatioName;
  private JTextArea applicationDescription;
  private JPanel contextPanel;
  private JPanel applicationPanel;
  private final JPopupMenu treeContextMenu;
  private AddContextDialog addContextDialog;

  List<Context> contextList = new ArrayList<>();
  Login current;
  DefaultMutableTreeNode selected;
  Context newContext;
  Application newApplication;
  Login newLogin;
  private DefaultMutableTreeNode root;
  private AddApplicationDialog addApplicationDialog;
  private String outputCipher = DEFAULT_CIPHER_SPEC;
  Persistor persistor;
  KeyWithSalt masterPassword;


  public TopFrame(String title) throws HeadlessException {
    super(title);
    String property = System.getProperty(CIPHER_PROP);
    if (property != null && !property.isEmpty()) {
      outputCipher = property;
    }
    try {
      persistor = new Persistor(location, outputCipher);
    } catch (EncryptionException e) {
      JOptionPane.showMessageDialog(this,e.getMessage() + " You can try an alternative Cipher by passing in -D"+CIPHER_PROP+"='<cipher>' argument. The program will now shut down safely.");
      throw new RuntimeException(e);
    }
    $$$setupUI$$$();
    Border border = BorderFactory.createLineBorder(Color.GRAY);
    loginDescription.setBorder(border);
    syncState();
    selected = root;
    contextTree.setSelectionPath(new TreePath(root.getPath()));
    treeContextMenu = new JPopupMenu();
    newContextPopupItem = new JMenuItem("New Context");
    newContextPopupItem.addActionListener(e -> addContext());
    newApplicationPopupItem = new JMenuItem("New Application");
    newApplicationPopupItem.addActionListener(e -> addApplication());
    treeContextMenu.add(newContextPopupItem);
    treeContextMenu.add(newApplicationPopupItem);
    treeContextMenu.setVisible(false);
    contextTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        TreePath selectionPath = contextTree.getSelectionPath();
        if (selectionPath != null) {
          selected = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
          if (selected != null) {
            Object userObject = selected.getUserObject();
            if (userObject instanceof Context context) {
              updateContextPanel(context);
              clearApplicationPanel();
            }
            if (userObject instanceof Application application) {
              updateApplicationPanel(application);
              updateContextPanel((Context) ((DefaultMutableTreeNode) selected.getParent()).getUserObject());
            }
          }
        }
      }
    });

    contextTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int rowForLocation = contextTree.getRowForLocation(e.getX(), e.getY());
        contextTree.setSelectionPath(contextTree.getPathForRow(rowForLocation));
        if (e.isPopupTrigger()) {
          contextToggle(e);
        }
      }

      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          contextToggle(e);
        }
      }
    });

    addNewButton.addActionListener(e -> {
      String name = loginName.getText();
      String description = loginDescription.getText();
      String identity = TopFrame.this.identity.getText();
      String secret = TopFrame.this.secret.getText();
      newLogin = new Login(true,name,description, Instant.now(),identity,secret,"","",null,"",null);
      current = newLogin;
      TreePath selectionPath = contextTree.getSelectionPath();
      if (selectionPath != null) {
        selected = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (selected != null) {
          Application app = (Application) selected.getUserObject();
          app.getLogins().add(newLogin);
          if (masterPassword == null) {
            JPasswordField pf = new JPasswordField();
            int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (okCxl == JOptionPane.OK_OPTION) {
              try {
                masterPassword = Encryption.getKey("AES", KEY_SIZE, pf.getPassword());
              } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                JOptionPane.showMessageDialog(this,"Unable to has password for " + outputCipher+"\n" +
                    ex.getClass() + " Exception message:" + ex.getMessage());
                ex.printStackTrace();
              }
            }
          }
          try {
            persistor.write(contextList,masterPassword);
            syncState();
            buildTree(true);
          } catch (IOException | InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException |
                   InvalidKeyException ex) {
            JOptionPane.showMessageDialog(this,"Unable to save data using " + outputCipher+"\n" +
                ex.getClass() + " Exception message:" + ex.getMessage());
            ex.printStackTrace();
          }
        }
      }

    });

    topPanel.setVisible(true);
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.add($$$getRootComponent$$$());
  }

  private void clearApplicationPanel() {
    this.applicatioName.setText("");
    this.applicationDescription.setText("");
    this.applicationPanel.setToolTipText("");
    clearLoginPanel();
    enableLoginPanel(false);
  }

  private void clearLoginPanel() {
    loginName.setText("");
    loginDescription.setText("");
    identity.setText("");
    secret.setText("");
  }

  private void enableLoginPanel(boolean enabled) {
    loginName.setEnabled(enabled);
    loginDescription.setEnabled(enabled);
    identity.setEnabled(enabled);
    secret.setEnabled(enabled);
    addNewButton.setEnabled(enabled);
  }

  private void updateApplicationPanel(Application context) {
    applicatioName.setText(context.getName());
    applicationDescription.setText(context.getDescription());
    applicationPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
    clearLoginPanel();
    enableLoginPanel(true);
  }

  private void updateContextPanel(Context context) {
    contextName.setText(context.getName());
    contextDescription.setText(context.getDescription());
    contextPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
  }

  private void addApplication() {
    if (addApplicationDialog == null) {
      addApplicationDialog = new AddApplicationDialog(this);
    }
    Context userObject = (Context) selected.getUserObject();
    addApplicationDialog.setApplications(userObject.getApplications());
    addApplicationDialog.setVisible(true);
    SwingUtilities.invokeLater(() -> buildTree(true));
  }

  private void addContext() {
    if (addContextDialog == null) {
      addContextDialog = new AddContextDialog(this);
    }
    addContextDialog.setContexts(contextList);
    addContextDialog.setVisible(true);
    SwingUtilities.invokeLater(() -> buildTree(true));
  }

  private void contextToggle(MouseEvent e) {
    if (selected == root) {
      newContextPopupItem.setVisible(true);
      newApplicationPopupItem.setVisible(false);
    } else if (selected != null && selected.getUserObject() instanceof Context) {
      newApplicationPopupItem.setVisible(true);
      newContextPopupItem.setVisible(false);
    } else {
      e.consume();
      return;
    }
    if (treeContextMenu.isVisible()) {
      treeContextMenu.setVisible(false);
    } else {
      treeContextMenu.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  /**
   * Rebuild the tree maintaining selection and expansion state if possible
   *
   * @param activeOnly if false, show inactive (updated) logins
   * @return the root node.
   */
  private DefaultMutableTreeNode buildTree(boolean activeOnly) {
    if (root == null) {
      root = new DefaultMutableTreeNode("Contexts");
      return root;
    }
    List<Object> expanded = new ArrayList<>();
    DefaultTreeModel model = null;
    Enumeration<TreePath> expandedDescendants = contextTree.getExpandedDescendants(new TreePath(root.getPath()));
    while (expandedDescendants != null && expandedDescendants.hasMoreElements()) {
      TreePath treePath = expandedDescendants.nextElement();
      DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
      expanded.add(lastPathComponent.getUserObject());
    }
    model = (DefaultTreeModel) contextTree.getModel();
    root.removeAllChildren();
    boolean selectedIsShown = false;
    for (Context context : contextList) {
      DefaultMutableTreeNode contextChild = new DefaultMutableTreeNode(context);
      model.insertNodeInto(contextChild, root, root.getChildCount());
      if (context == selected.getUserObject()) {
        contextTree.setSelectionPath(new TreePath(contextChild.getPath()));
        selected = contextChild;
        selectedIsShown = true;
      }
      for (Application application : context.getApplications()) {
        DefaultMutableTreeNode applicationChild = new DefaultMutableTreeNode(application);
        model.insertNodeInto(applicationChild, contextChild, contextChild.getChildCount());
        if (application == selected.getUserObject()) {
          contextTree.setSelectionPath(new TreePath(applicationChild.getPath()));
          selected = applicationChild;
          selectedIsShown = true;
        }
        List<Login> logins = activeOnly ? application.getActiveLogins() : application.getLogins();
        for (Login login : logins) {
          DefaultMutableTreeNode loginChild = new DefaultMutableTreeNode(login);
          model.insertNodeInto(loginChild, applicationChild, applicationChild.getChildCount());
          if (login == selected.getUserObject()) {
            contextTree.setSelectionPath(new TreePath(loginChild.getPath()));
            selected = loginChild;
            selectedIsShown = true;
          }

          //todo: update login form
        }
      }
    }
    if (!selectedIsShown) {
      contextTree.setSelectionPath(new TreePath(root.getPath()));
    }


    model.reload();

    forEachNode(root, c -> {
      TreePath pathForRow = new TreePath(c.getPath());
      if (newContext != null && newContext == ((DefaultMutableTreeNode) pathForRow.getLastPathComponent()).getUserObject()) {
        contextTree.setSelectionPath(pathForRow);
        newContext = null;
      }
      if (newApplication != null && newApplication == ((DefaultMutableTreeNode) pathForRow.getLastPathComponent()).getUserObject()) {
        contextTree.setSelectionPath(pathForRow);
        newApplication = null;
      }
    });

    // Note: despite no concrete documentation I could find, the selection setting above will undo
    // th expansions we are setting so this must come AFTER selection is set.
    forEachNode(root, c -> {
      if (expanded.contains(c.getUserObject())) {
        contextTree.expandPath(new TreePath(c.getPath()));
      }
    });
    return root;
  }

  private static void forEachNode(DefaultMutableTreeNode node, Consumer<DefaultMutableTreeNode> consumer) {
    Enumeration<TreeNode> children = node.children();
    while (children.hasMoreElements()) {
      TreeNode treeNode = children.nextElement();
      forEachNode((DefaultMutableTreeNode) treeNode, consumer);
    }
    consumer.accept(node);
  }

  private void syncState() {
    updateButton.setEnabled(current != null);
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout(0, 0));
    topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, topPanel.getFont()), null));
    final JLabel label1 = new JLabel();
    Font label1Font = this.$$$getFont$$$("Chandas", -1, 20, label1.getFont());
    if (label1Font != null) label1.setFont(label1Font);
    label1.setHorizontalAlignment(0);
    label1.setText("SecureSrc Identity Manager");
    topPanel.add(label1, BorderLayout.NORTH);
    final JSplitPane splitPane1 = new JSplitPane();
    topPanel.add(splitPane1, BorderLayout.EAST);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new BorderLayout(0, 0));
    splitPane1.setLeftComponent(panel1);
    treeScroll = new JScrollPane();
    treeScroll.setPreferredSize(new Dimension(200, 500));
    panel1.add(treeScroll, BorderLayout.CENTER);
    treeScroll.setViewportView(contextTree);
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridBagLayout());
    splitPane1.setRightComponent(panel2);
    loginDisplay = new JPanel();
    loginDisplay.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel2.add(loginDisplay, gbc);
    loginDisplay.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Login Info", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    final JLabel label2 = new JLabel();
    label2.setText("Name");
    label2.setToolTipText("The name for this login (not necessarily the userId)");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label2, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 9;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer2, gbc);
    loginName = new JTextField();
    loginName.setColumns(60);
    loginName.setEnabled(false);
    loginName.setToolTipText("The name for this login (not necessarily the userId)");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(loginName, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 9;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 9;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(spacer4, gbc);
    final JLabel label3 = new JLabel();
    label3.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label3, gbc);
    loginDescription = new JTextArea();
    loginDescription.setColumns(60);
    loginDescription.setEnabled(false);
    loginDescription.setLineWrap(true);
    loginDescription.setRows(3);
    loginDescription.setWrapStyleWord(true);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.BOTH;
    loginDisplay.add(loginDescription, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer5, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer6, gbc);
    final JLabel label4 = new JLabel();
    label4.setText("Identity");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label4, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer7, gbc);
    final JLabel label5 = new JLabel();
    label5.setText("Secret");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label5, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 7;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer8, gbc);
    identity = new JTextField();
    identity.setColumns(60);
    identity.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(identity, gbc);
    secret = new JTextField();
    secret.setColumns(60);
    secret.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(secret, gbc);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.BOTH;
    loginDisplay.add(panel3, gbc);
    addNewButton = new JButton();
    addNewButton.setEnabled(false);
    addNewButton.setText("Add New");
    panel3.add(addNewButton);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    panel3.add(panel4);
    updateButton = new JButton();
    updateButton.setEnabled(false);
    updateButton.setText("Update");
    panel3.add(updateButton);
    contextPanel = new JPanel();
    contextPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel2.add(contextPanel, gbc);
    contextPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Context", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, contextPanel.getFont()), null));
    final JLabel label6 = new JLabel();
    label6.setText("Name");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    contextPanel.add(label6, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    contextPanel.add(spacer10, gbc);
    contextName = new JTextField();
    contextName.setColumns(48);
    contextName.setText("");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(contextName, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer11, gbc);
    final JPanel spacer12 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer12, gbc);
    final JLabel label7 = new JLabel();
    label7.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    contextPanel.add(label7, gbc);
    final JPanel spacer13 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.VERTICAL;
    contextPanel.add(spacer13, gbc);
    contextDescription = new JTextArea();
    contextDescription.setColumns(48);
    contextDescription.setLineWrap(true);
    contextDescription.setRows(3);
    contextDescription.setText("");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.BOTH;
    contextPanel.add(contextDescription, gbc);
    applicationPanel = new JPanel();
    applicationPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.BOTH;
    panel2.add(applicationPanel, gbc);
    applicationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Application", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    final JLabel label8 = new JLabel();
    label8.setText("Name");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    applicationPanel.add(label8, gbc);
    final JPanel spacer14 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer14, gbc);
    final JPanel spacer15 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    applicationPanel.add(spacer15, gbc);
    applicatioName = new JTextField();
    applicatioName.setColumns(48);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(applicatioName, gbc);
    final JPanel spacer16 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer16, gbc);
    final JPanel spacer17 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer17, gbc);
    final JLabel label9 = new JLabel();
    label9.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    applicationPanel.add(label9, gbc);
    final JPanel spacer18 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer18, gbc);
    applicationDescription = new JTextArea();
    applicationDescription.setColumns(48);
    applicationDescription.setLineWrap(true);
    applicationDescription.setRows(3);
    applicationDescription.setText("");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.BOTH;
    applicationPanel.add(applicationDescription, gbc);
    label1.setLabelFor(treeScroll);
  }

  /**
   * @noinspection ALL
   */
  private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
    if (currentFont == null) return null;
    String resultName;
    if (fontName == null) {
      resultName = currentFont.getName();
    } else {
      Font testFont = new Font(fontName, Font.PLAIN, 10);
      if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
        resultName = fontName;
      } else {
        resultName = currentFont.getName();
      }
    }
    Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
    Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
    return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return topPanel;
  }

  private void createUIComponents() {
    contextTree = new JTree(buildTree(true));
    // TODO: place custom component creation code here
  }
}
