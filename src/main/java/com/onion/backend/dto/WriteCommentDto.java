package com.onion.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "댓글 작성 요청")
@Getter
public class WriteCommentDto {
    @Schema(description = "댓글 내용", example = "테스트 댓글")
    String content;
}
