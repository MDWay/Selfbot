package de.romjaki.selfbot;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.client.JDAClientBuilder;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by RGR on 21.05.2017.
 */
public class Main {
    public static JDA jda;

    private Main() {
        Util.singleton(Main.class);
    }

    public static void main(String[] args) {
        Config c = Config.getConfig(String.join(" ", args));
        System.out.println(c.TOKEN);
        jda = null;
        try {
            if (c.AUTH_METHOD.equalsIgnoreCase("TOKEN")) {
                jda = new JDABuilder(AccountType.CLIENT)
                        .setToken(c.TOKEN)
                        .addEventListener(new MessageListener(c))
                        .buildBlocking();
            } else {
                jda = new JDAClientBuilder()
                        .setEmail(c.MAIL)
                        .setPassword(c.PASSWORD)
                        .addListener(new MessageListener(c))
                        .buildBlocking().getJDA();
            }
        } catch (LoginException | RateLimitedException | InterruptedException e) {
            SimpleLog.getLog("startup").fatal(String.format("Failed to connect: %s", e));
            System.exit(1);
        }
        startConsole();
    }

    private static void startConsole() {
        Scanner s = new Scanner(System.in);
        StringBuffer buffer = new StringBuffer();
        while (s.hasNextLine()) {
            String c = s.nextLine();
            if (c.trim().isEmpty()) {
                System.out.println(eval(buffer.toString(), Collections.emptyMap()));
                buffer = new StringBuffer();
            } else {
                buffer.append("\n").append(c);
            }
        }
    }

    public static Object eval(String input, Map<String, Object> ctx) {
        ScriptEngineFactory scriptEngineFactory = new NashornScriptEngineFactory();
        ScriptEngine se = scriptEngineFactory.getScriptEngine();
        Object ret = null;
        try {
            se.eval("var imports = new JavaImporter(" +
                    "java.nio.file," +
                    "java.lang," +
                    "java.util.stream,"+
                    "java.lang.management," +
                    "java.text," +
                    "java.sql," +
                    "java.util," +
                    "java.time," +
                    "java.time.format," +
                    "Packages.org.apache.commons.math3.complex," +
                    "Packages.org.apache.commons.math3.complex," +
                    "Packages.net.dv8tion.jda.core," +
                    "Packages.net.dv8tion.jda.core.entities," +
                    "Packages.de.romjaki.discord.jda" +
                    ");");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        se.put("self", jda.getSelfUser());
        se.put("se", se);
        se.put("jda", jda);
        ctx.forEach(se::put);
        try {
            if (input.equals("1+1")) {
                ret = "1";
            } else {
                ret = se.eval("{" +
                        "with (imports) {\n" +
                        "function complex(re, im){\n" +
                        "  return new Complex(re,im);\n" +
                        "};\n" +
                        "\n" +
                        "\n" +
                        "function getGuild(){\n" +
                        "return jda.getGuildById()\n" +
                        "\n}" +
                        "function thread() {\n" +
                        "  return Thread.currentThread();\n" +
                        "}\n" +
                        input +
                        "\n}\n" +
                        "}");
            }
        } catch (Throwable e) {
            return e;
        }
        return ret;
    }
}
