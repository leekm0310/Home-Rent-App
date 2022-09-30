package com.nextsquad.house.service;

import com.nextsquad.house.domain.house.*;
import com.nextsquad.house.domain.user.User;
import com.nextsquad.house.dto.*;
import com.nextsquad.house.dto.bookmark.BookmarkRequestDto;
import com.nextsquad.house.dto.rentarticle.*;
import com.nextsquad.house.exception.*;
import com.nextsquad.house.login.jwt.JwtProvider;
import com.nextsquad.house.repository.rentarticle.HouseFacilityRepository;
import com.nextsquad.house.repository.rentarticle.HouseImageRepository;
import com.nextsquad.house.repository.rentarticle.RentArticleBookmarkRepository;
import com.nextsquad.house.repository.rentarticle.RentArticleRepository;
import com.nextsquad.house.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RentArticleService {

    private final RentArticleRepository rentArticleRepository;
    private final UserRepository userRepository;
    private final HouseImageRepository houseImageRepository;
    private final RentArticleBookmarkRepository rentArticleBookmarkRepository;
    private final HouseFacilityRepository houseFacilityRepository;
    private final JwtProvider jwtProvider;

    public RentArticleCreationResponse writeRentArticle(RentArticleRequest request, String token){
        User user = getUserFromAccessToken(token);

        List<String> houseImageUrls = request.getHouseImages();
        HouseFacility houseFacility = request.extractHouseFacility();
        houseFacilityRepository.save(houseFacility);

        RentArticle rentArticle = generateRentArticle(request, user, houseFacility);
        rentArticleRepository.save(rentArticle);

        saveHouseImage(rentArticle, houseImageUrls);

        return new RentArticleCreationResponse(rentArticle.getId());
    }

    public RentArticleListResponse getRentArticles(SearchConditionDto searchCondition, Pageable pageable, String token) {

        User user = getUserFromAccessToken(token);

        List<RentArticleBookmark> listByUser = rentArticleBookmarkRepository.findListByUser(user);
        Map<Long, Boolean> bookmarkHashMap = getBookmarkedArticleMap(listByUser);

        List<RentArticle> rentArticles = rentArticleRepository.findByKeyword(searchCondition, pageable);
        boolean hasNext = checkHasNext(pageable, rentArticles);

        List<RentArticleListElement> responseElements = rentArticles.stream()
                .map(RentArticleListElement::from)
                .peek(element -> {element.setBookmarked(bookmarkHashMap.get(element.getId()) != null);})
                .collect(Collectors.toList());

        return new RentArticleListResponse(responseElements, hasNext);
    }

    public RentArticleResponse generateRentArticle(Long id, String token){
        RentArticle rentArticle = rentArticleRepository.findById(id).orElseThrow(() -> new ArticleNotFoundException());
        if (rentArticle.isDeleted() || rentArticle.isCompleted()) {
            throw new IllegalArgumentException("삭제되었거나 거래가 완료된 글입니다.");
        }
        rentArticle.addViewCount();

        User user = getUserFromAccessToken(token);

        boolean isBookmarked = rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle).isPresent();

        return new RentArticleResponse(rentArticle, isBookmarked);
    }

    public GeneralResponseDto toggleIsCompleted(Long id, String accessToken) {
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException());

        authorizeArticleOwner(accessToken, rentArticle);

        rentArticle.toggleIsCompleted();
        return new GeneralResponseDto(200, "게시글 상태가 변경되었습니다.");
    }

    public GeneralResponseDto deleteArticle(Long id, String accessToken) {
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException());

        authorizeArticleOwner(accessToken, rentArticle);

        rentArticle.markAsDeleted();
        rentArticleBookmarkRepository.deleteByRentArticle(rentArticle);
        return new GeneralResponseDto(200, "게시글이 삭제되었습니다.");
    }

    public GeneralResponseDto addBookmark(BookmarkRequestDto bookmarkRequestDto, String token) {
        User user = getUserFromAccessToken(token);
        RentArticle rentArticle = rentArticleRepository.findById(bookmarkRequestDto.getArticleId()).orElseThrow(() -> new ArticleNotFoundException());

        if (rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle).isPresent()) {
            throw new DuplicateBookmarkException();
        }

        if (rentArticle.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글은 추가할 수 없습니다.");
        }
        if (rentArticle.isCompleted()) {
            throw new IllegalArgumentException("삭제된 게시글은 추가할 수 없습니다.");
        }
        rentArticleBookmarkRepository.save(new RentArticleBookmark(rentArticle, user));
        return new GeneralResponseDto(200, "북마크에 추가 되었습니다.");
    }

    public GeneralResponseDto deleteBookmark(BookmarkRequestDto bookmarkRequestDto, String token) {
        User user = getUserFromAccessToken(token);

        RentArticle rentArticle = rentArticleRepository.findById(bookmarkRequestDto.getArticleId())
                .orElseThrow(() -> new ArticleNotFoundException());
        RentArticleBookmark bookmark = rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle)
                .orElseThrow(() -> new BookmarkNotFoundException());
        rentArticleBookmarkRepository.delete(bookmark);
        return new GeneralResponseDto(200, "북마크가 삭제되었습니다.");
    }

    public GeneralResponseDto modifyRentArticle(Long id, RentArticleRequest request, String accessToken) {
        log.info("updating {}... ", request.getTitle());
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException());

        authorizeArticleOwner(accessToken, rentArticle);

        rentArticle.getHouseFacility().updateHouseFacility(request.extractHouseFacility());

        houseImageRepository.deleteAllByArticle(rentArticle);
        saveHouseImage(rentArticle, request.getHouseImages());

        rentArticle.modifyArticle(request);

        return new GeneralResponseDto(200, "게시글이 수정되었습니다.");
    }

    private boolean checkHasNext(Pageable pageable, List<RentArticle> rentArticles) {
        boolean checkHasNext = pageable.getPageSize() < rentArticles.size();
        if (checkHasNext) {
            rentArticles.remove(rentArticles.size() - 1);
        }
        return checkHasNext;
    }

    private Map<Long, Boolean> getBookmarkedArticleMap(List<RentArticleBookmark> listByUser) {
        Map<Long, Boolean> bookmarkHashMap = new HashMap<Long, Boolean>();
        for (RentArticleBookmark rentArticleBookmark : listByUser) {
            bookmarkHashMap.put(rentArticleBookmark.getRentArticle().getId(), true);
        }
        return bookmarkHashMap;
    }

    private User getUserFromAccessToken(String token) {
        Long id = jwtProvider.decode(token).getClaim("id").asLong();
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException());
        return user;
    }

    private void saveHouseImage(RentArticle rentArticle, List<String> houseImageUrls) {
        for (int i = 0; i < houseImageUrls.size(); i++) {
            houseImageRepository.save(new HouseImage(houseImageUrls.get(i), rentArticle, i));
        }
    }

    private void authorizeArticleOwner(String accessToken, RentArticle article) {
        Long loggedInId = jwtProvider.decode(accessToken).getClaim("id").asLong();
        User user = userRepository.findById(loggedInId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.equals(article.getUser())) {
            throw new AccessDeniedException();
        }
    }

    private RentArticle generateRentArticle(RentArticleRequest request, User user, HouseFacility houseFacility) {
        RentArticle rentArticle = RentArticle.builder()
                .user(user)
                .title(request.getTitle())
                .houseType(HouseType.valueOf(request.getHouseType()))
                .rentFee(request.getRentFee())
                .deposit(request.getDeposit())
                .availableFrom(request.getAvailableFrom())
                .contractExpiresAt(request.getContractExpiresAt())
                .maintenanceFee(request.getMaintenanceFee())
                .maintenanceFeeDescription(request.getMaintenanceFeeDescription())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .addressDescription(request.getAddressDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .houseFacility(houseFacility)
                .content(request.getContent())
                .contractType(ContractType.valueOf(request.getContractType()))
                .maxFloor(request.getMaxFloor())
                .thisFloor(request.getThisFloor())
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
        return rentArticle;
    }
}
