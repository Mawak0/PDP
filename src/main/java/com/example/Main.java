package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final Pattern DECLARATION = Pattern.compile("\\b(?:class|interface)\\s+([\\w$]+)\\s*([^\\{]*)\\{", Pattern.DOTALL);
    private static final Pattern RELATION = Pattern.compile("\\b(extends|implements)\\s+([^\\{]+?)(?=\\b(?:extends|implements)\\b|$)", Pattern.DOTALL);
    private static final Pattern TYPE = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)?");

    public static void main(String[] args) throws IOException {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Map<String, List<String>> index = new TreeMap<>();
        List<Path> files;

        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path file : files) {
            addRelations(Files.readString(file), index);
        }

        index.forEach((parent, children) -> {
            children.sort(Comparator.naturalOrder());
            System.out.println(parent + " -> " + String.join(", ", children));
        });
    }

    private static void addRelations(String source, Map<String, List<String>> index) {
        Matcher declarations = DECLARATION.matcher(source);
        while (declarations.find()) {
            String child = declarations.group(1);
            Matcher relations = RELATION.matcher(declarations.group(2).replaceAll("<[^<>]*>", " "));
            while (relations.find()) {
                for (String part : relations.group(2).split(",")) {
                    Matcher type = TYPE.matcher(part);
                    if (type.find()) {
                        String parent = type.group();
                        List<String> children = index.getOrDefault(parent, new ArrayList<>());
                        if (!children.contains(child)) {
                            children.add(child);
                        }
                        index.put(parent, children);
                    }
                }
            }
        }
    }
}
