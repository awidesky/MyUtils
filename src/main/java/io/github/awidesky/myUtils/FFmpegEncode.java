/*
 * Copyright (c) 2025 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.myUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FFmpegEncode {
	
	private final static int THREADS = Runtime.getRuntime().availableProcessors();
	private final static ExecutorService pool = Executors.newFixedThreadPool(THREADS);

	private static File root = new File("C:\\Users\\fvt\\Downloads\\vid\\o");
	private static File dest = new File(root, "h264_qsv_medium_crf23_1");
	private static File logDir = new File(dest, "logs");
	
	public static void main(String[] args) {

		if(!dest.exists()) dest.mkdirs();
		if(!logDir.exists()) logDir.mkdirs();
		
		System.out.println("Using " + THREADS + " threads...");
		
		Arrays.stream(root.listFiles())
		.filter(File::isFile).filter(f -> f.getName().endsWith(".mp4"))
		.map(f -> pool.submit(() -> launch(f)))
		.forEach(f -> {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Failed to wait for future!");
				e.printStackTrace();
			}
		});
		pool.shutdown();
		System.out.println("done!");

	}
	
	public static void launch(File f) {
		ProcessBuilder pb = new ProcessBuilder(
				"C:\\Users\\fvt\\Downloads\\ffmpeg-7.1.1-essentials_build\\ffmpeg-7.1.1-essentials_build\\bin\\ffmpeg.exe", 
				"-hide_banner", "-nostats",
				"-i", f.getAbsolutePath(),
				"-c:v", "h264_qsv",
				"-crf", "23",
				"-preset", "medium",
				"-c:a", "copy",
				new File(dest, f.getName()).getAbsolutePath()
				);
		File logFile = new File(logDir, f.getName() + "_log.txt");
		if(logFile.exists()) logFile.delete();
		try {
			logFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		pb.redirectError(logFile);
		pb.redirectOutput(logFile);
		
		try {
			Process p = pb.start();
			p.waitFor();
			ProcessHandle.Info info = p.info();
			System.out.printf("[%s] started : %s, totalTime: %dms, exit code : %d\n",
					pb.command().stream().collect(Collectors.joining(" ")), info.startInstant().get().toString(), info.totalCpuDuration().get().toMillis(), p.exitValue());
		} catch (InterruptedException | IOException e) {
			System.err.println(f.getAbsolutePath() + " failed!");
			e.printStackTrace();
		}
	}

}
