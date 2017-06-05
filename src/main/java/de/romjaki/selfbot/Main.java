package de.romjaki.selfbot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;

import javax.security.auth.login.LoginException;

/**
 * Created by RGR on 21.05.2017.
 */
public class Main {
    private Main() {
        Util.singleton(Main.class);
    }

    public static void main(String[] args) {
        Config c = Config.getConfig(String.join(" ", args));
        JDA jda = null;
        try {
            jda = new JDABuilder(AccountType.CLIENT)
                    .setToken(c.TOKEN)
                    .addEventListener(new MessageListener(c))
                    .buildAsync();
        } catch (LoginException | RateLimitedException e) {
            SimpleLog.getLog("startup").fatal(String.format("Failed to connect: %s", e));
            System.exit(1);
        }

    }

}
