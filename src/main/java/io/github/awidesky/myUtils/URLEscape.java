package io.github.awidesky.myUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Stream;


public class URLEscape {
	public static void main(String[] args) {
		System.out.println("URL Escaper v1.0\n>>>");
		try(Scanner sc = new Scanner(System.in)) {
			Stream.generate(sc::nextLine)
				.takeWhile(s -> !"/exit".equals(s))
				.map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8) + "\n>>>")
				.forEach(System.out::println);
		}
		System.out.println("Goodbye");
	}
}
