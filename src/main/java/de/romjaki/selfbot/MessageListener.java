package de.romjaki.selfbot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.fabianm.brainfuck.BrainfuckEngine;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by RGR on 21.05.2017.
 */
public class MessageListener extends ListenerAdapter {
    private static final Rot rotter = new Rot();
    private static String SANDUHRDINGS = "                  -`\n" +
            "                 .o+`\n" +
            "                `ooo/\n" +
            "               `+oooo:\n" +
            "              `+oooooo:\n" +
            "              -+oooooo+:\n" +
            "            `/:-:++oooo+:\n" +
            "           `/++++/+++++++:\n" +
            "          `/++++++++++++++:\n" +
            "         `/+++ooooooooooooo/`\n" +
            "        ./ooosssso++osssssso+`\n" +
            "       .oossssso-`` ``/ossssss+`\n" +
            "      -osssssso.      :ssssssso.\n" +
            "     :osssssss/        osssso+++.\n" +
            "    /ossssssss/        +ssssooo/-\n" +
            "  `/ossssso+/:-        -:/+osssso+-\n" +
            " `+sso+:-`                 `.-/+oso:\n" +
            "`++:.                           `-/+/\n" +
            ".`";

    private static String PLUS_SIGN = "```\n" +
            "  +\n" +
            "  +\n" +
            "+++++\n" +
            "  +\n" +
            "  +\n" +
            "  +\n" +
            "  +```";

    private Config config;

