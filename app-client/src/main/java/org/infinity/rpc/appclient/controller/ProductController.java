package org.infinity.rpc.appclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.Product;
import org.infinity.app.common.ProductService;
import org.infinity.app.common.UserService;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ProductController implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    @Consumer
    private ProductService     productService;
    @Consumer
    private UserService        userService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/api/product/product")
    public Product getProduct() {
        applicationContext.getBean(Product.class);
        int count = userService.count();
        log.debug("User count: {}", count);
        Product product = productService.get(1L);
        return product;
    }


}
