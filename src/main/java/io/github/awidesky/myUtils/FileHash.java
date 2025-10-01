package io.github.awidesky.myUtils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;

/*
 * Copyright (c) 2025 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


public class FileHash {
	
	static PrintWriter out;
	
	public static void main(String[] args) throws IOException {
		out = new PrintWriter(System.out, true);
		
		boolean re = compareTwoDirectories(
				Paths.get("/Users/eugenehong/Documents/인하/3-1"), null,
				Paths.get("/Users/eugenehong/Documents/인하/3-1"), null
				);
		out.println("IsSame : " + re);
		
		//SwingUtilities.invokeLater(MainFrame::new);
	}
	
	public static boolean compareTwoDirectories(Path d1, Consumer<HashInfo> p1, Path d2, Consumer<HashInfo> p2) throws IOException {
		FutureTask<ArrayList<HashInfo>> f1 = new FutureTask<ArrayList<HashInfo>>(() -> hash(d1, p1));
		FutureTask<ArrayList<HashInfo>> f2 = new FutureTask<ArrayList<HashInfo>>(() -> hash(d2, p2));
		new Thread(f1).start();
		new Thread(f2).start();
		
		ArrayList<HashInfo> l1;
		ArrayList<HashInfo> l2;
		try {
			l1 = f1.get();
			l2 = f2.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			l1 = hash(d1, p1);
			l2 = hash(d2, p2);
		}
		
		boolean ret = true;
		
		if(l1.size() != l2.size()) {
			out.println("Entries number mismatch! (%s : %d entries, %s : %d entries)"
					.formatted(d1.toString(), l1.size(), d2.toString(), l2.size()));
		}
		out.println(l1.size() + " files");
		out.println();
		
		long time = System.currentTimeMillis();
		HashMap<Path, HashInfo> s = new HashMap<>();
		l1.stream().filter(h -> !"*directory".equals(h.hash)).forEach(h -> s.put(h.relativePath, h));
		out.println("Missing entry in " + d1 + " : ");
		for(HashInfo h : l2) {
			if("*directory".equals(h.hash)) continue;
			
			HashInfo i = s.get(h.relativePath);
			if(h.equals(i)) {
				s.remove(h.relativePath);
				continue;
			}
			
			ret = false;
			if(i == null) {
				out.println("  Not exist : " + h);
			} else {
				out.println("  Hash diff : " + i.hashAndFullPath(d1)); //TODO : same hash, different name!
				out.println("       with : " + h.hashAndFullPath(d2));
				s.remove(h.relativePath);
			}
		}
		out.println("======");
		
		out.println("Missing entry in " + d2 + " : ");
		for(HashInfo h : s.values()) {
			ret = false;
			out.println("\t" + h);
		}
		out.println("======");
		out.println("Entry compare : " + (System.currentTimeMillis() - time) + "ms");
		
		return ret;
	}
	
	
	public static <T> T findFirstDuplacate(List<T> list) {
		Set<T> set = new HashSet<T>();
	    for (T each: list) if (!set.add(each)) return each;
	    return null;
	}
	
	public static ArrayList<HashInfo> hash(Path rootdir) throws IOException {
		return hash(rootdir, null);
	}
	public static ArrayList<HashInfo> hash(Path rootdir, Consumer<HashInfo> p1) throws IOException {
		long time = System.currentTimeMillis();
		Stream<HashInfo> stream = Files.walk(rootdir).parallel()
				.map(p -> new HashInfo(rootdir.relativize(p), FileHash.hashFile(p)));
		if(p1 != null) stream = stream.peek(p1::accept);
		ArrayList<HashInfo> ret = stream.sorted().collect(Collectors.toCollection(ArrayList<HashInfo>::new));
		time = System.currentTimeMillis() - time;
		out.printf("Hashing \"%s\" done in %dms\n", rootdir, time);
		
		HashInfo h = findFirstDuplacate(ret);
		if(h != null) System.err.println("Warning : duplicate endtry : " + h);
			
		return ret;
	}

	static String hashFile(Path file) {
		//out.println("Doing : " + file);
		if(Files.isDirectory(file)) return "*directory";
		
		ByteBuffer buf = ByteBuffer.allocateDirect(8*1024);
		try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)){
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			while(fc.read(buf.clear()) != -1) {
				md.update(buf.flip());
			}
			return HexFormat.of().formatHex(md.digest());
		} catch (Exception e) {
			e.printStackTrace();
			return "!!Hash Failed!!";
		}
	}
	
	
	public record HashInfo(Path relativePath, String hash) implements Comparable<HashInfo> {
		public boolean samePath(Path anotherPath) {
			return relativePath.toString().equals(anotherPath.toString());
		}
		
		@Override
		public int compareTo(HashInfo o) {
			return relativePath.toString().compareTo(o.relativePath.toString());
		}

		@Override
		public String toString() {
			return relativePath + " : " + hash ;
		}
		
		public String hashAndFullPath(Path dir) {
			return hash + " : " + dir.resolve(relativePath);
		}
	}
	
	
	public static void test() throws Exception {
		//Path d1 = Files.createTempDirectory("1");
		//Path d2 = Files.createTempDirectory("2");
		
		//Files.createTempFile(d2, null, null, null)
		
	}
	
	static class MainFrame extends JFrame {
		private final JTextArea logArea = new JTextArea();
		private Path dir1, dir2;
		private JTree tree1, tree2;
		private List<HashInfo> dir1Hashes, dir2Hashes;

		public MainFrame() {
			super("Directory Compare");

			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
			this.dir1 = chooser.getSelectedFile().toPath();
			this.tree1 = new JTree(new DefaultMutableTreeNode(dir1.getFileName().toString()));
			
			if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
			this.dir2 = chooser.getSelectedFile().toPath();
			this.tree2 = new JTree(new DefaultMutableTreeNode(dir2.getFileName().toString()));

			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setSize(1000, 700);
			setLocationRelativeTo(null);

			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Log", createLogPanel());
			tabs.addTab("Directory 1", createDirPanel(tree1, dir1, 1));
			tabs.addTab("Directory 2", createDirPanel(tree2, dir2, 2));
			add(tabs, BorderLayout.CENTER);
			setVisible(true);

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

			new Thread(() -> {
				try {
					boolean same = compareTwoDirectories(dir1, null, dir2, null);
					out.println("IsSame : " + same);
					SwingUtilities.invokeLater(() -> updateTooltips());
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

		private JPanel createDirPanel(final JTree tree, Path dir, int dirIndex) {
			buildTree(root, dir, dir);
			ToolTipManager.sharedInstance().registerComponent(tree);
			tree.setCellRenderer(new DefaultTreeCellRenderer() {
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value,
						boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
					if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof HashInfo h) {
						String status = "";
						if (dir1Hashes != null && dir2Hashes != null) {
							HashInfo other = (dirIndex == 1) ? findHashInList(h, dir2Hashes) : findHashInList(h, dir1Hashes);
							if (other == null) status = " [Missing in other dir]";
							else if (!h.hash().equals(other.hash())) status = " [Hash Mismatch]";
							else status = " [Match]";
						}
						setToolTipText(h.hash() + status);
					} else {
						setToolTipText(null);
					}
					return c;
				}
			});

			JButton openBtn = new JButton("Show in Explorer");
			openBtn.addActionListener(ev -> {
				TreePath path = tree.getSelectionPath();
				Path p = path == null ? dir : dir.resolve(((HashInfo)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject()).relativePath);
				try {
					Desktop.getDesktop().open((Files.isDirectory(p) ? p : p.getParent()).toFile()); //TODO : fix
				} catch (IOException e) {
					e.printStackTrace(out);
				}
			});

			JPanel p = new JPanel(new BorderLayout());
			p.add(new JScrollPane(tree), BorderLayout.CENTER);
			p.add(openBtn, BorderLayout.SOUTH);
			return p;
		}
		
		private void addTree(JTree tree, HashInfo h) {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
			for(Path p : h.relativePath) {
				Enumeration<TreeNode> ch = root.children();
				while(ch.hasMoreElements()) {
					
				}
			}
		}

		private void buildTree(DefaultMutableTreeNode parent, Path base, Path dir) {
			try {
				Files.list(dir).sorted().forEach(path -> {
					if (Files.isDirectory(path)) {
						DefaultMutableTreeNode node = new DefaultMutableTreeNode(path.getFileName().toString());
						parent.add(node);
						buildTree(node, base, path);
					} else {
						HashInfo h = new HashInfo(base.relativize(path), hashFile(path));
						parent.add(new DefaultMutableTreeNode(h));
					}
				});
			} catch (IOException e) {
				e.printStackTrace(out);
			}
		}

		private HashInfo findHashInList(HashInfo h, List<HashInfo> list) {
			for (HashInfo hi : list) {
				if (hi.relativePath().equals(h.relativePath())) return hi;
			}
			return null;
		}

		private void updateTooltips() {
			// Repaint trees to refresh tooltips after comparison
			for (Component comp : getContentPane().getComponents()) {
				if (comp instanceof JTabbedPane tabs) {
					for (int i = 0; i < tabs.getTabCount(); i++) {
						Component c = tabs.getComponentAt(i);
						if (c instanceof JPanel panel) {
							for (Component inner : panel.getComponents()) {
								if (inner instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTree tree) {
									tree.repaint();
								}
							}
						}
					}
				}
			}
		}
	}

}