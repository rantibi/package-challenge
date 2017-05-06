package com.mobiquityinc.packer;

import lombok.Data;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Data
public class Combination {
    ArrayList<Package> packages = new ArrayList<>();

    public double getCost() {
        return packages.stream()
                .mapToDouble(Package::getCost)
                .sum();
    }

    public double getWeight() {
        return packages.stream()
                .mapToDouble(Package::getWeight)
                .sum();
    }

    public Combination add(Package pack) {
        Combination combination = new Combination();
        combination.packages.addAll(packages);
        combination.packages.add(pack);
        return combination;
    }
}
