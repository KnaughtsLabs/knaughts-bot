package xyz.knaughts.models;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import xyz.knaughts.utils.variables.AuthorInfo;
import xyz.knaughts.utils.variables.Colors;
import xyz.knaughts.utils.variables.Images;

import java.util.ArrayList;
import java.util.List;

public final class Embeds {
    private Embeds() {
    }

    public static EmbedBuilderRowPair noteEmbed(String name, String avUrl, Note note) {
        final var embed = noteEmbedBuilder(name, avUrl, note);
        final var row = noteRow(note.id());
        return new EmbedBuilderRowPair(embed, row);
    }

    public static EmbedBuilderRowPair notesListEmbed(int currentPage, int totalPages, List<Note> notes, User user) {
        final var embedBuilder = notesListEmbedBuilder(user, currentPage, totalPages);
        final var row = notesRow(user, notes, embedBuilder, currentPage, totalPages);

        return new EmbedBuilderRowPair(embedBuilder, row);
    }

    private static List<ItemComponent> notesRow(User user, List<Note> notes, EmbedBuilder embed, int currentPage, int totalPages) {
        final var row = new ArrayList<ItemComponent>();

        if (currentPage > 1) {
            row.add(Button.of(ButtonStyle.PRIMARY,
                    String.format("knaughts.notes.prev.%s.%d", user.getId(), currentPage-1),
                    "⬅️"));
        }

        for (int i = 0; i < notes.size(); i++) {
            final Note note = notes.get(i);
            final String title = note.title().length() > 10 ? note.title().substring(0, 10) + "..." : note.title();
            final String content = note.content().length() > 50 ? note.content().substring(0, 50) + "..." : note.content();

            final MessageEmbed.Field field = new MessageEmbed.Field(
                    String.format("%d. %s ⎯ `%s`", i+1, title, note.id()),
                    "> " + content,
                    false
            );
            embed.addField(field);

            row.add(Button.of(ButtonStyle.SECONDARY, "knaughts.notes.view." + note.id(), "#"+(i+1)));
        }

        if (currentPage < totalPages) {
            row.add(Button.of(ButtonStyle.PRIMARY,
                    String.format("knaughts.notes.next.%s.%d", user.getId(), currentPage+1),
                    "➡️"));
        }

        return row;
    }

    private static List<ItemComponent> noteRow(String noteId) {
        final List<ItemComponent> row = new ArrayList<>();
        row.add(Button.of(ButtonStyle.PRIMARY, "knaughts.notes.edit." + noteId, "Edit"));
        row.add(Button.of(ButtonStyle.DANGER, "knaughts.notes.delete." + noteId, "Delete"));
        return row;
    }

    private static EmbedBuilder notesListEmbedBuilder(User user, int currentPage, int totalPages) {
        return new EmbedBuilder()
                .setTitle("`" + user.getName() + "`'s Notes")
                .setColor(Colors.ORANGE)
                .setFooter(String.format("Page %d of %d", currentPage, totalPages), Images.IMG_LOGO);
    }

    private static EmbedBuilder noteEmbedBuilder(String username, String avUrl, Note note) {
        return new EmbedBuilder()
                .setAuthor(username + "'s note", null, avUrl)
                .setTitle(note.title() +" ⎯`" + note.id() + "`")
                .setDescription(note.content())
                .setColor(Colors.ORANGE)
                .setFooter(String.format("Created: %s • Updated: %s", note.created(), note.updated()), Images.IMG_LOGO);
    }

    public static MessageEmbed notesGetErrorEmbed() {
        return new MessageEmbed(
                null,
                "Sorry, there was an error getting your notes.",
                "Please try again. If the issue persists, ask in the support server.",
                null,
                null,
                Colors.RED,
                null,
                null,
                AuthorInfo.SAD,
                null,
                null,
                null,
                null
        );
    }

    public static MessageEmbed notesCreateErrorEmbed() {
        return new MessageEmbed(
                null,
                "Sorry, there was an error creating your note.",
                "Please try again. If the issue persists, ask in the support server.",
                null,
                null,
                Colors.RED,
                null,
                null,
                AuthorInfo.SAD,
                null,
                null,
                null,
                null
        );
    }

    public static MessageEmbed notesListNotFoundErrorEmbed() {
        return new EmbedBuilder()
                        .setTitle("No notes found")
                        .setDescription("You don't have any notes yet. Create one with `/note`.")
                        .setColor(Colors.RED)
                        .setFooter("Knaughts", Images.IMG_LOGO)
                        .build();
    }

    public static MessageEmbed noteInvalidIdErrorEmbed() {
        return new EmbedBuilder()
                        .setTitle("Invalid note ID")
                        .setDescription("The note ID you provided is invalid, please try again.")
                        .setColor(Colors.RED)
                        .setFooter("Knaughts", Images.IMG_SAD)
                        .build();
    }

    public static MessageEmbed noteDeleteEmbed() {
        return new EmbedBuilder()
                        .setTitle("Note deleted")
                        .setDescription("Your note has been deleted.")
                        .setColor(Colors.ORANGE)
                        .setFooter("Knaughts", Images.IMG_LOGO)
                        .build();
    }

    public static MessageEmbed noteDeleteErrorEmbed() {
        return new EmbedBuilder()
                .setTitle("Note failed to delete")
                .setDescription("Sorry, something went wrong. Please try to delete your note again.")
                .setColor(Colors.RED)
                .setFooter("Knaughts", Images.IMG_LOGO)
                .build();
    }

    public static MessageEmbed noteEditErrorEmbed() {
        return new EmbedBuilder()
                .setTitle("Note failed to edit")
                .setDescription("Sorry, something went wrong. Please try to edit your note again.")
                .setColor(Colors.RED)
                .setFooter("Knaughts", Images.IMG_LOGO)
                .build();
    }

    public static MessageEmbed timeoutEmbed() {
        return new EmbedBuilder()
                .setColor(Colors.ORANGE)
                .setTitle("⏰ Timeout")
                .setDescription("Request timed out, please execute the `/notes` command again to access your notes.")
                .build();
    }

    /**
     * Represents a pair of an EmbedBuilder and a row of buttons.
     * @param embedBuilder the EmbedBuilder
     * @param row the list of buttons
     */
    public record EmbedBuilderRowPair(EmbedBuilder embedBuilder, List<ItemComponent> row) {}
}
