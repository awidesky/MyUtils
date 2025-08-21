/*
 * Copyright (c) 2025 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.myUtils;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class FontTest extends JFrame {
	private static final long serialVersionUID = 9090152451051763582L;
	
	private static final String TEST_STRING = "\uD83C\uDF9E\uD83C\uDFB5"; //🎞🎵

    public FontTest() {
        setTitle("Font Test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 800);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        List<JComponent> canDisplay = new LinkedList<>();
        List<JComponent> canNotDisplay = new LinkedList<>();

        for (Font font : fonts) {
            try {
                font = font.deriveFont(Font.PLAIN, 24f);

                JLabel text = new JLabel(TEST_STRING);
                text.setFont(font);
                JLabel name = new JLabel(font.getFontName() + "(" + font.getName() + ")");
                if(font.canDisplayUpTo(name.getText()) == -1) name.setFont(font);
                
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
                p.add(text);
                p.add(Box.createHorizontalStrut(10));
                p.add(name);
                
                if (font.canDisplayUpTo(TEST_STRING) == -1) {
                    text.setForeground(Color.BLACK);
                    canDisplay.add(p);
                } else {
                    text.setForeground(Color.RED);
                    canNotDisplay.add(p);
                }

            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        canDisplay.forEach(panel::add);
        canNotDisplay.forEach(panel::add);

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FontTest::new);
    }
}
