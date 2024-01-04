package xyz.knaughts.events;

import xyz.knaughts.database.Database;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

/**
 * Represents when the bot joins a server.
 */
public class OnGuildJoinEvent implements EventListener {
    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof GuildJoinEvent)
            Database.postServer(((GuildJoinEvent) event).getGuild().getId());
    }
}
