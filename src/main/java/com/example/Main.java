package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern IGNORED = Pattern.compile("(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern DECLARATION = Pattern.compile("\\b(?:class|interface)\\s+([\\w$]+)\\s*([^\\{]*)\\{", Pattern.DOTALL);
    private static final Pattern RELATION = Pattern.compile("\\b(extends|implements)\\s+([^\\{]+?)(?=\\b(?:extends|implements)\\b|$)", Pattern.DOTALL);
    private static final Pattern TYPE = Pattern.compile("(?:[A-Za-z_$][\\w$]*\\.)*[A-Za-z_$][\\w$]*");

    public static void main(String[] args) throws IOException, InterruptedException {
        String projectPath = "C:/Users/Пользователь/spring-framework";
        Path root = Path.of(projectPath);
        List<Path> files;

        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }

        Map<String, Set<String>> results = new HashMap<>();
        List<IOException> errors = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(files.size());

        for (Path file : files) {
            Thread.startVirtualThread(() -> {
                try {
                    addRelations(Files.readString(file), results);
                } catch (IOException e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("Java-файлов найдено: " + files.size());
        System.out.println("Типов в индексе: " + results.size());

        if (!errors.isEmpty()) throw errors.getFirst();

        results.forEach((parent, children) -> System.out.println(parent + " -> " + String.join(", ", children)));

        Set<String> leaves = new TreeSet<>(results.keySet());
        results.values().forEach(leaves::addAll);
        results.forEach((parent, children) -> {
            if (!children.isEmpty()) leaves.remove(parent);
        });
        System.out.println("Листьев: " + leaves.size());
    }

    private static void addRelations(String source, Map<String, Set<String>> index) {
        Matcher declarations = DECLARATION.matcher(IGNORED.matcher(source).replaceAll(" "));
        while (declarations.find()) {
            String child = declarations.group(1);
            index.putIfAbsent(child, new HashSet<>());
            Matcher relations = RELATION.matcher(withoutGenerics(declarations.group(2)));
            while (relations.find()) {
                for (String part : relations.group(2).split(",")) {
                    Matcher type = TYPE.matcher(part);
                    if (type.find()) {
                        String parent = type.group();
                        Set<String> children = index.getOrDefault(parent, new HashSet<>());
                        children.add(child);
                        index.put(parent, children);
                    }
                }
            }
        }
    }

    private static String withoutGenerics(String text) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (char symbol : text.toCharArray()) {
            if (symbol == '<') depth++;
            else if (symbol == '>') depth--;
            else if (depth == 0) result.append(symbol);
        }
        return result.toString();
    }
}
