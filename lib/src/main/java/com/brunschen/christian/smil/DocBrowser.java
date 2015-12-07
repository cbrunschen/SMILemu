package com.brunschen.christian.smil;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import com.brunschen.christian.smil.Doc.TocNode;

public class DocBrowser extends JPanel {

  public static final long serialVersionUID = 0L;

  private class HistoryEntry {
    public String urlString;
    public Point viewPosition = null;

    public HistoryEntry(String urlString) {
      this.urlString = urlString;
    }
  }

  private List<HistoryEntry> history = new ArrayList<HistoryEntry>();
  private int historyIndex = 0;
  private String lastUrlString = null;

  private JScrollPane scrollPane;
  private JButton backButton;
  private JButton forwardButton;
  private JEditorPane editorPane;
  private Set<String> internalUrlStrings = new HashSet<String>();

  public static class TreeNode {
    public String title;
    public String urlString;

    public TreeNode(String title, String urlString) {
      this.title = title;
      this.urlString = urlString;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  private void addTocChildren(String urlBase, DefaultMutableTreeNode treeNode, TocNode tocNode) {
    for (TocNode child : tocNode.children()) {
      DefaultMutableTreeNode treeChild = new DefaultMutableTreeNode(new TreeNode(child.name(), urlBase
          + Doc.PartSeparator + child.path()));
      treeNode.add(treeChild);
      addTocChildren(urlBase, treeChild, child);
    }
  }

  public DocBrowser(List<TreeNode> docNodes) {
    super();
    DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Documentation");
    for (TreeNode docNode : docNodes) {
      try {
        Doc doc = Doc.doc(new URL(docNode.urlString));
        if (doc != null) {
          DefaultMutableTreeNode docTreeNode = new DefaultMutableTreeNode(docNode);
          TocNode tocRoot = doc.tocRoot();
          addTocChildren(docNode.urlString, docTreeNode, tocRoot);
          treeRoot.add(docTreeNode);
          internalUrlStrings.add(docNode.urlString);
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    JPanel buttonRow = new JPanel();
    buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
    backButton = new JButton("←");
    backButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        goBack();
      }
    });
    buttonRow.add(backButton);
    forwardButton = new JButton("→");
    forwardButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        goForward();
      }
    });
    buttonRow.add(forwardButton);
    buttonRow.add(Box.createHorizontalGlue());
    buttonRow.setAlignmentX(0.5f);
    this.add(buttonRow);

    JTree tree = new JTree(treeRoot);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        JTree tree = (JTree) e.getSource();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node != null) {
          Object userObject = node.getUserObject();
          if (userObject != null && userObject instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) userObject;
            if (treeNode.urlString != null) {
              navigateTo(treeNode.urlString);
            }
          }
        }
      }
    });
    JScrollPane treeScrollPane = new JScrollPane(tree);
    treeScrollPane.setMinimumSize(new Dimension(50, 50));

    editorPane = new JEditorPane();
    editorPane.setEditable(false);
    editorPane.setEditorKitForContentType("text/html", new DocumentationEditorKit());
    editorPane.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
          navigateTo(e.getURL().toString());
        }
      }
    });
    scrollPane = new JScrollPane(editorPane);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.setPreferredSize(new Dimension(900, 500));
    scrollPane.setMinimumSize(new Dimension(50, 50));

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, treeScrollPane, scrollPane);
    splitPane.setAlignmentX(0.5f);

    this.add(splitPane);
  }

  public void navigateTo(String urlString) {
    history = history.subList(0, historyIndex);
    history.add(new HistoryEntry(urlString));
    goForward();
  }

  private void go(int direction) {
    storePosition();
    historyIndex += direction;
    refresh();
  }

  public void goBack() {
    go(-1);
  }

  public void goForward() {
    go(+1);
  }

  public void storePosition() {
    if (historyIndex > 0) {
      HistoryEntry hEntry = history.get(historyIndex - 1);
      hEntry.viewPosition = scrollPane.getViewport().getViewPosition();
    }
  }

  private void showError(String errorMessage) {
    try {
      editorPane.read(new StringReader(errorMessage), null);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  private static void escape(StringBuilder sb, String s) {
    for (char c : s.toCharArray()) {
      switch (c) {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        default:
          sb.append(c);
      }
    }
  }

  private String escape(String s) {
    StringBuilder sb = new StringBuilder();
    escape(sb, s);
    return sb.toString();
  }

  public void refresh() {
    HistoryEntry hEntry = history.get(historyIndex - 1);
    String urlString = hEntry.urlString;
    String urlStringWithPart;

    String part = null;
    String ref = null;
    int hashIndex = urlString.indexOf('#');
    if (hashIndex >= 0) {
      ref = urlString.substring(hashIndex + 1);
      urlString = urlString.substring(0, hashIndex);
    }
    urlStringWithPart = urlString;
    int dollarIndex = urlString.indexOf(Doc.PartSeparator);
    if (dollarIndex >= 0) {
      part = urlString.substring(dollarIndex + 1);
      urlString = urlString.substring(0, dollarIndex);
    }

    if (internalUrlStrings.contains(urlString)) {
      if (lastUrlString == null || !lastUrlString.equals(urlStringWithPart)) {
        try {
          URL url = new URL(urlString);
          Doc doc = Doc.doc(url);
          byte[] data = doc.data(part != null ? part : doc.defaultIdent());
          InputStream is = new ByteArrayInputStream(data);
          editorPane.setContentType(doc.contentType());
          HTMLDocument hdoc = (HTMLDocument) editorPane.getEditorKitForContentType("text/html").createDefaultDocument();
          hdoc.setBase(url);
          hdoc.setAsynchronousLoadPriority(-1);
          editorPane.read(is, hdoc);
          lastUrlString = urlStringWithPart;
        } catch (IOException e) {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          showError(String.format("<html><head></head><body>Cannot load URL '%s':<br><pre>%s</pre></body></html>",
              escape(urlString), escape(sw.toString())));
          lastUrlString = null;
        }
      } else {
      }
      if (lastUrlString != null) {
        if (hEntry.viewPosition != null) {
          final Point position = hEntry.viewPosition;
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              scrollPane.getViewport().setViewPosition(position);
            }
          });
        } else if (ref != null) {
          final String refCopy = ref;
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              editorPane.scrollToReference(refCopy);
            }
          });
        } else {
          editorPane.setCaretPosition(0);
        }
      }
    } else {
      String escapedUrlString = escape(urlString);
      showError(String.format("<html><head></head><body>" + "<h2>Outside Link: <em><tt>%s</tt></em></h2>" + "<p>"
          + "This Documentation Browser won't navigate outside SMILemu's documentation,"
          + "which is where your chosen URL, <em>%s</em>, would force us to go. "
          + "To go there, please copy the link into a web browser of your choice." + "</p></body></html>",
          escapedUrlString, escapedUrlString));
      lastUrlString = null;
    }
    backButton.setEnabled(historyIndex > 1);
    forwardButton.setEnabled(historyIndex < history.size());
  }

}
