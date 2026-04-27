package com.smartpos.backend.bootstrap;

import com.smartpos.backend.config.AppProperties;
import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.categories.CategoryEntity;
import com.smartpos.backend.categories.CategoryRepository;
import com.smartpos.backend.domain.enums.Role;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.products.ProductRepository;
import com.smartpos.backend.sales.SaleEntity;
import com.smartpos.backend.sales.SaleItemEntity;
import com.smartpos.backend.sales.SaleRepository;
import com.smartpos.backend.stock.StockLedgerService;
import com.smartpos.backend.suppliers.SupplierEntity;
import com.smartpos.backend.suppliers.SupplierRepository;
import com.smartpos.backend.paymenttypes.PaymentTypeEntity;
import com.smartpos.backend.paymenttypes.PaymentTypeRepository;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockLedgerService stockLedger;
    private final SaleRepository saleRepository;
    private final AuditService auditService;
    private final PaymentTypeRepository paymentTypeRepository;

    public DataSeeder(AppProperties props,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      CategoryRepository categoryRepository,
                      SupplierRepository supplierRepository,
                      ProductRepository productRepository,
                      StockLedgerService stockLedger,
                      SaleRepository saleRepository,
                      AuditService auditService,
                      PaymentTypeRepository paymentTypeRepository) {
        this.props = props;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockLedger = stockLedger;
        this.saleRepository = saleRepository;
        this.auditService = auditService;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().enabled()) {
            return;
        }
        String password = props.seed().defaultPassword();

        UUID ownerId = ensureUser("Store Owner", "owner@smartpos.local", Role.OWNER, password);
        UUID cashierId = ensureUser("Cashier User", "cashier@smartpos.local", Role.CASHIER, password);
        ensureUser("Warehouse", "warehouse@smartpos.local", Role.WAREHOUSE, password);

        ensurePaymentTypes();

        long productCount = productRepository.count();
        if (productCount < 5) {
            log.info("Seeding demo catalog + initial stock + sample sales (existing products: {})", productCount);
            seedDemoData(ownerId, cashierId);
        }
    }

    private void ensurePaymentTypes() {
        ensurePaymentType(PaymentMethod.CASH, "Cash");
        ensurePaymentType(PaymentMethod.TRANSFER, "Transfer");
        ensurePaymentType(PaymentMethod.EWALLET, "E-Wallet");
    }

    private void ensurePaymentType(PaymentMethod method, String name) {
        paymentTypeRepository.findByMethod(method).orElseGet(() -> {
            PaymentTypeEntity pt = new PaymentTypeEntity();
            pt.setMethod(method);
            pt.setName(name);
            pt.setAdminFee(BigDecimal.ZERO);
            pt.setActive(true);
            return paymentTypeRepository.save(pt);
        });
    }

    private UUID ensureUser(String name, String email, Role role, String password) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    if (props.seed().forceResetPasswords()) {
                        log.warn("Resetting seeded user password for {}", email);
                        existing.setPasswordHash(passwordEncoder.encode(password));
                        existing.setActive(true);
                        return userRepository.save(existing).getId();
                    }
                    return existing.getId();
                })
                .orElseGet(() -> {
                    log.info("Seeding user {}", email);
                    UserEntity u = new UserEntity();
                    u.setName(name);
                    u.setEmail(email);
                    u.setPasswordHash(passwordEncoder.encode(password));
                    u.setRole(role);
                    u.setActive(true);
                    return userRepository.save(u).getId();
                });
    }

    private void seedDemoData(UUID ownerId, UUID cashierId) {
        if (productRepository.existsBySkuIgnoreCase("SKU-1001")) {
            return;
        }

        CategoryEntity beans = new CategoryEntity();
        beans.setName("Coffee Beans");
        CategoryEntity tools = new CategoryEntity();
        tools.setName("Brewing Tools");
        categoryRepository.saveAll(List.of(beans, tools));

        SupplierEntity supplierA = new SupplierEntity();
        supplierA.setName("PT Kopi Nusantara");
        supplierA.setPhone("+62 21 555 0001");
        supplierA.setAddress("Jakarta, Indonesia");
        SupplierEntity supplierB = new SupplierEntity();
        supplierB.setName("CV Brew Lab");
        supplierB.setPhone("+62 22 555 0002");
        supplierB.setAddress("Bandung, Indonesia");
        supplierRepository.saveAll(List.of(supplierA, supplierB));

        ProductEntity p1 = product("SKU-1001", "Arabica Gayo 250g", beans.getId(), supplierA.getId(), "pack",
                bd(65000), bd(95000), "899000100001", 10, true);
        ProductEntity p2 = product("SKU-1002", "Robusta Blend 500g", beans.getId(), supplierA.getId(), "pack",
                bd(80000), bd(120000), "899000100002", 8, true);
        ProductEntity p3 = product("SKU-1003", "Manual Brew Kettle", tools.getId(), supplierB.getId(), "pcs",
                bd(240000), bd(350000), "899000200003", 2, true);
        ProductEntity p4 = product("SKU-1004", "Paper Filter V60", tools.getId(), supplierB.getId(), "box",
                bd(25000), bd(45000), "899000200004", 15, true);
        ProductEntity p5 = product("SKU-1005", "Cold Brew Bottle", tools.getId(), supplierB.getId(), "pcs",
                bd(52000), bd(78000), "899000200005", 5, true);
        productRepository.saveAll(List.of(p1, p2, p3, p4, p5));

        // Initial stock (via ledger)
        recordInitialStock(ownerId, p1.getId(), 25);
        recordInitialStock(ownerId, p2.getId(), 15);
        recordInitialStock(ownerId, p3.getId(), 6);
        recordInitialStock(ownerId, p4.getId(), 42);
        recordInitialStock(ownerId, p5.getId(), 18);

        // Sample sales so dashboard/reports are not empty
        seedSale(cashierId, List.of(
                saleLine(p1.getId(), 2, bd(95000), bd(0)),
                saleLine(p4.getId(), 1, bd(45000), bd(0))
        ), bd(10000), PaymentMethod.CASH);

        seedSale(cashierId, List.of(
                saleLine(p3.getId(), 1, bd(350000), bd(0))
        ), bd(0), PaymentMethod.TRANSFER);

        seedSale(ownerId, List.of(
                saleLine(p5.getId(), 3, bd(78000), bd(10000))
        ), bd(0), PaymentMethod.EWALLET);
    }

    private ProductEntity product(String sku, String name, UUID categoryId, UUID supplierId, String unit,
                                  BigDecimal cost, BigDecimal price, String barcode,
                                  int lowStockThreshold, boolean active) {
        ProductEntity p = new ProductEntity();
        p.setSku(sku);
        p.setName(name);
        p.setCategoryId(categoryId);
        p.setSupplierId(supplierId);
        p.setUnit(unit);
        p.setCost(cost);
        p.setPrice(price);
        p.setBarcode(barcode);
        p.setLowStockThreshold(lowStockThreshold);
        p.setActive(active);
        return p;
    }

    private void recordInitialStock(UUID actorId, UUID productId, int qty) {
        stockLedger.record(
                StockMovementType.ADJUSTMENT,
                productId,
                qty,
                null,
                "SEED",
                null,
                "Initial stock (seed)",
                actorId
        );
        auditService.record(
                AuditAction.STOCK_ADJUSTMENT,
                AuditEntityType.STOCK,
                productId,
                actorId,
                null
        );
    }

    private record SeedSaleLine(UUID productId, int qty, BigDecimal unitPrice, BigDecimal lineDiscount) {}

    private SeedSaleLine saleLine(UUID productId, int qty, BigDecimal unitPrice, BigDecimal lineDiscount) {
        return new SeedSaleLine(productId, qty, unitPrice, lineDiscount);
    }

    private void seedSale(UUID actorId, List<SeedSaleLine> lines, BigDecimal orderDiscount, PaymentMethod method) {
        BigDecimal subtotal = BigDecimal.ZERO;
        SaleEntity sale = new SaleEntity();

        for (SeedSaleLine l : lines) {
            BigDecimal gross = l.unitPrice.multiply(BigDecimal.valueOf(l.qty));
            BigDecimal lineTotal = gross.subtract(l.lineDiscount).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            SaleItemEntity item = new SaleItemEntity();
            item.setProductId(l.productId);
            item.setQty(l.qty);
            item.setUnitPrice(l.unitPrice);
            item.setLineDiscount(l.lineDiscount);
            item.setLineTotal(lineTotal);
            sale.addItem(item);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = orderDiscount == null ? BigDecimal.ZERO : orderDiscount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = total;

        sale.setInvoiceNo("SEED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        sale.setCashierId(actorId);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(total);
        sale.setPaymentMethod(method);
        sale.setPaidAmount(paid);
        sale.setChangeAmount(BigDecimal.ZERO);

        SaleEntity saved = saleRepository.save(sale);

        for (SaleItemEntity it : saved.getItems()) {
            stockLedger.record(
                    StockMovementType.SALE,
                    it.getProductId(),
                    -it.getQty(),
                    null,
                    "SALE",
                    saved.getId(),
                    null,
                    actorId
            );
        }

        auditService.record(
                AuditAction.SALE_CREATE,
                AuditEntityType.SALE,
                saved.getId(),
                actorId,
                null
        );
    }

    private static BigDecimal bd(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
