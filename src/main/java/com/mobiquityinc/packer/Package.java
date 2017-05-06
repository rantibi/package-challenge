package com.mobiquityinc.packer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Package {
    private long index;
    private double weight;
    private double cost;
}
