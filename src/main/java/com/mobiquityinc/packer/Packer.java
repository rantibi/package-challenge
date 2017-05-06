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

    public static final String INDEX = "index";
    public static final String WEIGHT = "weight";
    public static final String COST = "cost";
    public static final String PACKAGE_REGEX = "\\((?<" + INDEX + ">\\d+)\\,(?<" + WEIGHT + ">\\d+(\\.\\d{1,2})?)\\,€(?<" + COST + ">\\d+(\\.\\d{1,2})?)\\)";
    public static final int MAX_ITEMS_IN_LINE = 15;
    public static final int MAX_WEIGHT = 100;
    public static final int MAX_COST = 100;

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

        final double maxWeight;

        try {
            maxWeight = Double.parseDouble(splited[0]);
        } catch (NumberFormatException e) {
            throw new APIException("Left side of `:` must be a number", e, line, lineNumber);
        }

        Pattern pattern = Pattern.compile(PACKAGE_REGEX);
        Matcher matcher = pattern.matcher(splited[1]);
        int lastEnd = 0;
        List<Package> packages = new ArrayList<>();

        while (matcher.find()) {
            if (matcher.start() != lastEnd + 1) {
                throw new APIException(String.format("Right side of `:` must be in the following pattern (%s) separated by space", PACKAGE_REGEX), line, lineNumber);
            }

            try {
                Long index = Long.valueOf(matcher.group(INDEX));
                Double weight = Double.valueOf(matcher.group(WEIGHT));
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


    private static String getBestCombinationAsString(double maxWeight, List<Package> packages) {
        List<Combination> combinations = getCombination(packages);
        Optional<Combination> bestCombination = getBestCombination(maxWeight, combinations);

        if (!bestCombination.isPresent()) {
            return "-";
        }

        return bestCombination.get().getPackages().stream()
                .map(p -> Long.toString(p.getIndex()))
                .collect(Collectors.joining(","));
    }

    private static Optional<Combination> getBestCombination(double maxWeight, List<Combination> combinations) {
        return combinations.stream()
                .filter(combination -> combination.getWeight() <= maxWeight)
                .sorted((combination1, combination2) -> {
                    // the higher price first
                    int compare = Double.compare(combination2.getCost(), combination1.getCost());

                    if (compare != 0) {
                        return compare;
                    }

                    // the lower weight first
                    return Double.compare(combination1.getWeight(), combination2.getWeight());
                })
                .findFirst();
    }

    private static List<Combination> getCombination(List<Package> packages) {
        return getCombination(packages, 0, new ArrayList<>());
    }

    private static List<Combination> getCombination(List<Package> packages, int i, List<Combination> combinations) {
        if (i == packages.size()) {
            return combinations;
        }

        // form each combination create add combination that contain the package i,
        // add leave the original combination
        List<Combination> newCombinations = combinations.stream()
                .map(combination -> combination.add(packages.get(i)))
                .collect(Collectors.toList());
        combinations.addAll(newCombinations);

        // add combination that hold only this single package
        combinations.add(new Combination().add(packages.get(i)));
        return getCombination(packages, i + 1, combinations);
    }

    @Data
    @AllArgsConstructor
    private static class ParsedLine {
        private double maxWeight;
        private List<Package> packages;
    }
}
