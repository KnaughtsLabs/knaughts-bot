package xyz.knaughts.events;

import xyz.knaughts.KnaughtsBot;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

public class OnReadyEvent implements EventListener {
    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            KnaughtsBot.LOG.info("Knaughts bot is ready!");
        }
    }
}
