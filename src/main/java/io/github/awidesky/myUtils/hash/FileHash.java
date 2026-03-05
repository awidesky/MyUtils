package io.github.awidesky.myUtils.hash;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHash {
	
	private PrintWriter out;
	private AtomicLong count = new AtomicLong();
	private int logInterval = 10000;
	private boolean includeHidden = true;
	
	public static void main(String[] args) throws IOException {
		FileHash fh = new FileHash(new PrintWriter(System.out, true));

		var re = fh.compareTwoDirectories(
				Paths.get("/Volumes/a"),
				Paths.get("/Volumes/b")
				);
		System.out.println("IsSame : " + re.isSame);

		//SwingUtilities.invokeLater(MainFrame::new);
	}
	
	public FileHash(PrintWriter out) {
		setOut(out);
	}
	
	public CompareResult compareTwoDirectories(Path d1, Path d2) throws IOException {
		long entries = getInfo(d1);
		getInfo(d2); out.println();

		record HashResult(List<HashInfo> l1, List<HashInfo> l2, List<Path> missingIn1, List<Path> missingIn2) {};
		HashResult hashResult;
		
		FutureTask<HashResult> future = new FutureTask<>(() -> {
			LinkedList<Path> m1 = new LinkedList<>(), m2 = new LinkedList<>();
			return new HashResult(hashCommoms(d1, d2, m2), hashCommoms(d2, d1, m1), m1, m2);
		});
		new Thread(future).start();
		
		Timer timer = new Timer();
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				//TODO : 25%까지는 3초마다 출력하고, 이후에는 10~20%마다 출력
				out.println(("\t[%s] Hashed %" + ((int)Math.log10(2 * entries) + 1) + "d files so far...")
						.formatted(new SimpleDateFormat("kk:mm:ss.SSS").format(new Date()), count.getAcquire()));
			}
		};
		timer.schedule(tt, 3000, logInterval);
		try {
			hashResult = future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace(out);
			return null;
		} finally {
			timer.cancel();
		}
		
		if(hashResult.l1.size() != hashResult.l2.size()) { //TODO : delete
			out.println("Entries number mismatch! (%s : %d entries, %s : %d entries)"
					.formatted(d1.toString(), hashResult.l1.size(), d2.toString(), hashResult.l2.size()));
		}
		out.println();
		out.println(d1.toString() + " : hashed " + hashResult.l1.size() + " entries(files and directories)");
		out.println(d2.toString() + " : hashed " + hashResult.l2.size() + " entries(files and directories)");
		out.println();
		
		long time = System.currentTimeMillis();

		out.println("Missing entry in " + d1 + " : ");
		hashResult.missingIn1.stream().map(p -> "  " + p).forEach(System.out::println);
		out.println("======\n");
		
		out.println("Missing entry in " + d2 + " : ");
		hashResult.missingIn2.stream().map(p -> "  " + p).forEach(System.out::println);
		out.println("======\n");

		HashMap<Path, HashInfo> s = new LinkedHashMap<>();
		hashResult.l1.stream().forEach(h -> s.put(h.relativePath, h));
		List<HashInfoPair> hashDiffs = new LinkedList<>();
		out.println("Hash differences :");
		for(HashInfo h : hashResult.l2) {
			HashInfo i = s.get(h.relativePath);
			if(!h.equals(i)) {
				hashDiffs.add(new HashInfoPair(i, h));
				out.println("  Hash diff : " + i.hashAndFullPath(d1)); //TODO : same hash, different name!
				out.println("       with : " + h.hashAndFullPath(d2));
			}
		}
		out.println("======\n");
		out.println("Entry compare : " + (System.currentTimeMillis() - time) + "ms");
		
		return new CompareResult(d1, d2, hashResult.missingIn1, hashResult.missingIn2, hashDiffs,
				hashResult.missingIn1.isEmpty() && hashResult.missingIn2.isEmpty() && hashDiffs.isEmpty());
	}
	
	
	private long getInfo(Path dir) throws IOException {
		AtomicLong files = new AtomicLong();
		AtomicLong directories = new AtomicLong();
		AtomicLong size = new AtomicLong();
		getFileStream(dir).parallel().forEach(p -> {
			if(Files.isRegularFile(p)) {
				files.incrementAndGet();
				try {
					size.addAndGet(Files.size(p));
				} catch (IOException e) {
					e.printStackTrace(out);
				}
			} else if(Files.isDirectory(p)) directories.incrementAndGet();
		});
		long entries = files.get() +  directories.get();
		out.printf("Files : %d, Directories : %s, Total entries : %d, Total size : %s (%s)\n",
				files.get(), directories.get(), entries, formatFileSize(size.get()), dir.toString());
		return entries;
	}
	
	public ArrayList<HashInfo> hashCommoms(Path rootdir, Path anotherRoot, List<Path> missings) throws IOException {
		long time = System.currentTimeMillis();
		Vector<Path> m = new Vector<>();
		ArrayList<HashInfo> ret = getFileStream(rootdir).parallel()
				.filter(p -> {
					if(!Files.exists(anotherRoot.resolve(rootdir.relativize(p)))) {
						m.add(p);
						return false;
					}
					else return true;
				})
				.map(p -> new HashInfo(rootdir.relativize(p), hashFile(p)))
		 		.sorted().collect(Collectors.toCollection(ArrayList<HashInfo>::new));
		time = System.currentTimeMillis() - time;
		out.printf("Hashing \"%s\" done in %dms\n", rootdir, time);

		missings.addAll(m);
		missings.sort(Comparator.comparing(Path::toString));
		Iterator<Path> it = missings.iterator();
		while(it.hasNext()) {
			Path p = it.next();
			if(Files.isDirectory(p)) {
				while(it.hasNext() && it.next().startsWith(p)) {
					it.remove();
				}
			}
		}
		List<Path> relativized = missings.stream().map(rootdir::relativize).toList();
		missings.clear(); missings.addAll(relativized);
		
		return ret;
	}

	public String hashFile(Path file) {
		//out.println("Doing : " + file);
		if(Files.isDirectory(file)) return "*directory";
		
		ByteBuffer buf = ByteBuffer.allocateDirect(8*1024);
		try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)){
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			while(fc.read(buf.clear()) != -1) {
				md.update(buf.flip());
			}
			String ret = HexFormat.of().formatHex(md.digest());
			count.incrementAndGet();
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return "!!Hash Failed!!";
		}
	}
	
	private Stream<Path> getFileStream(Path dir) throws IOException {
		return Files.walk(dir).skip(1).filter(p -> { //TODO : fix, skip in parellel
			if(!includeHidden && !p.toFile().isHidden())
				return false;
			return true;
		});
	}
	
	public void setOut(PrintWriter out) {
		this.out = out;
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
			return dir.resolve(relativePath) + " : " + hash;
		}
	}
	public record HashInfoPair(HashInfo h1, HashInfo h2) {}
	public record CompareResult(Path dir1, Path dir2, List<Path> missingIn1, List<Path> missingIn2,
			List<HashInfoPair> hashDiffs, boolean isSame) {}
	
	public static void test() throws Exception {
		//Path d1 = Files.createTempDirectory("1");
		//Path d2 = Files.createTempDirectory("2");
		
		//Files.createTempFile(d2, null, null, null)
		
	}
	
	public static String formatFileSize(long length) {
		
		if(length == 0L) return "0.00 byte";
		
		switch ((int)(Math.log(length) / Math.log(1024))) {
		
		case 0:
			return String.format("%d", length) + " byte";
		case 1:
			return String.format("%.2f", length / 1024.0) + " KB";
		case 2:
			return String.format("%.2f", length / (1024.0 * 1024)) + " MB";
		case 3:
			return String.format("%.2f", length / (1024.0 * 1024 * 1024)) + " GB";
		}
		return String.format("%.2f", length / (1024.0 * 1024 * 1024 * 1024)) + " TB";
	}

}