package xyz.knaughts.events;

import xyz.knaughts.database.Database;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

public class OnGuildLeaveEvent implements EventListener {

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (!(event instanceof GuildLeaveEvent)) return;
        Database.patchServer(((GuildLeaveEvent) event).getGuild().getIdLong(), false);
    }
}
