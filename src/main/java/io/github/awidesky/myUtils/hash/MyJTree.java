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
	
	private static final long serialVersionUID = 8274666144466211316L;
	private final DefaultMutableTreeNode root;
	
	public MyJTree(Path path) {
		this(new DefaultMutableTreeNode(new HashTreeNodeObject(path.getFileName().toString(), path.toFile())));
	}
	
	private MyJTree(DefaultMutableTreeNode root) {
		super(new DefaultTreeModel(root, false));
		this.root = root;
	}
	
	public DefaultMutableTreeNode getRoot() {
		return root;
	}
	
	public void hashUpdated(HashInfo h) {
	    DefaultMutableTreeNode node = findNode(h.relativePath());

	    if (node == null) return;

	    Object o = node.getUserObject();

	    if (o instanceof HashTreeNodeObject no) {
	        no.hash = h.hash();
	        getModel().nodeChanged(node);
	    }
	}
	
	private DefaultMutableTreeNode findNode(Path relPath) {
	    DefaultMutableTreeNode current = root;

	    for (Path part : relPath) {

	        boolean found = false;

	        for (int i = 0; i < current.getChildCount(); i++) {

	            DefaultMutableTreeNode child =
	                    (DefaultMutableTreeNode) current.getChildAt(i);

	            Object o = child.getUserObject();

	            if (o instanceof HashTreeNodeObject no &&
	                no.name.equals(part.toString())) {

	                current = child;
	                found = true;
	                break;
	            }
	        }

	        if (!found) return null;
	    }

	    return current;
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
