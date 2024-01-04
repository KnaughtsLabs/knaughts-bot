package xyz.knaughts.utils.variables;

import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * A utility class for getting author information with given images to be used in embeds.
 */
public class AuthorInfo {
    public static final MessageEmbed.AuthorInfo DEFAULT = new MessageEmbed.AuthorInfo(
            "Knaughts",
            "https://knaughts.xyz",
            Images.IMG_LOGO,
            null
    );

    public static final MessageEmbed.AuthorInfo QUESTION = new MessageEmbed.AuthorInfo(
            "Knaughts",
            "https://knaughts.xyz",
            Images.IMG_QUESTION,
            null
    );

    public static final MessageEmbed.AuthorInfo SAD = new MessageEmbed.AuthorInfo(
            "Knaughts",
            "https://knaughts.xyz",
            Images.IMG_SAD,
            null
    );
}
