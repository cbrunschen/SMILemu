/*
 * ControlPanel
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.brunschen.christian.graphic.BooleanValue;
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.FalseValue;
import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Graphic;
import com.brunschen.christian.graphic.InverseValue;
import com.brunschen.christian.graphic.Label;
import com.brunschen.christian.graphic.Light;
import com.brunschen.christian.graphic.MultiGraphic;
import com.brunschen.christian.graphic.MultiGraphic.Gravity;
import com.brunschen.christian.graphic.PaddingGraphic;
import com.brunschen.christian.graphic.PointyKnob;
import com.brunschen.christian.graphic.PushButton;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.Solid;
import com.brunschen.christian.graphic.ValueUpdatedListener;
import com.brunschen.christian.smil.graphic.Clock;
import com.brunschen.christian.smil.graphic.Pulse;
import com.brunschen.christian.smil.graphic.Speaker;
import com.brunschen.christian.smil.graphic.Switch;

public class ControlPanel {

  public static double columnWidth = 15.0;
  public static double lightDiameter = 2 * columnWidth / 3;
  public static double fuseLightDiameter = 4 * columnWidth / 5;
  public static double buttonHeight = columnWidth;
  public static double labelHeight = 2 * columnWidth / 3;
  public static double dx = 2.0;
  public static double dy = 2.0;
  public static double ldx = (columnWidth - lightDiameter) / 2;
  public static double ldy = (buttonHeight - lightDiameter) / 2;
  public static double fdx = (columnWidth - fuseLightDiameter) / 2;
  public static double fdy = (buttonHeight - fuseLightDiameter) / 2;
  public static double ady = (columnWidth - labelHeight) / 2;
  public static double columnAdvance = columnWidth + dx;
  public static double gdx = 2 * dx;
  public static double groupAdvance = 4 * columnAdvance;
  public static double groupWidth = groupAdvance - dx;

  public static double leftResetOffset = columnAdvance;
  public static double leftExtraOffset = leftResetOffset + columnAdvance;
  public static double contentOffset = leftExtraOffset + columnAdvance;
  public static double contentAdvance = 40 * columnAdvance + 9 * gdx;
  public static double contentWidth = contentAdvance - dx;
  public static double rightExtraOffset = contentOffset + contentWidth + dx;
  public static double rightResetOffset = rightExtraOffset + columnAdvance;
  public static double rowWidth = rightResetOffset + 2 * columnAdvance - dx;

  public static double clockDiameter = 5 * columnAdvance - dx;

  public static Size lightSize = new Size(lightDiameter, lightDiameter);
  public static Size fuseLightSize = new Size(fuseLightDiameter, fuseLightDiameter);
  public static Size buttonSize = new Size(columnWidth, buttonHeight);
  public static Size labelSize = new Size(columnWidth, labelHeight);
  public static Size bigLabelSize = new Size(groupWidth, labelHeight);
  public static Size switchSize = new Size(columnWidth, 3 * columnWidth);
  public static Size lightRowSize = new Size(rowWidth, lightDiameter + 2 * ldy);
  public static Size buttonRowSize = new Size(rowWidth, buttonHeight);
  public static Size labelRowSize = new Size(rowWidth, labelHeight);
  public static Size thinSolidSize = new Size(rowWidth, 1.0);
  public static Size thickSolidSize = new Size(rowWidth, 3.0);

  public static Color labelColor = Color.BLACK;

  public Font clockFont;
  public Font labelFont;

  protected SMIL smil;
  protected MultiGraphic graphic;

  protected PointyKnob volumeControl;

  private class UpdateGraphic implements ValueUpdatedListener<Integer> {
    private Graphic graphic;

    public UpdateGraphic(Graphic graphic) {
      this.graphic = graphic;
    }

    public void valueUpdated(Integer oldValue, Integer newValue) {
      graphic.repaint();
    }
  }

  public ControlPanel(Font clockFont, Font labelFont) {
    super();
    this.clockFont = clockFont;
    this.labelFont = labelFont;
  }
  
  public void setSmil(SMIL smil) {
    this.smil = smil;
  }

  private Light addLight(MultiGraphic mg, Double offset, String lightText, BooleanValue lightValue, Color lightColor) {
    Light light = new Light(lightSize, lightText, lightValue, lightColor);
    mg.add(new PaddingGraphic(light, ldx, ldy), offset);
    return light;
  }

  private PushButton addButton(MultiGraphic mg, Double offset, String buttonText, PushButton.Listener... listeners) {
    PushButton button = new PushButton(buttonSize, buttonText, listeners);
    mg.add(button, offset);
    return button;
  }

  private Label addLabel(MultiGraphic mg, Double offset, String text) {
    Label label = new Label(labelSize, labelColor, labelFont, text);
    mg.add(label, offset);
    return label;
  }

  private Label addLabel(MultiGraphic mg, Double offset, String main, String subscript) {
    Label label = new Label(labelSize, labelColor, labelFont, main, subscript);
    mg.add(label, offset);
    return label;
  }

  private Light addContentLight(MultiGraphic mg, Double offset, Register r, int bit, String lightText) {
    BooleanValue lightValue = new RegisterBitIsSet(r, bit);
    Light light = addLight(mg, offset, lightText, lightValue, Color.RED);
    r.addValueUpdatedListenerForBit(new UpdateGraphic(light), bit);
    return light;
  }

  private Light addContentLight(MultiGraphic mg, Register r, int bit, String lightText) {
    return addContentLight(mg, null, r, bit, lightText);
  }

  private Light addContentLight(MultiGraphic mg, Double offset, Register r, int bit) {
    return addContentLight(mg, null, r, bit, r.name() + " bit " + bit);
  }

  private Light addContentLight(MultiGraphic mg, Register r, int bit) {
    return addContentLight(mg, null, r, bit);
  }

  private List<Light> addContentLights(MultiGraphic mg, Register r) {
    List<Light> list = new ArrayList<Light>(SMIL.WORD_BITS);

    mg.moveTo(contentOffset);

    // 10 groups of 4 lights and buttons
    for (int i = 0; i < SMIL.WORD_BITS; i++) {
      // add extra space between 4-groups
      if (i > 0 && i % 4 == 0) {
        mg.advance(gdx);
      }

      list.add(addContentLight(mg, r, i));

      // advance to the right
      mg.advance(dx);
    }
    return list;
  }

  private List<PushButton> addContentButtons(MultiGraphic mg, Register r) {
    List<PushButton> list = new ArrayList<PushButton>(SMIL.WORD_BITS);

    mg.moveTo(contentOffset);

    PushButton button;
    // 10 groups of 4 lights and buttons
    for (int i = 0; i < SMIL.WORD_BITS; i++) {
      // add extra space between 4-groups
      if (i > 0 && i % 4 == 0) {
        mg.advance(gdx);
      }

      String buttonText = "Set " + r.name() + " bit " + i;
      button = addButton(mg, null, buttonText, new SetRegisterBit(r, i));
      list.add(button);

      // advance to the right
      mg.advance(dx);
    }
    return list;
  }

  private List<Label> addContentLabels(MultiGraphic mg, Register r) {
    List<Label> list = new ArrayList<Label>(SMIL.WORD_BITS);

    mg.moveTo(contentOffset);

    Label label;
    // 10 groups of 4 lights and buttons
    for (int i = 0; i < SMIL.WORD_BITS; i++) {
      // add extra space between 4-groups
      if (i > 0 && i % 4 == 0) {
        mg.advance(gdx);
      }

      String labelText = "" + i;
      label = addLabel(mg, null, labelText);
      list.add(label);

      // advance to the right
      mg.advance(dx);
    }
    return list;
  }

  private List<PushButton> addResetButtons(MultiGraphic mg, ValueRegister r) {
    // Left reset button
    List<PushButton> list = new ArrayList<PushButton>(2);
    PushButton button = new PushButton(buttonSize, "Clear " + r.name() + " Left", new ClearRegister(mg, r, false));
    mg.add(button, leftResetOffset);
    list.add(button);
    // Right reset button
    button = new PushButton(buttonSize, "Clear " + r.name() + " Right", new ClearRegister(mg, r, true));
    mg.add(button, rightResetOffset);
    list.add(button);
    return list;
  }

  private List<Label> addRegisterLabels(MultiGraphic mg, Register r) {
    List<Label> list = new ArrayList<Label>(4);
    // left labels
    list.add(addLabel(mg, 0.0, r.name()));
    mg.advance(dx);
    list.add(addLabel(mg, leftResetOffset, "Noll"));

    // right labels
    list.add(addLabel(mg, rightResetOffset, "Noll"));
    mg.advance(dx);
    list.add(addLabel(mg, rightResetOffset + columnAdvance, r.name()));
    return list;
  }

  private void addThinDivider(MultiGraphic mg) {
    mg.advance(dy);
    mg.add(new Solid(thinSolidSize, Color.DARK_GRAY));
    mg.advance(dy);
  }

  private void addThickDivider(MultiGraphic mg) {
    mg.advance(3 * dy);
    mg.add(new Solid(thickSolidSize, Color.BLACK));
    mg.advance(3 * dy);
  }

  public MultiGraphic graphic() {
    if (graphic == null) {
      graphic = MultiGraphic.newColumnGraphic();
      MultiGraphic rg;
      
      Processor processor = smil.processor();

      // MD register
      rg = MultiGraphic.newRowGraphic(lightRowSize, Gravity.MIN);
      addResetButtons(rg, processor.md);
      addContentLights(rg, processor.md);
      Light mdExtraLight = addLight(rg, leftExtraOffset, processor.md.name() + " bit 0", new RegisterBitIsSet(processor.md, 0),
          Color.RED);
      processor.md.addValueUpdatedListenerForBit(new UpdateGraphic(mdExtraLight), 0);
      graphic.add(rg);

      graphic.advance(dy);

      rg = MultiGraphic.newRowGraphic(buttonRowSize, Gravity.MIN);
      addContentButtons(rg, processor.md);
      graphic.add(rg);

      addThinDivider(graphic);

      rg = MultiGraphic.newRowGraphic(labelRowSize, Gravity.MIN);
      addContentLabels(rg, processor.md);
      addRegisterLabels(rg, processor.md);
      graphic.add(rg);

      addThickDivider(graphic);

      // AR register
      rg = MultiGraphic.newRowGraphic(lightRowSize, Gravity.MIN);
      addResetButtons(rg, processor.ar);
      addContentLights(rg, processor.ar);
      addContentLight(rg, leftExtraOffset, processor.ar, processor.ar.EXTRA_BIT, processor.ar.name() + " bit 00");
      addContentLight(rg, rightExtraOffset, processor.ar, processor.ar.LOW_BIT, processor.ar.name() + " bit 40");
      graphic.add(rg);

      graphic.advance(dy);

      rg = MultiGraphic.newRowGraphic(buttonRowSize, Gravity.MIN);
      addContentButtons(rg, processor.ar);
      addButton(rg, leftExtraOffset, "Set " + processor.ar.name() + " bit 00", new SetRegisterBit(processor.ar, -1));
      addButton(rg, rightExtraOffset, "Set " + processor.ar.name() + " bit 40", new SetRegisterBit(processor.ar, 40));
      graphic.add(rg);

      addThinDivider(graphic);

      rg = MultiGraphic.newRowGraphic(labelRowSize, Gravity.MIN);
      addContentLabels(rg, processor.ar);
      addRegisterLabels(rg, processor.ar);
      addLabel(rg, leftExtraOffset, "00");
      addLabel(rg, rightExtraOffset, "40");
      graphic.add(rg);

      addThickDivider(graphic);

      // MR register
      rg = MultiGraphic.newRowGraphic(lightRowSize, Gravity.MIN);
      addResetButtons(rg, processor.mr);
      addContentLights(rg, processor.mr);
      addContentLight(rg, rightExtraOffset, processor.mr, 39);
      graphic.add(rg);

      addThinDivider(graphic);

      rg = MultiGraphic.newRowGraphic(labelRowSize, Gravity.MIN);
      addContentLabels(rg, processor.mr);
      addRegisterLabels(rg, processor.mr);
      addLabel(rg, rightExtraOffset, "39");
      graphic.add(rg);

      addThickDivider(graphic);

      // IR register
      rg = MultiGraphic.newRowGraphic(lightRowSize, Gravity.MIN);
      addResetButtons(rg, processor.ir);
      addContentLights(rg, processor.ir);
      graphic.add(rg);

      graphic.advance(dy);

      rg = MultiGraphic.newRowGraphic(buttonRowSize, Gravity.MIN);
      addContentButtons(rg, processor.ir);
      graphic.add(rg);

      addThinDivider(graphic);

      rg = MultiGraphic.newRowGraphic(labelRowSize, Gravity.MIN);
      addRegisterLabels(rg, processor.ir);
      rg.moveTo(contentOffset + groupAdvance + gdx);
      rg.add(new Label(bigLabelSize, labelColor, labelFont, "Adress"));
      rg.advance(groupAdvance + 2 * gdx + dx);
      rg.add(new Label(bigLabelSize, labelColor, labelFont, "Operation"));
      rg.advance(dx + gdx);
      addLabel(rg, null, "e", "1");
      rg.advance(dx);
      addLabel(rg, null, "e", "2");
      rg.advance(dx);
      addLabel(rg, null, "e", "3");
      rg.advance(dx);
      addLabel(rg, null, "e", "4");
      rg.advance(dx);

      rg.advance(2 * gdx + groupAdvance);
      rg.add(new Label(bigLabelSize, labelColor, labelFont, "Adress"));
      rg.advance(groupAdvance + 2 * gdx + dx);
      rg.add(new Label(bigLabelSize, labelColor, labelFont, "Operation"));
      rg.advance(dx + gdx);
      addLabel(rg, null, "e", "1");
      rg.advance(dx);
      addLabel(rg, null, "e", "2");
      rg.advance(dx);
      addLabel(rg, null, "e", "3");
      rg.advance(dx);
      addLabel(rg, null, "e", "4");
      rg.advance(dx);

      graphic.add(rg);

      addThickDivider(graphic);

      // KR and BR registers and other unspecified lights
      rg = MultiGraphic.newRowGraphic(lightRowSize, Gravity.MIN);

      rg.moveTo(leftExtraOffset);
      int dummyBit = 0;
      addContentLight(rg, processor.rr, 0, "Pulse");
      rg.advance(dx);
      addContentLight(rg, processor.dummy, dummyBit++, "R");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "E");
      rg.advance(dx);
      
      rg.advance(columnAdvance);
      dummyBit++;
      
      addContentLight(rg, processor.dummy, dummyBit++, "Z");
      rg.advance(dx);
      
      rg.advance(gdx);
      addContentLight(rg, processor.dummy, dummyBit++, "F");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "Ba");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "Bb");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "K");
      rg.advance(dx);
      
      rg.advance(gdx);
      addContentLight(rg, processor.dummy, dummyBit++, "Ju");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "Br");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "Juv");
      rg.advance(dx);
      
      addContentLight(rg, processor.dummy, dummyBit++, "Juh");
      rg.advance(dx);
      
      rg.advance(gdx + 3 * columnAdvance);

      addButton(rg, null, "Clear KR", new SetRegisterBits(processor.kr, ProgramCounter.RIGHT_BIT, 13, 0));
      rg.advance(dx);
      

      rg.advance(gdx);
      for (int i = 11; i >= 0; i--) {
        addContentLight(rg, processor.kr, i, "KR bit " + i);
        rg.advance(dx);
        if (i > 0 && i % 4 == 0) {
          rg.advance(gdx);
        }
      }
      addContentLight(rg, processor.kr, ProgramCounter.RIGHT_BIT, "KR bit 00");
      rg.advance(dx);
      
      rg.advance(gdx);
      rg.advance(columnAdvance);
      
      Light krVLight = addLight(rg, null, "V", new InverseValue(new RegisterBitIsSet(processor.kr, ProgramCounter.RIGHT_BIT)), Color.RED);
      rg.advance(dx);
      
      processor.kr.addValueUpdatedListenerForBit(new UpdateGraphic(krVLight), ProgramCounter.RIGHT_BIT);
      addContentLight(rg, processor.kr, ProgramCounter.RIGHT_BIT, "H");
      rg.advance(dx);
      
      rg.advance(gdx);
      addContentLight(rg, processor.dummy, dummyBit++, "Jh");
      rg.advance(dx);
      
      rg.advance(columnAdvance);
      
      for (int i = 5; i >= 0; i--) {
        addContentLight(rg, processor.br, i, "BR bit " + i);
        rg.advance(dx);
        if (i > 0 && i % 4 == 0) {
          rg.advance(gdx);
        }
      }

      rg.advance(columnAdvance);
      addButton(rg, null, "Clear BR", new SetRegisterBits(processor.br, 0, 6, 0L));
      graphic.add(rg);

      graphic.advance(dy);

      rg = MultiGraphic.newRowGraphic(buttonRowSize, Gravity.MIN);
      dummyBit = 0;
      addButton(rg, contentOffset, "R", new SetRegisterBit(processor.dummy, dummyBit++));
      rg.advance(dx);
      
      addButton(rg, null, "E", new SetRegisterBit(processor.dummy, dummyBit++));
      rg.advance(dx);
      
      addButton(rg, null, "", new SetRegisterBit(processor.dummy, dummyBit++));
      rg.advance(dx);
      
      addButton(rg, null, "Z", new SetRegisterBit(processor.dummy, dummyBit++));
      rg.advance(dx);
      
      rg.advance(gdx);
      addButton(rg, null, "F", new SetRegisterBit(processor.dummy, dummyBit++));
      rg.advance(dx);
      
      rg.advance(7 * columnAdvance + 2 * gdx);
      addButton(rg, null, "Återställ", new PushButton.Listener() {        
        public void buttonPushed(PushButton button) {
          smil.reset();
        }
        public void buttonReleased(PushButton button) {
        }
      });

      rg.advance(dx);
      
      rg.advance(3 * columnAdvance + gdx);
      for (int i = 11; i >= 0; i--) {
        addButton(rg, null, "Set KR bit " + i, new SetRegisterBit(processor.kr, i));
        rg.advance(dx);
        
        if (i > 0 && i % 4 == 0) {
          rg.advance(gdx);
        }
      }
      addButton(rg, null, "Set KR bit 00", new SetRegisterBit(processor.kr, ProgramCounter.RIGHT_BIT));
      rg.advance(dx);
      
      rg.advance(gdx + columnAdvance);
      
      addButton(rg, null, "Set V", new SetRegisterBit(processor.kr, ProgramCounter.RIGHT_BIT, false));
      rg.advance(dx);
      
      addButton(rg, null, "Set H", new SetRegisterBit(processor.kr, ProgramCounter.RIGHT_BIT));
      rg.advance(dx);
      
      rg.advance(gdx + 2 * columnAdvance);
      
      for (int i = 5; i >= 0; i--) {
        addButton(rg, null, "Set BR bit " + i, new SetRegisterBit(processor.br, i));
        rg.advance(dx);
        
        if (i > 0 && i % 4 == 0) {
          rg.advance(gdx);
        }
      }
      
      graphic.add(rg);

      addThinDivider(graphic);

      rg = MultiGraphic.newRowGraphic(labelRowSize, Gravity.MIN);
      rg.add(new Pulse(labelSize), leftExtraOffset);
      rg.advance(dx);
      addLabel(rg, null, "R");
      rg.advance(dx);
      addLabel(rg, null, "E");
      rg.advance(dx);
      rg.advance(columnAdvance);
      addLabel(rg, null, "Z");
      rg.advance(dx);
      rg.advance(gdx);
      addLabel(rg, null, "F");
      rg.advance(dx);
      addLabel(rg, null, "B", "A");
      rg.advance(dx);
      addLabel(rg, null, "B", "B");
      rg.advance(dx);
      addLabel(rg, null, "B", "K");
      rg.advance(dx);
      rg.advance(gdx);
      addLabel(rg, null, "J", "U");
      rg.advance(dx);
      addLabel(rg, null, "B", "R");
      rg.advance(dx);
      addLabel(rg, null, "J", "UV");
      rg.advance(dx);
      addLabel(rg, null, "J", "UH");
      rg.advance(dx);
      rg.advance(gdx);

      MultiGraphic cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      // System.err.format("Återställ columnGraphic bounds = %s, frame = %s\n",
      // Util.printRect(cg.bounds()), Util.printRect(cg.bounds()));
      cg.add(new Label(new Size(columnWidth, labelHeight / 2), labelColor, labelFont, "Åter-"));
      cg.add(new Label(new Size(columnWidth, labelHeight / 2), labelColor, labelFont, "ställ"));
      // System.err.format("Återställ columnGraphic bounds = %s, frame = %s\n",
      // Util.printRect(cg.bounds()), Util.printRect(cg.bounds()));
      rg.add(cg);
      rg.advance(dx + columnAdvance / 2);
      addLabel(rg, null, "KR");
      rg.advance(columnAdvance / 2 + dx);
      addLabel(rg, null, "Noll");
      rg.advance(dx);
      rg.advance(gdx);
      for (int i = 11; i >= 0; i--) {
        addLabel(rg, null, "" + i);
        rg.advance(dx);
        if (i > 0 && i % 4 == 0) {
          rg.advance(gdx);
        }
      }
      addLabel(rg, null, "00");
      rg.advance(dx);
      rg.advance(dx);
      addLabel(rg, null, "KR");
      rg.advance(dx);
      rg.advance(dx);
      addLabel(rg, null, "V");
      rg.advance(dx);
      addLabel(rg, null, "H");
      rg.advance(dx);
      rg.advance(gdx);
      addLabel(rg, null, "J", "H");
      rg.advance(dx);
      addLabel(rg, null, "BR");
      rg.advance(dx);
      addLabel(rg, null, "5");
      rg.advance(dx);
      addLabel(rg, null, "4");
      rg.advance(dx);
      rg.advance(gdx);
      addLabel(rg, null, "3");
      rg.advance(dx);
      addLabel(rg, null, "2");
      rg.advance(dx);
      addLabel(rg, null, "1");
      rg.advance(dx);
      addLabel(rg, null, "0");
      rg.advance(dx);
      addLabel(rg, null, "BR");
      rg.advance(dx);
      addLabel(rg, null, "Noll");

      graphic.add(rg);

      addThickDivider(graphic);

      // now the bottom panel
      rg = MultiGraphic.newRowGraphic(new Size(rowWidth, 0.0), Gravity.MIN);
      // the clock
      rg.advance(columnAdvance);
      final Clock clock = new Clock(new Size(clockDiameter, clockDiameter), clockFont);
      rg.add(clock);
      // once every few (5) seconds, update the clock 
      Timer timer = new Timer();
      timer.schedule(new TimerTask() {
        @Override public void run() {
          clock.repaint();
        }
      }, 5000, 5000);
      rg.advance(dx + columnAdvance + gdx);

      // first fuse indicator, GL switches
      cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      cg.add(new PaddingGraphic(new Light(fuseLightSize, "Fuse 1", new FalseValue(), Color.RED), fdx, fdy));
      cg.advance(columnAdvance);
      cg.add(new PushButton(buttonSize, "GL On"));
      cg.advance(columnAdvance);
      cg.add(new PushButton(buttonSize, "GL Off"));
      cg.advance(dy);
      cg.add(new Label(labelSize, labelColor, labelFont, "GL"));
      rg.add(cg);
      rg.advance(dx + columnAdvance);

      // first fuse indicator, GL switches
      cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      cg.add(new PaddingGraphic(new Light(fuseLightSize, "Fuse 2", new FalseValue(), Color.RED), fdx, fdy));
      cg.advance(columnAdvance);
      cg.add(new PushButton(buttonSize, "MS On"));
      cg.advance(columnAdvance);
      cg.add(new PushButton(buttonSize, "MS Off"));
      cg.advance(dy);
      cg.add(new Label(labelSize, labelColor, labelFont, "MS"));
      rg.add(cg);
      rg.advance(dx);

      cg = MultiGraphic.newColumnGraphic(Gravity.MIN);
      cg.advance(2 * columnWidth + dy);
      cg.add(new PaddingGraphic(new Label(new Size(columnAdvance + gdx, labelHeight), labelColor, labelFont, "Till"), 0.0, ady));
      cg.advance(columnAdvance);
      cg.add(new PaddingGraphic(new Label(new Size(columnAdvance + gdx, labelHeight), labelColor, labelFont, "Från"), 0.0, ady));
      rg.add(cg);

      // a row of 5 fuse lights
      cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      {
        MultiGraphic rg2 = MultiGraphic.newRowGraphic(Gravity.MIN);
        for (int i = 3; i <= 7; i++) {
          rg2.add(new PaddingGraphic(new Light(fuseLightSize, "Fuse " + i, new FalseValue(), Color.RED), fdx, fdy));
          if (i < 7) {
            rg2.advance(columnWidth);
          }
        }
        cg.add(rg2);
      }
      cg.advance(dy + columnAdvance);
      cg.add(new Label(new Size(5 * columnWidth, labelHeight), labelColor, labelFont, "Säkring"));
      rg.add(cg);
      rg.advance(2 * (groupAdvance + gdx) - 9 * columnWidth + columnAdvance);

      // speaker
      double speakerDiameter = 4 * columnAdvance + gdx;
      rg.add(new Speaker(new Size(speakerDiameter, speakerDiameter)));
      rg.advance(dx + columnAdvance);

      // volume control
      double volumeControlDiameter = 4 * columnAdvance + gdx;
      rg.add(volumeControl = new PointyKnob(new Size(volumeControlDiameter, volumeControlDiameter), -0.8 * Math.PI,
          0.8 * Math.PI, smil.volume()));
      volumeControl.addValueUpdatedListener(smil.volumeUpdatedListener());
      rg.advance(dx + columnAdvance);

      // first toggle switch
      cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      cg.add(new Label(bigLabelSize, labelColor, labelFont, "Stegvis"));
      cg.add(new Switch(switchSize, Switch.State.NEUTRAL, true, new Switch.Listener() {
        public void onSwitched(Switch sw) {
          switch (sw.getState()) {
            case UP:
              smil.setRunMode(false, false);
              break;
            case NEUTRAL:
              smil.setRunMode(true, false);
              break;
            case DOWN:
              smil.setRunMode(true, true);
              break;
          }
        }
      }));
      cg.add(new Label(bigLabelSize, labelColor, labelFont, "Stopp"));
      cg.add(new Label(bigLabelSize, labelColor, labelFont, "Villk."));
      rg.add(cg);

      // second toggle switch
      cg = MultiGraphic.newColumnGraphic(Gravity.MID);
      cg.add(new Label(bigLabelSize, labelColor, labelFont, "Remsstart"));
      cg.add(new Switch(switchSize, Switch.State.NEUTRAL, false, new Switch.Listener() {
        public void onSwitched(Switch sw) {
          switch (sw.getState()) {
            case UP:
              smil.tapeStart();
              break;
            case NEUTRAL:
              // no-op
              break;
            case DOWN:
              smil.start();
              break;
          }
        }
      }));
      cg.add(new Label(bigLabelSize, labelColor, labelFont, "Start"));
      rg.add(cg);

      graphic.add(rg);
      // pad the whole thing just a little
      graphic.pad(5, 5, 5, 5);

    }
    return graphic;
  }

  public void onSizeChanged(Size oldSize, Size newSize) {
  }

    public double volume() {
    return volumeControl.position();
  }

}
