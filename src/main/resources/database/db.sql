DROP TABLE crawler_db.retail_shop;
DROP TABLE IF EXISTS crawler_db.products;
DROP DATABASE IF EXISTS crawler_db;

CREATE DATABASE crawler_db;

CREATE TABLE crawler_db.products (
     id bigint NOT NULL AUTO_INCREMENT,
     external_id varchar(255) NOT NULL,
     rating double,
     title varchar(255) NOT NULL,
     `description` varchar(1024) NOT NULL,
     created_at timestamp NOT NULL,
     updated_at timestamp NOT NULL,
     PRIMARY KEY (id),
     UNIQUE(external_id)
);

CREATE TABLE crawler_db.retail_shop (
    id bigint NOT NULL AUTO_INCREMENT,
    product_id bigint NOT NULL,
    external_id varchar(255) NOT NULL,
    price double NOT NULL,
    `name` varchar(255) NOT NULL,
    rating_shop double,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY `retail_shop.product_id_external_id_unique` (`product_id`,`external_id`)
);