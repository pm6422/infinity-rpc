package org.infinity.rpc.appclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.Product;
import org.infinity.app.common.ProductService;
import org.infinity.app.common.UserService;
import org.infinity.rpc.client.annotation.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AppClientController {

    @Consumer
    @Autowired
    private ProductService productService;
    @Consumer
    @Autowired
    private UserService    userService;

    @GetMapping("/api/app-client/product")
    public Product getProduct() {
        int count = userService.count();
        log.debug("User count: {}", count);
        Product product = productService.get(1L);
        return product;
    }
}
