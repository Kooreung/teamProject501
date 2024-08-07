package com.backend.domain.post;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class Post {
    private Integer postId;
    private String title;
    private String content;
    private String nickName;
    private Integer view;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createDate;
    private Integer memberId;
    private Integer likeCount;
    private Integer commentCount;

    private String mdpick;
    private String picurl;
    private String banner;
    private String profileName;
    private String processYn;
    private String reportReason;
}