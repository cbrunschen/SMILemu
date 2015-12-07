package com.brunschen.christian.smil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Doc {

  private static Map<URL, Doc> cache = new HashMap<URL, Doc>();

  private String charset = null;

  private URL url;
  private String contentType;

  public Doc(URL url, String contentType) {
    super();
    this.url = url;
    this.contentType = contentType;
  }

  public URL url() {
    return url;
  }

  public String contentType() {
    return contentType;
  }

  public static Doc doc(URL url) throws IOException {
    Doc doc = cache.get(url);
    if (doc == null) {
      URLConnection conn = url.openConnection();
      String contentType = conn.getContentType();
      InputStream is = url.openStream();
      doc = new Doc(url, contentType);
      doc.read(is);
      cache.put(url, doc);
    }
    return doc;
  }

  public static final String Header = "Header";
  public static final String Footer = "Footer";
  public static final String PartSeparator = "$";
  public static final char Separator = '!';

  private Map<String, String> partData = new HashMap<String, String>();
  private Map<String, Integer> partIndexes = new HashMap<String, Integer>();
  private List<String> parts = new ArrayList<String>();
  private String header = "", footer = "";

  public class TocNode {
    private TocNode parent;
    private String name;
    private List<TocNode> children = new ArrayList<TocNode>();
    private Map<String, TocNode> childrenByName = new HashMap<String, TocNode>();

    public TocNode(TocNode parent, String name) {
      this.parent = parent;
      this.name = name;
    }

    public void addChild(TocNode child) {
      children.add(child);
      childrenByName.put(child.name(), child);
    }

    public String name() {
      return name;
    }

    public List<TocNode> children() {
      return children;
    }

    public TocNode child(String name) {
      return childrenByName.get(name);
    }

    public TocNode makeChild(String name) {
      TocNode child = child(name);
      if (child == null) {
        child = new TocNode(this, name);
        addChild(child);
      }
      return child;
    }

    public String path() {
      if (parent != null && parent.parent != null) {
        return parent.path() + Separator + name();
      } else {
        return name();
      }
    }

    public void appendTo(StringBuilder sb) {
      if (name != null) {
        sb.append(name);
      }
      if (children.size() > 0) {
        sb.append(":(");
        boolean first = true;
        for (TocNode child : children()) {
          if (first) {
            first = false;
          } else {
            sb.append(",");
          }
          child.appendTo(sb);
        }
        sb.append(")");
      }
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      appendTo(sb);
      return sb.toString();
    }
  }

  private TocNode tocRoot = new TocNode(null, null);

  static Pattern markerPattern = Pattern.compile("<!-- #([^#]*)# -->");
  static Pattern anchorPattern = Pattern.compile("<a\\s+name=\"([a-zA-Z0-9]+)\">");
  static Pattern localRefPattern = Pattern.compile("<a\\s+href=\"(#)([a-zA-Z0-9]+)\">");
  static Pattern charsetPattern = Pattern.compile("\\s*charset\\s*=\\s*\"([0-9a-zA-Z+-]*)\"");

  public void read(InputStream is) throws IOException {
    Map<String, String> partsByAnchor = new HashMap<String, String>();
    String currentPart = Header;
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    StringBuilder sb = new StringBuilder();
    while ((line = br.readLine()) != null) {
      Matcher m = markerPattern.matcher(line);
      if (m.matches()) {
        addPart(currentPart, sb.toString());
        currentPart = m.group(1);
        sb = new StringBuilder();
      } else {
        m = anchorPattern.matcher(line);
        int startAt = 0;
        while (m.find(startAt)) {
          partsByAnchor.put(m.group(1), currentPart);
          startAt = m.end();
        }
        m = charsetPattern.matcher(line);
        if (m.find()) {
          charset = m.group(1);
          sb.append(line.substring(0, m.start()));
          sb.append(line.substring(m.end()));
        } else {
          sb.append(line);
        }
        sb.append("\n");
      }
    }
    addPart(currentPart, sb.toString());
    for (String part : parts) {
      sb = new StringBuilder();
      String data = dataForPart(part);
      Matcher m = localRefPattern.matcher(data);
      int startAt = 0;
      while (m.find(startAt)) {
        String anchor = m.group(2);
        String matchingPart = partsByAnchor.get(anchor);
        if (part.equals(matchingPart)) {
          sb.append(data.substring(startAt, m.end()));
        } else {
          sb.append(data.substring(startAt, m.start(1)));
          sb.append(url());
          sb.append(PartSeparator);
          sb.append(matchingPart);
          sb.append('#');
          sb.append(anchor);
          sb.append(data.substring(m.end(2), m.end()));
        }
        startAt = m.end();
      }
      sb.append(data.substring(startAt));
      partData.put(part, sb.toString());
    }
  }

  public void addPart(String part, String data) {
    if (Header.equals(part)) {
      header = data;
    } else if (Footer.equals(part)) {
      footer = data;
    } else {
      int partIndex = parts.size();
      parts.add(part);
      partData.put(part, data);
      partIndexes.put(part, partIndex);
      int startAt = 0;
      int separatorIndex;
      TocNode parent = tocRoot;
      String namePart;
      while ((separatorIndex = part.indexOf(Separator, startAt)) >= 0) {
        namePart = part.substring(startAt, separatorIndex);
        parent = parent.makeChild(namePart);
        startAt = separatorIndex + 1;
      }
      namePart = part.substring(startAt);
      parent.makeChild(namePart);
    }
  }

  public String navForPart(String part) {
    if (parts.size() <= 1) {
      return "";
    }
    Integer index = partIndexes.get(part);
    StringBuilder sb = new StringBuilder();
    sb.append("<table border=\"0\" align=\"center\"><tr><td align=\"left\">");
    if (index > 0) {
      sb.append(String.format("<a href=\"%s%s%s\">Previous</a>", url(), PartSeparator, parts.get(index - 1)));
    } else {
      sb.append("<font color=\"#7f7f7f\">Previous</font>");
    }
    sb.append("</td><td width=\"15\"></td><td align=\"right\">");
    if (index < parts.size() - 1) {
      sb.append(String.format("<a href=\"%s%s%s\">Next</a>", url(), PartSeparator, parts.get(index + 1)));
    } else {
      sb.append("<font color=\"#7f7f7f\">Next</font>");
    }
    sb.append("</td></tr></table>\n");
    return sb.toString();
  }

  public List<String> parts() {
    return parts;
  }

  public String dataForPart(String part) {
    return partData.get(part);
  }

  public String part(String part) {
    return header + dataForPart(part) + navForPart(part) + footer;
  }

  public byte[] data(String ident) {
    try {
      return part(ident).getBytes(charset != null ? charset : "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public TocNode tocRoot() {
    return tocRoot;
  }

  public String defaultIdent() {
    return parts.size() > 1 ? parts.get(0) : "any";
  }

}
