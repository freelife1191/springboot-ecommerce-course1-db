package com.onion.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne
    // 외래키를 생성하지 않는다
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User author;

    @ManyToOne
    // 게시글 조회시 게시판 정보는 무시
    @JsonIgnore
    // 외래키를 생성하지 않는다
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Board board;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    // @CreatedDate 어노테이션을 사용하여 엔티티가 저장될 때마다 생성 시간을 자동으로 설정할 수 있다
    @CreatedDate
    @Column(insertable = true, updatable = false, nullable = false)
    @Comment(value = "등록일시")
    private LocalDateTime createdDate;

    // @LastModifiedDate 어노테이션을 사용하여 엔티티가 업데이트될 때마다 업데이트 시간을 자동으로 설정할 수 있다
    @LastModifiedDate
    @Column(nullable = false)
    @Comment(value = "수정일시")
    private LocalDateTime updatedDate;

    // @PrePersist 어노테이션을 사용하여 엔티티가 저장될 때마다 생성 시간을 자동으로 설정할 수 있다
    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // @PreUpdate 어노테이션을 사용하여 엔티티가 업데이트될 때마다 업데이트 시간을 자동으로 설정할 수 있다
    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}