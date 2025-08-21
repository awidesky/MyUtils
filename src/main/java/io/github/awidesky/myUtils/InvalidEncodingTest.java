package io.github.awidesky.myUtils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class InvalidEncodingTest extends JFrame {

	private static final long serialVersionUID = -2081108661521027382L;
	
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JTextArea inputTextArea = new JTextArea();
	private JComboBox<Charset> charsetComboBox;
	private JButton processButton;
	private boolean decodingWithTab = true;
	
	private final Map<String, Charset> encodings = new HashMap<>(Map.of(
		    "UTF-8", Charset.forName("UTF-8"),
		    "EUC-KR", Charset.forName("EUC-KR"),
		    "CP949", Charset.forName("windows-949"),
		    "Shift_JIS", Charset.forName("Shift_JIS"),
		    "EUC-JP", Charset.forName("EUC-JP"),
		    "ASCII", Charset.forName("US-ASCII"),
		    "Windows-1252", Charset.forName("windows-1252")
		));

	public static void main(String[] args) {
		SwingUtilities.invokeLater(InvalidEncodingTest::new);
	}

	public InvalidEncodingTest() {
		setTitle("Encoding Viewer (Encode with combobox, decode with tab name)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);

		JPanel inputPanel = new JPanel(new BorderLayout());

		inputTextArea.setLineWrap(true);
		inputTextArea.setWrapStyleWord(true);
		inputTextArea.setFont(UIManager.getFont("Label.font"));
		JScrollPane inputScrollPane = new JScrollPane(inputTextArea);
		inputPanel.add(inputScrollPane, BorderLayout.CENTER);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		charsetComboBox = new JComboBox<>(Charset.availableCharsets().values().toArray(Charset[]::new));
		charsetComboBox.setSelectedItem(StandardCharsets.UTF_8);

		processButton = new JButton("Process");
		processButton.addActionListener(e -> transcode());
		
		JButton switchButton = new JButton("switch mode");
		switchButton.addActionListener(e -> {
			decodingWithTab = !decodingWithTab;
			if(decodingWithTab) {
				setTitle("Encoding Viewer (Encode with combobox, decode with tab name)");
			} else {
				setTitle("Encoding Viewer (Encode with tab name, decode with combobox)");
			}
		});

		controlPanel.add(charsetComboBox);
		controlPanel.add(processButton);
		controlPanel.add(switchButton);

		inputPanel.add(controlPanel, BorderLayout.SOUTH);

		tabbedPane.addTab("Input", inputPanel);
		tabbedPane.addChangeListener(e -> transcode());
		
		for (Map.Entry<String, Charset> entry : encodings.entrySet()) {
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setFont(UIManager.getFont("Label.font"));

			JScrollPane scrollPane = new JScrollPane(textArea);
			tabbedPane.addTab(entry.getKey(), scrollPane);
		}

		add(tabbedPane, BorderLayout.CENTER);
		setLocationRelativeTo(null); // center on screen
		setVisible(true);
	}

	private void transcode() {
		if(decodingWithTab) {
			decodeWithTab();
		} else {
			encodeWithTab();
		}
	}
	
	private void decodeWithTab() {
		byte[] data = inputTextArea.getText().getBytes((Charset) charsetComboBox.getSelectedItem());

		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			String tabTitle = tabbedPane.getTitleAt(i);

			if ("Input".equals(tabTitle)) {
				continue;
			}

			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof JScrollPane scrollPane) {
				JViewport viewport = scrollPane.getViewport();
				Component view = viewport.getView();
				if (view instanceof JTextArea textArea) {
					try {
						Charset charset = encodings.get(tabTitle);
						String decoded = new String(data, charset);
						textArea.setText(decoded);
					} catch (Exception ex) {
						textArea.setText("Error decoding with " + tabTitle + " : " + ex.getMessage());
					}
				}
			}
		}
	}
	
	private void encodeWithTab() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			String tabTitle = tabbedPane.getTitleAt(i);
			
			if ("Input".equals(tabTitle)) {
				continue;
			}
			
			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof JScrollPane scrollPane) {
				JViewport viewport = scrollPane.getViewport();
				Component view = viewport.getView();
				if (view instanceof JTextArea textArea) {
					try {
						Charset charset = encodings.get(tabTitle);
						byte[] data = inputTextArea.getText().getBytes(charset);
						String decoded = new String(data, (Charset) charsetComboBox.getSelectedItem());
						textArea.setText(decoded);
					} catch (Exception ex) {
						textArea.setText("Error decoding with " + charsetComboBox.getSelectedItem() + " : " + ex.getMessage());
					}
				}
			}
		}
	}

}
