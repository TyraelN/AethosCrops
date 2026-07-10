package de.aethos.crops.Utils;

/**
 * Pro-Crop-Balancing aus der Config: Wirkung der drei Gene und die
 * Krankheits-Progression. Globale Defaults stehen unter 'genetics'/'diseases',
 * jeder Crop kann sie unter crops.<id>.genetics/diseases ueberschreiben.
 */
public record CropTuning(
        // Gen-Multiplikatoren: Wert 255 ergibt 1.0 + maxBonus (Wachstum/Ertrag)
        double growthMaxBonus,
        double yieldMaxBonus,
        // Resistenz 255 senkt die Krankheits-Chance auf 1.0 - maxReduction
        double resistanceMaxReduction,
        // Gesundheitsverlust in % pro Wachstums-Tick und Krankheits-Level
        double diseaseDamagePerLevel,
        int diseaseMaxLevel) {
}
