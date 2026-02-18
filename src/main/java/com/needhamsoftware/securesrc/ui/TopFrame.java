package com.needhamsoftware.securesrc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.crypto.AEADBadTagException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import javax.swing.text.html.HTMLEditorKit;
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
import com.needhamsoftware.securesrc.model.NamedObject;
import com.needhamsoftware.securesrc.search.LuceneSearch;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

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

  private static final File DEFAULT_SAVE_LOCATION = new File(USER_HOME_DIR, "ssim.dat");
  private final LuceneSearch searcher;
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
  private JButton addNewLoginButton;
  private JButton updateLoginButton;
  private JTextField contextName;
  private JTextArea contextDescription;
  private JTextField applicatioName;
  private JTextArea applicationDescription;
  private JPanel contextPanel;
  private JPanel applicationPanel;
  private JPanel searchPanel;
  private JTextField query;
  private JButton searchButton;
  private JEditorPane searchResults;
  private JButton addNewContextButton;
  private JButton updateContextButton;
  private JButton addNewApplicationButton;
  private JButton updateApplicationButton;
  private final JPopupMenu treeContextMenu;
  private AddContextDialog addContextDialog;

  private JMenuBar menubar;
  private JMenu viewMenu;
  private JCheckBoxMenuItem viewHistoryMenuItem;

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
  private boolean showHistory;

  public TopFrame(String title) throws HeadlessException {
    super(title);
    String property = System.getProperty(CIPHER_PROP);
    if (property != null && !property.isEmpty()) {
      outputCipher = property;
    }
    try {
      persistor = new Persistor(location, outputCipher);
    } catch (EncryptionException e) {
      JOptionPane.showMessageDialog(this, e.getMessage() + " You can try an alternative Cipher by passing in -D" + CIPHER_PROP + "='<cipher>' argument. The program will now shut down safely.");
      throw new RuntimeException(e);
    }
    if (location.exists()) {
      try {
        contextList = persistor.readEncryptedStorage(s -> {
          KeyWithSalt keyWithSalt = null;
          while (keyWithSalt == null) {
            // if this is failing we're doomed
            keyWithSalt = askPassword(s);
          }
          masterPassword = keyWithSalt;
          return masterPassword;
        });
      } catch (NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException |
               ClassNotFoundException | InvalidAlgorithmParameterException e) {
        // fatal - just die
        e.printStackTrace();
        System.exit(1);
      } catch (IOException e) {
        // Message and Die (jvm restart and message dialog vastly slows down UI script driven brute-forcing)
        if (e.getCause() instanceof AEADBadTagException) {
          JOptionPane.showMessageDialog(this,"Password does not match!");
        }
        System.exit(1);
      }
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
    addNewContextButton.addActionListener(e -> addContext());
    updateContextButton.addActionListener(e -> updateContext());
    newApplicationPopupItem = new JMenuItem("New Application");
    newApplicationPopupItem.addActionListener(e -> addApplication());
    addNewApplicationButton.addActionListener(e -> addApplication());
    updateApplicationButton.addActionListener(e -> updateApplication());
    addNewLoginButton.addActionListener(e -> createLogin(loginFromFormFields()));
    updateLoginButton.addActionListener(e -> updateLogin());
    treeContextMenu.add(newContextPopupItem);
    treeContextMenu.add(newApplicationPopupItem);
    treeContextMenu.setVisible(false);
    contextTree.addTreeSelectionListener(e -> {
      TreePath selectionPath = contextTree.getSelectionPath();
      if (selectionPath != null) {
        selected = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (selected != null) {
          Object userObject = selected.getUserObject();
          if (userObject instanceof Context context) {
            updateContextPanel(context);
          }
          if (userObject instanceof Application application) {
            DefaultMutableTreeNode context = (DefaultMutableTreeNode) selected.getParent();
            updateContextPanel((Context) context.getUserObject());
            updateApplicationPanel(application);
          }
          if (userObject instanceof Login login) {
            DefaultMutableTreeNode application = (DefaultMutableTreeNode) selected.getParent();
            DefaultMutableTreeNode context = (DefaultMutableTreeNode) application.getParent();
            updateContextPanel((Context) context.getUserObject());
            updateApplicationPanel((Application) application.getUserObject());
            updateLoginPanel(login);
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


    this.searcher = new LuceneSearch();

    searchButton.addActionListener(e -> {
      search();
    });
    searchResults.addHyperlinkListener(e -> {
      HyperlinkEvent.EventType eventType = e.getEventType();
      if (eventType.equals(HyperlinkEvent.EventType.ACTIVATED)) {
        URL url = e.getURL();
        String uuid = url.getHost();
        DefaultMutableTreeNode inTree = findInTree(uuid, root);
        contextTree.setSelectionPath(new TreePath(inTree.getPath()));
      }
    });
    searchResults.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        searchResults.getCaret().setVisible(false);
      }
    });
    HTMLEditorKit editorKit = (HTMLEditorKit) searchResults.getEditorKit();
    editorKit.getStyleSheet().addRule("li {list-style-type:none;padding:0px;margin:10px;margin-bottom:0px}");
    editorKit.getStyleSheet().addRule("ul {padding:0px;margin:10px;}");
    Font font = UIManager.getFont("Label.font");
    editorKit.getStyleSheet().addRule("body { font-family: " + font.getFamily() + "; " +
        "font-size: " + font.getSize() + "pt; }");
    editorKit.getStyleSheet().addRule("em {color:#6666ff}");
    topPanel.setVisible(true);
    menuBar();
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.add($$$getRootComponent$$$());
    syncState();
    buildTree();
    query.addActionListener(e -> search());
  }

  private void menuBar() {
    this.menubar = new JMenuBar();
    this.viewMenu = new JMenu("View");
    this.viewHistoryMenuItem = new JCheckBoxMenuItem("View History");
    this.viewHistoryMenuItem.addActionListener(e -> {
      showHistory = viewHistoryMenuItem.isSelected();
      buildTree();
      if (query.getText() != null && !query.getText().isEmpty()) {
        search();
      }
    });
    this.viewMenu.add(viewHistoryMenuItem);
    this.menubar.add(viewMenu);
    this.setJMenuBar(menubar);
  }

  private void search() {
    try {
      List<Document> search = searcher.search(query.getText());
      // build html for our JEditorPane to display
      StringBuilder html = new StringBuilder("<html><head></head><body><ul>");
      for (Document document : search) {
        html.append("<li><a href=\"http://");
        html.append(escapeHtml(document.get("id")));
        html.append("\"><strong>");
        html.append(escapeHtml(document.get("name")));
        html.append("</strong></a><br/>");
        html.append("<em>");
        html.append(escapeHtml(document.get("breadcrumb")));
        html.append("</em><br/>");
        html.append(escapeHtml(document.get("description")));
        html.append("<hr/></li>");
      }
      html.append("</body></html>");
      searchResults.setText(html.toString());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage());
    }
  }

  private void updateContext() {
    Context userObject = (Context) selected.getUserObject();
    userObject.setName(contextName.getText());
    userObject.setDescription(contextDescription.getText());
    persist();
  }

  private void updateApplication() {
    Application userObject = (Application) selected.getUserObject();
    userObject.setName(applicatioName.getText());
    userObject.setDescription(applicationDescription.getText());
    persist();
  }

  private void updateLogin() {
    Login newLogin1 = loginFromFormFields();
    Login userObject = (Login) selected.getUserObject();
    userObject.inActivate();
    contextTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) selected.getParent()).getPath()));
    createLogin(newLogin1);
  }

  private Login loginFromFormFields() {
    String name = loginName.getText();
    String description = loginDescription.getText();
    String identity = TopFrame.this.identity.getText();
    String secret = TopFrame.this.secret.getText();
    return new Login(true, name, description, Instant.now(), identity, secret,
        "", "", null, "", null);
  }

  private void createLogin(Login newLogin1) {

    newLogin = newLogin1;
    current = newLogin;
    TreePath selectionPath = contextTree.getSelectionPath();
    if (selectionPath != null) {
      selected = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
      if (selected != null) {
        Application app = (Application) selected.getUserObject();
        app.getLogins().add(newLogin);
        if (masterPassword == null) {
          // This should only happen if there was no data file to load.
          masterPassword = askPassword(null);
        }
        if (masterPassword == null) {
          // we failed don't try to write anything.
          return;
        }
        persist();
      }
    }
  }

  private void persist() {
    try {
      persistor.write(contextList, masterPassword);
      syncState();
      buildTree();
    } catch (IOException | InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException |
             InvalidKeyException | InvalidAlgorithmParameterException ex) {
      JOptionPane.showMessageDialog(this, "Unable to save data using " + outputCipher + "\n" +
          ex.getClass() + " Exception message:" + ex.getMessage());
      ex.printStackTrace();
    }
  }

  DefaultMutableTreeNode findInTree(String uuid, DefaultMutableTreeNode root) {
    if (uuid == null) {
      return null;
    }
    Object userObject = root.getUserObject();
    if (userObject instanceof NamedObject namedObject && uuid.equals(namedObject.getUuid())) {
      return root;
    }
    int childCount = root.getChildCount();
    for (int i = 0; i < childCount; i++) {
      DefaultMutableTreeNode inTree = findInTree(uuid, (DefaultMutableTreeNode) root.getChildAt(i));
      if (inTree != null) {
        return inTree;
      }
    }
    return null;
  }

  public static String escapeHtml(String html) {
    if (html == null) {
      return "null";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < html.length(); i++) {
      char ch = html.charAt(i);
      if (ch == '<') {
        sb.append("&lt;");
      } else if (ch == '>') {
        sb.append("&gt;");
      } else if (ch == '"') {
        sb.append("&quot;");
      } else if (ch == '&') {
        sb.append("&amp;");
      } else if (ch < ' ' || ch == '\'') {
        sb.append("&#").append((int) ch).append(';');
      } else {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private void updateLoginPanel(Login login) {
    loginName.setText(login.getName());
    loginDescription.setText(login.getDescription());
    identity.setText(login.getIdentity());
    secret.setText(login.getSecret());
    updateLoginButton.setEnabled(true);
  }

  private KeyWithSalt askPassword(byte[] salt) {
    JPasswordField pf = new JPasswordField();
    int okCxl = popUpPasswordDialog(pf);
    char[] password = pf.getPassword();
    if (okCxl == JOptionPane.OK_OPTION) {
      try {
        return Encryption.getKey("AES", KEY_SIZE, password, salt);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
        JOptionPane.showMessageDialog(this, "Unable to has password for " + outputCipher + "\n" +
            ex.getClass() + " Exception message:" + ex.getMessage());
        ex.printStackTrace();
      }
    }
    return null;
  }

  private static int popUpPasswordDialog(JPasswordField pf) {
    JOptionPane jOptionPane = new JOptionPane(pf,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION) {
      @Override
      public void selectInitialValue() {
        pf.requestFocusInWindow();
      }
    };
    JDialog dialog = jOptionPane.createDialog("Enter Password");
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
    Object value = jOptionPane.getValue();
    dialog.dispose();
    if (value == null) {
      value = JOptionPane.CLOSED_OPTION;
    }
    int okCxl = (Integer) value;
    return okCxl;
  }

  private void clearApplicationPanel() {
    this.applicatioName.setText("");
    this.applicationDescription.setText("");
    this.applicationPanel.setToolTipText("");
    clearLoginPanel();
    enableLoginPanel(false);
    updateApplicationButton.setEnabled(false);
  }

  private void clearLoginPanel() {
    loginName.setText("");
    loginDescription.setText("");
    identity.setText("");
    secret.setText("");
    updateLoginButton.setEnabled(false);
  }

  private void enableLoginPanel(boolean enabled) {
    loginName.setEnabled(enabled);
    loginDescription.setEnabled(enabled);
    identity.setEnabled(enabled);
    secret.setEnabled(enabled);
    addNewLoginButton.setEnabled(enabled);
  }

  private void updateApplicationPanel(Application context) {
    applicatioName.setText(context.getName());
    applicationDescription.setText(context.getDescription());
    applicationPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
    updateApplicationButton.setEnabled(true);
    addNewApplicationButton.setEnabled(false);
    clearLoginPanel();
    enableLoginPanel(true);
  }

  private void updateContextPanel(Context context) {
    contextName.setText(context.getName());
    contextDescription.setText(context.getDescription());
    contextPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
    updateContextButton.setEnabled(true);
    addNewContextButton.setEnabled(false);
    addNewApplicationButton.setEnabled(true);
    clearApplicationPanel();
  }

  private void addApplication() {
    if (addApplicationDialog == null) {
      addApplicationDialog = new AddApplicationDialog(this);
    }
    Context userObject = (Context) selected.getUserObject();
    addApplicationDialog.setApplications(userObject.getApplications());
    addApplicationDialog.setVisible(true);
    SwingUtilities.invokeLater(() -> buildTree());
  }

  private void addContext() {
    if (addContextDialog == null) {
      addContextDialog = new AddContextDialog(this);
    }
    addContextDialog.setContexts(contextList);
    addContextDialog.setVisible(true);
    SwingUtilities.invokeLater(() -> buildTree());
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
   * @return the root node.
   */
  private DefaultMutableTreeNode buildTree() {
    if (root == null) {
      root = new DefaultMutableTreeNode("Contexts");
      return root;
    }
    List<Object> expanded = new ArrayList<>();
    DefaultTreeModel model;
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
        List<Login> logins = showHistory ?  application.getLogins() : application.getActiveLogins();
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
    searcher.indexTreeModel(contextTree.getModel());
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
    updateLoginButton.setEnabled(current != null);
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
    final JLabel label1 = new JLabel();
    Font label1Font = this.$$$getFont$$$("Chandas", -1, 20, label1.getFont());
    if (label1Font != null) label1.setFont(label1Font);
    label1.setHorizontalAlignment(0);
    label1.setText("SecureSrc Identity Manager");
    topPanel.add(label1, BorderLayout.NORTH);
    final JSplitPane splitPane1 = new JSplitPane();
    topPanel.add(splitPane1, BorderLayout.CENTER);
    final JSplitPane splitPane2 = new JSplitPane();
    splitPane1.setLeftComponent(splitPane2);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new BorderLayout(0, 0));
    splitPane2.setLeftComponent(panel1);
    treeScroll = new JScrollPane();
    treeScroll.setPreferredSize(new Dimension(200, 500));
    panel1.add(treeScroll, BorderLayout.CENTER);
    treeScroll.setViewportView(contextTree);
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridBagLayout());
    splitPane2.setRightComponent(panel2);
    loginDisplay = new JPanel();
    loginDisplay.setLayout(new GridBagLayout());
    loginDisplay.setEnabled(true);
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
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
    gbc.weightx = 1.0;
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
    addNewLoginButton = new JButton();
    addNewLoginButton.setEnabled(false);
    addNewLoginButton.setText("Add New");
    panel3.add(addNewLoginButton);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    panel3.add(panel4);
    updateLoginButton = new JButton();
    updateLoginButton.setEnabled(false);
    updateLoginButton.setText("Update");
    panel3.add(updateLoginButton);
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
    addNewContextButton = new JButton();
    addNewContextButton.setText("Add New");
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(addNewContextButton, gbc);
    updateContextButton = new JButton();
    updateContextButton.setText("Update");
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(updateContextButton, gbc);
    final JPanel spacer14 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer14, gbc);
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
    final JPanel spacer15 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer15, gbc);
    final JPanel spacer16 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    applicationPanel.add(spacer16, gbc);
    applicatioName = new JTextField();
    applicatioName.setColumns(48);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(applicatioName, gbc);
    final JPanel spacer17 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer17, gbc);
    final JPanel spacer18 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer18, gbc);
    final JLabel label9 = new JLabel();
    label9.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    applicationPanel.add(label9, gbc);
    final JPanel spacer19 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer19, gbc);
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
    addNewApplicationButton = new JButton();
    addNewApplicationButton.setText("Add New");
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(addNewApplicationButton, gbc);
    updateApplicationButton = new JButton();
    updateApplicationButton.setText("Update");
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(updateApplicationButton, gbc);
    final JPanel spacer20 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer20, gbc);
    searchPanel = new JPanel();
    searchPanel.setLayout(new GridBagLayout());
    searchPanel.setAlignmentY(0.5f);
    splitPane1.setRightComponent(searchPanel);
    query = new JTextField();
    query.setColumns(30);
    query.setMinimumSize(new Dimension(100, 34));
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(query, gbc);
    final JPanel spacer21 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer21, gbc);
    searchButton = new JButton();
    searchButton.setText("Search");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(searchButton, gbc);
    final JPanel spacer22 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer22, gbc);
    final JPanel spacer23 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer23, gbc);
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 3;
    gbc.gridheight = 2;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5, 0, 0, 0);
    searchPanel.add(panel5, gbc);
    panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), " Search Results", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    searchResults = new JEditorPane();
    searchResults.setContentType("text/html");
    searchResults.setEditable(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5, 0, 0, 0);
    panel5.add(searchResults, gbc);
    final JPanel spacer24 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    searchPanel.add(spacer24, gbc);
    final JPanel spacer25 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    searchPanel.add(spacer25, gbc);
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
    contextTree = new JTree(buildTree());
    // TODO: place custom component creation code here
  }
}
