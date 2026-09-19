package com.example;

import com.thedeanda.lorem.LoremIpsum;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {
        int total_words = 0;
        Path path = Path.of("C:/Users/Пользователь/PDP/text.txt");

        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());

            String lorem = LoremIpsum.getInstance().getWords(350);
            Files.writeString(path, lorem);
        }

        String text = Files.readString(path).toLowerCase();

        Pattern pattern = Pattern.compile("\\p{L}+");
        Matcher matcher = pattern.matcher(text);

        Map<String, Integer> words = new HashMap<>();

        while (matcher.find()) {
            total_words++;
            String word = matcher.group();
            words.put(word, words.getOrDefault(word, 0) + 1);
        }

        for (String word : words.keySet()) {
            System.out.println(word + ": " + words.get(word));

        }
        System.out.println("Всего слов: "+total_words);
    }
}