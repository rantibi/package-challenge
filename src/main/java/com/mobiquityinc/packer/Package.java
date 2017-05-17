package com.mobiquityinc.packer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Package {
    private int index;
    private int weight;
    private double cost;
}
