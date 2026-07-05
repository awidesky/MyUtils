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
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHash {
	
	private PrintWriter out;
	private AtomicLong count = new AtomicLong();
	private int logInterval = 10000;
	private boolean includeHidden = true;
	private static final Normalizer.Form normalizeTo = Normalizer.Form.NFC;
	private boolean normalizePathname = true;
	
	private static int BUFFSIZE = 512 * 1024;
	private static final ThreadLocal<ByteBuffer> IOBUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(BUFFSIZE));
	private static final ThreadLocal<byte[]> HEAPBUFFER = ThreadLocal.withInitial(() -> new byte[BUFFSIZE]);
	private static final ThreadLocal<MessageDigest> HASH = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	});

	public FileHash(PrintWriter out) {
		setOut(out);
	}

	public FileHash(PrintWriter out, boolean normalizePathname) {
		setOut(out);
		this.normalizePathname = normalizePathname;
	}
	
	public boolean isNormalizePathname() {
		return normalizePathname;
	}

	/**
	 * Normalize path name into NFC
	 * */
	public void setNormalizePathname(boolean normalizePathname) {
		this.normalizePathname = normalizePathname;
	}

	
	public CompareResult compareTwoDirectories(Path d1, Path d2) throws IOException {
		return compareTwoDirectories(d1, null, d2, null);
	}
	public CompareResult compareTwoDirectories(
	        Path d1, Consumer<HashInfo> hashCallback1,
	        Path d2, Consumer<HashInfo> hashCallback2) throws IOException {

	    long entries = getInfo(d1);
	    getInfo(d2); 
	    out.println();
	    
	    
		Map<Path, Path> files1 = getFileStream(d1).map(d1::relativize).collect(Collectors.toMap(this::normalizeFilename, Function.identity()));
		Map<Path, Path> files2 = getFileStream(d2).map(d2::relativize).collect(Collectors.toMap(this::normalizeFilename, Function.identity()));
		
		Set<Path> keys1 = files1.keySet();
		Set<Path> keys2 = files2.keySet();

		Set<Path> commonKeys = new HashSet<>(keys1); commonKeys.retainAll(keys2);
		Set<Path> missingKeys1 = new HashSet<>(keys2); missingKeys1.removeAll(keys1);
		Set<Path> missingKeys2 = new HashSet<>(keys1); missingKeys2.removeAll(keys2);

		List<Path> missingIn1 = missingKeys1.stream().map(files2::get).toList();
		List<Path> missingIn2 = missingKeys2.stream().map(files1::get).toList();

		List<Path> common1 = commonKeys.stream().map(files1::get).toList();
		List<Path> common2 = commonKeys.stream().map(files2::get).toList();

	    record HashResult(List<HashInfo> l1, List<HashInfo> l2,
	                      List<Path> missingIn1, List<Path> missingIn2) {}

	    HashResult hashResult;

	    ExecutorService executor = Executors.newSingleThreadExecutor();

	    Future<HashResult> future = executor.submit(() -> {

	        List<HashInfo> l1 = hashFiles(d1, common1, hashCallback1);
	        List<HashInfo> l2 = hashFiles(d2, common2, hashCallback2);

			return new HashResult(l1, l2, missingIn1, missingIn2);
	    });
		
	    count.set(0);
		Timer timer = new Timer(true);
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				//TODO : 25%까지는 3초마다 출력하고, 이후에는 10~20%마다 출력
				out.println(("\t[%s] Hashed %" + ((int)Math.log10(2 * entries) + 1) + "d files so far...")
						.formatted(new SimpleDateFormat("kk:mm:ss.SSS").format(new Date()), count.get()));
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
			executor.shutdownNow();
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

		HashMap<Path, HashInfo> s = new HashMap<>();
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
		System.out.println("getinfo : dir " + dir);
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
	
	private Path normalizeFilename(Path p) {
		if(normalizePathname && !Normalizer.isNormalized(p.toString(), normalizeTo)) {
			return Paths.get(Normalizer.normalize(p.toString(), normalizeTo));
		} else {
			return p;
		}
		
	}

	public ArrayList<HashInfo> hashFiles(Path rootdir, Collection<Path> commons, Consumer<HashInfo> callback)
			throws IOException {

	    long time = System.currentTimeMillis();

	    Stream<HashInfo> stream = commons.parallelStream()
	            .map(rel -> {
	                Path full = rootdir.resolve(rel);
					return new HashInfo(normalizeFilename(rel), hashFile(full));
				});

	    if(callback != null)
	        stream = stream.peek(callback);

	    ArrayList<HashInfo> ret = stream
	            .sorted()
	            .collect(Collectors.toCollection(ArrayList::new));

	    time = System.currentTimeMillis() - time;

	    out.printf("Hashing \"%s\" done in %dms\n", rootdir, time);

	    return ret;
	}

	public String hashFile(Path file) {
		//out.println("Doing : " + file);
		if(Files.isDirectory(file)) return "*directory";
		
		ByteBuffer buf = IOBUFFER.get().clear();
		byte[] arr = HEAPBUFFER.get();
		try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)){
			MessageDigest md = HASH.get(); md.reset();
			while (true) {
			    int n = fc.read(buf.clear());
			    if (n == -1) break;
			    
				buf.flip().get(arr, 0, n);
				md.update(arr, 0, n);
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
		return Files.walk(dir).skip(1).filter(p -> includeHidden || !p.toFile().isHidden());
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