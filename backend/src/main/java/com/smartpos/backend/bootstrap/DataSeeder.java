package com.smartpos.backend.bootstrap;

import com.smartpos.backend.config.AppProperties;
import com.smartpos.backend.domain.enums.Role;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppProperties props, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().enabled()) {
            return;
        }
        if (userRepository.count() > 0) {
            return;
        }
        String password = props.seed().defaultPassword();
        log.info("Seeding default users (Owner/Cashier/Warehouse)");
        seed("Store Owner",  "owner@smartpos.local",     Role.OWNER,     password);
        seed("Cashier User", "cashier@smartpos.local",   Role.CASHIER,   password);
        seed("Warehouse",    "warehouse@smartpos.local", Role.WAREHOUSE, password);
    }

    private void seed(String name, String email, Role role, String password) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        u.setActive(true);
        userRepository.save(u);
    }
}
