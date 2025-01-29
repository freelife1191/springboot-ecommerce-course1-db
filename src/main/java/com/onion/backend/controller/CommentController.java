package com.onion.backend.controller;

import com.onion.backend.dto.WriteCommentDto;
import com.onion.backend.entity.Comment;
import com.onion.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{boardId}/articles/{articleId}/comments")
    public ResponseEntity<Comment> writeComment(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId") Long boardId,
            @PathVariable("articleId") Long articleId,
            @RequestBody WriteCommentDto writeCommentDto) {
        return ResponseEntity.ok(commentService.writeComment(boardId, articleId, writeCommentDto));
    }

    @PutMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<Comment> writeComment(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId") Long boardId,
            @PathVariable("articleId") Long articleId,
            @PathVariable("commentId") Long commentId,
            @RequestBody WriteCommentDto editCommentDto) {
        return ResponseEntity.ok(commentService.editComment(boardId, articleId, commentId, editCommentDto));
    }

    @DeleteMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<String> writeComment(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable Long boardId,
            @PathVariable Long articleId,
            @PathVariable Long commentId) {
        commentService.deleteComment(boardId, articleId, commentId);
        return ResponseEntity.ok("comment is deleted");
    }
}
