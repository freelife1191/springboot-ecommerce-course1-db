package com.onion.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "게시글 작성 요청")
@Getter
public class WriteArticleDto {
    @Schema(description = "게시판 ID", example = "1")
    Long boardId;
    @Schema(description = "게시글 제목", example = "테스트 제목")
    String title;
    @Schema(description = "게시글 내용", example = "테스트 내용")
    String content;
}
