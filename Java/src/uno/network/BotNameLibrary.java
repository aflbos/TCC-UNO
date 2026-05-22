package uno.network;

import java.util.*;

final class BotNameLibrary {
    private BotNameLibrary() {}

    private static final String[] ADJECTIVES = {
            "Curious", "Brave", "Sunny", "Clever", "Mellow", "Swift", "Chill", "Lucky",
            "Gentle", "Spicy", "Witty", "Quiet", "Bold", "Zany", "Nimble", "Cosmic",
            "Rapid", "Sneaky", "Calm", "Fierce", "Jolly", "Tiny", "Epic", "Shiny",
            "Sly", "Daring", "Icy", "Toasty", "Neon", "Crimson", "Azure", "Golden",
            "Silver", "Velvet", "Rustic", "Lunar", "Solar", "Stormy", "Breezy", "Jazzy",
            "Turbo", "Steady", "Playful", "Keen", "Mighty", "Glowing", "Proud", "Plucky"
    };

    private static final String[] NOUNS = {
            "Fox", "Otter", "Panda", "Koala", "Raven", "Tiger", "Lynx", "Hawk",
            "Cobra", "Gecko", "Badger", "Wolf", "Falcon", "Dolphin", "Jaguar", "Turtle",
            "Viper", "Eagle", "Bison", "Shark", "Mantis", "Cheetah", "Panther", "Orca",
            "Cougar", "Wombat", "Moose", "Heron", "Puma", "Ferret", "Yak", "Stingray",
            "Comet", "Meteor", "Nova", "Blaze", "Spark", "Echo", "Cipher", "Pixel",
            "Rocket", "Vector", "Phantom", "Orbit", "Avalanche", "Thunder", "Cyclone", "Monsoon"
    };

    static String generate(Random rng, Set<String> reserved, String prefix) {
        if (rng == null) rng = new Random();
        if (reserved == null) reserved = Collections.emptySet();
        if (prefix == null) prefix = "";

        for (int tries = 0; tries < 200; tries++) {
            String name = prefix
                    + ADJECTIVES[rng.nextInt(ADJECTIVES.length)]
                    + " "
                    + NOUNS[rng.nextInt(NOUNS.length)];
            if (!reserved.contains(name)) return name;
        }

        for (int i = 2; i < 10_000; i++) {
            String name = prefix + "Bot " + i;
            if (!reserved.contains(name)) return name;
        }

        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }
}

