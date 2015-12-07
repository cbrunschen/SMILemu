package com.brunschen.christian.smil;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.DTDConstants;
import javax.swing.text.html.parser.DocumentParser;
import javax.swing.text.html.parser.ParserDelegator;

public class DocumentationEditorKit extends HTMLEditorKit {

  public static final long serialVersionUID = 0L;

  private static final String dtdName = "html32";
  private static final Map<String, String> extraEntities = new HashMap<String, String>();
  static {
    extraEntities.put("minus", "−");
    extraEntities.put("ndash", "–");
    extraEntities.put("mdash", "—");
    extraEntities.put("larr", "←");
    extraEntities.put("rarr", "→");
    extraEntities.put("rArr", "⇒");
    extraEntities.put("ge", "≥");
    extraEntities.put("and", "∧");
    extraEntities.put("ne", "≠");
    extraEntities.put("diams", "♦");
    extraEntities.put("pi", "π");
  }
  private DTD fixedDtd = null;

  private void fixDtd(DTD dtd) {
    for (Map.Entry<String, String> entry : extraEntities.entrySet()) {
      dtd.defineEntity(entry.getKey(), DTDConstants.CDATA | DTDConstants.GENERAL, new char[] { entry.getValue().charAt(
          0) });
    }
  }

  public DocumentationEditorKit() {
    super();
    getStyleSheet().addRule("td.pad { width:15; }");
    getStyleSheet().addRule("td.padded { padding-right: 2em; }");
  }

  public class Parser extends ParserDelegator {
    public static final long serialVersionUID = 0L;

    public Parser() {
      super();
      try {
        DTD tmp = DTD.getDTD(dtdName);
        fixedDtd = createDTD(tmp, dtdName);
        fixDtd(fixedDtd);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet) throws IOException {
      new DocumentParser(fixedDtd).parse(r, cb, ignoreCharSet);
    }
  }

  @Override
  protected HTMLEditorKit.Parser getParser() {
    return new Parser();
  }

}
