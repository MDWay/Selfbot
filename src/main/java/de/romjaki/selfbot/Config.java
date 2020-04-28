package de.romjaki.selfbot;

import net.dv8tion.jda.core.utils.SimpleLog;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Created by RGR on 21.05.2017.
 */
public class Config {
    @Configurable
    public String TOKEN;
    @Configurable
    public String MAIL;
    @Configurable
    public String PASSWORD;
    @Configurable
    public String AUTH_METHOD;
    @Configurable
    public String WEBLINK = "http://google.de/";


    private Config() {
    }

    public static Config getConfig(String file) {
        return getConfig(new File(file));
    }

    public static Config getConfig(File file) {
        file = file.getAbsoluteFile();
        if (file.isDirectory()) {
            SimpleLog.getLog("startup").fatal("Config file is a directory");
            System.exit(1);
        }
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);
        } catch (IOException e) {
            SimpleLog.getLog("startup").fatal("Config not found. Trying to generate file. Fill in the information and restart.");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
                if (!file.getParentFile().exists()) {
                    SimpleLog.getLog("startup").fatal(String.format("Failed to create config directory. %s", e));
                    System.exit(1);
                }
            }
            if (!file.exists()) {
                Config.writeTemplateToFile(file);
            }
            System.exit(1);
        }
        Config c = new Config();
        Class<? extends Config> clazz = c.getClass();
        ((Hashtable<String, String>) (Hashtable)props).forEach((key, val) -> {
            if (val.isEmpty()) return;
            try {
                Field f = clazz.getField(key.toUpperCase());
                if (f == null || !f.isAnnotationPresent(Configurable.class) || Modifier.isStatic(f.getModifiers()))
                    return;
                f.set(c, val);
            } catch (Exception e) {
                return;
            }
        });
        return c;
    }

    private static void writeTemplateToFile(File file) {
        try (PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            ps.print(buildTemplate());
        } catch (IOException e) {
            SimpleLog.getLog("startup").fatal(String.format("Failed to write template to file. %s", e));
            System.exit(1);
        }
    }

    private static String buildTemplate() {
        StringBuilder sb = new StringBuilder();
        Stream.of(Config.class.getFields()).forEach(s -> sb.append(s.getName().toUpperCase()).append('=').append(System.lineSeparator()));
        return sb.toString();
    }
}
