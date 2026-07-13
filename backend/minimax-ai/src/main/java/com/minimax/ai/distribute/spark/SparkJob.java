package com.minimax.ai.distribute.spark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spark Job (V3.5.4)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SparkJob {
    private String id;
    private List<SparkStage> stages;
    private boolean completed;
}
