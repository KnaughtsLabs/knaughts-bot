package xyz.knaughts;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import xyz.knaughts.commands.AboutCommand;
import xyz.knaughts.commands.NoteCommand;
import xyz.knaughts.commands.NotesCommand;
import xyz.knaughts.database.Database;
import xyz.knaughts.events.OnGuildJoinEvent;
import xyz.knaughts.events.OnGuildLeaveEvent;
import xyz.knaughts.events.OnReadyEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;
import xyz.knaughts.utils.Cryptography;

/**
 * The Knaughts bot client.
 */
public class KnaughtsBot {
    public static final Logger LOG = JDALogger.getLog(KnaughtsBot.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting bot...");
        Cryptography.init();
        Database.init();

        final JDABuilder builder = JDABuilder
                .createDefault(System.getenv("KNAUGHTS_BOT_TOKEN"))
                .disableCache(CacheFlag.ACTIVITY)
                .setLargeThreshold(50)
                .setActivity(Activity.watching("for /note"))
                .addEventListeners(
                    new NoteCommand(),
                    new AboutCommand(),
                    new NotesCommand(),
                    new OnGuildJoinEvent(),
                    new OnGuildLeaveEvent(),
                    new OnReadyEvent()
                );

        LOG.info("Building bot...");
        final JDA bot = builder.build();

        LOG.info("Registering commands...");
        bot.updateCommands().addCommands(
                Commands.slash("note", "Create a new note"),
                Commands.slash("notes", "View your notes")
                        .addOption(OptionType.STRING, "id", "The ID of the note you want to view. Leave blank if you want to see all.", false),
                Commands.slash("about", "About Knaughts")
        ).queue();

        bot.awaitReady();
    }
}