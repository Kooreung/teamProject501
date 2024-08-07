package com.backend.mapper.commentreply;

import com.backend.domain.reply.Reply;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentReplyMapper {
    @Insert("""
                        INSERT INTO commentreply(replycomment,postid,memberid,commentid)
            VALUES (#{replyComment},#{postId},#{memberId},#{commentId})
                        """)
    void insertReply(Reply reply);

    @Select("""
              SELECT cr.replyId,cr.commentid,cr.replycomment,m.memberid,m.nickName,cr.createdate
                        FROM commentreply cr JOIN member m ON cr.memberid = m.memberid
                        WHERE commentid =#{commentId}
            """)
    List<Reply> selectReplyCommentList(Integer commentId);

    @Update("""
            UPDATE commentreply
            SET replyComment=#{replyComment} WHERE replyId = #{replyId}
            """)
    void modifyReply(Reply reply);

    @Delete("""
            DELETE FROM commentreply
            WHERE replyId = #{replyId}
            """)
    void deleteByReply(Reply reply);
}
