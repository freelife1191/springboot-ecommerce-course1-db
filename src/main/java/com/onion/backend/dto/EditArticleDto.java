package com.onion.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Optional;

@Schema(description = "게시글 수정 요청")
@Getter
public class EditArticleDto {
    @Schema(description = "게시글 제목", example = "수정 제목")
    Optional<String> title;
    @Schema(description = "게시글 내용", example = "수정 내용")
    Optional<String> content;
}