    public MessageListener(Config c) {
        this.config = c;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().equals(event.getJDA().getSelfUser())) {
            return;
        }
        Message message = event.getMessage();
        String raw = message.getRawContent();
        Message mes = null;
        boolean deleteAfter = false;
        if (raw.startsWith(":>")) {
            raw = raw.replaceFirst(":>", "::");
            deleteAfter = true;
        }
        if (raw.matches("(?si)^::embed\\s.*")) {
            mes = embed(event);
        }
        if (raw.matches("(?is)^::cite\\s.*")) {
            mes = cite(event);
        }
        if (raw.matches("(?si)^::game\\s.*")) {
            mes = game(event);
        }
        if (raw.matches("(?is)^::time.*")) {
            mes = time(event);
        }
        if (raw.matches("(?is)^::google\\s.*")) {
            mes = google(event);
        }
        if (raw.matches("(?is)^::spam\\s.*")) {
            mes = spam(event);
        }
        if (raw.matches("(?is)^::rot\\s.+")) {
            mes = rot(event);
        }
        if (raw.matches("(?is)^::sanduhrdings")) {
            mes = sanduhrdings(event);
        }
        if (raw.equals("+")) {
            message.editMessage(PLUS_SIGN).queue();
        }
        if (raw.matches("(?is)^::copy")) {
            mes = copy(event);
        }
        if (raw.matches("(?is)^::purge\\s.*")) {
            purge(event);
        }
        if (raw.matches("(?is)^::eval\\s.*")) {
            Thread t = new Thread(() -> eval(event));
            t.start();
            mes = null;
        }
        if (deleteAfter && mes != null) {
            mes.delete().queueAfter(5, SECONDS);
        }

    }

    private void eval(MessageReceivedEvent event) {
        String[] parts = event.getMessage().getRawContent().split("\\s+", 3);
        if (parts.length != 3) {
            event.getMessage().editMessage("Error: Use `::event <java|python|bf> <code>");
            return;
        }
        Object out = "";
        int x = 8;
        if (parts[1].matches(".*\\d$")) {
            x = Integer.parseInt(parts[1].replaceFirst("b(rain)?f(uck)?", ""));
            parts[1] = "bf";
        }
        switch (parts[1].toLowerCase()) {
            case "bf":
            case "brainfuck":
                out = evalBf(x, parts[2]);
                break;
            case "java":
                Map<String, Object> map = new HashMap<>();
                map.put("channel", event.getChannel());
                map.put("message", event.getMessage());
                map.put("guild", event.getGuild());
                out = Main.eval(parts[2], map);
                break;
            case "python":
                out = evalPy(parts[2]);
                break;
            default:
                out = new Exception("Unknown Language");
        }
        EmbedBuilder eB = new EmbedBuilder();
        eB.addField(":inbox_tray:Input", "```" + parts[1] + "\n" + parts[2] + "```", false);
        if (out instanceof Throwable) {
            eB.addField(":x:Exception", "```" + parts[1] + "\n" + out + "```", false);
        } else {
            eB.addField(":outbox_tray:Output", "```" + parts[1] + "\n" + out + "```", false);
        }
        event.getMessage().editMessage(eB.build()).queue();
    }

    private Object evalBf(int cells, String part) {
        StringWriter writer = new StringWriter();
        BrainfuckEngine engine = new BrainfuckEngine(cells, new BufferedOutputStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                writer.write(b);
            }
        }));
        try {
            engine.interpret(part);
        } catch (Exception e) {
            return e;
        }
        return writer.getBuffer();
    }

    private Object evalPy(String part) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"python"});
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            writer.write(part);
            writer.flush();
            p.waitFor();
            Scanner s = new Scanner(p.getInputStream());
            s.useDelimiter("\\A");
            return s.next();
        } catch (Exception e) {
            return e;
        }

    }

    private void purge(MessageReceivedEvent event) {
        String[] tmp = event.getMessage().getRawContent().split("\\s+");
        event.getMessage().delete().complete();
        int amount;
        if (tmp.length < 2) {
            amount = 100;
        } else {
            amount = clamp(Integer.parseInt(tmp[1]), 2, 100);
        }
        event.getChannel().getHistory().retrievePast(amount).complete().stream().filter(m -> m.getAuthor().equals(event.getJDA().getSelfUser())).forEach(m -> m.delete().queue());
    }

    private int clamp(int i, int min, int max) {
        if (min > max) throw new IllegalArgumentException("min > max");
        if (i < min) return min;
        if (i > max) return max;
        return i;
    }

    private Message copy(MessageReceivedEvent event) {
        Message m = event.getMessage();
        long id = Long.valueOf(m.getRawContent().split("\\s+")[1]);
        m.editMessage("```\n" + event.getChannel().getMessageById(id).complete().getRawContent() + "\n```").queue();
        return m;
    }

    private Message sanduhrdings(MessageReceivedEvent event) {
        event.getMessage().delete().queue();
        return event.getChannel().sendMessage("```\n" + SANDUHRDINGS + "\n```").complete();
    }


    private Message rot(MessageReceivedEvent event) {
        String[] parts = event.getMessage().getRawContent().replaceFirst("(?i)^:[:>]rot\\s+", "").split("\\s+", 2);
        event.getMessage().delete().queue();
        if (parts.length < 2) {
            return event.getChannel().sendMessage(new EmbedBuilder()
                    .setTitle("Error")
                    .setColor(Color.red)
                    .setDescription("Too few arguments: `::rot <rotN> <text>`")
                    .build()).complete();
        }
        int rot = Integer.parseInt(parts[0]) % 26;
        String message = parts[1];

        return event.getChannel().sendMessage(rotter.encrypt(message, rot * 2, false)).complete();
    }

    private Message spam(MessageReceivedEvent event) {
        String[] tmp = event.getMessage().getRawContent().replaceFirst("(?i)^:[:>]spam\\s+", "").split("\\s+", 2);
        if (tmp.length == 0) {
            return null;
        }
        String rep = "";
        int repc = 16;
        if (tmp.length == 1) {
            rep = tmp[0];
        }
        if (tmp.length == 2) {
            rep = tmp[1];
            repc = Integer.parseInt(tmp[0]);
        }
        if (repc > 0) {
            StringBuilder txt = new StringBuilder(repc * rep.length());
            for (int i = 0; i < repc; i++) {
                txt.append(rep + "\n");
            }
            event.getMessage().delete().queue();
            SimpleLog.getLog("command").info(String.format("Command spam executed: {\"text\":\"%s\",count=%d}", rep, repc));
            return event.getChannel().sendMessage(txt.toString()).complete();
        }
        if (repc < 0) {
            SimpleLog.getLog("command").info(String.format("Command spam executed with separated messages: {\"text\":\"%s\",count=%d}", rep, -repc));
            event.getMessage().delete().queue();
            for (int i = 0; i < -repc; i++) {
                event.getChannel().sendMessage(rep).queue();
            }
        }
        return null;
    }

    private Message google(MessageReceivedEvent event) {
        String text = event.getMessage().getRawContent().replaceFirst("(?i)^:[:>]google\\s+", "");
        String raw = text.replace(' ', '+');
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.red);
        embed.setTitle(text, "https://google.com/search?q=" + raw);
        embed.setImage("https://lh4.googleusercontent.com/-v0soe-ievYE/AAAAAAAAAAI/AAAAAAADwG4/8CFr3X3I_Fs/s0-c-k-no-ns/photo.jpg");
        event.getMessage().delete().queue();
        SimpleLog.getLog("command").info(String.format("Executed command google: {text:\"%s\"", text));
        return event.getChannel().sendMessage(embed.build()).complete();
    }

    private Message time(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.cyan);
        embed.setTitle("Uhrzeit", "https://www.google.de/search?q=uhrzeit");
        embed.addField(":timer:", new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()), true);
        event.getMessage().delete().queue();
        SimpleLog.getLog("command").info("Executed command time.");
        Message ret = event.getChannel().sendMessage(embed.build()).complete();
        return ret;
    }

    private Message game(MessageReceivedEvent event) {
        String line = event.getMessage().getRawContent();
        String args = line.replaceFirst("(?i)^:[>:]game\\s+", "");
        Game g = Game.of(args);
        event.getJDA().getPresence().setGame(g);
        SimpleLog.getLog("command").info(String.format("Executed command game: {name: \"%s\"}", args));
        event.getMessage().delete().queue();
        return null;
    }


    private Message cite(MessageReceivedEvent event) {
        String line = event.getMessage().getRawContent();
        String[] args = line.replaceFirst("^(?i):[:>]cite\\s+", "").split("\\s+", 2);
        EmbedBuilder embed = new EmbedBuilder();
        MessageHistory h = event.getChannel().getHistoryAround(args[0], 2).complete();
        Message cited = h.getMessageById(args[0]);
        embed.setAuthor(cited.getAuthor().getName(), "https://discordapp.com", cited.getAuthor().getAvatarUrl());
        embed.setColor(Color.CYAN);
        embed.setDescription(cited.getContent());
        embed.addField("**" + event.getJDA().getSelfUser().getName() + "** kommentiert:", args[1], true);
        event.getMessage().delete().queue();
        SimpleLog.getLog("command").info(String.format("Executed command cite: {messageId: %s, description:\"%s\"}", cited.getId(), args[1]));
        return event.getChannel().sendMessage(embed.build()).complete();
    }

    private Message embed(MessageReceivedEvent event) {

        String line = event.getMessage().getRawContent();
        String[] args = line.replaceFirst("^(?i):[>:]embed\\s+", "").split("\\s+", 2);
        EmbedBuilder embed = new EmbedBuilder();
        String text = args[1];
        String color = args[0];
        Color col = Color.green;
        if (color.startsWith("#")) {
            col = new Color(Integer.parseInt(color.replaceFirst("#", ""), 16));
        } else if (color.matches("^(?i)[a-z]+$")) {
            Class<Color> clazz = Color.class;
            try {
                Field f = clazz.getField(color.toLowerCase());
                col = (Color) f.get(null);
            } catch (Exception e) {
                SimpleLog.getLog("command").info(String.format("Color %s not found.", color));
            }
        } else if (color.matches("^[0-9]+$")) {
            col = new Color(Integer.valueOf(color));
        } else {
            SimpleLog.getLog("command").info(String.format("No match found: %s", color));
        }
        embed.setColor(col);
        if (text.contains("\n")) {
            String[] tmp = text.split("\n", 2);
            embed.setTitle(tmp[0], config.WEBLINK);
            embed.setDescription(tmp[1]);
        } else {
            embed.setDescription(text);
        }
        event.getMessage().delete().queue();
        SimpleLog.getLog("command").info(String.format("Executed \"embed\" command: {color:\"%s\",text:\"%s\"}", col, text));
        return event.getChannel().sendMessage(embed.build()).complete();
    }
}
