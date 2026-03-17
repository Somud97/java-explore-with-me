package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment not published event");
        }

        Comment comment = CommentMapper.toEntity(dto, author, event);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        Comment comment = getOwnedComment(userId, commentId);
        if (comment.getStatus() == CommentStatus.DELETED || comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Cannot update deleted comment");
        }
        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getOwnedComment(userId, commentId);
        if (comment.getStatus() == CommentStatus.DELETED || comment.getStatus() == CommentStatus.REJECTED) {
            return;
        }
        comment.setStatus(CommentStatus.DELETED);
        comment.setUpdatedOn(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository
                .findAllByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, page)
                .map(CommentMapper::toDto)
                .getContent();
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository
                .findAllByAuthorIdAndStatusNot(userId, CommentStatus.DELETED, page)
                .map(CommentMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional
    public void rejectComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        if (comment.getStatus() == CommentStatus.REJECTED) {
            return;
        }
        comment.setStatus(CommentStatus.REJECTED);
        comment.setUpdatedOn(LocalDateTime.now());
        commentRepository.save(comment);
    }

    private Comment getOwnedComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("User " + userId + " is not owner of comment " + commentId);
        }
        return comment;
    }
}

