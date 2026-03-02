package com.needhamsoftware.securesrc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.crypto.AEADBadTagException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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
import com.needhamsoftware.securesrc.search.SearchResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

@SuppressWarnings({"CallToPrintStackTrace", "FieldCanBeLocal"})
public class TopFrame extends JFrame {
  private static final String DEFAULT_CIPHER_SPEC = "AES/GCM/NoPadding";
  private static final String USER_HOME = System.getProperty("user.home");
  private static final File USER_HOME_DIR;
  public static final String CIPHER_PROP = "com.needhamsoftware.securesrc.cipher";
  public static final int DEFAULT_KEY_SIZE = 128;
  public static final String PREF_CIPHER_SPEC = "cipher_spec";
  public static final String PREF_KEY_SIZE = "key_size";

  static {
    try {
      USER_HOME_DIR = new File(USER_HOME).getCanonicalFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static final String SAVE_FILE_NAME = "ssim.dat";
  private static final File DEFAULT_SAVE_LOCATION = new File(USER_HOME_DIR, SAVE_FILE_NAME);
  private final LuceneSearch searcher;
  private final JMenuItem deleteLoginPopupItem;
  private final JMenuItem deleteApplicationPopupItem;
  private final JMenuItem deleteContextPopupItem;
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
  private JPasswordField secret;
  private JButton addNewLoginButton;
  private JButton updateLoginButton;
  private JTextField contextName;
  private JTextArea contextDescription;
  private JTextField applicationName;
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
  private JButton nextButton;
  private JButton previousButton;
  private JPanel searchInfo;
  private JLabel hitCountLabel;
  private JLabel hitCount;
  private JPanel secQPanel;
  private JTable sqTable;
  private JTextField authApp;
  private JPasswordField pin;
  private JTextField loginUrl;
  private JTextField browserProfile;
  private JCheckBox showSecretCheckBox;
  private JCheckBox showPinCheckBox;
  private JButton copySecretIconButton;
  private JButton copyPinIconButton;
  private JLabel encryptionSpecDisplay;
  private final JPopupMenu treeContextMenu;
  private AddContextDialog addContextDialog;

  private JMenuBar menubar;
  private JMenu prefsMenu;
  private JCheckBoxMenuItem viewHistoryMenuItem;
  private JMenuItem saveFileLocation;

  private final JFileChooser fileChooser;

  private List<Context> contextList = new ArrayList<>();
  private DefaultMutableTreeNode selected;
  Context newContext;
  Application newApplication;
  private Login newLogin;
  private DefaultMutableTreeNode root;
  private AddApplicationDialog addApplicationDialog;
  private String outputCipher = DEFAULT_CIPHER_SPEC;
  private int keySize = DEFAULT_KEY_SIZE;
  Persistor persistor;
  KeyWithSalt masterPassword;
  private boolean showHistory;
  Pager pager = new Pager();
  private JMenuItem newEncryption;
  private JMenuItem changeMasterPassword;
  private String sqTableDirty;
  private int editingRow;

  public TopFrame(String title) throws HeadlessException {
    super(title);
    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    String saveLocation = prefs.get("save_location", null);
    if (saveLocation != null) {
      location = new File(saveLocation);
    }

    // if the user has configured an alternate encryption
    outputCipher = prefs.get(PREF_CIPHER_SPEC, outputCipher);
    keySize = Integer.parseInt(prefs.get(PREF_KEY_SIZE, String.valueOf(keySize)));

    // emergency override of cipher spec via command line system prop.
    String property = System.getProperty(CIPHER_PROP);
    if (property != null && !property.isEmpty()) {
      outputCipher = property;
    }
    try {
      persistor = new Persistor(outputCipher);
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
            keyWithSalt = askPassword(s, s.length, false);
          }
          masterPassword = keyWithSalt;
          return masterPassword;
        }, location);
      } catch (NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException |
               ClassNotFoundException | InvalidAlgorithmParameterException e) {
        // fatal - just die
        e.printStackTrace();
        System.exit(1);
      } catch (IOException e) {
        // Message and Die (jvm restart and message dialog vastly slows down UI script driven brute-forcing)
        if (e.getCause() instanceof AEADBadTagException) {
          JOptionPane.showMessageDialog(this, "Password does not match!");
        }
        System.exit(1);
      }
    } else {
      if (!DEFAULT_SAVE_LOCATION.getAbsolutePath().equals(location.getAbsolutePath())) {
        JOptionPane.showMessageDialog(this, location + " does not appear to exist. If you create new entries a new file will be created in this location.");
      }
    }
    this.fileChooser = new JFileChooser(location);
    $$$setupUI$$$();
    DefaultTableModel dataModel = new DefaultTableModel(new Object[]{"Question (key)", "Answer (value)"}, 2);
    sqTable.setModel(dataModel);
    sqTable.setColumnSelectionAllowed(true);
    sqTable.setCellSelectionEnabled(true);
    sqTable.setSurrendersFocusOnKeystroke(true);
    dataModel.addTableModelListener(new TableModelListener() {
      volatile boolean reacting;

      @Override
      public void tableChanged(TableModelEvent e) {
        if (sqTable.getEditorComponent() != null) {
          TopFrame.this.sqTableDirty = ((Login) getSelected().getUserObject()).getUuid();
        }
        int column = e.getColumn();
        if (column == 1 && !reacting) {
          editingRow = e.getFirstRow();
          reacting = true;
          padTable((DefaultTableModel) sqTable.getModel(), editingRow + 1);
          clickAt(++editingRow, 0);
          reacting = false;
        }
      }
    });
    sqTable.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        Component oc = e.getOppositeComponent();
        // editing a cell shifts focus to the cell.
        // don't want to persist until user stops editing
        if (oc != null && "Table.editor".equals(oc.getName())) {
          return;
        }
        if (sqTableDirty != null) {
          DefaultMutableTreeNode inTree = findInTree(sqTableDirty, root);
          updateLogin((Login) inTree.getUserObject());
          sqTableDirty = null;
        }
      }
    });
    Border border = BorderFactory.createLineBorder(Color.GRAY);
    loginDescription.setBorder(border);
    setSelected(root);
    contextTree.setSelectionPath(new TreePath(root.getPath()));
    contextTree.setCellRenderer(new LoginTreeCellRenderer());
    updateEncryptionDisplay(outputCipher);
    treeContextMenu = new JPopupMenu();
    newContextPopupItem = new JMenuItem("New Context");
    newContextPopupItem.addActionListener(e -> addContext());
    addNewContextButton.addActionListener(e -> addContext());
    updateContextButton.addActionListener(e -> updateContext());
    newApplicationPopupItem = new JMenuItem("New Application");
    newApplicationPopupItem.addActionListener(e -> addApplication());

    deleteLoginPopupItem = new JMenuItem("Delete Login");
    deleteApplicationPopupItem = new JMenuItem("Delete Application");
    deleteContextPopupItem = new JMenuItem("Delete Context");
    deleteLoginPopupItem.addActionListener(e -> deleteLogin());
    deleteApplicationPopupItem.addActionListener(e -> deleteApplication());
    deleteContextPopupItem.addActionListener(e -> deleteContext());

    addNewApplicationButton.addActionListener(e -> addApplication());
    updateApplicationButton.addActionListener(e -> updateApplication());
    addNewLoginButton.addActionListener(e -> createLogin(loginFromFormFields(null)));
    updateLoginButton.addActionListener(e -> updateLogin((Login) getSelected().getUserObject()));
    nextButton.addActionListener(e -> next());
    previousButton.addActionListener(e -> previous());
    showSecretCheckBox.addActionListener(new ActionListener() {
      Character orig = null;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (orig == null) {
          orig = secret.getEchoChar();
        }
        if (e.getSource() instanceof JCheckBox cb) {
          if (cb.isSelected()) {
            secret.setEchoChar((char) 0);
          } else {
            secret.setEchoChar(orig);
          }
        }
      }
    });
    showPinCheckBox.addActionListener(new ActionListener() {
      Character orig = null;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (orig == null) {
          orig = secret.getEchoChar();
        }
        if (e.getSource() instanceof JCheckBox cb) {
          if (cb.isSelected()) {
            pin.setEchoChar((char) 0);
          } else {
            pin.setEchoChar(orig);
          }
        }
      }
    });

    treeContextMenu.add(newContextPopupItem);
    treeContextMenu.add(newApplicationPopupItem);
    treeContextMenu.add(deleteLoginPopupItem);
    treeContextMenu.add(deleteApplicationPopupItem);
    treeContextMenu.add(deleteContextPopupItem);
    treeContextMenu.setVisible(false);
    contextTree.addTreeSelectionListener(e -> SwingUtilities.invokeLater(() -> {
      TreePath selectionPath = contextTree.getSelectionPath();
      if (selectionPath != null) {
        setSelected((DefaultMutableTreeNode) selectionPath.getLastPathComponent());
        if (getSelected() != null) {
          Object userObject = getSelected().getUserObject();
          if (userObject instanceof String) {
            clearContextPannel();
          }
          if (userObject instanceof Context context) {
            updateContextPanel(context);
          }
          if (userObject instanceof Application application) {
            DefaultMutableTreeNode context = (DefaultMutableTreeNode) getSelected().getParent();
            updateContextPanel((Context) context.getUserObject());
            updateApplicationPanel(application);
          }
          if (userObject instanceof Login login) {

            DefaultMutableTreeNode clicked = getSelected();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) clicked.getParent();
            if (parent.getUserObject() instanceof Login) {
              // we are an inactive login nested under an active login
              // need to go up an extra level.
              parent = (DefaultMutableTreeNode) parent.getParent();
            }
            DefaultMutableTreeNode application = parent;
            DefaultMutableTreeNode context = (DefaultMutableTreeNode) application.getParent();
            updateContextPanel((Context) context.getUserObject());
            updateApplicationPanel((Application) application.getUserObject());
            updateLoginPanel(login);
            if (!login.isActive()) {
              enableLoginPanel(false);
              updateLoginButton.setEnabled(false);
            }
          }
        }
      }
    }));

    contextTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int rowForLocation = contextTree.getRowForLocation(e.getX(), e.getY());
        contextTree.setSelectionPath(contextTree.getPathForRow(rowForLocation));
        if (e.isPopupTrigger()) {
          SwingUtilities.invokeLater(() -> contextToggle(e));
        }
      }

      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          SwingUtilities.invokeLater(() -> contextToggle(e));
        }
      }
    });


    this.searcher = new LuceneSearch();

    searchButton.addActionListener(e -> search());
    searchResults.addHyperlinkListener(e -> {
      HyperlinkEvent.EventType eventType = e.getEventType();
      if (eventType.equals(HyperlinkEvent.EventType.ACTIVATED)) {
        URL url = e.getURL();
        // Note that we ignore the url scheme, and no web request is
        // conducted, were just grabbing a UUID so we can find the
        // item we want to select in the tree navigation left panel
        // In other words the "host" is just a UUID, not anything
        // addressable on the web.
        String uuid = url.getHost();
        DefaultMutableTreeNode inTree = findInTree(uuid, root);
        TreePath path = new TreePath(inTree.getPath());
        contextTree.setSelectionPath(path);
        contextTree.scrollPathToVisible(path);
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
    query.addActionListener(e -> search());
    Icon copyIconSortOf = UIManager.getIcon("FileView.fileIcon");
    copySecretIconButton.setIcon(copyIconSortOf);
    copySecretIconButton.addActionListener(e -> {
      StringSelection stringSelection = new StringSelection(String.copyValueOf(secret.getPassword()));
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, null);
    });
    copyPinIconButton.setIcon(copyIconSortOf);
    copyPinIconButton.addActionListener(e -> {
      StringSelection stringSelection = new StringSelection(String.copyValueOf(pin.getPassword()));
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, null);
    });
    JComponent comp = $$$getRootComponent$$$();
    comp.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F3"),
        "goToSearch");
    comp.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK),
        "goToSearch");
    comp.getActionMap().put("goToSearch",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            query.requestFocusInWindow();
          }
        });
    this.add(comp);
    buildTree();
    contextTree.setSelectionPath(new TreePath(new Object[]{root}));
  }

  @SuppressWarnings("SameParameterValue")
  private void clickAt(int row, int column) {
    sqTable.editCellAt(row, column);
    sqTable.setRowSelectionInterval(row, row);
    sqTable.setColumnSelectionInterval(column, column);
  }

  private void deleteContext() {
    if (getSelected().getUserObject() instanceof Context ctx) {
      if (ctx.getApplications().isEmpty()) {
        contextList.remove(ctx);
        buildTree();
        persist();
        search();
      } else {
        JOptionPane.showMessageDialog(this, "Please remove all applications first.");
      }
    } else {
      System.err.println("Not a Context?");
    }
  }

  private void deleteLogin() {
    DefaultMutableTreeNode tmp = getSelected();
    Object clickedObj = tmp.getUserObject();
    while ((tmp.getUserObject() instanceof Login)) {
      tmp = (DefaultMutableTreeNode) tmp.getParent();
    }
    if (tmp.getUserObject() instanceof Application app) {
      Login toDelete = (Login) clickedObj;
      if (toDelete.getOriginalUUID().equals(toDelete.getUuid())) {
        // we're deleting the ultimate ancestor, and we need to promote the first child
        if (getSelected().getChildCount() > 0) {
          DefaultMutableTreeNode childAt = (DefaultMutableTreeNode) getSelected().getChildAt(0);
          Login firstChild = (Login) childAt.getUserObject();
          String newParentUUID = firstChild.getUuid();
          firstChild.setOriginalUUID(newParentUUID);
          List<DefaultMutableTreeNode> others = new ArrayList<>();
          collectRelatedLogins(toDelete.getUuid(), others, root);
          for (DefaultMutableTreeNode other : others) {
            Login toFix = (Login) other.getUserObject();
            toFix.setOriginalUUID(newParentUUID);
          }
        }
      }
      app.getLogins().remove(toDelete);
      setSelected(tmp);
    }
    persist();
    search(); // ensure results don't contain deleted
    DefaultMutableTreeNode finalTmp = tmp;
    // ensure the login panel doesn't retain deleted
    SwingUtilities.invokeLater(()-> contextTree.setSelectionPath(new TreePath(finalTmp.getPath())));
  }

  private void deleteApplication() {
    if (getSelected().getUserObject() instanceof Application app) {
      if (app.getLogins().isEmpty()) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getSelected().getParent();
        Context ctx = (Context) parent.getUserObject();
        ctx.getApplications().remove(app);
        buildTree();
        persist();
        search();
      } else {
        JOptionPane.showMessageDialog(this, "Please remove all logins first.");
      }
    } else {
      System.err.println("Not an Application?");
    }
  }

  private void menuBar() {
    this.menubar = new JMenuBar();
    this.prefsMenu = new JMenu("Preferences");
    this.viewHistoryMenuItem = new JCheckBoxMenuItem("View History");
    this.saveFileLocation = new JMenuItem("Save File Location...");
    this.newEncryption = new JMenuItem("Change Encryption");
    this.changeMasterPassword = new JMenuItem("Change Password");

    this.viewHistoryMenuItem.addActionListener(e -> {
      showHistory = viewHistoryMenuItem.isSelected();
      buildTree();
      search();
    });
    saveFileLocation.addActionListener(e -> {
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      int result = fileChooser.showOpenDialog(TopFrame.this);
      if (result == JFileChooser.APPROVE_OPTION) {
        try {
          location = new File(fileChooser.getSelectedFile().getCanonicalPath(), SAVE_FILE_NAME);
          Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
          prefs.put("save_location", location.getAbsolutePath());
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(TopFrame.this, "Unable to resolve location:" + ex.getMessage());
        }
      }
    });
    newEncryption.addActionListener(e -> {
      EncryptionUpdatePanel eup = new EncryptionUpdatePanel();
      eup.getCipherSpec().setText(outputCipher);
      eup.getKeySize().setText(String.valueOf(keySize));

      JOptionPane jOptionPane = new JOptionPane(eup, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
        @Override
        public void selectInitialValue() {
          eup.getCipherSpec().requestFocusInWindow();
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

      String spec = eup.getCipherSpec().getText();
      String text = eup.getKeySize().getText();
      Preferences prefs = Preferences.userRoot().node(TopFrame.this.getClass().getName());
      if (text != null && !text.trim().isEmpty() && Integer.parseInt(text) != keySize) {
        int size = Integer.parseInt(text);
        KeyWithSalt newPW = askPassword(null, size, true);
        if (newPW != null) {
          masterPassword = newPW;
          keySize = size;
          prefs.put(PREF_KEY_SIZE, String.valueOf(keySize));
        }
      }
      if ((Integer) value == JOptionPane.OK_OPTION) {
        try {
          Encryption.loadCipher(spec);
          outputCipher = spec;
          prefs.put(PREF_CIPHER_SPEC, outputCipher);
          persistor = new Persistor(spec);
          updateEncryptionDisplay(spec);
        } catch (EncryptionException ex) {
          JOptionPane.showMessageDialog(this, "<html><body><p style='width: 200px;'>" + ex.getMessage() + "</p></body></html>");
          ex.printStackTrace();
        }
      }
      persist();
    });
    changeMasterPassword.addActionListener(e -> {
      KeyWithSalt newPw = askPassword(null, keySize, true);
      if (newPw != null) {
        masterPassword = newPw;
        persist();
      }
    });
    this.prefsMenu.add(viewHistoryMenuItem);
    this.prefsMenu.add(saveFileLocation);
    this.prefsMenu.add(newEncryption);
    this.prefsMenu.add(changeMasterPassword);
    this.menubar.add(prefsMenu);
    this.setJMenuBar(menubar);
  }

  private void updateEncryptionDisplay(String spec) {
    encryptionSpecDisplay.setText(spec + " (" + keySize + ")");
  }

  private void search() {
    try {
      String text = query.getText();
      if (text != null && !text.isEmpty()) {
        SearchResult result = pager.firstPage(i -> searcher.search(text, pager.getPageSize(), i));
        renderSearchResults(result);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage());
    }
  }

  private void next() {
    try {
      SearchResult result = pager.nextPage(i -> searcher.search(query.getText(), pager.getPageSize(), i));

      renderSearchResults(result);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage());
    }
  }

  private void previous() {
    try {
      SearchResult result = pager.prevPage(i -> searcher.search(query.getText(), pager.getPageSize(), i));
      renderSearchResults(result);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage());
    }
  }

  private void renderSearchResults(SearchResult result) {
    nextButton.setEnabled(result.totalHits > pager.docsToSkip() + pager.getPageSize());
    previousButton.setEnabled(pager.getCurrentPage() > 1);
    hitCount.setText(String.valueOf(result.totalHits));
    List<Document> search = result.resultPage;
    // build html for our JEditorPane to display
    StringBuilder html = new StringBuilder("<html><head></head><body><ul>");
    for (Document document : search) {
      // JEditorPane will try to produce a URL object based on the href
      // element so to make the links in the search panel clickable
      // we have to give them a bogus http scheme. No web requests
      // will be made since this is only used by com/needhamsoftware/securesrc/ui/TopFrame.java:412
      // the href will look like http://d593301b-6dac-46ca-b9af-406e46660385
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
  }

  private void updateContext() {
    Object userObject = getSelected().getUserObject();
    while (userObject instanceof Login || userObject instanceof Application) {
      setSelected((DefaultMutableTreeNode) getSelected().getParent());
      userObject = getSelected().getUserObject();
    }
    Context contextObject = (Context) getSelected().getUserObject();
    contextObject.setName(contextName.getText());
    contextObject.setDescription(contextDescription.getText());
    newContext = contextObject;
    persist();

  }

  private void updateApplication() {
    Object userObject = getSelected().getUserObject();
    while (userObject instanceof Login) {
      setSelected((DefaultMutableTreeNode) getSelected().getParent());
      userObject = getSelected().getUserObject();
    }
    Application applicationObject = (Application) getSelected().getUserObject();
    applicationObject.setName(applicationName.getText());
    applicationObject.setDescription(applicationDescription.getText());
    newApplication = applicationObject;
    persist();

  }

  private void updateLogin(Login login) {
    Login newLogin = loginFromFormFields(login.getOriginalUUID());
    login.inActivate();
    contextTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) getSelected().getParent()).getPath()));
    createLogin(newLogin);
  }

  @SuppressWarnings("deprecation")
  private Login loginFromFormFields(String originalUUID) {
    String name = loginName.getText();
    String description = loginDescription.getText();
    String identity = this.identity.getText();
    String secret = this.secret.getText();
    String authApp = this.authApp.getText();
    String pin = this.pin.getText();
    String loginUrl = this.loginUrl.getText();
    String browserProfile = this.browserProfile.getText();
    TableModel model = sqTable.getModel();
    LinkedHashMap<String, String> securityChallenges = new LinkedHashMap<>();
    for (int i = 0; i < model.getRowCount(); i++) {
      String key = (String) model.getValueAt(i, 0);
      String value = (String) model.getValueAt(i, 1);
      securityChallenges.put(key, value);
    }

    return new Login(true, name, description, Instant.now(), identity, secret,
        authApp, pin, loginUrl, browserProfile, originalUUID, securityChallenges);
  }

  private void createLogin(Login toCreate) {

    newLogin = toCreate;
    TreePath selectionPath = contextTree.getSelectionPath();
    if (selectionPath != null) {
      setSelected((DefaultMutableTreeNode) selectionPath.getLastPathComponent());
      if (getSelected() != null) {
        DefaultMutableTreeNode curr = getSelected();
        Object userObject;
        for (userObject = curr.getUserObject(); userObject instanceof Login; curr = (DefaultMutableTreeNode) curr.getParent()) {
          userObject = curr.getUserObject();
        }
        // first non-login should be an application
        if (userObject instanceof Application app) {
          app.getLogins().add(newLogin);
          if (!persist()) {
            app.getLogins().remove(newLogin);
          }
        } else {
          throw new IllegalStateException("Unexpected tree structure");
        }

      }
    }
  }

  private boolean persist() {
    if (masterPassword == null) {
      // This should only happen if there was no data file to load.
      masterPassword = askPassword(null, keySize, true);
    }
    if (masterPassword == null) {
      // we failed don't try to write anything.
      return false;
    }
    try {
      persistor.write(contextList, masterPassword, location);
      buildTree();
      return true;
    } catch (IOException | InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException |
             InvalidKeyException | InvalidAlgorithmParameterException ex) {
      JOptionPane.showMessageDialog(this, "Unable to save data using " + outputCipher + "\n" +
          ex.getClass() + " Exception message:" + ex.getMessage());
      ex.printStackTrace();
      return false;
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

  void collectRelatedLogins(String originalUUID, List<DefaultMutableTreeNode> collected, DefaultMutableTreeNode root) {
    if (root.getChildCount() > 0) {
      int childCount = root.getChildCount();
      for (int i = 0; i < childCount; i++) {
        collectRelatedLogins(originalUUID, collected, (DefaultMutableTreeNode) root.getChildAt(i));
      }
    }
    if (root.getUserObject() instanceof Login login) {
      if (login.getOriginalUUID().equals(originalUUID)) {
        collected.add(root);
      }
    }
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
    authApp.setText(login.getAuthApp());
    pin.setText(login.getPin());
    loginUrl.setText(login.getLoginUrl());
    browserProfile.setText(login.getBrowserProfile());
    DefaultTableModel model = (DefaultTableModel) sqTable.getModel();
    model.getDataVector().clear();
    sqTable.tableChanged(new TableModelEvent(model));
    for (Map.Entry<String, String> stringStringEntry : login.getSecurityChallenges().entrySet()) {
      model.addRow(new Object[]{stringStringEntry.getKey(), stringStringEntry.getValue()});
    }
    padTable(model, 3);
    sqTable.tableChanged(new TableModelEvent(model));
    updateLoginButton.setEnabled(true);
  }

  private static void padTable(DefaultTableModel model, int minrows) {
    do {
      model.addRow(new Object[]{"", ""});
    } while (model.getRowCount() < minrows);
  }

  private static JOptionPane findOptionPane(JComponent parent) {
    JOptionPane pane;
    if (!(parent instanceof JOptionPane)) {
      pane = findOptionPane((JComponent) parent.getParent());
    } else {
      pane = (JOptionPane) parent;
    }
    return pane;
  }

  private KeyWithSalt askPassword(byte[] salt, int ksize, boolean confirm) {
    PasswordPanel pp = new PasswordPanel();
    pp.requireConfirm(confirm);
    int okCxl = popUpPasswordDialog(pp);
    char[] password = pp.getPasswordField().getPassword();
    if (password.length == 0) {
      return null;
    }
    if (okCxl == JOptionPane.OK_OPTION) {
      try {
        KeyWithSalt key = Encryption.getKey("AES", ksize, password, salt);
        keySize = ksize; // after we successfully create the key!
        return key;
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
        JOptionPane.showMessageDialog(this, "Unable to hash password for " + outputCipher + "\n" +
            ex.getClass() + " Exception message:" + ex.getMessage());
        ex.printStackTrace();
      }
    }
    return null;
  }

  private static int popUpPasswordDialog(PasswordPanel pp) {
    JButton ok = new JButton("Ok");
    JButton cancel = new JButton("Cancel");
    ok.addActionListener(e -> {
      JButton source = (JButton) e.getSource();
      JOptionPane parentPane = findOptionPane(source);
      parentPane.setValue(JOptionPane.OK_OPTION);
    });
    cancel.addActionListener(e -> {
      JButton source = (JButton) e.getSource();
      JOptionPane parentPane = findOptionPane(source);
      parentPane.setValue(JOptionPane.CANCEL_OPTION);
    });
    pp.setExternalOkButton(ok);
    JOptionPane jOptionPane = new JOptionPane(pp, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{ok, cancel}, ok) {
      @Override
      public void selectInitialValue() {
        pp.getPasswordField().requestFocusInWindow();
      }
    };
    List<Component> focusItems = new ArrayList<>();
    focusItems.add(pp.getPasswordField());
    JPasswordField conf = pp.getConfirmPassword();
    if (conf.isVisible()) {
      focusItems.add(conf);
    }
    focusItems.add(ok);
    focusItems.add(cancel);

    FocusTraversalList policy = new FocusTraversalList(focusItems);
    jOptionPane.setFocusTraversalPolicy(policy);
    JDialog dialog = jOptionPane.createDialog("Enter Password");
    dialog.setFocusTraversalPolicy(policy);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
    Object value = jOptionPane.getValue();
    dialog.dispose();
    if (value == null) {
      value = JOptionPane.CLOSED_OPTION;
    }
    return (int) (Integer) value;
  }

  private void clearContextPannel() {
    contextName.setText("");
    contextDescription.setText("");
    enableContext(false);
    addNewContextButton.setEnabled(true);
    clearApplicationPanel();
    enableApplicationPannel(false);
  }

  private void enableApplicationPannel(boolean enabled) {
    this.applicationName.setEnabled(enabled);
    this.applicationDescription.setEnabled(enabled);
    updateApplicationButton.setEnabled(enabled);
    addNewApplicationButton.setEnabled(enabled);
  }

  private void clearApplicationPanel() {
    this.applicationName.setText("");
    this.applicationDescription.setText("");
    this.applicationPanel.setToolTipText("");
    enableApplicationPannel(false);
    addNewApplicationButton.setEnabled(true);
    clearLoginPanel();
    enableLoginPanel(false);
  }

  private void clearLoginPanel() {
    loginName.setText("");
    loginDescription.setText("");
    identity.setText("");
    secret.setText("");
    authApp.setText("");
    pin.setText("");
    loginUrl.setText("");
    browserProfile.setText("");
    DefaultTableModel model = (DefaultTableModel) sqTable.getModel();
    model.getDataVector().clear();
    sqTable.tableChanged(new TableModelEvent(model));
  }

  private void enableLoginPanel(boolean enabled) {
    loginName.setEnabled(enabled);
    loginDescription.setEnabled(enabled);
    identity.setEnabled(enabled);
    secret.setEnabled(enabled);
    pin.setEnabled(enabled);
    loginUrl.setEnabled(enabled);
    authApp.setEnabled(enabled);
    browserProfile.setEnabled(enabled);
    sqTable.setEnabled(enabled);
    addNewLoginButton.setEnabled(enabled);
  }

  private void updateApplicationPanel(Application context) {
    applicationName.setText(context.getName());
    applicationDescription.setText(context.getDescription());
    applicationPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
    enableApplicationPannel(true);
    addNewApplicationButton.setEnabled(false);
    clearLoginPanel();
    enableLoginPanel(true);
  }

  private void updateContextPanel(Context context) {
    contextName.setText(context.getName());
    contextDescription.setText(context.getDescription());
    contextPanel.setToolTipText("Created on:" + context.getCreatedDate().toString());
    enableContext(true);
    addNewContextButton.setEnabled(false);
    clearApplicationPanel();
  }

  private void enableContext(boolean enabled) {
    contextName.setEnabled(enabled);
    contextDescription.setEnabled(enabled);
    updateContextButton.setEnabled(enabled);
    addNewApplicationButton.setEnabled(enabled);
  }

  private void addApplication() {
    if (addApplicationDialog == null) {
      addApplicationDialog = new AddApplicationDialog(this);
    }
    Context userObject = (Context) getSelected().getUserObject();
    addApplicationDialog.setApplications(userObject.getApplications());
    addApplicationDialog.setVisible(true);
    SwingUtilities.invokeLater(this::buildTree);
  }

  private void addContext() {
    if (addContextDialog == null) {
      addContextDialog = new AddContextDialog(this);
    }
    addContextDialog.setContexts(contextList);
    addContextDialog.setVisible(true);
    SwingUtilities.invokeLater(this::buildTree);
  }

  private void contextToggle(MouseEvent e) {
    if (getSelected() == root) {
      newContextPopupItem.setVisible(true);
      newApplicationPopupItem.setVisible(false);
      deleteLoginPopupItem.setVisible(false);
      deleteApplicationPopupItem.setVisible(false);
      deleteContextPopupItem.setVisible(false);
    } else if (getSelected() != null && getSelected().getUserObject() instanceof Context) {
      newApplicationPopupItem.setVisible(true);
      newContextPopupItem.setVisible(false);
      deleteLoginPopupItem.setVisible(false);
      deleteApplicationPopupItem.setVisible(false);
      deleteContextPopupItem.setVisible(true);
    } else if (getSelected() != null && getSelected().getUserObject() instanceof Application) {
      newApplicationPopupItem.setVisible(false);
      newContextPopupItem.setVisible(false);
      deleteLoginPopupItem.setVisible(false);
      deleteApplicationPopupItem.setVisible(true);
      deleteContextPopupItem.setVisible(false);
    } else if (getSelected() != null && getSelected().getUserObject() instanceof Login) {
      newApplicationPopupItem.setVisible(false);
      newContextPopupItem.setVisible(false);
      deleteLoginPopupItem.setVisible(true);
      deleteApplicationPopupItem.setVisible(false);
      deleteContextPopupItem.setVisible(false);
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
      if (context == getSelected().getUserObject()) {
        contextTree.setSelectionPath(new TreePath(contextChild.getPath()));
        setSelected(contextChild);
        selectedIsShown = true;
      }
      for (Application application : context.getApplications()) {
        DefaultMutableTreeNode applicationChild = new DefaultMutableTreeNode(application);
        model.insertNodeInto(applicationChild, contextChild, contextChild.getChildCount());
        if (application == getSelected().getUserObject()) {
          contextTree.setSelectionPath(new TreePath(applicationChild.getPath()));
          setSelected(applicationChild);
          selectedIsShown = true;
        }
        List<Login> logins = showHistory ? application.getLogins() : application.getActiveLogins();
        List<Login> revisedLogins = new ArrayList<>();
        for (Login login : logins) {
          if (showHistory && !login.getOriginalUUID().equals(login.getUuid())) {
            revisedLogins.add(login);
          } else {
            DefaultMutableTreeNode loginChild = new DefaultMutableTreeNode(login);
            model.insertNodeInto(loginChild, applicationChild, applicationChild.getChildCount());
            if (login == getSelected().getUserObject()) {
              contextTree.setSelectionPath(new TreePath(loginChild.getPath()));
              setSelected(loginChild);
              selectedIsShown = true;
            }
          }
        }

        // assign children to groups based on original ID
        Map<String, List<Login>> childrenByOriginal = new HashMap<>();
        for (Login login : revisedLogins) {
          childrenByOriginal.computeIfAbsent(login.getOriginalUUID(), l -> new ArrayList<>()).add(login);
        }
        // sort each group by creation date
        for (List<Login> revisionList : childrenByOriginal.values()) {
          revisionList.sort(Comparator.comparingLong(l -> l.getCreatedDate().toEpochMilli()));
        }
        // now add each list to the node in the tree that has a uuid matching the original uuid
        for (Map.Entry<String, List<Login>> revOriginals : childrenByOriginal.entrySet()) {
          String originalUUID = revOriginals.getKey();
          for (Login login : revOriginals.getValue()) {
            DefaultMutableTreeNode loginChild = new DefaultMutableTreeNode(login);
            DefaultMutableTreeNode parent = findInTree(originalUUID, root);
            model.insertNodeInto(loginChild, parent, parent.getChildCount());
            if (login == getSelected().getUserObject()) {
              contextTree.setSelectionPath(new TreePath(loginChild.getPath()));
              setSelected(loginChild);
              selectedIsShown = true;
            }
          }
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
      if (newLogin != null && newLogin == ((DefaultMutableTreeNode) pathForRow.getLastPathComponent()).getUserObject()) {
        contextTree.setSelectionPath(pathForRow);
        newLogin = null;
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
    panel1.setMinimumSize(new Dimension(50, 15));
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
    gbc.gridy = 19;
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
    gbc.gridy = 19;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 19;
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
    secret = new JPasswordField();
    secret.setColumns(60);
    secret.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(secret, gbc);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 18;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    loginDisplay.add(panel3, gbc);
    final JLabel label6 = new JLabel();
    label6.setHorizontalAlignment(2);
    label6.setHorizontalTextPosition(2);
    label6.setText("Encryption:");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    panel3.add(label6, gbc);
    addNewLoginButton = new JButton();
    addNewLoginButton.setEnabled(false);
    addNewLoginButton.setText("Add New");
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 0;
    panel3.add(addNewLoginButton, gbc);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    panel3.add(panel4, gbc);
    updateLoginButton = new JButton();
    updateLoginButton.setEnabled(false);
    updateLoginButton.setText("Update");
    gbc = new GridBagConstraints();
    gbc.gridx = 8;
    gbc.gridy = 0;
    panel3.add(updateLoginButton, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel3.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 7;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel3.add(spacer10, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel3.add(spacer11, gbc);
    final JPanel spacer12 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 9;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel3.add(spacer12, gbc);
    encryptionSpecDisplay = new JLabel();
    encryptionSpecDisplay.setText("Label");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    panel3.add(encryptionSpecDisplay, gbc);
    final JPanel spacer13 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel3.add(spacer13, gbc);
    final JLabel label7 = new JLabel();
    label7.setEnabled(true);
    label7.setText("Auth App");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label7, gbc);
    final JLabel label8 = new JLabel();
    label8.setText("PIN");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 10;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label8, gbc);
    final JLabel label9 = new JLabel();
    label9.setText("Login URL");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 12;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label9, gbc);
    final JLabel label10 = new JLabel();
    label10.setText("Browser Profile");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 14;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(label10, gbc);
    secQPanel = new JPanel();
    secQPanel.setLayout(new BorderLayout(0, 0));
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 16;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    loginDisplay.add(secQPanel, gbc);
    secQPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Security Questions & Other Key/Value Pairs", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    final JScrollPane scrollPane1 = new JScrollPane();
    scrollPane1.setPreferredSize(new Dimension(452, 90));
    secQPanel.add(scrollPane1, BorderLayout.CENTER);
    sqTable = new JTable();
    sqTable.setCellSelectionEnabled(true);
    sqTable.setEnabled(false);
    sqTable.setMinimumSize(new Dimension(30, 60));
    sqTable.putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
    sqTable.putClientProperty("html.disable", Boolean.FALSE);
    scrollPane1.setViewportView(sqTable);
    final JPanel spacer14 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 9;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer14, gbc);
    final JPanel spacer15 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 11;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer15, gbc);
    final JPanel spacer16 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 13;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer16, gbc);
    final JPanel spacer17 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 15;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer17, gbc);
    final JPanel spacer18 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 17;
    gbc.fill = GridBagConstraints.VERTICAL;
    loginDisplay.add(spacer18, gbc);
    authApp = new JTextField();
    authApp.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 8;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(authApp, gbc);
    pin = new JPasswordField();
    pin.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 10;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(pin, gbc);
    loginUrl = new JTextField();
    loginUrl.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 12;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(loginUrl, gbc);
    browserProfile = new JTextField();
    browserProfile.setEnabled(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 14;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(browserProfile, gbc);
    showSecretCheckBox = new JCheckBox();
    showSecretCheckBox.setText("Show");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 6;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(showSecretCheckBox, gbc);
    showPinCheckBox = new JCheckBox();
    showPinCheckBox.setText("show");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 10;
    gbc.anchor = GridBagConstraints.WEST;
    loginDisplay.add(showPinCheckBox, gbc);
    copySecretIconButton = new JButton();
    copySecretIconButton.setPreferredSize(new Dimension(22, 22));
    copySecretIconButton.setRolloverEnabled(false);
    copySecretIconButton.setText("");
    copySecretIconButton.setToolTipText("Copy Password");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(copySecretIconButton, gbc);
    copyPinIconButton = new JButton();
    copyPinIconButton.setPreferredSize(new Dimension(22, 22));
    copyPinIconButton.setRolloverEnabled(false);
    copyPinIconButton.setText("");
    copyPinIconButton.setToolTipText("Copy Password");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loginDisplay.add(copyPinIconButton, gbc);
    contextPanel = new JPanel();
    contextPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel2.add(contextPanel, gbc);
    contextPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Context", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, contextPanel.getFont()), null));
    final JLabel label11 = new JLabel();
    label11.setText("Name");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    contextPanel.add(label11, gbc);
    final JPanel spacer19 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer19, gbc);
    final JPanel spacer20 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    contextPanel.add(spacer20, gbc);
    contextName = new JTextField();
    contextName.setColumns(48);
    contextName.setText("");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(contextName, gbc);
    final JPanel spacer21 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer21, gbc);
    final JPanel spacer22 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer22, gbc);
    final JLabel label12 = new JLabel();
    label12.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    contextPanel.add(label12, gbc);
    final JPanel spacer23 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.VERTICAL;
    contextPanel.add(spacer23, gbc);
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
    final JPanel spacer24 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contextPanel.add(spacer24, gbc);
    applicationPanel = new JPanel();
    applicationPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.BOTH;
    panel2.add(applicationPanel, gbc);
    applicationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Application", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    final JLabel label13 = new JLabel();
    label13.setText("Name");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    applicationPanel.add(label13, gbc);
    final JPanel spacer25 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer25, gbc);
    final JPanel spacer26 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    applicationPanel.add(spacer26, gbc);
    applicationName = new JTextField();
    applicationName.setColumns(48);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(applicationName, gbc);
    final JPanel spacer27 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer27, gbc);
    final JPanel spacer28 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer28, gbc);
    final JLabel label14 = new JLabel();
    label14.setText("Description");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.WEST;
    applicationPanel.add(label14, gbc);
    final JPanel spacer29 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer29, gbc);
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
    final JPanel spacer30 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    applicationPanel.add(spacer30, gbc);
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
    final JPanel spacer31 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer31, gbc);
    searchButton = new JButton();
    searchButton.setText("Search");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(searchButton, gbc);
    final JPanel spacer32 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer32, gbc);
    final JPanel spacer33 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 5;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchPanel.add(spacer33, gbc);
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
    searchInfo = new JPanel();
    searchInfo.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    panel5.add(searchInfo, gbc);
    nextButton = new JButton();
    nextButton.setText("Next");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchInfo.add(nextButton, gbc);
    final JPanel spacer34 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.VERTICAL;
    searchInfo.add(spacer34, gbc);
    previousButton = new JButton();
    previousButton.setText("Previous");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchInfo.add(previousButton, gbc);
    hitCount = new JLabel();
    hitCount.setText("0");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    searchInfo.add(hitCount, gbc);
    final JPanel spacer35 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    searchInfo.add(spacer35, gbc);
    hitCountLabel = new JLabel();
    hitCountLabel.setText("Found:");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    searchInfo.add(hitCountLabel, gbc);
    final JScrollPane scrollPane2 = new JScrollPane();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(5, 0, 0, 0);
    panel5.add(scrollPane2, gbc);
    searchResults = new JEditorPane();
    searchResults.setContentType("text/html");
    searchResults.setEditable(false);
    scrollPane2.setViewportView(searchResults);
    final JPanel spacer36 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    searchPanel.add(spacer36, gbc);
    final JPanel spacer37 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    searchPanel.add(spacer37, gbc);
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

  private DefaultMutableTreeNode getSelected() {
    return selected;
  }

  private void setSelected(DefaultMutableTreeNode selected) {
    this.selected = selected;
  }
}
