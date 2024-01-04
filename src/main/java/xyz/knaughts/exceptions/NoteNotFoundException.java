package xyz.knaughts.exceptions;

public class NoteNotFoundException extends Throwable {
    public NoteNotFoundException(String message) {
        super(message);
    }
}
