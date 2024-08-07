package com.backend.controller.comment;

import com.backend.domain.comment.Comment;
import com.backend.service.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {
    final CommentService service;

    @PostMapping("add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity getComment(@RequestBody Comment comment, Authentication authentication) {
        if (service.validate(comment)) {
            service.saveComment(comment, authentication);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("list/{postId}")
    public List<Comment> getCommentList(@PathVariable Integer postId) {
        return service.commentList(postId);
    }

    @PutMapping("edit")
    @PreAuthorize("isAuthenticated()")
    public void getCommentEdit(@RequestBody Comment comment, Authentication authentication) {
        if (service.hasMemberIdAccess(comment, authentication)) {
            service.commentEdit(comment);
        } else {
            throw new AccessDeniedException("실패");
        }
    }

    @DeleteMapping("delete")
    @PreAuthorize("isAuthenticated()")
    public void deleteComment(@RequestBody Comment comment, Authentication authentication) {
        service.commentDelete(comment, authentication);
    }
}
