/*
 * Tape
 *
 * Copyright (C) 2005  Christian Brunschen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package com.brunschen.christian.smil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tape {

  public static class Entry {
    private int width = 1; // by default; may be overwritten when the graphic investigates the tape

    public boolean hasContent() {
      return hasText() || hasValue();
    }

    public boolean hasText() {
      return false;
    }

    public List<String> text() {
      return null;
    }

    public boolean hasValue() {
      return false;
    }

    public int value() {
      return -1;
    }
    
    public void setWidth(int width) {
      this.width = width;
    }

    public int width() {
      return width;
    }
  }

  public static class Value extends Entry {
    private int value;

    public Value(int v) {
      value = v;
    }

    @Override
    public boolean hasValue() {
      return true;
    }

    @Override
    public int value() {
      return value;
    }
  }

  public static class Space extends Entry {
  }

  public static class Text extends Entry {
    private List<String> text = new LinkedList<String>();

    @Override
    public boolean hasText() {
      return true;
    }

    @Override
    public List<String> text() {
      return text;
    }
  }

  private List<Entry> entries;

  public Tape() {
    super();
    entries = null;
  }

  public Tape(BufferedReader r) {
    this();
    load(r);
  }

  public Tape(Reader r) {
    this();
    load(r);
  }

  public Tape(File f) {
    this();
    load(f);
  }

  public Tape(List<String> lines) {
    this();
    load(lines);
  }

  public void load(File f) {
    try {
      load(new FileReader(f));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void load(Reader r) {
    load(new BufferedReader(r));
  }

  public void load(BufferedReader r) {
    String line;
    List<String> lines = new ArrayList<String>();
    try {
      while ((line = r.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    load(lines);
  }

  public void load(List<String> lines) {
    int n = Math.min(4, lines.size());
    for (int i = 0; i < n; i++) {
      if (lines.get(i).endsWith("#")) {
        loadOld(lines);
        return;
      }
    }
    loadNew(lines);
  }

  public Entry loadLine(List<Entry> entries, Entry lastEntry, String line) {
    for (int i = 0; i < line.length(); i++) {
      int c = line.charAt(i);
      if (Character.isWhitespace(c)) {
        if (lastEntry != null && lastEntry.hasContent()) {
          entries.add(lastEntry = new Space());
        }
      } else {
        int value = Character.digit(c, 16);
        if (0 <= value && value < 16) {
          entries.add(lastEntry = new Value(value));
        } else {
          System.err.format("invalid character '%c' on line '%s', skipping rest of line", c, line);
        }
      }
    }
    entries.add(lastEntry = new Space());
    return lastEntry;
  }

  private static String commentPatternString = "#\\s*(.*)$";
  private Pattern commentPattern = Pattern.compile(commentPatternString);
  private Pattern commentLinePattern = Pattern.compile("^" + commentPatternString);

  public void loadNew(List<String> lines) {
    // System.err.format("Loading File New-Style\n");
    entries = new ArrayList<Entry>();
    entries.add(new Space());
    Text text = null;
    Entry lastEntry = null;
    Matcher m;
    for (String line : lines) {
      if ((m = commentLinePattern.matcher(line)).matches()) {
        // add this line to any preceding ones
        if (text == null) {
          text = new Text();
        }
        text.text().add(m.group(1));
      } else {
        // if there were any preceding comments, push them to the tape as a block
        if (text != null) {
          entries.add(text);
          text = null;
          entries.add(lastEntry = new Space());
        }

        String lineData = line;
        // if there's a comment, on this line, put it on the tape preceding the data on the same
        // line
        if ((m = commentPattern.matcher(line)).find()) {
          text = new Text();
          text.text().add(m.group(1));
          entries.add(text);
          text = null;
          entries.add(lastEntry = new Space());

          // and strip the comments off the line
          lineData = line.substring(0, m.start());
        }

        lastEntry = loadLine(entries, lastEntry, lineData);
      }
    }
    if (text != null) {
      entries.add(lastEntry = text);
    } else {
    }
  }

  public void loadOld(List<String> lines) {
    // System.err.format("Loading File Old-Style\n");
    // read the comments first
    entries = new ArrayList<Entry>();
    entries.add(new Space());
    Text text = new Text();
    int i = 0;
    for (; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.endsWith("#")) {
        text.text.add(line.substring(0, line.length() - 1));
        break;
      } else {
        text.text.add(line);
      }
    }
    Entry lastEntry = null;
    for (i++; i < lines.size(); i++) {
      lastEntry = loadLine(entries, lastEntry, lines.get(i));
    }
    entries.add(new Space());
    entries.add(text);
  }

  public Entry get(int i) {
    return entries.get(i);
  }

  public int length() {
    return entries == null ? -1 : entries.size();
  }

  public List<Integer> rows() {
    List<Integer> rows = new ArrayList<Integer>();
    for (Entry entry : entries) {
      if (entry != null && entry.hasValue()) {
        rows.add(((Value) entry).value);
      }
    }
    return rows;
  }

  public List<Long> words() {
    List<Integer> rows = rows();
    List<Long> words = new ArrayList<Long>();
    int wholeWords = rows.size() / 10;
    int extraRows = rows.size() % 10;
    for (int i = 0; i < wholeWords; i++) {
      long word = 0L;
      for (int j = 0; j < 10; j++) {
        word <<= 4;
        word |= rows.get(10 * i + j) & 0xf;
      }
      words.add(word);
    }
    if (extraRows > 0) {
      long word = 0L;
      for (int j = 0; j < extraRows; j++) {
        word <<= 4;
        word |= rows.get(10 * wholeWords + j) & 0xf;
      }
      word <<= 4 * (10 - extraRows);
      words.add(word);
    }
    return words;
  }
}
