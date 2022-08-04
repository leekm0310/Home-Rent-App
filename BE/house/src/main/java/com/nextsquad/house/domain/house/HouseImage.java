package com.nextsquad.house.domain.house;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class HouseImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "house_image_id")
    private Long id;
    private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rent_article_id")
    private RentArticle rentArticle;
    private int orderInList;

    public HouseImage(String storeFileUrl, int orderInList) {
        this.imageUrl = storeFileUrl;
        this.orderInList = orderInList;
    }
}
