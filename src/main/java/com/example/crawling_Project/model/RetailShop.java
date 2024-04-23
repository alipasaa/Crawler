package com.example.crawling_Project.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "retail_shop")
public class RetailShop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    private Double price;
    private String name;
    private Double rating_shop;
    private Date createdAt;
    private Date updatedAt;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;

    public void setProduct(Product product) { this.product = product; }

    public void setExternalId(String externalId) { this.externalId = externalId; }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRating_shop(Double rating_shop) {
        this.rating_shop = rating_shop;
    }

    public void setCreatedAt(Date created_at) {
        this.createdAt = created_at;
    }

    public void setUpdatedAt(Date updated_at) {
        this.updatedAt = updated_at;
    }

    @Override
    public String toString() {
        return "RetailShop{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", price=" + price +
                ", name='" + name + '\'' +
                ", rating_shop=" + rating_shop +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", product=" + product +
                '}';
    }
}
