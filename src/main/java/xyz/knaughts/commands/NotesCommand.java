package xyz.knaughts.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import xyz.knaughts.exceptions.NoteNotFoundException;
import xyz.knaughts.database.Database;
import xyz.knaughts.exceptions.NotesListNotFoundException;
import xyz.knaughts.models.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import xyz.knaughts.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents the <code>/notes</code> slash command. This command allows users to view, edit and delete their note(s).
 */
public class NotesCommand extends ListenerAdapter {

    /**
     * Determines which EmbedBuilder to send based on whether an id is provided; either sends
     * a single note or a list of notes, or an error embed if an error occurs.
     * @param event The slash command interaction event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("notes")) return;

        if (!event.getOptions().isEmpty()) {
            if (event.getOption("id").getAsString().length() != 15) {
                event.replyEmbeds(Embeds.noteInvalidIdErrorEmbed()).setEphemeral(true).queue();
            } else {
                handleViewNote(event, event.getOption("id").getAsString(), event.getUser());
            }
        } else {
            handleGetNotes(event, 1, false);
        }
    }

    /**
     * Determines which action to take based on the button ID.
     * @param event The button interaction event
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!Objects.requireNonNull(event.getButton().getId()).startsWith("knaughts.notes")) return;

        final String[] args = event.getButton().getId().split("\\.");
        final User user = event.getUser();

        switch (args[2]) {
            case "next", "prev" -> handleGetNotes(event, Integer.parseInt(args[4]), true);
            case "view" -> handleViewNote(event, args[3], user);
            case "edit" -> handleEditNote(event, args[3]);
            case "delete" -> handleDeleteNote(event, args[3], user);
            default -> throw new RuntimeException("Invalid button ID: " + event.getButton().getId());
        }
    }

    /**
     * Sends a database request to update a note.
     * @param event the modal interaction event.
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("k-notes-modal-edit")) return;

        final String[] args = event.getModalId().split("-");
        final var title = event.getValues().get(0).getAsString();
        final var content = event.getValues().get(1).getAsString();
        Database.patchNote(event.getUser().getId(), args[4], title, content)
                .thenAccept(note -> {
                    final Embeds.EmbedBuilderRowPair pair = Embeds.noteEmbed(event.getUser().getName(), event.getUser().getAvatarUrl(), note);
                    event.replyEmbeds(pair.embedBuilder().build())
                            .setActionRow(pair.row())
                            .setEphemeral(true)
                            .queue();
                })
                .exceptionally(e -> {
                    event.replyEmbeds(Embeds.noteEditErrorEmbed()).setEphemeral(true).queue();
                    return null;
                });
    }

    /**
     * Sends a database request to get a list of the user's notes which then sends an embed with the notes' information.
     * @param event the slash command interaction event.
     * @param page the page number to get.
     * @param edit whether to edit or reply to the original message.
     */
    private void handleGetNotes(IReplyCallback event, int page, boolean edit) {
        Database.getNotes(event.getUser().getId(), page)
                .thenAccept(future -> {
                    final var notes = future.notes();

                    if (notes.isEmpty())
                        event.replyEmbeds(Embeds.notesListNotFoundErrorEmbed()).setEphemeral(true).queue();
                    else
                        sendNotesList(event, future.currentPage(), future.totalPages(), notes, event.getUser(), edit);
                })
                .exceptionally(e -> {
                    handleNoteGetError(event, e.getCause());
                    return null;
                });
    }

    /**
     * Sends a database request to get a single note which then sends an embed with the note's information.
     * @param event the slash command interaction event.
     * @param noteId the note ID to get.
     * @param user the user who sent the slash command.
     */
    private void handleViewNote(IReplyCallback event, String noteId, User user) {
        Database.getNote(noteId, event.getUser().getId())
                .thenAccept(note -> {
                    final Embeds.EmbedBuilderRowPair pair = Embeds.noteEmbed(user.getName(), user.getAvatarUrl(), note);
                    event.replyEmbeds(pair.embedBuilder().build())
                            .setActionRow(pair.row())
                            .setEphemeral(true)
                            .queue();
                })
                .exceptionally(e -> {
                    handleNoteGetError(event, e.getCause());
                    return null;
                });
    }

