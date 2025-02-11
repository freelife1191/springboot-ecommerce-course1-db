package com.onion.backend.controller;

import com.onion.backend.dto.EditArticleDto;
import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.service.ArticleService;
import com.onion.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;
    private final CommentService commentService;

    @PostMapping("/{boardId}/articles")
    public ResponseEntity<Article> writeArticle(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId") Long boardId,
            @RequestBody WriteArticleDto writeArticleDto) {
        return ResponseEntity.ok(articleService.writeArticle(boardId, writeArticleDto));
    }

    @GetMapping("/{boardId}/articles")
    public ResponseEntity<List<Article>> getArticle(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId") Long boardId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Long firstId) {
        if (lastId != null) {
            // lastId가 있으면 lastId 이전의 게시글을 가져온다
            return ResponseEntity.ok(articleService.getOldArticle(boardId, lastId));
        }
        if (firstId != null) {
            // firstId가 있으면 firstId 이후의 게시글을 가져온다
            return ResponseEntity.ok(articleService.getNewArticle(boardId, firstId));
        }
        // lastId와 firstId가 모두 없으면 최신 게시글 10개를 가져온다
        return ResponseEntity.ok(articleService.firstGetArticle(boardId));
    }

    @PutMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> editArticle(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable("boardId") Long boardId,
            @Parameter(description = "게시글 아이디", example = "1")
            @PathVariable("articleId") Long articleId,
            @RequestBody EditArticleDto editArticleDto) {
        return ResponseEntity.ok(articleService.editArticle(boardId, articleId, editArticleDto));
    }

    @DeleteMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<String> deleteArticle(@PathVariable Long boardId, @PathVariable Long articleId) {
        articleService.deleteArticle(boardId, articleId);
        return ResponseEntity.ok("article is deleted");
    }

    @GetMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> getArticleWithComment(
            @Parameter(description = "게시판 아이디", example = "1")
            @PathVariable Long boardId,
            @PathVariable Long articleId) {
        CompletableFuture<Article> article = commentService.getArticleWithComment(boardId, articleId);
        return ResponseEntity.ok(article.resultNow());
    }
}
