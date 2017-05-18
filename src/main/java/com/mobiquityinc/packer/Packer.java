package com.mobiquityinc.packer;

import com.mobiquityinc.exception.APIException;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Packer {

    private static final String INDEX = "index";
    private static final String WEIGHT = "weight";
    private static final String COST = "cost";
    private static final String PACKAGE_REGEX = "\\((?<" + INDEX + ">\\d+)\\,(?<" + WEIGHT + ">\\d+(\\.\\d{1,2})?)\\,€(?<" + COST + ">\\d+(\\.\\d{1,2})?)\\)";
    private static final int MAX_ITEMS_IN_LINE = 15;
    private static final int MAX_WEIGHT = 100 * 100;
    private static final int MAX_COST = 100 * 100;

    public static String pack(String inputPath) throws APIException {
        return parsedInputFile(inputPath).stream()
                .map(parsedLine -> getBestCombinationAsString(parsedLine.getMaxWeight(), parsedLine.getPackages()))
                .collect(Collectors.joining("\n"));
    }

    private static List<ParsedLine> parsedInputFile(String inputPath) throws APIException {
        List<ParsedLine> parsedLines = new ArrayList<>();

        try (FileInputStream inputStream = new FileInputStream(inputPath)) {
            try (Scanner scanner = new Scanner(inputStream)) {
                for (long lineId = 0; scanner.hasNext(); lineId++) {
                    String line = scanner.nextLine();
                    parsedLines.add(parseLine(lineId, line));
                }
            }
        } catch (IOException e) {
            throw new APIException(e);
        }

        return parsedLines;
    }

    private static ParsedLine parseLine(long lineNumber, String line) throws APIException {
        String[] splited = line.split(":");

        if (splited.length != 2) {
            throw new APIException("Line must contain exactly one `:`", line, lineNumber);
        }

        final int maxWeight;

        try {
            maxWeight = (int) (Double.parseDouble(splited[0]) * 100);
        } catch (NumberFormatException e) {
            throw new APIException("Left side of `:` must be a number", e, line, lineNumber);
        }

        Pattern pattern = Pattern.compile(PACKAGE_REGEX);
        Matcher matcher = pattern.matcher(splited[1]);
        int lastEnd = 0;
        List<Package> packages = new ArrayList<>();

        while (matcher.find()) {
            if (matcher.start() != lastEnd + 1 || splited[1].charAt(lastEnd) != ' ') {
                throw new APIException(String.format("Right side of `:` must be in the following pattern (%s) separated by space", PACKAGE_REGEX), line, lineNumber);
            }

            try {
                Integer index = Integer.valueOf(matcher.group(INDEX));
                int weight = (int) (Double.valueOf(matcher.group(WEIGHT)) * 100);
                Double cost = Double.valueOf(matcher.group(COST));

                if (index > MAX_ITEMS_IN_LINE || index < 0) {
                    throw new APIException(String.format("index mas be in range (1, %d)", MAX_ITEMS_IN_LINE), line, lineNumber);
                }

                if (weight > MAX_WEIGHT || weight < 0) {
                    throw new APIException(String.format("weight mas be in range (0, %f)", MAX_WEIGHT), line, lineNumber);
                }

                if (cost > MAX_COST || cost < 0) {
                    throw new APIException(String.format("cost mas be in range (0, %f)", MAX_COST), line, lineNumber);
                }

                packages.add(new Package(index, weight, cost));
            } catch (NumberFormatException | IllegalFormatConversionException e) {
                throw new APIException(e, line, lineNumber);
            }

            lastEnd = matcher.end();
        }

        if (lastEnd != splited[1].length()) {
            throw new APIException("unexpected characters in the end of the line", line, lineNumber);
        }

        long[] indexes = packages.stream()
                .mapToLong(Package::getIndex)
                .toArray();

        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] != i + 1) {
                throw new APIException("The indexes in not order well, or some index is missing", line, lineNumber);
            }
        }

        return new ParsedLine(maxWeight, packages);
    }

    private static String getBestCombinationAsString(int maxWeight, List<Package> packages) {
        int n = packages.size() + 1;
        int w = maxWeight + 1;
        double[][] a = new double[n][w];


        for (int i = 1; i < n; i++) {
            Package pack = packages.get(i - 1);

            for (int j = 1; j < w; j++) {
                if (pack.getWeight() > j) {
                    a[i][j] = a[i - 1][j];
                } else {
                    a[i][j] = Math.max(a[i - 1][j], a[i - 1][j - pack.getWeight()] + pack.getCost());
                }
            }
        }

        List<Integer> indexes = new ArrayList<>();
        int j = maxWeight;
        double totalcost = a[n - 1][w - 1];
        for (; j > 0 && a[n - 1][j - 1] == totalcost; j--);

        for (int i = n - 1; i > 0; i--) {
            if (a[i][j] != a[i - 1][j]) {
                indexes.add(packages.get(i - 1).getIndex());
                j -= packages.get(i - 1).getWeight();
            }
        }

        String result =
                indexes.stream()
                        .mapToInt(i -> i)
                        .sorted()
                        .mapToObj(index -> Integer.toString(index))
                        .collect(Collectors.joining(","));
        return result.isEmpty() ? "-" : result;
    }

    @Data
    @AllArgsConstructor
    private static class ParsedLine {
        private int maxWeight;
        private List<Package> packages;
    }
}
