package com.onion.backend.service;

import com.onion.backend.dto.EditArticleDto;
import com.onion.backend.dto.WriteArticleDto;
import com.onion.backend.entity.Article;
import com.onion.backend.entity.Board;
import com.onion.backend.entity.User;
import com.onion.backend.exception.ForbiddenException;
import com.onion.backend.exception.RateLimitException;
import com.onion.backend.exception.ResourceNotFoundException;
import com.onion.backend.repository.ArticleRepository;
import com.onion.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final BoardService boardService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Transactional
    public Article writeArticle(Long boardId, WriteArticleDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Board board = boardService.findById(boardId).orElse(null);
        Article firstArticle = findById(1L).orElse(null);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board == null) {
            board = boardService.generateBoard();
            // throw new ResourceNotFoundException("board not found");
        }
        if (firstArticle == null) {
            generateArticle(
                    author.orElseThrow(() -> new ResourceNotFoundException("author not found"))
                    , board
            );
            // throw new ResourceNotFoundException("article not found");
        }
        if (!this.isCanWriteArticle()) {
            throw new RateLimitException("article not written by rate limit");
        }
        Article article = new Article();
        article.setBoard(board);
        article.setAuthor(author.get());
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        articleRepository.save(article);
        return article;
    }

    public Article generateArticle(User author, Board board) {
        Article article = new Article();
        article.setTitle("자동 생성 타이틀");
        article.setContent("자동 생성 컨텐츠");
        article.setAuthor(author);
        articleRepository.save(article);
        return article;
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    /**
     * 게시판의 게시글을 최신순으로 10개 가져온다
     * @param boardId
     * @return
     */
    public List<Article> firstGetArticle(Long boardId) {
        return articleRepository.findTop10ByBoardIdOrderByCreatedDateDesc(boardId);
    }

    /**
     * 요청 게시글 이전 게시글 10개를 가져온다
     * @param boardId
     * @param articleId
     * @return
     */
    public List<Article> getOldArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdLessThanOrderByCreatedDateDesc(boardId, articleId);
    }

    /**
     * 요청 게시글 이후 게시글 10개를 가져온다
     * @param boardId
     * @param articleId
     * @return
     */
    public List<Article> getNewArticle(Long boardId, Long articleId) {
        return articleRepository.findTop10ByBoardIdAndArticleIdGreaterThanOrderByCreatedDateDesc(boardId, articleId);
    }

    @Transactional
    public Article editArticle(Long boardId, Long articleId, EditArticleDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardService.findById(boardId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getAuthor() != author.get()) {
            throw new ForbiddenException("article author different");
        }
        if (!this.isCanEditArticle()) {
            throw new RateLimitException("article not edited by rate limit");
        }
        if (dto.getTitle().isPresent()) {
            article.get().setTitle(dto.getTitle().get());
        }
        if (dto.getContent().isPresent()) {
            article.get().setContent(dto.getContent().get());
        }
        articleRepository.save(article.get());
        return article.get();
    }

    @Transactional
    public boolean deleteArticle(Long boardId, Long articleId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> author = userRepository.findByUsername(userDetails.getUsername());
        Optional<Board> board = boardService.findById(boardId);
        if (author.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }
        if (board.isEmpty()) {
            throw new ResourceNotFoundException("board not found");
        }
        Optional<Article> article = articleRepository.findById(articleId);
        if (article.isEmpty()) {
            throw new ResourceNotFoundException("article not found");
        }
        if (article.get().getAuthor() != author.get()) {
            throw new ForbiddenException("article author different");
        }
        if (!this.isCanEditArticle()) {
            throw new RateLimitException("article not edited by rate limit");
        }
        article.get().setIsDeleted(true);
        articleRepository.save(article.get());
        return true;
    }

    private long count() {
        return articleRepository.count();
    }

    private boolean isCanWriteArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByCreatedDate(userDetails.getUsername());
        if (count() < 2) return true;
        return this.isDifferenceMoreThanFiveSeconds(latestArticle.getCreatedDate());
    }

    private boolean isCanEditArticle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Article latestArticle = articleRepository.findLatestArticleByAuthorUsernameOrderByUpdatedDate(userDetails.getUsername());
        if (count() < 2) return true;
        return this.isDifferenceMoreThanFiveSeconds(latestArticle.getUpdatedDate());
    }

    private boolean isDifferenceMoreThanFiveSeconds(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Duration duration = Duration.between(localDateTime, dateAsLocalDateTime);

        return Math.abs(duration.toSeconds()) > 5;
    }
}
