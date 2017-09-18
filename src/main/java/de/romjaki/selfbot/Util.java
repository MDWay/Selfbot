package de.romjaki.selfbot;

import net.dv8tion.jda.core.entities.TextChannel;

/**
 * Created by RGR on 19.05.2017.
 */
public class Util {
    private Util() {
        Util.singleton(Util.class);
    }

    public static void singleton(Class<?> clazz) {
        throw new Error("No " + clazz.toGenericString() + " instances for you!");
    }

    public static boolean isBotChannel(TextChannel channel) {
        return channel.getName().toLowerCase().contains("bot");
    }


    public static String escape(String join) {
        return join.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t");
    }

    public static int clamp(int min, int max, int val) {
        return val < min ? min : (val > max ? max : val);
    }
}
