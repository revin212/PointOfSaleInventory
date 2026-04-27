package com.smartpos.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpos.backend.domain.enums.Role;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final String OWNER_EMAIL    = "owner.test@smartpos.local";
    public static final String CASHIER_EMAIL  = "cashier.test@smartpos.local";
    public static final String WAREHOUSE_EMAIL = "warehouse.test@smartpos.local";
    public static final String DEFAULT_PASSWORD = "TestPass123!";

    protected MockMvc mockMvc;

    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected WebApplicationContext context;
    @Autowired protected JdbcTemplate jdbcTemplate;

    private static final String[] TABLES_IN_DELETE_ORDER = {
            "audit_logs",
            "stock_movements",
            "product_stocks",
            "sale_return_items",
            "sale_returns",
            "sale_items",
            "sales",
            "purchase_receipt_items",
            "purchase_receipts",
            "purchase_items",
            "purchases",
            "refresh_tokens",
            "cash_movements",
            "shifts",
            "customers",
            "products",
            "categories",
            "suppliers",
            "users"
    };

    @BeforeEach
    void baseSetup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        cleanDatabase();
        ensureUser("Test Owner",     OWNER_EMAIL,    Role.OWNER);
        ensureUser("Test Cashier",   CASHIER_EMAIL,  Role.CASHIER);
        ensureUser("Test Warehouse", WAREHOUSE_EMAIL, Role.WAREHOUSE);
    }

    protected void cleanDatabase() {
        for (String table : TABLES_IN_DELETE_ORDER) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    protected UserEntity ensureUser(String name, String email, Role role) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }
}
