package com.onion.backend.service;

import com.onion.backend.entity.Board;
import com.onion.backend.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    public Board generateBoard() {
        Board board = new Board();
        board.setTitle("자유게시판");
        board.setDescription("자유롭게 글을 쓸 수 있는 게시판입니다");
        boardRepository.save(board);
        return board;
    }

    public Optional<Board> findById(Long boardId) {
        return boardRepository.findById(boardId);
    }
}
