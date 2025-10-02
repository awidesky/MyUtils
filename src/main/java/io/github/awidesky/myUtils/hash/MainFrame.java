package io.github.awidesky.myUtils.hash;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import io.github.awidesky.myUtils.hash.FileHash.HashInfo;
import io.github.awidesky.myUtils.hash.MyJTree.HashTreeNodeObject;

public class MainFrame extends JFrame {
	private final PrintWriter out;
	private final FileHash hasher;
	private final JTextArea logArea = new JTextArea();
	private Path dir1, dir2;
	private MyJTree tree1, tree2;

	public MainFrame() {
		super("Directory Compare");

		// redirect PrintWriter out -> logArea
		out = new PrintWriter(new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) {
				SwingUtilities.invokeLater(() -> {
					logArea.append(new String(cbuf, off, len));
					logArea.setCaretPosition(logArea.getDocument().getLength());
				});
			}
			@Override public void flush() {}
			@Override public void close() {}
		}, false);
		hasher = new FileHash(out);
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
		this.dir1 = chooser.getSelectedFile().toPath();
		this.tree1 = new MyJTree(dir1.toFile());
		
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
		this.dir2 = chooser.getSelectedFile().toPath();
		this.tree2 = new MyJTree(dir2.toFile());

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 700);
		setLocationRelativeTo(null);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Log", createLogPanel());
		tabs.addTab("Directory 1", createDirPanel(tree1, dir1, 1));
		tabs.addTab("Directory 2", createDirPanel(tree2, dir2, 2));
		add(tabs, BorderLayout.CENTER);
		setVisible(true);

		new Thread(() -> {
			try {
				boolean same = hasher.compareTwoDirectories(dir1, hashCallback(dir, ), dir2, null);
				out.println("IsSame : " + same);
			} catch (IOException e) {
				e.printStackTrace(out);
			}
		}).start();
	}

	private JPanel createLogPanel() {
		logArea.setEditable(false);
		JScrollPane scroll = new JScrollPane(logArea);
		JPanel p = new JPanel(new BorderLayout());
		p.add(scroll, BorderLayout.CENTER);
		return p;
	}

	private JPanel createDirPanel(final MyJTree tree, Path dir, int dirIndex) {
		Arrays.stream(dir.toFile().listFiles()).sorted(Comparator.comparing(File::getName))
			.forEach(c -> buildTree(tree.getModel(), "", tree.getRoot(), c));
		ToolTipManager.sharedInstance().registerComponent(tree);

		JButton openBtn = new JButton("Show in Explorer");
		openBtn.addActionListener(ev -> {
			TreePath path = tree.getSelectionPath();
			Path p = path == null ? dir : Paths.get(dir.toString(), Arrays.copyOfRange((String[]) path.getPath(), 1, path.getPath().length));
			try {
				Desktop.getDesktop().open((Files.isDirectory(p) ? p : p.getParent()).toFile());
			} catch (IOException e) {
				e.printStackTrace(out);
			}
		});

		JPanel p = new JPanel(new BorderLayout());
		p.add(new JScrollPane(tree), BorderLayout.CENTER);
		p.add(openBtn, BorderLayout.SOUTH);
		return p;
	}
	
	private void buildTree(DefaultTreeModel treeModel, String parentVirtualPath, DefaultMutableTreeNode parentNode, File file) {
		//if(file.isHidden()) return;
		String relativePath = parentVirtualPath + file.getName();
		HashTreeNodeObject n = new HashTreeNodeObject(file.getName(), file);
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(n);
		treeModel.insertNodeInto(node, parentNode, parentNode.getChildCount());
		
		if (file.isDirectory()) {
			Arrays.stream(file.listFiles()).sorted(Comparator.comparing(File::getName))
				.forEach(c -> buildTree(treeModel, relativePath + "/", node, c));
		}
	}

	private HashInfo findHashInList(HashInfo h, List<HashInfo> list) {
		for (HashInfo hi : list) {
			if (hi.relativePath().equals(h.relativePath())) return hi;
		}
		return null;
	}

}
