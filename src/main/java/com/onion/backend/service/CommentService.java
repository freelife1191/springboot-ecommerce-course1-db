package com.onion.backend.service;

import com.onion.backend.dto.WriteCommentDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.Board;
import com.onion.backend.entity.Comment;
import com.onion.backend.entity.User;
import com.onion.backend.exception.ForbiddenException;
import com.onion.backend.exception.RateLimitException;
import com.onion.backend.exception.ResourceNotFoundException;
import com.onion.backend.repository.ArticleRepository;
import com.onion.backend.repository.BoardRepository;
import com.onion.backend.repository.CommentRepository;
import com.onion.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final BoardRepository boardRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Comment writeComment(Long boardId, Long articleId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanWriteComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Comment comment = new Comment();
        comment.setArticle(article.get());
        comment.setAuthor(author.get());
        comment.setContent(dto.getContent());
        commentRepository.save(comment);
        return comment;
    }

    @Transactional
    public Comment editComment(Long boardId, Long articleId, Long commentId, WriteCommentDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty() || comment.get().getIsDeleted()) {
            throw new ResourceNotFoundException("comment not found");
        }
        if (comment.get().getAuthor() != author.get()) {
            throw new ForbiddenException("comment author different");
        }
        if (dto.getContent() != null) {
            comment.get().setContent(dto.getContent());
        }
        commentRepository.save(comment.get());
        return comment.get();
    }

    public boolean deleteComment(Long boardId, Long articleId, Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (!this.isCanEditComment()) {
            throw new RateLimitException("comment not written by rate limit");
        }
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardRepository.findById(boardId);
        Optional<Article> article = articleRepository.findById(articleId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getIsDeleted()) {
            throw new ForbiddenException("article is deleted");
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty() || comment.get().getIsDeleted()) {
            throw new ResourceNotFoundException("comment not found");
        }
        if (comment.get().getAuthor() != author.get()) {
            throw new ForbiddenException("comment author different");
        }
        comment.get().setIsDeleted(true);
        commentRepository.save(comment.get());
        return true;
    }

    private long count() {
        return commentRepository.count();
    }

    private boolean isCanWriteComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (count() < 2) return true;
        return this.isDifferenceMoreThanOneSeconds(latestComment.getCreatedDate());
    }

    private boolean isCanEditComment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Comment latestComment = commentRepository.findLatestCommentOrderByCreatedDate(userDetails.getUsername());
        if (count() < 2) return true;
        return this.isDifferenceMoreThanOneSeconds(latestComment.getUpdatedDate());
    }

    private boolean isDifferenceMoreThanOneSeconds(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);

        return Math.abs(duration.toSeconds()) > 1;
    }

    /**
     * 게시글을 가져오는 비동기 메서드
     * @param boardId
     * @param articleId
     * @return
     */
    @Async // 비동기 처리를 위한 어노테이션
    protected CompletableFuture<Article> getArticle(Long boardId, Long articleId) {
        Optional<Board> board = boardRepository.findById(boardId);
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isEmpty() || article.get().getIsDeleted()) {
            throw new ResourceNotFoundException("article not found");
        }
        return CompletableFuture.completedFuture(article.get());
    }

    /**
     * 게시글의 댓글을 가져오는 비동기 메서드
     * @param articleId
     * @return
     */
    @Async // 비동기 처리를 위한 어노테이션
    protected CompletableFuture<List<Comment>> getComments(Long articleId) {
        return CompletableFuture.completedFuture(commentRepository.findByArticleId(articleId));
    }

    /**
     * 비동기로 게시글과 댓글을 가져와서 조합하는 메서드
     * @param boardId
     * @param articleId
     * @return
     */
    public CompletableFuture<Article> getArticleWithComment(Long boardId, Long articleId) {
        CompletableFuture<Article> articleFuture = this.getArticle(boardId, articleId);
        CompletableFuture<List<Comment>> commentsFuture = this.getComments(articleId);

        return CompletableFuture.allOf(articleFuture, commentsFuture)
                .thenApply(voidResult -> {
                    try {
                        Article article = articleFuture.get();
                        List<Comment> comments = commentsFuture.get();
                        article.setComments(comments);
                        return article;
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }
}
