package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern IGNORED = Pattern.compile("(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern DECLARATION = Pattern.compile("\\b(?:class|interface)\\s+([\\w$]+)\\s*([^\\{]*)\\{", Pattern.DOTALL);
    private static final Pattern RELATION = Pattern.compile("\\b(extends|implements)\\s+([^\\{]+?)(?=\\b(?:extends|implements)\\b|$)", Pattern.DOTALL);
    private static final Pattern TYPE = Pattern.compile("(?:[A-Za-z_$][\\w$]*\\.)*[A-Za-z_$][\\w$]*");

    public static void main(String[] args) throws IOException {
        String projectPath = "C:/Users/Пользователь/PDP";
        Path root = Path.of(projectPath);
        Map<String, Set<String>> index = new TreeMap<>();
        List<Path> files;

        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path file : files) {
            addRelations(Files.readString(file), index);
        }

        index.forEach((parent, children) -> System.out.println(parent + " -> " + String.join(", ", children)));
    }

    private static void addRelations(String source, Map<String, Set<String>> index) {
        Matcher declarations = DECLARATION.matcher(IGNORED.matcher(source).replaceAll(" "));
        while (declarations.find()) {
            String child = declarations.group(1);
            Matcher relations = RELATION.matcher(withoutGenerics(declarations.group(2)));
            while (relations.find()) {
                for (String part : relations.group(2).split(",")) {
                    Matcher type = TYPE.matcher(part);
                    if (type.find()) {
                        String parent = type.group();
                        Set<String> children = index.getOrDefault(parent, new TreeSet<>());
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
