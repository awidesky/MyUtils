package io.github.awidesky.myUtils;

import java.util.Scanner;

public class TR {

	public static void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input :");
        while(scanner.hasNextLine()) {
        	String input = scanner.nextLine();
        	for (String token : input.trim().split("\\s+")) {
        		System.out.println(token);
        	}
        }
        scanner.close();
	}

}
