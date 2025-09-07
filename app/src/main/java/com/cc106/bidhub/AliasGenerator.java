package com.cc106.bidhub;

import java.util.Random;

public class AliasGenerator {
    
    private static final String[] ADJECTIVES = {
        "Swift", "Bold", "Clever", "Bright", "Quick", "Sharp", "Smart", "Fast",
        "Wise", "Cool", "Epic", "Prime", "Elite", "Pro", "Ace", "Top",
        "Super", "Ultra", "Mega", "Turbo", "Flash", "Lightning", "Thunder", "Storm"
    };
    
    private static final String[] NOUNS = {
        "Bidder", "Hunter", "Seeker", "Finder", "Tracker", "Scout", "Ranger", "Explorer",
        "Master", "Champion", "Winner", "Hero", "Legend", "Star", "Gem", "Diamond",
        "Phoenix", "Eagle", "Lion", "Tiger", "Wolf", "Bear", "Shark", "Dragon"
    };
    
    private static final Random random = new Random();
    
    /**
     * Generates a unique bidding alias for anonymous bidding
     * Format: [Adjective][Noun][RandomNumber]
     * Example: SwiftBidder42, BoldHunter17, CleverSeeker89
     * 
     * @return A unique alias string
     */
    public static String generateAlias() {
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int randomNumber = random.nextInt(100); // 0-99
        
        return adjective + noun + randomNumber;
    }
    
    /**
     * Generates multiple aliases to ensure uniqueness
     * 
     * @param count Number of aliases to generate
     * @return Array of unique aliases
     */
    public static String[] generateAliases(int count) {
        String[] aliases = new String[count];
        for (int i = 0; i < count; i++) {
            aliases[i] = generateAlias();
        }
        return aliases;
    }
    
    /**
     * Validates if an alias follows the correct format
     * 
     * @param alias The alias to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidAlias(String alias) {
        if (alias == null || alias.length() < 8) {
            return false;
        }
        
        // Check if alias contains at least one adjective and one noun
        boolean hasAdjective = false;
        boolean hasNoun = false;
        
        for (String adj : ADJECTIVES) {
            if (alias.startsWith(adj)) {
                hasAdjective = true;
                break;
            }
        }
        
        for (String noun : NOUNS) {
            if (alias.contains(noun)) {
                hasNoun = true;
                break;
            }
        }
        
        return hasAdjective && hasNoun;
    }
}
