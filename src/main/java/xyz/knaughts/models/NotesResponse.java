package xyz.knaughts.models;

import java.util.List;

public record NotesResponse(List<Note> notes, int currentPage, int totalPages) {
}