    /**
     * Opens a modal where the user can edit a note. The current note content is gotten from a database request.
     * @param event the button interaction event.
     * @param noteId the note ID to edit.
     */
    private void handleEditNote(ButtonInteractionEvent event, String noteId) {
        Database.getNote(noteId, event.getUser().getId())
                .thenAccept(note -> {
                    final var title = TextInput.create("k-notes-title", "Title", TextInputStyle.SHORT)
                            .setRequired(true)
                            .setMinLength(1)
                            .setMaxLength(30)
                            .setValue(note.title())
                            .build();

                    final var content = TextInput.create("k-notes-content", "Content", TextInputStyle.PARAGRAPH)
                            .setRequired(true)
                            .setMinLength(1)
                            .setMaxLength(500)
                            .setValue(note.content())
                            .build();

                    final var modal = Modal.create("k-notes-modal-edit-" + note.id(), "Edit note")
                            .addComponents(ActionRow.of(title), ActionRow.of(content))
                            .build();

                    event.replyModal(modal).queue();
                })
                .exceptionally(e -> {
                    handleNoteGetError(event, e.getCause());
                    return null;
                });
    }

    /**
     * Sends a database request to delete a note and confirms the deletion with an embed.
     * @param event the button interaction event.
     * @param noteId the note ID to delete.
     * @param user the user who sent the slash command.
     */
    private void handleDeleteNote(ButtonInteractionEvent event, String noteId, User user) {
        Database.deleteNote(noteId, user.getId())
                .thenAccept(deleted -> {
                    if (deleted)
                        event.replyEmbeds(Embeds.noteDeleteEmbed()).setEphemeral(true).queue();
                    else
                        event.replyEmbeds(Embeds.noteDeleteErrorEmbed()).setEphemeral(true).queue();
                })
                .exceptionally(e -> {
                    event.replyEmbeds(Embeds.noteDeleteErrorEmbed()).setEphemeral(true).queue();
                    return null;
                });
    }

    /**
     * Sends an embed with a list of notes.
     * @param event the slash command interaction event.
     * @param currentPage the current page number.
     * @param totalPages the total number of pages.
     * @param notes the list of notes.
     * @param user the user who sent the slash command.
     * @param edit whether to edit or reply to the original message. Should be true if a user is cycling through pages.
     */
    private void sendNotesList(IReplyCallback event, int currentPage, int totalPages, List<Note> notes, User user, boolean edit) {
        final Embeds.EmbedBuilderRowPair pair = Embeds.notesListEmbed(currentPage, totalPages, notes, user);
        if (edit && !(event instanceof ButtonInteractionEvent)) {
            throw new IllegalArgumentException("Cannot edit a message that is not a button interaction event");
        }

        if (edit) {
            ((ButtonInteractionEvent) event).deferEdit().setEmbeds(pair.embedBuilder().build()).setActionRow(pair.row())
                    .queue(hook -> hook.editOriginalEmbeds(Embeds.timeoutEmbed())
                            .setComponents(new ArrayList<>())
                            .queueAfter(30, TimeUnit.SECONDS));
        } else {
            event.deferReply().setEmbeds(pair.embedBuilder().build()).setActionRow(pair.row()).setEphemeral(true)
                    .queue(hook -> hook.editOriginalEmbeds(Embeds.timeoutEmbed())
                            .setComponents(new ArrayList<>())
                            .queueAfter(30, TimeUnit.SECONDS));
        }
    }

    /**
     * Sends an error embed based on the exception.
     * @param event the slash command interaction event.
     * @param throwable the exception.
     */
    private void handleNoteGetError(IReplyCallback event, Throwable throwable) {
        if (throwable instanceof NoteNotFoundException)
            event.replyEmbeds(Embeds.noteInvalidIdErrorEmbed()).setEphemeral(true).queue();
        else if (throwable instanceof NotesListNotFoundException)
            event.replyEmbeds(Embeds.notesListNotFoundErrorEmbed()).setEphemeral(true).queue();
        else
            event.replyEmbeds(Embeds.notesGetErrorEmbed()).setEphemeral(true).queue();
    }
}