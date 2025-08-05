package io.github.awidesky.myUtils;

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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class FileHash {
	
	private static PrintWriter out;
	
	public static void main(String[] args) throws IOException {
		boolean re = compareTwoDirectories(
				Paths.get("/Users/eugenehong/Documents/인하/3-1"),
				Paths.get("/Users/eugenehong/Library/Mobile Documents/com~apple~CloudDocs/inha/3-1")
				);
		
		out = new PrintWriter(System.out);
		out.println("IsSame : " + re);
	}
	
	public static boolean compareTwoDirectories(Path d1, Path d2) throws IOException {
		FutureTask<ArrayList<HashInfo>> f1 = new FutureTask<ArrayList<HashInfo>>(() -> hash(d1));
		FutureTask<ArrayList<HashInfo>> f2 = new FutureTask<ArrayList<HashInfo>>(() -> hash(d2));
		new Thread(f1).start();
		new Thread(f2).start();
		
		ArrayList<HashInfo> l1;
		ArrayList<HashInfo> l2;
		try {
			l1 = f1.get();
			l2 = f2.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			l1 = hash(d1);
			l2 = hash(d2);
		}
		
		boolean ret = true;
		
		if(l1.size() != l2.size()) {
			out.println("Entries number mismatch! (%s : %d entries, %s : %d entries)"
					.formatted(d1.toString(), l1.size(), d2.toString(), l2.size()));
		}
		out.println(l1.size() + " files");
		out.println();
		
		long time = System.currentTimeMillis();
		HashMap<String, HashInfo> s = new HashMap<String, HashInfo>();
		l1.stream().forEach(h -> s.put(h.hash, h));
		out.println("Missing entry in " + d1 + " : ");
		for(HashInfo h : l2) {
			HashInfo i = s.get(h.hash);
			if(h.equals(i)) {
				s.remove(h.hash);
				continue;
			}
			
			ret = false;
			if(i == null) {
				out.println("  Not exist : " + h);
			} else {
				out.println("  Hash diff : " + i.hashAndFullPath(d1));
				out.println("       with : " + h.hashAndFullPath(d2));
				s.remove(h.hash);
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
		long time = System.currentTimeMillis();
		ArrayList<HashInfo> ret = Files.walk(rootdir).parallel()
				//.filter(Files::isRegularFile)
				.map(p -> new HashInfo(rootdir.relativize(p), FileHash.hashFile(p)))
				.sorted().collect(Collectors.toCollection(ArrayList<HashInfo>::new));
		time = System.currentTimeMillis() - time;
		out.printf("Hashing \"%s\" done in %dms\n", rootdir, time);
		
		HashInfo h = findFirstDuplacate(ret);
		if(h != null) System.err.println("Warning : duplicate endtry : " + h);
			
		return ret;
	}

	private static String hashFile(Path file) {
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
}
