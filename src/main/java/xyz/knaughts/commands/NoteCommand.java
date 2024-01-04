package xyz.knaughts.commands;

import xyz.knaughts.database.Database;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import xyz.knaughts.models.Embeds;

/**
 * Represents the <code>/note</code> slash command. This command allows users to create a new note.
 */
public class NoteCommand extends ListenerAdapter {

    /**
     * Opens a modal where the user can create a new note.
     * @param event the slash command interaction event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("note")) return;

        final var title = TextInput.create("k-notes-title", "Title", TextInputStyle.SHORT)
                .setPlaceholder("My new note")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(30)
                .build();

        final var content = TextInput.create("k-notes-content", "Note Content", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Write your notes here...")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(500)
                .build();

        final Modal modal = Modal.create("k-notes-modal-create", "Create a note")
                .addComponents(ActionRow.of(title), ActionRow.of(content))
                .build();

        event.replyModal(modal).queue();
    }

    /**
     * Sends a database request to create the note.
     * @param event the modal interaction event
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("k-notes-modal-create")) return;
        final String title = event.getValues().get(0).getAsString();
        final String content = event.getValues().get(1).getAsString();
        Database.postNote(event.getUser().getId(), title, content)
                .thenAccept(id -> event.reply("Your note has been created! Note ID: `" + id + "`").setEphemeral(true).queue())
                .exceptionally(e -> {
                    event.replyEmbeds(Embeds.notesCreateErrorEmbed()).setEphemeral(true).queue();
                    return null;
                });
    }
}
