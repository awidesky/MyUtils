package io.github.awidesky.myUtils.hash;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import io.github.awidesky.myUtils.hash.FileHash.HashInfo;


public class MyJTree extends JTree {
	
	private final DefaultMutableTreeNode root;
	private Path path;
	
	public MyJTree(Path path) {
		this(new DefaultMutableTreeNode(new HashTreeNodeObject(path.getFileName().toString(), path.toFile())));
		this.path = path;
	}
	
	private MyJTree(DefaultMutableTreeNode root) {
		super(new DefaultTreeModel(root, false));
		this.root = root;
	}
	
	public DefaultMutableTreeNode getRoot() {
		return root;
	}
	
	public void hashUpdated(HashInfo h) {
		
	}
	
	@Override
	public DefaultTreeModel getModel() {
		return (DefaultTreeModel)super.getModel();
	}
	
	@Override
    public String getToolTipText(MouseEvent e) {
    	Point p = e.getPoint();
        TreePath path = getPathForLocation(p.x, p.y);
        if (path == null) return null;

        Object o = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
		if (o instanceof HashTreeNodeObject no) {
			return no.hash;
		} else return o.toString();
	}
	
	public static class HashTreeNodeObject {
		public final String name;
		public final File path;
		public String hash;
		
		public HashTreeNodeObject(String name, File path) {
			this.name = name;
			this.path = path;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
