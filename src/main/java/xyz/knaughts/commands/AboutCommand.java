package xyz.knaughts.commands;

import xyz.knaughts.utils.variables.AuthorInfo;
import xyz.knaughts.utils.variables.Colors;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import xyz.knaughts.utils.variables.Images;

import java.util.List;

/**
 * Represents the <code>/about</code> slash command. This command provides information about Knaughts.
 */
public class AboutCommand extends ListenerAdapter {

    /**
     * Shows the user a modal with information about Knaughts.
     * @param event the slash command interaction event.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("about")) return;

        event.replyEmbeds(
                new MessageEmbed(
                        null,
                        "About Knaughts",
                        "Knaughts is a super-powered notes bot for Discord.",
                        null,
                        null,
                        Colors.ORANGE,
                        new MessageEmbed.Thumbnail(
                                Images.IMG_QUESTION,
                                null,
                                100,
                                100
                        ),
                        null,
                        AuthorInfo.QUESTION,
                        null,
                        null,
                        null,
                        List.of(
                                new MessageEmbed.Field(
                                        "Commands",
                                        """
                                                ∙ `/note` - Create a new note.
                                                ∙ `/notes [id]` - View your notes. Provide an `id` to view a specific note.
                                                ∙ `/about` - About Knaughts""",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "Links",
                                        """
                                                ∙ [Website](https://knaughts.xyz)
                                                ∙ [Support Server](https://knaughts.xyz/discord)
                                                ∙ [Source](https://knaughts.xyz/github)
                                                ∙ [Invite](https://knaughts.xyz/invite)""",
                                        false
                                )
                        )
                )
        ).setEphemeral(true).queue();
    }
}
