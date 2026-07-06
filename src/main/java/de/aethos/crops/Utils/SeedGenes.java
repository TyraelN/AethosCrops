package de.aethos.crops.Utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Genetische Werte eines einzelnen Samens, jeweils im Byte-Bereich 0-255.
 * <p>
 * 0 ist der neutrale Default (Vanilla-Verhalten); Gene wirken nur verbessernd:
 * growth     - beschleunigt das Wachstum (1.0x - 2.0x)
 * yield      - erhoeht die Drop-Menge bei der Ernte (1.0x - 2.0x)
 * resistance - senkt die Krankheits-Wahrscheinlichkeit (Faktor 1.0x - 0.5x)
 * <p>
 * Nach aussen sichtbar ist nur die Sterne-Guete (1-5), der gerundete
 * Durchschnitt aller drei Werte. Samen gleicher Guete stacken miteinander.
 */
public final class SeedGenes {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 255;

    private final int growth;
    private final int yield;
    private final int resistance;

    public SeedGenes(int growth, int yield, int resistance) {
        this.growth = clamp(growth);
        this.yield = clamp(yield);
        this.resistance = clamp(resistance);
    }

    public int getGrowth() {
        return growth;
    }

    public int getYield() {
        return yield;
    }

    public int getResistance() {
        return resistance;
    }

    public double getAverage() {
        return (growth + yield + resistance) / 3.0D;
    }

    // Guete 1-5: Gesamtwert 0..765 linear auf die fuenf Sterne-Stufen verteilt.
    public int getStars() {
        return 1 + ((growth + yield + resistance) * 5) / (3 * (MAX_VALUE + 1));
    }

    public double growthMultiplier() {
        return 1.0D + growth / (double) MAX_VALUE;
    }

    public double yieldMultiplier() {
        return 1.0D + yield / (double) MAX_VALUE;
    }

    public double diseaseChanceMultiplier() {
        return 1.0D - resistance / (2.0D * MAX_VALUE);
    }

    // Vererbung: jeder Wert weicht zufaellig um bis zu +/- spread vom Elternwert ab.
    public SeedGenes mutate(int spread) {
        if (spread <= 0) {
            return this;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new SeedGenes(
                growth + random.nextInt(-spread, spread + 1),
                yield + random.nextInt(-spread, spread + 1),
                resistance + random.nextInt(-spread, spread + 1)
        );
    }

    public static SeedGenes random() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new SeedGenes(
                random.nextInt(MIN_VALUE, MAX_VALUE + 1),
                random.nextInt(MIN_VALUE, MAX_VALUE + 1),
                random.nextInt(MIN_VALUE, MAX_VALUE + 1)
        );
    }

    public String serialize() {
        return growth + "." + yield + "." + resistance;
    }

    public static SeedGenes deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split("\\.", 3);
        if (parts.length != 3) {
            return null;
        }

        try {
            return new SeedGenes(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int clamp(int value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof SeedGenes genes)) {
            return false;
        }

        return growth == genes.growth && yield == genes.yield && resistance == genes.resistance;
    }

    @Override
    public int hashCode() {
        return (growth << 16) | (yield << 8) | resistance;
    }

    @Override
    public String toString() {
        return "SeedGenes[growth=" + growth + ", yield=" + yield + ", resistance=" + resistance + "]";
    }
}
