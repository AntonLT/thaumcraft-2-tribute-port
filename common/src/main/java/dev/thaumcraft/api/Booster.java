package dev.thaumcraft.api;

/**
 * What one block adds when placed around an enchanting table or Quaesitum, in the same ring as vanilla bookshelves.
 * A bookshelf is 1, 2, 1, 0.25; a brain in a jar is 4, 4, 2, 1.
 * A Quaesitum counts only boosters it can see past an enchantment power transmitter, like the vanilla table,
 * takes the 16 strongest, triples their research speed, and doubles their other research values.
 */
public interface Booster {
    /** Enchanting power for Thaumcraft's enchanting machines. */
    int enchanting();
    /** Ticks removed from each Quaesitum research cycle, down to a 20-tick minimum. */
    int researchSpeed();
    /** Added to the Quaesitum success bonus, which scales the base success chance by {@code (100 + bonus)}. */
    int researchBonus();
    /** Lowers the chance that a failed step costs theory progress, and the chance that inputs are consumed. */
    double failureProtection();
}
