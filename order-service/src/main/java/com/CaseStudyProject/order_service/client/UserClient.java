package com.CaseStudyProject.order_service.client;

import com.CaseStudyProject.order_service.config.FeignConfig;
import com.CaseStudyProject.order_service.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for declarative REST communication with the USER-SERVICE.
 * Used by the Order Service to retrieve customer information.
 */
@FeignClient(name = "USER-SERVICE", configuration = FeignConfig.class)
public interface UserClient {

    /**
     * Calls the User Service endpoint to fetch user details by their unique ID.
     * @param id The ID of the user to be retrieved.
     * @return A UserDTO containing the user's profile information.
     */
    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

}