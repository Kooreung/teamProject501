package com.backend.service.post;

import com.backend.domain.place.Place;
import com.backend.domain.post.Banner;
import com.backend.domain.post.Post;
import com.backend.mapper.member.MemberMapper;
import com.backend.mapper.post.PostMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    @Autowired
    private HttpServletRequest request;

    final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    String bucketName;

    @Value("${image.src.prefix}")
    String srcPrefix;
    @Autowired
    private MemberMapper memberMapper;

    // 게시글 추가 | 작성 서비스
    public Integer savePost(Post post, Authentication authentication) {
        post.setMemberId(Integer.valueOf(authentication.getName()));
        postMapper.insertPost(post);
        return post.getPostId();
    }

    // 게시글 작성 시 제목, 내용 공백 확인
    public boolean validate(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            return false;
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            return false;
        }
        return true;
    }

    // 게시글 조회 서비스
    public Map<String, Object> getPostInfo(Integer postId, Authentication authentication) {
        // 조회수 증가
        if (canIncrementView(postId)) {
            postMapper.incrementViewCount(postId);
            HttpSession session = request.getSession();
            session.setAttribute("lastViewTime_" + postId, Instant.now());
        }
        Post post = postMapper.selectByPostId(postId);
        String auth = postMapper.selectAuthByPostId(postId);
        String profileName = memberMapper.getProfileNameByMemberId(post.getMemberId());

        String key = String.format("%s/member/%s/%s", srcPrefix, post.getMemberId(), profileName);

        post.setProfileName(key);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> like = new HashMap<>();

        // 로그인 안하면 빈 하트 표기, 로그인 하면 좋아요 한 게시물 하트 표기
        if (authentication == null) {
            like.put("like", false);
        } else {
            int c = postMapper.selectLikeByPostIdAndMemberId(postId, authentication.getName());
            like.put("like", c == 1);
        }

        // 게시물 조회 시 좋아요 카운트 전송
        like.put("count", postMapper.countLikeByBoardId(postId));
        result.put("like", like);
        result.put("post", post);
        result.put("author", auth);
        // 게시물 조회 시 댓글 수 카운트 전송
        int commentCount = postMapper.selectCountCommentByBoardId(postId);
        result.put("commentCount", commentCount);

        return result;
    }

    //조회수 증가 세션 확인 서비스
    private boolean canIncrementView(Integer postId) {
        HttpSession session = request.getSession();
        Instant lastViewTime = (Instant) session.getAttribute("lastViewTime_" + postId);
        if (lastViewTime != null) {
            // 마지막 조회 시간부터 일정 시간(10분)이 지났는지 확인
            Instant now = Instant.now();
            Instant earliestTimeToIncrement = lastViewTime.plusSeconds(600);
            return now.isAfter(earliestTimeToIncrement);
        } else {
            // 이전에 조회 기록이 없는 경우에는 항상 조회수를 증가시킬 수 있도록 함
            return true;
        }
    }

    // 게시글 목록 서비스 전 위치 값 전송
    public Map<String, Object> getPostListByLocation(Double latitude, Double longitude) {
        Map<String, Object> pageInfo = new HashMap<>();
        List<Post> posts = postMapper.selectAllPost(0, null, null, null, null, latitude, longitude);

        pageInfo.put("postList", posts);
        return pageInfo;
    }

    // 게시글 목록 서비스
    public Map<String, Object> getPostList(Integer page, String listSlider, String searchType, String searchKeyword, String searchReg, Double latitude, Double longitude) {
        Map pageInfo = new HashMap();

        Integer countAllPost = postMapper.countAllpost(searchType, listSlider, searchKeyword, searchReg, latitude, longitude);
        Integer offset = (page - 1) * 5;
        Integer lastPageNumber = (countAllPost - 1) / 5 + 1;
        Integer leftPageNumber = ((page - 1) / 10) * 10 + 1;
        Integer rightPageNumber = leftPageNumber + 9;

        rightPageNumber = Math.min(rightPageNumber, lastPageNumber);
        leftPageNumber = rightPageNumber - 9;
        leftPageNumber = Math.max(leftPageNumber, 1);

        Integer prevPageNumber = leftPageNumber - 1;
        Integer nextPageNumber = rightPageNumber + 1;

        if (prevPageNumber > 0) {
            pageInfo.put("prevPageNumber", prevPageNumber);
        }
        if (nextPageNumber <= lastPageNumber) {
            pageInfo.put("nextPageNumber", nextPageNumber);
        }

        pageInfo.put("currentPageNumber", page);
        pageInfo.put("lastPageNumber", lastPageNumber);
        pageInfo.put("leftPageNumber", leftPageNumber);
        pageInfo.put("rightPageNumber", rightPageNumber);

        List<Post> posts = postMapper.selectAllPost(offset, listSlider, searchType, searchKeyword, searchReg, latitude, longitude);

        for (Post post : posts) {
            Integer memberId = post.getMemberId();
            String profileName = memberMapper.getProfileNameByMemberId(memberId);
            String key = String.format("%s/member/%s/%s", srcPrefix, post.getMemberId(), profileName);
            post.setProfileName(key);
        }

        return Map.of("pageInfo", pageInfo, "postList", posts);
    }

    // 게시글 Top 3 인기글 목록 서비스
    public List<Post> getPostListOfBest() {
        List<Post> posts = postMapper.selectPostOfBest();
        for (Post post : posts) {
            Integer memberId = post.getMemberId();
            String profileName = memberMapper.getProfileNameByMemberId(memberId);
            String key = String.format("%s/member/%s/%s", srcPrefix, post.getMemberId(), profileName);
            post.setProfileName(key);
        }
        return posts;
    }

    // 게시글에서 선택한 장소 목록 서비스
    public List<Place> getPlaceList(Integer postId) {
        return postMapper.selectPlaceList(postId);
    }

    public List<Place> getPlaceListData(String selectPlaces) {
        return postMapper.selectPlaceListData(selectPlaces);
    }

    // 게시글 수정 서비스
    public void postEdit(Post post) {
        postMapper.updatePost(post);
    }

    // 게시글 수정 시 권한 체크 서비스
    public boolean hasMemberIdAccess(Integer postId, Authentication authentication) {
        boolean scopeAdmin = authentication.getAuthorities().stream().map(a -> a.toString()).anyMatch(a -> a.equals("SCOPE_admin"));
        Post post = postMapper.selectByPostId(postId);
        return post.getMemberId().equals(Integer.valueOf(authentication.getName())) || scopeAdmin;
    }

    // 게시글 삭제 서비스
    public void postRemove(Integer postId) {
        postMapper.deleteById(postId);
    }

    //좋아요 카운트 서비스
    public Map<String, Object> postLike(Map<String, Object> like, Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        result.put("like", false);
        Integer postId = (Integer) like.get("postId");
        Integer memberId = Integer.valueOf(authentication.getName());

        int count = postMapper.deleteLike(postId, memberId);
        if (count == 0) {
            postMapper.insertLike(postId, memberId);
        }
        result.put("count", postMapper.countLikeByBoardId(postId));
        return result;
    }

    //좋아요 목록 서비스
    public Map<String, Object> getLikeAllList(Integer memberId, Integer page, String searchType, String searchKeyword) {
        // 페이징 내용
        Map pageInfo = new HashMap();

        Integer countAllPost = postMapper.countAllLikePost(memberId, searchType, searchKeyword);
        Integer offset = (page - 1) * 5;
        Integer lastPageNumber = (countAllPost - 1) / 5 + 1;
        Integer leftPageNumber = (page - 1) / 10 * 10 + 1;
        Integer rightPageNumber = leftPageNumber + 9;

        rightPageNumber = Math.min(rightPageNumber, lastPageNumber);
        leftPageNumber = rightPageNumber - 9;
        leftPageNumber = Math.max(leftPageNumber, 1);

        Integer prevPageNumber = leftPageNumber - 1;
        Integer nextPageNumber = rightPageNumber + 1;

        if (prevPageNumber > 0) {
            pageInfo.put("prevPageNumber", prevPageNumber);
        }
        if (nextPageNumber <= lastPageNumber) {
            pageInfo.put("nextPageNumber", nextPageNumber);
        }

        pageInfo.put("currentPageNumber", page);
        pageInfo.put("lastPageNumber", lastPageNumber);
        pageInfo.put("leftPageNumber", leftPageNumber);
        pageInfo.put("rightPageNumber", rightPageNumber);

        List<Post> posts = postMapper.selectLikeList(memberId, offset, searchType, searchKeyword);

        for (Post post : posts) {
            Integer inPostMemberId = post.getMemberId();
            String profileName = memberMapper.getProfileNameByMemberId(inPostMemberId);
            String key = String.format("%s/member/%s/%s", srcPrefix, post.getMemberId(), profileName);
            post.setProfileName(key);
        }

        return Map.of("pageInfo", pageInfo, "postList", posts);
    }

    //md 게시물 목록 서비스
    public Map<String, Object> getMdList(Map<String, Object> post, String searchType, String searchKeyword) {
        List<Post> posts = postMapper.selectMdPostList(post, searchType, searchKeyword);
        for (Post onePost : posts) {
            Integer inPostMemberId = onePost.getMemberId();
            String profileName = memberMapper.getProfileNameByMemberId(inPostMemberId);
            String key = String.format("%s/member/%s/%s", srcPrefix, onePost.getMemberId(), profileName);
            onePost.setProfileName(key);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("post", posts);
        return result;
    }

    public Map<String, Object> myList(Integer memberId, Integer page) {
        Integer offset = (page - 1) * 5;
        List<Post> post = postMapper.getMyList(memberId, offset);

        List<Post> count = postMapper.getMyListCount(memberId);
        Map<String, Object> result = new HashMap<>();
        result.put("post", post);
        result.put("count", count);
        return result;
    }

    public Map<String, Object> getMdPickList() {
        List<Post> posts = postMapper.selectMdPickPostList();

        for (Post post : posts) {
            String url = String.format("%s/banner/mdPostBanner/%s", srcPrefix, post.getBanner());
            post.setBanner(url);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("post", posts);

        return result;
    }

    // mdPick 추가(업데이트)
    public void mdPickPush(Integer postId, MultipartFile banner) throws IOException {
        postMapper.updateMdPickPush(postId);

        String key = String.format("prj3/banner/mdPostBanner/%s/%s", postId, banner.getOriginalFilename());
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        s3Client.putObject(objectRequest, RequestBody.fromInputStream(banner.getInputStream(), banner.getSize()));
        String bannerName = String.format("%s/%s", postId, banner.getOriginalFilename());
        postMapper.updateBannerByPostId(postId, bannerName);
    }

    // mdPick 삭제(업데이트)
    public void mdPickPop(Integer postId) {

        String key = String.format("prj3/banner/mdPostBanner/%s", postMapper.getMdBannerNameByPostId(postId));
        DeleteObjectRequest objectRequest2 = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(objectRequest2);
        postMapper.mdPickPop(postId);
    }

    public String getMdPick(Integer postId) {
        return postMapper.selectMdPickByPostId(postId);
    }

    // mdPick 한 게시물 개수
    public Integer getMdPickCount() {
        return postMapper.countMdPick();
    }


    public void bannerAdd(String city, String link, MultipartFile file) throws IOException {

        String key = String.format("prj3/banner/localBanner/%s/%s", city, file.getOriginalFilename());
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        s3Client.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String src = String.format("%s/%s", city, file.getOriginalFilename());
        postMapper.insertBanner(city, link, src);
    }


    public List<Banner> getBannerList() {

        List<Banner> bannerList = postMapper.selectBannerList();
        for (Banner banner : bannerList) {
            String src = String.format("%s/banner/localBanner/%s", srcPrefix, banner.getBannerSrc());
            banner.setBannerSrc(src);
        }
        return bannerList;
    }


    public int removeBanner(Integer bannerId) {

        Banner banner = postMapper.selectBannerSrcById(bannerId);

        String key = String.format("prj3/banner/localBanner/%s", banner.getBannerSrc());
        DeleteObjectRequest objectRequest2 = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(objectRequest2);
        return postMapper.deleteBannerById(bannerId);
    }
}