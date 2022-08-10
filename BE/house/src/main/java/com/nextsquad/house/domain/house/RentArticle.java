package com.nextsquad.house.domain.house;

import com.nextsquad.house.domain.user.User;
import com.nextsquad.house.dto.RentArticleRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RentArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "rent_article_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String address;
    private String addressDetail;
    private String addressDescription;
    private double latitude;
    private double longitude;
    private String title;
    private String content;
    @Enumerated(EnumType.STRING)
    private ContractType contractType;
    @Enumerated(EnumType.STRING)
    private HouseType houseType;
    private int deposit;
    private int rentFee;
    private int maintenanceFee;
    private String maintenanceFeeDescription;
    private LocalDate availableFrom;
    private LocalDate contractExpiresAt;
    private ArticleStatus status;
    private int viewCount;
    @DateTimeFormat(pattern = "yyyy-MM-dd:HH:mm:ss")
    private LocalDateTime createdAt;
    @DateTimeFormat(pattern = "yyyy-MM-dd:HH:mm:ss")
    private LocalDateTime modifiedAt;
    private int maxFloor;
    private int thisFloor;
    @OneToMany(mappedBy = "rentArticle", fetch = FetchType.LAZY)
    private List<RentArticleFacility> facilities;
    @OneToMany(mappedBy = "rentArticle", fetch = FetchType.LAZY)
    private List<RentArticleSecurityFacility> securityFacilities;
    private boolean hasParkingLot;
    private boolean hasBalcony;
    private boolean hasElevator;
    private boolean isCompleted;
    private boolean isDeleted;
    @OneToMany(mappedBy = "rentArticle", fetch = FetchType.LAZY)
    private List<HouseImage> houseImages = new ArrayList<>();
    @OneToMany(mappedBy = "rentArticle", fetch = FetchType.LAZY)
    private List<RentArticleBookmark> bookmarks = new ArrayList<>();
    public HouseImage getMainImage() {
        return houseImages.get(0);
    }

    public void toggleIsCompleted() {
        isCompleted = !isCompleted;
    }

    public void markAsDeleted() {
        isDeleted = true;
    }

    public void addViewCount(){
        viewCount++;
    }

    public void modifyArticle(RentArticleRequest request) {
        this.address = request.getAddress();
        this.addressDetail = request.getAddressDetail();
        this.addressDescription = request.getAddressDescription();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
        this.title = request.getTitle();
        this.content = request.getContent();
        this.contractType = ContractType.valueOf(request.getContractType());
        this.houseType = HouseType.valueOf(request.getHouseType());
        this.deposit = request.getDeposit();
        this.rentFee = request.getRentFee();
        this.maintenanceFee = request.getMaintenanceFee();
        this.maintenanceFeeDescription = request.getMaintenanceFeeDescription();
        this.availableFrom = request.getAvailableFrom();
        this.contractExpiresAt = request.getContractExpiresAt();
        this.maxFloor = request.getMaxFloor();
        this.thisFloor = request.getThisFloor();
        this.hasParkingLot = request.isHasParkingLot();
        this.hasBalcony = request.isHasBalcony();
        this.hasElevator = request.isHasElevator();
    }
}
