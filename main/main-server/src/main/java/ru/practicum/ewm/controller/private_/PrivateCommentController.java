package ru.practicum.ewm.controller.private_;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    public CommentDto createComment(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @RequestBody @Valid NewCommentDto dto) {
        return commentService.createComment(userId, eventId, dto);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody @Valid UpdateCommentDto dto) {
        return commentService.updateComment(userId, commentId, dto);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
    }

    @GetMapping("/comments")
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "0") int from,
                                            @RequestParam(defaultValue = "10") int size) {
        int safeSize = size <= 0 ? 10 : size;
        return commentService.getUserComments(userId, from, safeSize);
    }
}

