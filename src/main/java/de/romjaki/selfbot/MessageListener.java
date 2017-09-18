package de.romjaki.selfbot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.SimpleLog;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.SequenceInputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.romjaki.selfbot.Main.jda;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by RGR on 21.05.2017.
 */
public class MessageListener extends ListenerAdapter {
    private static final Rot rotter = new Rot();
    private static final String AFK_IMAGE = "http://unisci24.com/data_images/wlls/2/174841-afk.jpg";
    static boolean AFK = false;
    static AtomicInteger name = new AtomicInteger(0);
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
    private static String CODEBLOCK = "To make a codeblock:\n" +
            "\n" +
            "\\`\\`\\`Language here\n" +
            "code here\n" +
            "\\`\\`\\`\n" +
            "\n" +
            "~ ~ ~ ~ ~ \n" +
            "\n" +
            "For example:\n" +
            "\n" +
            "\\`\\`\\`C\n" +
            "#include <iostream>\n" +
            "#include <cstdlib>\n" +
            "\n" +
            "int main()\n" +
            "{\n" +
            "\tstd::cout << \"Hello world!\\n\";\n" +
            "\treturn EXIT_SUCESS;\n" +
            "}\n" +
            "\\`\\`\\`\n" +
            "\n" +
            "Will print:\n" +
            "\n" +
            "```C\n" +
            "#include <iostream>\n" +
            "#include <cstdlib>\n" +
            "\n" +
            "int main()\n" +
            "{\n" +
            "\tstd::cout << \"Hello world!\\n\";\n" +
            "\treturn EXIT_SUCESS;\n" +
            "}\n" +
            "```\n";
    private static String[] strikeImages = new String[]{null};
    private Config config;

    public MessageListener(Config c) {
        this.config = c;
    }

    public static String getNextName() {
        return "" + name.getAndIncrement();
    }

    private static String constructHangman(String realWord, char[] alreadyGuessed) {
        String d = "";
        for (int i = 0; i < realWord.length(); i++) {
            char c = realWord.charAt(i);
            d += contains(alreadyGuessed, c) ? c : '-';
        }
        return d;
    }

