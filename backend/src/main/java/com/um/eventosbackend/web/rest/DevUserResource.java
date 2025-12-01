package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.service.UserService;
import com.um.eventosbackend.service.dto.AdminUserDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller for development-only user queries.
 * <p>
 * This controller is only available in the "dev" profile and provides endpoints
 * for querying users without requiring ADMIN role, useful for development and testing.
 */
@RestController
@RequestMapping("/api/dev")
@Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
public class DevUserResource {

    private static final Logger LOG = LoggerFactory.getLogger(DevUserResource.class);

    private final UserService userService;
    private final Environment env;

    public DevUserResource(UserService userService, Environment env) {
        this.userService = userService;
        this.env = env;
    }

    /**
     * {@code GET /dev/users} : get all users with details (development only).
     * <p>
     * This endpoint is only available in development profile and does not require ADMIN role.
     * It returns detailed user information including email, activation status, etc.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all users.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsersForDev(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get all users (development endpoint)");
        
        // Ensure we're in dev profile
        if (!env.acceptsProfiles(org.springframework.core.env.Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Page<AdminUserDTO> page = userService.getAllManagedUsers(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * {@code GET /dev/users/{login}} : get user by login (development only).
     * <p>
     * This endpoint is only available in development profile and does not require ADMIN role.
     *
     * @param login the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{login}")
    public ResponseEntity<AdminUserDTO> getUserByLoginForDev(@PathVariable("login") String login) {
        LOG.debug("REST request to get User by login: {} (development endpoint)", login);
        
        // Ensure we're in dev profile
        if (!env.acceptsProfiles(org.springframework.core.env.Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return userService
            .getUserWithAuthoritiesByLogin(login)
            .map(AdminUserDTO::new)
            .map(user -> ResponseEntity.ok().body(user))
            .orElse(ResponseEntity.notFound().build());
    }
}

