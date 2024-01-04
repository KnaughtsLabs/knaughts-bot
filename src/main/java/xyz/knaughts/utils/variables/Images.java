package xyz.knaughts.utils.variables;

/**
 * A utility class for getting URLs to the Knaughts logo images.
 * You should replace the environment variables with links to your own images.
 */
public final class Images {
    public static final String IMG_LOGO = System.getenv("KNAUGHTS_IMG_LOGO");
    public static final String IMG_QUESTION = System.getenv("KNAUGHTS_IMG_QUESTION");
    public static final String IMG_SAD = System.getenv("KNAUGHTS_IMG_SAD");

    private Images() {}
}