    private static boolean contains(char[] alreadyGuessed, char c) {
        for (char ch : alreadyGuessed) {
            if (ch == c) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().equals(event.getJDA().getSelfUser())) {
            if (AFK && event.getMessage().getMentionedUsers().contains(jda.getSelfUser())) {
                event.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Color.green)
                        .setImage(AFK_IMAGE)
                        .setTitle("AFK")
                        .setDescription(jda.getSelfUser().getAsMention() + " is AFK.")
                        .setFooter("This afk-disclaimer was triggered because you mentioned " + jda.getSelfUser().getAsMention(), jda.getSelfUser().getEffectiveAvatarUrl())
                        .build()).queue(message -> message.delete().queueAfter(20, SECONDS));
            }
            return;
        }
        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        boolean containsAfk = false;
        for (MessageEmbed embed : embeds) {
            if (embed.getTitle().toLowerCase().contains("afk")) {
                containsAfk = true;
            }
        }
        if (!containsAfk && AFK) {
            AFK = false;
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
        if (raw.matches("(?is)^::codeblock.*")) {
            mes = codeblock(event);
        }
        if (raw.matches("(?is)^::eval\\s.*")) {
            Thread t = new Thread(() -> eval(event));
            t.start();
            mes = null;
        }
        if (raw.matches("(?is)^::moveto\\s.*")) {
            mes = moveTo(event);
        }
        if (raw.matches("(?is)^::afk.*")) {
            AFK = true;
            event.getMessage().delete().queue();
            mes = null;
        }
        if (raw.matches("(?is)^::hangman\\s.*")) {
            mes = hangman(event);
        }
        if (deleteAfter && mes != null) {
            mes.delete().queueAfter(5, SECONDS);
        }

    }

    private Message moveTo(MessageReceivedEvent event) {
        Message m = event.getMessage();
        String[] parts = m.getRawContent().split("\\s+", 3);
        if (parts.length < 2) {
            m.editMessage(new EmbedBuilder()
                    .setTitle("MOVE FAILED")
                    .setDescription("Missing category")
                    .build()).queue();
            return m;
        }
        String cat = parts[1].replaceAll("#|>|<", "");
        String reason = "Moved.";
        if (parts.length == 3) {
            reason = parts[2];
        }
        TextChannel toMove = event.getTextChannel();
        Category moveTo = event.getGuild().getCategoryById(cat);
        toMove.getManager().setParent(moveTo).reason(reason).queue();
        m.editMessage(new EmbedBuilder()
                .setTitle("MOVE SUCCEEDED")
                .setDescription("Moved " + toMove.getAsMention() + " to <#" + moveTo.getId() + ">")
                .setColor(Color.green)
                .build()).queue();
        return m;
    }

    private Message hangman(MessageReceivedEvent event) {
        SimpleLog.getLog("info").info("in hangman");
        String word = event.getMessage().getContent().replaceAll("(?is)^[>:]:hangman\\s+", "");
        event.getMessage().editMessage(hangmanEmbed(constructHangman(word, new char[0]), "").build()).queue();
        startHangmanProccess(event.getMessage(), word);
        return event.getMessage();
    }

    private EmbedBuilder hangmanEmbed(String s, String guesses) {
        return new EmbedBuilder()
                .setTitle("HANGMAN")
                .setDescription(s)
                .addField("Guesses", "`" + String.join(", ", guesses.split("")) + "`", false)
                .setColor(Color.blue);

    }

    private void startHangmanProccess(Message message, String realWord) {
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onGenericGuildMessageReaction(GenericGuildMessageReactionEvent event) {
                if (event.getMessageIdLong() != message.getIdLong()) {
                    return;
                }
                if (event.getMember().getUser().isBot()) {
                    return;
                }
                SimpleLog.getLog("hangman").info("in hangman");
                Message mes = event.getChannel().getHistoryAround(event.getMessageId(), 2).complete().getMessageById(event.getMessageId());
                List<MessageEmbed> embeds = mes.getEmbeds();
                String guesses = null;
                int strikes = 0;
                if (!embeds.isEmpty()) {
                    for (MessageEmbed embed : embeds) {
                        for (MessageEmbed.Field field : embed.getFields()) {
                            if (field.getName().toLowerCase().contains("guesses")) {
                                guesses = field.getValue().replaceAll("[^a-z]", "");
                            }
                            if (field.getName().toLowerCase().contains("strikes")) {
                                strikes = Integer.parseInt(field.getValue());
                            }
                            if (field.getName().toLowerCase().contains("solved")) {
                                return;
                            }
                        }
                    }
                }
                if (strikes > realWord.length() / 3 + 3) {
                    return;
                }
                char emoteD = Character.toLowerCase((char) (event.getReactionEmote().toString().charAt(4) - '\uDDE6' + 'a'));
                SimpleLog.getLog("hangman").info(emoteD);
                if (emoteD < 'a' || emoteD > 'z') {
                    return;
                }
                if (guesses.contains(String.valueOf(emoteD))) {
                    return;
                }
                if (!realWord.contains(String.valueOf(emoteD))) {
                    strikes++;
                }
                guesses += emoteD;
                String display = constructHangman(realWord, guesses.toCharArray());

                EmbedBuilder eB = hangmanEmbed(display, guesses);
                if (display.equalsIgnoreCase(realWord)) {
                    eB.addField("Solved by", event.getMember().getAsMention(), false);
                    eB.setColor(Color.green);
                }
                if (strikes > realWord.length() / 3 + 3) {
                    eB.setColor(Color.red);
                    eB.addField("FAILED", "(by) " + event.getMember().getAsMention(), false);
                }
                if (strikes > 0) {
                    eB.addField("Strikes", strikes + "", false);
                    eB.setImage(getImageForStrikes(strikes, realWord.length() / 3 + 3));
                }
                message.editMessage(eB.build()).queue();
            }
        });
    }

    private String getImageForStrikes(int strikes, int maxStrikes) {
        int index = strikes * strikeImages.length / maxStrikes;
        if (index >= strikeImages.length) {
            return null;
        }
        if (index < 0) {
            return null;
        }
        return strikeImages[index];
    }

    private Message codeblock(MessageReceivedEvent event) {
        event.getMessage().editMessage(CODEBLOCK).queue();
        return event.getMessage();
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
                parts[2] = parts[2].replaceAll("(?is)(?<rest>.*)<#(?<id>[0-9]+)>", "${rest}jda.getTextChannelById(\"${id}\")");//TEXTCHANNELS
                parts[2] = parts[2].replaceAll("(?is)(?<rest>.*)<@!?(?<id>[0-9]+)>", "${rest}guild.getMemberById(\"${id}\")");//MEMBERS
                parts[2] = parts[2].replaceAll("(?is)(?<rest>.*)<@&(?<id>[0-9]+)>", "${rest}guild.getRoleById(\"${id}\")");//ROLES
                parts[2] = parts[2].replaceAll("(?is)(?<rest>.*)<:(?<name>[a-z0-9]+):(?<id>[0-9]+)>", "${rest}jda.getEmoteById(\"${id}\")");//EMOTES

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

    private Object evalBf(int cellcount, String part) {
        byte[] cells = new byte[cellcount];
        int pointer = 0;
        Arrays.fill(cells, (byte) 0x0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            switch (c) {
                case '+': {
                    cells[pointer]++;
                    break;
                }
                case '-': {
                    cells[pointer]--;
                    break;
                }
                case '<': {
                    pointer--;
                    break;
                }
                case '>': {
                    pointer++;
                    break;
                }
                case '.': {
                    sb.append(cells[pointer]);
                    break;
                }
                case '[': {
                    if (cells[pointer] == (byte) 0x00) {
                        int depth = 0;
                        for (; i < part.length(); i++) {
                            char d = part.charAt(c);
                            if (d == '[') {
                                depth++;
                            }
                            if (d == ']') {
                                depth--;
                            }
                            if (depth == 0) {
                                break;
                            }
                        }
                    }
                    break;
                }
                case ']': {
                    if (cells[pointer] != (byte) 0x00) {
                        int depth = 0;
                        for (; i >= 0; i--) {
                            char d = part.charAt(c);
                            if (d == ']') {
                                depth++;
                            }
                            if (d == '[') {
                                depth--;
                            }
                            if (depth == 0) {
                                break;
                            }
                        }
                    }
                }
            }
            if (pointer < 0) {
                pointer += cellcount;
            }
            pointer %= cellcount;
        }
        return sb.toString();
    }

    private Object evalPy(String part) {
        try {
            String tempF = getNextName() + ".py";
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempF));
            writer.write(part);
            writer.flush();
            writer.close();
            Process p = Runtime.getRuntime().exec(new String[]{"python", tempF});
            Scanner s = new Scanner(new SequenceInputStream(p.getInputStream(), p.getErrorStream()));
            s.useDelimiter("\\A");
            return s;
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
